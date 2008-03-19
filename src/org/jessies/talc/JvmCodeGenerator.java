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
    private final Type fileValueType = Type.getType(FileValue.class);
    private final Type integerValueType = Type.getType(IntegerValue.class);
    private final Type listValueType = Type.getType(ListValue.class);
    private final Type matchValueType = Type.getType(MatchValue.class);
    private final Type numericValueType = Type.getType(NumericValue.class);
    private final Type realValueType = Type.getType(RealValue.class);
    private final Type stringValueType = Type.getType(StringValue.class);
    
    private final Type orgJessiesTalcFunctionsType = Type.getType(Functions.class);
    
    private final Type generatedClassType = Type.getType("LGeneratedClass;");
    
    private final Type javaLangObjectType = Type.getType(Object.class);
    
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
    
    // The class we're currently emitting code for.
    private ClassVisitor cv;
    // The method in which to dump code found at the global scope.
    // Only used when we come to the end of a method to reset mg.
    // You shouldn't ever need to reference this directly.
    private GeneratorAdapter globalMethod;
    // The method we're currently emitting code for (which may or may not be globalMethod).
    private GeneratorAdapter mg;
    
    private int nextLocal = -1;
    
    private class JvmLocalVariableAccessor implements VariableAccessor {
        private int localSlot;
        
        private JvmLocalVariableAccessor(int localSlot) {
            this.localSlot = localSlot;
        }
        
        public void emitGet() {
            mg.visitVarInsn(Opcodes.ALOAD, localSlot);
        }
        
        public void emitPut() {
            mg.visitVarInsn(Opcodes.ASTORE, localSlot);
        }
    }
    
    private class JvmStaticFieldAccessor implements VariableAccessor {
        private Type owner;
        private String name;
        private Type type;
        
        private JvmStaticFieldAccessor(Type owner, String name, Type type) {
            this.owner = owner;
            this.name = name;
            this.type = type;
        }
        
        public void emitGet() {
            mg.getStatic(owner, name, type);
        }
        
        public void emitPut() {
            mg.putStatic(owner, name, type);
        }
    }
    
    public JvmCodeGenerator(TalcClassLoader classLoader, List<AstNode> ast) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classWriter.visitSource(ast.get(0).location().sourceFilename(), null);
        compile(ast, classWriter);
        byte[] bytecode = classWriter.toByteArray();
        
        // FIXME: use this for caching rather than/as well as debugging?
        if (Talc.debugging('s') || Talc.debugging('V')) {
            saveGeneratedCode(bytecode);
        }
        
        if (Talc.debugging('v')) {
            ClassReader cr = new ClassReader(bytecode);
            CheckClassAdapter.verify(cr, false, new PrintWriter(System.err));
        }
        
        classLoader.defineClass("GeneratedClass", bytecode);
    }
    
    private void saveGeneratedCode(byte[] bytecode) {
        File filename = new File("/tmp/GeneratedClass.class");
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(bytecode);
            fos.close();
            if (Talc.debugging('V')) {
                String command = "java -cp /usr/share/java/bcel-5.2.jar:" + filename.getParent() + ":" + System.getProperty("java.class.path") + " org.apache.bcel.verifier.Verifier GeneratedClass";
                System.err.println("talc: verifying with:\n" + "  " + command);
                Functions.shell(new StringValue(command));
            }
        } catch (Exception ex) {
            System.err.println("talc: warning: couldn't write generated class file to \"" + filename + "\".");
        }
    }
    
    private void compile(List<AstNode> ast, ClassWriter classWriter) {
        this.cv = classWriter;
        if (Talc.debugging('S')) {
            cv = new TraceClassVisitor(cv, new PrintWriter(System.out));
        }
        // FIXME: what does this cost? should it be optional for end-users?
        cv = new CheckClassAdapter(cv);
        
        cv.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "GeneratedClass", null, "java/lang/Object", null);
        
        globalMethod = mg = new GeneratorAdapter(Opcodes.ACC_STATIC, new Method("<clinit>", Type.VOID_TYPE, new Type[0]), null, null, cv);
        mg.visitCode();
        emitClassInitializer();
        mg.endMethod();
        
        // It's convenient to be able to run the class, so we can point an arbitrary JVM at it to see what it thinks.
        // To enable that, generate a "public static void main(String[] args)" method.
        globalMethod = mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Method.getMethod("void main(String[])"), null, null, cv);
        mg.visitCode();
        emitMainMethod(ast);
        mg.endMethod();
        
        cv.visitEnd();
    }
    
    private void emitClassInitializer() {
        // Create the static fields corresponding to the built-in variables.
        // Their initializers should appear at the start of the constructor.
        for (AstNode.VariableDefinition builtInVariableDefinition : Scope.builtInVariableDefinitions()) {
            builtInVariableDefinition.accept(this);
            popAnythingLeftBy(builtInVariableDefinition);
        }
        mg.returnValue();
    }
    
    private void emitMainMethod(List<AstNode> ast) {
        // main has a (String[] args) argument.
        int argsLocal = 0;
        nextLocal = 1;
        
        // ARGS = new ListValue(args);
        mg.newInstance(listValueType);
        mg.dup();
        mg.visitVarInsn(Opcodes.ALOAD, argsLocal);
        mg.invokeConstructor(listValueType, new Method("<init>", Type.VOID_TYPE, new Type[] { Type.getType(String[].class) }));
        mg.putStatic(generatedClassType, "ARGS", listValueType);
        
        // Compile the user code.
        for (AstNode node : ast) {
            node.accept(this);
        }
        
        mg.returnValue();
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
            
            case EQ:        eq(binOp, "eq"); break;
            case NE:        eq(binOp, "ne"); break;
            // FIXME: there's no reason why we can't offer these relational operators on non-numeric types. But should we?
            case LE:        cmp(binOp, GeneratorAdapter.LE); break;
            case GE:        cmp(binOp, GeneratorAdapter.GE); break;
            case GT:        cmp(binOp, GeneratorAdapter.GT); break;
            case LT:        cmp(binOp, GeneratorAdapter.LT); break;
            
            case ASSIGN:            binOp.rhs().accept(this); assignTo(binOp.lhs()); break;
            case PLUS_ASSIGN:       numericAddOrStringConcatenation(binOp); assignTo(binOp.lhs()); break;
            case SUB_ASSIGN:        invokeBinaryOp(binOp, numericValueType, "subtract"); assignTo(binOp.lhs()); break;
            case MUL_ASSIGN:        invokeBinaryOp(binOp, numericValueType, "multiply"); assignTo(binOp.lhs()); break;
            case POW_ASSIGN:        invokeBinaryOp(binOp, numericValueType, "pow"); assignTo(binOp.lhs()); break;
            case DIV_ASSIGN:        invokeBinaryOp(binOp, numericValueType, "divide"); assignTo(binOp.lhs()); break;
            case MOD_ASSIGN:        invokeBinaryOp(binOp, numericValueType, "mod"); assignTo(binOp.lhs()); break;
            case SHL_ASSIGN:        invokeBinaryOp(binOp, numericValueType, "shiftLeft"); assignTo(binOp.lhs()); break;
            case SHR_ASSIGN:        invokeBinaryOp(binOp, numericValueType, "shiftRight"); assignTo(binOp.lhs()); break;
            case AND_ASSIGN:        invokeBinaryOp(binOp, numericValueType, "and"); assignTo(binOp.lhs()); break;
            case OR_ASSIGN:         invokeBinaryOp(binOp, numericValueType, "or"); assignTo(binOp.lhs()); break;
            case XOR_ASSIGN:        invokeBinaryOp(binOp, numericValueType, "xor"); assignTo(binOp.lhs()); break;
            
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
        AstNode.VariableDefinition variableDefinition = variableName.definition();
        mg.checkCast(typeForTalcType(variableDefinition.type()));
        variableDefinition.accessor().emitPut();
    }
    
    private void eq(AstNode.BinaryOperator binOp, String eqOrNe) {
        binOp.lhs().accept(this);
        binOp.rhs().accept(this);
        mg.invokeStatic(orgJessiesTalcFunctionsType, new Method(eqOrNe, booleanValueType, new Type[] { javaLangObjectType, javaLangObjectType }));
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
    
    private void assignTo(AstNode lhs) {
        AstNode.VariableName variableName = (AstNode.VariableName) lhs;
        AstNode.VariableDefinition variableDefinition = variableName.definition();
        mg.checkCast(typeForTalcType(variableDefinition.type()));
        mg.dup();
        variableDefinition.accessor().emitPut();
    }
    
    public Void visitBlock(AstNode.Block block) {
        for (AstNode statement : block.statements()) {
            statement.accept(this);
            popAnythingLeftBy(statement);
        }
        return null;
    }
    
    private void popAnythingLeftBy(AstNode node) {
        // FIXME: is there a cleaner way to do this?
        
        // If the code we generated for "statement" left a value on the stack, we need to pop it off!
        if (node instanceof AstNode.BinaryOperator || node instanceof AstNode.Constant || node instanceof AstNode.ListLiteral || node instanceof AstNode.VariableDefinition || node instanceof AstNode.VariableName) {
            mg.pop();
        } else if (node instanceof AstNode.FunctionCall) {
            // Pop unused return values from non-void functions.
            AstNode.FunctionCall functionCall = (AstNode.FunctionCall) node;
            if (functionCall.definition().returnType() != TalcType.VOID) {
                mg.pop();
            }
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
        if (constantType == TalcType.NULL || constant.constant() == null) {
            mg.visitInsn(Opcodes.ACONST_NULL);
        } else if (constantType == TalcType.BOOL) {
            mg.getStatic(booleanValueType, (constant.constant() == BooleanValue.TRUE) ? "TRUE" : "FALSE", booleanValueType);
        } else if (constantType == TalcType.INT) {
            mg.newInstance(integerValueType);
            mg.dup();
            // FIXME: we should choose the <init>(long) constructor where possible (which will be in almost every case).
            mg.push(constant.constant().toString());
            mg.push(10);
            mg.invokeConstructor(integerValueType, Method.getMethod("void <init>(String, int)"));
        } else if (constantType == TalcType.REAL) {
            mg.push(((RealValue) constant.constant()).doubleValue());
            mg.invokeStatic(realValueType, new Method("valueOf", realValueType, new Type[] { Type.DOUBLE_TYPE }));
        } else if (constantType == TalcType.STRING) {
            mg.newInstance(stringValueType);
            mg.dup();
            mg.push(constant.constant().toString());
            mg.invokeConstructor(stringValueType, Method.getMethod("void <init>(String)"));
        } else {
            throw new TalcError(constant, "don't know how to generate code for constants of type " + constantType);
        }
        return null;
    }
    
    public Void visitClassDefinition(AstNode.ClassDefinition classDefinition) {
        throw new TalcError(classDefinition, "don't know how to generate code for user-defined classes");
        //return null;
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
        if (forStatement.initializer() != null) {
            forStatement.initializer().accept(this);
            popAnythingLeftBy(forStatement.initializer());
        }
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
        // FIXME: we need some kind of "iterable" concept in the language. until then, this code assumes we're dealing with a list.
        
        ArrayList<AstNode.VariableDefinition> loopVariables = (ArrayList<AstNode.VariableDefinition>) forEachStatement.loopVariableDefinitions();
        if (loopVariables.size() == 1) {
            // The user didn't ask for the key, but we'll be needing it, so synthesize it before visiting the loop variable definitions.
            AstNode.VariableDefinition k = new AstNode.VariableDefinition(null, null, TalcType.INT, new AstNode.Constant(null, new IntegerValue(0), TalcType.INT), false);
            loopVariables.add(0, k);
        }
        for (AstNode.VariableDefinition loopVariable : loopVariables) {
            visitVariableDefinition(loopVariable);
        }
        
        LoopInfo loopInfo = enterLoop();
        
        Label headLabel = mg.newLabel();
        
        // collection: list = <expression>;
        forEachStatement.expression().accept(this);
        JvmLocalVariableAccessor collection = new JvmLocalVariableAccessor(nextLocal++);
        mg.checkCast(listValueType);
        mg.dup();
        collection.emitPut();
        
        // max: int = collection.length();
        JvmLocalVariableAccessor max = new JvmLocalVariableAccessor(nextLocal++);
        mg.invokeVirtual(listValueType, new Method("length", integerValueType, new Type[0]));
        max.emitPut();
        
        VariableAccessor k = loopVariables.get(0).accessor();
        VariableAccessor v = loopVariables.get(1).accessor();
        Type vType = typeForTalcType(loopVariables.get(1).type());
        
        // headLabel:
        mg.mark(headLabel);
        // if (k >= max) goto breakLabel;
        k.emitGet();
        max.emitGet();
        mg.invokeInterface(numericValueType, Method.getMethod("int compareTo(org.jessies.talc.NumericValue)"));
        mg.push(0);
        mg.ifICmp(GeneratorAdapter.GE, loopInfo.breakLabel);
        // v = collection.__get_item__(k);
        collection.emitGet();
        k.emitGet();
        mg.invokeVirtual(listValueType, new Method("__get_item__", javaLangObjectType, new Type[] { integerValueType }));
        mg.checkCast(vType);
        v.emitPut();
        // <body>
        forEachStatement.body().accept(this);
        // continueLabel:
        mg.mark(loopInfo.continueLabel);
        // ++k;
        k.emitGet();
        mg.invokeVirtual(integerValueType, new Method("inc", integerValueType, new Type[0]));
        k.emitPut();
        // goto headLabel;
        mg.goTo(headLabel);
        // breakLabel:
        mg.mark(loopInfo.breakLabel);
        
        leaveLoop();
        return null;
    }
    
    public Void visitFunctionCall(AstNode.FunctionCall functionCall) {
        String functionName = functionCall.functionName();
        AstNode.FunctionDefinition definition = functionCall.definition();
        AstNode[] arguments = functionCall.arguments();
        
        // Assume we're dealing with a global user-defined function...
        Type containingType = generatedClassType;
        // ...unless we know it's not.
        TalcType talcContainingType = definition.containingType();
        //System.err.println("call to " + functionName + " in type " + talcContainingType + " defined in scope " + definition.scope());
        if (talcContainingType != null) {
            containingType = typeForTalcType(talcContainingType);
        } else if (definition.scope() == null) {
            // We need a special case for built-in "global" functions.
            containingType = orgJessiesTalcFunctionsType;
        }
        
        if (definition.isVarArgs()) {
            if (arguments.length == 1) {
                arguments[0].accept(this);
                mg.invokeStatic(containingType, Method.getMethod("void " + functionName + " (java.lang.Object)"));
            } else {
                mg.push(arguments.length);
                mg.newArray(javaLangObjectType);
                for (int i = 0; i < arguments.length; ++i) {
                    mg.dup();
                    mg.push(i);
                    arguments[i].accept(this);
                    mg.checkCast(javaLangObjectType);
                    mg.arrayStore(javaLangObjectType);
                }
                mg.invokeStatic(containingType, Method.getMethod("void " + functionName + " (java.lang.Object[])"));
            }
        } else {
            Method method = methodForFunctionDefinition(definition);
            if (functionCall.instance() != null) {
                if (functionName.equals("to_s")) {
                    // A special case: for Java compatibility to_s is toString underneath.
                    mg.newInstance(stringValueType);
                    mg.dup();
                    functionCall.instance().accept(this);
                    mg.invokeVirtual(javaLangObjectType, Method.getMethod("java.lang.String toString()"));
                    mg.invokeConstructor(stringValueType, Method.getMethod("void <init>(java.lang.String)"));
                    return null;
                } else {
                    functionCall.instance().accept(this);
                    mg.checkCast(containingType);
                    
                    // FIXME: is this fall-through right?
                }
            }
            
            if (definition.isConstructor()) {
                mg.newInstance(containingType);
                mg.dup();
            }
            
            Type[] methodArgumentTypes = method.getArgumentTypes();
            for (int i = 0; i < arguments.length; ++i) {
                arguments[i].accept(this);
                mg.checkCast(methodArgumentTypes[i]);
            }
            if (functionCall.instance() != null) {
                mg.invokeVirtual(containingType, method);
            } else if (definition.isConstructor()) {
                mg.invokeConstructor(containingType, method);
            } else {
                mg.invokeStatic(containingType, method);
            }
            
            // Because we implement generics by erasure, we should "checkcast" non-void return types.
            TalcType resolvedReturnType = functionCall.resolvedReturnType();
            if (resolvedReturnType != TalcType.VOID) {
                mg.checkCast(typeForTalcType(resolvedReturnType));
            }
            
            // FIXME: check here whether 'method' exists, and fail here rather than waiting for the verifier?
            //throw new TalcError(functionCall, "don't know how to generate code for call to \"" + functionName + "\"");
        }
        return null;
    }
    
    private Type typeForTalcType(TalcType talcType) {
        if (talcType == TalcType.BOOL) {
            return booleanValueType;
        } else if (talcType == TalcType.FILE) {
            return fileValueType;
        } else if (talcType == TalcType.INT) {
            return integerValueType;
        } else if (talcType == TalcType.MATCH) {
            return matchValueType;
        } else if (talcType == TalcType.OBJECT) {
            return javaLangObjectType;
        } else if (talcType == TalcType.REAL) {
            return realValueType;
        } else if (talcType == TalcType.STRING) {
            return stringValueType;
        } else if (talcType == TalcType.VOID) {
            return Type.VOID_TYPE;
        } else if (talcType == TalcType.T || talcType == TalcType.K || talcType == TalcType.V) {
            // We implement generics by erasure.
            return javaLangObjectType;
        } else if (talcType.rawName().equals("list")) {
            // FIXME: this is a particularly big hack.
            return listValueType;
        } else {
            throw new RuntimeException("don't know how to represent TalcType " + talcType);
        }
    }
    
    private Method methodForFunctionDefinition(AstNode.FunctionDefinition definition) {
        List<TalcType> talcTypes = definition.formalParameterTypes();
        Type[] argumentTypes = new Type[talcTypes.size()];
        for (int i = 0; i < argumentTypes.length; ++i) {
            argumentTypes[i] = typeForTalcType(talcTypes.get(i));
        }
        String name;
        Type returnType;
        if (definition.isConstructor()) {
            name = "<init>";
            returnType = Type.VOID_TYPE;
        } else {
            name = definition.functionName();
            returnType = typeForTalcType(definition.returnType());
        }
        return new Method(name, returnType, argumentTypes);
    }
    
    public Void visitFunctionDefinition(AstNode.FunctionDefinition functionDefinition) {
        Method m = methodForFunctionDefinition(functionDefinition);
        mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, m, null, null, cv);
        
        // FIXME: this should be 1 for non-static methods!
        nextLocal = 0;
        for (AstNode.VariableDefinition formalParameter : functionDefinition.formalParameters()) {
            formalParameter.setAccessor(new JvmLocalVariableAccessor(nextLocal++));
        }
        
        mg.visitCode();
        functionDefinition.body().accept(this);
        if (functionDefinition.returnType() == TalcType.VOID) {
            // Void functions are allowed an implicit "return".
            // The bytecode verifier doesn't care if we have an unreachable RETURN bytecode, but it does care if we fall off the end of a method!
            mg.returnValue();
        }
        mg.endMethod();
        mg = globalMethod;
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
        int resultLocal = nextLocal++;
        mg.visitVarInsn(Opcodes.ASTORE, resultLocal);
        
        List<AstNode> expressions = listLiteral.expressions();
        for (AstNode expression : expressions) {
            // <Generate code for the expression.>
            expression.accept(this);
            
            // result.push_back(expression);
            mg.visitVarInsn(Opcodes.ALOAD, resultLocal);
            mg.swap();
            mg.invokeVirtual(listValueType, Method.getMethod("org.jessies.talc.ListValue push_back(java.lang.Object)"));
        }
        
        // We didn't need a dup in the loop above because push_back leaves the result on the stack anyway.
        // Likewise, we don't need a loadLocal here unless we never went round the loop.
        if (expressions.size() == 0) {
            mg.visitVarInsn(Opcodes.ALOAD, resultLocal);
        }
        return null;
    }
    
    public Void visitReturnStatement(AstNode.ReturnStatement returnStatement) {
        if (returnStatement.expression() != null) {
            returnStatement.expression().accept(this);
            mg.checkCast(typeForTalcType(returnStatement.returnType()));
        }
        mg.returnValue();
        return null;
    }
    
    public Void visitVariableDefinition(AstNode.VariableDefinition variableDefinition) {
        Type type = typeForTalcType(variableDefinition.type());
        VariableAccessor accessor;
        if (variableDefinition.scope() == Scope.globalScope() || variableDefinition.scope() == Scope.builtInScope()) {
            // If we're at global scope, we may need to back variables with fields.
            // Escape analysis would tell us whether or not we do, but we don't do any of that, so we have to assume the worst.
            cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, variableDefinition.identifier(), type.getDescriptor(), null, null);
            accessor = new JvmStaticFieldAccessor(generatedClassType, variableDefinition.identifier(), type);
        } else {
            // If we're at local scope, we can back variables with locals.
            accessor = new JvmLocalVariableAccessor(nextLocal++);
        }
        variableDefinition.setAccessor(accessor);
        variableDefinition.initializer().accept(this);
        mg.checkCast(type);
        mg.dup();
        accessor.emitPut();
        return null;
    }
    
    public Void visitVariableName(AstNode.VariableName variableName) {
        variableName.definition().accessor().emitGet();
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
