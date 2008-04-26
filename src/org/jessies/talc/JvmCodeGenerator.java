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
import org.jessies.talc.bytecode.*;

/**
 * Creates Java classes corresponding to given ASTs.
 * There's little to no attempt at optimization here.
 */
public class JvmCodeGenerator implements AstVisitor<Void> {
    // We use some types repeatedly, so let's try to ask for any given type just once.
    // FIXME: these "types" are actually class names. We should say so.
    // FIXME: we often want the corresponding signatures. Add constants for them?
    private static final String integerValueType = "org/jessies/talc/IntegerValue";
    private static final String listValueType = "org/jessies/talc/ListValue";
    private static final String realValueType = "org/jessies/talc/RealValue";
    
    private static final String generatedClassType = "GeneratedClass";
    
    private static final String javaLangObjectType = "java/lang/Object";
    private static final String javaLangStringType = "java/lang/String";
    
    // Our highly sophisticated register allocator.
    // Also used to set the value of the eponymous field of the Code attribute.
    private short maxLocals;
    
    // We need the ability to track active loops to implement "break" and "continue".
    private static class LoopInfo { int breakLabel, continueLabel; }
    private ArrayStack<LoopInfo> activeLoops = new ArrayStack<LoopInfo>();
    private LoopInfo enterLoop() {
        LoopInfo loopInfo = new LoopInfo();
        loopInfo.breakLabel = cv.acquireLabel();
        loopInfo.continueLabel = cv.acquireLabel();
        activeLoops.push(loopInfo);
        return loopInfo;
    }
    private void leaveLoop() {
        activeLoops.pop();
    }
    
    // The class we're currently emitting code for.
    private ClassFileWriter cv;
    
    private TalcClassLoader classLoader;
    
    private class JvmLocalVariableAccessor implements VariableAccessor {
        private int variable;
        
        private JvmLocalVariableAccessor(String identifier, String signature, int variable) {
            this.variable = variable;
            cv.addVariableDescriptor(identifier, signature, cv.getCurrentCodeOffset(), variable);
        }
        
        public void emitGet() {
            cv.addALoad(variable);
        }
        
        public void emitPut() {
            cv.addAStore(variable);
        }
    }
    
    private class JvmFieldAccessor implements VariableAccessor {
        private String className;
        private String fieldName;
        private String fieldType;
        private boolean isStatic;
        
        private JvmFieldAccessor(String className, String fieldName, String fieldType, boolean isStatic) {
            this.className = className;
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.isStatic = isStatic;
        }
        
        public void emitGet() {
            if (isStatic) {
                cv.add(ByteCode.GETSTATIC, className, fieldName, fieldType);
            } else {
                cv.add(ByteCode.ALOAD_0);
                cv.add(ByteCode.GETFIELD, className, fieldName, fieldType);
            }
        }
        
        public void emitPut() {
            if (isStatic) {
                cv.add(ByteCode.PUTSTATIC, className, fieldName, fieldType);
            } else {
                cv.add(ByteCode.ALOAD_0);
                cv.add(ByteCode.SWAP);
                cv.add(ByteCode.PUTFIELD, className, fieldName, fieldType);
            }
        }
    }
    
    /**
     * JVM constant pools can only contain primitives or java.lang.Strings.
     * We make use of that (via ClassFileWriter), but Talc "int" and "real"
     * constants, being boxed in IntegerValue and RealValue, can't be stored
     * in the constant pool.
     * 
     * To work round this, we have our own "Talc constant pool", implemented
     * as a "private static final Object[]" field. We collect constants during
     * code generation, add a call to <clinit> to a special __init_constants__
     * method, and generate code to write the constants into the array after
     * we're finished generating user code.
     */
    private JvmTalcConstantPool talcConstantPool;
    private class JvmTalcConstantPool {
        private final String owner;
        private static final String constantsFieldName = "$__talc_constants";
        private static final String constantsFieldSignature = "[Ljava/lang/Object;";
        
        private ArrayList<Object> constants = new ArrayList<Object>();
        private HashMap<Object, Integer> constantIndexes = new HashMap<Object, Integer>();
        
        private JvmTalcConstantPool(String owner) {
            this.owner = owner;
            cv.addField(constantsFieldName, constantsFieldSignature, (short) (ClassFileWriter.ACC_PRIVATE | ClassFileWriter.ACC_STATIC | ClassFileWriter.ACC_FINAL));
        }
        
        public void addConstantAndEmitCode(Object constant, String type) {
            if (Talc.debugging('C')) {
                emitConstant(constant);
            } else {
                // FIXME: don't store duplicates.
                Integer constantIndex = constantIndexes.get(constant);
                if (constantIndex == null) {
                    constantIndex = constants.size();
                    constants.add(constant);
                    constantIndexes.put(constant, constantIndex);
                }
                emitGet(constantIndex, type);
            }
        }
        
        private void emitGet(int index, String type) {
            cv.add(ByteCode.GETSTATIC, owner, constantsFieldName, constantsFieldSignature);
            cv.addPush(index);
            cv.add(ByteCode.AALOAD);
            cv.add(ByteCode.CHECKCAST, type);
        }
        
        public void emitCallToTalcConstantPoolInitializer() {
            if (Talc.debugging('C')) {
                return;
            }
            cv.addInvoke(ByteCode.INVOKESTATIC, owner, "__init_constants__", "()V");
        }
        
        public void emitTalcConstantPoolInitializer() {
            if (Talc.debugging('C')) {
                return;
            }
            
            maxLocals = 0;
            cv.startMethod("__init_constants__", "()V", (short) (ClassFileWriter.ACC_PRIVATE | ClassFileWriter.ACC_STATIC));
            
            // $talc_constants = new Object[constants.size()];
            cv.addPush(constants.size());
            cv.add(ByteCode.ANEWARRAY, javaLangObjectType);
            
            for (int i = 0; i < constants.size(); ++i) {
                cv.add(ByteCode.DUP);
                cv.addPush(i);
                emitConstant(constants.get(i));
                cv.add(ByteCode.AASTORE);
            }
            cv.add(ByteCode.PUTSTATIC, owner, constantsFieldName, constantsFieldSignature);
            cv.add(ByteCode.RETURN);
            
            cv.stopMethod(maxLocals);
        }
        
