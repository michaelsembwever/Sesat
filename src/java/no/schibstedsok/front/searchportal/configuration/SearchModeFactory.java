// Copyright (2006) Schibsted Søk AS
package no.schibstedsok.front.searchportal.configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import no.schibstedsok.common.ioc.BaseContext;
import no.schibstedsok.common.ioc.ContextWrapper;
import no.schibstedsok.front.searchportal.InfrastructureException;
import no.schibstedsok.front.searchportal.configuration.loader.DocumentLoader;
import no.schibstedsok.front.searchportal.configuration.loader.PropertiesLoader;
import no.schibstedsok.front.searchportal.configuration.loader.ResourceContext;
import no.schibstedsok.front.searchportal.configuration.loader.UrlResourceLoader;
import no.schibstedsok.front.searchportal.executor.ParallelSearchCommandExecutor;
import no.schibstedsok.front.searchportal.executor.SearchCommandExecutor;
import no.schibstedsok.front.searchportal.executor.SequentialSearchCommandExecutor;
import no.schibstedsok.front.searchportal.query.transform.AgeFilterTransformer;
import no.schibstedsok.front.searchportal.query.transform.NowQueryTransformer;
import no.schibstedsok.front.searchportal.query.transform.WebTvQueryTransformer;
import no.schibstedsok.front.searchportal.result.handler.CombineNavigatorsHandler;
import no.schibstedsok.front.searchportal.result.handler.DataModelResultHandler;
import no.schibstedsok.front.searchportal.query.transform.TvSearchQueryTransformer;
import no.schibstedsok.front.searchportal.result.handler.FieldEscapeHandler;
import no.schibstedsok.front.searchportal.result.handler.NumberOperationHandler;
import no.schibstedsok.front.searchportal.result.handler.TvSearchSortingHandler;
import no.schibstedsok.front.searchportal.view.output.TextOutputResultHandler;
import no.schibstedsok.front.searchportal.view.output.VelocityResultHandler;
import no.schibstedsok.front.searchportal.view.output.XmlOutputResultHandler;
import no.schibstedsok.front.searchportal.query.transform.ExactTitleMatchTransformer;
import no.schibstedsok.front.searchportal.query.transform.InfopageQueryTransformer;
import no.schibstedsok.front.searchportal.query.transform.NewsTransformer;
import no.schibstedsok.front.searchportal.query.transform.PrefixRemoverTransformer;
import no.schibstedsok.front.searchportal.query.transform.QueryTransformer;
import no.schibstedsok.front.searchportal.query.transform.SimpleSiteSearchTransformer;
import no.schibstedsok.front.searchportal.query.transform.SynonymQueryTransformer;
import no.schibstedsok.front.searchportal.query.transform.TermPrefixTransformer;
import no.schibstedsok.front.searchportal.query.transform.TvQueryTransformer;
import no.schibstedsok.front.searchportal.query.transform.WeatherQueryTransformer;
import no.schibstedsok.front.searchportal.query.transform.WeatherInfopageQueryTransformer;
import no.schibstedsok.front.searchportal.result.handler.AddDocCountModifier;
import no.schibstedsok.front.searchportal.result.handler.AgeCalculatorResultHandler;
import no.schibstedsok.front.searchportal.result.handler.CategorySplitter;
import no.schibstedsok.front.searchportal.result.handler.ContentSourceCollector;
import no.schibstedsok.front.searchportal.result.handler.DiscardDuplicatesResultHandler;
import no.schibstedsok.front.searchportal.result.handler.DiscardOldNewsResultHandler;
import no.schibstedsok.front.searchportal.result.handler.FieldChooser;
import no.schibstedsok.front.searchportal.result.handler.FindFileFormat;
import no.schibstedsok.front.searchportal.result.handler.ForecastWindHandler;
import no.schibstedsok.front.searchportal.result.handler.ImageHelper;
import no.schibstedsok.front.searchportal.result.handler.MapCoordHandler;
import no.schibstedsok.front.searchportal.result.handler.MultiValuedFieldCollector;
import no.schibstedsok.front.searchportal.result.handler.PhoneNumberChooser;
import no.schibstedsok.front.searchportal.result.handler.PhoneNumberFormatter;
import no.schibstedsok.front.searchportal.result.handler.ResultHandler;
import no.schibstedsok.front.searchportal.result.handler.SpellingSuggestionChooser;
import no.schibstedsok.front.searchportal.result.handler.ForecastDateHandler;
import no.schibstedsok.front.searchportal.result.handler.SumFastModifiers;
import no.schibstedsok.front.searchportal.result.handler.DateFormatHandler;
import no.schibstedsok.front.searchportal.result.handler.WeatherCelciusHandler;
import no.schibstedsok.front.searchportal.result.handler.WeatherDateHandler;
import no.schibstedsok.front.searchportal.site.Site;
import no.schibstedsok.front.searchportal.site.SiteContext;
import no.schibstedsok.front.searchportal.site.SiteKeyedFactory;
import no.schibstedsok.front.searchportal.util.config.AbstractDocumentFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author <a href="mailto:mick@wever.org>mick</a>
 * @version <tt>$Id$</tt>
 */
public final class SearchModeFactory extends AbstractDocumentFactory implements SiteKeyedFactory{

    /**
     * The context any SearchModeFactory must work against. *
     */
    public interface Context extends BaseContext, ResourceContext, SiteContext {}

   // Constants -----------------------------------------------------

    private static final Map<Site, SearchModeFactory> INSTANCES = new HashMap<Site,SearchModeFactory>();
    private static final ReentrantReadWriteLock INSTANCES_LOCK = new ReentrantReadWriteLock();

    public static final String MODES_XMLFILE = "modes.xml";


    private static final Map<SearchMode,Map<String,SearchConfiguration>> COMMANDS
            = new HashMap<SearchMode,Map<String,SearchConfiguration>>();
    private static final ReentrantReadWriteLock COMMANDS_LOCK = new ReentrantReadWriteLock();

