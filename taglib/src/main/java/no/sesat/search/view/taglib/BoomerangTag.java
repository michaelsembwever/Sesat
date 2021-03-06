/* Copyright (2012) Schibsted ASA
 *   This file is part of Possom.
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
 *
 * BoomerangTag.java
 *
 * Created on May 27, 2006, 5:55 PM
 */

package no.sesat.search.view.taglib;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import no.sesat.search.datamodel.DataModel;
import no.sesat.search.result.Boomerang;
import no.sesat.search.site.Site;

/**
 *
 *
 * @version
 */

public final class BoomerangTag extends SimpleTagSupport {

    /**
     * Initialization of url property.
     */
    private String url;

    /**
     * Initialization of param property.
     */
    private String param;

    /**Called by the container to invoke this tag.
     * The implementation of this method is provided by the tag library developer,
     * and handles all tag processing, body iteration, etc.
     */
    public void doTag() throws JspException {

        final PageContext cxt = (PageContext) getJspContext();
        final JspWriter out = cxt.getOut();

        try {

            final JspFragment f = getJspBody();
            if (f != null){
                f.invoke(out);
            }

            final DataModel datamodel = (DataModel) cxt.findAttribute(DataModel.KEY);
            final Site site = datamodel.getSite().getSite();

            out.print(Boomerang.getUrl(site, url, param));

        }catch(Exception e){
            throw new JspException(e);
        }

    }

    /**
     * Setter for the url attribute.
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     * Setter for the param attribute.
     */
    public void setParam(String value) {
        this.param = value;
    }

}
