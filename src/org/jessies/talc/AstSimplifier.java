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

public class AstSimplifier implements AstVisitor<AstNode> {
    private long creationTime;
    
    public AstSimplifier(List<AstNode> ast) {
        creationTime = System.nanoTime();
        for (AstNode node : ast) {
            node.accept(this);
        }
    }
    
    public long creationTime() {
        return creationTime;
    }
    
    public AstNode visitBinaryOperator(AstNode.BinaryOperator binOp) {
        AstNode lhs = binOp.lhs().accept(this);
        AstNode rhs = simplifyIfNotNull(binOp.rhs());
        Token op = binOp.op();
        // We only simplify integer expressions.
        // Section 12.3.2 of Muchnick's "Advanced Compiler Design & Implementation" claims replacing division-by-constant with multiplication is the only valid floating-point algebraic simplification.
        // FIXME: simplify boolean expressions too.
        if (op == Token.PLUS) {
            // 0 + x == x
            if (isZero(lhs)) {
                return rhs;
            }
            // x + 0 == x
            if (isZero(rhs)) {
                return lhs;
            }
        }
        if (op == Token.SUB) {
            // x - 0 == x
            if (isZero(rhs)) {
                return lhs;
            }
            // FIXME: is "0 - x == -x" worth doing, in our context?
            // FIXME: if lhs equals rhs, return 0.
        }
        if (op == Token.MUL) {
            if (isZero(lhs)) {
                // 0 * x == 0
                return lhs;
            } else if (isZero(rhs)) {
                // x * 0 == 0
                return rhs;
            } else if (isOne(lhs)) {
                // 1 * x == x
                return rhs;
            } else if (isOne(rhs)) {
                // x * 1 == x
                return lhs;
            }
        }
        if (op == Token.DIV) {
            if (isZero(lhs)) {
                // 0 / x == 0
                return lhs;
            }
            if (isOne(rhs)) {
                // x / 1 == x
                return lhs;
            }
            // FIXME: if lhs equals rhs, return 1.
        }
        if (op == Token.SHL || op == Token.SHR) {
            // x << 0 == x
            // x >> 0 == x
            if (isZero(rhs)) {
                return lhs;
            }
        }
        return new AstNode.BinaryOperator(binOp.location(), op, lhs, rhs);
    }
    
    private static boolean isEqualToIntegerConstant(AstNode node, IntegerValue value) {
        if (node instanceof AstNode.Constant == false) {
            return false;
        }
        AstNode.Constant constant = (AstNode.Constant) node;
        if (constant.constant() instanceof IntegerValue == false) {
            return false;
        }
        IntegerValue constantValue = (IntegerValue) constant.constant();
        return (constantValue.compareTo(value) == 0);
    }
    
    private static boolean isZero(AstNode node) {
        return isEqualToIntegerConstant(node, IntegerValue.ZERO);
    }
    
    private static boolean isOne(AstNode node) {
        return isEqualToIntegerConstant(node, IntegerValue.ONE);
    }
    