    private static final Logger LOG = Logger.getLogger(SearchModeFactory.class);
    private static final String ERR_DOC_BUILDER_CREATION
            = "Failed to DocumentBuilderFactory.newInstance().newDocumentBuilder()";
    private static final String ERR_MISSING_IMPLEMENTATION = "Missing implementation case in CommandTypes";
    private static final String ERR_ONLY_ONE_CHILD_NAVIGATOR_ALLOWED
            = "Each FastNavigator is only allowed to have one child. Parent was ";
    private static final String ERR_FAST_EPS_QR_SERVER = "" +
            "Query server adressen cannot contain the scheme (http://): ";
    private static final String INFO_PARSING_MODE = "Parsing mode ";
    private static final String INFO_PARSING_CONFIGURATION = " Parsing configuration ";
    private static final String INFO_PARSING_NAVIGATOR = "  Parsing navigator ";
    private static final String INFO_PARSING_RESULT_HANDLER = "  Parsing result handler ";
    private static final String INFO_PARSING_QUERY_TRANSFORMER = "  Parsing query transformer ";
    private static final String DEBUG_PARSED_PROPERTY = "  Property property ";

   // Attributes ----------------------------------------------------

    private final Map<String,SearchMode> modes = new HashMap<String,SearchMode>();
    private final ReentrantReadWriteLock modesLock = new ReentrantReadWriteLock();

    private final DocumentLoader loader;
    private final Context context;

    private String templatePrefix;

   // Static --------------------------------------------------------

    public static SearchModeFactory valueOf(final Context cxt) {

        final Site site = cxt.getSite();

        INSTANCES_LOCK.readLock().lock();
        SearchModeFactory instance = INSTANCES.get(site);
        INSTANCES_LOCK.readLock().unlock();

        if (instance == null) {
            try {
                instance = new SearchModeFactory(cxt);
            } catch (ParserConfigurationException ex) {
                LOG.error(ERR_DOC_BUILDER_CREATION,ex);
            }
        }
        return instance;
    }

    public boolean remove(final Site site){

        try{
            INSTANCES_LOCK.writeLock().lock();
            return null != INSTANCES.remove(site);
        }finally{
            INSTANCES_LOCK.writeLock().unlock();
        }
    }

   // Constructors --------------------------------------------------

    /** Creates a new instance of ModeFactoryImpl */
    private SearchModeFactory(final Context cxt)
            throws ParserConfigurationException {

        LOG.trace("ModeFactory(cxt)");
        INSTANCES_LOCK.writeLock().lock();

        context = cxt;

        // configuration files
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        loader = context.newDocumentLoader(MODES_XMLFILE, builder);

        // update the store of factories
        INSTANCES.put(context.getSite(), this);
        // start initialisation
        init();
        INSTANCES_LOCK.writeLock().unlock();

    }

   // Public --------------------------------------------------------

    public SearchMode getMode(final String id){

        LOG.trace("getMode(" + id + ")");

        SearchMode mode = getModeImpl(id);
        if(mode == null && id != null && id.length() >0 && context.getSite().getParent() != null){
            // not found in this site's modes.xml. look in parent's site.
            final SearchModeFactory factory = valueOf(ContextWrapper.wrap(
                    Context.class,
                    new SiteContext(){
                        public Site getSite(){
                            return context.getSite().getParent();
                        }
                        public PropertiesLoader newPropertiesLoader(final String resource, final Properties properties) {
                            return UrlResourceLoader.newPropertiesLoader(this, resource, properties);
                        }
                        public DocumentLoader newDocumentLoader(final String resource, final DocumentBuilder builder) {
                            return UrlResourceLoader.newDocumentLoader(this, resource, builder);
                        }
                    },
                    context
                ));
            mode = factory.getMode(id);
        }
        return mode;
    }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

    private void init(){

        loader.abut();
        LOG.debug("Parsing " + MODES_XMLFILE + " started");
        final Document doc = loader.getDocument();
        final Element root = doc.getDocumentElement();

        templatePrefix = root.getAttribute("template-prefix");

        // loop through modes.
        final NodeList modeList = root.getElementsByTagName("mode");
        for (int i = 0; i < modeList.getLength(); ++i) {
            final Element modeE = (Element) modeList.item(i);
            final String id = modeE.getAttribute("id");
            LOG.info(INFO_PARSING_MODE + modeE.getLocalName() + " " + id);
            final SearchMode inherit = getMode(modeE.getAttribute("inherit"));
            final SearchMode mode = new SearchMode(inherit);
            mode.setId(id);
            mode.setExecutor(parseExecutor(modeE.getAttribute("executor"),
                    inherit != null ? inherit.getExecutor() : new SequentialSearchCommandExecutor()));
            mode.setQueryAnalysisEnabled(parseBoolean(modeE.getAttribute("analysis"),
                    inherit != null ? inherit.isQueryAnalysisEnabled() : false));

            // setup new commands list for this mode
            final Map<String,SearchConfiguration> modesCommands = new HashMap<String,SearchConfiguration>();
            try{
                COMMANDS_LOCK.writeLock().lock();
                COMMANDS.put(mode, modesCommands);
            }finally{
                COMMANDS_LOCK.writeLock().unlock();
            }

            // now loop through commands
            for(CommandTypes commandType : CommandTypes.values()){
                final NodeList commandsList = modeE.getElementsByTagName(commandType.getXmlName());
                for (int j = 0; j < commandsList.getLength(); ++j) {
                    final Element commandE = (Element) commandsList.item(j);
                    final SearchConfiguration sc = commandType.parseSearchConfiguration(context, commandE, mode);
                    modesCommands.put(sc.getName(), sc);
                    mode.addSearchConfiguration(sc);
                }
            }
            // add mode
            try{
                modesLock.writeLock().lock();
                modes.put(id, mode);
            }finally{
                modesLock.writeLock().unlock();
            }
        }

        // finished
        LOG.debug("Parsing " + MODES_XMLFILE + " finished");

    }

