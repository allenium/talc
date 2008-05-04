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

import java.util.*;

/**
 * Checks for errors other than type errors, which are handled by AstTypeChecker.
 */
public class AstErrorChecker implements AstVisitor<Void> {
    private long creationTime;
    private int loopDepth = 0;
    private int functionDepth = 0;
    
    public AstErrorChecker(List<AstNode> ast) {
        creationTime = System.nanoTime();
        for (AstNode node : ast) {
            node.accept(this);
        }
    }
    
    public long creationTime() {
        return creationTime;
    }
    
    public Void visitAssertStatement(AstNode.AssertStatement assertStatement) {
        return null;
    }
    
    public Void visitBinaryOperator(AstNode.BinaryOperator binOp) {
        binOp.lhs().accept(this);
        if (binOp.rhs() != null) {
            binOp.rhs().accept(this);
        }
        switch (binOp.op()) {
        case POST_DECREMENT:
        case POST_INCREMENT:
        case PRE_DECREMENT:
        case PRE_INCREMENT:
        case ASSIGN:
        case PLUS_ASSIGN:
        case SUB_ASSIGN:
        case MUL_ASSIGN:
        case POW_ASSIGN:
        case DIV_ASSIGN:
        case MOD_ASSIGN:
        case SHL_ASSIGN:
        case SHR_ASSIGN:
        case AND_ASSIGN:
        case OR_ASSIGN:
        case XOR_ASSIGN:
            // Check we're not trying to modify something we can't.
            if (binOp.lhs() instanceof AstNode.VariableName == false) {
                throw new TalcError(binOp, "can't assign to a non-variable");
            }
            AstNode.VariableName variableName = (AstNode.VariableName) binOp.lhs();
            AstNode.VariableDefinition variableDefinition = binOp.scope().findVariable(variableName.identifier());
            if (variableDefinition.isFinal()) {
                throw new TalcError(binOp, "can't reassign final variable \"" + variableName.identifier() + "\"\n" + variableDefinition.location() + "...defined here");
            }
        }
        return null;
    }
    
    public Void visitBlock(AstNode.Block block) {
        for (AstNode statement : block.statements()) {
            statement.accept(this);
        }
        return null;
    }
    
    public Void visitBreakStatement(AstNode.BreakStatement breakStatement) {
        if (loopDepth == 0) {
            throw new TalcError(breakStatement, "can't \"break\" without an enclosing loop");
        }
        return null;
    }
    
    public Void visitClassDefinition(AstNode.ClassDefinition classDefinition) {
        // FIXME: any more specific checks needed here?
        for (AstNode.VariableDefinition field : classDefinition.fields()) {
            field.accept(this);
        }
        for (AstNode.FunctionDefinition method : classDefinition.methods()) {
            method.accept(this);
        }
        return null;
    }
    
    public Void visitConstant(AstNode.Constant constant) {
        return null;
    }
    
    public Void visitContinueStatement(AstNode.ContinueStatement continueStatement) {
        if (loopDepth == 0) {
            throw new TalcError(continueStatement, "can't \"continue\" without an enclosing loop");
        }
        return null;
    }
    
    public Void visitDoStatement(AstNode.DoStatement doStatement) {
        ++loopDepth;
        doStatement.body().accept(this);
        --loopDepth;
        doStatement.expression().accept(this);
        return null;
    }
    
    public Void visitForStatement(AstNode.ForStatement forStatement) {
        if (forStatement.initializer() != null) {
            forStatement.initializer().accept(this);
        }
        forStatement.conditionExpression().accept(this);
        forStatement.updateExpression().accept(this);
        ++loopDepth;
        forStatement.body().accept(this);
        --loopDepth;
        return null;
    }
    
    public Void visitForEachStatement(AstNode.ForEachStatement forEachStatement) {
        forEachStatement.expression().accept(this);
        ++loopDepth;
        forEachStatement.body().accept(this);
        --loopDepth;
        return null;
    }
    
    public Void visitFunctionCall(AstNode.FunctionCall functionCall) {
        if (functionCall.instance() != null) {
            functionCall.instance().accept(this);
        }
        for (AstNode argument : functionCall.arguments()) {
            argument.accept(this);
        }
        return null;
    }
    
    public Void visitFunctionDefinition(AstNode.FunctionDefinition functionDefinition) {
        ++functionDepth;
        if (functionDepth > 1) {
            throw new TalcError(functionDefinition, "functions cannot be nested");
        }
        functionDefinition.body().accept(this);
        --functionDepth;
        return null;
    }
    
    public Void visitIfStatement(AstNode.IfStatement ifStatement) {
        for (AstNode expression : ifStatement.expressions()) {
            expression.accept(this);
        }
        for (AstNode body : ifStatement.bodies()) {
            body.accept(this);
        }
        ifStatement.elseBlock().accept(this);
        return null;
    }
    
    public Void visitListLiteral(AstNode.ListLiteral listLiteral) {
        for (AstNode expression : listLiteral.expressions()) {
            expression.accept(this);
        }
        return null;
    }
    
    public Void visitReturnStatement(AstNode.ReturnStatement returnStatement) {
        if (returnStatement.expression() != null) {
            returnStatement.expression().accept(this);
        }
        return null;
    }
    
    public Void visitVariableDefinition(AstNode.VariableDefinition variableDefinition) {
        variableDefinition.initializer().accept(this);
        return null;
    }
    
    public Void visitVariableName(AstNode.VariableName variableName) {
        return null;
    }
    
    public Void visitWhileStatement(AstNode.WhileStatement whileStatement) {
        whileStatement.expression().accept(this);
        ++loopDepth;
        whileStatement.body().accept(this);
        --loopDepth;
        return null;
    }
}
