// Copyright (2007) Schibsted Søk AS
package no.schibstedsok.searchportal.mode.command;

import no.schibstedsok.searchportal.mode.config.TvenrichCommandConfig;
import no.schibstedsok.searchportal.result.BasicResultList;
import no.schibstedsok.searchportal.result.FastSearchResult;
import no.schibstedsok.searchportal.result.ResultItem;
import no.schibstedsok.searchportal.result.ResultList;
import org.apache.log4j.Logger;

/**
 * A search command that combine the results from TV and WebTV search.
 */
public final class TvEnrichSearchCommand extends StaticSearchCommand {

    private static final Logger LOG = Logger.getLogger(TvEnrichSearchCommand.class);

    public TvEnrichSearchCommand(final Context cxt) {

        super(cxt);
    }

    public ResultList<? extends ResultItem> execute() {
        int hitCount = 0;

        final ResultList<ResultItem> result = new BasicResultList<ResultItem>();
        TvenrichCommandConfig tesc = (TvenrichCommandConfig) this.getSearchConfiguration();
        if (tesc.getWaitOn() != null) {
            final String waitOn = tesc.getWaitOn();

            final String[] cmds = waitOn.split(",");
            try {
                for (String cmd : cmds) {
                    final FastSearchResult fsr = (FastSearchResult)getSearchResult(cmd, datamodel);
                    hitCount += fsr.getHitCount();
                    result.addResults(fsr.getResults());
                }
            } catch (Exception e) {
                LOG.error(e);
                return new BasicResultList<ResultItem>();
            }
        }

        result.setHitCount(hitCount);
        return result;
    }
}