    private static SearchCommandExecutor parseExecutor(final String name, final SearchCommandExecutor def){

        if("parallel".equalsIgnoreCase(name)){
            return new ParallelSearchCommandExecutor();
        }else if("sequential".equalsIgnoreCase(name)){
            return new SequentialSearchCommandExecutor();
        }
        return def;
    }

    private SearchMode getModeImpl(final String id){

        try{
            modesLock.readLock().lock();
            return modes.get(id);

        }finally{
            modesLock.readLock().unlock();
        }
    }

   // Inner classes -------------------------------------------------

    private enum CommandTypes {
        COMMAND (AbstractSearchConfiguration.class),
        ADVANCED_FAST_COMMAND (AdvancedFastSearchConfiguration.class),
        BLENDING_NEWS_COMMAND (BlendingNewsSearchConfiguration.class),
        FAST_COMMAND (FastSearchConfiguration.class),
        HITTA_COMMAND (HittaSearchConfiguration.class),
        MATH_COMMAND (MathExpressionSearchConfiguration.class),
        MOBILE_COMMAND(MobileSearchConfiguration.class),
        NEWS_COMMAND (NewsSearchConfiguration.class),
        OVERTURE_PPC_COMMAND(OverturePPCSearchConfiguration.class),
        PICTURE_COMMAND(PicSearchConfiguration.class),
        SENSIS_COMMAND(SensisSearchConfiguration.class),
        STATIC_COMMAND(StaticSearchConfiguration.class),
        STOCK_COMMAND(StockSearchConfiguration.class),
        STORMWEATHER_COMMAND(StormWeatherSearchConfiguration.class),
        TVSEARCH_COMMAND(TvSearchConfiguration.class),
        YAHOO_IDP_COMMAND(YahooIdpSearchConfiguration.class),
        YELLOWPAGES_COMMAND(YellowSearchConfiguration.class),
        WEB_COMMAND(WebSearchConfiguration.class),
        WHITEPAGES_COMMAND(WhiteSearchConfiguration.class),
        DAILY_WORD_COMMAND(DailyWordConfiguration.class);
        

        private final Class<? extends SearchConfiguration> clazz;
        private final String xmlName;


        CommandTypes(final Class<? extends SearchConfiguration> clazz){
            this.clazz = clazz;
            xmlName = name().replaceAll("_","-").toLowerCase();
        }

        public String getXmlName(){
            return xmlName;
        }

