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

import java.util.List;

import org.apache.struts2.osgi.virgo.internal.deployer.StrutsFactory;
import org.eclipse.virgo.util.osgi.manifest.VersionRange;
import org.eclipse.virgo.util.osgi.manifest.parse.HeaderDeclaration;
import org.eclipse.virgo.util.osgi.manifest.parse.HeaderParser;
import org.eclipse.virgo.util.osgi.manifest.parse.HeaderParserFactory;
import org.eclipse.virgo.util.osgi.manifest.parse.ParserLogger;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StrutsHostDefinition {

    private static final Logger logger = LoggerFactory.getLogger(StrutsHostDefinition.class);

    private final String symbolicName;

    private final VersionRange versionRange;

    private StrutsHostDefinition(String symbolicName, VersionRange versionRange) {
        this.symbolicName = symbolicName;
        this.versionRange = versionRange;
    }

    public String getSymbolicName() {
        return this.symbolicName;
    }

    public VersionRange getVersionRange() {
        return this.versionRange;
    }

    public static StrutsHostDefinition parse(String descriptor) {
        HeaderParser parser = createHeaderParser();
        List<HeaderDeclaration> header = parser.parseHeader(descriptor);
        if (header == null || header.size() != 1) {
            logger.error("Invalid Struts-Host header '{}'", descriptor);
            throw new IllegalArgumentException("Invalid Struts-Host header '" + descriptor + "'");
        }
        HeaderDeclaration declaration = header.get(0);
        String factoryName = declaration.getNames().get(0);

        String rangeString = declaration.getAttributes().get(Constants.VERSION_ATTRIBUTE);
        return createStrutsHostDefinition(factoryName, rangeString);
    }

    private static StrutsHostDefinition createStrutsHostDefinition(String factoryName, String rangeString) {
        VersionRange range = VersionRange.NATURAL_NUMBER_RANGE;
        if (rangeString != null) {
            range = new VersionRange(rangeString);
        }
        logger.info("Creating a StrutsHostDefinition for factory '{}' and range '{}'", factoryName, range);
        return new StrutsHostDefinition(factoryName, range);
    }

    private static HeaderParser createHeaderParser() {
        return HeaderParserFactory.newHeaderParser(new ParserLogger() {

            public String[] errorReports() {
                return new String[0];
            }

            public void outputErrorMsg(Exception re, String item) {
                logger.error(item, re);
            }
        });
    }
    public static StrutsHostDefinition fromServiceReference(ServiceReference<StrutsFactory> strutsFactoryReference) {
        String factoryName = (String) strutsFactoryReference.getProperty(StrutsFactory.FACTORY_NAME_PROPERTY);
        if (factoryName == null) {
            logger.error("Missing service property '{}'", StrutsFactory.FACTORY_NAME_PROPERTY);
            throw new IllegalArgumentException("Missing service property '" + StrutsFactory.FACTORY_NAME_PROPERTY + "'");
        }
        String rangeString = (String) strutsFactoryReference.getProperty(StrutsFactory.FACTORY_RANGE_PROPERTY);
        return createStrutsHostDefinition(factoryName, rangeString);
    }
}
