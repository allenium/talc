/*
 * This file is part of Talc.
 * Copyright (C) 2008 Elliott Hughes <enh@jessies.org>.
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
 * Each AstNode.VariableDefinition can have a VariableAccessor. The idea is
 * that the code generator uses a different implementation of this interface
 * for each kind of variable it needs to support (local slots and fields, for
 * the JVM code generator, say). When the code generator decides how and where
 * it's going to store a variable, it should create a VariableAccessor and
 * associate it with the AstNode.VariableDefinition. The VariableAccessor
 * should know how to generate the code to get/put the variable.
 * 
 * AstNode doesn't need to know about the details of the individual code
 * generators, and code generators don't need to worry about what kind of
 * variable they're dealing with at every reference to it.
 */
public interface VariableAccessor {
    public void emitGet();
    public void emitPut();
}
