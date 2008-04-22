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

public class AstSimplifier implements AstVisitor<AstNode> {
    private long creationTime;
    
    public AstSimplifier() {
        creationTime = System.nanoTime();
    }
    
    public long creationTime() {
        return creationTime;
    }
    
    public List<AstNode> simplify(List<AstNode> ast) {
        return simplifyAstNodeList(ast);
    }
    
    public AstNode visitBinaryOperator(AstNode.BinaryOperator binOp) {
        AstNode lhs = binOp.lhs().accept(this);
        binOp.setLhs(lhs);
        AstNode rhs = simplifyIfNotNull(binOp.rhs());
        binOp.setRhs(rhs);
        
        Token op = binOp.op();
        // We only simplify integer expressions.
        // Section 12.3.2 of Muchnick's "Advanced Compiler Design & Implementation" claims replacing division-by-constant with multiplication is the only valid floating-point algebraic simplification.
        if (op == Token.PLUS) {
            // 0 + x == x
            if (isZero(lhs)) {
                return rhs;
            }
            // x + 0 == x
            if (isZero(rhs)) {
                return lhs;
            }
            // We can concatenate string constants at compile time.
            String lhsString = stringConstant(lhs);
            String rhsString = stringConstant(rhs);
            if (lhsString != null && rhsString != null) {
                return new AstNode.Constant(binOp.location(), lhsString + rhsString, TalcType.STRING);
            }
        } else if (op == Token.PLUS_ASSIGN) {
            // x += 0 == x
            if (isZero(rhs)) {
                return lhs;
            }
            // x += 1 == ++x
            if (isOne(rhs)) {
                binOp.setOp(Token.PRE_INCREMENT);
                binOp.setRhs(null);
            }
        } else if (op == Token.SUB) {
            // 0 - x == -x
            if (isZero(lhs)) {
                AstNode.BinaryOperator result = new AstNode.BinaryOperator(binOp.location(), Token.NEG, rhs, null);
                result.setType(binOp.type());
                return result;
            }
            // x - 0 == x
            if (isZero(rhs)) {
                return lhs;
            }
        } else if (op == Token.SUB_ASSIGN) {
            // x -= 0 == 0
            if (isZero(rhs)) {
                return lhs;
            }
            // x -= 1 == --x
            if (isOne(rhs)) {
                binOp.setOp(Token.PRE_DECREMENT);
                binOp.setRhs(null);
            }
        } else if (op == Token.MUL) {
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
        } else if (op == Token.DIV) {
            if (isZero(lhs)) {
                // 0 / x == 0
                return lhs;
            }
            if (isOne(rhs)) {
                // x / 1 == x
                return lhs;
            }
        } else if (op == Token.SHL || op == Token.SHR) {
            // x << 0 == x
            // x >> 0 == x
            if (isZero(rhs)) {
                return lhs;
            }
        } else if (op == Token.NEG) {
            // We can do compile-time negation of numeric constants.
            Object lhsConstant = constant(lhs);
            if (lhsConstant instanceof IntegerValue) {
                return new AstNode.Constant(binOp.location(), ((IntegerValue) lhsConstant).negate(), TalcType.INT);
            } else if (lhsConstant instanceof RealValue) {
                return new AstNode.Constant(binOp.location(), ((RealValue) lhsConstant).negate(), TalcType.REAL);
            }
        } else if (op == Token.L_AND) {
            // true && x = x
            if (constant(lhs) == BooleanValue.TRUE) {
                return rhs;
            }
            // false && x = false
            if (constant(lhs) == BooleanValue.FALSE) {
                return lhs;
            }
        } else if (op == Token.L_OR) {
            // true || x = true
            if (constant(lhs) == BooleanValue.TRUE) {
                return lhs;
            }
            // false || x = x
            if (constant(lhs) == BooleanValue.FALSE) {
                return rhs;
            }
        }
        
        Object lhsConstant = constant(lhs);
        Object rhsConstant = constant(rhs);
        if ((op == Token.B_NOT || op == Token.FACTORIAL) && lhsConstant != null && lhsConstant instanceof IntegerValue) {
            // A unary operator with an integer constant lhs. Easy.
            return new AstNode.Constant(binOp.location(), evaluateIntegerExpression(binOp, (IntegerValue) lhsConstant, null), TalcType.INT);
        }
        if (lhsConstant != null && rhsConstant != null) {
            // Relational operators on two constants can be evaluated at compile-time.
            if (op == Token.EQ) {
                return new AstNode.Constant(binOp.location(), Functions.eq(lhsConstant, rhsConstant), TalcType.BOOL);
            } else if (op == Token.NE) {
                return new AstNode.Constant(binOp.location(), Functions.ne(lhsConstant, rhsConstant), TalcType.BOOL);
            }
            
            if (lhsConstant instanceof IntegerValue && rhsConstant instanceof IntegerValue) {
                IntegerValue lhsValue = (IntegerValue) lhsConstant;
                IntegerValue rhsValue = (IntegerValue) rhsConstant;
                
                if (op == Token.LE) {
                    return new AstNode.Constant(binOp.location(), BooleanValue.valueOf(lhsValue.compareTo(rhsValue) <= 0), TalcType.BOOL);
                } else if (op == Token.GE) {
                    return new AstNode.Constant(binOp.location(), BooleanValue.valueOf(lhsValue.compareTo(rhsValue) >= 0), TalcType.BOOL);
                } else if (op == Token.GT) {
                    return new AstNode.Constant(binOp.location(), BooleanValue.valueOf(lhsValue.compareTo(rhsValue) > 0), TalcType.BOOL);
                } else if (op == Token.LT) {
                    return new AstNode.Constant(binOp.location(), BooleanValue.valueOf(lhsValue.compareTo(rhsValue) < 0), TalcType.BOOL);
                }
                
                // Must be an "arithmetic" integer expression.
                return new AstNode.Constant(binOp.location(), evaluateIntegerExpression(binOp, lhsValue, rhsValue), TalcType.INT);
            }
        }
        
        return binOp;
    }
    
