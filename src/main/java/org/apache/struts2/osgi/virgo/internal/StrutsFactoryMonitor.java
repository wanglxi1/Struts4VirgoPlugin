/*******************************************************************************
 * Copyright (c) 2008, 2010 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.apache.struts2.osgi.virgo.internal;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.struts2.osgi.virgo.internal.deployer.StrutsFactory;
import org.eclipse.virgo.medic.eventlog.EventLogger;
import org.eclipse.virgo.util.osgi.ServiceRegistrationTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StrutsFactoryMonitor implements ServiceTrackerCustomizer<StrutsFactory, Object> {
	public final static String KEY_HOST_ID = "struts.host.id";
	public final static String KEY_CONTEXT_PATH = "struts.context.path";
	public final static String KEY_NAME = "struts.name";
	
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final BundleContext bundleContext;
	private final ServiceTracker<StrutsFactory, Object> strutsFactoryTracker;
	private final EventLogger eventLogger;

	public StrutsFactoryMonitor(BundleContext bundleContext, EventLogger eventLogger) {
		this.bundleContext = bundleContext;
		this.strutsFactoryTracker = new ServiceTracker<StrutsFactory, Object>(bundleContext, StrutsFactory.class, this);
		this.eventLogger = eventLogger;
	}

	public void start() {
		this.strutsFactoryTracker.open();
	}

	public void stop() {
		this.strutsFactoryTracker.close();
	}

	public Object addingService(ServiceReference<StrutsFactory> reference) {
		StrutsFactory strutsFactory = this.bundleContext.getService(reference);
		if (strutsFactory != null) {
			BundleContext strutsBundleContext = reference.getBundle().getBundleContext();
			StrutsBinder strutsBinder = new StrutsBinder(strutsBundleContext, strutsFactory,
					StrutsHostDefinition.fromServiceReference(reference), this.eventLogger);
			strutsBinder.start();
			return strutsBinder;
		}
		logger.warn("Unable to create StrutsBinder due to missing StrutsFactory");
		return null;
	}

	public void modifiedService(ServiceReference<StrutsFactory> reference, Object service) {
	}

	public void removedService(ServiceReference<StrutsFactory> reference, Object service) {
		logger.info("Destroying StrutsBinder for bundle '{}'", reference.getBundle());
		((StrutsBinder) service).destroy();
	}

	private static enum StrutsLifecycleState {
		AWAITING_INIT, INIT_SUCCEEDED, INIT_FAILED
	}

	private static final class StrutsBinder implements ServiceListener {
		private static final String SNAP_ORDER = "struts.order";
		private final Logger logger = LoggerFactory.getLogger(this.getClass());

		private final BundleContext context;
		private final HostSelector hostSelector;
		private final Object hostStateMonitor = new Object();
		private final Object strutsStateMonitor = new Object();
		private boolean queriedInitialHosts = false;
		private ServiceReference<ServletContext> hostReference;
		private final ServiceRegistrationTracker registrationTracker = new ServiceRegistrationTracker();
		private final EventLogger eventLogger;
		private Struts struts;
		private final StrutsFactory factory;

		public StrutsBinder(final BundleContext context, final StrutsFactory strutsFactory, final StrutsHostDefinition hostDefinition,
				final EventLogger eventLogger) {
			this.context = context;
			this.hostSelector = new HostSelector(hostDefinition, (String) context.getBundle().getHeaders().get("Module-Scope"));
			this.eventLogger = eventLogger;
			this.factory = strutsFactory;
		}

		private void start() {
			registerHostListener();
		}

		private void registerHostListener() {
			try {
				this.context.addServiceListener(this, "(objectClass=javax.servlet.ServletContext)");
				logger.info("Listening for hosts to be registered.");
				searchForExistingHost();
			} catch (InvalidSyntaxException e) {
				logger.error("Filter syntax invalid");
			}
		}

		private void hostPublished(ServiceReference<ServletContext> hostReference) {
			assert (!Thread.holdsLock(this.hostStateMonitor));

			ServletContext servletContext = this.context.getService(hostReference);
			if (servletContext != null) {
				synchronized (this.hostStateMonitor) {
					Collection<ServiceReference<ServletContext>> references = new HashSet<ServiceReference<ServletContext>>();
					references.add(hostReference);
					ServiceReference<ServletContext> matchedHost = this.hostSelector.selectHost(references);

					if (matchedHost == null) {
						logger.info("Host {} did not match {} ", hostReference.getBundle().getSymbolicName(), this.hostSelector.getHostDefinition().toString());
						return;
					}
				}

				Bundle hostBundle = hostReference.getBundle();

				StrutsLifecycleState newState = StrutsLifecycleState.INIT_FAILED;

				Struts struts = this.factory.createStruts(new Host(hostBundle, servletContext));
				try {
					logger.info("Initializing struts '{}'", struts.getContextPath());
					struts.init();

					newState = StrutsLifecycleState.INIT_SUCCEEDED;

					logger.info("Publishing struts '{}'", struts.getContextPath());
					publishStrutsService(struts, hostBundle);

				} catch (ServletException e) {
					this.eventLogger.log(StrutsLogEvents.STRUTS_INIT_FAILURE,
							servletContext.getContextPath() + " --> " + struts.getContextPath(), e.getMessage());
				} finally {
					synchronized (this.strutsStateMonitor) {
						if (newState == StrutsLifecycleState.INIT_SUCCEEDED) {
							this.struts = struts;
						}
					}
				}
			}
		}

		private void publishStrutsService(Struts struts, Bundle hostBundle) {
			Hashtable<Object, Object> props = struts.getStrutsProperties();
			Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();

			for (Object key : props.keySet()) {
				serviceProperties.put(key.toString(), props.get(key));
			}

			String strutsOrder = (String) serviceProperties.get(SNAP_ORDER);
			if (strutsOrder != null) {
				serviceProperties.put(Constants.SERVICE_RANKING, Integer.parseInt(strutsOrder));
			}
			serviceProperties.put(KEY_HOST_ID, Long.toString(hostBundle.getBundleId()));
			serviceProperties.put(KEY_CONTEXT_PATH, struts.getContextPath());
			serviceProperties.put(KEY_NAME, (String) this.context.getBundle().getHeaders().get("Bundle-Name"));

			ServiceRegistration<Struts> registration = this.context.registerService(Struts.class, struts, serviceProperties);
			this.registrationTracker.track(registration);
			logger.info("Published struts service for '{}'", struts.getContextPath());
		}

		private void destroy() {
			try {
				destroyStruts();
			} finally {
				unregisterHostListener();
			}
		}

		private void unregisterHostListener() {
			logger.info("No longer listening for hosts to be registered.");
			this.context.removeServiceListener(this);
		}

		public void serviceChanged(ServiceEvent event) {
			synchronized (this.hostStateMonitor) {
				while (!queriedInitialHosts) {
					try {
						this.hostStateMonitor.wait();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}

			int type = event.getType();
			@SuppressWarnings("unchecked")
			ServiceReference<ServletContext> serviceReference = (ServiceReference<ServletContext>) event.getServiceReference();

			if (type == ServiceEvent.REGISTERED && this.hostReference == null) {
				hostPublished(serviceReference);
			} else if (type == ServiceEvent.UNREGISTERING) {
				if (serviceReference.equals(this.hostReference)) {
					hostRetracted(serviceReference);
				}
			}
		}

		private void hostRetracted(ServiceReference<ServletContext> serviceReference) {
			try {
				destroyStruts();
			} finally {
				synchronized (this.hostStateMonitor) {
					this.hostReference = null;
				}
			}
		}

		private void destroyStruts() {
			Struts s = null;
			synchronized (this.strutsStateMonitor) {
				s = this.struts;
				this.struts = null;
			}
			this.registrationTracker.unregisterAll();
			if (s != null) {
				logger.info("Retracted struts service for '{}'", s.getContextPath());
				s.destroy();
			}
		}

		private void searchForExistingHost() {
			ServiceReference<ServletContext> existingHost = null;
			Collection<ServiceReference<ServletContext>> candidates = findHostCandidiates();
			if (candidates != null && !candidates.isEmpty()) {
				logger.info("{} host candidates found", candidates.size());
			} else {
				logger.info("No host candidates found");
			}

			synchronized (this.hostStateMonitor) {
				try {
					existingHost = this.hostSelector.selectHost(candidates);
					this.queriedInitialHosts = true;
				} finally {
					this.hostStateMonitor.notifyAll();
				}
			}
			if (existingHost != null) {
				hostPublished(existingHost);
			}
		}

		private Collection<ServiceReference<ServletContext>> findHostCandidiates() {
			try {
				return this.context.getServiceReferences(ServletContext.class, null);
			} catch (InvalidSyntaxException ise) {
				throw new IllegalStateException("Unexpected invalid filter syntax with null filter", ise);
			}
		}
	}
}
