/* Copyright (2005-2006) Schibsted Søk AS
 * OperationClause.java
 *
 * Created on 11 January 2006, 14:16
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package no.schibstedsok.front.searchportal.query;


/** An operation clause. Often a join between two other clauses, but can also be a prefix operator
 * to another term.
 *
 * @version $Id$
 * @author <a href="mailto:mick@wever.org">Michael Semb Wever</a>
 */
public interface OperationClause extends Clause {
    /**
     * Get the clause.
     * 
     * 
     * @return the clause.
     */
    Clause getFirstClause();

}
