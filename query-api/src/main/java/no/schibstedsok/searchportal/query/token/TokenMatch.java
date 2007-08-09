/*
 * Copyright (2005-2007) Schibsted Søk AS
 * This file is part of SESAT.
 * You can use, redistribute, and/or modify it, under the terms of the SESAT License.
 * You should have received a copy of the SESAT License along with this program.  
 * If not, see https://dev.schibstedsok.no/confluence/display/SESAT/SESAT+License
 */
package no.schibstedsok.searchportal.query.token;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @version $Id$
 **/
public final class TokenMatch implements Comparable {

    private final String token;
    private final String match;
    private final Integer start;
    private final Integer end;
    private final Pattern matcher;
    /**
     * Holds value of property _touched.
     */
    private boolean touched = false;

    public TokenMatch(final String token, final String match, final int start, final int end) {
        this.token = token;
        this.match = match;
        this.start = Integer.valueOf(start);
        this.end = Integer.valueOf(end);
        // (^|\s) or ($|\s) is neccessary to avoid matching fragments of words.
        matcher = Pattern.compile("(^|\\s)" + match + "($|\\s)", RegExpEvaluatorFactory.REG_EXP_OPTIONS);
    }

    public int compareTo(final Object o) {
        final TokenMatch other = (TokenMatch) o;

        return start.compareTo(other.getStart());
    }

    /**
     * Get the start index.
     *
     * @return the end index.
     */
    public Integer getStart() {
        return start;
    }

    /**
     * Get the match.
     *
     * @return the match.
     */
    public String getMatch() {
        return match;
    }

    /**
     * Get the regular expression Matcher to use to find a sub-match.
     *
     * @return the match.
     */
    public Matcher getMatcher(final String string) {
        return matcher.matcher(string);
    }

    /**
     * Get the token.
     *
     * @return the token.
     */
    public String getToken() {
        return token;
    }

    /**
     * Get the end.
     *
     * @return the end.
     */
    public Integer getEnd() {
        return end;
    }

    /**
     * Getter for property touched.
     * @return Value of property touched.
     */
    public boolean isTouched() {
        return touched;
    }

    /**
     * Setter for property touched.
     * @param touched New value of property touched.
     */
    public void setTouched(final boolean touched) {
        this.touched = touched;
    }
}
