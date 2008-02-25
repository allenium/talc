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

/**
 * Used to represent a type while parsing.
 * At that point, we don't necessarily have a TalcType handy, so we keep these around instead.
 * At a later point, when all TalcType instances should have been created, we can come back and work out what these were supposed to refer to.
 */
public class TalcTypeDescriptor {
    private TalcType actualType;
    private String typeName;
    private TalcTypeDescriptor keyTypeDescriptor;
    private TalcTypeDescriptor valueTypeDescriptor;
    
    public TalcTypeDescriptor(TalcType actualType) {
        this.actualType = actualType;
    }
    
    public TalcTypeDescriptor(String typeName) {
        this(typeName, null, null);
    }
    
    public TalcTypeDescriptor(String typeName, TalcTypeDescriptor keyTypeDescriptor, TalcTypeDescriptor valueTypeDescriptor) {
        this.typeName = typeName;
        this.keyTypeDescriptor = keyTypeDescriptor;
        this.valueTypeDescriptor = valueTypeDescriptor;
    }
    
    public TalcType type() {
        if (actualType == null) {
            // Look up the TalcType corresponding to this descriptor.
            actualType = TalcType.byName(typeName);
            if (keyTypeDescriptor != null || valueTypeDescriptor != null) {
                actualType = TalcType.instantiateType(actualType, keyTypeDescriptor.type(), (valueTypeDescriptor != null) ? valueTypeDescriptor.type() : null);
            }
        }
        return actualType;
    }
    
    public void setActualType(TalcType newActualType) {
        this.actualType = newActualType;
    }
    
    public String rawName() {
        return typeName;
    }
    
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (actualType != null) {
            result.append(actualType);
        } else if (typeName != null) {
            result.append(typeName);
            if (keyTypeDescriptor != null || valueTypeDescriptor != null) {
                result.append("<");
                if (keyTypeDescriptor != null) {
                    result.append(keyTypeDescriptor);
                }
                if (valueTypeDescriptor != null) {
                    if (keyTypeDescriptor != null) {
                        result.append(", ");
                    }
                    result.append(valueTypeDescriptor);
                }
                result.append(">");
            }
        } else {
            result.append("TalcTypeDescriptor[empty]");
        }
        return result.toString();
    }
}
