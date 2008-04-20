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

public class SymbolTable implements AstVisitor<Void> {
    private long creationTime;
    private ArrayStack<Scope> scopes = new ArrayStack<Scope>();
    
    public SymbolTable(List<AstNode> ast) {
        creationTime = System.nanoTime();
        
        scopes.push(Scope.globalScope());
        
        for (AstNode node : ast) {
            node.accept(this);
        }
    }
    
    public long creationTime() {
        return creationTime;
    }
    
    private void pushScope() {
        scopes.push(new Scope(scopes.peek()));
    }
    
    private void popScope() {
        scopes.pop();
    }
    
    private void setScope(AstNode node) {
        node.setScope(scopes.peek());
    }
    
    public Void visitBinaryOperator(AstNode.BinaryOperator binOp) {
        setScope(binOp);
        binOp.lhs().accept(this);
        if (binOp.rhs() != null) {
            binOp.rhs().accept(this);
        }
        return null;
    }
    
    public Void visitBlock(AstNode.Block block) {
        setScope(block);
        pushScope();
        try {
            for (AstNode statement : block.statements()) {
                statement.accept(this);
            }
        } finally {
            popScope();
        }
        return null;
    }
    
    public Void visitBreakStatement(AstNode.BreakStatement breakStatement) {
        setScope(breakStatement);
        return null;
    }
    
    public Void visitClassDefinition(AstNode.ClassDefinition classDefinition) {
        // FIXME: is this right? i think this should be classDefinition.setScope(<superclass's scope>). but how do we do that? not another pass?! maybe this whole scope-setting thing's a bad idea...
        setScope(classDefinition);
        String className = classDefinition.className();
        TalcType newClass = TalcType.makeUserDefinedClass(TalcType.OBJECT, className);
        TalcType.addClass(newClass);
        classDefinition.setType(newClass);
        
        scopes.push(newClass.members());
        try {
            for (AstNode.VariableDefinition field : classDefinition.fields()) {
                field.markAsField();
                field.accept(this);
            }
            for (AstNode.FunctionDefinition method : classDefinition.methods()) {
                if (method.functionName().equals(className)) {
                    method.markAsConstructor();
                }
                method.accept(this);
            }
        } finally {
            scopes.pop();
        }
        
        return null;
    }
    
    public Void visitConstant(AstNode.Constant constant) {
        setScope(constant);
        return null;
    }
    
    public Void visitContinueStatement(AstNode.ContinueStatement continueStatement) {
        setScope(continueStatement);
        return null;
    }
    
    public Void visitDoStatement(AstNode.DoStatement doStatement) {
        setScope(doStatement);
        pushScope();
        try {
            doStatement.body().accept(this);
        } finally {
            popScope();
        }
        doStatement.expression().accept(this);
        return null;
    }
    
    public Void visitForStatement(AstNode.ForStatement forStatement) {
        setScope(forStatement);
        pushScope();
        try {
            if (forStatement.initializer() != null) {
                forStatement.initializer().accept(this);
            }
            forStatement.conditionExpression().accept(this);
            forStatement.updateExpression().accept(this);
            forStatement.body().accept(this);
        } finally {
            popScope();
        }
        return null;
    }
    
    public Void visitForEachStatement(AstNode.ForEachStatement forEachStatement) {
        setScope(forEachStatement);
        
        // Check the expression first, because the loop variables aren't visible in the expression.
        forEachStatement.expression().accept(this);
        pushScope();
        try {
            // Add the loop variables.
            for (AstNode.VariableDefinition loopVariable : forEachStatement.loopVariableDefinitions()) {
                scopes.peek().addVariable(loopVariable);
            }
            // Then check the body.
            forEachStatement.body().accept(this);
        } finally {
            popScope();
        }
        return null;
    }
    
    public Void visitFunctionCall(AstNode.FunctionCall functionCall) {
        setScope(functionCall);
        if (functionCall.instance() != null) {
            functionCall.instance().accept(this);
        }
        for (AstNode argument : functionCall.arguments()) {
            argument.accept(this);
        }
        return null;
    }
    
    public Void visitFunctionDefinition(AstNode.FunctionDefinition functionDefinition) {
        setScope(functionDefinition);
        // Add the function first...
        scopes.peek().addFunction(functionDefinition);
        // ...and then check the body, to allow recursive definitions.
        pushScope();
        try {
            List<String> names = functionDefinition.formalParameterNames();
            List<TalcTypeDescriptor> typeDescriptors = functionDefinition.formalParameterTypeDescriptors();
            // FIXME: could we set these VariableDefinitions up earlier? maybe create them in Parser, even?
            ArrayList<AstNode.VariableDefinition> formalParameters = new ArrayList<AstNode.VariableDefinition>();
            for (int i = 0; i < names.size(); ++i) {
                AstNode.VariableDefinition formalParameter = new AstNode.VariableDefinition(null, names.get(i), typeDescriptors.get(i), null, false);
                formalParameter.accept(this);
                formalParameters.add(formalParameter);
            }
            functionDefinition.setFormalParameters(formalParameters);
            functionDefinition.body().accept(this);
        } finally {
            popScope();
        }
        return null;
    }
    
    public Void visitIfStatement(AstNode.IfStatement ifStatement) {
        setScope(ifStatement);
        for (AstNode expression : ifStatement.expressions()) {
            expression.accept(this);
        }
        for (AstNode body : ifStatement.bodies()) {
            pushScope();
            try {
                body.accept(this);
            } finally {
                popScope();
            }
        }
        pushScope();
        try {
            ifStatement.elseBlock().accept(this);
        } finally {
            popScope();
        }
        return null;
    }
    
    public Void visitListLiteral(AstNode.ListLiteral listLiteral) {
        for (AstNode expression : listLiteral.expressions()) {
            expression.accept(this);
        }
        return null;
    }
    
    public Void visitReturnStatement(AstNode.ReturnStatement returnStatement) {
        setScope(returnStatement);
        if (returnStatement.expression() != null) {
            returnStatement.expression().accept(this);
        }
        return null;
    }
    
    public Void visitVariableDefinition(AstNode.VariableDefinition variableDefinition) {
        setScope(variableDefinition);
        // Check the initializer first...
        if (variableDefinition.initializer() != null) {
            variableDefinition.initializer().accept(this);
        }
        // ...and add the variable to the current scope second, to prevent attempts at recursive initialization.
        scopes.peek().addVariable(variableDefinition);
        return null;
    }
    
    public Void visitVariableName(AstNode.VariableName variableName) {
        setScope(variableName);
        variableName.setDefinition(scopes.peek().findVariable(variableName.identifier()));
        if (variableName.definition() == null) {
            throw new TalcError(variableName, "no variable \"" + variableName.identifier() + "\" in scope");
        }
        return null;
    }
    
    public Void visitWhileStatement(AstNode.WhileStatement whileStatement) {
        setScope(whileStatement);
        whileStatement.expression().accept(this);
        pushScope();
        try {
            whileStatement.body().accept(this);
        } finally {
            popScope();
        }
        return null;
    }
}