        public SearchConfiguration parseSearchConfiguration(
                final Context cxt,
                final Element commandE,
                final SearchMode mode){

            final SearchConfiguration inherit = findParent(commandE.getAttribute("inherit"), mode);
            final String id = commandE.getAttribute("id");
            LOG.info(INFO_PARSING_CONFIGURATION + commandE.getLocalName() + " " + id);

            try {
                final Constructor<? extends SearchConfiguration> con;
                con = clazz.getConstructor(SearchConfiguration.class);
                final SearchConfiguration sc;
                sc = con.newInstance(inherit);
                sc.setResultsToReturn(parseInt(commandE.getAttribute("results-to-return"),
                        inherit != null ? inherit.getResultsToReturn() : -1));

                if(sc instanceof AbstractSearchConfiguration){
                    // everything extends AbstractSearchConfiguration
                    final AbstractSearchConfiguration asc = (AbstractSearchConfiguration) sc;
                    final AbstractSearchConfiguration ascInherit = inherit instanceof AbstractSearchConfiguration
                            ? (AbstractSearchConfiguration)inherit
                            : null;

                    asc.setName(id);

                    asc.setAlwaysRunEnabled(parseBoolean(commandE.getAttribute("always-run"),
                            ascInherit != null ? ascInherit.isAlwaysRunEnabled() : false));

                    if(commandE.getAttribute("field-filters").length() >0){
                        final String[] fieldFilters = commandE.getAttribute("field-filters").split(",");
                        for(String fieldFilter : fieldFilters){
                            if(fieldFilter.contains(" AS ")){
                                final String[] ff = fieldFilter.split(" AS ");
                                asc.addFieldFilter(ff[0], ff[1]);
                            }else{
                                asc.addFieldFilter(fieldFilter, fieldFilter);
                            }
                        }
                    }
                    asc.setPagingEnabled(parseBoolean(commandE.getAttribute("paging"),
                            ascInherit != null ? ascInherit.isPagingEnabled() : false));

                    asc.setUseParameterAsQuery(commandE.getAttribute("query-parameter"));

                    if(commandE.getAttribute("result-fields").length() >0){
                        final String[] resultFields = commandE.getAttribute("result-fields").split(",");
                        for(String resultField : resultFields){
                            asc.addResultField(resultField.trim().split(" AS "));
                        }
                    }

                    asc.setStatisticsName(parseString(commandE.getAttribute("statistical-name"),
                            ascInherit != null ? ascInherit.getStatisticsName() : ""));



                }
                if(sc instanceof FastSearchConfiguration){
                    final FastSearchConfiguration fsc = (FastSearchConfiguration) sc;
                    final FastSearchConfiguration fscInherit = inherit instanceof FastSearchConfiguration
                            ? (FastSearchConfiguration)inherit
                            : null;
                    fsc.setClusteringEnabled(parseBoolean(commandE.getAttribute("clustering"),
                            fscInherit != null ? fscInherit.isClusteringEnabled() : false));
                    fsc.setCollapsingEnabled(parseBoolean(commandE.getAttribute("collapsing"),
                            fscInherit != null ? fscInherit.isCollapsingEnabled() : false));
                    //fsc.setCollectionFilterString(commandE.getAttribute("collection-filter-string")); // FIXME !!
                    if(commandE.getAttribute("collections").length() >0){
                        final String[] collections = commandE.getAttribute("collections").split(",");
                        for(String collection : collections){
                            fsc.addCollection(collection);
                        }
                    }
                    fsc.setFilter(parseString(commandE.getAttribute("filter"),
                            fscInherit != null ? fscInherit.getFilter() : ""));
                    fsc.setIgnoreNavigationEnabled(parseBoolean(commandE.getAttribute("ignore-navigation"),
                            fscInherit != null ? fscInherit.isIgnoreNavigationEnabled() : false));
                    fsc.setOffensiveScoreLimit(parseInt(commandE.getAttribute("offensive-score-limit"),
                            fscInherit != null ? fscInherit.getOffensiveScoreLimit() : -1));
                    fsc.setQtPipeline(parseString(commandE.getAttribute("qt-pipeline"),
                            fscInherit != null ? fscInherit.getQtPipeline() : ""));
                    final String queryServerUrl = commandE.getAttribute("query-server-url");
                    fsc.setQueryServerURL(parseProperty(cxt, queryServerUrl,
                            fscInherit != null ? fscInherit.getQueryServerURL() : null));
                    fsc.setRelevantQueriesEnabled(parseBoolean(commandE.getAttribute("relevant-queries"),
                            fscInherit != null ? fscInherit.isRelevantQueriesEnabled() : false));
                    fsc.setSortBy(parseString(commandE.getAttribute("sort-by"),
                            fscInherit != null ? fscInherit.getSortBy() : ""));
                    fsc.setSpamScoreLimit(parseInt(commandE.getAttribute("spam-score-limit"),
                            fscInherit != null ? fscInherit.getSpamScoreLimit() : -1));
                    fsc.setSpellcheckEnabled(parseBoolean(commandE.getAttribute("spellcheck"),
                             fscInherit != null ? fscInherit.isSpellcheckEnabled() : false));
                    //fsc.setSynonymEnabled(Boolean.parseBoolean(commandE.getAttribute("synonyms"))); // FIXME !!

                    // navigators
                    final NodeList nList = commandE.getElementsByTagName("navigators");
                    for(int i = 0; i < nList.getLength(); ++i){
                        final Collection<FastNavigator> navigators = parseNavigators((Element)nList.item(i));
                        for(FastNavigator navigator : navigators){
                            fsc.addNavigator(navigator, navigator.getId());
                        }

                    }
                }
                if (sc instanceof AdvancedFastSearchConfiguration) {
                    final AdvancedFastSearchConfiguration asc = (AdvancedFastSearchConfiguration) sc;
                    final AdvancedFastSearchConfiguration ascInherit = inherit instanceof AdvancedFastSearchConfiguration
                            ? (AdvancedFastSearchConfiguration) inherit
                            : null;
                    asc.setView(parseString(commandE.getAttribute("view"),
                            ascInherit != null ? ascInherit.getView() : ""));
                    asc.setSortBy(parseString(commandE.getAttribute("sort-by"),
                            ascInherit != null ? ascInherit.getSortBy() : "default"));
                    asc.setCollapsingEnabled(parseBoolean(commandE.getAttribute("collapsing"),
                            ascInherit != null ? ascInherit.isCollapsingEnabled() : false));
                    asc.setCollapseOnField(parseString(commandE.getAttribute("collapse-on-field"),
                            ascInherit != null ? ascInherit.getCollapseOnField() : ""));
                    asc.setQtPipeline(parseString(commandE.getAttribute("qt-pipeline"),
                            ascInherit != null ? ascInherit.getQtPipeline() : ""));

                    final String qrServer = commandE.getAttribute("query-server");
                    final String qrServerValue = parseProperty(cxt, qrServer,
                            ascInherit != null ? ascInherit.getQueryServer() : null);

                    if (qrServerValue.startsWith("http://")) {
                       throw new IllegalArgumentException(ERR_FAST_EPS_QR_SERVER + qrServerValue);
                    }

                    asc.setQueryServer(qrServerValue);
                    // navigators
                    final NodeList nList = commandE.getElementsByTagName("navigators");
                    for(int i = 0; i < nList.getLength(); ++i){
                        final Collection<FastNavigator> navigators = parseNavigators((Element)nList.item(i));
                        for(FastNavigator navigator : navigators){
                            asc.addNavigator(navigator, navigator.getId());
                        }

                    }
                }
                if(sc instanceof HittaSearchConfiguration){
                    final HittaSearchConfiguration hsc = (HittaSearchConfiguration) sc;
                    final HittaSearchConfiguration hscInherit = inherit instanceof HittaSearchConfiguration
                            ? (HittaSearchConfiguration)inherit
                            : null;
                    hsc.setCatalog(parseString(commandE.getAttribute("catalog"),
                            hscInherit != null ? hscInherit.getCatalog() : ""));
                    hsc.setKey(parseString(commandE.getAttribute("key"),
                            hscInherit != null ? hscInherit.getKey() : ""));
                }
                if(sc instanceof MathExpressionSearchConfiguration){
                    final MathExpressionSearchConfiguration msc = (MathExpressionSearchConfiguration) sc;
                }
                if(sc instanceof NewsSearchConfiguration){
                    final NewsSearchConfiguration nsc = (NewsSearchConfiguration) sc;
                }
                if(sc instanceof AbstractYahooSearchConfiguration){
                    final AbstractYahooSearchConfiguration osc = (AbstractYahooSearchConfiguration) sc;
                    final AbstractYahooSearchConfiguration oscInherit = inherit instanceof AbstractYahooSearchConfiguration
                            ? (AbstractYahooSearchConfiguration)inherit
                            : null;
                    osc.setEncoding(parseString(commandE.getAttribute("encoding"),
                            oscInherit != null ? oscInherit.getEncoding() : ""));
                    osc.setHost(parseString(commandE.getAttribute("host"),
                            oscInherit != null ? oscInherit.getHost() : ""));
                    osc.setPartnerId(parseString(commandE.getAttribute("partner-id"),
                            oscInherit != null ? oscInherit.getPartnerId() : ""));
                    osc.setPort(parseInt(commandE.getAttribute("port"),
                            oscInherit != null ? oscInherit.getPort() : 80));
                }
                if(sc instanceof OverturePPCSearchConfiguration){
                    final OverturePPCSearchConfiguration osc = (OverturePPCSearchConfiguration) sc;
                    final OverturePPCSearchConfiguration oscInherit = inherit instanceof OverturePPCSearchConfiguration
                            ? (OverturePPCSearchConfiguration)inherit
                            : null;
                    osc.setUrl(parseString(commandE.getAttribute("url"),
                            oscInherit != null ? oscInherit.getUrl() : ""));
                }
                if(sc instanceof YahooIdpSearchConfiguration){
                    final YahooIdpSearchConfiguration ysc = (YahooIdpSearchConfiguration) sc;
                    final YahooIdpSearchConfiguration yscInherit = inherit instanceof YahooIdpSearchConfiguration
                            ? (YahooIdpSearchConfiguration)inherit
                            : null;
                    ysc.setDatabase(parseString(commandE.getAttribute("database"),
                            yscInherit != null ? yscInherit.getDatabase() : ""));
                    ysc.setDateRange(parseString(commandE.getAttribute("date-range"),
                            yscInherit != null ? yscInherit.getDateRange() : ""));
                    ysc.setFilter(parseString(commandE.getAttribute("filter"),
                            yscInherit != null ? yscInherit.getFilter() : ""));
                    ysc.setRegion(parseString(commandE.getAttribute("region"),
                            yscInherit != null ? yscInherit.getRegion() : ""));
                    ysc.setRegionMix(parseString(commandE.getAttribute("region-mix"),
                            yscInherit != null ? yscInherit.getRegionMix() : ""));
                    ysc.setSpellState(parseString(commandE.getAttribute("spell-state"),
                            yscInherit != null ? yscInherit.getSpellState() : ""));
                    ysc.setUnique(parseString(commandE.getAttribute("unique"),
                            yscInherit != null ? yscInherit.getUnique() : ""));
                }
                if(sc instanceof PicSearchConfiguration){
                    final PicSearchConfiguration psc = (PicSearchConfiguration) sc;
                    final PicSearchConfiguration pscInherit = inherit instanceof PicSearchConfiguration
                            ? (PicSearchConfiguration)inherit
                            : null;
                    final String queryServerHost = commandE.getAttribute("query-server-host");
                    psc.setQueryServerHost(parseProperty(cxt, queryServerHost,
                            pscInherit != null ? pscInherit.getQueryServerHost() : null));
                    final String queryServerPort = commandE.getAttribute("query-server-port");
                    psc.setQueryServerPort(Integer.valueOf(parseProperty(cxt, queryServerPort,
                            pscInherit != null ? String.valueOf(pscInherit.getQueryServerPort()) : "0")));
                    psc.setPicsearchCountry(parseString(commandE.getAttribute("picsearchCountry"),
                            pscInherit != null ? pscInherit.getPicsearchCountry() : "no"));
                }
                if(sc instanceof SensisSearchConfiguration){
                    final SensisSearchConfiguration ssc = (SensisSearchConfiguration) sc;
                }
                if(sc instanceof StockSearchConfiguration){
                    final StockSearchConfiguration ssc = (StockSearchConfiguration) sc;
                }
                if(sc instanceof WebSearchConfiguration){
                    final WebSearchConfiguration wsc = (WebSearchConfiguration) sc;
                }
                if(sc instanceof WhiteSearchConfiguration){
                    final WhiteSearchConfiguration wsc = (WhiteSearchConfiguration) sc;
                }
                if(sc instanceof YellowSearchConfiguration){
                    final YellowSearchConfiguration ysc = (YellowSearchConfiguration) sc;
                }
                if (sc instanceof MobileSearchConfiguration) {
                    final MobileSearchConfiguration msc = (MobileSearchConfiguration) sc;

                    msc.setPersonalizationGroup(commandE.getAttribute("personalization-group"));
                    msc.setTelenorPersonalizationGroup(commandE.getAttribute("telenor-personalization-group"));
                    msc.setSortBy(commandE.getAttribute("sort-by"));
                    msc.setSource(commandE.getAttribute("source"));
                    msc.setFilter(commandE.getAttribute("filter"));
                }
                if (sc instanceof BlendingNewsSearchConfiguration) {
                    final BlendingNewsSearchConfiguration bnsc = (BlendingNewsSearchConfiguration) sc;

                    final String filters[] = commandE.getAttribute("filters").split(",");

                    final List<String> filterList = new ArrayList<String>();

                    for (int i = 0; i < filters.length; i++) {
                        filterList.add(filters[i].trim());
                    }

                    bnsc.setFiltersToBlend(filterList);
                    bnsc.setDocumentsPerFilter(Integer.parseInt(commandE.getAttribute("documentsPerFilter")));
                }

                if (sc instanceof StormWeatherSearchConfiguration) {
					final StormWeatherSearchConfiguration swsc = (StormWeatherSearchConfiguration) sc;
					if(commandE.getAttribute("xml-elements").length() >0){
                        final String[] elms = commandE.getAttribute("xml-elements").split(",");
                        for(String elm : elms){
                            swsc.addElementValue(elm.trim());
                        }
                    }
					
				}

                if (sc instanceof TvSearchConfiguration) {
                    final TvSearchConfiguration tssc = (TvSearchConfiguration) sc;
                    final String[] defaultChannels = commandE.getAttribute("default-channels").split(",");
                    for (String channel : defaultChannels) {
                        tssc.addDefaultChannel(channel.trim());
                    }
                    tssc.setResultsToFetch(Integer.parseInt(commandE.getAttribute("results-to-fetch")));

                }

                // query transformers
                NodeList qtNodeList = commandE.getElementsByTagName("query-transformers");
                final Element qtRootElement = (Element) qtNodeList.item(0);
                if(qtRootElement != null){
                    qtNodeList = qtRootElement.getChildNodes();

                    // clear all inherited query-transformers
                    sc.clearQueryTransformers();
                    
                    for(int i = 0; i < qtNodeList.getLength(); i++) {
                        final Node node = qtNodeList.item(i);
                        if (!(node instanceof Element)) {
                            continue;
                        }
                        final Element qt = (Element) node;
                        for (QueryTransformerTypes qtType : QueryTransformerTypes.values()) {
                            if (qt.getTagName().equals(qtType.getXmlName())) {
                                sc.addQueryTransformer(qtType.parseQueryTransformer(qt));
                            }
                        }
                    }
                }

                // result handlers
                NodeList rhNodeList = commandE.getElementsByTagName("result-handlers");
                final Element rhRootElement = (Element) rhNodeList.item(0);
                if(rhRootElement != null){
                    rhNodeList = rhRootElement.getChildNodes();
                    
                    // clear all inherited result handlers
                    sc.clearResultHandlers();
                    
                    for(int i = 0; i < rhNodeList.getLength(); i++) {
                        final Node node = rhNodeList.item(i);
                        if (!(node instanceof Element)) {
                            continue;
                        }
                        final Element rh = (Element) node;
                        for (ResultHandlerTypes rhType : ResultHandlerTypes.values()) {
                            if (rh.getTagName().equals(rhType.getXmlName())) {
                                sc.addResultHandler(rhType.parseResultHandler(rh));
                            }
                        }
                    }
                }

                return sc;

            } catch (InstantiationException ex) {
                throw new InfrastructureException(ex);
            } catch (IllegalAccessException ex) {
                throw new InfrastructureException(ex);
            } catch (SecurityException ex) {
                throw new InfrastructureException(ex);
            } catch (NoSuchMethodException ex) {
                throw new InfrastructureException(ex);
            } catch (IllegalArgumentException ex) {
                throw new InfrastructureException(ex);
            } catch (InvocationTargetException ex) {
                throw new InfrastructureException(ex);
            }
        }

