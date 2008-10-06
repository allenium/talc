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

public class RealValue implements Comparable<RealValue> {
    private static final RealValue ZERO = new RealValue(0.0);
    private static final RealValue ONE = new RealValue(1.0);
    
    private final double value;
    
    public RealValue(String digits) {
        this.value = Double.parseDouble(digits);
    }
    
    public RealValue(double value) {
        this.value = value;
    }
    
    public RealValue abs() {
        return new RealValue(Math.abs(value));
    }
    
    public RealValue add(RealValue rhs) {
        return new RealValue(value + rhs.value);
    }
    
    public RealValue subtract(RealValue rhs) {
        return new RealValue(value - rhs.value);
    }
    
    public RealValue multiply(RealValue rhs) {
        return new RealValue(value * rhs.value);
    }
    
    public RealValue divide(RealValue rhs) {
        return new RealValue(value / rhs.value);
    }
    
    public RealValue pow(RealValue rhs) {
        return new RealValue(Math.pow(value, rhs.value));
    }
    
    public RealValue negate() {
        return new RealValue(-value);
    }
    
    public RealValue decrement() {
        return new RealValue(value - 1.0);
    }
    
    public RealValue increment() {
        return new RealValue(value + 1.0);
    }
    
    /**
     * Returns -1, 0 or 1 if this RealValue is less than, equal to, or greater than rhs.
     * The suggested idiom for performing any boolean comparison 'op' is: (x.compareTo(y) op 0).
     */
    public int compareTo(RealValue rhs) {
        return Double.compare(value, rhs.value);
    }
    
    public RealValue cbrt() {
        return new RealValue(Math.cbrt(value));
    }
    
    public RealValue log(RealValue base) {
        return new RealValue(Math.log(value) / Math.log(base.value));
    }
    
    public RealValue log10() {
        return new RealValue(Math.log10(value));
    }
    
    public RealValue logE() {
        return new RealValue(Math.log(value));
    }
    
    public RealValue signum() {
        return new RealValue(Math.signum(value));
    }
    
    public RealValue sqrt() {
        return new RealValue(Math.sqrt(value));
    }
    
    @Override public boolean equals(Object o) {
        if (o instanceof RealValue) {
            return Double.doubleToLongBits(value) == Double.doubleToLongBits(((RealValue) o).value);
        }
        return false;
    }
    
    // Match java.lang.Double's behavior.
    @Override public int hashCode() {
        long bits = Double.doubleToLongBits(value);
        return (int)(bits ^ (bits >>> 32));
    }
    
    public Object toNativeJavaObject() {
        return value;
    }
    
    public double doubleValue() {
        return value;
    }
    
    public static RealValue valueOf(double d) {
        if (d == 0.0) {
            return ZERO;
        } else if (d == 1.0) {
            return ONE;
        }
        return new RealValue(d);
    }
    
    /** Returns the equivalent IntegerValue, or throws an exception. */
    public IntegerValue to_i() {
        return IntegerValue.valueOf((long) value);
    }
    
    /** Returns the equivalent RealValue, or throws an exception. */
    public RealValue to_r() {
        return this;
    }
    
    public String toString() {
        return String.valueOf(value);
    }
}
