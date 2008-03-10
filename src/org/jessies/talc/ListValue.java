/*
 * This file is part of Talc.
 * Copyright (C) 2007 Elliott Hughes <enh@jessies.org>.
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

public class ListValue implements Value {
    private ArrayList<Value> list = new ArrayList<Value>();
    
    public ListValue() {
    }
    
    // list<string> is popular!
    public ListValue(String[] strings) {
        list.ensureCapacity(strings.length);
        for (String string : strings) {
            push_back(new StringValue(string));
        }
    }
    
    public ListValue(Collection<? extends Value> collection) {
        list.addAll(collection);
    }
    
    public ListValue add_all(ListValue others) {
        list.addAll(others.list);
        return this;
    }
    
    public ListValue clear() {
        list.clear();
        return this;
    }
    
    public boolean contains(Value v) {
        return list.contains(v);
    }
    
    public Value __get_item__(IntegerValue i) {
        return list.get(i.intValue());
    }
    
    public BooleanValue is_empty() {
        return BooleanValue.valueOf(list.size() == 0);
    }
    
    public String join(String separator) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < list.size(); ++i) {
            if (i > 0) {
                result.append(separator);
            }
            result.append(list.get(i));
        }
        return result.toString();
    }
    
    public IntegerValue length() {
        return new IntegerValue(list.size());
    }
    
    public Value peek_back() {
        return list.get(list.size() - 1);
    }
    
    public Value peek_front() {
        return list.get(0);
    }
    
    public Value pop_back() {
        return remove_at(list.size() - 1);
    }
    
    public Value pop_front() {
        return remove_at(0);
    }
    
    public ListValue push_back(Value v) {
        list.add(v);
        return this;
    }
    
    public ListValue push_front(Value v) {
        list.add(0, v);
        return this;
    }
    
    public void put(int i, Value v) {
        list.set(i, v);
    }
    
    public ListValue remove_all(ListValue others) {
        list.removeAll(others.list);
        return this;
    }
    
    public Value remove_at(int index) {
        return list.remove(index);
    }
    
    public boolean remove_first(Value v) {
        return list.remove(v);
    }
    
    public ListValue reverse() {
        ListValue result = new ListValue();
        for (int i = list.size() - 1; i >= 0; --i) {
            result.push_back(list.get(i));
        }
        return result;
    }
    
    public ListValue sort() {
        ListValue result = new ListValue();
        result.add_all(this);
        Collections.sort(result.list, new Comparator<Value>() {
            public int compare(Value o1, Value o2) {
                if (o1 instanceof IntegerValue) {
                    return ((IntegerValue) o1).compareTo((IntegerValue) o2);
                } else if (o1 instanceof RealValue) {
                    return ((RealValue) o1).compareTo((RealValue) o2);
                } else {
                    // FIXME: is this a good idea?
                    return o1.toString().compareTo(o2.toString());
                }
            }
            
            public boolean equals(Object o) {
                return false;
            }
        });
        return result;
    }
    
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("[");
        result.append(join(", "));
        result.append("]");
        return result.toString();
    }
    
    public TalcType type() {
        return TalcType.LIST_OF_T;
    }
    
    public ListValue uniq() {
        ListValue result = new ListValue();
        result.list.addAll(new LinkedHashSet<Value>(list));
        return result;
    }
}
