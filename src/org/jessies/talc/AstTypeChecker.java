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

public class AstTypeChecker implements AstVisitor<TalcType> {
    private final boolean DEBUG_TYPES = Talc.debugging('T');
    
    private AstNode.ClassDefinition currentClassDefinition = null;
    private AstNode.FunctionDefinition currentFunctionDefinition = null;
    private long creationTime;
    
    public AstTypeChecker(List<AstNode> ast) {
        creationTime = System.nanoTime();
        for (AstNode node : ast) {
            node.accept(this);
        }
    }
    
    public long creationTime() {
        return creationTime;
    }
    
    public TalcType visitAssertStatement(AstNode.AssertStatement assertStatement) {
        TalcType testType = assertStatement.testExpression().accept(this);
        expectBooleanType("assert", assertStatement, assertStatement.testExpression());
        if (assertStatement.explanatoryExpression() != null) {
            assertStatement.explanatoryExpression().accept(this);
        }
        return TalcType.VOID;
    }
    
    public TalcType visitBinaryOperator(AstNode.BinaryOperator binOp) {
        TalcType type = visitBinaryOperator0(binOp);
        binOp.setType(type);
        return type;
    }
    
    public TalcType visitBinaryOperator0(AstNode.BinaryOperator binOp) {
        switch (binOp.op()) {
            case NEG:     return checkNumeric(binOp);
            case PLUS:    return visitNumericAddOrStringConcatenation(binOp);
            case SUB:     return checkNumeric(binOp);
            case MUL:     return checkNumeric(binOp);
            case POW:     return checkNumeric(binOp);
            case DIV:     return checkNumeric(binOp);
            case MOD:     return checkInt(binOp);
            
            case SHL:     return checkInt(binOp);
            case SHR:     return checkInt(binOp);
            
            case B_AND:   return checkInt(binOp);
            case B_NOT:   return checkInt(binOp);
            case B_OR:    return checkInt(binOp);
            case B_XOR:   return checkInt(binOp);
            
            case L_NOT:   return checkBoolean(binOp);
            case L_AND:   return checkBoolean(binOp);
            case L_OR:    return checkBoolean(binOp);
            
            case FACTORIAL:      return checkInt(binOp);
            case POST_DECREMENT: return checkNumeric(binOp);
            case POST_INCREMENT: return checkNumeric(binOp);
            case PRE_DECREMENT:  return checkNumeric(binOp);
            case PRE_INCREMENT:  return checkNumeric(binOp);
            
            case EQ:             return checkEqualityTestable(binOp);
            case NE:             return checkEqualityTestable(binOp);
            // FIXME: need a language concept of "comparable" (or just look for a "compareTo" method?)
            case LE:             checkNumeric(binOp); return TalcType.BOOL;
            case GE:             checkNumeric(binOp); return TalcType.BOOL;
            case GT:             checkNumeric(binOp); return TalcType.BOOL;
            case LT:             checkNumeric(binOp); return TalcType.BOOL;
            
            case ASSIGN:         return checkAssignable(binOp.lhs(), binOp.rhs());
            case PLUS_ASSIGN:    return visitNumericAddOrStringConcatenation(binOp);
            case SUB_ASSIGN:     return checkNumeric(binOp);
            case MUL_ASSIGN:     return checkNumeric(binOp);
            case POW_ASSIGN:     return checkInt(binOp);
            case DIV_ASSIGN:     return checkNumeric(binOp);
            case MOD_ASSIGN:     return checkInt(binOp);
            case SHL_ASSIGN:     return checkInt(binOp);
            case SHR_ASSIGN:     return checkInt(binOp);
            case AND_ASSIGN:     return checkInt(binOp);
            case OR_ASSIGN:      return checkInt(binOp);
            case XOR_ASSIGN:     return checkInt(binOp);
        default:
            throw new TalcError(binOp, "unimplemented operation " + binOp.op());
        }
    }
    
    private TalcType visitNumericAddOrStringConcatenation(AstNode.BinaryOperator binOp) {
        TalcType lhsType = binOp.lhs().accept(this);
        TalcType rhsType = binOp.rhs().accept(this);
        if (lhsType == TalcType.STRING && rhsType == TalcType.STRING) {
            return lhsType;
        }
        if (lhsType == TalcType.STRING || rhsType == TalcType.STRING) {
            throw new TalcError(binOp, "both operands to string concatenation must be of string type, got " + lhsType + " and " + rhsType);
        }
        return checkNumeric(binOp);
    }
    
