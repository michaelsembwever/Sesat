/* Copyright (2005-2006) Schibsted Søk AS
 *
 * AbstractQueryParserContext.java
 *
 * Created on 12 January 2006, 12:06
 *
 */

package no.schibstedsok.searchportal.query.parser;

import no.schibstedsok.searchportal.query.AndClause;
import no.schibstedsok.searchportal.query.AndNotClause;
import no.schibstedsok.searchportal.query.Clause;
import no.schibstedsok.searchportal.query.DefaultOperatorClause;
import no.schibstedsok.searchportal.query.EmailClause;
import no.schibstedsok.searchportal.query.IntegerClause;
import no.schibstedsok.searchportal.query.NotClause;
import no.schibstedsok.searchportal.query.OrClause;
import no.schibstedsok.searchportal.query.OrganisationNumberClause;
import no.schibstedsok.searchportal.query.PhoneNumberClause;
import no.schibstedsok.searchportal.query.PhraseClause;
import no.schibstedsok.searchportal.query.UrlClause;
import no.schibstedsok.searchportal.query.WordClause;
import no.schibstedsok.searchportal.query.XorClause;
import org.apache.log4j.Logger;

/** Default implementation of QueryParser.Context's createXxxClause methods.
 *
 * @version $Id$
 * @author <a href="mailto:mick@wever.org">Michael Semb Wever</a>
 */
public abstract class AbstractQueryParserContext implements AbstractQueryParser.Context {

    private static final Logger LOG = Logger.getLogger(AbstractQueryParserContext.class);

    /** Creates a new instance of AbstractQueryParserContext.
     */
    public AbstractQueryParserContext() {
    }

    /** {@inheritDoc}
     */
    public final String getQueryString() {
        return getTokenEvaluationEngine().getQueryString();
    }


    //// Operator creators
    /** {@inheritDoc}
     */
    public DefaultOperatorClause createDefaultOperatorClause(final Clause first, final Clause second){

        LOG.debug("createDefaultOperatorClause(" + first + "," + second + ")");
        return DefaultOperatorClauseImpl.createDefaultOperatorClause(first, second, getTokenEvaluationEngine());
    }

    /** {@inheritDoc}
     */
    public final AndClause createAndClause(
        final Clause first,
        final Clause second) {

        LOG.debug("createAndClause(" + first + "," + second + ")");
        return AndClauseImpl.createAndClause(first, second, getTokenEvaluationEngine());
    }

    /** {@inheritDoc}
     */
    public final OrClause createOrClause(
        final Clause first,
        final Clause second) {

        LOG.debug("createOrClause(" + first + "," + second + ")");
        return OrClauseImpl.createOrClause(first, second, getTokenEvaluationEngine());
    }

    /** {@inheritDoc}
     */
    public final XorClause createXorClause(
        final Clause first,
        final Clause second,
        final XorClause.Hint hint) {

        LOG.debug("createXorClause(" + first + "," + second + "," + hint + ")");
        return XorClauseImpl.createXorClause(first, second, hint, getTokenEvaluationEngine());
    }

    /** {@inheritDoc}
     */
    public final AndNotClause createAndNotClause(
        final Clause first) {

        LOG.debug("createAndNotClause(" + first + ")");
        return AndNotClauseImpl.createAndNotClause(first, getTokenEvaluationEngine());
    }

    /** {@inheritDoc}
     */
    public final NotClause createNotClause(
        final Clause first) {

        LOG.debug("createNotClause(" + first + ")");
        return NotClauseImpl.createNotClause(first, getTokenEvaluationEngine());
    }


    //// Leaf creators

    /** {@inheritDoc}
     */
    public final WordClause createWordClause(
        final String term,
        final String field) {

        LOG.debug("createWordClause(" + term + "," + field + ")");
        return WordClauseImpl.createWordClause(term, field, getTokenEvaluationEngine());
    }

    /** {@inheritDoc}
     */
    public final PhraseClause createPhraseClause(
        final String term,
        final String field) {

        LOG.debug("createPhraseClause(" + term + "," + field + ")");
        return PhraseClauseImpl.createPhraseClause(term, field, getTokenEvaluationEngine());
    }

    /** {@inheritDoc}
     */
    public final IntegerClause createIntegerClause(
        final String term,
        final String field) {

        LOG.debug("createIntegerClause(" + term + "," + field + ")");
        return IntegerClauseImpl.createIntegerClause(term, field, getTokenEvaluationEngine());
    }

    /** {@inheritDoc}
     */
    public final PhoneNumberClause createPhoneNumberClause(
        final String term,
        final String field) {

        LOG.debug("createPhoneNumberClause(" + term + "," + field + ")");
        return PhoneNumberClauseImpl.createPhoneNumberClause(term, field, getTokenEvaluationEngine());
    }

    /** {@inheritDoc}
     */
    public final OrganisationNumberClause createOrganisationNumberClause(
        final String term,
        final String field) {

        LOG.debug("createOrganisationNumberClause(" + term + "," + field + ")");
        return OrganisationNumberClauseImpl.createOrganisationNumberClause(term, field, getTokenEvaluationEngine());
    }

    /** {@inheritDoc}
     */
    public final UrlClause createUrlClause(final String term, final String field){

        LOG.debug("createUrlClause(" + term + "," + field + ")");
        return UrlClauseImpl.createUrlClause(term, field, getTokenEvaluationEngine());
    }
    /** {@inheritDoc}
     */
    public final EmailClause createEmailClause(final String term, final String field){

        LOG.debug("createEmailClause(" + term + "," + field + ")");
        return EmailClauseImpl.createEmailClause(term, field, getTokenEvaluationEngine());
    }
}