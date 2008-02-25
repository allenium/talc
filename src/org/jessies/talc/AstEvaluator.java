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

public class AstEvaluator implements AstVisitor<Value> {
    private Environment rho;
    
    public AstEvaluator(Environment rho) {
        this.rho = rho;
    }
    
    public Value visitBinaryOperator(AstNode.BinaryOperator binOp) {
        Value result = null;
        AstNode lhs = binOp.lhs();
        switch (binOp.op()) {
            case NEG:     result = lhsNumber(binOp).negate(); break;
            case PLUS:    result = visitNumericAddOrStringConcatenation(binOp); break;
            case SUB:     result = lhsNumber(binOp).subtract(rhsNumber(binOp)); break;
            case MUL:     result = lhsNumber(binOp).multiply(rhsNumber(binOp)); break;
            case POW:     result = lhsNumber(binOp).pow(rhsNumber(binOp)); break;
            case DIV:     result = lhsNumber(binOp).divide(rhsNumber(binOp)); break;
            case MOD:     result = lhsInt(binOp).mod(rhsInt(binOp)); break;
            
            case SHL:     result = lhsInt(binOp).shiftLeft(rhsInt(binOp)); break;
            case SHR:     result = lhsInt(binOp).shiftRight(rhsInt(binOp)); break;
            
            case B_AND:   result = lhsInt(binOp).and(rhsInt(binOp)); break;
            case B_NOT:   result = lhsInt(binOp).not(); break;
            case B_OR:    result = lhsInt(binOp).or(rhsInt(binOp)); break;
            case B_XOR:   result = lhsInt(binOp).xor(rhsInt(binOp)); break;
            
            case L_NOT:   result = lhsBoolean(binOp).not(); break;
            case L_AND:   result = BooleanValue.valueOf(lhsBoolean(binOp).booleanValue() && rhsBoolean(binOp).booleanValue()); break;
            case L_OR:    result = BooleanValue.valueOf(lhsBoolean(binOp).booleanValue() || rhsBoolean(binOp).booleanValue()); break;
            
            case FACTORIAL:      result = ((IntegerValue) binOp.lhs().accept(this)).factorial(); break;
            
            case POST_DECREMENT: result = prePostIncrementDecrement(binOp, false, false); break;
            case POST_INCREMENT: result = prePostIncrementDecrement(binOp, false, true); break;
            case PRE_DECREMENT:  result = prePostIncrementDecrement(binOp, true, false); break;
            case PRE_INCREMENT:  result = prePostIncrementDecrement(binOp, true, true); break;
            
            case EQ:             result = BooleanValue.valueOf(binOp.lhs().accept(this).equals(binOp.rhs().accept(this))); break;
            case NE:             result = BooleanValue.valueOf(binOp.lhs().accept(this).equals(binOp.rhs().accept(this)) == false); break;
            // FIXME: there's no reason why we can't offer these relational operators on non-numeric types. But should we?
            case LE:             result = BooleanValue.valueOf(lhsNumber(binOp).compareTo(rhsNumber(binOp)) <= 0); break;
            case GE:             result = BooleanValue.valueOf(lhsNumber(binOp).compareTo(rhsNumber(binOp)) >= 0); break;
            case GT:             result = BooleanValue.valueOf(lhsNumber(binOp).compareTo(rhsNumber(binOp)) > 0); break;
            case LT:             result = BooleanValue.valueOf(lhsNumber(binOp).compareTo(rhsNumber(binOp)) < 0); break;
            
            case ASSIGN:         result = assignTo(lhs, binOp.rhs().accept(this)); break;
            case PLUS_ASSIGN:    result = assignTo(lhs, visitNumericAddOrStringConcatenation(binOp)); break;
            case SUB_ASSIGN:     result = assignTo(lhs, lhsNumber(binOp).subtract(rhsNumber(binOp))); break;
            case MUL_ASSIGN:     result = assignTo(lhs, lhsNumber(binOp).multiply(rhsNumber(binOp))); break;
            case POW_ASSIGN:     result = assignTo(lhs, lhsNumber(binOp).pow(rhsNumber(binOp))); break;
            case DIV_ASSIGN:     result = assignTo(lhs, lhsNumber(binOp).divide(rhsNumber(binOp))); break;
            case MOD_ASSIGN:     result = assignTo(lhs, rhsInt(binOp).mod(rhsInt(binOp))); break;
            case SHL_ASSIGN:     result = assignTo(lhs, rhsInt(binOp).shiftLeft(rhsInt(binOp))); break;
            case SHR_ASSIGN:     result = assignTo(lhs, rhsInt(binOp).shiftRight(rhsInt(binOp))); break;
            case AND_ASSIGN:     result = assignTo(lhs, rhsInt(binOp).and(rhsInt(binOp))); break;
            case OR_ASSIGN:      result = assignTo(lhs, rhsInt(binOp).or(rhsInt(binOp))); break;
            case XOR_ASSIGN:     result = assignTo(lhs, rhsInt(binOp).xor(rhsInt(binOp))); break;
            
        default:
            throw new RuntimeException("Unimplemented operation " + binOp.op());
        }
        return result;
    }
    
