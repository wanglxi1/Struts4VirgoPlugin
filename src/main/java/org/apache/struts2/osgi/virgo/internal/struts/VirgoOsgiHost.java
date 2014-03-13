package org.apache.struts2.osgi.virgo.internal.struts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class VirgoOsgiHost implements OsgiHost {
	
	private ServletContext servletContext;

	@Override
	public void init(ServletContext servletContext) {
		this.servletContext = servletContext;
		this.servletContext.setAttribute(OSGI_BUNDLE_CONTEXT, this.getBundleContext());
	}
	
	@Override
	public void destroy() throws Exception {
		this.servletContext = null;
	}

	
	 /**
     * This bundle map will not change, but the status of the bundles can change over time.
     * Use getActiveBundles() for active bundles
     */
	@Override
    public Map<String, Bundle> getBundles() {
        Map<String, Bundle> bundles = new HashMap<String, Bundle>();
        for (Bundle bundle : this.getBundleContext().getBundles()) {
            bundles.put(bundle.getSymbolicName(), bundle);
        }

        return Collections.unmodifiableMap(bundles);
    }

	@Override
    public Map<String, Bundle> getActiveBundles() {
        Map<String, Bundle> bundles = new HashMap<String, Bundle>();
        for (Bundle bundle : this.getBundleContext().getBundles()) {
            if (bundle.getState() == Bundle.ACTIVE)
                bundles.put(bundle.getSymbolicName(), bundle);
        }

        return Collections.unmodifiableMap(bundles);
    }

	@Override
	public BundleContext getBundleContext() {
		return OsgiUtil.getBundleContextFromVirgoServlet(servletContext);
	}

}
