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

public class Variables {
    private HashMap<String, Value> variables;
    
    public Variables() {
        variables = new HashMap<String, Value>();
    }
    
    public Value defineVariable(String name, Value value) {
        return assignVariable(name, value);
    }
    
    public Value assignVariable(String name, Value newValue) {
        variables.put(name, newValue);
        return newValue;
    }
    
    public boolean contains(String name) {
        return variables.containsKey(name);
    }
    
    public Value valueOf(String name) {
        return variables.get(name);
    }
}
