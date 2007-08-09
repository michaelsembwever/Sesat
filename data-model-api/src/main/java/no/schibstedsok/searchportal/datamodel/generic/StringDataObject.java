/* Copyright (2007) Schibsted Søk AS
 * This file is part of SESAT.
 * You can use, redistribute, and/or modify it, under the terms of the SESAT License.
 * You should have received a copy of the SESAT License along with this program.  
 * If not, see https://dev.schibstedsok.no/confluence/display/SESAT/SESAT+License
 */
/*
 * StringDataObject.java
 *
 * Created on 23 January 2007, 12:43
 *
 */

package no.schibstedsok.searchportal.datamodel.generic;

import java.io.Serializable;
import no.schibstedsok.searchportal.datamodel.generic.DataObject;
import no.schibstedsok.searchportal.datamodel.*;

/**
 *
 * @author <a href="mailto:mick@semb.wever.org">Mck</a>
 * @version <tt>$Id$</tt>
 */
@DataObject
public interface StringDataObject extends Serializable{
    /**
     * 
     * @return 
     */
    String getString();
    /**
     * 
     * @return 
     */
    String getUtf8UrlEncoded();
    /**
     * 
     * @return 
     */
    String getIso88591UrlEncoded();
    /**
     * 
     * @return 
     */
    String getXmlEscaped();
}