        private SearchConfiguration findParent(
                final String id,
                final SearchMode mode){

            SearchMode m = mode;
            SearchConfiguration config = null;
            do{
                COMMANDS_LOCK.readLock().lock();
                final Map<String,SearchConfiguration> configs = COMMANDS.get(m);
                COMMANDS_LOCK.readLock().unlock();
                config = configs.get(id);
                m = m.getParentSearchMode();
            }while(config == null && m != null);

            return config;
        }

        private String parseProperty(final Context cxt, final String s, final String def){

            final String key = s.trim().length() == 0 ? def != null ? def : "" : s;
            final String value = SiteConfiguration.valueOf(
                    ContextWrapper.wrap(SiteConfiguration.Context.class, cxt))
                    .getProperty(key);
            final String result = value != null ? value : key;
            LOG.debug(DEBUG_PARSED_PROPERTY + key + " --> " + result);
            return result;
        }

        private Collection<FastNavigator> parseNavigators(final Element navsE){

            final Collection<FastNavigator> navigators = new ArrayList<FastNavigator>();
            final NodeList children = navsE.getChildNodes();
            for(int i = 0; i < children.getLength(); ++i){
                final Node child = children.item(i);
                if(child instanceof Element && "navigator".equals(((Element)child).getTagName())){
                    final Element navE = (Element)child;
                    final String id = navE.getAttribute("id");
                    final String name = navE.getAttribute("name");
                    LOG.info(INFO_PARSING_NAVIGATOR + id + " [" + name + "]");
                    final FastNavigator nav = new FastNavigator(
                            name,
                            navE.getAttribute("field"),
                            navE.getAttribute("display-name"));
                    nav.setId(id);
                    final Collection<FastNavigator> childNavigators = parseNavigators(navE);
                    if(childNavigators.size() > 1){
                        throw new IllegalStateException(ERR_ONLY_ONE_CHILD_NAVIGATOR_ALLOWED + id);
                    }else if(childNavigators.size() == 1){
                        nav.setChildNavigator(childNavigators.iterator().next());
                    }
                    navigators.add(nav);
                }
            }

            return navigators;
        }
    }

