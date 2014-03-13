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

import org.eclipse.virgo.kernel.serviceability.LogEventDelegate;
import org.eclipse.virgo.medic.eventlog.Level;
import org.eclipse.virgo.medic.eventlog.LogEvent;

public enum StrutsLogEvents implements LogEvent {
    HOST_CREATED(0, Level.INFO), //
    HOST_DESTROYED(1, Level.INFO), //

    STRUTS_BOUND(10, Level.INFO), //
    STRUTS_UNBOUND(11, Level.INFO), //
    STRUTS_INIT_FAILURE(12, Level.ERROR);

    private static final String PREFIX = "ST";

    private final LogEventDelegate delegate;

    private StrutsLogEvents(int code, Level level) {
        this.delegate = new LogEventDelegate(PREFIX, code, level);
    }

    /**
     * {@inheritDoc}
     */
    public String getEventCode() {
        return this.delegate.getEventCode();
    }

    /**
     * {@inheritDoc}
     */
    public Level getLevel() {
        return this.delegate.getLevel();
    }

}
