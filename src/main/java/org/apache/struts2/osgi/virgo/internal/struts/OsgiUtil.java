/*
 * $Id$
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.struts2.osgi.virgo.internal.struts;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.RequestUtils;
import org.apache.struts2.osgi.virgo.internal.StrutsHostDefinition;
import org.eclipse.gemini.web.core.WebContainer;
import org.eclipse.virgo.kernel.deployer.core.DeploymentException;
import org.eclipse.virgo.kernel.install.artifact.BundleInstallArtifact;
import org.eclipse.virgo.kernel.install.artifact.InstallArtifact;
import org.eclipse.virgo.util.osgi.manifest.BundleManifest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;

public class OsgiUtil {
    private static final Logger LOG = LoggerFactory.getLogger(OsgiUtil.class);
    public static final String OSGI_HEADER_STRUTS_HOST = "Struts2-Host";
    
    public static BundleContext getBundleContextFromVirgoServlet(ServletContext context) {
    	return (BundleContext)context.getAttribute(WebContainer.ATTRIBUTE_BUNDLE_CONTEXT);
    }
    
    public static String parseNamespace(String uri, Collection<String> namespaces) {
		String namespace;
		int lastSlash = uri.lastIndexOf("/");
		if (lastSlash == -1) {
			namespace = "";
		} else if (lastSlash == 0) {
			// ww-1046, assume it is the root namespace, it will fallback to
			// default namespace anyway if not found in root namespace.
			namespace = "/";
		} else {
			// Try to find the namespace in those defined, defaulting to ""
			String prefix = uri.substring(0, lastSlash);
			namespace = "";
			boolean rootAvailable = false;
			// Find the longest matching namespace, defaulting to the default
			for (String ns : namespaces) {
				if (ns != null && prefix.startsWith(ns) && (prefix.length() == ns.length() || prefix.charAt(ns.length()) == '/')) {
					if (ns.length() > namespace.length()) {
						namespace = ns;
					}
				}
				if ("/".equals(ns)) {
					rootAvailable = true;
				}
			}

			// Still none found, use root namespace if found
			if (rootAvailable && "".equals(namespace)) {
				namespace = "/";
			}
		}
		
		return namespace;
	}
    public static String parseNamespace(HttpServletRequest request, Collection<String> namespaces) {
    	return parseNamespace(getUri(request), namespaces);
    }
    private static String getUri(HttpServletRequest request) {
        // handle http dispatcher includes.
        String uri = (String) request.getAttribute("javax.servlet.include.servlet_path");
        if (uri != null) {
            return uri;
        }

        uri = RequestUtils.getServletPath(request);
        if (uri != null && !"".equals(uri)) {
            return uri;
        }

        uri = request.getRequestURI();
        return uri.substring(request.getContextPath().length());
    }
    
    
    
    public static boolean isNeedStrutsVirgoSupport(InstallArtifact installArtifact) throws DeploymentException {
    	if (installArtifact instanceof BundleInstallArtifact) {
            return OsgiUtil.hasStrutsHostHeader(getBundleManifest((BundleInstallArtifact) installArtifact));
        } else {
            return false;
        }
    }
    public static BundleManifest getBundleManifest(BundleInstallArtifact bundleInstallArtifact) throws DeploymentException {
        try {
            return bundleInstallArtifact.getBundleManifest();
        } catch (IOException ioe) {
            throw new DeploymentException("Failed to get bundle manifest from '" + bundleInstallArtifact + "'", ioe);
        }
    }
    public static boolean hasStrutsHostHeader(BundleManifest manifest) {
        return manifest.getHeader(OSGI_HEADER_STRUTS_HOST) != null;
    }
    public static boolean hasStrutsHostHeader(Bundle bundle) {
        return bundle.getHeaders().get(OSGI_HEADER_STRUTS_HOST) != null;
    }

    public static StrutsHostDefinition getStrutsHostHeader(BundleManifest manifest) {
        String header = manifest.getHeader(OSGI_HEADER_STRUTS_HOST);
        return (header == null ? null : StrutsHostDefinition.parse(header));
    }
    
    
    /**
     * A bundle is a jar, and a bunble URL will be useless to clients, this method translates
     * a URL to a resource inside a bundle from "bundle:something/path" to "jar:file:bundlelocation!/path"
     */
    public static URL translateBundleURLToJarURL(URL bundleUrl, Bundle bundle) throws MalformedURLException {
        if (bundleUrl != null && "bundle".equalsIgnoreCase(bundleUrl.getProtocol())) {
            StringBuilder sb = new StringBuilder("jar:");
            sb.append(bundle.getLocation());
            sb.append("!");
            sb.append(bundleUrl.getFile());
            return new URL(sb.toString());
        }

        return bundleUrl;
    }

    /**
     * Calls getBean() on the passed object using refelection. Used on Spring context
     * because they are loaded from bundles (in anothe class loader)
     */
    public static Object getBean(Object beanFactory, String beanId) {
        try {
            Method getBeanMethod = beanFactory.getClass().getMethod("getBean", String.class);
            return getBeanMethod.invoke(beanFactory, beanId);
        } catch (Exception ex) {
            if (LOG.isErrorEnabled())
                LOG.error("Unable to call getBean() on object of type [#0], with bean id [#1]", ex, beanFactory.getClass().getName(), beanId);
        }

        return null;
    }

    /**
     * Calls containsBean on the passed object using refelection. Used on Spring context
     * because they are loaded from bundles (in anothe class loader)
     */
    public static boolean containsBean(Object beanFactory, String beanId) {
        try {
            Method getBeanMethod = beanFactory.getClass().getMethod("containsBean", String.class);
            return (Boolean) getBeanMethod.invoke(beanFactory, beanId);
        } catch (Exception ex) {
            if (LOG.isErrorEnabled())
                LOG.error("Unable to call containsBean() on object of type [#0], with bean id [#1]", ex, beanFactory.getClass().getName(), beanId);
        }

        return false;
    }
}
