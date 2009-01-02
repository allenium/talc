/*
 * This file is part of Talc.
 * Copyright (C) 2007-2009 Elliott Hughes <enh@jessies.org>.
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

public final class StringFunctions {
    private StringFunctions() {
    }
    
    public static String __get_item__(String s, IntegerValue index) {
        int i = index.intValue();
        return s.substring(i, i + 1);
    }
    
    public static BooleanValue contains(String s, String substring) {
        return BooleanValue.valueOf(s.contains(substring));
    }
    
    public static BooleanValue ends_with(String s, String suffix) {
        return BooleanValue.valueOf(s.endsWith(suffix));
    }
    
    public static String escape_html(String s) {
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace(">", "&gt;").replace("<", "&lt;");
    }
    
    private static Object translateToNativeJavaObject(Object o) {
        if (o instanceof IntegerValue) {
            return ((IntegerValue) o).toNativeJavaObject();
        } else if (o instanceof RealValue) {
            return ((RealValue) o).toNativeJavaObject();
        }
        return o;
    }
    
    public static String format(String s, Object[] values) {
        // We know that the 'values' array is a compiler-generated temporary.
        // As long as Object[] isn't representable in Talc, this is safe.
        for (int i = 0; i < values.length; ++i) {
            values[i] = translateToNativeJavaObject(values[i]);
        }
        return String.format(s, values);
    }
    
    public static String gsub(String s, String pattern, String replacement) {
        return s.replaceAll(pattern, replacement);
    }
    
    public static String lc(String s) {
        return s.toLowerCase();
    }
    
    public static String lc_first(String s) {
        return s.toLowerCase().substring(0, 1) + s.substring(1);
    }
    
    public static IntegerValue size(String s) {
        return IntegerValue.valueOf(s.length());
    }
    
    public static MatchValue match(String s, String pattern) {
        final Pattern p = Pattern.compile(pattern);
        final Matcher m = p.matcher(s);
        if (m.find()) {
            return new MatchValue(m);
        }
        return null;
    }
    
    // FIXME: it would be better to return an iterable, so we don't have to precompute everything.
    public static ListValue match_all(String s, String pattern) {
        final ListValue result = new ListValue();
        final Pattern p = Pattern.compile(pattern);
        final Matcher m = p.matcher(s);
        while (m.find()) {
            result.push_back(new MatchValue(m));
        }
        return result;
    }
    
    // Why "repeat"? Most languages use operator*, but Talc's not going to.
    // Ruby uses "dup" to mean something more like strdup(3), and "times" sounds like baby-talk for operator*.
    public static String repeat(String s, IntegerValue count) {
        final int n = count.intValue();
        final StringBuilder result = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; ++i) {
            result.append(s);
        }
        return result.toString();
    }
    
    public static String replace(String s, String oldSubstring, String newSubstring) {
        return s.replace(oldSubstring, newSubstring);
    }
    
    public static ListValue split(String s, String pattern) {
        return new ListValue(s.split(pattern));
    }
    
    public static BooleanValue starts_with(String s, String prefix) {
        return BooleanValue.valueOf(s.startsWith(prefix));
    }
    
    public static String sub(String s, String pattern, String replacement) {
        return s.replaceFirst(pattern, replacement);
    }
    
    public static IntegerValue to_i(String s) {
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
    
    public static RealValue to_r(String s) {
        return new RealValue(s);
    }
    
    public static String trim(String s) {
        return s.trim();
    }
    
    public static String uc(String s) {
        return s.toUpperCase();
    }
    
    public static String uc_first(String s) {
        return s.toUpperCase().substring(0, 1) + s.substring(1);
    }
}
