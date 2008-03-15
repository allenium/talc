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

public class MapValue implements Value {
    private HashMap<Value, Value> map = new HashMap<Value, Value>();
    
    public MapValue() {
    }
    
    public Value __get_item__(Value key) {
        return map.get(key);
    }
    
    public Value __set_item__(Value key, Value value) {
        return map.put(key, value);
    }
    
    public MapValue clear() {
        map.clear();
        return this;
    }
    
    public BooleanValue has_key(Value key) {
        return BooleanValue.valueOf(map.containsKey(key));
    }
    
    public BooleanValue has_value(Value value) {
        return BooleanValue.valueOf(map.containsValue(value));
    }
    
    public ListValue keys() {
        return new ListValue(map.keySet());
    }
    
    public IntegerValue length() {
        return new IntegerValue(map.size());
    }
    
    public MapValue remove(Value key) {
        map.remove(key);
        return this;
    }
    
    public TalcType type() {
        return TalcType.MAP_OF_K_V;
    }
    
    public String toString() {
        return map.toString();
    }
    
    public ListValue values() {
        return new ListValue(map.values());
    }
}
