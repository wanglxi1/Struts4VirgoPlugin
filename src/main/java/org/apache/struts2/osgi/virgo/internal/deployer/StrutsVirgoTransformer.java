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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.struts2.osgi.virgo.internal.struts.OsgiUtil;
import org.eclipse.gemini.web.core.InstallationOptions;
import org.eclipse.gemini.web.core.WebBundleManifestTransformer;
import org.eclipse.virgo.kernel.deployer.core.DeploymentException;
import org.eclipse.virgo.kernel.install.artifact.BundleInstallArtifact;
import org.eclipse.virgo.kernel.install.artifact.InstallArtifact;
import org.eclipse.virgo.kernel.install.environment.InstallEnvironment;
import org.eclipse.virgo.kernel.install.pipeline.stage.transform.Transformer;
import org.eclipse.virgo.util.common.GraphNode;
import org.eclipse.virgo.util.common.GraphNode.ExceptionThrowingDirectedAcyclicGraphVisitor;
import org.eclipse.virgo.util.osgi.manifest.BundleManifest;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Thread-safe.
 * 
 */
final class StrutsVirgoTransformer implements Transformer {

    private static final String STRUTS_MODULE_TYPE = "web-struts";

    private static final Logger logger = LoggerFactory.getLogger(StrutsVirgoTransformer.class);

    private final WebBundleManifestTransformer manifestTransformer;
    
    private InstallOptionsFactory installOptionFactory = new DefaultInstallOptionsFactory();

    public StrutsVirgoTransformer(WebBundleManifestTransformer manifestTransformer) {
        this.manifestTransformer = manifestTransformer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transform(GraphNode<InstallArtifact> installGraph, InstallEnvironment installEnvironment) throws DeploymentException {
        installGraph.visit(new ExceptionThrowingDirectedAcyclicGraphVisitor<InstallArtifact, DeploymentException>() {

            public boolean visit(GraphNode<InstallArtifact> node) throws DeploymentException {
                InstallArtifact installArtifact = node.getValue();
                if (OsgiUtil.isNeedStrutsVirgoSupport(installArtifact)) {
                    BundleManifest bundleManifest = OsgiUtil.getBundleManifest((BundleInstallArtifact) installArtifact);
                    doTransform(bundleManifest, getSourceUrl(installArtifact));
                }
                return true;
            }
        });
    }
    
    void doTransform(BundleManifest bundleManifest, URL sourceUrl) throws DeploymentException {
        logger.info("Transforming bundle at '{}'", sourceUrl.toExternalForm());
        
        try {
            bundleManifest.setModuleType(STRUTS_MODULE_TYPE);
            bundleManifest.setHeader("SpringSource-DefaultWABHeaders", "true");
            bundleManifest.setHeader(Constants.BUNDLE_CLASSPATH, ".");
            InstallationOptions installationOptions = installOptionFactory.createDefaultInstallOptions();
            this.manifestTransformer.transform(bundleManifest, sourceUrl, installationOptions, false);
        } catch (IOException ioe) {
            logger.error(String.format("Error transforming manifest for struts '%s' version '%s'",
                bundleManifest.getBundleSymbolicName().getSymbolicName(), bundleManifest.getBundleVersion()), ioe);
            throw new DeploymentException("Error transforming manifest for struts '" + bundleManifest.getBundleSymbolicName().getSymbolicName()
                + "' version '" + bundleManifest.getBundleVersion() + "'", ioe);
        }
    }

    private static URL getSourceUrl(InstallArtifact installArtifact) throws DeploymentException {
        File file = installArtifact.getArtifactFS().getFile();
        if (file != null) {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException murle) {
                logger.error(String.format("Install artifact '%s' has source URI that is not a valid URL", installArtifact), murle);
                throw new DeploymentException("Install artifact '" + installArtifact + "' has source URI that is not a valid URL", murle);
            }
        } else {
            logger.error("Install artifact '{}' has a null source URI", installArtifact);
            throw new DeploymentException("Install artifact '" + installArtifact + "' has a null source URI");
        }
    }

    public void setInstallOptionFactory(InstallOptionsFactory installOptionsFactory) {
        this.installOptionFactory = installOptionsFactory;
    }
}
