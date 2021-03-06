/* Copyright (2005-2012) Schibsted ASA
 *   This file is part of Possom.
 *   Possom is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Possom is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with Possom.  If not, see <http://www.gnu.org/licenses/>.
 *
 * UrlResourceLoader.java
 *
 * Created on 20 January 2006, 10:24
 *
 */

package no.sesat.search.site.config;


import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import no.sesat.search.http.HTTPClient;
import no.sesat.search.site.Site;
import no.sesat.search.site.SiteContext;
import org.apache.log4j.Logger;

/** Loads resources through URL references.
 *
 * @version $Id$
 *
 */
public class UrlResourceLoader extends AbstractResourceLoader {

    // Constants -----------------------------------------------------

    private static final GeneralCacheAdministrator PRESENCE_CACHE = new GeneralCacheAdministrator();
    private static final int REFRESH_PERIOD = 60; // one minute

    private static final Logger LOG = Logger.getLogger(UrlResourceLoader.class);

    private static final String DEBUG_CHECKING_EXISTANCE_OF = "Checking existance of ";
    private static final int CACHE_CAPACITY = 1000;

    /** An application using the URLResourceLoader can make use this. **/
    public static final Site.Context SITE_CONTEXT = new Site.Context(){
            @Override
            public String getParentSiteName(final SiteContext siteContext) {
                // we have to do this manually instead of using SiteConfiguration,
                //  because SiteConfiguration relies on the parent site that we haven't get initialised.
                // That is, the PARENT_SITE_KEY property MUST be explicit in the site's configuration.properties.
                final Properties props = new Properties();
                final PropertiesLoader loader
                        = UrlResourceLoader.newPropertiesLoader(siteContext, Site.CONFIGURATION_FILE, props);

                loader.abut();
                final String parentName = props.getProperty(Site.PARENT_SITE_KEY);
                if(null == parentName && 0 == props.size()){
                    throw new IllegalArgumentException("Invalid site " + siteContext.getSite());
                }
                return parentName;
            }
        };

    // Attributes ----------------------------------------------------

    static{
        PRESENCE_CACHE.setCacheCapacity(CACHE_CAPACITY);
    }


    // Static --------------------------------------------------------

    /** Creates a SiteConfiguration.Context based off the given site and delegating all Resource handling to this.
     * The returned object also implements ResourceContext **/
    public static SiteConfiguration.Context newSiteConfigurationContext(final Site site){
        return new SiteConfiguration.Context(){
            @Override
            public PropertiesLoader newPropertiesLoader(
                    final SiteContext siteCxt,
                    final String resource,
                    final Properties properties) {

                return UrlResourceLoader.newPropertiesLoader(siteCxt, resource, properties);
            }
            public DocumentLoader newDocumentLoader(
                        final SiteContext siteCxt,
                        final String resource,
                        final DocumentBuilder builder) {

                return UrlResourceLoader.newDocumentLoader(siteCxt, resource, builder);
            }
            public BytecodeLoader newBytecodeLoader(SiteContext context, String className, final String jar) {
                return UrlResourceLoader.newBytecodeLoader(context, className, jar);
            }
            @Override
            public Site getSite() {
                return site;
            }
        };
    }

    /** Create a new PropertiesLoader for the given resource name/path and load it into the given properties.
     * @param siteCxt the SiteContext that will tell us which site we are dealing with.
     * @param resource the resource name/path.
     * @param properties the properties to hold the individual properties loaded.
     * @return the new PropertiesLoader to use.
     **/
    public static PropertiesLoader newPropertiesLoader(
            final SiteContext siteCxt,
            final String resource,
            final Properties properties) {

        final PropertiesLoader pl = new UrlResourceLoader(siteCxt);
        pl.init(resource, properties);
        return pl;
    }

    /** Create a new DocumentLoader for the given resource name/path and load it with the given DocumentBuilder.
     * @param siteCxt the SiteContext that will tell us which site we are dealing with.
     * @param resource the resource name/path.
     * @param builder the DocumentBuilder to build the DOM resource with.
     * @return the new DocumentLoader to use.
     **/
    public static DocumentLoader newDocumentLoader(
            final SiteContext siteCxt,
            final String resource,
            final DocumentBuilder builder) {

        final DocumentLoader dl = new UrlResourceLoader(siteCxt);
        builder.setEntityResolver(new LocalEntityResolver());
        dl.init(resource, builder);
        return dl;
    }