    private TalcType checkAssignable(AstNode lhs, AstNode rhs) {
        TalcType lhsType = lhs.accept(this);
        TalcType rhsType = rhs.accept(this);
        if (rhsType.canBeAssignedTo(lhsType) == false) {
            throw new TalcError(lhs, "type " + rhsType + " cannot be assigned to variable \"" + lhs + "\" of type " + lhsType);
        }
        return lhsType;
    }
    
    private TalcType checkBoolean(AstNode.BinaryOperator binOp) {
        return checkSingleAcceptableType(binOp, TalcType.BOOL);
    }
    
    private TalcType checkInt(AstNode.BinaryOperator binOp) {
        return checkSingleAcceptableType(binOp, TalcType.INT);
    }
    
    private TalcType checkSingleAcceptableType(AstNode.BinaryOperator binOp, TalcType singleAcceptableType) {
        AstNode lhs = binOp.lhs();
        TalcType lhsType = lhs.accept(this);
        AstNode rhs = binOp.rhs();
        if (rhs != null) {
            TalcType rhsType = rhs.accept(this);
            if (lhsType != singleAcceptableType || rhsType != singleAcceptableType) {
                throw new TalcError(rhs, "operands to "  + binOp.op() + " must be " + singleAcceptableType + ", got " + lhsType + " and " + rhsType);
            }
        } else {
            if (lhsType != singleAcceptableType) {
                throw new TalcError(lhs, "operand to " + binOp.op() + " must be " + singleAcceptableType + ", got " + lhsType);
            }
        }
        return singleAcceptableType;
    }
    
    private TalcType checkNumeric(AstNode.BinaryOperator binOp) {
        AstNode lhs = binOp.lhs();
        TalcType lhsType = lhs.accept(this);
        AstNode rhs = binOp.rhs();
        if (rhs != null) {
            TalcType rhsType = rhs.accept(this);
            if (rhsType != lhsType) {
                throw new TalcError(rhs, "operands to " + binOp.op() + " must have the same type, got " + lhsType + " and " + rhsType);
            }
        }
        if (lhsType != TalcType.INT && lhsType != TalcType.REAL) {
            throw new TalcError(lhs, "operands to " + binOp.op() + " must be numeric, got " + lhsType);
        }
        return lhsType;
    }
    
    private TalcType checkEqualityTestable(AstNode.BinaryOperator binOp) {
        // We want to avoid cases like "0 == 0 == 0" which should be a compile-time error.
        // If we interpret "(0 == 0) == 0" as "true == 0", we'll get "false", which is weird.
        // We also want to avoid "0 == 0.0", because we take Java's line that instances of different classes are non-equal.
        // It could be that Talc's whole approach for equality/inequality is broken.
        final TalcType lhsType = binOp.lhs().accept(this);
        final TalcType rhsType = binOp.rhs().accept(this);
        if ((lhsType == TalcType.BOOL || rhsType == TalcType.BOOL) && lhsType != rhsType) {
            throw new TalcError(binOp, "if one operand to " + binOp.op() + " is of type bool, so must the other be; got " + lhsType + " and " + rhsType);
        }
        if ((lhsType == TalcType.INT || rhsType == TalcType.INT) && lhsType != rhsType) {
            throw new TalcError(binOp, "if one operand to " + binOp.op() + " is of type int, so must the other be; got " + lhsType + " and " + rhsType);
        }
        if ((lhsType == TalcType.REAL || rhsType == TalcType.REAL) && lhsType != rhsType) {
            throw new TalcError(binOp, "if one operand to " + binOp.op() + " is of type real, so must the other be; got " + lhsType + " and " + rhsType);
        }
        return TalcType.BOOL;
    }
    
    public TalcType visitBlock(AstNode.Block block) {
        for (AstNode statement : block.statements()) {
            statement.accept(this);
        }
        return TalcType.VOID;
    }
    
    public TalcType visitBreakStatement(AstNode.BreakStatement node) {
        return TalcType.VOID;
    }
    