    private enum QueryTransformerTypes {
        EXACT_TITLE_MATCH (ExactTitleMatchTransformer.class),
        INFOPAGE (InfopageQueryTransformer.class),
        WEATHER (WeatherQueryTransformer.class),
        WEATHERINFOPAGE (WeatherInfopageQueryTransformer.class),
        NEWS (NewsTransformer.class),
        PREFIX_REMOVER (PrefixRemoverTransformer.class),
        SIMPLE_SITE_SEARCH (SimpleSiteSearchTransformer.class),
        SYNONYM (SynonymQueryTransformer.class),
        TERM_PREFIX (TermPrefixTransformer.class),
        NOW (NowQueryTransformer.class),
        WEBTV (WebTvQueryTransformer.class),
        TV (TvQueryTransformer.class),
        TVSEARCH(TvSearchQueryTransformer.class),
        AGEFILTER(AgeFilterTransformer.class);

        private final Class<? extends QueryTransformer> clazz;
        private final String xmlName;

        QueryTransformerTypes(final Class<? extends QueryTransformer> c){
            clazz = c;
            xmlName = name().replaceAll("_","-").toLowerCase();
        }

        public String getXmlName(){
            return xmlName;
        }

        public QueryTransformer parseQueryTransformer(final Element qt){
            try {

                LOG.info(INFO_PARSING_QUERY_TRANSFORMER + xmlName);
                final QueryTransformer transformer = clazz.newInstance();
                switch(this){
                    case PREFIX_REMOVER:
                        final PrefixRemoverTransformer prqt = (PrefixRemoverTransformer) transformer;
                        prqt.addPrefixes(qt.getAttribute("prefixes").split(","));
                        break;
                    case SIMPLE_SITE_SEARCH:
                        final SimpleSiteSearchTransformer ssqt = (SimpleSiteSearchTransformer) transformer;
                        ssqt.setParameterName(qt.getAttribute("parameter"));
                        break;
                    case TERM_PREFIX:
                        final TermPrefixTransformer tpqt = (TermPrefixTransformer) transformer;
                        tpqt.setPrefix(qt.getAttribute("prefix"));
                        tpqt.setNumberPrefix(qt.getAttribute("number-prefix"));
                        break;
                    case NOW:
                        final NowQueryTransformer nqt = (NowQueryTransformer) transformer;
                        nqt.setPrefix(qt.getAttribute("prefix"));
                        break;
                    case TVSEARCH:
                        final TvSearchQueryTransformer tsqt = (TvSearchQueryTransformer) transformer;
                        tsqt.setWithEndtime(qt.getAttribute("with-endtime").equals("true"));
                        break;
                    case AGEFILTER:
                        final AgeFilterTransformer agft = (AgeFilterTransformer) transformer;
                        agft.setAgeField(qt.getAttribute("field"));
                        break;
                    case WEATHER:
                        final WeatherQueryTransformer wqt = (WeatherQueryTransformer) transformer;
                        wqt.setDefaultLocations(qt.getAttribute("default-locations").split(","));
                }
                return transformer;

            } catch (InstantiationException ex) {
                throw new InfrastructureException(ex);
            } catch (IllegalAccessException ex) {
                throw new InfrastructureException(ex);
            }
        }

    }

