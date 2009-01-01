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

public class MatchValue {
    private final MatchResult match;
    
    public MatchValue(Matcher matcher) {
        this.match = matcher.toMatchResult();
    }
    
    @Override public boolean equals(Object o) {
        if (o instanceof MatchValue) {
            return match.equals(((MatchValue) o).match);
        }
        return false;
    }
    
    public String group(IntegerValue i) {
        return match.group(i.intValue());
    }
    
    public IntegerValue groupCount() {
        return IntegerValue.valueOf(match.groupCount());
    }
    
    @Override public int hashCode() {
        return match.hashCode();
    }
    
    public String toString() {
        return match.toString();
    }
}