    public TalcType visitClassDefinition(AstNode.ClassDefinition classDefinition) {
        currentClassDefinition = classDefinition;
        
        // FIXME: we need to do more work here!
        
        // Visit the class' fields first.
        for (AstNode.VariableDefinition field : classDefinition.fields()) {
            field.accept(this);
        }
        
        // We visit a class' methods twice. First, we avoid visiting the bodies
        // in case they refer to other methods that won't have been visited yet.
        for (AstNode.FunctionDefinition method : classDefinition.methods()) {
            visitFunctionDefinition(method, 1);
        }
        // Next, we visit them again, visiting just the bodies this time.
        for (AstNode.FunctionDefinition method : classDefinition.methods()) {
            visitFunctionDefinition(method, 2);
        }
        
        currentClassDefinition = null;
        return null;
    }
    
    public TalcType visitConstant(AstNode.Constant constant) {
        return constant.type();
    }
    
    public TalcType visitContinueStatement(AstNode.ContinueStatement node) {
        return TalcType.VOID;
    }
    
    public TalcType visitDoStatement(AstNode.DoStatement doStatement) {
        doStatement.body().accept(this);
        expectBooleanType("do", doStatement, doStatement.expression());
        return TalcType.VOID;
    }
    
    public TalcType visitForStatement(AstNode.ForStatement forStatement) {
        if (forStatement.initializer() != null) {
            forStatement.initializer().accept(this);
        }
        expectBooleanType("for", forStatement, forStatement.conditionExpression());
        forStatement.updateExpression().accept(this);
        forStatement.body().accept(this);
        return TalcType.VOID;
    }
    
    public TalcType visitForEachStatement(AstNode.ForEachStatement forEachStatement) {
        TalcType expressionType = forEachStatement.expression().accept(this);
        forEachStatement.setExpressionType(expressionType);
        // FIXME: really, we want an "Iterable" interface, and to invoke ".iterator()" on the expression.
        TalcType keyType = expressionType.typeParameter(TalcType.K);
        TalcType valueType = expressionType.typeParameter(TalcType.V);
        // Our "string" is effectively "list<string>", but we obviously don't describe it recursively!
        // Having an Iterable interface will remove the need for this special case too.
        if (expressionType == TalcType.STRING) {
            keyType = TalcType.INT;
            valueType = TalcType.STRING;
        }
        if (valueType == null) {
            // FIXME: check this is really a list rather than just assuming!
            keyType = TalcType.INT;
            valueType = expressionType.typeParameter(TalcType.T);
        }
        if (keyType == null || valueType == null) {
            throw new TalcError(forEachStatement, "for-each expression must have a collection type, got " + expressionType + " instead");
        }
        
        List<AstNode.VariableDefinition> loopVariableDefinitions = forEachStatement.loopVariableDefinitions();
        // Two loop variables bind to the key and value types, in that order.
        if (loopVariableDefinitions.size() == 2) {
            loopVariableDefinitions.get(0).fixUpType(keyType);
            loopVariableDefinitions.get(1).fixUpType(valueType);
        } else {
            loopVariableDefinitions.get(0).fixUpType(valueType);
        }
        
        forEachStatement.body().accept(this);
        return TalcType.VOID;
    }
    