    public AstNode visitBlock(AstNode.Block block) {
        if (block == AstNode.Block.EMPTY_BLOCK) {
            return block;
        }
        
        // The source language insists on blocks everywhere to encourage good style, but there's no reason we can't optimize unnecessary blocks away internally.
        List<AstNode> newStatements = simplifyAstNodeList(block.statements());
        final int newStatementsCount = newStatements.size();
        if (newStatementsCount == 0) {
            // If this block ends up empty, we already have a handy empty block.
            return AstNode.Block.EMPTY_BLOCK;
        } else if (newStatementsCount == 1 && newStatements.get(0) instanceof AstNode.VariableDefinition == false) {
            // If this block ends up containing a single AstNode that's not a variable definition, we can elide this block and just return the child AstNode.
            return newStatements.get(0);
        } else {
            // This is a complicated enough block to be worth keeping.
            return new AstNode.Block(block.location(), newStatements);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T extends AstNode> List<T> simplifyAstNodeList(List<T> nodes) {
        ArrayList<T> newNodes = new ArrayList<T>(nodes.size());
        for (T node : nodes) {
            newNodes.add((T) node.accept(this));
        }
        return newNodes;
    }
    
    @SuppressWarnings("unchecked")
    private <T extends AstNode> T simplifyIfNotNull(T node) {
        return (node != null) ? (T) node.accept(this) : null;
    }
    
    public AstNode visitBreakStatement(AstNode.BreakStatement node) {
        return node;
    }
    
    public AstNode visitClassDefinition(AstNode.ClassDefinition classDefinition) {
        List<AstNode.VariableDefinition> newFields = simplifyAstNodeList(classDefinition.fields());
        List<AstNode.FunctionDefinition> newMethods = simplifyAstNodeList(classDefinition.methods());
        return new AstNode.ClassDefinition(classDefinition.location(), classDefinition.className(), newFields, newMethods);
    }
    
    public AstNode visitConstant(AstNode.Constant node) {
        return node;
    }
    
    public AstNode visitContinueStatement(AstNode.ContinueStatement node) {
        return node;
    }
    
    public AstNode visitDoStatement(AstNode.DoStatement doStatement) {
        return new AstNode.DoStatement(doStatement.location(), doStatement.body().accept(this), doStatement.expression().accept(this));
    }
    
    public AstNode visitForStatement(AstNode.ForStatement forStatement) {
        AstNode.VariableDefinition newInitializer = simplifyIfNotNull(forStatement.initializer());
        AstNode newConditionExpression = forStatement.conditionExpression().accept(this);
        AstNode newUpdateExpression = forStatement.updateExpression().accept(this);
        return new AstNode.ForStatement(forStatement.location(), newInitializer, newConditionExpression, newUpdateExpression, forStatement.body().accept(this));
    }
    
    public AstNode visitForEachStatement(AstNode.ForEachStatement forEachStatement) {
        return new AstNode.ForEachStatement(forEachStatement.location(), forEachStatement.loopVariableDefinitions(), forEachStatement.expression().accept(this), forEachStatement.body().accept(this));
    }
    
    public AstNode visitFunctionCall(AstNode.FunctionCall call) {
        AstNode[] arguments = call.arguments();
        AstNode[] newArguments = new AstNode[arguments.length];
        for (int i = arguments.length - 1; i >= 0; --i) {
            AstNode argument = arguments[i];
            newArguments[i] = argument.accept(this);
        }
        return new AstNode.FunctionCall(call.location(), call.functionName(), simplifyIfNotNull(call.instance()), newArguments);
    }
    
    public AstNode visitFunctionDefinition(AstNode.FunctionDefinition function) {
        return new AstNode.FunctionDefinition(function.location(), function.functionName(), function.formalParameterNames(), function.formalParameterTypes(), function.returnType(), function.body().accept(this));
    }
    
    public AstNode visitIfStatement(AstNode.IfStatement ifStatement) {
        List<AstNode> newExpressions = simplifyAstNodeList(ifStatement.expressions());
        List<AstNode> newBodies = simplifyAstNodeList(ifStatement.bodies());
        AstNode newElseBlock = ifStatement.elseBlock().accept(this);
        return new AstNode.IfStatement(ifStatement.location(), newExpressions, newBodies, newElseBlock);
    }
    
    public AstNode visitListLiteral(AstNode.ListLiteral listLiteral) {
        List<AstNode> newExpressions = simplifyAstNodeList(listLiteral.expressions());
        return new AstNode.ListLiteral(listLiteral.location(), newExpressions);
    }
    
    public AstNode visitReturnStatement(AstNode.ReturnStatement returnStatement) {
        if (returnStatement.expression() != null) {
            return new AstNode.ReturnStatement(returnStatement.location(), returnStatement.expression().accept(this));
        }
        return returnStatement;
    }
    
    public AstNode visitVariableDefinition(AstNode.VariableDefinition var) {
        return new AstNode.VariableDefinition(var.location(), var.identifier(), var.type(), var.initializer().accept(this), var.isFinal());
    }
    
    public AstNode visitVariableName(AstNode.VariableName node) {
        return node;
    }
    
    public AstNode visitWhileStatement(AstNode.WhileStatement whileStatement) {
        return new AstNode.WhileStatement(whileStatement.location(), whileStatement.expression().accept(this), whileStatement.body().accept(this));
    }
}
