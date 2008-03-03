/*
 * This file is part of Talc.
 * Copyright (C) 2008 Elliott Hughes <enh@jessies.org>.
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

import java.io.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * Creates Java classes corresponding to given ASTs.
 * There's little to no attempt at optimization here.
 * 
 * All the actual class file writing is done by objectweb.org's ASM library.
 */
public class JvmCodeGenerator implements AstVisitor<Void> {
    // We use a lot of types repeatedly, so let's try to ask for any given type just once.
    private final Type booleanValueType = Type.getType(BooleanValue.class);
    private final Type integerValueType = Type.getType(IntegerValue.class);
    private final Type listValueType = Type.getType(ListValue.class);
    private final Type numericValueType = Type.getType(NumericValue.class);
    private final Type realValueType = Type.getType(RealValue.class);
    private final Type stringValueType = Type.getType(StringValue.class);
    private final Type valueType = Type.getType(Value.class);
    
    // We need the ability to track active loops to implement "break" and "continue".
    private static class LoopInfo { Label breakLabel, continueLabel; }
    private ArrayStack<LoopInfo> activeLoops = new ArrayStack<LoopInfo>();
    private LoopInfo enterLoop() {
        LoopInfo loopInfo = new LoopInfo();
        loopInfo.breakLabel = mg.newLabel();
        loopInfo.continueLabel = mg.newLabel();
        activeLoops.push(loopInfo);
        return loopInfo;
    }
    private void leaveLoop() {
        activeLoops.pop();
    }
    
    // The method we're currently emitting code for.
    private GeneratorAdapter mg;
    
    public JvmCodeGenerator(TalcClassLoader classLoader, List<AstNode> ast) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classWriter.visitSource(ast.get(0).location().getSourceFilename(), null);
        compile(ast, classWriter);
        byte[] bytecode = classWriter.toByteArray();
        
        // FIXME: use this for caching rather than debugging?
        /*
        try {
            FileOutputStream fos = new FileOutputStream("/tmp/GeneratedClass.class");
            fos.write(bytecode);
            fos.close();
        } catch (Exception ex) {
            System.err.println("talc: warning: couldn't write generated class file to /tmp.");
        }
        */
        
