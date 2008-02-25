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

public interface AstVisitor<ResultT> {
    public ResultT visitBinaryOperator(AstNode.BinaryOperator binOp);
    public ResultT visitBlock(AstNode.Block block);
    public ResultT visitBreakStatement(AstNode.BreakStatement breakStatement);
    public ResultT visitConstant(AstNode.Constant constant);
    public ResultT visitClassDefinition(AstNode.ClassDefinition classDefinition);
    public ResultT visitContinueStatement(AstNode.ContinueStatement continueStatement);
    public ResultT visitDoStatement(AstNode.DoStatement doStatement);
    public ResultT visitForStatement(AstNode.ForStatement forStatement);
    public ResultT visitForEachStatement(AstNode.ForEachStatement forEachStatement);
    public ResultT visitFunctionCall(AstNode.FunctionCall functionCall);
    public ResultT visitFunctionDefinition(AstNode.FunctionDefinition functionDefinition);
    public ResultT visitIfStatement(AstNode.IfStatement ifStatement);
    public ResultT visitListLiteral(AstNode.ListLiteral listLiteral);
    public ResultT visitReturnStatement(AstNode.ReturnStatement returnStatement);
    public ResultT visitVariableDefinition(AstNode.VariableDefinition variableDefinition);
    public ResultT visitVariableName(AstNode.VariableName variableName);
    public ResultT visitWhileStatement(AstNode.WhileStatement whileStatement);
}
