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
    
    // If 'bignum' is null, this IntegerValue's value is 'fixnum'. Otherwise, it's 'bignum' and 'fixnum' is ignored.
    private long fixnum;
    private BigInteger bignum;
    
    // Internally, we often need to distinguish fixnum IntegerValues from bignum ones.
    boolean isBig() {
        return bignum != null;
    }
    
    // Internally, we often want to treat an IntegerValue as if it was a bignum, whether it is or not.
    private BigInteger big() {
        return (bignum != null) ? bignum : BigInteger.valueOf(fixnum);
    }
    
    // Used by JvmCodeGenerator. It is an error to call this on a bignum.
    long longValue() {
        return fixnum;
    }
    
    public static IntegerValue valueOf(long l) {
        if (l >= -128 && l <= 127) {
            return cache[CACHE_OFFSET + (int) l];
        }
        return new IntegerValue(l);
    }
    
    public IntegerValue(String digits, int base) {
        try {
            this.fixnum = Long.parseLong(digits, base);
        } catch (NumberFormatException ex) {
            this.bignum = new BigInteger(digits, base);
        }
    }
    
    private IntegerValue(long value) {
        this.fixnum = value;
    }
    
    private IntegerValue(BigInteger value) {
        this.bignum = value;
    }
    
    private static IntegerValue valueOf(BigInteger value) {
        // Collapse to a fixnum if possible.
        if ((value.bitLength() + 1) <= 64) {
            return new IntegerValue(value.longValue());
        }
        return new IntegerValue(value);
    }
    
    public IntegerValue abs() {
        if (isBig() || fixnum == Long.MIN_VALUE) {
            return new IntegerValue(big().abs());
        } else {
            return IntegerValue.valueOf(Math.abs(fixnum));
        }
    }
    
    public IntegerValue add(IntegerValue rhs) {
        if (isBig() || rhs.isBig()) {
            return IntegerValue.valueOf(big().add(rhs.big()));
        } else {
            final long a = fixnum;
            final long b = rhs.fixnum;
            final long c = a + b;
            if ((c ^ a) < 0 && (c ^ b) < 0) {
                return new IntegerValue(big().add(rhs.big()));
            } else {
                return IntegerValue.valueOf(c);
            }
        }
    }
    
    public IntegerValue subtract(IntegerValue rhs) {
        if (isBig() || rhs.isBig()) {
            return IntegerValue.valueOf(big().subtract(rhs.big()));
        } else {
            final long a = fixnum;
            final long b = rhs.fixnum;
            final long c = a - b;
            if ((c ^ a) < 0 && (c ^ ~b) < 0) {
                return new IntegerValue(big().subtract(rhs.big()));
            } else {
                return IntegerValue.valueOf(c);
            }
        }
    }
    
    public IntegerValue multiply(IntegerValue rhs) {
        if (isBig() || rhs.isBig()) {
            return IntegerValue.valueOf(big().multiply(rhs.big()));
        } else {
            final long a = fixnum;
            final long b = rhs.fixnum;
            final long c = a * b;
            if (a != 0 && (c / a) != b) {
                return new IntegerValue(big().multiply(rhs.big()));
            } else {
                return IntegerValue.valueOf(c);
            }
        }
    }
    
    public IntegerValue divide(IntegerValue rhs) {
        if (isBig() || rhs.isBig()) {
            return IntegerValue.valueOf(big().divide(rhs.big()));
        } else {
            return IntegerValue.valueOf(fixnum / rhs.fixnum);
        }
    }
    
    public IntegerValue mod(IntegerValue rhs) {
        if (isBig() || rhs.isBig()) {
            return IntegerValue.valueOf(big().mod(rhs.big()));
        } else {
            return IntegerValue.valueOf(fixnum % rhs.fixnum);
        }
    }
    
    public IntegerValue shiftLeft(IntegerValue rhs) {
        // FIXME: check that rhs not too large?
        if (isBig() || rhs.isBig()) {
            return new IntegerValue(big().shiftLeft(rhs.intValue()));
        } else {
            return IntegerValue.valueOf(fixnum << rhs.fixnum);
        }
    }
    
    public IntegerValue shiftRight(IntegerValue rhs) {
        // FIXME: check that rhs not too large?
        if (isBig() || rhs.isBig()) {
            return IntegerValue.valueOf(big().shiftRight(rhs.intValue()));
        } else {
            return IntegerValue.valueOf(fixnum >> rhs.fixnum);
        }
    }
    
    public IntegerValue pow(IntegerValue rhs) {
        // FIXME: check that rhs not too large?
        // FIXME: special-case small enough fixnums?
        return new IntegerValue(big().pow(rhs.intValue()));
    }
    
    public IntegerValue and(IntegerValue rhs) {
        if (isBig() || rhs.isBig()) {
            return IntegerValue.valueOf(big().and(rhs.big()));
        } else {
            return IntegerValue.valueOf(fixnum & rhs.fixnum);
        }
    }
    
    public IntegerValue not() {
        if (isBig()) {
            return IntegerValue.valueOf(bignum.not());
        } else {
            return IntegerValue.valueOf(~fixnum);
        }
    }
    
    public IntegerValue or(IntegerValue rhs) {
        if (isBig() || rhs.isBig()) {
            return IntegerValue.valueOf(big().or(rhs.big()));
        } else {
            return IntegerValue.valueOf(fixnum | rhs.fixnum);
        }
    }
    
    public IntegerValue xor(IntegerValue rhs) {
        if (isBig() || rhs.isBig()) {
            return IntegerValue.valueOf(big().xor(rhs.big()));
        } else {
            return IntegerValue.valueOf(fixnum ^ rhs.fixnum);
        }
    }
    
    public IntegerValue negate() {
        if (isBig() || fixnum == Long.MIN_VALUE) {
            return new IntegerValue(big().negate());
        } else {
            return IntegerValue.valueOf(-fixnum);
        }
    }
    
    public IntegerValue decrement() {
        if (isBig() || fixnum == Long.MIN_VALUE) {
            return new IntegerValue(big().subtract(BigInteger.ONE));
        } else {
            return IntegerValue.valueOf(fixnum - 1);
        }
    }
    
    public IntegerValue increment() {
        if (isBig() || fixnum == Long.MAX_VALUE) {
            return new IntegerValue(big().add(BigInteger.ONE));
        } else {
            return IntegerValue.valueOf(fixnum + 1);
        }
    }
    
    /**
     * Returns -1, 0 or 1 if this IntegerValue is less than, equal to, or greater than rhs.
     * The suggested idiom for performing any boolean comparison 'op' is: (x.compareTo(y) op 0).
     */
    public int compareTo(IntegerValue rhs) {
        if (isBig() || rhs.isBig()) {
            return big().compareTo(rhs.big());
        } else {
            if (fixnum < rhs.fixnum) {
                return -1;
            } else if (fixnum == rhs.fixnum) {
                return 0;
            } else {
                return 1;
            }
        }
    }
    
    public IntegerValue signum() {
        if (isBig()) {
            return IntegerValue.valueOf(big().signum());
        } else {
            return IntegerValue.valueOf(Long.signum(fixnum));
        }
    }
    
    @Override public boolean equals(Object o) {
        if (o instanceof IntegerValue == false) {
            return false;
        }
        final IntegerValue rhs = (IntegerValue) o;
        // We normalize in valueOf, so we know that equal values must either both be big or both be fix.
        if (isBig() || rhs.isBig()) {
            return big().equals(rhs.big());
        } else {
            return fixnum == rhs.fixnum;
        }
    }
    
    @Override public int hashCode() {
        if (isBig()) {
            return big().hashCode();
        } else {
            return (int)(fixnum ^ (fixnum >>> 32));
        }
    }
    
    public IntegerValue factorial() {
        final BigInteger n = big();
        if (n.signum() < 0) {
            throw new IllegalArgumentException("factorial requires a non-negative integer argument; got " + this + " instead");
        }
        BigInteger result = BigInteger.ONE;
        BigInteger i = BigInteger.ONE;
        while (i.compareTo(n) < 0) {
            i = i.add(BigInteger.ONE);
            result = result.multiply(i);
        }
        return IntegerValue.valueOf(result);
    }
    
    public Object toNativeJavaObject() {
        // FIXME: this isn't necessarily appropriate. We might want to throw an exception instead.
        if (isBig()) {
            return big();
        } else {
            return Long.valueOf(fixnum);
        }
    }
    
    public int intValue() {
        return isBig() ? big().intValue() : (int) fixnum;
    }
    
    public IntegerValue to_i() {
        return this;
    }
    
    /** Returns the equivalent RealValue, or throws an exception. */
    public RealValue to_r() {
        double result = isBig() ? big().doubleValue() : (double) fixnum;
        if (result == Double.NEGATIVE_INFINITY || result == Double.POSITIVE_INFINITY) {
            throw new RuntimeException("Integer value too large");
        }
        return new RealValue(result);
    }
    
    public String toString() {
        if (isBig()) {
            return big().toString();
        } else {
            return Long.toString(fixnum);
        }
    }
    
    public String to_base(IntegerValue base) {
        if (isBig() || base.isBig()) {
            return big().toString(base.intValue());
        } else {
            return Long.toString(fixnum, (int) base.fixnum);
        }
    }
    
    public String to_char() {
        if (isBig()) {
            return String.valueOf((char) intValue());
        } else {
            return String.valueOf((char) fixnum);
        }
    }
}