        private void emitConstant(Object constant) {
            if (constant instanceof RealValue) {
                RealValue realValue = (RealValue) constant;
                cv.addPush(realValue.doubleValue());
                cv.addInvoke(ByteCode.INVOKESTATIC, realValueType, "valueOf", "(D)L" + realValueType + ";");
            } else {
                IntegerValue integerValue = (IntegerValue) constant;
                cv.add(ByteCode.NEW, integerValueType);
                cv.add(ByteCode.DUP);
                // FIXME: we should choose valueOf(long) where possible (which will be in almost every case).
                cv.addPush(integerValue.toString());
                cv.addPush(10);
                cv.addInvoke(ByteCode.INVOKESPECIAL, integerValueType, "<init>", "(Ljava/lang/String;I)V");
            }
        }
    }
    
    public JvmCodeGenerator(TalcClassLoader classLoader, List<AstNode> ast) {
        this.classLoader = classLoader;
        compile(ast);
    }
    
    private void defineClass(String className, byte[] bytecode) {
        // FIXME: use 's' for caching rather than/as well as debugging?
        if (Talc.debugging('s') || Talc.debugging('S') || Talc.debugging('v')) {
            File filename = saveGeneratedCode(className, bytecode);
            // FIXME: would be nice to have our own built-in disassembler.
            if (Talc.debugging('S')) {
                disassemble(className, filename);
            }
            // FIXME: verification failures should probably stop us in our tracks.
            if (Talc.debugging('v')) {
                verify(filename);
            }
        }
        classLoader.defineClass(className, bytecode);
    }
    
