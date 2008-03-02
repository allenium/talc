/*
 * This file is part of Talc.
 * Copyright (C) 2008 Elliott Hughes <enh@jessies.org>.
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

/**
 * Makes defineClass and loadClass accessible.
 * This lets us conveniently store our generated classes in a ClassLoader,
 * and then get hold of them again when it's time to run one.
 */
public class TalcClassLoader extends ClassLoader {
    public TalcClassLoader() {
    }
    
    public Class<?> defineClass(String name, byte[] bytes) throws ClassFormatError {
        return super.defineClass(name, bytes, 0, bytes.length);
    }
    
    public Class<?> getClass(String name) throws ClassNotFoundException {
        return super.loadClass(name, true);
    }
}