    public TalcType visitFunctionCall(AstNode.FunctionCall functionCall) {
        String functionName = functionCall.functionName();
        
        // We look for the function in the current scope unless it's a call to an instance or class method.
        TalcType searchType = null;
        TalcType instanceType = null;
        if (functionCall.instance() != null) {
            // Calls to instance methods need to be looked up in the scope of the instance expression's class.
            searchType = instanceType = functionCall.instance().accept(this);
        }
        TalcType classType = null;
        if (functionCall.classTypeDescriptor() != null) {
            // Calls to class methods need to be looked up in the scope of the relevant class.
            searchType = classType = functionCall.classTypeDescriptor().type();
            if (searchType == null) {
                throw new TalcError(functionCall, "unknown type \"" + functionCall.classTypeDescriptor() + "\"");
            }
        }
        if (functionCall.instance() != null && functionCall.classTypeDescriptor() != null) {
            throw new TalcError(functionCall, "\"" + functionName + "\" can't be both an instance method of class " + instanceType + " and a class method of class " + classType);
        }
        String what = "global function";
        if (classType != null) {
            what = "class method";
        } else if (instanceType != null) {
            what = "instance method";
        }
        what += " \"" + functionName + "\"";
        
        // So which scope?
        Scope searchScope = functionCall.scope();
        if (searchType != null) {
            searchScope = searchType.members();
            if (searchType.isInstantiatedParametricType()) {
                // Calls to methods of instantiated parametric types need to be looked up in the scope of the uninstantiated parametric type.
                searchScope = searchType.uninstantiatedParametricType().members();
            }
        }
        
        AstNode.FunctionDefinition functionDefinition = searchScope.findFunction(functionName);
        
        if (functionDefinition == null) {
            if (classType != null) {
                throw new TalcError(functionCall, "no " + what + " in class " + classType);
            } else if (instanceType != null) {
                throw new TalcError(functionCall, "no " + what + " in class " + instanceType);
            } else {
                throw new TalcError(functionCall, "no " + what + " in scope");
            }
        }
        
        functionCall.setDefinition(functionDefinition);
        
        if (functionDefinition.isConstructor()) {
            what = "constructor for class \"" + functionName + "\"";
            if (functionCall.instance() != null) {
                throw new TalcError(functionCall, what + " must be invoked via the \"new\" operator");
            }
        }
        
        AstNode[] actualParameters = functionCall.arguments();
        List<TalcType> formalParameterTypes = functionDefinition.formalParameterTypes();
        // FIXME: need a better test for this!
        if (functionDefinition.isVarArgs() == false && formalParameterTypes == null) {
            throw new TalcError(functionCall, what + " used before its definition\n" + functionDefinition.location() + "...here");
        }
        if (functionDefinition.isVarArgs() == false && actualParameters.length != formalParameterTypes.size()) {
            throw new TalcError(functionCall, what + " given wrong number of arguments (expected " + formalParameterTypes.size() + " but got " + actualParameters.length + ")");
        }
        for (int i = 0; i < actualParameters.length; ++i) {
            TalcType actualParameterType = actualParameters[i].accept(this);
            if (functionDefinition.isVarArgs()) {
                // About the only thing we can say here is that "void" is not acceptable.
                if (actualParameterType == TalcType.VOID) {
                    throw new TalcError(functionCall, "argument " + i + " to " + what + " has type " + actualParameterType);
                }
            } else {
                TalcType requiredType = resolveTypeParameters(functionCall, searchType, formalParameterTypes.get(i));
                if (actualParameterType.canBeAssignedTo(requiredType) == false) {
                    throw new TalcError(functionCall, "argument " + i + " to " + what + " has type " + actualParameterType + " but must be assignable to type " + requiredType + " (in " + searchType + ")");
                }
                functionCall.setResolvedArgumentType(i, requiredType);
            }
        }
        
        TalcType returnType = functionDefinition.returnType();
        TalcType resolvingType = (classType != null) ? classType : instanceType;
        if (resolvingType != null) {
            if (resolvingType.isUninstantiatedParametricType()) {
                throw new TalcError(functionCall, "can't resolve return type of " + what + " using uninstantiated parametric type " + resolvingType);
            }
            returnType = resolveTypeParameters(functionCall, resolvingType, returnType);
        }
        
        // We set this whether it's different from the declared return type or not, for the convenience of later phases.
        functionCall.setResolvedReturnType(returnType);
        
        if (DEBUG_TYPES) { System.err.println(what + " has declared return type " + functionDefinition.returnType() + ", resolving type " + resolvingType + ", and resolved return type " + returnType); }
        return returnType;
    }
    
    // Turn any type variables (K, V, or T) in "t" into the specific type bound in the given instantiatedType.
    private TalcType resolveTypeParameters(AstNode node, TalcType instantiatedType, TalcType t) {
        if (DEBUG_TYPES) { System.err.println("resolveTypeParameters(" + instantiatedType + ", " + t + ")"); }
        if (t.equals(TalcType.K) || t.equals(TalcType.V) || t.equals(TalcType.T)) {
            t = instantiatedType.typeParameter(t);
        } else if (t.isUninstantiatedParametricType()) {
            if (instantiatedType == null || instantiatedType.isUninstantiatedParametricType()) {
                throw new TalcError(node, "can't resolve type parameters in " + t + " using " + instantiatedType + " because the latter is not an instantiated type");
            }
            t = TalcType.instantiateType(t, instantiatedType.typeParameter(TalcType.K), instantiatedType.typeParameter(TalcType.V));
        }
        return t;
    }
    
    public TalcType visitFunctionDefinition(AstNode.FunctionDefinition functionDefinition) {
        visitFunctionDefinition(functionDefinition, 1|2);
        return null;
    }
    
