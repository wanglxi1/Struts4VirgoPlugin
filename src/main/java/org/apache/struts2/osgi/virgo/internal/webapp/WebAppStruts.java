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

package org.apache.struts2.osgi.virgo.internal.webapp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.struts2.osgi.virgo.DefaultBundleAccessor;
import org.apache.struts2.osgi.virgo.internal.Host;
import org.apache.struts2.osgi.virgo.internal.Struts;
import org.apache.struts2.osgi.virgo.internal.StrutsException;
import org.apache.struts2.osgi.virgo.internal.StrutsLogEvents;
import org.apache.struts2.osgi.virgo.internal.webapp.ManagerUtils.ClassLoaderCallback;
import org.eclipse.gemini.web.core.WebContainer;
import org.eclipse.gemini.web.tomcat.spi.WebBundleClassLoaderFactory;
import org.eclipse.virgo.medic.eventlog.EventLogger;
import org.eclipse.virgo.util.io.IOUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Document WebAppStruts
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * TODO Document concurrent semantics of WebAppStruts
 * 
 */
class WebAppStruts implements Struts {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final static Class SERVLET_CLASS = org.apache.jasper.servlet.JspServlet.class;
    
    private final Host host;
    private final Bundle strutsBundle;
    private volatile ClassLoader strutsClassLoader;
    private final WebBundleClassLoaderFactory classLoaderFactory;
    private final EventLogger eventLogger;
    private Servlet servlet;
    private Collection<String> namespaces;
    

    /**
     * @param host
     * @param strutsBundle
     * @param webBundleClassLoaderFactory
     */
    public WebAppStruts(Host host, Bundle strutsBundle, WebBundleClassLoaderFactory webBundleClassLoaderFactory, EventLogger eventLogger) {
        this.host = host;
        this.strutsBundle = strutsBundle;
        this.classLoaderFactory = webBundleClassLoaderFactory;
        this.eventLogger = eventLogger;
        
        this.namespaces = DefaultBundleAccessor.getInstance().getPackagesByBundle(strutsBundle);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ServletException
     */
    public final void init() throws ServletException {
        logger.info("Initializing struts '{}'", this.strutsBundle.getSymbolicName());
        StrutsServletContext servletContext = new StrutsServletContext(this.host.getServletContext(), this.strutsBundle);
        servletContext.setAttribute(WebContainer.ATTRIBUTE_BUNDLE_CONTEXT, this.strutsBundle.getBundleContext());

        this.strutsClassLoader = this.classLoaderFactory.createWebBundleClassLoader(this.strutsBundle);

        try {
            ((Lifecycle) strutsClassLoader).start();
        } catch (LifecycleException e) {
            logger.error("Failed to start struts's class loader", e);
            throw new ServletException("Failed to start web bundle's class loader", e);
        }
        
        this.initServlet(servletContext);

        this.eventLogger.log(StrutsLogEvents.STRUTS_BOUND, this.strutsBundle.getSymbolicName());
    }
    
    private final void initServlet(final StrutsServletContext servletContext) throws ServletException {
    	try {
            ManagerUtils.doWithThreadContextClassLoader(this.strutsClassLoader, new ClassLoaderCallback<Void>() {
                public Void doWithClassLoader() throws ServletException {
                	try {
						WebAppStruts.this.servlet = (Servlet)SERVLET_CLASS.newInstance();
					} catch (Exception e) {
						throw new ServletException("Create Servlet Fail", e);
					}
                	
                	ImmutableServletConfig servletConfig = new ImmutableServletConfig(servletContext);
                	WebAppStruts.this.servlet.init(servletConfig);
                	
                    return null;
                }
            });
        } catch (IOException e) {
            logger.error("Unexpected IOException from servlet init", e);
            throw new ServletException("Unexpected IOException from servlet init", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void destroy() {
        ClassLoader strutsClassLoader = this.strutsClassLoader;

        if (strutsClassLoader != null) {
            try {
                ((Lifecycle) strutsClassLoader).stop();
            } catch (LifecycleException e) {
                logger.error("Failed to stop struts's class loader", e);
                throw new StrutsException("Failed to stop web bundle class loader", e);
            }
        } else {
            // TODO Log warning that class loader was null during destroy
        }
        this.eventLogger.log(StrutsLogEvents.STRUTS_UNBOUND, this.strutsBundle.getSymbolicName());
    }

    /**
     * {@inheritDoc}
     */
    public final void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (servlet != null) {
        	servlet.service(request, response);
        } else {
            // TODO Log warning that dispatcher is not present
            throw new ServletException("handleRequest invoked when virtual container was null");
        }
    }


    /**
     * {@inheritDoc}
     */
    public Properties getStrutsProperties() {
        Properties properties = new Properties();
        URL url = this.strutsBundle.getEntry("META-INF/struts.properties");
        if (url != null) {
            InputStream is = null;
            try {
                is = url.openStream();
                properties.load(is);
            } catch (IOException ioe) {

            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        return properties;
    }

	@Override
	public String getContextPath() {
		return this.namespaces.toString();
	}
}