    private Value prePostIncrementDecrement(AstNode.BinaryOperator binOp, boolean isPre, boolean isIncrement) {
        if (isPre) {
            return assignTo(binOp.lhs(), incrementDecrement(lhsNumber(binOp), isIncrement));
        } else {
            NumericValue n = lhsNumber(binOp);
            Value result = n;
            assignTo(binOp.lhs(), incrementDecrement(n, isIncrement));
            return result;
        }
    }
    
    private NumericValue incrementDecrement(NumericValue n, boolean isIncrement) {
        if (n instanceof IntegerValue) {
            IntegerValue i = (IntegerValue) n;
            return isIncrement ? i.add(IntegerValue.ONE) : i.subtract(IntegerValue.ONE);
        } else {
            RealValue r = (RealValue) n;
            return isIncrement ? r.add(RealValue.ONE) : r.subtract(RealValue.ONE);
        }
    }
    
    private Value visitNumericAddOrStringConcatenation(AstNode.BinaryOperator binOp) {
        // '+' adds numbers or concatenates strings.
        Value lhs = binOp.lhs().accept(this);
        Value rhs = binOp.rhs().accept(this);
        if (lhs instanceof NumericValue) {
            return ((NumericValue) lhs).add((NumericValue) rhs);
        } else {
            // Type checking should have ensured these are both strings, but let's double-check.
            return new StringValue(((StringValue) lhs).toString() + ((StringValue) rhs).toString());
        }
    }
    
    private Value lhsValue(AstNode.BinaryOperator binOp) {
        return binOp.lhs().accept(this);
    }
    
    private Value rhsValue(AstNode.BinaryOperator binOp) {
        return binOp.rhs().accept(this);
    }
    
    private BooleanValue lhsBoolean(AstNode.BinaryOperator binOp) {
        return (BooleanValue) lhsValue(binOp);
    }
    
    private BooleanValue rhsBoolean(AstNode.BinaryOperator binOp) {
        return (BooleanValue) rhsValue(binOp);
    }
    
    private IntegerValue lhsInt(AstNode.BinaryOperator binOp) {
        return (IntegerValue) lhsValue(binOp);
    }
    
    private IntegerValue rhsInt(AstNode.BinaryOperator binOp) {
        return (IntegerValue) rhsValue(binOp);
    }
    
    private NumericValue lhsNumber(AstNode.BinaryOperator binOp) {
        return (NumericValue) lhsValue(binOp);
    }
    
    private NumericValue rhsNumber(AstNode.BinaryOperator binOp) {
        return (NumericValue) rhsValue(binOp);
    }
    
    private Value assignTo(AstNode lhs, Value newValue) {
        AstNode.VariableName variableName = (AstNode.VariableName) lhs;
        String name = variableName.identifier();
        return (variableName.isFieldAccess()) ? ((UserDefinedClassValue) rho.valueOf("this")).putField(name, newValue) : rho.assignVariable(name, newValue);
    }
    
