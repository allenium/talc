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

public class RealValue implements NumericValue {
    public static RealValue ONE = new RealValue(1.0);
    
    private Double value;
    
    public RealValue(String digits) {
        this.value = new Double(digits);
    }
    
    public RealValue(double value) {
        this.value = new Double(value);
    }
    
    public RealValue abs() {
        return new RealValue(Math.abs(value));
    }
    
    public NumericValue add(NumericValue rhs) {
        return new RealValue(value.doubleValue() + ((RealValue) rhs).value.doubleValue());
    }
    
    public NumericValue subtract(NumericValue rhs) {
        return new RealValue(value.doubleValue() - ((RealValue) rhs).value.doubleValue());
    }
    
    public NumericValue multiply(NumericValue rhs) {
        return new RealValue(value.doubleValue() * ((RealValue) rhs).value.doubleValue());
    }
    
    public NumericValue divide(NumericValue rhs) {
        return new RealValue(value.doubleValue() / ((RealValue) rhs).value.doubleValue());
    }
    
    public NumericValue pow(NumericValue rhs) {
        return new RealValue(Math.pow(value.doubleValue(), ((RealValue) rhs).doubleValue()));
    }
    
    public NumericValue negate() {
        return new RealValue(new Double(-value.doubleValue()));
    }
    
    public int compareTo(NumericValue rhs) {
        return value.compareTo(((RealValue) rhs).value);
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
            return value.equals(((RealValue) o).value);
        }
        return false;
    }
    
    public int hashCode() {
        return value.hashCode();
    }
    
    public double doubleValue() {
        return value.doubleValue();
    }
    
    public IntegerValue toIntegerValue() {
        return new IntegerValue((long) value.doubleValue());
    }
    
    public RealValue toRealValue() {
        return this;
    }
    
    public String toString() {
        return value.toString();
    }
    
    public TalcType type() {
        return TalcType.REAL;
    }
}
