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

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;


/**
 * An immutable implementation of {@link ServletConfig}
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread-safe.
 * 
 */
public final class ImmutableServletConfig implements ServletConfig {
	private final static String SERVLET_NAME = "___jsp_servlet";
	
	private final ServletContext servletContext;        

    public ImmutableServletConfig(ServletContext servletContext) {
    	this.servletContext = servletContext;
    }

    /**
     * {@inheritDoc}
     */
    public String getServletName() {
        return SERVLET_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration<String> getInitParameterNames(){
    	return Collections.enumeration(new HashSet<String>());
    }

	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public String getInitParameter(String name) {
		return null;
	}
}
