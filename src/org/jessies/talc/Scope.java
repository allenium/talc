/*
 * This file is part of Talc.
 * Copyright (C) 2007-2009 Elliott Hughes <enh@jessies.org>.
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

public class Scope {
    private static Scope builtInScope;
    private static Scope globalScope;
    
    private Scope parent;
    private HashMap<String, AstNode.FunctionDefinition> functions;
    private HashMap<String, AstNode.VariableDefinition> variables;
    
    public Scope(Scope parent) {
        this.parent = parent;
    }
    
    public void addFunction(AstNode.FunctionDefinition f) {
        if (functions == null) {
            functions = new HashMap<String, AstNode.FunctionDefinition>();
        }
        functions.put(f.functionName(), f);
    }
    
    public void addVariable(AstNode.VariableDefinition v) {
        v.setScope(this);
        if (variables == null) {
            variables = new HashMap<String, AstNode.VariableDefinition>();
        }
        AstNode.VariableDefinition oldDefinition = variables.get(v.identifier());
        if (oldDefinition != null) {
            throw new TalcError(v, "redefinition of \"" + v.identifier() + "\"...\n" + oldDefinition.location() + "...previously defined here");
        }
        variables.put(v.identifier(), v);
    }
    
    public AstNode.FunctionDefinition findFunction(String name) {
        if (functions != null) {
            AstNode.FunctionDefinition f = functions.get(name);
            if (f != null) {
                return f;
            }
        }
        if (parent != null) {
            return parent.findFunction(name);
        }
        // If there's no parent scope, we're either in the top of an
        // inheritance hierarchy, or the builtInScope. In the latter case,
        // there's no where left to look. But at the top of an inheritance
        // hierarchy, we want to fall back on the global scope.
        if (this != builtInScope) {
            return globalScope.findFunction(name);
        }
        return null;
    }
    
    public AstNode.VariableDefinition findVariable(String name) {
        if (variables != null) {
            AstNode.VariableDefinition v = variables.get(name);
            if (v != null) {
                return v;
            }
        }
        if (parent != null) {
            return parent.findVariable(name);
        }
        return null;
    }
    
    public String describeScope() {
        StringBuilder result = new StringBuilder();
        if (variables != null || functions != null) {
            result.append("\n");
        }
        if (variables != null) {
            TreeMap<String, AstNode.VariableDefinition> sortedVariables = new TreeMap<String, AstNode.VariableDefinition>(variables);
            for (AstNode.VariableDefinition variableDefinition : sortedVariables.values()) {
                result.append("  ");
                result.append(variableDefinition);
                result.append(";\n");
            }
        }
        if (functions != null) {
            if (variables != null) {
                result.append("\n");
            }
            TreeMap<String, AstNode.FunctionDefinition> sortedFunctions = new TreeMap<String, AstNode.FunctionDefinition>(functions);
            int constructorCount = 0;
            for (int pass = 0; pass < 2; ++pass) {
                if (constructorCount > 0) {
                    result.append("\n");
                }
                for (AstNode.FunctionDefinition functionDefinition : sortedFunctions.values()) {
                    // We dump the constructors on pass 0, everything else on pass 1.
                    if (functionDefinition.isConstructor() != (pass == 0)) {
                        continue;
                    }
                    String returnType = "";
                    String rest = functionDefinition.signature();
                    if (pass == 0) {
                        ++constructorCount;
                    } else {
                        final int spaceIndex = rest.indexOf(' ');
                        returnType = rest.substring(0, spaceIndex);
                        rest = rest.substring(spaceIndex + 1);
                    }
                    result.append(String.format("  %14s %s\n", returnType, rest));
                }
            }
        }
        return result.toString();
    }
    
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Scope == false) {
            return false;
        }
        Scope other = (Scope) o;
        return (fieldEquals(parent, other.parent) && fieldEquals(functions, other.functions) && fieldEquals(variables, other.variables));
    }
    
    private <T> boolean fieldEquals(T field, T otherField) {
        return (field == null ? otherField == null : field.equals(otherField));
    }
    
    public static Scope builtInScope() {
        return builtInScope;
    }
    
    public static Scope globalScope() {
        return globalScope;
    }
    
    public static void initGlobalScope(String argv0) {
        builtInScope = new Scope(null);
        // Built-in functions.
        builtInScope.addFunction(new BuiltInFunction("backquote", Arrays.asList("command"), Arrays.asList(TalcType.STRING), TalcType.STRING));
        builtInScope.addFunction(new BuiltInFunction("exit", Arrays.asList("status"), Arrays.asList(TalcType.INT), TalcType.VOID));
        builtInScope.addFunction(new BuiltInFunction("getenv", Arrays.asList("name"), Arrays.asList(TalcType.STRING), TalcType.STRING));
        builtInScope.addFunction(new BuiltInFunction("gets", TalcType.STRING));
        builtInScope.addFunction(new BuiltInFunction("print", null, null, TalcType.VOID));
        builtInScope.addFunction(new BuiltInFunction("prompt", Arrays.asList("prompt"), Arrays.asList(TalcType.STRING), TalcType.STRING));
        builtInScope.addFunction(new BuiltInFunction("puts", null, null, TalcType.VOID));
        builtInScope.addFunction(new BuiltInFunction("rnd", Arrays.asList("n"), Arrays.asList(TalcType.INT), TalcType.INT));
        builtInScope.addFunction(new BuiltInFunction("shell", Arrays.asList("command"), Arrays.asList(TalcType.STRING), TalcType.INT));
        builtInScope.addFunction(new BuiltInFunction("system", Arrays.asList("command"), Arrays.asList(TalcType.LIST_OF_STRING), TalcType.INT));
        builtInScope.addFunction(new BuiltInFunction("time_ms", TalcType.INT));
        // Built-in constants.
        builtInScope.addVariable(new BuiltInConstant("ARGV0", TalcType.STRING, argv0));
        builtInScope.addVariable(new BuiltInConstant("ARGS", TalcType.LIST_OF_STRING, null));
        builtInScope.addVariable(new BuiltInConstant("FILE_SEPARATOR", TalcType.STRING, java.io.File.separator));
        builtInScope.addVariable(new BuiltInConstant("PATH_SEPARATOR", TalcType.STRING, java.io.File.pathSeparator));
        
        // Note that we have to surround the built-in scope with a scope for user-defined globals.
        // Both things are "global", but it's important not to confuse the two.
        globalScope = new Scope(builtInScope);
    }
    
    public static Collection<AstNode.VariableDefinition> builtInVariableDefinitions() {
        return builtInScope.variables.values();
    }
}
