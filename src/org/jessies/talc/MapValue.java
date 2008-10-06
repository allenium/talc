/*
 * This file is part of Talc.
 * Copyright (C) 2007-2008 Elliott Hughes <enh@jessies.org>.
 * 
 * Talc is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Talc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jessies.talc;

import java.util.*;

public class MapValue {
    private final HashMap<Object, Object> map = new HashMap<Object, Object>();
    
    public MapValue() {
    }
    
    public Object __get_item__(Object key) {
        return map.get(key);
    }
    
    public Object __set_item__(Object key, Object value) {
        return map.put(key, value);
    }
    
    // Used by JvmCodeGenerator.visitMapLiteral to avoid a "dup" and "pop" per key/value pair.
    public MapValue __set_item__2(Object key, Object value) {
        map.put(key, value);
        return this;
    }
    
    public MapValue clear() {
        map.clear();
        return this;
    }
    
    @Override public boolean equals(Object o) {
        if (o instanceof MapValue) {
            return map.equals(((MapValue) o).map);
        }
        return false;
    }
    
    public BooleanValue has_key(Object key) {
        return BooleanValue.valueOf(map.containsKey(key));
    }
    
    public BooleanValue has_value(Object value) {
        return BooleanValue.valueOf(map.containsValue(value));
    }
    
    @Override public int hashCode() {
        return map.hashCode();
    }
    
    // Used in JvmCodeGenerator to implement for-each for maps.
    public Iterator<Object> keyIterator() {
        return map.keySet().iterator();
    }
    
    public ListValue keys() {
        return new ListValue(map.keySet());
    }
    
    public IntegerValue size() {
        return IntegerValue.valueOf(map.size());
    }
    
    public MapValue remove(Object key) {
        map.remove(key);
        return this;
    }
    
    public String toString() {
        // Adapted from AbstractMap.toString to use something closer to Talc's map literal syntax.
        // Given "puts([1:"one",2:"two",3:"three"]);" we want:
        //   [1:one, 2:two, 3:three]
        // rather than:
        //   {1=one, 2=two, 3=three}
        final Iterator<Map.Entry<Object, Object>> it = map.entrySet().iterator();
        if (!it.hasNext()) {
            return "[:]";
        }
        
        final StringBuilder result = new StringBuilder();
        result.append('[');
        while (it.hasNext()) {
            final Map.Entry<Object,Object> e = it.next();
            final Object key = e.getKey();
            final Object value = e.getValue();
            result.append((key == this) ? "(this map)" : key);
            result.append(':');
            result.append((value == this) ? "(this map)" : value);
            if (it.hasNext()) {
                result.append(", ");
            }
        }
        result.append(']');
        return result.toString();
    }
    
    public ListValue values() {
        return new ListValue(map.values());
    }
}
