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

public class IntegerValue implements Comparable<IntegerValue> {
    // Cache common values, equivalent to what the JLS mandates for boxed integers in Java.
    private static final IntegerValue[] cache = new IntegerValue[-(-128) + 127 + 1];
    private static final int CACHE_OFFSET = 128;
    static {
        for(int i = 0; i < cache.length; ++i) {
            cache[i] = new IntegerValue(i - CACHE_OFFSET);
        }
    }
    
    private BigInteger value;
    
    public static IntegerValue valueOf(long l) {
        if (l >= -128 && l <= 127) {
            return cache[CACHE_OFFSET + (int) l];
        }
        return new IntegerValue(l);
    }
    
    public IntegerValue(String digits, int base) {
        this.value = new BigInteger(digits, base);
    }
    
    public IntegerValue(BigInteger value) {
        this.value = value;
    }
    
    private IntegerValue(long value) {
        this.value = BigInteger.valueOf(value);
    }
    
    public IntegerValue abs() {
        return new IntegerValue(value.abs());
    }
    
    public IntegerValue add(IntegerValue rhs) {
        return new IntegerValue(value.add(rhs.value));
    }
    
    public IntegerValue subtract(IntegerValue rhs) {
        return new IntegerValue(value.subtract(rhs.value));
    }
    
    public IntegerValue multiply(IntegerValue rhs) {
        return new IntegerValue(value.multiply(rhs.value));
    }
    
    public IntegerValue divide(IntegerValue rhs) {
        return new IntegerValue(value.divide(rhs.value));
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
    
    public IntegerValue pow(IntegerValue rhs) {
        // FIXME: check that rhs not too large.
        return new IntegerValue(value.pow(rhs.value.intValue()));
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
    
    public IntegerValue negate() {
        return new IntegerValue(value.negate());
    }
    
    public IntegerValue decrement() {
        return new IntegerValue(value.subtract(BigInteger.ONE));
    }
    
    public IntegerValue increment() {
        return new IntegerValue(value.add(BigInteger.ONE));
    }
    
    /**
     * Returns -1, 0 or 1 if this IntegerValue is less than, equal to, or greater than rhs.
     * The suggested idiom for performing any boolean comparison 'op' is: (x.compareTo(y) op 0).
     */
    public int compareTo(IntegerValue rhs) {
        return value.compareTo(rhs.value);
    }
    
    public IntegerValue signum() {
        return IntegerValue.valueOf(value.signum());
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
    
    // Used to implement for-each loops, so it's not unreasonable to assume the value will fit in 32 bits.
    public IntegerValue inc() {
        return IntegerValue.valueOf(value.intValue() + 1);
    }
    
    public int intValue() {
        return value.intValue();
    }
    
    /** Returns the equivalent IntegerValue, or throws an exception. */
    public IntegerValue to_i() {
        return this;
    }
    
    /** Returns the equivalent RealValue, or throws an exception. */
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
    
    public String to_base(IntegerValue base) {
        return value.toString(base.intValue());
    }
    
    public String to_char() {
        return String.valueOf((char) intValue());
    }
}
