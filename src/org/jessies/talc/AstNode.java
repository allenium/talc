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

public abstract class AstNode {
    protected SourceLocation location;
    private Scope scope;
    
    public abstract <ResultT> ResultT accept(AstVisitor<ResultT> visitor);
    
    public SourceLocation location() {
        return location;
    }
    
    public Scope scope() {
        return scope;
    }
    
    public void setScope(Scope scope) {
        this.scope = scope;
    }
    
    public static class AssertStatement extends AstNode {
        private AstNode testExpression;
        private AstNode explanatoryExpression;
        
        public AssertStatement(SourceLocation location, AstNode testExpression, AstNode explanatoryExpression) {
            this.location = location;
            this.testExpression = testExpression;
            this.explanatoryExpression = explanatoryExpression;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitAssertStatement(this);
        }
        
        public AstNode testExpression() {
            return testExpression;
        }
        
        public AstNode explanatoryExpression() {
            return explanatoryExpression;
        }
        
        public void setTestExpression(AstNode testExpression) {
            this.testExpression = testExpression;
        }
        
        public void setExplanatoryExpression(AstNode explanatoryExpression) {
            this.explanatoryExpression = explanatoryExpression;
        }
    }
    
    public static class BinaryOperator extends AstNode {
        private Token op;
        private AstNode lhs;
        private AstNode rhs;
        private TalcType type;
        
        public BinaryOperator(SourceLocation location, Token op, AstNode lhs, AstNode rhs) {
            this.location = location;
            this.op = op;
            this.lhs = lhs;
            this.rhs = rhs;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitBinaryOperator(this);
        }
        
        public Token op() {
            return op;
        }
        
        public void setOp(Token op) {
            this.op = op;
        }
        
        public AstNode lhs() {
            return lhs;
        }
        
        public void setLhs(AstNode lhs) {
            this.lhs = lhs;
        }
        
        public AstNode rhs() {
            return rhs;
        }
        
        public void setRhs(AstNode rhs) {
            this.rhs = rhs;
        }
        
        public void setType(TalcType type) {
            this.type = type;
        }
        
        public TalcType type() {
            return type;
        }
        
        public String toString() {
            return op.name() + "(" + lhs + ", " + rhs + ")";
        }
    }
    
    public static class Block extends AstNode {
        public static final Block EMPTY_BLOCK = new Block(null, Collections.<AstNode>emptyList());
        
        private List<AstNode> statements;
        
        public Block(SourceLocation location, List<AstNode> statements) {
            this.location = location;
            this.statements = statements;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitBlock(this);
        }
        
        public List<AstNode> statements() {
            return statements;
        }
        
