/*
 * DataModelFilter.java
 *
 * Created on 26 January 2007, 22:29
 *
 */

package no.schibstedsok.searchportal.http.filters;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import no.schibstedsok.searchportal.datamodel.DataModel;
import no.schibstedsok.searchportal.datamodel.DataModelFactory;
import no.schibstedsok.searchportal.datamodel.generic.DataObject;
import no.schibstedsok.searchportal.datamodel.generic.StringDataObject;
import no.schibstedsok.searchportal.datamodel.junkyard.JunkYardDataObject;
import no.schibstedsok.searchportal.datamodel.request.ParametersDataObject;
import no.schibstedsok.searchportal.datamodel.site.SiteDataObject;
import no.schibstedsok.searchportal.site.Site;
import no.schibstedsok.searchportal.site.SiteContext;
import no.schibstedsok.searchportal.site.SiteKeyedFactoryInstantiationException;
import no.schibstedsok.searchportal.site.config.PropertiesLoader;
import no.schibstedsok.searchportal.site.config.SiteConfiguration;
import no.schibstedsok.searchportal.site.config.UrlResourceLoader;
import org.apache.log4j.Logger;

/** Ensures that a session is created, and that a new DataModel, with Site and Browser dataObjects,
 * exists within it.
 *
 * @author <a href="mailto:mick@semb.wever.org">Mck</a>
 * @version <tt>$Id$</tt>
 */
public final class DataModelFilter implements Filter {

    // Constants -----------------------------------------------------

    private static final Logger LOG = Logger.getLogger(DataModelFilter.class);

    // Attributes ----------------------------------------------------

    // Static --------------------------------------------------------


    // Constructors --------------------------------------------------

    /** Creates a new instance of DataModelFilter */
    public DataModelFilter() {
    }



    // Public --------------------------------------------------------

    public void init(FilterConfig config) throws ServletException {
    }

    public void doFilter(
            final ServletRequest request,
            final ServletResponse response,
            final FilterChain chain)
                throws IOException, ServletException {

        if(request instanceof HttpServletRequest){
            final HttpServletRequest httpRequest = (HttpServletRequest)request;
            final Site site = (Site) request.getAttribute(Site.NAME_KEY);

            final DataModelFactory factory;
            try{
                factory = DataModelFactory.valueOf(new DataModelFactory.Context(){
                    public Site getSite() {
                        return site;
                    }
                
                    public PropertiesLoader newPropertiesLoader(final SiteContext siteCxt,
                                                                final String resource,
                                                                final Properties properties) {
                        return UrlResourceLoader.newPropertiesLoader(siteCxt, resource, properties);
                    }
                });

            }catch(SiteKeyedFactoryInstantiationException skfie){
                LOG.error(skfie.getMessage(), skfie);
                throw new ServletException(skfie.getMessage(), skfie);
            }

            final ParametersDataObject parametersDO = updateDataModelForRequest(factory, httpRequest);

            getDataModel(factory, httpRequest).setParameters(parametersDO);
        }
        chain.doFilter(request, response);
    }



    public void destroy() {
    }

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------

    private static DataModel getDataModel(final DataModelFactory factory, final HttpServletRequest request){

        final HttpSession session = request.getSession();

        DataModel datamodel = (DataModel) session.getAttribute(DataModel.KEY);

        if(null == datamodel){
            datamodel = createDataModel(factory, request);
            session.setAttribute(DataModel.KEY, datamodel);
        }
        
        // DataModel's ControlLevel will be DATA_MODEL_CONSTRUCTION or VIEW_CONSTRUCTION (from the past request)
        //  Increment it onwards to REQUEST_CONSTRUCTION.
        return factory.incrementControlLevel(datamodel);
}

    private static DataModel createDataModel(final DataModelFactory factory, final HttpServletRequest request){

        final Site site = (Site) request.getAttribute(Site.NAME_KEY);
        final SiteConfiguration siteConf = (SiteConfiguration) request.getAttribute(SiteConfiguration.NAME_KEY);

        final DataModel datamodel = factory.instantiate();

        final SiteDataObject siteDO = factory.instantiate(
                SiteDataObject.class,
                new DataObject.Property("site", site),
                new DataObject.Property("siteConfiguration", siteConf));

        // TODO BrowserDataObject


        final JunkYardDataObject junkYardDO = factory.instantiate(
                JunkYardDataObject.class,
                new DataObject.Property("values", new Hashtable<String,Object>()));

        datamodel.setSite(siteDO);
        datamodel.setJunkYard(junkYardDO);
        
        return datamodel;
    }

    /** Update the request elements in the datamodel. **/
    private static ParametersDataObject updateDataModelForRequest(
            final DataModelFactory factory, 
            final HttpServletRequest request){
     
        // Note that we do not support String[] parameter values!
        final Map<String,StringDataObject> values = new HashMap<String,StringDataObject>();
        for(Enumeration<String> e = request.getParameterNames(); e.hasMoreElements(); ){
            final String key = e.nextElement();
            values.put(key, factory.instantiate(
                StringDataObject.class,
                new DataObject.Property("string", request.getParameter(key))));
        }
        final ParametersDataObject parametersDO = factory.instantiate(
                ParametersDataObject.class,
                new DataObject.Property("values", values),
                new DataObject.Property("contextPath", request.getContextPath()));

        
        return parametersDO;
    }
    
    // Inner classes -------------------------------------------------

}