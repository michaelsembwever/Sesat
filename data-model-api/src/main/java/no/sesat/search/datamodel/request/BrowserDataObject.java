/* Copyright (2012) Schibsted ASA
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
/*
 * BrowserDataObject.java
 *
 * Created on 23 January 2007, 12:31
 *
 */

package no.sesat.search.datamodel.request;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import no.sesat.search.datamodel.generic.DataNode;
import no.sesat.search.datamodel.generic.StringDataObject;

/** Holds information regarding the user's browser.
 * Typically HTTP Request Headers.
 *
 *
 * @version <tt>$Id$</tt>
 */
@DataNode
public interface BrowserDataObject extends Serializable {

    /**
     *
     * @return
     */
    StringDataObject getUserAgent();
    /**
     *
     * @return
     */
    StringDataObject getRemoteAddr();
    /**
     *
     * @return
     */
    StringDataObject getForwardedFor();
    /**
     *
     * @return
     */
    Locale getLocale();
    /**
     *
     * @return
     */
    List<Locale> getSupportedLocales();

}
