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

import java.util.regex.*;

public class StringValue implements Value {
    private String value;
    
    public StringValue(String value) {
        this.value = value;
    }
    
    public StringValue(StringValue lhs, StringValue rhs) {
        this(lhs.toString() + rhs.toString());
    }
    
    public boolean equals(Object o) {
        if (o instanceof StringValue) {
            return value.equals(((StringValue) o).value);
        }
        return false;
    }
    
    public Value match(String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(value);
        if (m.find()) {
            return new MatchValue(m);
        }
        return null;
    }
    
    public int hashCode() {
        return value.hashCode();
    }
    
    public String toString() {
        return value;
    }
    
    public TalcType type() {
        return TalcType.STRING;
    }
}
