/* Copyright (2007) Schibsted Søk AS
 *   This file is part of SESAT.
 *
 *   SESAT is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   SESAT is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with SESAT.  If not, see <http://www.gnu.org/licenses/>.
 *
 * ImportPublish.java
 *
 * Created on 12 March 2007, 15:38
 *
 */

package no.sesat.search.view;

import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import no.sesat.search.datamodel.DataModel;
import no.sesat.search.http.HTTPClient;
import no.sesat.search.site.config.SiteConfiguration;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.apache.log4j.Logger;

/**
 * General support to import page fragments from publishing system.
 * Caches content on a one minute basis to reduce outbound socket connections.
 *
 * @author <a href="mailto:mick@semb.wever.org">Mck</a>
 * @version <tt>$Id$</tt>
 */
public final class ImportPublish {
    
    // Constants -----------------------------------------------------

    private static final GeneralCacheAdministrator CACHE = new GeneralCacheAdministrator();   
    private static final int REFRESH_PERIOD = 60; // one minute

    private static final Logger LOG = Logger.getLogger(ImportPublish.class);

    // Attributes ----------------------------------------------------
    
    // Static --------------------------------------------------------
    
    /**
     * 
     * @param page 
     * @param datamodel 
     * @param out 
     * @throws java.io.IOException 
     */
    public static String importPage(
            final String page, 
            final DataModel datamodel) throws IOException{
        
        final Properties props = datamodel.getSite().getSiteConfiguration().getProperties();

        final String fileExtension = page.endsWith(".xml") ? "" : ".html"; 
        
        final URL u = new URL(props.getProperty(SiteConfiguration.PUBLISH_SYSTEM_URL) + page + fileExtension);
        final String physicalHost = props.getProperty(SiteConfiguration.PUBLISH_PHYSICAL_HOST);
        final String cacheKey = '[' + physicalHost + ']' + u.toString();
        
        String content = "";
        try{
            content = (String) CACHE.getFromCache(cacheKey, REFRESH_PERIOD);
        
        } catch (NeedsRefreshException nre) {
        
            boolean updatedCache = false;
            final HTTPClient client = HTTPClient.instance(u, physicalHost);
            
            try{
                final BufferedReader reader = client.getBufferedReader("");
                final StringBuilder builder = new StringBuilder();
          
                for(String line = reader.readLine(); line != null; line = reader.readLine()){
                    builder.append(line);
                    builder.append('\n');
                }
                content = builder.toString();
                CACHE.putInCache(cacheKey, content);
                updatedCache = true;

            }catch(IOException ioe){
                content = (String) nre.getCacheContent();
                throw client.interceptIOException(ioe);
                
            }finally{
                if(!updatedCache){ 
                    CACHE.cancelUpdate(cacheKey);
                }
            }
        }
        return content;
    }
         
    /**
     * 
     * @param page 
     * @param datamodel 
     * @param out 
     * @throws java.io.IOException 
     */
    public static Document importXml(
            final String page, 
            final DataModel datamodel) throws IOException, ParserConfigurationException, SAXException{ 
        
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        try {
            return builder.parse(new InputSource(new StringReader(importPage(page, datamodel))));
        } catch (SAXException e) {
            LOG.error("XML Parse exception. ", e);
            return builder.newDocument();
        }
    }
    
    // Constructors --------------------------------------------------
    
    /** Creates a new instance of NewClass */
    private ImportPublish() {
    }
    
    // Public --------------------------------------------------------
    
    
    // Package protected ---------------------------------------------
    
    // Protected -----------------------------------------------------
    
    // Private -------------------------------------------------------
    
    // Inner classes -------------------------------------------------
    
}
