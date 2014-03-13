package org.apache.struts2.osgi.virgo;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.struts2.StrutsException;
import org.apache.struts2.osgi.virgo.internal.struts.OsgiHost;
import org.apache.struts2.osgi.virgo.internal.struts.VirgoOsgiHost;

public class StrutsOsgiVirgoListener implements ServletContextListener {
	public static final String OSGI_HOST = "__struts_osgi_host";

	private OsgiHost osgiHost;
	
	private static EnumSet<DispatcherType> DISPATCHER_TYPES = EnumSet.range(DispatcherType.FORWARD, DispatcherType.INCLUDE);
	private static String URL_PATTERN = "*.jsp";

	public final void contextInitialized(ServletContextEvent sce) {
		this.osgiHost = new VirgoOsgiHost();
		
		ServletContext servletContext = sce.getServletContext();
		servletContext.setAttribute(OSGI_HOST, osgiHost);
		
		if(servletContext instanceof ApplicationContextFacade) {
			ApplicationContextFacade applicationContext = (ApplicationContextFacade)servletContext;
			FilterRegistration filterRegistration = applicationContext.addFilter(OSGI_HOST, StrutsOsgiVirgoDispatcherFilter.class);
			filterRegistration.addMappingForUrlPatterns(DISPATCHER_TYPES, true, URL_PATTERN);
		}
		
        try {
            osgiHost.init(servletContext);
        } catch (Exception e) {
            throw new StrutsException("Eclipse Virgo failed to start", e);
        }
	}

	public final void contextDestroyed(ServletContextEvent sce) {
		try {
            osgiHost.destroy();
        } catch (Exception e) {
            throw new StrutsException("Eclipse Virgo failed to stop", e);
        }
	}
}
