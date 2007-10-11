/* Copyright (2007) Schibsted Søk AS
 *   This file is part of SESAT.
 *
 *   SESAT is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   SESAT is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with SESAT.  If not, see <http://www.gnu.org/licenses/>.
 *
 * ResourceLoadException.java
 *
 * Created on October 23, 2006, 12:31 PM
 */

package no.sesat.search.site.config;

/**
 *
 * @author mick
 */
public final class ResourceLoadException extends RuntimeException{ // TODO this is not a RuntimeException!
    
    /** Creates a new instance of ResourceLoadException */
    private ResourceLoadException() {
    }
    
    /** Creates a new instance of ResourceLoadException */
    public ResourceLoadException(final String msg) {
        super(msg);
    }
    
    /** Creates a new instance of ResourceLoadException */
    public ResourceLoadException(final String msg, final Throwable th) {
        super(msg, th);
    }
    
}
