/*
 * MapDataObject.java
 *
 * Created on 23 January 2007, 13:31
 *
 */

package no.schibstedsok.searchportal.datamodel.generic;

import no.schibstedsok.searchportal.datamodel.generic.DataObject;
import no.schibstedsok.searchportal.datamodel.*;
import no.schibstedsok.searchportal.datamodel.generic.MapDataObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This helper class provides a utility implementation of the
 * no.schibstedsok.searchportal.datamodel.MapDataObject interface.
 * </p>
 * <p>
 * Since this class directly implements the MapDataObject interface, the class
 * can, and is intended to be used either by subclassing this implementation,
 * or via ad-hoc delegation of an instance of this class from another.
 * </p>
 *
 * <b>Synchronised implementation of MapDataObject</b>.
 * Uses a ReentrantReadWriteLock in a delegated HashMap in preference to a Hashtable to maximise performance.
 * Access to the whole map is through a Collections.unmodifiable(map) defensive copy.
 *
 * @author <a href="mailto:mick@semb.wever.org">Mck</a>
 * @version <tt>$Id$</tt>
 */
@DataObject
public class MapDataObjectSupport<V> implements MapDataObject<V>{

    private final Map<String,V> map = new HashMap<String,V>(){

        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        @Override
        public V get(final Object key){

            try{
                lock.readLock().lock();
                return super.get(key);

            }finally{
                lock.readLock().unlock();
            }
        }

        @Override
        public V put(final String key, final V value){

            try{
                lock.writeLock().lock();
                return super.put(key, value);

            }finally{
                lock.writeLock().unlock();
            }
        }
    };

    public MapDataObjectSupport(final Map<String,V> map){
        
        if( null != map ){
            this.map.putAll(map);
        }
    }

    public V getValue(final String key){

        return map.get(key);
    }

    public void setValue(final String key, final V value){

        map.put(key, value);
    }

    public Map<String, V> getValues() {

        //return Collections.unmodifiableMap(map);
        return map;
    }

}
