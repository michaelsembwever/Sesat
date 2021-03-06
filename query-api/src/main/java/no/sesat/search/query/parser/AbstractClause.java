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
 *
 * AbstractClause.java
 *
 * Created on 11 January 2006, 14:17
 *
 */

package no.sesat.search.query.parser;

import java.util.Collections;
import java.util.Set;
import no.sesat.commons.ref.ReferenceMap;
import no.sesat.search.query.Clause;
import no.sesat.commons.visitor.Visitor;
import no.sesat.search.query.token.DeadTokenEvaluationEngineImpl.DeadEvaluationRuntimeException;
import no.sesat.search.query.token.TokenEvaluator;
import no.sesat.search.query.token.TokenEvaluationEngine;
import no.sesat.search.query.token.TokenPredicate;
import no.sesat.search.query.token.EvaluationException;
import no.sesat.search.query.token.EvaluationRuntimeException;
import no.sesat.search.query.token.TokenPredicateUtility;
import org.apache.log4j.Logger;


/** Basic implementation of the Clause interface.
 * Provides basic implementation of the predicates lists, defaulting to empty lists.
 * Also provides weak reference hashmaps to keep record and reuse the Clauses already in use in the JVM.
 * <b>Objects of this class are immutable</b>
 *
 * @version $Id$
 *
 */
public abstract class AbstractClause implements Clause {

    private static final Logger LOG = Logger.getLogger(AbstractClause.class);
    /**
     * Error message when reflection cannot find the required constructor.
     */
    protected static final String ERR_FAILED_FINDING_OR_USING_CONSTRUCTOR
            = "Failed to find (or use) constructor with parameters (String, String, Set, Set) for class: ";
    /**
     * Error message when trying to use the incorrect constructor.
     **/
    protected static final String ERR_MUST_ALWAYS_USE_ARGED_CONSTRUCTOR
            = "Illegal to call constructor without arguments!";
    private static final String DEBUG_FOUND_PREDICATE_PREFIX = "Found (for \"";
    private static final String DEBUG_FOUND_PREDICATE_KNOWN = "\") known ";
    private static final String DEBUG_FOUND_PREDICATE_POSSIBLE = "\") possible ";
    private static final String INFO_WEAK_CACHE_SIZE_1 ="WeakCache for ";
    private static final String INFO_WEAK_CACHE_SIZE_2 =" at ";
    private static final String DEBUG_REFERENCE_REUSED = "Re-using a weakReference. Cache size: ";
    private static final String ERR_FAILED_TO_FIND_ALL_PREDICATES = "Failed to find all predicates."
            + " Marking token predicate stale >>";

    private final String term;
    private final Set<TokenPredicate> knownPredicates;
    private final Set<TokenPredicate> possiblePredicates;

    public static final ReferenceMap.Type DFAULT_REFERENCE_MAP_TYPE = ReferenceMap.Type.WEAK;

    /**
     * See if there is an identical and immutable Clause already in use in the JVM.
     * @param <T>
     * @param key the <B>unique</B> (for this AbstractClause subtype) key for the Clause we are looking for.
     * @param weakCache the map containing the key to WeakReference (of the Clause) mappings.
     * @return the AbstractClause in use already, matching the key. <B>May be <CODE>null</CODE></B>.
     */
    protected static final <T extends AbstractClause> T findClauseInUse(
            final String key,
            final ReferenceMap<String,T> weakCache) {

        T result = weakCache.get(key);

        return result;
    }

    /**
     * Note there is an identical and immutable Clause ready to use in the JVM.
     * @param <T>
     * @param key the <B>unique</B> (for this AbstractClause subtype) key
     *   for the Clause we are about to add to the mappings.
     * @param clause the Clause we are about to add to the mappings.
     * @param weakCache the map containing the key to WeakReference (of the Clause) mappings.
     * @return If the weakCache contained an clause for the key, then this is returned. Otherwise
     * the clasue entered as a parameter is returned.
     */
    protected static final <T extends AbstractClause> T addClauseInUse(
            final String key,
            final T clause,
            final ReferenceMap<String,T> weakCache) {

        T inUse = weakCache.get(key);

        if(null == inUse){
            // clause is not in use
            inUse = weakCache.put(key, clause);

            if(null != inUse){
                // weakCache.get(key) is only read volatile so it may be there was another clause already there
                //  and only the write volatile inside weakCache.put(key.calue) sees through this small window.

                // restore original clause that was in use
                weakCache.put(key, inUse);

            }else{
                inUse = clause;
            }
        }

        return inUse;
    }

