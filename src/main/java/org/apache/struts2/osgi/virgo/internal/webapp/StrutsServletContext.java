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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.apache.struts2.osgi.virgo.internal.StrutsException;
import org.eclipse.virgo.util.common.IterableEnumeration;
import org.osgi.framework.Bundle;

/**
 * TODO Document StrutsServletContext
 * <p />
 *
 * <strong>Concurrent Semantics</strong><br />
 *
 * TODO Document concurrent semantics of StrutsServletContext
 *
 */
public class StrutsServletContext implements ServletContext {

    private static final String HOST_PATH_PREFIX = "host:";
    
    private final ServletContext delegate;
    private final Bundle strutsBundle;
    
    
    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    public StrutsServletContext(ServletContext delegate, Bundle strutsBundle) {
        this.delegate = delegate;
        this.strutsBundle = strutsBundle;
    }

    /**
     * @param name
     * @return
     * @see javax.servlet.ServletContext#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name) {
        Object attribute = this.attributes.get(name);
        if (attribute == null) {
            attribute = delegate.getAttribute(name);
        }
        return attribute; 
    }

    /**
     * @return
     * @see javax.servlet.ServletContext#getAttributeNames()
     */
    public Enumeration<String> getAttributeNames() {
        Set<String> attributeNamesSet = new HashSet<String>(this.attributes.keySet());
        IterableEnumeration<String> delegateAttributeNames = new IterableEnumeration<String>((Enumeration<String>)delegate.getAttributeNames());        
        for (String delegateAttributeName : delegateAttributeNames) {
            attributeNamesSet.add(delegateAttributeName);
        }
        Vector<String> attributeNames = new Vector<String>();
        for (String attributeName : attributeNamesSet) {
            attributeNames.add(attributeName);
        }
        return attributeNames.elements();
    }

    /**
     * @param uripath
     * @return
     * @see javax.servlet.ServletContext#getContext(java.lang.String)
     */
    public ServletContext getContext(String uripath) {
        return delegate.getContext(uripath);
    }

    /**
     * @return
     * @see javax.servlet.ServletContext#getContextPath()
     */
    public String getContextPath() {
        return delegate.getContextPath();
    }

    /**
     * @param name
     * @return
     * @see javax.servlet.ServletContext#getInitParameter(java.lang.String)
     */
    public String getInitParameter(String name) {
        return delegate.getInitParameter(name);
    }

    /**
     * @return
     * @see javax.servlet.ServletContext#getInitParameterNames()
     */
    public Enumeration<String> getInitParameterNames() {
        return delegate.getInitParameterNames();
    }

    /**
     * @return
     * @see javax.servlet.ServletContext#getMajorVersion()
     */
    public int getMajorVersion() {
        return delegate.getMajorVersion();
    }

    /**
     * @param file
     * @return
     * @see javax.servlet.ServletContext#getMimeType(java.lang.String)
     */
    public String getMimeType(String file) {
        return delegate.getMimeType(file);
    }

    /**
     * @return
     * @see javax.servlet.ServletContext#getMinorVersion()
     */
    public int getMinorVersion() {
        return delegate.getMinorVersion();
    }

    /**
     * @param name
     * @return
     * @see javax.servlet.ServletContext#getNamedDispatcher(java.lang.String)
     */
    public RequestDispatcher getNamedDispatcher(String name) {
        return delegate.getNamedDispatcher(name);
    }

    /**
     * @param path
     * @return
     * @see javax.servlet.ServletContext#getRealPath(java.lang.String)
     */
    public String getRealPath(String path) {
        return delegate.getRealPath(path);
    }

    /**
     * @param path
     * @return
     * @see javax.servlet.ServletContext#getRequestDispatcher(java.lang.String)
     */
    public RequestDispatcher getRequestDispatcher(String path) {
        return delegate.getRequestDispatcher(path);
    }

    /**
     * @param path
     * @return
     * @throws MalformedURLException
     * @see javax.servlet.ServletContext#getResource(java.lang.String)
     */
    public URL getResource(String path) throws MalformedURLException {
    	boolean hostOnly = false;
        if (path.startsWith(HOST_PATH_PREFIX)) {
            path = path.substring(HOST_PATH_PREFIX.length());
            hostOnly = true;
        }
		if (path == null || !path.startsWith("/")) {
			throw new MalformedURLException(String.format("'%s' is not a valid resource path", path));
		}
        if(hostOnly){
        	return delegate.getResource(path);
        } else {
        	URL resource = getLocalResource(path);
            if (resource == null) {
                resource = delegate.getResource(path);
            }
            return resource;
        }
    }

    private URL getLocalResource(String path) {
        URL entry = this.strutsBundle.getEntry(path);
//        String namespace = OsgiUtil.parseNamespace(path, this.namespaces);
//        if (entry == null && path.startsWith(namespace)) {
//            entry = this.strutsBundle.getEntry(path.substring(namespace.length()));
//        }
        return entry;
    }
    
    