    private IntegerValue evaluateIntegerExpression(AstNode.BinaryOperator binOp, IntegerValue lhs, IntegerValue rhs) {
        switch (binOp.op()) {
            case PLUS: return lhs.add(rhs);
            case SUB: return lhs.subtract(rhs);
            case MUL: return lhs.multiply(rhs);
            case POW: return lhs.pow(rhs);
            case DIV: return lhs.divide(rhs);
            case MOD: return lhs.mod(rhs);
            case SHL: return lhs.shiftLeft(rhs);
            case SHR: return lhs.shiftRight(rhs);
            case B_AND: return lhs.and(rhs);
            case B_NOT: return lhs.not();
            case B_OR: return lhs.or(rhs);
            case B_XOR: return lhs.xor(rhs);
            case FACTORIAL: return lhs.factorial();
        default:
            throw new TalcError(binOp, "don't know how to compute " + binOp + " at compile time");
        }
    }
    
    private static boolean isEqualToIntegerConstant(AstNode node, IntegerValue value) {
        IntegerValue constantValue = integerConstant(node);
        if (constantValue == null) {
            return false;
        }
        return (constantValue.compareTo(value) == 0);
    }
    
    private static Object constant(AstNode node) {
        if (node instanceof AstNode.Constant == false) {
            return null;
        }
        AstNode.Constant constant = (AstNode.Constant) node;
        return constant.constant();
    }
    
    // Returns the integer constant 'node' represents, or null if 'node' isn't an integer constant.
    private static IntegerValue integerConstant(AstNode node) {
        if (node instanceof AstNode.Constant == false) {
            return null;
        }
        AstNode.Constant constant = (AstNode.Constant) node;
        Object value = constant.constant();
        if (value instanceof IntegerValue == false) {
            return null;
        }
        return (IntegerValue) value;
    }
    
    // Returns the string constant 'node' represents, or null if 'node' isn't an string constant.
    private static String stringConstant(AstNode node) {
        if (node instanceof AstNode.Constant == false) {
            return null;
        }
        AstNode.Constant constant = (AstNode.Constant) node;
        Object value = constant.constant();
        if (value instanceof String == false) {
            return null;
        }
        return (String) value;
    }
    
    private static boolean isZero(AstNode node) {
        return isEqualToIntegerConstant(node, IntegerValue.valueOf(0));
    }
    
