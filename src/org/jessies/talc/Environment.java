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

public class Environment {
    private static class StackFrame {
        private Variables variables = new Variables();
    }
    
    private ArrayStack<StackFrame> stack;
    
    public Environment() {
        stack = new ArrayStack<StackFrame>();
        
        // The top-most stack frame contains the built-in variables.
        pushStackFrame();
        Variables variables = stack.peek().variables;
        for (AstNode.VariableDefinition builtIn : Scope.builtInVariableDefinitions()) {
            variables.defineVariable(builtIn.identifier(), ((AstNode.Constant) builtIn.initializer()).constant());
        }
        
        // The next stack frame is the global scope for user-defined variables.
        pushStackFrame();
    }
    
    public void pushStackFrame() {
        stack.push(new StackFrame());
    }
    
    public void popStackFrame() {
        stack.pop();
    }
    
    public Value assignVariable(String name, Value value) {
        // FIXME: we don't need to search the whole stack at run-time!
        for (StackFrame frame : stack) {
            // FIXME: looking up the name twice is unnecessary.
            if (frame.variables.contains(name)) {
                return frame.variables.assignVariable(name, value);
            }
        }
        // Error checking should catch this before it can happen. If we get here, the bug's in the earlier phases.
        throw new IllegalStateException("unknown variable \"" + name + "\" can't be assigned value " + value);
    }
    
    public Value defineVariable(String name, Value value) {
        return stack.peek().variables.defineVariable(name, value);
    }
    
    public Value valueOf(String name) {
        // FIXME: we don't need to search the whole stack at run-time!
        for (StackFrame frame : stack) {
            Value value = frame.variables.valueOf(name);
            if (value != null) {
                return value;
            }
        }
        // Error checking should catch this before it can happen. If we get here, the bug's in the earlier phases.
        // We probably found the variable, but its value was "null".
        return null;
    }
}