    /**
     * Creates new BytecodeLoader for the given site and resource.
     *
     * @param siteCxt context telling us which site to use.
     * @param resource the class to load bytecode for.
     * @return a bytecode loader for resource.
     */
    public static BytecodeLoader newBytecodeLoader(
            final SiteContext siteCxt,
            final String resource,
            final String jar) {

        final BytecodeLoader bcLoader = new UrlResourceLoader(siteCxt);
        bcLoader.initBytecodeLoader(resource, jar);
        return bcLoader;
    }

    /** Calls doesUrlExist(url, "localhost") **/
    public static boolean doesUrlExist(final URL url){
        return doesUrlExist(url, "localhost");
    }

    /** Checks that the url (requested with the given host header) exists.
     *
     * @param url the url whom's existence is checked
     * @param hostHeader  the host header to use on the request (typically localhost)
     * @return true if in existence
     **/
    public static boolean doesUrlExist(final URL url, final String hostHeader){

        boolean success = false;

        try{
            success = (Boolean)PRESENCE_CACHE.getFromCache(url.toString(), REFRESH_PERIOD);

        }catch(NeedsRefreshException nre){

            boolean updatedCache = false;
            try {

                success = (null != hostHeader
                        ? HTTPClient.instance(url, hostHeader)
                        : HTTPClient.instance(url) )
                        .exists("");

                LOG.trace(DEBUG_CHECKING_EXISTANCE_OF + url + " is " + success);

                PRESENCE_CACHE.putInCache(url.toString(), success);
                updatedCache = true;

            } catch (NullPointerException e) {
                LOG.debug(url.toString(), e);

            } catch (SocketTimeoutException ste) {
                LOG.debug(url.toString() + '\n' + ste);

            } catch (IOException e) {
                LOG.warn(url.toString(), e);

            }  finally  {
                if(!updatedCache){
                    PRESENCE_CACHE.cancelUpdate(url.toString());
                }
            }
        }

        return success;
    }

    // Constructors --------------------------------------------------

    /**
     */
    protected UrlResourceLoader(final SiteContext cxt) {
        super(cxt);
    }


    // Public --------------------------------------------------------


    @Override
    public boolean urlExists(final URL url) {

        return doesUrlExist(url);
    }


    // Z implementation ----------------------------------------------

    // Y overrides ---------------------------------------------------

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------

    @Override
    protected URL getResource(final Site site) {
        return getURL(getResource(), site);
    }

    private static String getResourceDirectory(final String resource) {
        if (resource.contains("jar!")) {
            return "lib/";
        } else if (resource.endsWith(".class")) {
            return "classes/";
        } else if (resource.endsWith(".jsp")) {
            return "jsp/";
        } else {
            return "conf/";
        }
    }

    public static URL getURL(final String resource, final Site site) {

        final String jarScheme = resource.contains("jar!") ? "jar:" : "";

        try {
            return new URL(jarScheme + "http://"
                    + site.getName()
                    + site.getConfigContext()
                    + getResourceDirectory(resource)
                    + resource);
        } catch (MalformedURLException ex) {
            throw new ResourceLoadException("Read Configuration from " + resource, ex);
        }
    }

    @Override
    protected InputStream getInputStreamFor(final URL url) {

        HTTPClient client = null;
        try {
            client = HTTPClient.instance(url, "localhost");
            return client.getBufferedStream("");

        }catch (IOException ex) {
            throw new ResourceLoadException(readResourceDebug(url), client.interceptIOException(ex));
        }


    }

    public static String getHostHeader(final String resource){
        return resource.substring(7,resource.indexOf('/',8));
    }

    @Override
    protected final String readResourceDebug(final URL url){

        return "Read Configuration from " + url;
    }

    // Private -------------------------------------------------------

    // Inner classes -------------------------------------------------
}