    private static boolean isOne(AstNode node) {
        return isEqualToIntegerConstant(node, IntegerValue.valueOf(1));
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
        } else {
            // This is a complicated enough block to be worth keeping.
            // You might think that if this block only contains a single AstNode that's not a variable definition, we can just return the child AstNode.
            // Unfortunately, that's not true: it breaks our reliance on JvmCodeGenerator.visitBlock being the one to call JvmCodeGenerator.popAnythingLeftBy.
            block.setStatements(newStatements);
            return block;
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T extends AstNode> ArrayList<T> simplifyAstNodeList(List<T> nodes) {
        ArrayList<T> newNodes = new ArrayList<T>(nodes.size());
        for (T node : nodes) {
            T newNode = (T) node.accept(this);
            if (newNode != null) {
                newNodes.add(newNode);
            }
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
        classDefinition.setFields(simplifyAstNodeList(classDefinition.fields()));
        classDefinition.setMethods(simplifyAstNodeList(classDefinition.methods()));
        return classDefinition;
    }
    
    public AstNode visitConstant(AstNode.Constant node) {
        return node;
    }
    
    public AstNode visitContinueStatement(AstNode.ContinueStatement node) {
        return node;
    }
    
    public AstNode visitDoStatement(AstNode.DoStatement doStatement) {
        doStatement.setBody(doStatement.body().accept(this));
        doStatement.setExpression(doStatement.expression().accept(this));
        return doStatement;
    }
    
    public AstNode visitForStatement(AstNode.ForStatement forStatement) {
        forStatement.setInitializer(simplifyIfNotNull(forStatement.initializer()));
        forStatement.setConditionExpression(forStatement.conditionExpression().accept(this));
        forStatement.setUpdateExpression(forStatement.updateExpression().accept(this));
        forStatement.setBody(forStatement.body().accept(this));
        return forStatement;
    }
    
    public AstNode visitForEachStatement(AstNode.ForEachStatement forEachStatement) {
        forEachStatement.setExpression(forEachStatement.expression().accept(this));
        forEachStatement.setBody(forEachStatement.body().accept(this));
        return forEachStatement;
    }
    
    public AstNode visitFunctionCall(AstNode.FunctionCall call) {
        AstNode[] oldArguments = call.arguments();
        AstNode[] newArguments = new AstNode[oldArguments.length];
        for (int i = oldArguments.length - 1; i >= 0; --i) {
            newArguments[i] = oldArguments[i].accept(this);
        }
        call.setArguments(newArguments);
        call.setInstance(simplifyIfNotNull(call.instance()));
        return call;
    }
    
    public AstNode visitFunctionDefinition(AstNode.FunctionDefinition function) {
        function.setBody(function.body().accept(this));
        return function;
    }
    
    public AstNode visitIfStatement(AstNode.IfStatement ifStatement) {
        ArrayList<AstNode> expressions = simplifyAstNodeList(ifStatement.expressions());
        ArrayList<AstNode> bodies = simplifyAstNodeList(ifStatement.bodies());
        AstNode elseBlock = ifStatement.elseBlock().accept(this);
        
        // Check for constant expressions.
        Iterator<AstNode> eIt = expressions.iterator();
        Iterator<AstNode> bIt = bodies.iterator();
        while (eIt.hasNext()) {
            AstNode exp = eIt.next(); bIt.next();
            if (constant(exp) == BooleanValue.FALSE) {
                // Drop any false expressions and its corresponding body.
                eIt.remove(); bIt.remove();
            } else if (constant(exp) == BooleanValue.TRUE) {
                // Drop *everything* after (but not including) a true expression.
                while (eIt.hasNext()) {
                    eIt.next(); bIt.next();
                    eIt.remove(); bIt.remove();
                }
                elseBlock = AstNode.Block.EMPTY_BLOCK;
            }
        }
        
        // Check whether we optimized this "if" out of existence.
        if (expressions.size() == 0) {
            return elseBlock;
        }
        
        ifStatement.setExpressions(expressions);
        ifStatement.setBodies(bodies);
        ifStatement.setElseBlock(elseBlock);
        return ifStatement;
    }
    
    public AstNode visitListLiteral(AstNode.ListLiteral listLiteral) {
        listLiteral.setExpressions(simplifyAstNodeList(listLiteral.expressions()));
        return listLiteral;
    }
    
    public AstNode visitReturnStatement(AstNode.ReturnStatement returnStatement) {
        returnStatement.setExpression(simplifyIfNotNull(returnStatement.expression()));
        return returnStatement;
    }
    
    public AstNode visitVariableDefinition(AstNode.VariableDefinition var) {
        var.setInitializer(var.initializer().accept(this));
        return var;
    }
    
    public AstNode visitVariableName(AstNode.VariableName node) {
        return node;
    }
    
    public AstNode visitWhileStatement(AstNode.WhileStatement whileStatement) {
        AstNode expression = whileStatement.expression().accept(this);
        if (constant(expression) == BooleanValue.FALSE) {
            // The user probably didn't write a dead loop on purpose.
            throw new TalcError(whileStatement.expression(), "this \"while\" expression is always false, so the loop body is unreachable");
        }
        
        whileStatement.setExpression(expression);
        whileStatement.setBody(whileStatement.body().accept(this));
        return whileStatement;
    }
}
