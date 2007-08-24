/*
 * DataModelContext.java
 *
 * Created on 19 March 2007, 10:49
 *
 */

package no.sesat.search.datamodel;

/** Defines the context for consumers of a DataModel.
 *
 * @author <a href="mailto:mick@semb.wever.org">Mck</a>
 * @version <tt>$Id$</tt>
 */
public interface DataModelContext {
    /**
     * 
     * @return 
     */
    DataModel getDataModel();
}