/*
 * Copyright (2005-2006) Schibsted Søk AS
 */
package no.schibstedsok.front.searchportal.query.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import no.schibstedsok.front.searchportal.query.IntegerClause;
import no.schibstedsok.front.searchportal.query.UrlClause;
import no.schibstedsok.front.searchportal.query.token.TokenEvaluatorFactory;
import no.schibstedsok.front.searchportal.query.token.TokenPredicate;

/**
 * UrlClauseImpl. Contains one http URL.
 * 
 * <b>Objects of this class are immutable</b>
 * 
 * @author <a hrefIntegerClauseImpl@wever.org">Michael Semb Wever</a>
 * @version $Id: IntegerClauseImpl.java 2335 2006-02-18 13:45:11Z mickw $
 */
public final class UrlClauseImpl extends AbstractLeafClause implements UrlClause {

    /** Values are WeakReference object to AbstractClause.
     * Unsynchronized are there are no 'changing values', just existance or not of the AbstractClause in the system.
     */
    private static final Map/*<Long,WeakReference<AbstractClause>>*/ WEAK_CACHE = new HashMap/*<Long,WeakReference<AbstractClause>>*/();

    /* A IntegerClauseImpl specific collection of TokenPredicates that *could* apply to this Clause type. */
    private static final Collection/*<Predicate>*/ PREDICATES_APPLICABLE;

    static {
        final Collection/*<Predicate>*/ predicates = new ArrayList();
        predicates.add(TokenPredicate.ALWAYSTRUE);
        // Predicates from RegExpEvaluators

        // Add all FastTokenPredicates
        predicates.addAll(TokenPredicate.getFastTokenPredicates());
        PREDICATES_APPLICABLE = Collections.unmodifiableCollection(predicates);
    }

    /**
     * Creator method for UrlClauseImpl objects. By avoiding the constructors,
     * and assuming all UrlClauseImpl objects are immutable, we can keep track
     * (via a weak reference map) of instances already in use in this JVM and reuse
     * them.
     * The methods also allow a chunk of creation logic for the UrlClauseImpl to be moved
     * out of the QueryParserImpl.jj file to here.
     * 
     * @param term the term this clause represents.
     * @param field any field this clause was specified against.
     * @param predicate2evaluatorFactory the factory handing out evaluators against TokenPredicates.
     * Also holds state information about the current term/clause we are finding predicates against.
     * @return returns a UrlClauseImpl matching the term, left and right child clauses.
     * May be either newly created or reused.
     */
    public static UrlClauseImpl createUrlClause(
        final String term,
        final String field,
        final TokenEvaluatorFactory predicate2evaluatorFactory) {

        // update the factory with what the current term is
        predicate2evaluatorFactory.setCurrentTerm(term);
        // use helper method from AbstractLeafClause
        return (UrlClauseImpl) createClause(
                UrlClauseImpl.class,
                term,
                field,
                predicate2evaluatorFactory,
                PREDICATES_APPLICABLE, WEAK_CACHE);
    }

    /**
     * Create clause with the given term, known and possible predicates.
     * @param term the term (query string) for this clause.
     * @param field the field for this clause. <b>May be <code>null</code></b>.
     * @param knownPredicates the set of known predicates for this clause.
     * @param possiblePredicates the set of possible predicates for this clause.
     */
    protected UrlClauseImpl(
            final String term,
            final String field,
            final Set/*<Predicate>*/ knownPredicates,
            final Set/*<Predicate>*/ possiblePredicates) {

        super(term, field, knownPredicates, possiblePredicates);
    }

}
