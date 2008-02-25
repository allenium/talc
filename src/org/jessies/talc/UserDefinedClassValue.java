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

public class UserDefinedClassValue implements Value {
    private TalcType type;
    private HashMap<String, Value> fields;
    
    public UserDefinedClassValue(TalcType type) {
        this.type = type;
        // FIXME: we could optimize this in several ways. We know at compile-time how many fields there in "type". If we wanted, we could assign each an integer.
        this.fields = new HashMap<String, Value>();
    }
    
    public Value putField(String name, Value value) {
        fields.put(name, value);
        return value;
    }
    
    public Value getField(String name) {
        return fields.get(name);
    }
    
    public TalcType type() {
        return type;
    }
    
    public String toString() {
        StringBuilder result = new StringBuilder();
        // Imitate Java's default Object.toString for our own user-defined types.
        result.append(type);
        result.append("@");
        result.append(Integer.toHexString(hashCode()));
        // Go one better by automatically dumping all the fields.
        result.append(fields);
        return result.toString();
    }
}
