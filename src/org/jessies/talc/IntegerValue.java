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

import java.math.*;

public class IntegerValue implements NumericValue {
    public static final IntegerValue ZERO = new IntegerValue(BigInteger.ZERO);
    public static final IntegerValue ONE = new IntegerValue(BigInteger.ONE);
    public static final IntegerValue NINE = new IntegerValue(BigInteger.valueOf(9));
    
    private BigInteger value;
    
    public IntegerValue(String digits, int base) {
        this.value = new BigInteger(digits, base);
    }
    
    public IntegerValue(BigInteger value) {
        this.value = value;
    }
    
    public IntegerValue(long value) {
        this.value = BigInteger.valueOf(value);
    }
    
    public IntegerValue abs() {
        return new IntegerValue(value.abs());
    }
    
    public NumericValue add(NumericValue rhs) {
        return new IntegerValue(value.add(((IntegerValue) rhs).value));
    }
    
    public NumericValue subtract(NumericValue rhs) {
        return new IntegerValue(value.subtract(((IntegerValue) rhs).value));
    }
    
    public NumericValue multiply(NumericValue rhs) {
        return new IntegerValue(value.multiply(((IntegerValue) rhs).value));
    }
    
    public NumericValue divide(NumericValue rhs) {
        return new IntegerValue(value.divide(((IntegerValue) rhs).value));
    }
    
    public IntegerValue mod(IntegerValue rhs) {
        return new IntegerValue(value.mod(rhs.value));
    }
    
    public IntegerValue shiftLeft(IntegerValue rhs) {
        // FIXME: check that rhs not too large.
        return new IntegerValue(value.shiftLeft(rhs.value.intValue()));
    }
    
    public IntegerValue shiftRight(IntegerValue rhs) {
        // FIXME: check that rhs not too large.
        return new IntegerValue(value.shiftRight(rhs.value.intValue()));
    }
    
    public NumericValue pow(NumericValue rhs) {
        // FIXME: check that rhs not too large.
        return new IntegerValue(value.pow(((IntegerValue) rhs).value.intValue()));
    }
    
    public IntegerValue and(IntegerValue rhs) {
        return new IntegerValue(value.and(rhs.value));
    }
    
    public IntegerValue not() {
        return new IntegerValue(value.not());
    }
    
    public IntegerValue or(IntegerValue rhs) {
        return new IntegerValue(value.or(rhs.value));
    }
    
    public IntegerValue xor(IntegerValue rhs) {
        return new IntegerValue(value.xor(rhs.value));
    }
    
    public NumericValue negate() {
        return new IntegerValue(value.negate());
    }
    
    public int compareTo(NumericValue rhs) {
        return value.compareTo(((IntegerValue) rhs).value);
    }
    
    public IntegerValue signum() {
        return new IntegerValue(value.signum());
    }
    
    public boolean equals(Object o) {
        if (o instanceof IntegerValue) {
            return value.equals(((IntegerValue) o).value);
        }
        return false;
    }
    
    public int hashCode() {
        return value.hashCode();
    }
    
    public IntegerValue factorial() {
        if (value.signum() < 0) {
            throw new IllegalArgumentException("factorial requires a non-negative integer argument; got " + this + " instead");
        }
        BigInteger n = value;
        BigInteger result = BigInteger.ONE;
        BigInteger i = BigInteger.ONE;
        while (i.compareTo(n) < 0) {
            i = i.add(BigInteger.ONE);
            result = result.multiply(i);
        }
        return new IntegerValue(result);
    }
    
    public int intValue() {
        return value.intValue();
    }
    
    public IntegerValue to_i() {
        return this;
    }
    
    public RealValue to_r() {
        double result = value.doubleValue();
        if (result == Double.NEGATIVE_INFINITY || result == Double.POSITIVE_INFINITY) {
            throw new RuntimeException("Integer value too large");
        }
        return new RealValue(result);
    }
    
    public String toString() {
        return value.toString();
    }
    
    public StringValue to_base(IntegerValue base) {
        return new StringValue(value.toString(base.intValue()));
    }
    
    public StringValue to_char() {
        return new StringValue(String.valueOf((char) intValue()));
    }
    
    public TalcType type() {
        return TalcType.INT;
    }
}
