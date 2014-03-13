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

import org.apache.struts2.osgi.virgo.internal.Host;
import org.apache.struts2.osgi.virgo.internal.Struts;

public interface StrutsFactory {
    
    static final String FACTORY_NAME_PROPERTY = "struts.factory.host.name";

    static final String FACTORY_RANGE_PROPERTY = "struts.factory.host.range";

    Struts createStruts(Host host);
}