    public Value visitBlock(AstNode.Block block) {
        rho.pushStackFrame();
        try {
            for (AstNode statement : block.statements()) {
                statement.accept(this);
            }
            return null;
        } finally {
            rho.popStackFrame();
        }
    }
    
    public Value visitBreakStatement(AstNode.BreakStatement node) {
        throw new BreakException();
    }
    
    private static class BreakException extends ExceptionWithoutStackTrace {
        public BreakException() {
        }
    }
    
    private static class ContinueException extends ExceptionWithoutStackTrace {
        public ContinueException() {
        }
    }
    
    public Value visitClassDefinition(AstNode.ClassDefinition classDefinition) {
        return null;
    }
    
    public Value visitConstant(AstNode.Constant node) {
        return node.constant();
    }
    
    public Value visitContinueStatement(AstNode.ContinueStatement node) {
        throw new ContinueException();
    }
    
    public Value visitDoStatement(AstNode.DoStatement doStatement) {
        AstNode expression = doStatement.expression();
        AstNode body = doStatement.body();
        do {
            try {
                body.accept(this);
            } catch (BreakException breakException) {
                break;
            } catch (ContinueException continueException) {
                continue;
            }
        } while (((BooleanValue) expression.accept(this)).booleanValue());
        return null;
    }
    
    public Value visitForStatement(AstNode.ForStatement forStatement) {
        rho.pushStackFrame();
        try {
            if (forStatement.initializer() != null) {
                forStatement.initializer().accept(this);
            }
            
            AstNode conditionExpression = forStatement.conditionExpression();
            AstNode updateExpression = forStatement.updateExpression();
            AstNode body = forStatement.body();
            for (; ((BooleanValue) conditionExpression.accept(this)).booleanValue(); updateExpression.accept(this)) {
                try {
                    body.accept(this);
                } catch (BreakException breakException) {
                    break;
                } catch (ContinueException continueException) {
                    continue;
                }
            }
            return null;
        } finally {
            rho.popStackFrame();
        }
    }
    
    public Value visitForEachStatement(AstNode.ForEachStatement forEachStatement) {
        rho.pushStackFrame();
        try {
            List<String> loopVariableNames = forEachStatement.loopVariableNames();
            final int loopVariableCount = loopVariableNames.size();
            
            // FIXME: need some kind of "iterable" concept in the language.
            ListValue list = (ListValue) forEachStatement.expression().accept(this);
            // FIXME: what about modification of the list while we're looping?
            for (int key = 0; key < list.length(); ++key) {
                Value value = list.get(key);
                if (loopVariableCount == 1) {
                    rho.defineVariable(loopVariableNames.get(0), value);
                } else {
                    rho.defineVariable(loopVariableNames.get(0), new IntegerValue(key));
                    rho.defineVariable(loopVariableNames.get(1), value);
                }
                try {
                    forEachStatement.body().accept(this);
                } catch (BreakException breakException) {
                    break;
                } catch (ContinueException continueException) {
                    continue;
                }
            }
            return null;
        } finally {
            rho.popStackFrame();
        }
    }
    
    public Value visitFunctionCall(AstNode.FunctionCall functionCall) {
        Value instance = null;
        
        // We look for the function in the current scope unless it's a call to an instance or class method.
        TalcType searchType = null;
        if (functionCall.instance() != null) {
            // Calls to instance methods need to be looked up in the scope of the instance expression's class.
            instance = functionCall.instance().accept(this);
            searchType = instance.type();
        }
        
        //
        // FIXME: is this right here?
        //
        if (functionCall.classTypeDescriptor() != null) {
            // Calls to class methods need to be looked up in the scope of the relevant class.
            searchType = functionCall.classTypeDescriptor().type();
        }
        //
        //
        
        // So which scope?
        Scope searchScope = functionCall.scope();
        if (searchType != null) {
            searchScope = searchType.members();
            if (searchType.isInstantiatedParametricType()) {
                // Calls to methods of instantiated parametric types need to be looked up in the scope of the uninstantiated parametric type.
                searchScope = searchType.uninstantiatedParametricType().members();
            }
        }
        
        // The type checker guarantees that we'll find the function we're looking for.
        AstNode.FunctionDefinition functionDefinition = searchScope.findFunction(functionCall.functionName());
        return functionDefinition.invoke(this, instance, functionCall.arguments());
    }
    
