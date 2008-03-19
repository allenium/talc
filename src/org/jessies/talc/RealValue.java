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

public class RealValue implements NumericValue {
    public static RealValue ZERO = new RealValue(0.0);
    public static RealValue ONE = new RealValue(1.0);
    
    private double value;
    
    public RealValue(String digits) {
        this.value = Double.parseDouble(digits);
    }
    
    public RealValue(double value) {
        this.value = value;
    }
    
    public RealValue abs() {
        return new RealValue(Math.abs(value));
    }
    
    public NumericValue add(NumericValue rhs) {
        return new RealValue(value + ((RealValue) rhs).value);
    }
    
    public NumericValue subtract(NumericValue rhs) {
        return new RealValue(value - ((RealValue) rhs).value);
    }
    
    public NumericValue multiply(NumericValue rhs) {
        return new RealValue(value * ((RealValue) rhs).value);
    }
    
    public NumericValue divide(NumericValue rhs) {
        return new RealValue(value / ((RealValue) rhs).value);
    }
    
    public NumericValue pow(NumericValue rhs) {
        return new RealValue(Math.pow(value, ((RealValue) rhs).value));
    }
    
    public NumericValue negate() {
        return new RealValue(-value);
    }
    
    public int compareTo(NumericValue rhs) {
        return Double.compare(value, ((RealValue) rhs).value);
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
    
    public boolean equals(Object o) {
        if (o instanceof RealValue) {
            return Double.doubleToLongBits(value) == Double.doubleToLongBits(((RealValue) o).value);
        }
        return false;
    }
    
    // Match java.lang.Double's behavior.
    public int hashCode() {
        long bits = Double.doubleToLongBits(value);
        return (int)(bits ^ (bits >>> 32));
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
    
    public IntegerValue to_i() {
        return new IntegerValue((long) value);
    }
    
    public RealValue to_r() {
        return this;
    }
    
    public String toString() {
        return String.valueOf(value);
    }
    
    public TalcType type() {
        return TalcType.REAL;
    }
}