    private enum ResultHandlerTypes {
        ADD_DOC_COUNT (AddDocCountModifier.class),
        AGE_CALCULATOR (AgeCalculatorResultHandler.class),
        CATEGORY_SPLITTER (CategorySplitter.class),
        CONTENT_SOURCE_COLLECTOR (ContentSourceCollector.class),
        DATA_MODEL (DataModelResultHandler.class),
        DISCARD_OLD_NEWS (DiscardOldNewsResultHandler.class),
        DISCARD_DUPLICATES (DiscardDuplicatesResultHandler.class),
        FIELD_CHOOSER (FieldChooser.class),
        FIND_FILE_FORMAT (FindFileFormat.class),
        IMAGE_HELPER (ImageHelper.class),
        MULTIVALUED_FIELD_COLLECTOR (MultiValuedFieldCollector.class),
        NUMBER_OPERATION (NumberOperationHandler.class),
        PHONE_NUMBER_CHOOSER (PhoneNumberChooser.class),
        PHONE_NUMBER_FORMATTER (PhoneNumberFormatter.class),
        SPELLING_SUGGESTION_CHOOSER (SpellingSuggestionChooser.class),
        SUM (SumFastModifiers.class),
        DATE_FORMAT (DateFormatHandler.class),
        WEATHER_CELCIUS (WeatherCelciusHandler.class),
        WEATHER_DATE (WeatherDateHandler.class),
        FORECAST_DATE (ForecastDateHandler.class),
        FORECAST_WIND (ForecastWindHandler.class),
        MAP_COORD (MapCoordHandler.class),
        TVSEARCH_SORTING (TvSearchSortingHandler.class),
        COMBINE_NAVIGATORS (CombineNavigatorsHandler.class),
        TEXT_OUTPUT (TextOutputResultHandler.class),
        VELOCITY_OUTPUT (VelocityResultHandler.class),
        FIELD_ESCAPE (FieldEscapeHandler.class),
        XML_OUTPUT (XmlOutputResultHandler.class);


        private final Class<? extends ResultHandler> clazz;
        private final String xmlName;

        ResultHandlerTypes(final Class<? extends ResultHandler> c){
            clazz = c;
            xmlName = name().replaceAll("_","-").toLowerCase();
        }

        public String getXmlName(){
            return xmlName;
        }

