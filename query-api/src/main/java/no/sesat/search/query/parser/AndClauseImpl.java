/*
 * Copyright (2005-2012) Schibsted ASA
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
package no.sesat.search.query.parser;

import java.lang.ref.Reference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import no.sesat.commons.ref.ReferenceMap;
import no.sesat.search.query.AndClause;
import no.sesat.search.query.Clause;
import no.sesat.search.query.LeafClause;
import no.sesat.search.query.token.EvaluationState;
import no.sesat.search.query.token.TokenEvaluationEngine;
import no.sesat.search.query.token.TokenPredicate;
import no.sesat.search.site.Site;

/**
 * The AndClauseImpl represents a joining clause between two terms in the query.
 * For example: "term1 AND term2".
 * <b>Objects of this class are immutable</b>
 *
 *
 * @version $Id$
 */
public final class AndClauseImpl extends AbstractBinaryClause implements AndClause {

    private static final int WEAK_CACHE_INITIAL_CAPACITY = 2000;
    private static final float WEAK_CACHE_LOAD_FACTOR = 0.5f;
    private static final int WEAK_CACHE_CONCURRENCY_LEVEL = 16;

    /** Values are WeakReference object to AbstractClause.
     * Unsynchronized are there are no 'changing values', just existance or not of the AbstractClause in the system.
     */
    private static final Map<Site,ReferenceMap<String,AndClauseImpl>> WEAK_CACHE
            = new ConcurrentHashMap<Site,ReferenceMap<String,AndClauseImpl>>();

    /**
     * Creator method for AndClauseImpl objects. By avoiding the constructors,
     * and assuming all AndClauseImpl objects are immutable, we can keep track
     * (via a weak reference map) of instances already in use in this JVM and reuse
     * them.
     * The methods also allow a chunk of creation logic for the AndClauseImpl to be moved
     * out of the QueryParserImpl.jj file to here.
     *
     * @param first the left child clause of the operation clause we are about to create (or find).
     * @param second the right child clause of the operation clause we are about to create (or find).
     * @param engine the factory handing out evaluators against TokenPredicates.
     * Also holds state information about the current term/clause we are finding predicates against.
     * @return returns a AndAndClauseImplstance matching the term, left and right child clauses.
     * May be either newly created or reused.
     */
    public static AndClauseImpl createAndClause(
        final Clause first,
        final Clause second,
        final TokenEvaluationEngine engine) {

        // construct the proper "schibstedsøk" formatted term for this operation.
        //  XXX eventually it would be nice not to have to expose the internal string representation of this object.
        final String term =
                (first instanceof LeafClause && ((LeafClause) first).getField() != null
                    ?  ((LeafClause) first).getField() + ':'
                    : "")
                + first.getTerm()
                + " AND "
                + (second instanceof LeafClause && ((LeafClause) second).getField() != null
                    ?  ((LeafClause) second).getField() + ':'
                    : "")
                + second.getTerm();

        try{
            // create predicate sets
            engine.setState(new EvaluationState(term, new HashSet<TokenPredicate>(), new HashSet<TokenPredicate>()));

            final String unique = '(' + term + ')';

            // the weakCache to use.
            ReferenceMap<String,AndClauseImpl> weakCache = WEAK_CACHE.get(engine.getSite());
            if(weakCache == null){

                weakCache = new ReferenceMap<String,AndClauseImpl>(
                    DFAULT_REFERENCE_MAP_TYPE,
                    new ConcurrentHashMap<String,Reference<AndClauseImpl>>(
                        WEAK_CACHE_INITIAL_CAPACITY,
                        WEAK_CACHE_LOAD_FACTOR,
                        WEAK_CACHE_CONCURRENCY_LEVEL));

                WEAK_CACHE.put(engine.getSite(), weakCache);
            }

            // use helper method from AbstractLeafClause
            return createClause(
                    AndClauseImpl.class,
                    unique,
                    first,
                    second,
                    engine,
                    weakCache);

        }finally{
            engine.setState(null);
        }
    }

    /**
     * Create clause with the given term, field, known and possible predicates.
     * @param term the term (query string) for this clause.
     * @param first the left child clause
     * @param second the right child clause
     * @param knownPredicates the set of known predicates for this clause.
     * @param possiblePredicates the set of possible predicates for this clause.
     */
    protected AndClauseImpl(
            final String term,
            final Clause first,
            final Clause second,
            final Set<TokenPredicate> knownPredicates,
            final Set<TokenPredicate> possiblePredicates) {

        super(term, first, second, knownPredicates, possiblePredicates);
    }
}