    private void visitFunctionDefinition(AstNode.FunctionDefinition functionDefinition, int passBitmask) {
        // Check the function signature, if we've been asked to.
        if ((passBitmask & 1) != 0) {
            functionDefinition.fixUpTypes((currentClassDefinition != null) ? currentClassDefinition.type() : null);
            for (AstNode.VariableDefinition formalParameter : functionDefinition.formalParameters()) {
                formalParameter.accept(this);
            }
            
            // Check for dodgy constructors.
            if (functionDefinition.isConstructor() && functionDefinition.returnType() != currentClassDefinition.type()) {
                String className = currentClassDefinition.className();
                throw new TalcError(functionDefinition, "constructor for class \"" + className + "\" must have return type of \"" + className + "\"; got \"" + functionDefinition.returnType() + "\" instead");
            }
        }
        
        // Check the function body, if we've been asked to.
        if ((passBitmask & 2) != 0 && functionDefinition.body() != null) {
            currentFunctionDefinition = functionDefinition;
            functionDefinition.body().accept(this);
            currentFunctionDefinition = null;
        }
    }
    
    public TalcType visitIfStatement(AstNode.IfStatement ifStatement) {
        for (AstNode expression : ifStatement.expressions()) {
            expectBooleanType("if", ifStatement, expression);
        }
        for (AstNode block : ifStatement.bodies()) {
            block.accept(this);
        }
        ifStatement.elseBlock().accept(this);
        return TalcType.VOID;
    }
    
