/* Copyright (2006-2007) Schibsted Søk AS
 * This file is part of SESAT.
 * You can use, redistribute, and/or modify it, under the terms of the SESAT License.
 * You should have received a copy of the SESAT License along with this program.  
 * If not, see https://dev.schibstedsok.no/confluence/display/SESAT/SESAT+License

 */
package no.schibstedsok.searchportal.query.transform;

import no.schibstedsok.searchportal.query.transform.AbstractQueryTransformerConfig.Controller;


/**
 * Add iypcompanyid to the front of the original query, which is a companyid.
 *
 * @author <a href="mailto:daniele@conduct.no">Daniel Engfeldt</a>
 * @version $Revision:$
 */
@Controller("CatalogueInfopageQueryTransformer")
public final class CatalogueInfopageQueryTransformerConfig extends AbstractQueryTransformerConfig {
}
