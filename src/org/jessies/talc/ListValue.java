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

public class ListValue {
    private final ArrayList<Object> list = new ArrayList<Object>();
    
    public ListValue() {
    }
    
    // list<string> is popular!
    public ListValue(String[] strings) {
        list.ensureCapacity(strings.length);
        for (String string : strings) {
            push_back(string);
        }
    }
    
    public ListValue(Collection<? extends Object> collection) {
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
    
    public BooleanValue contains(Object v) {
        return BooleanValue.valueOf(list.contains(v));
    }
    
    @Override public boolean equals(Object o) {
        if (o instanceof ListValue) {
            return list.equals(((ListValue) o).list);
        }
        return false;
    }
    
    public Object __get_item__(IntegerValue i) {
        return list.get(i.intValue());
    }
    
    @Override public int hashCode() {
        return list.hashCode();
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
    
    public Object peek_back() {
        return list.get(list.size() - 1);
    }
    
    public Object peek_front() {
        return list.get(0);
    }
    
    public Object pop_back() {
        return list.remove(list.size() - 1);
    }
    
    public Object pop_front() {
        return list.remove(0);
    }
    
    public ListValue push_back(Object v) {
        list.add(v);
        return this;
    }
    
    public ListValue push_front(Object v) {
        list.add(0, v);
        return this;
    }
    
    public ListValue remove_all(ListValue others) {
        list.removeAll(others.list);
        return this;
    }
    
    public ListValue remove_at(IntegerValue index) {
        list.remove(index.intValue());
        return this;
    }
    
    public BooleanValue remove_first(Object v) {
        return BooleanValue.valueOf(list.remove(v));
    }
    
    public ListValue reverse() {
        ListValue result = new ListValue();
        for (int i = list.size() - 1; i >= 0; --i) {
            result.push_back(list.get(i));
        }
        return result;
    }
    
    public Object __set_item__(IntegerValue i, Object v) {
        list.set(i.intValue(), v);
        return v;
    }
    
    public IntegerValue size() {
        return IntegerValue.valueOf(list.size());
    }
    
    public ListValue sort() {
        ListValue result = new ListValue();
        result.add_all(this);
        Collections.sort(result.list, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
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
    
    public ListValue uniq() {
        ListValue result = new ListValue();
        result.list.addAll(new LinkedHashSet<Object>(list));
        return result;
    }
}
