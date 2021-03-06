/* Copyright (2005-2012) Schibsted ASA
 * This file is part of Possom.
 *
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
 */
package no.sesat.search.query.token;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import no.sesat.commons.ioc.ContextWrapper;
import no.sesat.search.site.config.DocumentLoader;
import no.sesat.search.site.Site;
import no.sesat.search.site.SiteContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import no.sesat.search.site.SiteKeyedFactoryInstantiationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Responsible for loading and serving all the Regular Expression Token Evaluators.
 * These regular expression patterns come from the configuration file SearchConstants.REGEXP_EVALUATOR_XMLFILE.
 *
 * RegExpEvaluator's are re-used across queries so to cache the compiled patterns.
 *
 * @version <tt>$Id$</tt>
 */
public final class RegExpEvaluatorFactory extends AbstractEvaluatorFactory{

    private static final Logger LOG = Logger.getLogger(RegExpEvaluatorFactory.class);

    private static final String ERR_DOC_BUILDER_CREATION
            = "Failed to DocumentBuilderFactory.newInstance().newDocumentBuilder()";

    /** General properties to all regular expressions configured. **/
    private static final int REG_EXP_OPTIONS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    /** The name of the file where regular expressions for each TokenPredicate will be configured. **/
    public static final String REGEXP_EVALUATOR_XMLFILE = "RegularExpressionEvaluators.xml";

    // TODO this will leak when sites are redeploy without Possom being restarted.
    private static final Map<Site,Map<TokenPredicate,RegExpTokenEvaluator>> EVALUATORS
            = new HashMap<Site,Map<TokenPredicate,RegExpTokenEvaluator>>();
    private static final ReentrantReadWriteLock EVALUATORS_LOCK = new ReentrantReadWriteLock();

    public RegExpEvaluatorFactory(final Context cxt)
            throws SiteKeyedFactoryInstantiationException {

        super(cxt);
        try{
            init(cxt);

        }catch(ParserConfigurationException pce){
            throw new SiteKeyedFactoryInstantiationException(ERR_DOC_BUILDER_CREATION, pce);
        }
    }

    /** Loads the resource SearchConstants.REGEXP_EVALUATOR_XMLFILE containing all regular expression patterns
     *   for all the RegExpTokenEvaluators we will be using.
     */
    private static void init(final Context cxt) throws ParserConfigurationException {

        final Site site = cxt.getSite();
        final Site parent = site.getParent();
        final boolean parentUninitialised;

        try{
            EVALUATORS_LOCK.readLock().lock();

            // initialise the parent site's configuration
            parentUninitialised = (null != parent && null == EVALUATORS.get(parent));

        }finally{
            EVALUATORS_LOCK.readLock().unlock();
        }

        if(parentUninitialised){
            init(ContextWrapper.wrap(
                    AbstractEvaluatorFactory.Context.class,
                    parent.getSiteContext(),
                    cxt
                ));
        }

        if(null == EVALUATORS.get(site)){

            try{
                EVALUATORS_LOCK.writeLock().lock();

                    // create map entry for this site
                    EVALUATORS.put(site, new HashMap<TokenPredicate,RegExpTokenEvaluator>());


                    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setValidating(false);
                    final DocumentBuilder builder = factory.newDocumentBuilder();
                    final DocumentLoader loader
                            = cxt.newDocumentLoader(cxt, REGEXP_EVALUATOR_XMLFILE, builder);

                    loader.abut();
                    LOG.info("Parsing " + REGEXP_EVALUATOR_XMLFILE + " started");
                    final Document doc = loader.getDocument();

                    assert null != doc : "No document loaded for " + site.getName();

                    final Element root = doc.getDocumentElement();
                    if(null != root){
                        final NodeList evaluators = root.getElementsByTagName("evaluator");
                        for (int i = 0; i < evaluators.getLength(); ++i) {

                            final Element evaluator = (Element) evaluators.item(i);

                            final String tokenName = evaluator.getAttribute("token");
                            LOG.info(" ->evaluator@token: " + tokenName);

                            TokenPredicate token;
                            try{
                                token = TokenPredicateUtility.getTokenPredicate(tokenName);

                            }catch(IllegalArgumentException iae){
                                LOG.debug(tokenName + " does not exist. Will create it. Underlying exception was " + iae);
                                token = TokenPredicateUtility.createAnonymousTokenPredicate(
                                        tokenName);
                            }

                            final boolean queryDep = Boolean.parseBoolean(evaluator.getAttribute("query-dependant"));
                            LOG.info(" ->evaluator@query-dependant: " + queryDep);

                            final Collection<Pattern> compiled = new ArrayList<Pattern>();

                            final NodeList patterns = evaluator.getElementsByTagName("pattern");
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
                            EVALUATORS.get(site).put(token, regExpTokenEvaluator);

                        }
                    }
                    LOG.info("Parsing " + REGEXP_EVALUATOR_XMLFILE + " finished");

            }finally{
                EVALUATORS_LOCK.writeLock().unlock();
            }
        }
    }

    public TokenEvaluator getEvaluator(final TokenPredicate token) throws EvaluationException {

        TokenEvaluator result;
        final Context cxt = getContext();

        try{
            EVALUATORS_LOCK.readLock().lock();

            result = EVALUATORS.get(cxt.getSite()).get(token);

        }finally{
            EVALUATORS_LOCK.readLock().unlock();
        }

        if(result == null && null != cxt.getSite().getParent()){

            result = instanceOf(ContextWrapper.wrap(
                    Context.class,
                    cxt.getSite().getParent().getSiteContext(),
                    cxt
                )).getEvaluator(token);
        }
        if(null == result || TokenEvaluationEngineImpl.ALWAYS_FALSE_EVALUATOR == result){
            // if we cannot find an evaulator, then always fail evaluation.
            //  Rather than encourage a NullPointerException
            result = TokenEvaluationEngineImpl.ALWAYS_FALSE_EVALUATOR;
        }
        return result;
    }

}
