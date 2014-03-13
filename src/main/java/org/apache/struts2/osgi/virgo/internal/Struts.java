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

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public interface Struts {
    
    void init() throws ServletException;

    void destroy();
    
    void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;

    Properties getStrutsProperties();
    
    String getContextPath();
}
