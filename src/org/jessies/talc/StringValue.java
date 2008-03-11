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
    
    public BooleanValue contains(StringValue substring) {
        return BooleanValue.valueOf(value.contains(substring.value));
    }
    
    public BooleanValue ends_with(StringValue suffix) {
        return BooleanValue.valueOf(value.endsWith(suffix.value));
    }
    
    public boolean equals(Object o) {
        if (o instanceof StringValue) {
            return value.equals(((StringValue) o).value);
        }
        return false;
    }
    
    public StringValue escape_html() {
        return new StringValue(value.replace("&", "&amp;").replace("\"", "&quot;").replace(">", "&gt;").replace("<", "&lt;"));
    }
    
    public StringValue gsub(StringValue pattern, StringValue replacement) {
        return new StringValue(value.replaceAll(pattern.value, replacement.value));
    }
    
    public StringValue lc() {
        return new StringValue(value.toLowerCase());
    }
    
    public StringValue lc_first() {
        return new StringValue(value.toLowerCase().substring(0, 1) + value.substring(1));
    }
    
    public IntegerValue length() {
        return new IntegerValue(value.length());
    }
    
    public MatchValue match(StringValue pattern) {
        Pattern p = Pattern.compile(pattern.value);
        Matcher m = p.matcher(value);
        if (m.find()) {
            return new MatchValue(m);
        }
        return null;
    }
    
    public int hashCode() {
        return value.hashCode();
    }
    
    public StringValue replace(StringValue oldSubstring, StringValue newSubstring) {
        return new StringValue(value.replace(oldSubstring.value, newSubstring.value));
    }
    
    public ListValue split(StringValue pattern) {
        return new ListValue(value.split(pattern.value));
    }
    
    public BooleanValue starts_with(StringValue prefix) {
        return BooleanValue.valueOf(value.startsWith(prefix.value));
    }
    
    public StringValue sub(StringValue pattern, StringValue replacement) {
        return new StringValue(value.replaceFirst(pattern.value, replacement.value));
    }
    
    public String toString() {
        return value;
    }
    
    public IntegerValue to_i() {
        String s = value;
        int base = 10;
        if (s.startsWith("0x")) {
            base = 16;
            s = s.substring(2);
        } else if (s.startsWith("0b")) {
            base = 2;
            s = s.substring(2);
        } else if (s.startsWith("0o")) {
            base = 8;
            s = s.substring(2);
        }
        return new IntegerValue(s, base);
    }
    
    public RealValue to_r() {
        return new RealValue(value);
    }
    
    public StringValue trim() {
        return new StringValue(value.trim());
    }
    
    public TalcType type() {
        return TalcType.STRING;
    }
    
    public StringValue uc() {
        return new StringValue(value.toUpperCase());
    }
    
    public StringValue uc_first() {
        return new StringValue(value.toUpperCase().substring(0, 1) + value.substring(1));
    }
}