        public ResultHandler parseResultHandler(final Element rh){

            try {
                LOG.info(INFO_PARSING_RESULT_HANDLER + xmlName);
                final ResultHandler handler = clazz.newInstance();
                switch(this){
                    case ADD_DOC_COUNT:
                        final AddDocCountModifier adc = (AddDocCountModifier) handler;
                        adc.setModifierName(rh.getAttribute("modifier"));
                        break;
                    case AGE_CALCULATOR:
                        final AgeCalculatorResultHandler ac = (AgeCalculatorResultHandler) handler;
                        ac.setTargetField(rh.getAttribute("target"));
                        ac.setSourceField(rh.getAttribute("source"));
                        break;
                    case FIELD_CHOOSER:
                        final FieldChooser fc = (FieldChooser) handler;
                        {
                            fc.setTargetField(rh.getAttribute("target"));
                            final String[] fields = rh.getAttribute("fields").split(",");
                            for(String field : fields){
                                fc.addField(field);
                            }
                        }
                        break;
                    case MULTIVALUED_FIELD_COLLECTOR:
                        final MultiValuedFieldCollector mvfc = (MultiValuedFieldCollector) handler;
                        {
                            if(rh.getAttribute("fields").length() >0){
                                final String[] fields = rh.getAttribute("fields").split(",");
                                for(String field : fields){
                                    if(field.contains(" AS ")){
                                        final String[] ff = field.split(" AS ");
                                        mvfc.addField(ff[0], ff[1]);
                                    }else{
                                        mvfc.addField(field, field);
                                    }
                                }
                            }
                        }
                        break;
                    case NUMBER_OPERATION:
                        final NumberOperationHandler noh = (NumberOperationHandler)handler;
                        {
                            if(rh.getAttribute("fields").length() >0){
                                final String[] fields = rh.getAttribute("fields").split(",");
                                for(String field : fields){
                                    noh.addField(field);
                                }
                            }
                            noh.setTarget(parseString(rh.getAttribute("target"), ""));
                            noh.setOperation(parseString(rh.getAttribute("operation"), ""));
                            noh.setMinDigits(parseInt(rh.getAttribute("min-digits"), 1));
                            noh.setMaxDigits(parseInt(rh.getAttribute("max-digits"), 99));
                            noh.setMinFractionDigits(parseInt(rh.getAttribute("min-fraction-digits"), 0));
                            noh.setMaxFractionDigits(parseInt(rh.getAttribute("max-fraction-digits"), 99));
                        }
                        break;
                    case IMAGE_HELPER:
                        final ImageHelper im = (ImageHelper) handler;
                        {
                            if(rh.getAttribute("fields").length() >0){
                                final String[] fields = rh.getAttribute("fields").split(",");
                                for(String field : fields){
                                    if(field.contains(" AS ")){
                                        final String[] ff = field.split(" AS ");
                                        im.addField(ff[0], ff[1]);
                                    }else{
                                        im.addField(field, field);
                                    }
                                }
                            }
                        }
                        break;
                    case SPELLING_SUGGESTION_CHOOSER:
                        final SpellingSuggestionChooser ssc = (SpellingSuggestionChooser) handler;
                        ssc.setMinScore(parseInt(rh.getAttribute("min-score"), -1));
                        ssc.setMaxSuggestions(parseInt(rh.getAttribute("max-suggestions"), -1));
                        ssc.setMaxDistance(parseInt(rh.getAttribute("max-distance"), -1));
                        ssc.setMuchBetter(parseInt(rh.getAttribute("much-better"), -1));
                        ssc.setLongQuery(parseInt(rh.getAttribute("long-query"), -1));
                        ssc.setVeryLongQuery(parseInt(rh.getAttribute("very-long-query"), -1));
                        ssc.setLongQueryMaxSuggestions(parseInt(rh.getAttribute("long-query-max-suggestions"), -1));
                        break;
                    case SUM:
                        final SumFastModifiers sfm = (SumFastModifiers) handler;
                        final String[] modifiers = rh.getAttribute("modifiers").split(",");
                        for(String modifier : modifiers){
                            sfm.addModifierName(modifier);
                        }
                        sfm.setNavigatorName(rh.getAttribute("navigation"));
                        sfm.setTargetModifier(rh.getAttribute("target"));
                        break;
                    case DATE_FORMAT:
                        final DateFormatHandler dh = (DateFormatHandler) handler;
                        if (rh.hasAttribute("prefix")) {
                            dh.setFieldPrefix(rh.getAttribute("prefix"));
                        }
                        dh.setSourceField(rh.getAttribute("source"));
                        break;
                    case WEATHER_CELCIUS:
                        final WeatherCelciusHandler ch = (WeatherCelciusHandler) handler;
                        ch.setTargetField(rh.getAttribute("target"));
                        ch.setSourceField(rh.getAttribute("source"));
                        break;
                    case MAP_COORD:
                        final MapCoordHandler mch = (MapCoordHandler) handler;
                        break;
                    case FORECAST_WIND:
                        final ForecastWindHandler wh = (ForecastWindHandler) handler;
                        break;
                    case DISCARD_DUPLICATES:	//subclasses must be checked first
                        final DiscardDuplicatesResultHandler ddh = (DiscardDuplicatesResultHandler) handler;
                        ddh.setSourceField(rh.getAttribute("key"));
                        ddh.setDiscardCase(new Boolean(rh.getAttribute("ignorecase")).booleanValue());
                        break;
                    case FORECAST_DATE:	//subclasses must be checked first
                        final ForecastDateHandler sdateh = (ForecastDateHandler) handler;
                        sdateh.setTargetField(rh.getAttribute("target"));
                        sdateh.setSourceField(rh.getAttribute("source"));
                        break;
                    case WEATHER_DATE:
                        final WeatherDateHandler dateh = (WeatherDateHandler) handler;
                        dateh.setTargetField(rh.getAttribute("target"));
                        dateh.setSourceField(rh.getAttribute("source"));
                        break;
                    case TVSEARCH_SORTING:
                        final TvSearchSortingHandler tssh = (TvSearchSortingHandler) handler;

                        tssh.setResultsPerBlock(Integer.parseInt(rh.getAttribute("results-per-block")));
                        tssh.setBlocksPerPage(Integer.parseInt(rh.getAttribute("blocks-per-page")));
                        break;
                    case FIELD_ESCAPE:
                        final FieldEscapeHandler feh = (FieldEscapeHandler) handler;
                        feh.setSourceField(rh.getAttribute("source-field"));
                        feh.setTargetField(rh.getAttribute("target-field"));
                        break;
                    case COMBINE_NAVIGATORS:
                        final CombineNavigatorsHandler cnh = (CombineNavigatorsHandler) handler;
                        cnh.setTarget(rh.getAttribute("target"));

                        final NodeList navs = rh.getElementsByTagName("navigator");
                        
                        for (int i = 0; i < navs.getLength(); i++) {
                            final Element nav = (Element) navs.item(i);

                            final NodeList mods = nav.getElementsByTagName("modifier");
                            for (int j = 0; j < mods.getLength(); j++) {
                                final Element mod = (Element) mods.item(j);
                                
                                cnh.addMapping(nav.getAttribute("name"), mod.getAttribute("name"));
                            }
                        }
                        break;
                }

                return handler;

            } catch (InstantiationException ex) {
                throw new InfrastructureException(ex);
            } catch (IllegalAccessException ex) {
                throw new InfrastructureException(ex);
            }
        }

    }
}
