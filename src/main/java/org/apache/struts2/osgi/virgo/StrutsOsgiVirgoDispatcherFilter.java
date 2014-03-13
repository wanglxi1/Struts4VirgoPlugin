package org.apache.struts2.osgi.virgo;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.osgi.virgo.internal.Struts;
import org.apache.struts2.osgi.virgo.internal.StrutsFactoryMonitor;
import org.apache.struts2.osgi.virgo.internal.struts.OsgiUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrutsOsgiVirgoDispatcherFilter implements javax.servlet.Filter {
	private static final String FILTER_PATTERN = "(& (objectClass=" + Struts.class.getName() + ")("+StrutsFactoryMonitor.KEY_HOST_ID+"=%d))";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private ServiceTracker<Struts, Object> tracker;
	private BundleContext bundleContext;
	private final Map<Bundle, Struts> strutses = new ConcurrentHashMap<Bundle, Struts>();

	private Filter createFilter(BundleContext bundleContext) throws InvalidSyntaxException {
		String filterString = String.format(FILTER_PATTERN, bundleContext.getBundle().getBundleId());
		return FrameworkUtil.createFilter(filterString);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.bundleContext = OsgiUtil.getBundleContextFromVirgoServlet(filterConfig.getServletContext());
		try {
			this.tracker = new ServiceTracker<Struts, Object>(bundleContext, createFilter(bundleContext), new OsgiStrutsRegistryCustomizer());
		} catch (InvalidSyntaxException e) {
			throw new ServletException(e);
		}
		
		this.tracker.open();
        logger.info("Struts registry created");
	}
	
	@Override
	public void destroy() {
		logger.info("Struts registry destroyed");
        this.tracker.close();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if ((request instanceof HttpServletRequest) && (response instanceof HttpServletResponse)) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            Bundle currentBundle = DefaultBundleAccessor.getInstance().getCurrentBundle();
            if(currentBundle != null) {
            	Struts struts = strutses.get(currentBundle);
    			if(struts != null) {
    				struts.handleRequest(httpRequest, httpResponse);
    				return;
    			}
            }
		}
		chain.doFilter(request, response);
	}

	

	private final class OsgiStrutsRegistryCustomizer implements ServiceTrackerCustomizer<Struts, Object> {

		public Object addingService(ServiceReference<Struts> reference) {
			String contextPath = getStrutsContextPath(reference);
			if (contextPath != null) {
				logger.info("Adding struts service '{}' to service registry for context path '{}'", reference.toString(), contextPath);
				Struts struts = bundleContext.getService(reference);
				
				strutses.put(reference.getBundle(), struts);
				
				return null;
			}

			logger.warn("Not adding struts service '{}' to service registry as context path is null", reference.toString());
			return null;
		}

		public void modifiedService(ServiceReference<Struts> reference, Object service) {}

		public void removedService(ServiceReference<Struts> reference, Object service) {
			String contextPath = getStrutsContextPath(reference);
			if (contextPath != null) {
				logger.info("Removing struts service '{}' from service registry for context path '{}'", reference.toString(), contextPath);
				
				strutses.remove(reference.getBundle());
			}

			logger.warn("Not removing struts service '{}' from registry as context path is null", reference.toString());
			bundleContext.ungetService(reference);
		}

		private String getStrutsContextPath(ServiceReference<Struts> reference) {
			return (String) reference.getProperty(StrutsFactoryMonitor.KEY_CONTEXT_PATH);
		}

	}

}