    /**
     * @param path
     * @return
     * @see javax.servlet.ServletContext#getResourceAsStream(java.lang.String)
     */
    public InputStream getResourceAsStream(String path) {
        if (path.startsWith(HOST_PATH_PREFIX)) {
            path = path.substring(HOST_PATH_PREFIX.length());
        } else {
	        URL resource = getLocalResource(path);
	        if (resource != null) {
	            try {
	                return resource.openStream();
	            } catch (IOException e) {
	                throw new StrutsException("Failed to open stream for resource " + resource + " in bundle " + this.strutsBundle, e);
	            }
	        }
        }
        return delegate.getResourceAsStream(path);
    }

    /**
     * @param path
     * @return
     * @see javax.servlet.ServletContext#getResourcePaths(java.lang.String)
     */
    public Set<String> getResourcePaths(String path) {       
        Enumeration<?> entryPaths = this.strutsBundle.getEntryPaths(path);
        if (entryPaths == null) {
            return null;
        } else {
            Set<String> resourcePaths = new HashSet<String>();
            while (entryPaths.hasMoreElements()) {
                String entryPath = (String)entryPaths.nextElement();
                if (path.startsWith("/") && !entryPath.startsWith("/")) {
                    entryPath = "/" + entryPath;
                }
                resourcePaths.add((String)entryPath);
            }
            return resourcePaths;
        }                
    }

    /**
     * @return
     * @see javax.servlet.ServletContext#getServerInfo()
     */
    public String getServerInfo() {
        return delegate.getServerInfo();
    }

    /**
     * @param name
     * @return
     * @throws ServletException
     * @deprecated
     * @see javax.servlet.ServletContext#getServlet(java.lang.String)
     */
    public Servlet getServlet(String name) throws ServletException {
        return delegate.getServlet(name);
    }

    /**
     * @return
     * @see javax.servlet.ServletContext#getServletContextName()
     */
    public String getServletContextName() {
        return delegate.getServletContextName();
    }

    /**
     * @return
     * @deprecated
     * @see javax.servlet.ServletContext#getServletNames()
     */
    public Enumeration<String> getServletNames() {
        return delegate.getServletNames();
    }

    /**
     * @return
     * @deprecated
     * @see javax.servlet.ServletContext#getServlets()
     */
    public Enumeration<Servlet> getServlets() {
        return delegate.getServlets();
    }

    /**
     * @param exception
     * @param msg
     * @deprecated
     * @see javax.servlet.ServletContext#log(java.lang.Exception, java.lang.String)
     */
    public void log(Exception exception, String msg) {
        delegate.log(exception, msg);
    }

    /**
     * @param message
     * @param throwable
     * @see javax.servlet.ServletContext#log(java.lang.String, java.lang.Throwable)
     */
    public void log(String message, Throwable throwable) {
        delegate.log(message, throwable);
    }

    /**
     * @param msg
     * @see javax.servlet.ServletContext#log(java.lang.String)
     */
    public void log(String msg) {
        delegate.log(msg);
    }

    /**
     * @param name
     * @see javax.servlet.ServletContext#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    /**
     * @param name
     * @param object
     * @see javax.servlet.ServletContext#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String name, Object object) {
        this.attributes.put(name, object);
    }

	@Override
	public int getEffectiveMajorVersion() {
		return this.delegate.getEffectiveMajorVersion();
	}

	@Override
	public int getEffectiveMinorVersion() {
		return this.delegate.getEffectiveMinorVersion();
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		return this.delegate.setInitParameter(name, value);
	}

	@Override
	public Dynamic addServlet(String servletName, String className) {
		return this.delegate.addServlet(servletName, className);
	}

	@Override
	public Dynamic addServlet(String servletName, Servlet servlet) {
		return this.delegate.addServlet(servletName, servlet);
	}

	@Override
	public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
		return this.delegate.addServlet(servletName, servletClass);
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
		return this.delegate.createServlet(clazz);
	}

	@Override
	public ServletRegistration getServletRegistration(String servletName) {
		return this.delegate.getServletRegistration(servletName);
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		return this.delegate.getServletRegistrations();
	}

	@Override
	public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
		return this.delegate.addFilter(filterName, className);
	}

	@Override
	public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
		return this.delegate.addFilter(filterName, filter);
	}

	@Override
	public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
		return this.delegate.addFilter(filterName, filterClass);
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
		return this.delegate.createFilter(clazz);
	}

	@Override
	public FilterRegistration getFilterRegistration(String filterName) {
		return this.delegate.getFilterRegistration(filterName);
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		return this.delegate.getFilterRegistrations();
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		return this.delegate.getSessionCookieConfig();
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
		this.delegate.setSessionTrackingModes(sessionTrackingModes);
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		return this.delegate.getDefaultSessionTrackingModes();
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		return this.delegate.getEffectiveSessionTrackingModes();
	}

	@Override
	public void addListener(String className) {
		this.delegate.addListener(className);
	}

	@Override
	public <T extends EventListener> void addListener(T t) {
		this.delegate.addListener(t);
	}

	@Override
	public void addListener(Class<? extends EventListener> listenerClass) {
		this.delegate.addListener(listenerClass);
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
		return this.delegate.createListener(clazz);
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		return this.delegate.getJspConfigDescriptor();
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.delegate.getClassLoader();
	}

	@Override
	public void declareRoles(String... roleNames) {
		this.delegate.declareRoles(roleNames);
	}
}
