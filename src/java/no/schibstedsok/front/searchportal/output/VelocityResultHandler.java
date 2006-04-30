// Copyright (2006) Schibsted Søk AS
package no.schibstedsok.front.searchportal.output;

import java.io.IOException;
import java.util.Properties;
import no.geodata.maputil.CoordHelper;
import no.schibstedsok.common.ioc.ContextWrapper;
import no.schibstedsok.front.searchportal.InfrastructureException;
import no.schibstedsok.front.searchportal.configuration.SearchConfiguration;
import no.schibstedsok.front.searchportal.configuration.SiteConfiguration;
import no.schibstedsok.front.searchportal.configuration.loader.PropertiesLoader;
import no.schibstedsok.front.searchportal.view.i18n.TextMessages;
import no.schibstedsok.front.searchportal.query.run.RunningQuery;
import no.schibstedsok.front.searchportal.result.Decoder;
import no.schibstedsok.front.searchportal.result.Linkpulse;
import no.schibstedsok.front.searchportal.result.handler.ResultHandler;
import no.schibstedsok.front.searchportal.util.SearchConstants;
import no.schibstedsok.front.searchportal.velocity.VelocityEngineFactory;
import no.schibstedsok.front.searchportal.site.Site;
import no.schibstedsok.front.searchportal.util.PagingDisplayHelper;
import no.schibstedsok.front.searchportal.util.TradeDoubler;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.generic.MathTool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.StringWriter;
import java.io.Writer;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.net.URLEncoder;

/** Handles the populating the velocity contexts.
 * Strictly view domain.
 *
 * @author <a href="mailto:magnus.eklund@schibsted.no">Magnus Eklund</a>
 * @version <tt>$Revision$</tt>
 */
public final class VelocityResultHandler implements ResultHandler {

    private static final String PUBLISH_URL = "publishSystemBaseURL";
    private static final String PUBLISH_HOST = "publishSystemHostHeader";

    private static final Logger LOG = Logger.getLogger(VelocityResultHandler.class);

    private static final String INFO_TEMPLATE_NOT_FOUND = "Could not find template ";
    private static final String ERR_IN_TEMPLATE = "Error parsing template ";
    private static final String ERR_GETTING_TEMPLATE = "Error getting template ";
    private static final String ERR_NP_WRITING_TO_STREAM = "Possible client cancelled request. (NullPointerException writing to response's stream).";

    public static VelocityEngine getEngine(final Site site){

        return VelocityEngineFactory.valueOf(new VelocityEngineFactory.Context() {
                public Site getSite() {
                    return site;
                }
            });
    }

    public static Template getTemplate(
            final VelocityEngine engine,
            final Site site,
            final String templateName){

        final String templateUrl = site.getTemplateDir() + "/" + templateName + ".vm";
        try {
            return  engine.getTemplate(templateUrl);

        } catch (ResourceNotFoundException ex) {
            // expected possible behaviour
            LOG.debug(INFO_TEMPLATE_NOT_FOUND + templateUrl);

        } catch (ParseErrorException ex) {
            LOG.error(ERR_IN_TEMPLATE + templateUrl, ex);
            throw new InfrastructureException(ex);

        } catch (Exception ex) {
            LOG.error(ERR_GETTING_TEMPLATE + templateUrl, ex);
            throw new InfrastructureException(ex);
        }
        return null;
    }

    public static VelocityContext newContextInstance(final VelocityEngine engine){
        final VelocityContext context = new VelocityContext();
        final Site site = (Site) engine.getProperty(Site.NAME_KEY);
        final Site fallbackSite = (Site) engine.getProperty("site.fallback");
        // site
        context.put(Site.NAME_KEY, site);
        context.put("fallbackSite", fallbackSite);
        context.put("locale", site.getLocale());
        // publishing system
        context.put(PUBLISH_URL, engine.getProperty(SearchConstants.PUBLISH_SYSTEM_URL));
        context.put(PUBLISH_HOST, engine.getProperty(SearchConstants.PUBLISH_SYSTEM_HOST));
        // coord helper
        context.put("coordHelper", new CoordHelper());
        // properties
        context.put("linkpulse", new Linkpulse(SiteConfiguration.valueOf(site).getProperties()));
        // decoder
        context.put("decoder", new Decoder());
        // math tool
        context.put("math", new MathTool());
        return context;
    }