    /**
     * Find the predicates that are applicable to the clause.
     * (Only the clause's term is known and is kept in state inside the TokenEvaluationEngine).
     * Add known predicates to <CODE>knownPredicates</CODE>.
     * Add possible (requires further checking against the whole query heirarchy)
     *   predicates to <CODE>possiblePredicates</CODE>.
     *
     * @param engine the factory handing out evaluators against TokenPredicates.
     * Also holds state information about the current term/clause we are finding predicates against.
     *   to the current clause we are finding predicates for.
     * @return
     */
    protected static final boolean findPredicates(final TokenEvaluationEngine engine) {

        boolean success = true;


        for (TokenPredicate token : TokenPredicateUtility.getTokenPredicates()) {

            success &= findPredicate(engine, token, success);
            success &= findPredicate(engine, token.exactPeer(), success);
        }

        return success;
    }

    private static final boolean findPredicate(
            final TokenEvaluationEngine engine,
            final TokenPredicate token,
            final boolean pastSuccess){

        boolean success = pastSuccess;

        final Set<TokenPredicate> knownPredicates = engine.getState().getKnownPredicates();
        final Set<TokenPredicate> possiblePredicates = engine.getState().getPossiblePredicates();
        final String currTerm = engine.getState().getTerm();

        // check it hasn't already been added
        if(!(knownPredicates.contains(token) || possiblePredicates.contains(token))){

            try{
                if (token.evaluate(engine)) {
                    final TokenEvaluator evaluator = engine.getEvaluator(token);
                    if (evaluator.isQueryDependant(token)) {
                        possiblePredicates.add(token);
                        LOG.debug(DEBUG_FOUND_PREDICATE_PREFIX + currTerm + DEBUG_FOUND_PREDICATE_POSSIBLE + token);
                    }  else  {
                        knownPredicates.add(token);
                        LOG.debug(DEBUG_FOUND_PREDICATE_PREFIX + currTerm + DEBUG_FOUND_PREDICATE_KNOWN + token);
                    }
                }
            }catch(EvaluationException ie){
                if(success){
                    success = false;
                    LOG.error(ERR_FAILED_TO_FIND_ALL_PREDICATES + currTerm);
                }
            }catch(DeadEvaluationRuntimeException dere){
                success |= false;
                // don't log this as it is intentional evaluation failure
            }catch(EvaluationRuntimeException ee){
                if(success){
                    success = false;
                    LOG.error(ERR_FAILED_TO_FIND_ALL_PREDICATES + currTerm);
                }
            }
        }
        return success;
    }

    /**
     * Create clause with the given term, known and possible predicates.
     * @param term the term (query string) for this clause.
     * @param knownPredicates the set of known predicates for this clause.
     * @param possiblePredicates the set of possible predicates for this clause.
     */
    protected AbstractClause(
            final String term,
            final Set<TokenPredicate> knownPredicates,
            final Set<TokenPredicate> possiblePredicates) {

        this.term = term;
        this.knownPredicates = Collections.unmodifiableSet(knownPredicates);
        this.possiblePredicates = Collections.unmodifiableSet(possiblePredicates);
    }

    /**
     * Get the term for this Clause.
     * Does not include any field values (eg "firstname:").
     * @return the term for this clause.
     */
    @Override
    public String getTerm() {
        return term;
    }

    /**
     * Get the set of knownPredicates for this Clause.
     * The set is unmodifiable.
     * @return set of knownPredicates.
     */
    @Override
    public Set<TokenPredicate> getKnownPredicates() {
        return knownPredicates;
    }

    /**
     * Get the set of possiblePredicates for this Clause.
     * The set is unmodifiable.
     * @return set of possiblePredicates.
     */
    @Override
    public Set<TokenPredicate> getPossiblePredicates() {
        return possiblePredicates;
    }


    @Override
    public void accept(final Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + getTerm() + ']';
    }

    /** Provide a replicatable hashCode so the same segment inside the ConcurrentHashMap
     * is used for any accidently duplicate created clauses.
     * The ConcurrentHashMap is used in this package's implementation of the flyweight pattern.
     *
     * It is intended for equals(..) to use instance reference equality, ie Object.equals(..).
     *
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {

        return ((getClass().hashCode() * 37) + term.hashCode()) + 17;
    }

}
