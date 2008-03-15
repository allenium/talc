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

public class MatchValue implements Value {
    private Matcher matcher;
    
    public MatchValue(Matcher matcher) {
        this.matcher = matcher;
    }
    
    public boolean equals(Object o) {
        if (o instanceof MatchValue) {
            return matcher.equals(((MatchValue) o).matcher);
        }
        return false;
    }
    
    public StringValue group(IntegerValue i) {
        return new StringValue(matcher.group(i.intValue()));
    }
    
    public IntegerValue groupCount() {
        return new IntegerValue(matcher.groupCount());
    }
    
    public int hashCode() {
        return matcher.hashCode();
    }
    
    public String toString() {
        return matcher.toString();
    }
    
    public TalcType type() {
        return TalcType.MATCH;
    }
}
