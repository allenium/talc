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

public class BooleanValue implements Value {
    public static final BooleanValue TRUE = new BooleanValue();
    public static final BooleanValue FALSE = new BooleanValue();
    
    private BooleanValue() {
    }
    
    public static BooleanValue valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }
    
    public BooleanValue not() {
        return (booleanValue() ? FALSE : TRUE);
    }
    
    public boolean booleanValue() {
        return (this == TRUE);
    }
    
    public String toString() {
        return (booleanValue() ? "true" : "false");
    }
    
    public TalcType type() {
        return TalcType.BOOL;
    }
}
