/* Copyright (2005-2007) Schibsted Søk AS
 * This file is part of SESAT.
 * You can use, redistribute, and/or modify it, under the terms of the SESAT License.
 * You should have received a copy of the SESAT License along with this program.  
 * If not, see https://dev.sesat.no/confluence/display/SESAT/SESAT+License
 */
package no.sesat.search.query.token;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import no.schibstedsok.commons.ioc.BaseContext;
import no.schibstedsok.commons.ioc.ContextWrapper;
import no.sesat.search.site.config.DocumentLoader;
import no.sesat.search.site.config.ResourceContext;
import no.sesat.search.site.Site;
import no.sesat.search.site.SiteContext;
import no.sesat.search.site.SiteKeyedFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Responsible for loading and serving all the Regular Expression Token Evaluators.
 * These regular expression patterns come from the configuration file SearchConstants.REGEXP_EVALUATOR_XMLFILE.
 *
 * @author <a href="mailto:magnus.eklund@schibsted.no">Magnus Eklund</a>
 * @author <a href="mailto:mick@wever.org">Michael Semb Wever</a>
 * @version <tt>$Id$</tt>
 */
public final class RegExpEvaluatorFactory implements SiteKeyedFactory{

    /**
     * The context the RegExpEvaluatorFactory must work against.
     */
    public interface Context extends BaseContext, ResourceContext, SiteContext {
    }

    private static final Logger LOG = Logger.getLogger(RegExpEvaluatorFactory.class);

    private static final String ERR_MUST_USE_CONTEXT_CONSTRUCTOR = "Must use constructor that supplies a context!";
    private static final String ERR_DOC_BUILDER_CREATION
            = "Failed to DocumentBuilderFactory.newInstance().newDocumentBuilder()";

    /** TODO comment me. **/
    static final int REG_EXP_OPTIONS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    /** TODO comment me. **/
    public static final String REGEXP_EVALUATOR_XMLFILE = "RegularExpressionEvaluators.xml";

    /**
     * No need to synchronise this. Worse that can happen is multiple identical INSTANCES are created at the same
     * time. But only one will persist in the map.
     *  There might be a reason to synchronise to avoid the multiple calls to the search-front-config context to obtain
     * the resources to improve the performance. But I doubt this would gain much, if anything at all.
     */
    private static final Map<Site,RegExpEvaluatorFactory> INSTANCES = new HashMap<Site,RegExpEvaluatorFactory>();
    private static final ReentrantReadWriteLock INSTANCES_LOCK = new ReentrantReadWriteLock();

    private final Context context;
    private final DocumentLoader loader;
    private volatile boolean init = false;

    private Map<TokenPredicate,RegExpTokenEvaluator> regExpEvaluators
            = new HashMap<TokenPredicate,RegExpTokenEvaluator>();

    /**
     * Illegal Constructor. Must use RegExpEvaluatorFactory(SiteContext).
     */
    private RegExpEvaluatorFactory() {
        throw new IllegalArgumentException(ERR_MUST_USE_CONTEXT_CONSTRUCTOR);
    }


    private RegExpEvaluatorFactory(final Context cxt)
            throws ParserConfigurationException {

        try{
            INSTANCES_LOCK.writeLock().lock();
            context = cxt;
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            final DocumentBuilder builder = factory.newDocumentBuilder();
            loader = context.newDocumentLoader(cxt, REGEXP_EVALUATOR_XMLFILE, builder);

            INSTANCES.put(context.getSite(), this);
            init();
            
        }finally{
            INSTANCES_LOCK.writeLock().unlock();
        }
    }