    private static boolean allSameType(TalcType[] types) {
        if (types.length > 0) {
            TalcType previousType = types[0];
            for (int i = 1; i < types.length; ++i) {
                if (types[i] != previousType) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public TalcType visitListLiteral(AstNode.ListLiteral listLiteral) {
        if (DEBUG_TYPES) { System.out.println("visitListLiteral()"); }
        final List<AstNode> expressions = listLiteral.expressions();
        final int expressionCount = expressions.size();
        if (expressionCount == 0) {
            // If there are no expressions, we have the empty list.
            return TalcType.LIST_OF_NOTHING;
        }
        final TalcType elementType = elementTypeOfExpressionList(new ExpressionListAccessor(expressions, expressionCount));
        final TalcType listType = TalcType.instantiateType(TalcType.LIST_OF_T, elementType, null);
        if (DEBUG_TYPES) { System.out.println("visitListLiteral() => elementType=" + elementType + " listType=" + listType); }
        return listType;
    }
    
    private TalcType elementTypeOfExpressionList(ExpressionListAccessor expressions) {
        final int expressionCount = expressions.size();
        TalcType[] expressionTypes = new TalcType[expressionCount];
        for (int i = 0; i < expressionCount; ++i) {
            expressionTypes[i] = expressions.get(i).accept(this);
            if (DEBUG_TYPES) { System.err.println("expressionTypes[" + i + "] = " + expressionTypes[i]); }
        }
        int[] classDepths = new int[expressionCount];
        while (allSameType(expressionTypes) == false) {
            int maxDepth = 0;
            for (int i = 0; i < expressionCount; ++i) {
                classDepths[i] = expressionTypes[i].depth();
                if (classDepths[i] > maxDepth) {
                    maxDepth = classDepths[i];
                }
            }
            if (DEBUG_TYPES) { System.err.println("list literal contains mixed types; moving deepest (" + maxDepth + ") closer to root..."); }
            for (int i = 0; i < expressionCount; ++i) {
                if (classDepths[i] == maxDepth) {
                    expressionTypes[i] = expressionTypes[i].superclass();
                    if (DEBUG_TYPES) { System.err.println("expressionTypes[" + i + "] = " + expressionTypes[i]); }
                }
            }
        }
        final TalcType elementType = expressionTypes[0];
        return elementType;
    }
    
    // Allows us to pass a List<AstNode> to elementTypeOfExpressionList, but also to present views onto an underlying list.
    // This is useful because AstNode.MapLiteral doesn't currently distinguish its key expressions and value expressions.
    // FIXME: we should probably just keep two separate lists.
    private static class ExpressionListAccessor {
        protected final List<AstNode> expressions;
        private final int size;
        
        protected ExpressionListAccessor(List<AstNode> expressions, int size) {
            this.expressions = expressions;
            this.size = size;
        }
        
        protected AstNode get(int i) {
            return expressions.get(i);
        }
        
        protected int size() {
            return size;
        }
    }
    
    public TalcType visitMapLiteral(AstNode.MapLiteral mapLiteral) {
        if (DEBUG_TYPES) { System.out.println("visitMapLiteral()"); }
        final List<AstNode> expressions = mapLiteral.expressions();
        final int pairCount = expressions.size() / 2;
        if (pairCount == 0) {
            // If there are no pairs, we have the empty map.
            return TalcType.MAP_OF_NOTHING;
        }
        final TalcType keyType = elementTypeOfExpressionList(new ExpressionListAccessor(expressions, pairCount) {
            @Override protected AstNode get(int i) { return expressions.get(2*i); }
        });
        final TalcType valueType = elementTypeOfExpressionList(new ExpressionListAccessor(expressions, pairCount) {
            @Override protected AstNode get(int i) { return expressions.get(2*i + 1); }
        });
        final TalcType mapType = TalcType.instantiateType(TalcType.MAP_OF_K_V, keyType, valueType);
        if (DEBUG_TYPES) { System.out.println("visitMapLiteral() => keyType=" + keyType + " valueType=" + valueType + " mapType=" + mapType); }
        return mapType;
    }
    
    public TalcType visitReturnStatement(AstNode.ReturnStatement returnStatement) {
        if (currentFunctionDefinition == null) {
            throw new TalcError(returnStatement, "can't \"return\" without an enclosing function");
        }
        TalcType returnType = currentFunctionDefinition.returnType();
        TalcType returnedType = TalcType.VOID;
        if (returnStatement.expression() != null) {
            returnedType = returnStatement.expression().accept(this);
        }
        if (currentFunctionDefinition.isConstructor()) {
            if (returnedType != TalcType.VOID) {
                throw new TalcError(returnStatement, "can't return a value from a constructor");
            }
        } else if (returnedType.canBeAssignedTo(returnType) == false) {
            throw new TalcError(returnStatement, "return expression has type " + returnedType + " but must be assignable to " + returnType);
        }
        returnStatement.setReturnType(returnType);
        // The "return" itself is a statement.
        return TalcType.VOID;
    }
    
    public TalcType visitVariableDefinition(AstNode.VariableDefinition variableDefinition) {
        TalcType actualType = null;
        if (variableDefinition.initializer() != null) {
            actualType = variableDefinition.initializer().accept(this);
        }
        
        variableDefinition.fixUpType(actualType);
        TalcType declaredType = variableDefinition.type();
        if (declaredType == TalcType.VOID) {
            throw new TalcError(variableDefinition, "variables such as \"" + variableDefinition.identifier() + "\" cannot have void type");
        }
        
        if (variableDefinition.initializer() != null) {
            if (actualType == null || actualType.canBeAssignedTo(declaredType) == false) {
                throw new TalcError(variableDefinition, "variable \"" + variableDefinition.identifier() + "\" has an initializer of type " + describeType(actualType) + " which cannot be assigned to the variable's declared type of " + describeType(declaredType) + ")");
            }
        }
        return declaredType;
    }
    
    private static String describeType(TalcType type) {
        if (type == null) {
            return "unknown type";
        } else {
            return type.toString() + " (" + type.describe() + ")";
        }
    }
    
    public TalcType visitVariableName(AstNode.VariableName variableName) {
        AstNode.VariableDefinition variableDefinition = variableName.definition();
        // Ensure we've visited the variable definition and thus fixed up its type descriptor to a type.
        variableDefinition.accept(this);
        // Record whether, during evaluation, we should look for the variable on the stack or as a field of "this".
        if (variableDefinition.isField()) {
            variableName.markAsFieldAccess();
        }
        return variableDefinition.type();
    }
    
    public TalcType visitWhileStatement(AstNode.WhileStatement whileStatement) {
        expectBooleanType("while", whileStatement, whileStatement.expression());
        whileStatement.body().accept(this);
        return TalcType.VOID;
    }
    
    private void expectBooleanType(String statementName, AstNode statement, AstNode expression) {
        TalcType type = expression.accept(this);
        if (type != TalcType.BOOL) {
            throw new TalcError(statement, "\"" + statementName + "\" conditions must have boolean type, got " + type + " instead");
        }
    }
}
