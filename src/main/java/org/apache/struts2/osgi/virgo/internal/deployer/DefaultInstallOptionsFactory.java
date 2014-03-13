/*******************************************************************************
 * This file is part of the Virgo Web Server.
 *
 * Copyright (c) 2010 Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SpringSource, a division of VMware - initial API and implementation and/or initial documentation
 *******************************************************************************/


package org.apache.struts2.osgi.virgo.internal.deployer;

import java.util.Collections;

import org.eclipse.gemini.web.core.InstallationOptions;

/**
 * User: dsklyut Date: Jun 22, 2010 Time: 12:25:51 PM
 */
public class DefaultInstallOptionsFactory implements InstallOptionsFactory {

    public InstallationOptions createDefaultInstallOptions() {
        InstallationOptions installationOptions = new InstallationOptions(Collections.<String, String> emptyMap());
        installationOptions.setDefaultWABHeaders(true);
        return installationOptions;
    }

}