    public void handleResult(final Context cxt, final Map parameters) {

        // Skip this result handler if xml is wanted.
        final String[] xmlParam = (String[]) parameters.get("xml");
        if (xmlParam != null && xmlParam[0].equals("yes")) {
            return;
        }

        LOG.trace("handleResult()");

        // This requirement of the users of this class to send the web stuff
        // as parameters is a bit too implicit...

        final HttpServletRequest request = (HttpServletRequest) parameters.get("request");
        final HttpServletResponse response = (HttpServletResponse) parameters.get("response");

        if (request == null || response == null) {
            throw new IllegalStateException("Both request and response must be set in the parameters");
        }

        // write to a separate writer first for threading reasons
        final Writer w = new StringWriter();
        final SearchConfiguration searchConfiguration = cxt.getSearchResult().getSearchCommand().getSearchConfiguration();

            if (LOG.isDebugEnabled()) {
                LOG.debug("handleResult: Looking for template: " + searchConfiguration + searchConfiguration.getName() + ".vm");
            }

            final Site site = cxt.getSite();
            final VelocityEngine engine = getEngine(site);
            final Template template = getTemplate(engine, site, searchConfiguration.getName());

            if (LOG.isDebugEnabled()) {
                LOG.debug("handleResult: Created Template=" + template.getName());
            }

            final VelocityContext context = newContextInstance(engine);
            populateVelocityContext(context, cxt, request, response);

            try {

                template.merge(context, w);
                response.getWriter().write(w.toString());

            } catch (MethodInvocationException ex) {
                throw new InfrastructureException(ex);

            } catch (ResourceNotFoundException ex) {
                throw new InfrastructureException(ex);

            } catch (ParseErrorException ex) {
                throw new InfrastructureException(ex);

            } catch (IOException ex) {
                throw new InfrastructureException(ex);

            } catch (NullPointerException ex) {
                //  at com.opensymphony.module.sitemesh.filter.RoutablePrintWriter.write(RoutablePrintWriter.java:132)

                // indicates an error in the underlying RoutablePrintWriter stream
                //  typically the client has closed the connection
                LOG.warn(ERR_NP_WRITING_TO_STREAM);

            } catch (Exception ex) {
                throw new InfrastructureException(ex);

            }

    }

    protected void populateVelocityContext(final VelocityContext context,
                                           final Context cxt,
                                           final HttpServletRequest request,
                                           final HttpServletResponse response) {

        LOG.trace("populateVelocityContext()");

        String queryString = cxt.getQuery().getQueryString();

        String queryStringURLEncoded = null;

        try {
            queryStringURLEncoded = URLEncoder.encode(queryString, "UTF-8");
            queryString = StringEscapeUtils.escapeHtml(queryString);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        context.put("result", cxt.getSearchResult());
        context.put("request", request);
        context.put("response", response);
        context.put("query", queryStringURLEncoded);
        context.put("globalSearchTips", ((RunningQuery) request.getAttribute("query")).getGlobalSearchTips());
        context.put("command", cxt.getSearchResult().getSearchCommand());
        context.put("queryHTMLEscaped", queryString);
        
        context.put("text", TextMessages.valueOf(ContextWrapper.wrap(TextMessages.Context.class,cxt)));
        context.put("currentTab", cxt.getSearchTab());
        
        context.put("contextPath", request.getContextPath());
        context.put("hashGenerator", request.getAttribute("hashGenerator"));
        context.put("runningQuery", cxt.getSearchResult().getSearchCommand().getRunningQuery());
        
        context.put("tradedoubler", new TradeDoubler(request));

        final SearchConfiguration config = cxt.getSearchResult().getSearchCommand().getSearchConfiguration();

        if (config.isPagingEnabled()) {
            final PagingDisplayHelper pager = new PagingDisplayHelper(cxt.getSearchResult().getHitCount(), config.getResultsToReturn(), 10);
            pager.setCurrentOffset(cxt.getSearchResult().getSearchCommand().getRunningQuery().getOffset());
            context.put("pager", pager);
        }


    }
}
