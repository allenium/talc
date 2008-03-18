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

import java.util.*;

/**
 * Lets us pretend that built-in functions are user-defined functions with
 * empty bodies, so they take advantage of the normal compile-time checks.
 */ 
public class BuiltInFunction extends AstNode.FunctionDefinition {
    // The full monty: a function with a name, parameters, and a return type.
    public BuiltInFunction(String functionName, List<String> formalParameterNames, List<TalcType> formalParameterTypes, TalcType returnType) {
        super(null, functionName, formalParameterNames, formalParameterTypes, returnType, AstNode.Block.EMPTY_BLOCK);
    }
    
    // A common special case: a function with no parameters.
    public BuiltInFunction(String functionName, TalcType returnType) {
        this(functionName, Collections.<String>emptyList(), Collections.<TalcType>emptyList(), returnType);
    }
}