        public void setStatements(List<AstNode> statements) {
            this.statements = statements;
        }
        
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("{ ");
            for (AstNode statement : statements) {
                result.append(statement);
                result.append(" ; ");
            }
            result.append(" }");
            return result.toString();
        }
    }
    
    public static class BreakStatement extends AstNode {
        public BreakStatement(SourceLocation location) {
            this.location = location;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitBreakStatement(this);
        }
        
        public String toString() {
            return "break";
        }
    }
    
    public static class ClassDefinition extends AstNode {
        private String className;
        private List<AstNode.VariableDefinition> fields;
        private List<AstNode.FunctionDefinition> methods;
        
        private TalcType type;
        
        public ClassDefinition(SourceLocation location, String className, List<AstNode.VariableDefinition> fields, List<AstNode.FunctionDefinition> methods) {
            this.location = location;
            this.className = className;
            this.fields = fields;
            this.methods = methods;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitClassDefinition(this);
        }
        
        public String className() {
            return className;
        }
        
        public List<AstNode.VariableDefinition> fields() {
            return fields;
        }
        
        public void setFields(List<AstNode.VariableDefinition> fields) {
            this.fields = fields;
        }
        
        public List<AstNode.FunctionDefinition> methods() {
            return methods;
        }
        
        public void setMethods(List<AstNode.FunctionDefinition> methods) {
            this.methods = methods;
        }
        
        public void setType(TalcType newType) {
            type = newType;
        }
        
        public TalcType type() {
            return type;
        }
        
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("class ");
            result.append(className);
            result.append(" {");
            for (AstNode.VariableDefinition field : fields) {
                result.append(field);
                result.append(";");
            }
            for (AstNode.FunctionDefinition method : methods) {
                result.append(method);
            }
            result.append("}");
            return result.toString();
        }
    }
    
    public static class ContinueStatement extends AstNode {
        public ContinueStatement(SourceLocation location) {
            this.location = location;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitContinueStatement(this);
        }
        
        public String toString() {
            return "continue";
        }
    }
    
    public static class Constant extends AstNode {
        private Object constant;
        private TalcType type;
        
        public Constant(SourceLocation location, Object constant, TalcType type) {
            this.location = location;
            this.constant = constant;
            this.type = type;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitConstant(this);
        }
        
        public Object constant() {
            return constant;
        }
        
        public TalcType type() {
            return type;
        }
        
        public String toString() {
            if (constant instanceof String) {
                return "\"" + constant + "\"";
            }
            return (constant != null) ? constant.toString() : "null";
        }
    }
    
    public static class DoStatement extends AstNode {
        private AstNode expression;
        private AstNode body;
        
        public DoStatement(SourceLocation location, AstNode body, AstNode expression) {
            this.location = location;
            this.expression = expression;
            this.body = body;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitDoStatement(this);
        }
        
        public AstNode expression() {
            return expression;
        }
        
        public void setExpression(AstNode expression) {
            this.expression = expression;
        }
        
        public AstNode body() {
            return body;
        }
        
        public void setBody(AstNode body) {
            this.body = body;
        }
        
        public String toString() {
            return "do { " + body + "} while (" + expression + ")";
        }
    }
    
    public static class ForEachStatement extends AstNode {
        private List<AstNode.VariableDefinition> loopVariableDefinitions;
        private AstNode expression;
        private AstNode body;
        private TalcType expressionType;
        
        public ForEachStatement(SourceLocation location, List<AstNode.VariableDefinition> loopVariableDefinitions, AstNode expression, AstNode body) {
            this.location = location;
            this.loopVariableDefinitions = loopVariableDefinitions;
            this.expression = expression;
            this.body = body;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitForEachStatement(this);
        }
        
        public List<AstNode.VariableDefinition> loopVariableDefinitions() {
            return loopVariableDefinitions;
        }
        
        public AstNode expression() {
            return expression;
        }
        
        public void setExpression(AstNode expression) {
            this.expression = expression;
        }
        
        public AstNode body() {
            return body;
        }
        
        public void setBody(AstNode body) {
            this.body = body;
        }
        
        public void setExpressionType(TalcType type) {
            this.expressionType = type;
        }
        
        public TalcType expressionType() {
            return expressionType;
        }
        
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("for (");
            for (int i = 0; i < loopVariableDefinitions.size(); ++i) {
                if (i > 0) {
                    result.append(", ");
                }
                result.append(loopVariableDefinitions.get(i));
            }
            result.append("; ");
            result.append(expression);
            result.append(") ");
            result.append(body);
            return result.toString();
        }
    }
    
    public static class ForStatement extends AstNode {
        private VariableDefinition variableDefinition;
        private AstNode conditionExpression;
        private AstNode updateExpression;
        private AstNode body;
        
        public ForStatement(SourceLocation location, VariableDefinition variableDefinition, AstNode conditionExpression, AstNode updateExpression, AstNode body) {
            this.location = location;
            this.variableDefinition = variableDefinition;
            this.conditionExpression = conditionExpression;
            this.updateExpression = updateExpression;
            this.body = body;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitForStatement(this);
        }
        
        public VariableDefinition initializer() {
            return variableDefinition;
        }
        
        public void setInitializer(VariableDefinition variableDefinition) {
            this.variableDefinition = variableDefinition;
        }
        
        public AstNode conditionExpression() {
            return conditionExpression;
        }
        
        public void setConditionExpression(AstNode conditionExpression) {
            this.conditionExpression = conditionExpression;
        }
        
        public AstNode updateExpression() {
            return updateExpression;
        }
        
        public void setUpdateExpression(AstNode updateExpression) {
            this.updateExpression = updateExpression;
        }
        
        public AstNode body() {
            return body;
        }
        
        public void setBody(AstNode body) {
            this.body = body;
        }
        
        public String toString() {
            return "for (" + variableDefinition + "; " + conditionExpression + "; " + updateExpression + ") " + body;
        }
    }
    
    public static class FunctionCall extends AstNode {
        private String functionName;
        // For instance methods, null otherwise.
        private AstNode instance;
        // For class methods, null otherwise.
        private TalcTypeDescriptor classTypeDescriptor;
        private AstNode[] arguments;
        
        // During semantic analysis, this will be updated to point to the definition of the function we're going to call.
        private FunctionDefinition definition;
        
        private TalcType[] resolvedArgumentTypes;
        private TalcType resolvedReturnType;
        
        // A call of a global function.
        // FIXME: this may turn out to be a call to another method on "this", in which case we'll have to fix up "instance" later, when we realize.
        public FunctionCall(SourceLocation location, String functionName, AstNode[] arguments) {
            this.location = location;
            this.functionName = functionName;
            this.arguments = arguments;
            this.resolvedArgumentTypes = new TalcType[arguments.length];
        }
        
        // A call of an instance method.
        public FunctionCall(SourceLocation location, String functionName, AstNode instance, AstNode[] arguments) {
            this(location, functionName, arguments);
            this.instance = instance;
        }
        
        // A call of a constructor or class method.
        public FunctionCall(SourceLocation location, String functionName, TalcTypeDescriptor classTypeDescriptor, AstNode[] arguments) {
            this(location, functionName, arguments);
            this.classTypeDescriptor = classTypeDescriptor;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitFunctionCall(this);
        }
        
        public String functionName() {
            return functionName;
        }
        
        public AstNode instance() {
            return instance;
        }
        
        public void setInstance(AstNode instance) {
            this.instance = instance;
        }
        
        public TalcTypeDescriptor classTypeDescriptor() {
            return classTypeDescriptor;
        }
        
        public AstNode[] arguments() {
            return arguments;
        }
        
        public void setArguments(AstNode[] arguments) {
            this.arguments = arguments;
        }
        
        public void setDefinition(FunctionDefinition definition) {
            this.definition = definition;
        }
        
        public FunctionDefinition definition() {
            return definition;
        }
        
        public void setResolvedReturnType(TalcType resolvedReturnType) {
            this.resolvedReturnType = resolvedReturnType;
        }
        
        public TalcType resolvedReturnType() {
            return resolvedReturnType;
        }
        
        public void setResolvedArgumentType(int i, TalcType resolvedArgumentType) {
            this.resolvedArgumentTypes[i] = resolvedArgumentType;
        }
        
        public TalcType resolvedArgumentType(int i) {
            return resolvedArgumentTypes[i];
        }
        
        public String toString() {
            StringBuilder result = new StringBuilder();
            if (instance != null) {
                result.append(instance);
                result.append(".");
            }
            if (classTypeDescriptor != null) {
                result.append(classTypeDescriptor);
                result.append(".");
            }
            result.append(functionName);
            result.append("(");
            for (int i = 0; i < arguments.length; ++i) {
                if (i > 0) { result.append(", "); }
                result.append(arguments[i].toString());
            }
            result.append(")");
            return result.toString();
        }
    }
    
    public static class FunctionDefinition extends AstNode {
        private String functionName;
        private List<String> formalParameterNames;
        private List<TalcType> formalParameterTypes;
        private TalcType returnType;
        private AstNode body;
        
        // "extern" functions have these two fields set.
        private String externLanguageName;
        private String externFunctionDescriptor;
        
        // Null, or the type of the class this is a method on.
        private TalcType containingType;
        private boolean isClassMethod = false;
        private boolean isConstructor = false;
        
        // Until the symbol table is built, these are all we have, type-wise.
        private List<TalcTypeDescriptor> formalParameterTypeDescriptors;
        private TalcTypeDescriptor returnTypeDescriptor;
        
        private List<AstNode.VariableDefinition> formalParameters;
        
        public FunctionDefinition(SourceLocation location, String functionName, List<String> formalParameterNames, List<TalcType> formalParameterTypes, TalcType returnType, AstNode body) {
            this.location = location;
            this.functionName = functionName;
            this.formalParameterNames = formalParameterNames;
            this.formalParameterTypes = formalParameterTypes;
            this.returnType = returnType;
            this.body = body;
        }
        
        public FunctionDefinition(SourceLocation location, String functionName, List<String> formalParameterNames, List<TalcTypeDescriptor> formalParameterTypeDescriptors, TalcTypeDescriptor returnTypeDescriptor, AstNode body) {
            this.location = location;
            this.functionName = functionName;
            this.formalParameterNames = formalParameterNames;
            this.formalParameterTypeDescriptors = formalParameterTypeDescriptors;
            this.returnTypeDescriptor = returnTypeDescriptor;
            this.body = body;
        }
        
        public void fixUpTypes(TalcType containingType) {
            this.containingType = containingType;
            if (formalParameterTypes == null) {
                formalParameterTypes = new ArrayList<TalcType>();
                for (int i = 0; i < formalParameterTypeDescriptors.size(); ++i) {
                    TalcTypeDescriptor typeDescriptor = formalParameterTypeDescriptors.get(i);
                    TalcType type = typeDescriptor.type();
                    if (type == null) {
                        throw new TalcError(this, "declared type of argument \"" + formalParameterNames.get(i) + "\" to function \""+ functionName + "\", " + typeDescriptor + ", doesn't exist");
                    }
                    formalParameterTypes.add(type);
                }
            }
            if (returnType == null) {
                returnType = returnTypeDescriptor.type();
                if (returnType == null) {
                    throw new TalcError(this, "declared return type of function \"" + functionName + "\", " + returnTypeDescriptor + ", doesn't exist");
                }
            }
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitFunctionDefinition(this);
        }
        
        // Used by the "print" and "puts" built-in functions to avoid type checking.
        public boolean isVarArgs() {
            return (formalParameterNames == null && formalParameterTypes == null);
        }
        
        public void markAsClassMethod() {
            isClassMethod = true;
        }
        
        public boolean isClassMethod() {
            return isClassMethod;
        }
        
        public void markAsConstructor() {
            isConstructor = true;
        }
        
        public boolean isConstructor() {
            return isConstructor;
        }
        
        public String functionName() {
            return functionName;
        }
        
        public void setFormalParameters(List<AstNode.VariableDefinition> formalParameters) {
            this.formalParameters = formalParameters;
        }
        
        public List<AstNode.VariableDefinition> formalParameters() {
            return formalParameters;
        }
        
        public List<String> formalParameterNames() {
            return formalParameterNames;
        }
        
        public List<TalcType> formalParameterTypes() {
            return formalParameterTypes;
        }
        
        public List<TalcTypeDescriptor> formalParameterTypeDescriptors() {
            return formalParameterTypeDescriptors;
        }
        
        public TalcType returnType() {
            return returnType;
        }
        
        public TalcType containingType() {
            return containingType;
        }
        
        public AstNode body() {
            return body;
        }
        
        public void setBody(AstNode body) {
            this.body = body;
        }
        
        public void setExtern(String externLanguageName, String externFunctionDescriptor) {
            this.externLanguageName = externLanguageName;
            this.externFunctionDescriptor = externFunctionDescriptor;
        }
        
        public String externLanguageName() {
            return externLanguageName;
        }
        
        public String externFunctionDescriptor() {
            return externFunctionDescriptor;
        }
        
        public boolean isExtern() {
            return (externLanguageName != null);
        }
        
        public String signature() {
            StringBuilder result = new StringBuilder();
            result.append(functionName);
            result.append("(");
            if (isVarArgs()) {
                result.append("...");
            } else {
                for (int i = 0; i < formalParameterNames.size(); ++i) {
                    if (i > 0) { result.append(", "); }
                    result.append(formalParameterNames.get(i));
                    result.append(": ");
                    result.append((formalParameterTypes != null) ? formalParameterTypes.get(i) : formalParameterTypeDescriptors.get(i));
                }
            }
            result.append(") : ");
            result.append((returnType != null ? returnType.toString() : returnTypeDescriptor.toString()));
            return result.toString();
        }
        
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("function ");
            result.append(signature());
            result.append(" ");
            result.append(body.toString());
            return result.toString();
        }
    }
    
    public static class IfStatement extends AstNode {
        private List<AstNode> expressions;
        private List<AstNode> bodies;
        private AstNode elseBlock;
        
        public IfStatement(SourceLocation location, List<AstNode> expressions, List<AstNode> bodies, AstNode elseBlock) {
            this.location = location;
            this.expressions = expressions;
            this.bodies = bodies;
            this.elseBlock = elseBlock;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitIfStatement(this);
        }
        
        public List<AstNode> expressions() {
            return expressions;
        }
        
        public void setExpressions(List<AstNode> expressions) {
            this.expressions = expressions;
        }
        
        public List<AstNode> bodies() {
            return bodies;
        }
        
        public void setBodies(List<AstNode> bodies) {
            this.bodies = bodies;
        }
        
        public AstNode elseBlock() {
            return elseBlock;
        }
        
        public void setElseBlock(AstNode elseBlock) {
            this.elseBlock = elseBlock;
        }
        
        public String toString() {
            StringBuilder result = new StringBuilder();
            final int expressionCount = expressions.size();
            for (int i = 0; i < expressionCount; ++i) {
                if (i > 1) {
                    result.append(" else ");
                }
                result.append("if (" + expressions.get(i) + ") " + bodies.get(i));
            }
            if (elseBlock != Block.EMPTY_BLOCK) {
                result.append(" else " + elseBlock);
            }
            return result.toString();
        }
    }
    
    public static class ListLiteral extends AstNode {
        private List<AstNode> expressions;
        
        public ListLiteral(SourceLocation location, List<AstNode> expressions) {
            this.location = location;
            this.expressions = expressions;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitListLiteral(this);
        }
        
        public List<AstNode> expressions() {
            return expressions;
        }
        
        public void setExpressions(List<AstNode> expressions) {
            this.expressions = expressions;
        }
        
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("[");
            for (int i = 0; i < expressions.size(); ++i) {
                if (i > 0) {
                    result.append(", ");
                }
                AstNode expression = expressions.get(i);
                result.append(expression);
            }
            result.append("]");
            return result.toString();
        }
    }
    
    public static class MapLiteral extends AstNode {
        private List<AstNode> expressions;
        
        // FIXME: we should probably separate the keyExpressions from the valueExpressions.
        public MapLiteral(SourceLocation location, List<AstNode> expressions) {
            this.location = location;
            this.expressions = expressions;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitMapLiteral(this);
        }
        
        public List<AstNode> expressions() {
            return expressions;
        }
        
        public void setExpressions(List<AstNode> expressions) {
            this.expressions = expressions;
        }
        
        public String toString() {
            final StringBuilder result = new StringBuilder();
            result.append("[");
            for (int i = 0; i < expressions.size(); ++i) {
                if (i > 0) {
                    result.append(", ");
                }
                final AstNode key = expressions.get(i++);
                final AstNode value = expressions.get(i);
                result.append(key);
                result.append(":");
                result.append(value);
            }
            result.append("]");
            return result.toString();
        }
    }
    
    public static class ReturnStatement extends AstNode {
        private AstNode expression;
        private TalcType returnType;
        
        public ReturnStatement(SourceLocation location, AstNode expression) {
            this.location = location;
            this.expression = expression;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitReturnStatement(this);
        }
        
        public AstNode expression() {
            return expression;
        }
        
        public void setExpression(AstNode expression) {
            this.expression = expression;
        }
        
        public void setReturnType(TalcType returnType) {
            this.returnType = returnType;
        }
        
        public TalcType returnType() {
            return returnType;
        }
        
        public String toString() {
            return "return " + expression;
        }
    }
    
    public static class VariableDefinition extends AstNode {
        private String identifier;
        private TalcType type;
        private AstNode initializer;
        private boolean isFinal;
        private boolean isField;
        
        // Until the symbol table is built, this is all we have, type-wise.
        private TalcTypeDescriptor typeDescriptor;
        
        // Used by the code generator to remember where it put this variable.
        private VariableAccessor accessor;
        
        public VariableDefinition(SourceLocation location, String identifier, TalcTypeDescriptor typeDescriptor, AstNode initializer, boolean isFinal) {
            this.location = location;
            this.identifier = identifier;
            this.typeDescriptor = typeDescriptor;
            this.initializer = initializer;
            this.isFinal = isFinal;
            this.isField = false;
        }
        
        public VariableDefinition(SourceLocation location, String identifier, TalcType type, AstNode initializer, boolean isFinal) {
            this.location = location;
            this.identifier = identifier;
            this.type = type;
            this.initializer = initializer;
            this.isFinal = isFinal;
            this.isField = false;
        }
        
        public void fixUpType(TalcType initializerType) {
            if (type == null) {
                if (typeDescriptor != null) {
                    // The declaration had an explicit type, so use it.
                    type = typeDescriptor.type();
                    if (type == null) {
                        throw new TalcError(this, "declared type of variable \"" + identifier + "\", " + typeDescriptor + ", doesn't exist");
                    }
                } else {
                    // The declaration was implicitly typed ("<name> := <value>"), so use the initializer type.
                    type = initializerType;
                    if (Talc.debugging('i')) {
                        System.err.println(location() + "note: inferred type of \"" + identifier + "\" from initializer as " + type);
                    }
                }
            }
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitVariableDefinition(this);
        }
        
        public String identifier() {
            return identifier;
        }
        
        public boolean isFinal() {
            return isFinal;
        }
        
        public boolean isField() {
            return isField;
        }
        
        public void markAsField() {
            this.isField = true;
        }
        
        public TalcType type() {
            return type;
        }
        
        public AstNode initializer() {
            return initializer;
        }
        
        public void setInitializer(AstNode initializer) {
            this.initializer = initializer;
        }
        
        public void setAccessor(VariableAccessor accessor) {
            this.accessor = accessor;
        }
        
        public VariableAccessor accessor() {
            return accessor;
        }
        
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(identifier);
            result.append(": ");
            if (type != null) {
                result.append(type);
            } else if (typeDescriptor != null) {
                result.append(typeDescriptor);
            } else {
                result.append("<to-be-inferred>");
            }
            result.append(" = ");
            result.append(initializer);
            return result.toString();
        }
    }
    
    public static class VariableName extends AstNode {
        private String identifier;
        private boolean isFieldAccess;
        
        // Points to this variable's definition. Set up by SymbolTable and used by most later phases.
        private AstNode.VariableDefinition definition;
        
        public VariableName(SourceLocation location, String identifier) {
            this.location = location;
            this.identifier = identifier;
            this.isFieldAccess = false;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitVariableName(this);
        }
        
        public String identifier() {
            return identifier;
        }
        
        public boolean isFieldAccess() {
            return isFieldAccess;
        }
        
        public void markAsFieldAccess() {
            this.isFieldAccess = true;
        }
        
        public void setDefinition(AstNode.VariableDefinition definition) {
            this.definition = definition;
        }
        
        public AstNode.VariableDefinition definition() {
            return definition;
        }
        
        public String toString() {
            return identifier;
        }
    }
    
    public static class WhileStatement extends AstNode {
        private AstNode expression;
        private AstNode body;
        
        public WhileStatement(SourceLocation location, AstNode expression, AstNode body) {
            this.location = location;
            this.expression = expression;
            this.body = body;
        }
        
        public <ResultT> ResultT accept(AstVisitor<ResultT> visitor) {
            return visitor.visitWhileStatement(this);
        }
        
        public AstNode expression() {
            return expression;
        }
        
        public void setExpression(AstNode expression) {
            this.expression = expression;
        }
        
        public AstNode body() {
            return body;
        }
        
        public void setBody(AstNode body) {
            this.body = body;
        }
        
        public String toString() {
            return "while (" + expression + ") { " + body + "}";
        }
    }
}
