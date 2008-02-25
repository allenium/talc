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

public class InvalidArgumentsException extends RuntimeException {
    public InvalidArgumentsException(Token op, Value... arguments) {
        super(makeMessage(op, arguments));
    }
    
    private static String makeMessage(Token op, Value[] arguments) {
        StringBuilder result = new StringBuilder();
        result.append("Invalid arguments to " + op + ": ");
        for (int i = 0; i < arguments.length; ++i) {
            if (i > 0) result.append(", ");
            result.append(arguments[i]);
        }
        return result.toString();
    }
}
