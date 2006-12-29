/* Copyright (2005-2006) Schibsted Søk AS
 *
 * TestCase.java
 *
 * Created on June 28, 2006, 12:06 PM
 *
 */

package no.schibstedsok.searchportal.site;

import java.util.Locale;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import no.schibstedsok.searchportal.site.Site;
import no.schibstedsok.searchportal.site.Site.Context;
import no.schibstedsok.searchportal.site.SiteContext;
import no.schibstedsok.searchportal.site.config.FileResourceLoader;
import no.schibstedsok.searchportal.site.config.PropertiesLoader;

/**
 * @version $Id$
 * @author <a href="mailto:mick@wever.org">Michael Semb Wever</a>
 */
public abstract class SiteTestCase extends junit.framework.TestCase {

    // Constants -----------------------------------------------------

    private static final Logger LOG = Logger.getLogger(SiteTestCase.class);

    // Attributes ----------------------------------------------------

    // Static --------------------------------------------------------

    static{
    
//        removingAppendersWarning();
//
//        // This will be dangerous if this class is loaded in non-test operation
//        if(Logger.getRootLogger().getAppender("TEST_APPEND") == null){
//            // switch appenders over to the test logfile
//            Logger.getRootLogger().removeAllAppenders();
//            Logger.getRootLogger().addAppender(LOG.getAppender("TEST_APPEND"));
//        }
    }

    // Constructors --------------------------------------------------
    
    protected SiteTestCase(){}

    /** Creates a new instance of TestCase */
    public SiteTestCase(final String testName) {
        super(testName);
    }

    // Public --------------------------------------------------------

    // Z implementation ----------------------------------------------

    // no.schibstedsok.searchportal.TestCase overrides ----------------------------

    /** TODO comment me. **/
    protected void setUp() throws Exception {
        super.setUp();

        
        MDC.put("test", getClass().getSimpleName());
        
    }

    /** TODO comment me. **/
    protected void tearDown() throws Exception {
        super.tearDown();

        MDC.remove("test");
    }    

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------
    
    protected Site.Context getSiteConstructingContext(){
        
        return new Context(){
            public String getParentSiteName(final SiteContext siteContext){
                // we have to do this manually instead of using SiteConfiguration,
                //  because SiteConfiguration relies on the parent site that we haven't get initialised.
                // That is, the PARENT_SITE_KEY property MUST be explicit in the site's configuration.properties.
                final Properties props = new Properties();
                final PropertiesLoader loader
                        = FileResourceLoader.newPropertiesLoader(siteContext, Site.CONFIGURATION_FILE, props);
                loader.abut();
                return props.getProperty(Site.PARENT_SITE_KEY);
            }
        };
    }
    
    protected Site getTestingSite(){
        
        final String basedir = System.getProperty("basedir").replaceAll("/war", "");
        return Site.valueOf(
                getSiteConstructingContext(),
                basedir.substring(basedir.lastIndexOf('/')+1).replaceAll("/", ""),
                Locale.getDefault());
    }    

    // Private -------------------------------------------------------
    
    private static void removingAppendersWarning(){
        
        LOG.warn("==================================");
        LOG.warn("REMOVING ALL APPENDERS FOR TESTING");
        LOG.warn("==================================");
        
        System.out.println("\n==================================");
        System.out.println("REMOVING ALL APPENDERS FOR TESTING");
        System.out.println("==================================");
    }
    
    // Inner classes -------------------------------------------------



    
    
}