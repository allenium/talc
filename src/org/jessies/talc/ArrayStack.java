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

/**
 * Java 6 offers ArrayDeque as its default stack type.
 * Before that, there was only Stack, which was ugly and inefficient.
 * We could use Stack, but it would be full of gotchas (such as iteration being backwards).
 * We can't use ArrayDeque until Java 6 is everywhere (GCJ already has ArrayDeque, but Mac OS is currently pure Java 5).
 * 
 * So, this class offers a safe, clear stack in the meantime.
 * It may become useful in the long term too, if we decide we want (say) lazy construction of the underlying ArrayList.
 */
public class ArrayStack<T> implements Iterable<T> {
    private ArrayList<Object> data = new ArrayList<Object>();
    
    public ArrayStack() {
    }
    
    public Iterator<T> iterator() {
        return new ArrayStackIterator();
    }
    
    private class ArrayStackIterator implements Iterator<T> {
        private int cursor = data.size() - 1;
        
        public boolean hasNext() {
            return (cursor >= 0);
        }
        
        @SuppressWarnings("unchecked")
        public T next() {
            return (T) data.get(cursor--);
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    @SuppressWarnings("unchecked")
    public T peek() {
        return (T) data.get(data.size() - 1);
    }
    
    @SuppressWarnings("unchecked")
    public T pop() {
        return (T) data.remove(data.size() - 1);
    }
    
    public void push(T item) {
        data.add(item);
    }
}
