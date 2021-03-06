/* Copyright (2006-2012) Schibsted ASA
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
package no.sesat.search.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple implementation of a search result item.
 * Is not multi-thread safe.
 * Mutates on setter methods.
 * Delegates all fields (of all types) to the one map.
 *
 * Any field "recordid" is considered as a key to equality between result items.
 *
 *
 * @version <tt>$Id$</tt>
 */
public class BasicResultItem implements ResultItem {

    private static final String URL_KEY = "url";
    private static final String TITLE_KEY = "title";

    private final HashMap<String,Serializable> fields = new HashMap<String,Serializable>();
    private final Map<String,Serializable> fieldsReadOnly = Collections.unmodifiableMap(fields);

    /**
     *
     */
    public BasicResultItem(){}

    /**
     *
     * @param title
     * @param url
     */
    protected BasicResultItem(final String title, final String url){

        fields.put(TITLE_KEY, StringChopper.chop(title, -1));
        fields.put(URL_KEY, StringChopper.chop(url, -1));
    }

    /**
     *
     * @param copy
     */
    public BasicResultItem(final ResultItem copy){

       for(String fieldName : copy.getFieldNames()){
           fields.put(fieldName, copy.getObjectField(fieldName));
       }
    }

    /**
     *
     * @param field
     * @param value
     * @return
     */
    public BasicResultItem addField(final String field, final String value) {

        fields.put(field, StringChopper.chop(value, -1));
        return this;
    }

    /**
     *
     * @param field
     * @return
     */
    public String getField(final String field) {

        assert null == fields.get(field) || fields.get(field) instanceof String
                :  field + " is not a String. It is a "
                + (null != fields.get(field) ? fields.get(field).getClass().getSimpleName() : "null");

        final String fieldValue = (String) fields.get(field);
        return fieldValue != null && fieldValue.trim().length() > 0 ? fieldValue : null;
    }

    /**
     * JavaBean standards access (read-only) to the fields map. Useful for JSPs.
     * @return the fields map, via Collections.unmodifiableMap(..)
     */
    public Map<String,Serializable> getFields(){
        return fieldsReadOnly;
    }

    /**
     *
     * @param field
     * @return
     */
    public Serializable getObjectField(final String field) {

        return fields.get(field);
    }

    /**
     *
     * @param field
     * @param value
     * @return
     */
    public BasicResultItem addObjectField(final String field, final Serializable value) {

        fields.put(field, value);
        return this;
    }

    /**
     *
     * @param field
     * @return
     */
    public Integer getInteger(final String field) {

        assert null == fields.get(field) || fields.get(field) instanceof Integer || fields.get(field) instanceof String
                :  field + " is not a Integer (or String). It is a "
                + (null != fields.get(field) ? fields.get(field).getClass().getSimpleName() : "null");

        return null != fields.get(field)
                ? fields.get(field) instanceof Integer
                    ? (Integer)fields.get(field)
                    : Integer.parseInt((String) fields.get(field))
                : null;
    }

    /**
     *
     * @param field
     * @param maxLength
     * @return
     */
    public String getField(final String field, final int maxLength) {

        assert null == fields.get(field) || fields.get(field) instanceof String
                :  field + " is not a String. It is a "
                + (null != fields.get(field) ? fields.get(field).getClass().getSimpleName() : "null");

        final String fieldValue = (String) fields.get(field);

        return fieldValue != null && fieldValue.trim().length() > 0
                ? StringChopper.chop(fieldValue, maxLength)
                : null;
    }

    /** Returns a defensive copy of the field names existing in this resultItem.
     *
     * @return
     */
    public Collection<String> getFieldNames() {

        return Collections.unmodifiableSet(fields.keySet());
    }

    /** Returns a live copy of the field's collection.
     *
     * If field is of String type, it is wrapped in a collection as a single entry.
     *
     * @param field
     * @return
     */
    public Collection<String> getMultivaluedField(final String field) {

        assert null == fields.get(field) || fields.get(field) instanceof Collection || fields.get(field) instanceof String
                :  field + " is not a Collection (or String). It is a "
                + (null != fields.get(field) ? fields.get(field).getClass().getSimpleName() : "null");

        return fields.get(field) instanceof Collection
                ? (Collection<String>) fields.get(field)
                : Collections.singletonList((String)fields.get(field));
    }

    /**
     *
     * @param field
     * @param value
     * @return
     */
    public BasicResultItem addToMultivaluedField(final String field, final String value) {

        if (! fields.containsKey(field)) {
            fields.put(field, new ArrayList<String>());
        }

        final Collection<String> previousValues = (Collection<String>) fields.get(field);
        previousValues.add(value);
        return this;
    }

    @Override
    public boolean equals(final Object obj) {

        boolean result = false;
        if( obj instanceof ResultItem ){
            final ResultItem other = (ResultItem) obj;

            if (other.getField("recordid") != null && getField("recordid") != null) {
                result = getField("recordid").equals(other.getField("recordid"));
            }else{
                result = true;
                for(String fieldName : other.getFieldNames()){
                    if (other.getObjectField(fieldName) == null) {
                        result &= null == getObjectField(fieldName);
                    } else {
                        result &= other.getObjectField(fieldName).equals(getObjectField(fieldName));
                    }
                }
            }
        }else{
            result = super.equals(obj);
        }
        return result;
    }

    @Override
    public int hashCode() {

        if (getField("recordid") != null) {
            return getField("recordid").hashCode();

        } else {
            // there nothing else to this object than the fields map.
            return fields.hashCode();
        }
    }

    public String getUrl() {

        return getField(URL_KEY);
    }

    public ResultItem setUrl(final String url) {

        return addField(URL_KEY, url);
    }

    public String getTitle() {

        return getField(TITLE_KEY);
    }

    public ResultItem setTitle(final String title) {

        return addField(TITLE_KEY, title);
    }

}
