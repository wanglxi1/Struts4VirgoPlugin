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

import org.apache.struts2.osgi.virgo.internal.Host;
import org.apache.struts2.osgi.virgo.internal.Struts;
import org.apache.struts2.osgi.virgo.internal.deployer.StrutsFactory;
import org.eclipse.gemini.web.tomcat.spi.WebBundleClassLoaderFactory;
import org.eclipse.virgo.medic.eventlog.EventLogger;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebAppStrutsFactory implements StrutsFactory {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Bundle strutsBundle;
    private final WebBundleClassLoaderFactory classLoaderFactory;
    private final EventLogger eventLogger;

    public WebAppStrutsFactory(Bundle strutsBundle, WebBundleClassLoaderFactory webBundleClassLoaderFactory, EventLogger eventLogger) {
        this.strutsBundle = strutsBundle;
        this.classLoaderFactory = webBundleClassLoaderFactory;
        this.eventLogger = eventLogger;
    }

    public Struts createStruts(Host host) {
        logger.info("Creating new struts that binds struts bundle '{}' to host bundle '{}'", this.strutsBundle, host.getBundle());
        return new WebAppStruts(host, this.strutsBundle, this.classLoaderFactory, this.eventLogger);
    }

}
