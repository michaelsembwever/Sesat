/* Copyright (2006-2007) Schibsted Søk AS
 * This file is part of SESAT.
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
 */
package no.sesat.search.view.velocity;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.log4j.Logger;

import java.io.Writer;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;
import no.sesat.search.result.ResultList;

/**
 * Handles presenting the hit count number.
 * Locale dependant number formatting for positive numbers, and
 * Useful presentation for negative (error) numbers.
 * 
 * The argument must be a ResultList instance.
 *
 * @author mick
 * @version $Id$
 */
public final class HitCountDirective extends AbstractDirective {

    private static final Logger LOG = Logger.getLogger(HitCountDirective.class);

    private static final String NAME = "hitcount";

    /**
      * {@inheritDoc}
      */
    public String getName() {
        return NAME;
    }

    /**
      * {@inheritDoc}
      */
    public int getType() {
        return LINE;
    }

    /**
     * @param cxt The cxt.
     * @param writer The writer.
     * @param node The node.
     *
     * @return return true on success.
     *
     * @throws IOException
     * @throws ResourceNotFoundException
     * @throws ParseErrorException
     * @throws MethodInvocationException
     */
    public boolean render(final InternalContextAdapter cxt, final Writer writer, final Node node)
            throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {

        if (node.jjtGetNumChildren() < 1 || !(getObjectArgument(cxt, node, 0) instanceof ResultList)) {
            LOG.error("#" + getName() + " - missing or invalid argument");
            return false;
        }

        final ResultList results = (ResultList) getObjectArgument(cxt, node, 0);
        
        switch(results.getHitCount()){
            
            case -1:
                writer.append('?');
                break;
                
            default:
                final Format formatter 
                        = NumberFormat.getIntegerInstance(getDataModel(cxt).getSite().getSite().getLocale());
                
                writer.append(formatter.format(results.getHitCount()));
                break;
        }

        return true;
    }
}