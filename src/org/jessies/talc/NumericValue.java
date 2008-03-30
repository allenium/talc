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

public interface NumericValue {
    public NumericValue add(NumericValue rhs);
    public NumericValue subtract(NumericValue rhs);
    public NumericValue multiply(NumericValue rhs);
    public NumericValue divide(NumericValue rhs);
    public NumericValue pow(NumericValue rhs);
    public NumericValue negate();
    
    public NumericValue decrement();
    public NumericValue increment();
    
    /** Returns the equivalent IntegerValue, or throws an exception. */
    public IntegerValue to_i();
    /** Returns the equivalent RealValue, or throws an exception. */
    public RealValue to_r();
    
    /**
     * Returns -1, 0 or 1 if this NumericValue is less than, equal to, or greater than rhs.
     * The suggested idiom for performing any boolean comparison 'op' is: (x.compareTo(y) op 0).
     */
    public int compareTo(NumericValue rhs);
}
