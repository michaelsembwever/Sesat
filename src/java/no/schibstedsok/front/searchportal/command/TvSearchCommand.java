/*
 * Copyright (2005-2006) Schibsted Søk AS
 *
 */
package no.schibstedsok.front.searchportal.command;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import no.schibstedsok.front.searchportal.configuration.TvSearchConfiguration;
import no.schibstedsok.front.searchportal.result.BasicSearchResult;
import no.schibstedsok.front.searchportal.result.SearchResult;
import no.schibstedsok.front.searchportal.result.SearchResultItem;
import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:anders.johan.jamtli@sesam.no">Anders Johan Jamtli</a>
 * @version <tt>$Revision: 2567 $</tt>
 */
public class TvSearchCommand extends AbstractSimpleFastSearchCommand {

    private static final Logger LOG = Logger.getLogger(TvSearchCommand.class);

    protected StringBuilder defaultChannelFilter = new StringBuilder();
    private String additionalFilter;
    
    /** Creates a new instance of TvSearchCommand
     *
     * @param cxt Search command context.
     * @param parameters Search command parameters.
     */
    public TvSearchCommand(final Context cxt, final Map parameters) {
        super(cxt, parameters);
        LOG.debug("Creating TvSearchCommand");
        synchronized (this) {
//            defaultChannelFilter = new StringBuilder(super.getAdditionalFilter());

            defaultChannelFilter.append("+(");
            for (String channel : getTvSearchConfiguration().getDefaultChannels()) {
                defaultChannelFilter.append(" sgeneric5nav:");
                defaultChannelFilter.append(channel);
            }
            defaultChannelFilter.append(")");
            additionalFilter = "";
        }
    }

    protected int getResultsToReturn() {
        final String sortByString = this.getParameters().get("userSortBy") != null ? (String) this.getParameters().get("userSortBy") : "channel";
        if ("day".equals(sortByString)) {
            return 15;
        }
        if (getRunningQuery().getQuery().isBlank()) {
            return getTvSearchConfiguration().getResultsToFetch();
        } else {
            return getTvSearchConfiguration().getResultsToReturn();
        }
    }
    
    public SearchResult execute() {
        final String sortByString = this.getParameters().get("userSortBy") != null ? (String) this.getParameters().get("userSortBy") : "channel";
        SearchResult sr = null;
        
        if ("day".equals(sortByString)) {
            sr = new BasicSearchResult(this);
            int totalHits = 0;
            for (int i = 0; i < 7; i++) {
                setAdditionalFilter(" +sgeneric7nav:" + i);
                SearchResult result = super.execute();
                sr.getResults().addAll(result.getResults());
                totalHits += result.getHitCount();
            }
            sr.setHitCount(totalHits);
        } else {
            sr = super.execute();
        }

        return sr;
    }

    private TvSearchConfiguration getTvSearchConfiguration() {
        return (TvSearchConfiguration) getSearchConfiguration();
    }

    protected String getAdditionalFilter() {
        /* Only fetch default channels on blank query */
        if (getRunningQuery().getQuery().isBlank() && !getParameters().containsKey("nav_channels") && !getParameters().containsKey("nav_categories")) {
            return additionalFilter + " " + defaultChannelFilter.toString();
        }
        
        if (getRunningQuery().getQuery().isBlank() && getParameters().containsKey("output") && getParameters().get("output").equals("rss")) {
            return additionalFilter + " " + defaultChannelFilter.toString();
        }
        
        return additionalFilter;
    }
    
    protected void setAdditionalFilter(String additionalFilter) {
        this.additionalFilter = additionalFilter;
    }
}
