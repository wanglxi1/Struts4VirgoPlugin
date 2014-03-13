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

package org.apache.struts2.osgi.virgo.internal.deployer;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.struts2.osgi.virgo.internal.StrutsHostDefinition;
import org.apache.struts2.osgi.virgo.internal.struts.OsgiUtil;
import org.apache.struts2.osgi.virgo.internal.webapp.WebAppStrutsFactory;
import org.eclipse.gemini.web.tomcat.spi.WebBundleClassLoaderFactory;
import org.eclipse.virgo.kernel.deployer.core.DeploymentException;
import org.eclipse.virgo.kernel.install.artifact.BundleInstallArtifact;
import org.eclipse.virgo.kernel.install.artifact.InstallArtifact;
import org.eclipse.virgo.kernel.install.artifact.InstallArtifactLifecycleListenerSupport;
import org.eclipse.virgo.kernel.shim.scope.Scope;
import org.eclipse.virgo.medic.eventlog.EventLogger;
import org.eclipse.virgo.util.osgi.ServiceRegistrationTracker;
import org.eclipse.virgo.util.osgi.manifest.BundleManifest;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Concurrent Semantics</strong><br />
 * Thread-safe.
 * 
 */
final class StrutsLifecycleListener extends InstallArtifactLifecycleListenerSupport {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<InstallArtifact, ServiceRegistrationTracker> registrationTrackers = new ConcurrentHashMap<InstallArtifact, ServiceRegistrationTracker>();

    private final WebBundleClassLoaderFactory classLoaderFactory;

    private final EventLogger eventLogger;

    public StrutsLifecycleListener(WebBundleClassLoaderFactory classLoaderFactory, EventLogger eventLogger) {
        this.classLoaderFactory = classLoaderFactory;
        this.eventLogger = eventLogger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStarted(InstallArtifact installArtifact) throws DeploymentException {
        if (OsgiUtil.isNeedStrutsVirgoSupport(installArtifact)) {
            Bundle bundle = ((BundleInstallArtifact) installArtifact).getBundle();
            BundleManifest bundleManifest = OsgiUtil.getBundleManifest((BundleInstallArtifact) installArtifact);

            ServiceRegistration<StrutsFactory> registration = createAndRegisterStrutsFactoryService(bundle, bundleManifest);

            ServiceRegistrationTracker registrationTracker = new ServiceRegistrationTracker();
            registrationTracker.track(registration);

            this.registrationTrackers.put(installArtifact, registrationTracker);
        }
    }

    ServiceRegistration<StrutsFactory> createAndRegisterStrutsFactoryService(Bundle bundle, BundleManifest bundleManifest) {
        logger.info("Creating a StrutsFactory for bundle '{}'", bundle);
        StrutsFactory strutsFactory = new WebAppStrutsFactory(bundle, this.classLoaderFactory, this.eventLogger);

        StrutsHostDefinition hostDefinition = OsgiUtil.getStrutsHostHeader(bundleManifest);

        Dictionary<String, String> serviceProperties= new Hashtable<String, String>();
        serviceProperties.put(Scope.PROPERTY_SERVICE_SCOPE, Scope.SCOPE_ID_GLOBAL); // expose service outside any containing scope
        serviceProperties.put(StrutsFactory.FACTORY_NAME_PROPERTY, hostDefinition.getSymbolicName());
        serviceProperties.put(StrutsFactory.FACTORY_RANGE_PROPERTY, hostDefinition.getVersionRange().toParseString());

        ServiceRegistration<StrutsFactory> registration = bundle.getBundleContext().registerService(StrutsFactory.class, strutsFactory, serviceProperties);
        return registration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopping(InstallArtifact installArtifact) {
        logger.info("Destroying StrutsFactory for bundle '{}'", installArtifact.getName());
        ServiceRegistrationTracker serviceRegistrationTracker = this.registrationTrackers.remove(installArtifact);
        if (serviceRegistrationTracker != null) {
            serviceRegistrationTracker.unregisterAll();
        }
    }
    
}