    /** Loads the resource SearchConstants.REGEXP_EVALUATOR_XMLFILE containing all regular expression patterns
     *   for all the RegExpTokenEvaluators we will be using.
     *  Keeps thread-safe state so that this method can be called multiple times with the resource only loaded once.
     *  ( Truth is that it may run more than once, in parallel during the first call, but because regExpEvaluators uses
     *    the TokenPredicates as keys there will not be differing states.
     *    Just a small performance lost during this first call. )
     */
    private void init() {

        try{
            INSTANCES_LOCK.writeLock().lock();
            if (!init) {
                loader.abut();
                LOG.info("Parsing " + REGEXP_EVALUATOR_XMLFILE + " started");
                final Document doc = loader.getDocument();
                
                assert null != doc : "No document loaded for " + context.getSite().getName();
                
                final Element root = doc.getDocumentElement();
                if(null != root){
                    final NodeList evaluators = root.getElementsByTagName("evaluator");
                    for (int i = 0; i < evaluators.getLength(); ++i) {

                        final Element evaluator = (Element) evaluators.item(i);

                        final String tokenName = evaluator.getAttribute("token");
                        LOG.info(" ->evaluator@token: " + tokenName);

                        final TokenPredicate token = TokenPredicate.valueOf(tokenName);

                        final boolean queryDep = Boolean.parseBoolean(evaluator.getAttribute("query-dependant"));
                        LOG.info(" ->evaluator@query-dependant: " + queryDep);

                        final Collection compiled = new ArrayList();

                        final NodeList patterns = ((Element) evaluator).getElementsByTagName("pattern");
                        for (int j = 0; j < patterns.getLength(); ++j) {
                            final Element pattern = (Element) patterns.item(j);

                            final String expression = pattern.getFirstChild().getNodeValue();
                            LOG.info(" --->pattern: " + expression);

                            // (^|\s) or ($|\s) is neccessary to avoid matching fragments of words.
                            final String prefix = expression.startsWith("^") ? "" : "(^|\\s)";
                            final String suffix = expression.endsWith("$") ? "" : "(\\:|$|\\s)";
                            // compile pattern
                            final Pattern p = Pattern.compile(prefix + expression + suffix, REG_EXP_OPTIONS);
                            compiled.add(p);
                        }

                        final RegExpTokenEvaluator regExpTokenEvaluator = new RegExpTokenEvaluator(compiled, queryDep);
                        regExpEvaluators.put(token, regExpTokenEvaluator);

                    }
                }
                LOG.info("Parsing " + REGEXP_EVALUATOR_XMLFILE + " finished");
                init = true;
            }
        }finally{
            INSTANCES_LOCK.writeLock().unlock();
        }
    }

    /** Main method to retrieve the correct RegExpEvaluatorFactory to further obtain
     * RegExpTokenEvaluators and StopWordRemover.
     * @param cxt the contextual needs this factory must use to operate.
     * @return RegExpEvaluatorFactory for this site.
     */
    public static RegExpEvaluatorFactory valueOf(final Context cxt) {

        final Site site = cxt.getSite();
        RegExpEvaluatorFactory instance;
        try{
            INSTANCES_LOCK.readLock().lock();
            instance = INSTANCES.get(site);
        }finally{
            INSTANCES_LOCK.readLock().unlock();
        }

        if (instance == null) {
            try {
                instance = new RegExpEvaluatorFactory(cxt);

            } catch (ParserConfigurationException ex) {
                LOG.error(ERR_DOC_BUILDER_CREATION, ex);
            }
        }
        return instance;
    }

    /**
     * If the regular expression is not found in this site's RegularExpressionEvaluators.xml file
     * it will fallback and look in the parent site.
     * @param token the predicate the evaluator is to be used for
     * @return the RegExpTokenEvaluator to use.
     */
    public TokenEvaluator getEvaluator(final TokenPredicate token) {
        
        TokenEvaluator result = regExpEvaluators.get(token);
        if(result == null && null != context.getSite().getParent()){

            result = valueOf(ContextWrapper.wrap(
                    Context.class,
                    new SiteContext(){
                        public Site getSite(){
                            return context.getSite().getParent();
                        }
                    },
                    context
                )).getEvaluator(token);
        }
        if(result == null){
            // if we cannot find an evaulator, then always fail evaluation.
            //  Rather than encourage a NullPointerException
            result = TokenEvaluationEngineImpl.ALWAYS_FALSE_EVALUATOR;
        }
        return result;
    }

    /** TODO comment me. **/
    public boolean remove(final Site site) {

        try{
            INSTANCES_LOCK.writeLock().lock();
            return null != INSTANCES.remove(site);
        }finally{
            INSTANCES_LOCK.writeLock().unlock();
        }
    }

}