        classLoader.defineClass("GeneratedClass", bytecode);
    }
    
    private void compile(List<AstNode> ast, ClassWriter classWriter) {
        ClassVisitor cv = classWriter;
        if (Talc.debugging('S')) {
            cv = new TraceClassVisitor(cv, new PrintWriter(System.out));
        }
        // FIXME: what does this cost? should it be optional for end-users?
        cv = new CheckClassAdapter(cv);
        
        cv.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "GeneratedClass", null, "java/lang/Object", null);
        
        // Create the implicit constructor.
        Method m = Method.getMethod("void <init>()");
        mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC, m, null, null, cv);
        mg.visitCode();
        mg.loadThis();
        mg.invokeConstructor(Type.getType(Object.class), m);
        
        for (AstNode node : ast) {
            node.accept(this);
        }
        
        mg.returnValue();
        mg.endMethod();
        
        cv.visitEnd();
    }
    
    public Void visitBinaryOperator(AstNode.BinaryOperator binOp) {
        switch (binOp.op()) {
            case NEG:       invokeUnaryOp(binOp, numericValueType, "negate"); break;
            
            case PLUS:      numericAddOrStringConcatenation(binOp); break;
            
            case SUB:       invokeBinaryOp(binOp, numericValueType, "subtract"); break;
            case MUL:       invokeBinaryOp(binOp, numericValueType, "multiply"); break;
            case POW:       invokeBinaryOp(binOp, numericValueType, "pow"); break;
            case DIV:       invokeBinaryOp(binOp, numericValueType, "divide"); break;
            case MOD:       invokeBinaryOp(binOp, integerValueType, "mod"); break;
            
            case SHL:       invokeBinaryOp(binOp, integerValueType, "shiftLeft"); break;
            case SHR:       invokeBinaryOp(binOp, integerValueType, "shiftRight"); break;
            
            case B_AND:     invokeBinaryOp(binOp, integerValueType, "and"); break;
            case B_NOT:     invokeUnaryOp(binOp, integerValueType, "not"); break;
            case B_OR:      invokeBinaryOp(binOp, integerValueType, "or"); break;
            case B_XOR:     invokeBinaryOp(binOp, integerValueType, "xor"); break;
            
            case L_NOT:     l_not(binOp); break;
            case L_AND:     l_shortCircuit(binOp, "FALSE"); break;
            case L_OR:      l_shortCircuit(binOp, "TRUE"); break;
            
            case FACTORIAL: invokeUnaryOp(binOp, integerValueType, "factorial"); break;
            
            case POST_DECREMENT: prePostIncrementDecrement(binOp, false, false); break;
            case POST_INCREMENT: prePostIncrementDecrement(binOp, false, true); break;
            case PRE_DECREMENT:  prePostIncrementDecrement(binOp, true, false); break;
            case PRE_INCREMENT:  prePostIncrementDecrement(binOp, true, true); break;
            
            case EQ:        eq(binOp, "TRUE", "FALSE"); break;
            case NE:        eq(binOp, "FALSE", "TRUE"); break;
            //// FIXME: there's no reason why we can't offer these relational operators on non-numeric types. But should we?
            case LE:        cmp(binOp, GeneratorAdapter.LE); break;
            case GE:        cmp(binOp, GeneratorAdapter.GE); break;
            case GT:        cmp(binOp, GeneratorAdapter.GT); break;
            case LT:        cmp(binOp, GeneratorAdapter.LT); break;
            
            case ASSIGN:    assign(binOp); break;
            //case ASSIGN:         result = assignTo(lhs, binOp.rhs().accept(this)); break;
            //case PLUS_ASSIGN:    result = assignTo(lhs, visitNumericAddOrStringConcatenation(binOp)); break;
            //case SUB_ASSIGN:     result = assignTo(lhs, lhsNumber(binOp).subtract(rhsNumber(binOp))); break;
            //case MUL_ASSIGN:     result = assignTo(lhs, lhsNumber(binOp).multiply(rhsNumber(binOp))); break;
            //case POW_ASSIGN:     result = assignTo(lhs, lhsNumber(binOp).pow(rhsNumber(binOp))); break;
            //case DIV_ASSIGN:     result = assignTo(lhs, lhsNumber(binOp).divide(rhsNumber(binOp))); break;
            //case MOD_ASSIGN:     result = assignTo(lhs, rhsInt(binOp).mod(rhsInt(binOp))); break;
            //case SHL_ASSIGN:     result = assignTo(lhs, rhsInt(binOp).shiftLeft(rhsInt(binOp))); break;
            //case SHR_ASSIGN:     result = assignTo(lhs, rhsInt(binOp).shiftRight(rhsInt(binOp))); break;
            //case AND_ASSIGN:     result = assignTo(lhs, rhsInt(binOp).and(rhsInt(binOp))); break;
            //case OR_ASSIGN:      result = assignTo(lhs, rhsInt(binOp).or(rhsInt(binOp))); break;
            //case XOR_ASSIGN:     result = assignTo(lhs, rhsInt(binOp).xor(rhsInt(binOp))); break;
            
        default:
            throw new TalcError(binOp, "don't know how to generate code for " + binOp.op());
        }
        return null;
    }
    
    private void numericAddOrStringConcatenation(AstNode.BinaryOperator binOp) {
        if (binOp.type() == TalcType.STRING) {
            mg.newInstance(stringValueType);
            mg.dup();
            binOp.lhs().accept(this);
            binOp.rhs().accept(this);
            mg.invokeConstructor(stringValueType, Method.getMethod("void <init>(org.jessies.talc.StringValue, org.jessies.talc.StringValue)"));
        } else {
            invokeBinaryOp(binOp, numericValueType, "add");
        }
    }
    
    // Leaves ((binOp.lhs() == BooleanValue.TRUE) ? BooleanValue.FALSE : BooleanValue.TRUE) on the stack.
    // FIXME: is it worth having this manual inline? i did it like this because it was the first binOp i implemented. if i were coming to it now, i'd use invokeVirtual.
    private void l_not(AstNode.BinaryOperator binOp) {
        binOp.lhs().accept(this);
        
        Label pushFalseLabel = mg.newLabel();
        Label doneLabel = mg.newLabel();
        
        mg.getStatic(booleanValueType, "TRUE", booleanValueType);
        mg.ifCmp(booleanValueType, GeneratorAdapter.EQ, pushFalseLabel);
        mg.getStatic(booleanValueType, "TRUE", booleanValueType);
        mg.goTo(doneLabel);
        mg.mark(pushFalseLabel);
        mg.getStatic(booleanValueType, "FALSE", booleanValueType);
        mg.mark(doneLabel);
    }
    
    // Short-circuits the evaluation of binOp.rhs() if the binOp.lhs() is the BooleanValue corresponding to 'trueOrFalse'.
    // Implements either && or ||, depending on whether you supply "FALSE" or "TRUE".
    private void l_shortCircuit(AstNode.BinaryOperator binOp, String trueOrFalse) {
        Label shortCircuitLabel = mg.newLabel();
        Label doneLabel = mg.newLabel();
        
        binOp.lhs().accept(this);
        mg.getStatic(booleanValueType, trueOrFalse, booleanValueType);
        mg.ifCmp(booleanValueType, GeneratorAdapter.EQ, shortCircuitLabel);
        binOp.rhs().accept(this);
        mg.goTo(doneLabel);
        mg.mark(shortCircuitLabel);
        mg.getStatic(booleanValueType, trueOrFalse, booleanValueType);
        mg.mark(doneLabel);
    }
    
    private void prePostIncrementDecrement(AstNode.BinaryOperator binOp, boolean isPre, boolean isIncrement) {
        // Get the initial value on the stack.
        binOp.lhs().accept(this);
        // For post-increment/decrement, we want to return this.
        if (isPre == false) {
            mg.dup();
        }
        // Add/subtract 1.
        Type type = (binOp.type() == TalcType.INT) ? integerValueType : realValueType;
        mg.getStatic(type, "ONE", type);
        mg.invokeInterface(numericValueType, new Method(isIncrement ? "add" : "subtract", numericValueType, new Type[] { numericValueType }));
        // For pre-increment/decrement, we want to return this.
        if (isPre) {
            mg.dup();
        }
        // Store the new value.
        AstNode.VariableName variableName = (AstNode.VariableName) binOp.lhs();
        mg.storeLocal(variableName.definition().local());
    }
    
    // Compares binOp.lhs() and binOp.rhs() using Object.equals.
    // Returns the BooleanValue corresponding eqResult if they're equal, and neResult otherwise.
    // Implements == and !=.
    private void eq(AstNode.BinaryOperator binOp, String eqResult, String neResult) {
        Label equalLabel = mg.newLabel();
        Label doneLabel = mg.newLabel();
        
        binOp.lhs().accept(this);
        binOp.rhs().accept(this);
        mg.invokeVirtual(Type.getType(Object.class), Method.getMethod("boolean equals(java.lang.Object)"));
        mg.ifZCmp(GeneratorAdapter.NE, equalLabel);
        mg.getStatic(booleanValueType, neResult, booleanValueType);
        mg.goTo(doneLabel);
        mg.mark(equalLabel);
        mg.getStatic(booleanValueType, eqResult, booleanValueType);
        mg.mark(doneLabel);
    }
    
    private void cmp(AstNode.BinaryOperator binOp, int comparison) {
        Label equalLabel = mg.newLabel();
        Label doneLabel = mg.newLabel();
        
        // Equivalent to: BooleanValue.valueOf(lhsNumber(binOp).compareTo(rhsNumber(binOp)) <comparison> 0);
        binOp.lhs().accept(this);
        binOp.rhs().accept(this);
        mg.invokeInterface(numericValueType, Method.getMethod("int compareTo(org.jessies.talc.NumericValue)"));
        mg.ifZCmp(comparison, equalLabel);
        mg.getStatic(booleanValueType, "FALSE", booleanValueType);
        mg.goTo(doneLabel);
        mg.mark(equalLabel);
        mg.getStatic(booleanValueType, "TRUE", booleanValueType);
        mg.mark(doneLabel);
    }
    
    private void invokeUnaryOp(AstNode.BinaryOperator binOp, Type type, String name) {
        binOp.lhs().accept(this);
        if (type == numericValueType) {
            mg.invokeInterface(type, new Method(name, type, new Type[0]));
        } else {
            mg.invokeVirtual(type, new Method(name, type, new Type[0]));
        }
    }
    
    private void invokeBinaryOp(AstNode.BinaryOperator binOp, Type type, String name) {
        binOp.lhs().accept(this);
        binOp.rhs().accept(this);
        if (type == numericValueType) {
            mg.invokeInterface(type, new Method(name, type, new Type[] { type }));
        } else {
            mg.invokeVirtual(type, new Method(name, type, new Type[] { type }));
        }
    }
    
    private void assign(AstNode.BinaryOperator binOp) {
        binOp.rhs().accept(this);
        AstNode.VariableName variableName = (AstNode.VariableName) binOp.lhs();
        mg.storeLocal(variableName.definition().local());
    }
    
    public Void visitBlock(AstNode.Block block) {
        for (AstNode statement : block.statements()) {
            statement.accept(this);
            popAnythingLeftBy(statement);
        }
        return null;
    }
    
    private void popAnythingLeftBy(AstNode node) {
        // If the code we generated for "statement" left a value on the stack, we need to pop it off!
        // FIXME: is there a cleaner way to do this?
        // FIXME: what about AstNode.FunctionCall?
        if (node instanceof AstNode.BinaryOperator || node instanceof AstNode.Constant || node instanceof AstNode.ListLiteral || node instanceof AstNode.VariableName) {
            mg.pop();
        }
    }
    
    public Void visitBreakStatement(AstNode.BreakStatement breakStatement) {
        LoopInfo loopInfo = activeLoops.peek();
        mg.goTo(loopInfo.breakLabel);
        return null;
    }
    
    public Void visitConstant(AstNode.Constant constant) {
        // FIXME: this is all very unfortunate. life would be simpler if we'd use Java's "built-in" Boolean and String types, and do something a bit cleverer for "int", too.
        TalcType constantType = constant.type();
        if (constantType == TalcType.BOOLEAN) {
            mg.getStatic(booleanValueType, (constant.constant() == BooleanValue.TRUE) ? "TRUE" : "FALSE", booleanValueType);
        } else if (constantType == TalcType.INT) {
            mg.newInstance(integerValueType);
            mg.dup();
            // FIXME: we should choose the <init>(long) constructor where possible (which will be in almost every case).
            mg.push(constant.constant().toString());
            mg.push(10);
            mg.invokeConstructor(integerValueType, Method.getMethod("void <init>(String, int)"));
        } else if (constantType == TalcType.NULL) {
            mg.visitInsn(Opcodes.ACONST_NULL);
        } else if (constantType == TalcType.REAL) {
            mg.newInstance(realValueType);
            mg.dup();
            mg.push(((RealValue) constant.constant()).doubleValue());
            mg.invokeConstructor(realValueType, Method.getMethod("void <init>(double)"));
        } else if (constantType == TalcType.STRING) {
            mg.newInstance(stringValueType);
            mg.dup();
            mg.push(constant.constant().toString());
            mg.invokeConstructor(stringValueType, Method.getMethod("void <init>(String)"));
        } else {
            throw new TalcError(constant, "don't know how to generate code for constants of this type");
        }
        return null;
    }
    
    public Void visitClassDefinition(AstNode.ClassDefinition classDefinition) {
        return null;
    }
    
    public Void visitContinueStatement(AstNode.ContinueStatement continueStatement) {
        LoopInfo loopInfo = activeLoops.peek();
        mg.goTo(loopInfo.continueLabel);
        return null;
    }
    
    public Void visitDoStatement(AstNode.DoStatement doStatement) {
        LoopInfo loopInfo = enterLoop();
        
        // continueLabel:
        mg.mark(loopInfo.continueLabel);
        // <body>
        doStatement.body().accept(this);
        // if (<expression> == false) goto breakLabel;
        doStatement.expression().accept(this);
        mg.getStatic(booleanValueType, "FALSE", booleanValueType);
        mg.ifCmp(booleanValueType, GeneratorAdapter.EQ, loopInfo.breakLabel);
        // goto continueLabel;
        mg.goTo(loopInfo.continueLabel);
        // breakLabel:
        mg.mark(loopInfo.breakLabel);
        
        leaveLoop();
        return null;
    }
    
    public Void visitForStatement(AstNode.ForStatement forStatement) {
        LoopInfo loopInfo = enterLoop();
        
        Label headLabel = mg.newLabel();
        
        // <initializer>
        if (forStatement.initializer() != null) forStatement.initializer().accept(this);
        // headLabel:
        mg.mark(headLabel);
        // if (<condition> == false) goto breakLabel;
        forStatement.conditionExpression().accept(this);
        mg.getStatic(booleanValueType, "FALSE", booleanValueType);
        mg.ifCmp(booleanValueType, GeneratorAdapter.EQ, loopInfo.breakLabel);
        // <body>
        forStatement.body().accept(this);
        // continueLabel:
        mg.mark(loopInfo.continueLabel);
        // <update-expression>
        forStatement.updateExpression().accept(this);
        popAnythingLeftBy(forStatement.updateExpression());
        // goto headLabel;
        mg.goTo(headLabel);
        // breakLabel:
        mg.mark(loopInfo.breakLabel);
        
        leaveLoop();
        return null;
    }
    
    public Void visitForEachStatement(AstNode.ForEachStatement forEachStatement) {
        return null;
    }
    
    public Void visitFunctionCall(AstNode.FunctionCall functionCall) {
        String functionName = functionCall.functionName();
        // FIXME: generalize this!
        if (functionName.equals("puts") || functionName.equals("print")) {
            AstNode[] arguments = functionCall.arguments();
            if (arguments.length == 1) {
                arguments[0].accept(this);
                mg.invokeStatic(Type.getType(Functions.class), Method.getMethod("void " + functionName + " (org.jessies.talc.Value)"));
            } else {
                mg.push(arguments.length);
                mg.newArray(valueType);
                for (int i = 0; i < arguments.length; ++i) {
                    mg.dup();
                    mg.push(i);
                    arguments[i].accept(this);
                    mg.arrayStore(valueType);
                }
                mg.invokeStatic(Type.getType(Functions.class), Method.getMethod("void " + functionName + " (org.jessies.talc.Value[])"));
            }
        } else {
            throw new TalcError(functionCall, "don't know how to generate code for call to \"" + functionName + "\"");
        }
        return null;
    }
    
    public Void visitFunctionDefinition(AstNode.FunctionDefinition functionDefinition) {
        return null;
    }
    
    // Implements "if" statements.
    // The structure we generate is, I think, unusual, chosen because it seemed simplest to me.
    public Void visitIfStatement(AstNode.IfStatement ifStatement) {
        List<AstNode> expressions = ifStatement.expressions();
        List<AstNode> bodies = ifStatement.bodies();
        final int expressionCount = expressions.size();
        
        // We have a label for each expression...
        Label[] labels = new Label[expressionCount];
        for (int i = 0; i < labels.length; ++i) {
            labels[i] = mg.newLabel();
        }
        // ...a label for any expressionless "else" block...
        Label elseLabel = mg.newLabel();
        // ...and a label for the end of the whole "if" statement.
        Label doneLabel = mg.newLabel();
        
        // Unlike most compilers, we actually keep all the expressions together in a sort of "jump table"...
        for (int i = 0; i < expressionCount; ++i) {
            expressions.get(i).accept(this);
            mg.getStatic(booleanValueType, "TRUE", booleanValueType);
            mg.ifCmp(booleanValueType, GeneratorAdapter.EQ, labels[i]);
        }
        mg.goTo(elseLabel);
        
        // ...that jumps to the appropriate block.
        for (int i = 0; i < expressionCount; ++i) {
            mg.mark(labels[i]);
            bodies.get(i).accept(this);
            mg.goTo(doneLabel);
        }
        
        // If no expression was true, we'll jump here, to the "else" block.
        // The else block may be empty, in which case we'll just fall through to the end of the whole "if" statement.
        mg.mark(elseLabel);
        ifStatement.elseBlock().accept(this);
        
        mg.mark(doneLabel);
        
        return null;
    }
    
    public Void visitListLiteral(AstNode.ListLiteral listLiteral) {
        // ListValue result = new ListValue();
        mg.newInstance(listValueType);
        mg.dup();
        mg.invokeConstructor(listValueType, Method.getMethod("void <init>()"));
        int resultLocal = mg.newLocal(listValueType);
        mg.storeLocal(resultLocal);
        
        for (AstNode expression : listLiteral.expressions()) {
            // <Generate code for the expression.>
            expression.accept(this);
            
            // result.push_back(expression);
            mg.loadLocal(resultLocal);
            mg.swap();
            mg.invokeVirtual(listValueType, Method.getMethod("org.jessies.talc.ListValue push_back(org.jessies.talc.Value)"));
        }
        
        // We don't need to use loadLocal (or a dup in the loop above) because push_back leaves the result on the stack anyway.
        //mg.loadLocal(resultLocal);
        return null;
    }
    
    public Void visitReturnStatement(AstNode.ReturnStatement returnStatement) {
        return null;
    }
    
    public Void visitVariableDefinition(AstNode.VariableDefinition variableDefinition) {
        // FIXME: we can't assume that all variables are locals!
        int local = mg.newLocal(valueType);
        variableDefinition.setLocal(local);
        variableDefinition.initializer().accept(this);
        mg.storeLocal(local);
        return null;
    }
    
    public Void visitVariableName(AstNode.VariableName variableName) {
        // FIXME: we can't assume that all variables are locals!
        mg.loadLocal(variableName.definition().local());
        return null;
    }
    
    public Void visitWhileStatement(AstNode.WhileStatement whileStatement) {
        LoopInfo loopInfo = enterLoop();
        
        // continueLabel:
        mg.mark(loopInfo.continueLabel);
        // if (<expression> == false) goto breakLabel;
        whileStatement.expression().accept(this);
        mg.getStatic(booleanValueType, "FALSE", booleanValueType);
        mg.ifCmp(booleanValueType, GeneratorAdapter.EQ, loopInfo.breakLabel);
        // <body>
        whileStatement.body().accept(this);
        // goto continueLabel;
        mg.goTo(loopInfo.continueLabel);
        // breakLabel:
        mg.mark(loopInfo.breakLabel);
        
        leaveLoop();
        return null;
    }
}