    public Value invokeFunction(AstNode.FunctionDefinition f, Value instance, AstNode[] actualParameters) {
        List<String> formalParameters = f.formalParameterNames();
        final int parameterCount = formalParameters.size();
        
        // Evaluate the arguments in the call scope before adding any of the arguments to the function body scope.
        // This avoids problems when a formal parameter name causes shadowing.
        Value[] values = new Value[parameterCount];
        for (int i = 0; i < parameterCount; ++i) {
            values[i] = actualParameters[i].accept(this);
        }
        
        // Push a new stack frame for the function body...
        rho.pushStackFrame();
        try {
            Value result = null;
            
            if (instance != null) {
                // Set the local "this" to point to the instance in question.
                rho.defineVariable("this", instance);
            } else if (f.isConstructor()) {
                // Create a new object and fill in its fields.
                result = f.containingType().newInstance(this);
                // Set the local "this" to point to the new object.
                rho.defineVariable("this", result);
            }
            // ...assign the actual parameters to the formal parameters...
            for (int i = 0; i < parameterCount; ++i) {
                rho.defineVariable(formalParameters.get(i), values[i]);
            }
            // ...evaluate the body...
            try {
                f.body().accept(this);
            } catch (ReturnException returnException) {
                if (f.isConstructor() == false) {
                    result = returnException.value();
                }
            }
            return result;
        } finally {
            // ...and finally pop the function body stack frame.
            rho.popStackFrame();
        }
    }
    
    public Value visitFunctionDefinition(AstNode.FunctionDefinition functionDefinition) {
        return null;
    }
    
    public Value visitIfStatement(AstNode.IfStatement ifStatement) {
        List<AstNode> expressions = ifStatement.expressions();
        List<AstNode> bodies = ifStatement.bodies();
        final int expressionCount = expressions.size();
        for (int i = 0; i < expressionCount; ++i) {
            if (((BooleanValue) expressions.get(i).accept(this)).booleanValue()) {
                bodies.get(i).accept(this);
                return null;
            }
        }
        ifStatement.elseBlock().accept(this);
        return null;
    }
    
    public Value visitListLiteral(AstNode.ListLiteral listLiteral) {
        ListValue result = new ListValue();
        for (AstNode expression : listLiteral.expressions()) {
            result.push_back(expression.accept(this));
        }
        return result;
    }
    
    public Value visitReturnStatement(AstNode.ReturnStatement returnStatement) {
        throw new ReturnException(returnStatement.expression() != null ? returnStatement.expression().accept(this) : null);
    }
    
    private static class ReturnException extends ExceptionWithoutStackTrace {
        private Value value;
        
        public ReturnException(Value value) {
            this.value = value;
        }
        
        public Value value() {
            return value;
        }
    }
    
    private static class ExceptionWithoutStackTrace extends RuntimeException {
        @Override
        public synchronized Throwable fillInStackTrace() {
            // Do nothing, for performance.
            // See http://blogs.sun.com/jrose/entry/longjumps_considered_inexpensive
            return this;
        }
    }
    
    public Value visitVariableDefinition(AstNode.VariableDefinition var) {
        Value initialValue = var.initializer().accept(this);
        return rho.defineVariable(var.identifier(), initialValue);
    }
    
    public Value visitVariableName(AstNode.VariableName variableName) {
        String name = variableName.identifier();
        return (variableName.isFieldAccess()) ? ((UserDefinedClassValue) rho.valueOf("this")).getField(name) : rho.valueOf(name);
    }
    
    public Value visitWhileStatement(AstNode.WhileStatement whileStatement) {
        AstNode expression = whileStatement.expression();
        AstNode body = whileStatement.body();
        while (((BooleanValue) expression.accept(this)).booleanValue()) {
            try {
                body.accept(this);
            } catch (BreakException breakException) {
                break;
            } catch (ContinueException continueException) {
                continue;
            }
        }
        return null;
    }
}