    private static File saveGeneratedCode(String className, byte[] bytecode) {
        File filename = new File("/tmp/" + className + ".class");
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(bytecode);
            fos.close();
        } catch (Exception ex) {
            System.err.println("talc: warning: couldn't write generated class file to \"" + filename + "\".");
        }
        return filename;
    }
    
    private static void disassemble(String className, File filename) {
        String command = "javap -c -l -private -v -classpath " + filename.getParent() + " " + className;
        System.err.println("talc: disassembling with:\n" + "  " + command);
        Functions.shell(command);
    }
    
    private static void verify(File filename) {
        // Gather up all the ASM jar files from /usr/share/java/.
        // FIXME: Talc will need some kind of "glob" facility, and we could re-use it here.
        ListValue classpath = new ListValue();
        for (File jar : new File("/usr/share/java/").listFiles()) {
            if (jar.getName().endsWith(".jar")) {
                if (jar.getName().startsWith("asm")) {
                    classpath.push_back(jar);
                }
            }
        }
        // Add the directory we wrote the generated code to.
        classpath.push_back(filename.getParent());
        // Add our own classpath (for the Talc runtime classes).
        classpath.push_back(System.getProperty("java.class.path"));
        
        // Call the external verifier.
        String command = "java -cp " + classpath.join(":") + " org.objectweb.asm.util.CheckClassAdapter " + filename;
        System.err.println("talc: verifying with:\n" + "  " + command);
        Functions.shell(command);
    }
    
    private void compile(List<AstNode> ast) {
        String sourceFilename = ast.get(0).location().sourceFilename();
        this.cv = new ClassFileWriter(generatedClassType, javaLangObjectType, sourceFilename);
        cv.setFlags(ClassFileWriter.ACC_PUBLIC);
        emitClassInitializer(generatedClassType);
        
        // It's convenient to be able to run the class, so we can point an arbitrary JVM at it to see what it thinks.
        // To enable that, generate a "public static void main(String[] args)" method.
        // This method also generates the code corresponding to global function definitions.
        maxLocals = 0;
        cv.startMethod("main", "([Ljava/lang/String;)V", (short) (ClassFileWriter.ACC_PUBLIC | ClassFileWriter.ACC_STATIC));
        
        // main has a (String[] args) argument.
        int argsLocal = maxLocals++;
        
        // ARGS = new ListValue(args);
        cv.add(ByteCode.NEW, listValueType);
        cv.add(ByteCode.DUP);
        cv.addALoad(argsLocal);
        cv.addInvoke(ByteCode.INVOKESPECIAL, listValueType, "<init>", "([Ljava/lang/String;)V");
        cv.add(ByteCode.PUTSTATIC, generatedClassType, "ARGS", "Lorg/jessies/talc/ListValue;");
        
        // Compile the global code, saving global functions and user-defined classes for later.
        ArrayList<AstNode.FunctionDefinition> functionDefinitions = new ArrayList<AstNode.FunctionDefinition>();
        ArrayList<AstNode.ClassDefinition> classDefinitions = new ArrayList<AstNode.ClassDefinition>();
        for (AstNode node : ast) {
            if (node instanceof AstNode.ClassDefinition) {
                classDefinitions.add((AstNode.ClassDefinition) node);
            } else if (node instanceof AstNode.FunctionDefinition) {
                functionDefinitions.add((AstNode.FunctionDefinition) node);
            } else {
                node.accept(this);
                popAnythingLeftBy(node);
            }
        }
        
        //mg.popScope();
        cv.add(ByteCode.RETURN);
        cv.stopMethod(maxLocals);
        
        // Now we've finished with the global code, we can go back over the global functions.
        emitGlobalFunctions(functionDefinitions);
        
        talcConstantPool.emitTalcConstantPoolInitializer();
        
        defineClass(generatedClassType, cv.toByteArray());
        cv = null;
        
        emitUserDefinedClasses(classDefinitions);
    }
    
    private void emitClassInitializer(String className) {
        maxLocals = 0;
        cv.startMethod("<clinit>", "()V", ClassFileWriter.ACC_STATIC);
        
        // Create a constant pool for Talc-level constants.
        talcConstantPool = new JvmTalcConstantPool(className);
        talcConstantPool.emitCallToTalcConstantPoolInitializer();
        
        if (className.equals(generatedClassType)) {
            // Create and initialize the static fields corresponding to the built-in variables.
            for (AstNode.VariableDefinition builtInVariableDefinition : Scope.builtInVariableDefinitions()) {
                builtInVariableDefinition.accept(this);
                popAnythingLeftBy(builtInVariableDefinition);
            }
        }
        
        cv.add(ByteCode.RETURN);
        cv.stopMethod(maxLocals);
    }
    
    private void emitGlobalFunctions(List<AstNode.FunctionDefinition> functionDefinitions) {
        for (AstNode.FunctionDefinition functionDefinition : functionDefinitions) {
            visitFunctionDefinition(functionDefinition);
        }
    }
    
    private void emitUserDefinedClasses(List<AstNode.ClassDefinition> classDefinitions) {
        for (AstNode.ClassDefinition classDefinition : classDefinitions) {
            visitClassDefinition(classDefinition);
        }
    }
    
    public Void visitBinaryOperator(AstNode.BinaryOperator binOp) {
        switch (binOp.op()) {
            case NEG:            invokeUnaryOp(binOp, "negate"); break;
            
            case PLUS:           numericAddOrStringConcatenation(binOp); break;
            
            case SUB:            invokeBinaryOp(binOp, "subtract"); break;
            case MUL:            invokeBinaryOp(binOp, "multiply"); break;
            case POW:            invokeBinaryOp(binOp, "pow"); break;
            case DIV:            invokeBinaryOp(binOp, "divide"); break;
            case MOD:            invokeBinaryOp(binOp, "mod"); break;
            
            case SHL:            invokeBinaryOp(binOp, "shiftLeft"); break;
            case SHR:            invokeBinaryOp(binOp, "shiftRight"); break;
            
            case B_AND:          invokeBinaryOp(binOp, "and"); break;
            case B_NOT:          invokeUnaryOp(binOp, "not"); break;
            case B_OR:           invokeBinaryOp(binOp, "or"); break;
            case B_XOR:          invokeBinaryOp(binOp, "xor"); break;
            
            case L_NOT:          l_not(binOp); break;
            case L_AND:          l_shortCircuit(binOp, "FALSE"); break;
            case L_OR:           l_shortCircuit(binOp, "TRUE"); break;
            
            case FACTORIAL:      invokeUnaryOp(binOp, "factorial"); break;
            
            case POST_DECREMENT: prePostIncrementDecrement(binOp, false, false); break;
            case POST_INCREMENT: prePostIncrementDecrement(binOp, false, true); break;
            case PRE_DECREMENT:  prePostIncrementDecrement(binOp, true, false); break;
            case PRE_INCREMENT:  prePostIncrementDecrement(binOp, true, true); break;
            
            case EQ:             eq(binOp, "eq"); break;
            case NE:             eq(binOp, "ne"); break;
            
            case LE:             cmp(binOp, ByteCode.IFLE); break;
            case GE:             cmp(binOp, ByteCode.IFGE); break;
            case GT:             cmp(binOp, ByteCode.IFGT); break;
            case LT:             cmp(binOp, ByteCode.IFLT); break;
            
            case ASSIGN:         binOp.rhs().accept(this); assignTo(binOp.lhs()); break;
            case PLUS_ASSIGN:    numericAddOrStringConcatenation(binOp); assignTo(binOp.lhs()); break;
            case SUB_ASSIGN:     invokeBinaryOp(binOp, "subtract"); assignTo(binOp.lhs()); break;
            case MUL_ASSIGN:     invokeBinaryOp(binOp, "multiply"); assignTo(binOp.lhs()); break;
            case POW_ASSIGN:     invokeBinaryOp(binOp, "pow"); assignTo(binOp.lhs()); break;
            case DIV_ASSIGN:     invokeBinaryOp(binOp, "divide"); assignTo(binOp.lhs()); break;
            case MOD_ASSIGN:     invokeBinaryOp(binOp, "mod"); assignTo(binOp.lhs()); break;
            case SHL_ASSIGN:     invokeBinaryOp(binOp, "shiftLeft"); assignTo(binOp.lhs()); break;
            case SHR_ASSIGN:     invokeBinaryOp(binOp, "shiftRight"); assignTo(binOp.lhs()); break;
            case AND_ASSIGN:     invokeBinaryOp(binOp, "and"); assignTo(binOp.lhs()); break;
            case OR_ASSIGN:      invokeBinaryOp(binOp, "or"); assignTo(binOp.lhs()); break;
            case XOR_ASSIGN:     invokeBinaryOp(binOp, "xor"); assignTo(binOp.lhs()); break;
            
        default:
            throw new TalcError(binOp, "ICE: don't know how to generate code for " + binOp.op());
        }
        return null;
    }
    
    private void numericAddOrStringConcatenation(AstNode.BinaryOperator binOp) {
        if (binOp.type() == TalcType.STRING) {
            // FIXME: should generalize to cope with arbitrary chains of concatenation (though probably above this level).
            binOp.lhs().accept(this);
            binOp.rhs().accept(this);
            visitLineNumber(binOp);
            cv.addInvoke(ByteCode.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;");
        } else {
            invokeBinaryOp(binOp, "add");
        }
    }
    
    private void l_not(AstNode.BinaryOperator binOp) {
        binOp.lhs().accept(this);
        visitLineNumber(binOp);
        cv.addInvoke(ByteCode.INVOKEVIRTUAL, "org/jessies/talc/BooleanValue", "not", "()Lorg/jessies/talc/BooleanValue;");
    }
    
    // Short-circuits the evaluation of binOp.rhs() if the binOp.lhs() is the BooleanValue corresponding to 'trueOrFalse'.
    // Implements either && or ||, depending on whether you supply "FALSE" or "TRUE".
    private void l_shortCircuit(AstNode.BinaryOperator binOp, String trueOrFalse) {
        int shortCircuitLabel = cv.acquireLabel();
        int doneLabel = cv.acquireLabel();
        
        binOp.lhs().accept(this);
        visitLineNumber(binOp);
        pushTrueOrFalse(trueOrFalse);
        cv.add(ByteCode.IF_ACMPEQ, shortCircuitLabel);
        binOp.rhs().accept(this);
        visitLineNumber(binOp);
        cv.add(ByteCode.GOTO, doneLabel);
        cv.markLabel(shortCircuitLabel);
        pushTrueOrFalse(trueOrFalse);
        cv.markLabel(doneLabel);
    }
    
    private void prePostIncrementDecrement(AstNode.BinaryOperator binOp, boolean isPre, boolean isIncrement) {
        // Get the initial value on the stack.
        binOp.lhs().accept(this);
        visitLineNumber(binOp);
        // For post-increment/decrement, we want to return the value we currently have on the top of the stack.
        if (isPre == false) {
            cv.add(ByteCode.DUP);
        }
        // Increment/decrement.
        String type = typeForTalcType(binOp.type());
        cv.addInvoke(ByteCode.INVOKEVIRTUAL, type, (isIncrement ? "increment" : "decrement"), "()L" + type + ";");
        // For pre-increment/decrement, we want to return the value we currently have on the top of the stack.
        if (isPre) {
            cv.add(ByteCode.DUP);
        }
        // Store the new value.
        AstNode.VariableName variableName = (AstNode.VariableName) binOp.lhs();
        AstNode.VariableDefinition variableDefinition = variableName.definition();
        cv.add(ByteCode.CHECKCAST, typeForTalcType(variableDefinition.type()));
        variableDefinition.accessor().emitPut();
    }
    
    private void eq(AstNode.BinaryOperator binOp, String eqOrNe) {
        binOp.lhs().accept(this);
        binOp.rhs().accept(this);
        visitLineNumber(binOp);
        cv.addInvoke(ByteCode.INVOKESTATIC, "org/jessies/talc/Functions", eqOrNe, "(Ljava/lang/Object;Ljava/lang/Object;)Lorg/jessies/talc/BooleanValue;");
    }
    
    private void cmp(AstNode.BinaryOperator binOp, int jumpOpcode) {
        int equalLabel = cv.acquireLabel();
        int doneLabel = cv.acquireLabel();
        
        // Equivalent to: BooleanValue.valueOf(lhsNumber(binOp).compareTo(rhsNumber(binOp)) <comparison> 0);
        binOp.lhs().accept(this);
        binOp.rhs().accept(this);
        visitLineNumber(binOp);
        cv.addInvoke(ByteCode.INVOKEINTERFACE, "java/lang/Comparable", "compareTo", "(Ljava/lang/Object;)I");
        cv.add(jumpOpcode, equalLabel);
        pushFalse();
        cv.add(ByteCode.GOTO, doneLabel);
        cv.markLabel(equalLabel);
        pushTrue();
        cv.markLabel(doneLabel);
    }
    
    private void invokeUnaryOp(AstNode.BinaryOperator binOp, String name) {
        binOp.lhs().accept(this);
        String type = typeForTalcType(binOp.type());
        visitLineNumber(binOp);
        cv.addInvoke(ByteCode.INVOKEVIRTUAL, type, name, "()L" + type + ";");
    }
    
    private void invokeBinaryOp(AstNode.BinaryOperator binOp, String name) {
        binOp.lhs().accept(this);
        binOp.rhs().accept(this);
        String type = typeForTalcType(binOp.type());
        visitLineNumber(binOp);
        cv.addInvoke(ByteCode.INVOKEVIRTUAL, type, name, "(L" + type + ";)L" + type + ";");
    }
    
    private void assignTo(AstNode lhs) {
        visitLineNumber(lhs);
        AstNode.VariableName variableName = (AstNode.VariableName) lhs;
        AstNode.VariableDefinition variableDefinition = variableName.definition();
        cv.add(ByteCode.CHECKCAST, typeForTalcType(variableDefinition.type()));
        cv.add(ByteCode.DUP);
        variableDefinition.accessor().emitPut();
    }
    
    public Void visitBlock(AstNode.Block block) {
        //mg.pushScope();
        for (AstNode statement : block.statements()) {
            statement.accept(this);
            popAnythingLeftBy(statement);
        }
        //mg.popScope();
        return null;
    }
    
    private void popAnythingLeftBy(AstNode node) {
        // FIXME: is there a cleaner way to do this?
        
        // If the code we generated for "statement" left a value on the stack, we need to pop it off!
        if (node instanceof AstNode.BinaryOperator || node instanceof AstNode.Constant || node instanceof AstNode.ListLiteral || node instanceof AstNode.VariableDefinition || node instanceof AstNode.VariableName) {
            cv.add(ByteCode.POP);
        } else if (node instanceof AstNode.FunctionCall) {
            // Pop unused return values from non-void functions.
            AstNode.FunctionCall functionCall = (AstNode.FunctionCall) node;
            if (functionCall.definition().returnType() != TalcType.VOID) {
                cv.add(ByteCode.POP);
            }
        }
    }
    
    public Void visitBreakStatement(AstNode.BreakStatement breakStatement) {
        visitLineNumber(breakStatement);
        LoopInfo loopInfo = activeLoops.peek();
        cv.add(ByteCode.GOTO, loopInfo.breakLabel);
        return null;
    }
    
    public Void visitConstant(AstNode.Constant constant) {
        visitLineNumber(constant);
        // FIXME: this is all very unfortunate. life would be simpler if we'd use Java's "built-in" Boolean type, and do something a bit cleverer for "int", too.
        TalcType constantType = constant.type();
        if (constantType == TalcType.NULL || constant.constant() == null) {
            cv.add(ByteCode.ACONST_NULL);
        } else if (constantType == TalcType.BOOL) {
            pushTrueOrFalse((constant.constant() == BooleanValue.TRUE) ? "TRUE" : "FALSE");
        } else if (constantType == TalcType.INT) {
            talcConstantPool.addConstantAndEmitCode(constant.constant(), integerValueType);
        } else if (constantType == TalcType.REAL) {
            talcConstantPool.addConstantAndEmitCode(constant.constant(), realValueType);
        } else if (constantType == TalcType.STRING) {
            // FIXME: .class files have 64KiB limits on UTF-8 constants, so we might want to break long strings up.
            cv.addPush(constant.constant().toString());
        } else {
            throw new TalcError(constant, "ICE: don't know how to generate code for constants of type " + constantType);
        }
        return null;
    }
    
    public Void visitClassDefinition(AstNode.ClassDefinition classDefinition) {
        String className = classDefinition.className();
        String sourceFilename = classDefinition.location().sourceFilename();
        this.cv = new ClassFileWriter(className, javaLangObjectType, sourceFilename);
        cv.setFlags(ClassFileWriter.ACC_PUBLIC);
        emitClassInitializer(className);
        
        // Generate a method before adding the fields, so we've somewhere to
        // put code for the field initializers; constructors will invoke this
        // method rather than duplicate the initialization.
        maxLocals = 1;
        cv.startMethod("__init_fields__", "()V", ClassFileWriter.ACC_PRIVATE);
        for (AstNode.VariableDefinition field : classDefinition.fields()) {
            visitVariableDefinition(field);
        }
        cv.add(ByteCode.RETURN);
        cv.stopMethod(maxLocals);
        
        for (AstNode.FunctionDefinition method : classDefinition.methods()) {
            visitFunctionDefinition(method);
        }
        
        talcConstantPool.emitTalcConstantPoolInitializer();
        
        defineClass(className, cv.toByteArray());
        cv = null;
        return null;
    }
    
    public Void visitContinueStatement(AstNode.ContinueStatement continueStatement) {
        visitLineNumber(continueStatement);
        LoopInfo loopInfo = activeLoops.peek();
        cv.add(ByteCode.GOTO, loopInfo.continueLabel);
        return null;
    }
    
    public Void visitDoStatement(AstNode.DoStatement doStatement) {
        LoopInfo loopInfo = enterLoop();
        
        // continueLabel:
        cv.markLabel(loopInfo.continueLabel);
        // <body>
        doStatement.body().accept(this);
        // if (<expression> == false) goto breakLabel;
        doStatement.expression().accept(this);
        visitLineNumber(doStatement);
        pushFalse();
        cv.add(ByteCode.IF_ACMPEQ, loopInfo.breakLabel);
        // goto continueLabel;
        cv.add(ByteCode.GOTO, loopInfo.continueLabel);
        // breakLabel:
        cv.markLabel(loopInfo.breakLabel);
        
        leaveLoop();
        return null;
    }
    
    public Void visitForStatement(AstNode.ForStatement forStatement) {
        LoopInfo loopInfo = enterLoop();
        
        int headLabel = cv.acquireLabel();
        
        // <initializer>
        if (forStatement.initializer() != null) {
            forStatement.initializer().accept(this);
            popAnythingLeftBy(forStatement.initializer());
        }
        // headLabel:
        cv.markLabel(headLabel);
        // if (<condition> == false) goto breakLabel;
        forStatement.conditionExpression().accept(this);
        visitLineNumber(forStatement);
        pushFalse();
        cv.add(ByteCode.IF_ACMPEQ, loopInfo.breakLabel);
        // <body>
        forStatement.body().accept(this);
        // continueLabel:
        cv.markLabel(loopInfo.continueLabel);
        // <update-expression>
        forStatement.updateExpression().accept(this);
        visitLineNumber(forStatement);
        popAnythingLeftBy(forStatement.updateExpression());
        // goto headLabel;
        cv.add(ByteCode.GOTO, headLabel);
        // breakLabel:
        cv.markLabel(loopInfo.breakLabel);
        
        leaveLoop();
        return null;
    }
    
    public Void visitForEachStatement(AstNode.ForEachStatement forEachStatement) {
        // FIXME: we need some kind of "iterable" concept in the language. until then, this code assumes we're dealing with a list.
        
        visitLineNumber(forEachStatement);
        ArrayList<AstNode.VariableDefinition> loopVariables = (ArrayList<AstNode.VariableDefinition>) forEachStatement.loopVariableDefinitions();
        AstNode.VariableDefinition kDefinition;
        if (loopVariables.size() == 1) {
            // The user didn't ask for the key, but we'll be needing it, so synthesize it before visiting the loop variable definitions.
            kDefinition = new AstNode.VariableDefinition(null, "$key", TalcType.INT, null, false);
            loopVariables.add(0, kDefinition);
        } else {
            // The user did ask for the key, but won't (can't!) have supplied an initializer.
            kDefinition = loopVariables.get(0);
        }
        kDefinition.setInitializer(new AstNode.Constant(null, IntegerValue.valueOf(0), TalcType.INT));
        
        for (AstNode.VariableDefinition loopVariable : loopVariables) {
            visitVariableDefinition(loopVariable);
            popAnythingLeftBy(loopVariable);
        }
        
        LoopInfo loopInfo = enterLoop();
        
        int headLabel = cv.acquireLabel();
        
        // collection: list = <expression>;
        forEachStatement.expression().accept(this);
        visitLineNumber(forEachStatement);
        JvmLocalVariableAccessor collection = new JvmLocalVariableAccessor("$collection", ClassFileWriter.classNameToSignature(listValueType), maxLocals++);
        cv.add(ByteCode.CHECKCAST, listValueType);
        cv.add(ByteCode.DUP);
        collection.emitPut();
        
        // max: int = collection.length();
        JvmLocalVariableAccessor max = new JvmLocalVariableAccessor("$max", ClassFileWriter.classNameToSignature(integerValueType), maxLocals++);
        cv.addInvoke(ByteCode.INVOKEVIRTUAL, listValueType, "size", "()Lorg/jessies/talc/IntegerValue;");
        max.emitPut();
        
        VariableAccessor k = loopVariables.get(0).accessor();
        VariableAccessor v = loopVariables.get(1).accessor();
        String vType = typeForTalcType(loopVariables.get(1).type());
        
        // headLabel:
        cv.markLabel(headLabel);
        // if (k >= max) goto breakLabel;
        k.emitGet();
        max.emitGet();
        cv.addInvoke(ByteCode.INVOKEINTERFACE, "java/lang/Comparable", "compareTo", "(Ljava/lang/Object;)I");
        cv.addPush(0);
        cv.add(ByteCode.IF_ICMPGE, loopInfo.breakLabel);
        // v = collection.__get_item__(k);
        collection.emitGet();
        k.emitGet();
        cv.addInvoke(ByteCode.INVOKEVIRTUAL, listValueType, "__get_item__", "(Lorg/jessies/talc/IntegerValue;)Ljava/lang/Object;");
        cv.add(ByteCode.CHECKCAST, vType);
        v.emitPut();
        // <body>
        forEachStatement.body().accept(this);
        // continueLabel:
        cv.markLabel(loopInfo.continueLabel);
        // ++k;
        visitLineNumber(forEachStatement);
        k.emitGet();
        cv.addInvoke(ByteCode.INVOKEVIRTUAL, integerValueType, "inc", "()Lorg/jessies/talc/IntegerValue;");
        k.emitPut();
        // goto headLabel;
        cv.add(ByteCode.GOTO, headLabel);
        // breakLabel:
        cv.markLabel(loopInfo.breakLabel);
        
        leaveLoop();
        return null;
    }
    
    public Void visitFunctionCall(AstNode.FunctionCall functionCall) {
        visitLineNumber(functionCall);
        String functionName = functionCall.functionName();
        AstNode.FunctionDefinition definition = functionCall.definition();
        AstNode[] arguments = functionCall.arguments();
        
        // Assume we're dealing with a global user-defined function...
        String containingType = generatedClassType;
        // ...unless we know it's not.
        TalcType talcContainingType = definition.containingType();
        //System.err.println("call to " + functionName + " in type " + talcContainingType + " defined in scope " + definition.scope());
        if (talcContainingType != null) {
            containingType = typeForTalcType(talcContainingType);
        } else if (definition.scope() == null) {
            // We need a special case for built-in "global" functions.
            containingType = "org/jessies/talc/Functions";
        }
        
        // FIXME: generalize this.
        String proxyType = null;
        String proxyFirstArgumentType = null;
        if (containingType == javaLangStringType) {
            proxyType = "org/jessies/talc/StringFunctions";
            proxyFirstArgumentType = containingType;
        }
        
        if (definition.isVarArgs()) {
            if (arguments.length == 1) {
                arguments[0].accept(this);
                cv.addInvoke(ByteCode.INVOKESTATIC, containingType, functionName, "(Ljava/lang/Object;)V");
            } else {
                cv.addPush(arguments.length);
                cv.add(ByteCode.ANEWARRAY, javaLangObjectType);
                for (int i = 0; i < arguments.length; ++i) {
                    cv.add(ByteCode.DUP);
                    cv.addPush(i);
                    arguments[i].accept(this);
                    cv.add(ByteCode.CHECKCAST, javaLangObjectType);
                    cv.add(ByteCode.AASTORE);
                }
                cv.addInvoke(ByteCode.INVOKESTATIC, containingType, functionName, "([Ljava/lang/Object;)V");
            }
        } else {
            if (definition.isConstructor()) {
                cv.add(ByteCode.NEW, containingType);
                cv.add(ByteCode.DUP);
            } else if (functionCall.instance() != null) {
                functionCall.instance().accept(this);
                if (functionName.equals("to_s")) {
                    // A special case: for Java compatibility to_s is toString underneath.
                    cv.addInvoke(ByteCode.INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
                    return null;
                } else {
                    if (proxyFirstArgumentType != null) {
                        // If proxyFirstArgumentType is non-null, you intend to invoke a method on
                        // a proxy class, which means it's static method, which means you need an
                        // extra first argument to take the place of "this", and proxyFirstArgumentType
                        // is the type of that argument, *not* the type of the class containing
                        // the method.
                        cv.add(ByteCode.CHECKCAST, proxyFirstArgumentType);
                    } else {
                        cv.add(ByteCode.CHECKCAST, containingType);
                    }
                }
            } else if (definition.containingType() != null) {
                cv.add(ByteCode.ALOAD_0);
            }
            
            List<TalcType> formalParameterTypes = definition.formalParameterTypes();
            for (int i = 0; i < arguments.length; ++i) {
                arguments[i].accept(this);
                
                // Emit a "checkcast", just in case.
                // FIXME: really, we shouldn't ever be in this position. We should take care of this on the return from generic methods. (Anywhere else?)
                cv.add(ByteCode.CHECKCAST, typeForTalcType(formalParameterTypes.get(i)));
            }
            
            String name = definition.functionName();
            String methodSignature = methodSignature(definition);
            if (proxyType != null) {
                // We have to insert the argument representing "this" in these static methods.
                methodSignature = "(" + ClassFileWriter.classNameToSignature(containingType) + methodSignature.substring(1);
                cv.addInvoke(ByteCode.INVOKESTATIC, proxyType, name, methodSignature);
            } else if (definition.isConstructor()) {
                cv.addInvoke(ByteCode.INVOKESPECIAL, containingType, "<init>", methodSignature);
            } else if (functionCall.instance() != null) {
                // Explicit "obj.m()".
                cv.addInvoke(ByteCode.INVOKEVIRTUAL, containingType, name, methodSignature);
            } else if (definition.containingType() != null) {
                // Implicit "this.m()" (i.e. "m()" inside another method).
                cv.addInvoke(ByteCode.INVOKEVIRTUAL, containingType, name, methodSignature);
            } else {
                cv.addInvoke(ByteCode.INVOKESTATIC, containingType, name, methodSignature);
            }
            
            // Because we implement generics by erasure, we should "checkcast" non-void return types.
            // FIXME: we only need to do this in a generic context.
            TalcType resolvedReturnType = functionCall.resolvedReturnType();
            if (resolvedReturnType != TalcType.VOID) {
                cv.add(ByteCode.CHECKCAST, typeForTalcType(resolvedReturnType));
            }
            
            // FIXME: check here whether 'method' exists, and fail here rather than waiting for the verifier?
            //throw new TalcError(functionCall, "don't know how to generate code for call to \"" + functionName + "\"");
        }
        return null;
    }
    
    private String typeForTalcType(TalcType talcType) {
        if (talcType == TalcType.BOOL) {
            return "org/jessies/talc/BooleanValue";
        } else if (talcType == TalcType.FILE) {
            return "org/jessies/talc/FileValue";
        } else if (talcType == TalcType.INT) {
            return integerValueType;
        } else if (talcType == TalcType.MATCH) {
            return "org/jessies/talc/MatchValue";
        } else if (talcType == TalcType.OBJECT) {
            return javaLangObjectType;
        } else if (talcType == TalcType.REAL) {
            return realValueType;
        } else if (talcType == TalcType.STRING) {
            return javaLangStringType;
        } else if (talcType == TalcType.VOID) {
            return "V";
        } else if (talcType == TalcType.T || talcType == TalcType.K || talcType == TalcType.V) {
            // We implement generics by erasure.
            return javaLangObjectType;
        } else if (talcType.rawName().equals("list")) {
            // FIXME: this is a particularly big hack.
            return listValueType;
        } else if (talcType.rawName().equals("map")) {
            // FIXME: this is a particularly big hack.
            return "org/jessies/talc/MapValue";
        } else if (talcType.isUserDefined()) {
            return talcType.rawName();
        } else {
            throw new RuntimeException("don't know how to represent TalcType " + talcType);
        }
    }
    
    private String methodSignature(AstNode.FunctionDefinition definition) {
        StringBuffer result = new StringBuffer("(");
        for (TalcType talcType : definition.formalParameterTypes()) {
            result.append(ClassFileWriter.classNameToSignature(typeForTalcType(talcType)));
        }
        result.append(")");
        if (definition.isConstructor() || definition.returnType() == TalcType.VOID) {
            result.append("V");
        } else {
            result.append(ClassFileWriter.classNameToSignature(typeForTalcType(definition.returnType())));
        }
        return result.toString();
    }
    
    public Void visitFunctionDefinition(AstNode.FunctionDefinition functionDefinition) {
        String functionName = functionDefinition.functionName();
        
        short flags = ClassFileWriter.ACC_PUBLIC;
        if (functionDefinition.isConstructor()) {
            functionName = "<init>";
        }
        
        if (functionDefinition.scope() == Scope.globalScope()) {
            flags |= ClassFileWriter.ACC_STATIC;
        }
        
        if (functionName.equals("to_s")) {
            functionName = "toString";
        }
        
        maxLocals = 0;
        cv.startMethod(functionName, methodSignature(functionDefinition), flags);
        
        visitLineNumber(functionDefinition);
        
        // For non-static methods, "this" is argument 0 and the arguments start from 1.
        JvmLocalVariableAccessor thisAccessor = null;
        String containingClassName = null;
        String containingClassSignature = null;
        if (functionDefinition.containingType() != null) {
            containingClassName = typeForTalcType(functionDefinition.containingType());
            containingClassSignature = ClassFileWriter.classNameToSignature(containingClassName);
        }
        if ((flags & ClassFileWriter.ACC_STATIC) == 0) {
            thisAccessor = new JvmLocalVariableAccessor("this", containingClassSignature, maxLocals++);
        }
        
        for (AstNode.VariableDefinition formalParameter : functionDefinition.formalParameters()) {
            String formalParameterSignature = ClassFileWriter.classNameToSignature(typeForTalcType(formalParameter.type()));
            formalParameter.setAccessor(new JvmLocalVariableAccessor(formalParameter.identifier(), formalParameterSignature, maxLocals++));
        }
        
        //mg.pushScope();
        if (functionDefinition.isConstructor()) {
            // Constructors need to call their superclass constructor.
            thisAccessor.emitGet();
            // FIXME: not all classes will have java/lang/Object as their superclass!
            cv.addInvoke(ByteCode.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
            thisAccessor.emitGet();
            cv.addInvoke(ByteCode.INVOKEVIRTUAL, containingClassName, "__init_fields__", "()V");
        }
        functionDefinition.body().accept(this);
        //mg.popScope();
        
        if (functionDefinition.isConstructor() || functionDefinition.returnType() == TalcType.VOID) {
            // Void functions in Talc are allowed an implicit "return".
            // The bytecode verifier doesn't care if we have an unreachable RETURN bytecode, but it does care if we fall off the end of a method!
            cv.add(ByteCode.RETURN);
        }
        
        cv.stopMethod(maxLocals);
        return null;
    }
    
    // Implements "if" statements.
    // The structure we generate is, I think, unusual; chosen because it seemed simplest to me.
    public Void visitIfStatement(AstNode.IfStatement ifStatement) {
        List<AstNode> expressions = ifStatement.expressions();
        List<AstNode> bodies = ifStatement.bodies();
        final int expressionCount = expressions.size();
        
        // We have a label for each expression...
        int[] labels = new int[expressionCount];
        for (int i = 0; i < labels.length; ++i) {
            labels[i] = cv.acquireLabel();
        }
        // ...a label for any expressionless "else" block...
        int elseLabel = cv.acquireLabel();
        // ...and a label for the end of the whole "if" statement.
        int doneLabel = cv.acquireLabel();
        
        // Unlike most compilers, we actually keep all the expressions together in a sort of "jump table"...
        for (int i = 0; i < expressionCount; ++i) {
            expressions.get(i).accept(this);
            pushTrue();
            cv.add(ByteCode.IF_ACMPEQ, labels[i]);
        }
        cv.add(ByteCode.GOTO, elseLabel);
        
        // ...that jumps to the appropriate block.
        for (int i = 0; i < expressionCount; ++i) {
            cv.markLabel(labels[i]);
            bodies.get(i).accept(this);
            cv.add(ByteCode.GOTO, doneLabel);
        }
        
        // If no expression was true, we'll jump here, to the "else" block.
        // The else block may be empty, in which case we'll just fall through to the end of the whole "if" statement.
        cv.markLabel(elseLabel);
        ifStatement.elseBlock().accept(this);
        
        cv.markLabel(doneLabel);
        // We need this in case this "if" is the last statement in a method.
        // If it is, then a goto to doneLabel would jump past the end of the code.
        // The ASM and BCEL verifiers don't mind (perhaps because they can see the gotos aren't taken), but the JVM verifier rejects such code.
        cv.add(ByteCode.NOP);
        return null;
    }
    
    public Void visitListLiteral(AstNode.ListLiteral listLiteral) {
        // ListValue result = new ListValue();
        visitLineNumber(listLiteral);
        cv.add(ByteCode.NEW, listValueType);
        cv.add(ByteCode.DUP);
        cv.addInvoke(ByteCode.INVOKESPECIAL, listValueType, "<init>", "()V");
        //mg.pushScope();
        int result = maxLocals++;
        cv.add(ByteCode.DUP);
        cv.addAStore(result);
        
        List<AstNode> expressions = listLiteral.expressions();
        for (AstNode expression : expressions) {
            // <Generate code for the expression.>
            expression.accept(this);
            
            // result.push_back(expression);
            visitLineNumber(listLiteral);
            cv.addInvoke(ByteCode.INVOKEVIRTUAL, listValueType, "push_back", "(Ljava/lang/Object;)Lorg/jessies/talc/ListValue;");
        }
        
        //mg.popScope();
        return null;
    }
    
    public Void visitReturnStatement(AstNode.ReturnStatement returnStatement) {
        if (returnStatement.expression() != null) {
            returnStatement.expression().accept(this);
            cv.add(ByteCode.CHECKCAST, typeForTalcType(returnStatement.returnType()));
            visitLineNumber(returnStatement);
            cv.add(ByteCode.ARETURN);
            return null;
        }
        
        visitLineNumber(returnStatement);
        cv.add(ByteCode.RETURN);
        return null;
    }
    
    public Void visitVariableDefinition(AstNode.VariableDefinition variableDefinition) {
        String type = typeForTalcType(variableDefinition.type());
        String signature = ClassFileWriter.classNameToSignature(type);
        VariableAccessor accessor;
        if (variableDefinition.scope() == Scope.globalScope() || variableDefinition.scope() == Scope.builtInScope()) {
            // If we're at global scope, we may need to back variables with fields.
            // Escape analysis would tell us whether or not we do, but we don't do any of that, so we have to assume the worst.
            short access = ClassFileWriter.ACC_PRIVATE | ClassFileWriter.ACC_STATIC;
            if (variableDefinition.isFinal()) {
                access |= ClassFileWriter.ACC_FINAL;
            }
            
            cv.addField(variableDefinition.identifier(), signature, access);
            accessor = new JvmFieldAccessor(cv.getClassName(), variableDefinition.identifier(), signature, true);
        } else if (variableDefinition.isField()) {
            short access = ClassFileWriter.ACC_PRIVATE;
            if (variableDefinition.isFinal()) {
                access |= ClassFileWriter.ACC_FINAL;
            }
            
            cv.addField(variableDefinition.identifier(), signature, access);
            accessor = new JvmFieldAccessor(cv.getClassName(), variableDefinition.identifier(), signature, false);
        } else {
            // If we're at local scope, we can back variables with locals.
            accessor = new JvmLocalVariableAccessor(variableDefinition.identifier(), signature, maxLocals++);
        }
        variableDefinition.setAccessor(accessor);
        variableDefinition.initializer().accept(this);
        visitLineNumber(variableDefinition);
        cv.add(ByteCode.CHECKCAST, type);
        cv.add(ByteCode.DUP);
        accessor.emitPut();
        return null;
    }
    
    public Void visitVariableName(AstNode.VariableName variableName) {
        visitLineNumber(variableName);
        variableName.definition().accessor().emitGet();
        return null;
    }
    
    public Void visitWhileStatement(AstNode.WhileStatement whileStatement) {
        LoopInfo loopInfo = enterLoop();
        
        // continueLabel:
        cv.markLabel(loopInfo.continueLabel);
        // if (<expression> == false) goto breakLabel;
        whileStatement.expression().accept(this);
        visitLineNumber(whileStatement);
        pushFalse();
        cv.add(ByteCode.IF_ACMPEQ, loopInfo.breakLabel);
        // <body>
        whileStatement.body().accept(this);
        // goto continueLabel;
        visitLineNumber(whileStatement);
        cv.add(ByteCode.GOTO, loopInfo.continueLabel);
        // breakLabel:
        cv.markLabel(loopInfo.breakLabel);
        
        leaveLoop();
        return null;
    }
    
    private void pushTrue() {
        pushTrueOrFalse("TRUE");
    }
    
    private void pushFalse() {
        pushTrueOrFalse("FALSE");
    }
    
    private void pushTrueOrFalse(String trueOrFalse) {
        cv.add(ByteCode.GETSTATIC, "org/jessies/talc/BooleanValue", trueOrFalse, "Lorg/jessies/talc/BooleanValue;");
    }
    
    private void visitLineNumber(AstNode node) {
        if (node != null) {
            SourceLocation location = node.location();
            if (location != null) {
                cv.addLineNumberEntry((short) location.lineNumber());
            }
        }
    }
}
