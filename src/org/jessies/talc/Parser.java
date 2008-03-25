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

public class Parser {
    private Lexer lexer;
    private boolean DEBUG_PARSER = Talc.debugging('p');
    
    public Parser(Lexer lexer) {
        this.lexer = lexer;
    }
    
    public List<AstNode> parse() {
        ArrayList<AstNode> result = new ArrayList<AstNode>();
        while (lexer.token() != Token.END_OF_INPUT) {
            AstNode statement = parseStatement();
            if (DEBUG_PARSER) { System.out.println("parsed " + statement); }
            // Don't bother recording empty statements.
            if (statement != null) {
                result.add(statement);
            }
        }
        return result;
    }
    
    private AstNode parseStatement() {
        if (DEBUG_PARSER) { System.out.println("parseStatement()"); }
        
        switch (lexer.token()) {
            case BREAK: return parseBreakStatement();
            case CLASS: return parseClassDefinition();
            case CONTINUE: return parseContinueStatement();
            case DO: return parseDoStatement();
            case IF: return parseIfStatement();
            case FOR: return parseForStatement();
            case FUNCTION: return parseFunctionDefinition();
            case RETURN: return parseReturnStatement();
            case WHILE: return parseWhileStatement();
        case SEMICOLON:
            throw new TalcError(lexer, "empty statement");
        default:
            AstNode expression = parseExpression();
            expect(Token.SEMICOLON);
            return expression;
        }
    }
    
    private String whatWeGot() {
        StringBuilder result = new StringBuilder();
        result.append(lexer.token());
        switch (lexer.token()) {
        case IDENTIFIER:
            result.append(" \"");
            result.append(lexer.identifier());
            result.append("\"");
            break;
        case INT_LITERAL:
        case REAL_LITERAL:
            result.append(" ");
            result.append(lexer.numericLiteral());
        }
        return result.toString();
    }
    
    private void expect(Token what) {
        if (lexer.token() != what) {
            throw new TalcError(lexer, "expected " + what + ", got " + whatWeGot() + " instead");
        }
        lexer.nextToken();
    }
    
    private void checkForUnreachableStatements() {
        if (lexer.token() != Token.CLOSE_BRACE) {
            throw new TalcError(lexer, "unreachable statement");
        }
    }
    
    private String expectIdentifier(String description) {
        if (lexer.token() != Token.IDENTIFIER) {
            throw new TalcError(lexer, "expected " + description + ", got " + whatWeGot() + " instead");
        }
        String result = lexer.identifier();
        lexer.nextToken();
        return result;
    }
    
    private AstNode parseBreakStatement() {
        if (DEBUG_PARSER) { System.out.println("parseBreakStatement()"); }
        
        SourceLocation location = lexer.getLocation();
        expect(Token.BREAK);
        expect(Token.SEMICOLON);
        checkForUnreachableStatements();
        return new AstNode.BreakStatement(location);
    }
    
    private AstNode parseContinueStatement() {
        if (DEBUG_PARSER) { System.out.println("parseContinueStatement()"); }
        
        SourceLocation location = lexer.getLocation();
        expect(Token.CONTINUE);
        expect(Token.SEMICOLON);
        checkForUnreachableStatements();
        return new AstNode.ContinueStatement(location);
    }
    
    private AstNode parseClassDefinition() {
        if (DEBUG_PARSER) { System.out.println("parseClassDefinition()"); }
        
        SourceLocation location = lexer.getLocation();
        expect(Token.CLASS);
        String className = expectIdentifier("class name");
        // FIXME: support "extends", "implements". for now, everything extends object and implements nothing.
        expect(Token.OPEN_BRACE);
        ArrayList<AstNode.VariableDefinition> fields = new ArrayList<AstNode.VariableDefinition>();
        ArrayList<AstNode.FunctionDefinition> methods = new ArrayList<AstNode.FunctionDefinition>();
        while (lexer.token() == Token.IDENTIFIER || lexer.token() == Token.FUNCTION) {
            if (lexer.token() == Token.FUNCTION) {
                methods.add(parseFunctionDefinition());
            } else {
                SourceLocation location2 = lexer.getLocation();
                String variableName = expectIdentifier("field name");
                fields.add(parseVariableDefinition(location2, variableName));
                expect(Token.SEMICOLON);
            }
        }
        expect(Token.CLOSE_BRACE);
        return new AstNode.ClassDefinition(location, className, fields, methods);
    }
    
    private AstNode.FunctionDefinition parseFunctionDefinition() {
        if (DEBUG_PARSER) { System.out.println("parseFunctionDefinition()"); }
        
        SourceLocation location = lexer.getLocation();
        expect(Token.FUNCTION);
        String functionName = expectIdentifier("function name");
        ArrayList<String> formalParameterNames = new ArrayList<String>();
        ArrayList<TalcTypeDescriptor> formalParameterTypeDescriptors = new ArrayList<TalcTypeDescriptor>();
        parseFormalParameters(Token.OPEN_PARENTHESIS, Token.CLOSE_PARENTHESIS, formalParameterNames, formalParameterTypeDescriptors);
        // FIXME: allow "function f(x:int) { ... }" as shorthand for a void function?
        expect(Token.COLON);
        TalcTypeDescriptor returnTypeDescriptor = parseType();
        AstNode.Block body = parseBlock();
        return new AstNode.FunctionDefinition(location, functionName, Collections.unmodifiableList(formalParameterNames), Collections.unmodifiableList(formalParameterTypeDescriptors), returnTypeDescriptor, body);
    }
    
    private void parseFormalParameters(Token startToken, Token endToken, ArrayList<String> names, ArrayList<TalcTypeDescriptor> typeDescriptors) {
        if (DEBUG_PARSER) { System.out.println("parseFormalParameters()"); }
        
        expect(startToken);
        while (lexer.token() != Token.END_OF_INPUT && lexer.token() != endToken) {
            if (lexer.token() == Token.IDENTIFIER) {
                names.add(lexer.identifier());
                lexer.nextToken();
                expect(Token.COLON);
                typeDescriptors.add(parseType());
                if (lexer.token() == Token.COMMA) {
                    lexer.nextToken();
                } else if (lexer.token() != endToken) {
                    throw new TalcError(lexer, "expected \",\" or " + endToken + ", got " + whatWeGot() + " instead");
                }
            }
        }
        expect(endToken);
    }
    
    private TalcTypeDescriptor parseType() {
        if (DEBUG_PARSER) { System.out.println("parseType()"); }
        // type system progress:
        // present:
        //  TYPE = PRIMITIVE | list < TYPE > | map < TYPE , TYPE > | user_defined_class_name
        // future:
        //  TYPE = PRIMITIVE | list < TYPE > | map < TYPE , TYPE > | user_defined_class_name | user_defined_class_name < TYPE... >
        switch (lexer.token()) {
        case VOID:
            expect(Token.VOID);
            return new TalcTypeDescriptor(TalcType.VOID);
        case IDENTIFIER:
            String identifier = lexer.identifier();
            lexer.nextToken();
            if (lexer.token() != Token.LT) {
                // Simple type.
                return new TalcTypeDescriptor(identifier);
            }
            // Parameterized type.
            expect(Token.LT);
            TalcTypeDescriptor firstTypeDescriptor = parseType();
            TalcTypeDescriptor secondTypeDescriptor = null;
            if (lexer.token() == Token.COMMA) {
                expect(Token.COMMA);
                secondTypeDescriptor = parseType();
            }
            expect(Token.GT);
            return new TalcTypeDescriptor(identifier, firstTypeDescriptor, secondTypeDescriptor);
        default:
            throw new TalcError(lexer, "expected type, got " + whatWeGot() + " instead");
        }
    }
    
    // Parses "( EXPR... )" for function calls or "[ EXPR... ]" for list literals.
    private List<AstNode> parseExpressionList(Token startToken, Token endToken, String why) {
        if (DEBUG_PARSER) { System.out.println("parseExpressionList()"); }
        
        expect(startToken);
        ArrayList<AstNode> list = new ArrayList<AstNode>();
        while (lexer.token() != endToken) {
            AstNode expression = parseExpression();
            list.add(expression);
            if (lexer.token() == Token.COMMA) {
                lexer.nextToken();
                continue;
            } else if (lexer.token() == endToken) {
                break;
            } else {
                throw new TalcError(lexer, "expected \",\" or " + endToken + " next in " + why + ", got " + whatWeGot() + " instead");
            }
        }
        expect(endToken);
        return list;
    }
    
    private AstNode parseIfStatement() {
        if (DEBUG_PARSER) { System.out.println("parseIfStatement()"); }
        
        ArrayList<AstNode> expressions = new ArrayList<AstNode>();
        ArrayList<AstNode> blocks = new ArrayList<AstNode>();
        AstNode elseBlock = AstNode.Block.EMPTY_BLOCK;
        
        SourceLocation location = lexer.getLocation();
        expect(Token.IF);
        expect(Token.OPEN_PARENTHESIS);
        expressions.add(parseExpression());
        expect(Token.CLOSE_PARENTHESIS);
        blocks.add(parseBlock());
        
        while (lexer.token() == Token.ELSE) {
            expect(Token.ELSE);
            if (lexer.token() == Token.IF) {
                expect(Token.IF);
                expect(Token.OPEN_PARENTHESIS);
                expressions.add(parseExpression());
                expect(Token.CLOSE_PARENTHESIS);
                blocks.add(parseBlock());
            } else {
                elseBlock = parseBlock();
                break;
            }
        }
        return new AstNode.IfStatement(location, expressions, blocks, elseBlock);
    }
    
    private AstNode parseReturnStatement() {
        if (DEBUG_PARSER) { System.out.println("parseReturnStatement()"); }
        
        SourceLocation location = lexer.getLocation();
        expect(Token.RETURN);
        AstNode expression = (lexer.token() != Token.SEMICOLON) ? parseExpression() : null;
        expect(Token.SEMICOLON);
        checkForUnreachableStatements();
        return new AstNode.ReturnStatement(location, expression);
    }
    
    private AstNode parseForStatement() {
        if (DEBUG_PARSER) { System.out.println("parseForStatement()"); }
        
        SourceLocation location = lexer.getLocation();
        expect(Token.FOR);
        expect(Token.OPEN_PARENTHESIS);
        
        // Optional variable definition ("<name> : [<type>] = <initializer> ;") or for-each list ("<name> [, <name>] ;").
        AstNode.VariableDefinition variableDefinition = null;
        if (lexer.token() == Token.IDENTIFIER) {
            SourceLocation location2 = lexer.getLocation();
            String variableName = lexer.identifier();
            expect(Token.IDENTIFIER);
            if (lexer.token() == Token.COMMA || lexer.token() == Token.IN) {
                return parseForEachStatement(location, variableName, location2);
            }
            variableDefinition = parseVariableDefinition(location2, variableName);
        }
        expect(Token.SEMICOLON);
        
        // Optional condition expression (taken to be "true" if missing).
        AstNode conditionExpression = (lexer.token() == Token.SEMICOLON) ? new AstNode.Constant(location, BooleanValue.TRUE, TalcType.BOOL) : parseExpression();
        expect(Token.SEMICOLON);
        
        // Optional update expression (for convenience also taken to be "true" if missing).
        AstNode updateExpression = (lexer.token() == Token.CLOSE_PARENTHESIS) ? new AstNode.Constant(location, BooleanValue.TRUE, TalcType.BOOL) : parseExpression();
        expect(Token.CLOSE_PARENTHESIS);
        AstNode.Block body = parseBlock();
        return new AstNode.ForStatement(location, variableDefinition, conditionExpression, updateExpression, body);
    }
    
    private AstNode parseForEachStatement(SourceLocation location, String variableName, SourceLocation location2) {
        if (DEBUG_PARSER) { System.out.println("parseForEachStatement()"); }
        
        // "for ( <name> [, <name>] in <expression> ) <block>"
        // When we're called, we've already swallowed as far as the optional COMMA or SEMICOLON.
        ArrayList<AstNode.VariableDefinition> loopVariableDefinitions = new ArrayList<AstNode.VariableDefinition>();
        
        // First, obligatory loop variable.
        loopVariableDefinitions.add(new AstNode.VariableDefinition(location, variableName, (TalcType) null, new AstNode.Constant(location, null, TalcType.NULL), false));
        
        // Second, optional loop variable.
        if (lexer.token() == Token.COMMA) {
            expect(Token.COMMA);
            String variableName2 = expectIdentifier("second loop variable name in for-each loop");
            loopVariableDefinitions.add(new AstNode.VariableDefinition(location, variableName2, (TalcType) null, new AstNode.Constant(location, null, TalcType.NULL), false));
        }
        
        expect(Token.IN);
        AstNode expression = parseExpression();
        expect(Token.CLOSE_PARENTHESIS);
        AstNode.Block body = parseBlock();
        return new AstNode.ForEachStatement(location, loopVariableDefinitions, expression, body);
    }
    
    private AstNode.VariableDefinition parseVariableDefinition(SourceLocation location, String identifier) {
        if (DEBUG_PARSER) { System.out.println("parseVariableDefinition()"); }
        
        // Variable definition "<identifier> : [final] <type> = <initializer>".
        // Lexer is already at the COLON.
        expect(Token.COLON);
        boolean isFinal = false;
        if (lexer.token() == Token.FINAL) {
            expect(Token.FINAL);
            isFinal = true;
        }
        TalcTypeDescriptor typeDescriptor = (lexer.token() != Token.ASSIGN) ? parseType() : null;
        expect(Token.ASSIGN);
        AstNode initializer = parseExpression();
        return new AstNode.VariableDefinition(location, identifier, typeDescriptor, initializer, isFinal);
    }
    
    private AstNode parseDoStatement() {
        if (DEBUG_PARSER) { System.out.println("parseDoStatement()"); }
        
        SourceLocation location = lexer.getLocation();
        expect(Token.DO);
        AstNode.Block body = parseBlock();
        expect(Token.WHILE);
        expect(Token.OPEN_PARENTHESIS);
        AstNode expression = parseExpression();
        expect(Token.CLOSE_PARENTHESIS);
        expect(Token.SEMICOLON);
        return new AstNode.DoStatement(location, body, expression);
    }
    
    private AstNode parseWhileStatement() {
        if (DEBUG_PARSER) { System.out.println("parseWhileStatement()"); }
        
        SourceLocation location = lexer.getLocation();
        expect(Token.WHILE);
        expect(Token.OPEN_PARENTHESIS);
        AstNode expression = parseExpression();
        expect(Token.CLOSE_PARENTHESIS);
        AstNode.Block body = parseBlock();
        return new AstNode.WhileStatement(location, expression, body);
    }
    
    private AstNode.Block parseBlock() {
        if (DEBUG_PARSER) { System.out.println("parseBlock()"); }
        SourceLocation location = lexer.getLocation();
        expect(Token.OPEN_BRACE);
        ArrayList<AstNode> body = new ArrayList<AstNode>();
        while (lexer.token() != Token.CLOSE_BRACE) {
            body.add(parseStatement());
        }
        expect(Token.CLOSE_BRACE);
        return new AstNode.Block(location, body);
    }
    
    // Here's our operator precedence.
    // It's big and complicated, mainly to "be like C", which may or may not be a good idea.
    // Note that some levels are reserved for operators we don't actually have yet.
    // Note also that, thanks to '-' being both subtraction and unary minus, you have to say whether Token is supposed to be unary or binary to get its precedence.
    
    // 0: = += -= *= /= %= **= &= |= ^= <<= >>=
    // 1: ?:
    // 2: ||
    // 3: &&
    // 4: |
    // 5: XOR
    // 6: &
    // 7: == !=
    // 8: > >= < <=
    // 9: << >>
    // 10: + -
    // 11: * / %
    // 12: UNARY_MINUS ! ~ ++ --
    // 13: **
    
    private int unaryOperatorPrecedence(Token op) {
        switch (op) {
        case B_NOT:
        case FACTORIAL:
        case L_NOT:
        case NEG:
        case POST_DECREMENT:
        case POST_INCREMENT:
        case PRE_DECREMENT:
        case PRE_INCREMENT:
            return 12;
        default:
            throw new TalcError(lexer, "token " + op + " is not a unary operator");
        }
    }
    
    private static boolean isUnaryOperator(Token token) {
        switch (token) {
        case B_NOT:
        case L_NOT:
        case FACTORIAL:
        case NEG:
        case MINUS:
        case MINUS_MINUS:
        case PLING:
        case PLUS_PLUS:
        case POST_DECREMENT:
        case POST_INCREMENT:
        case PRE_DECREMENT:
        case PRE_INCREMENT:
            return true;
        default:
            return false;
        }
    }
    
    private int binaryOperatorPrecedence(Token op) {
        switch (op) {
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
            return 0;
        case L_OR:
            return 2;
        case L_AND:
            return 3;
        case B_OR:
            return 4;
        case B_XOR:
            return 5;
        case B_AND:
            return 6;
        case EQ:
        case NE:
            return 7;
        case GT:
        case GE:
        case LT:
        case LE:
            return 8;
        case SHL:
        case SHR:
            return 9;
        case PLUS:
        case SUB:
        case MINUS:
            return 10;
        case MUL:
        case DIV:
        case MOD:
            return 11;
        case POW:
            return 13;
        default:
            throw new TalcError(lexer, "token " + op + " is not a binary operator");
        }
    }
    
    private static boolean isBinaryOperator(Token token) {
        switch (token) {
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
        case L_OR:
        case L_AND:
        case B_OR:
        case B_XOR:
        case B_AND:
        case EQ:
        case NE:
        case GT:
        case GE:
        case LT:
        case LE:
        case SHL:
        case SHR:
        case PLUS:
        case MINUS:
        case SUB:
        case MUL:
        case DIV:
        case MOD:
        case POW:
            return true;
        default:
            return false;
        }
    }
    
    // Associativity is simple.
    // Everything's left-associative apart from all forms of assignment and the ** binary operator.
    private static boolean isRightAssociative(Token token) {
        switch (token) {
        case POW:
        case PLUS_ASSIGN:
        case SUB_ASSIGN:
        case MUL_ASSIGN:
        case POW_ASSIGN:
        case DIV_ASSIGN:
        case MOD_ASSIGN:
        case SHL_ASSIGN:
        case SHR_ASSIGN:
        case ASSIGN:
        case AND_ASSIGN:
        case OR_ASSIGN:
        case XOR_ASSIGN:
            return true;
        default:
            return false;
        }
    }
    
    private static boolean isPrefixOperator(Token t) {
        switch (t) {
        case MINUS:
        case MINUS_MINUS:
        case PLING:
        case PLUS_PLUS:
            return true;
        default:
            return false;
        }
    }
    
    private static boolean isPostfixOperator(Token t) {
        switch (t) {
        case MINUS_MINUS:
        case PLING:
        case PLUS_PLUS:
            return true;
        default:
            return false;
        }
    }
    
    private Token toPrefixOperator(Token t) {
        switch (t) {
            case MINUS: return Token.NEG;
            case MINUS_MINUS: return Token.PRE_DECREMENT;
            case PLING: return Token.L_NOT;
            case PLUS_PLUS: return Token.PRE_INCREMENT;
        default:
            throw new TalcError(lexer, "token " + t + " is not a prefix operator");
        }
    }
    
    private Token toPostfixOperator(Token t) {
        switch (t) {
            case MINUS_MINUS: return Token.POST_DECREMENT;
            case PLING: return Token.FACTORIAL;
            case PLUS_PLUS: return Token.POST_INCREMENT;
        default:
            throw new TalcError(lexer, "token " + t + " is not a prefix operator");
        }
    }
    
    private static Token toBinaryOperator(Token t) {
        switch (t) {
            case MINUS: return Token.SUB;
        default:
            return t;
        }
    }
    
    private AstNode parseExpression() {
        return parseExpression(0);
    }
    
    // Implements the "precedence climbing" algorithm from http://www.engr.mun.ca/~theo/Misc/exp_parsing.htm.
    // The "classic" algorithm would be fine if we were using a tool to generate the parser, but we're not.
    // Dijkstra's "shunting yard" algorithm hasn't been necessary yet.
    private AstNode parseExpression(int minPrecedence) {
        if (DEBUG_PARSER) { System.out.println("parseExpression(" + minPrecedence + ")"); }
        AstNode t = parsePrimary();
        Token op;
        while (isBinaryOperator(op = lexer.token()) && binaryOperatorPrecedence(op) >= minPrecedence) {
            SourceLocation location = lexer.getLocation();
            lexer.nextToken();
            AstNode rhs = parseExpression(isRightAssociative(op) ? binaryOperatorPrecedence(op) : binaryOperatorPrecedence(op) + 1);
            t = new AstNode.BinaryOperator(location, toBinaryOperator(op), t, rhs);
        }
        return t;
    }
    
    private AstNode parsePrimary() {
        if (DEBUG_PARSER) { System.out.println("parsePrimary()"); }
        
        SourceLocation location = lexer.getLocation();
        Token op = lexer.token();
        if (isUnaryOperator(op) && isPrefixOperator(op)) {
            SourceLocation unaryPrefixLocation = lexer.getLocation();
            lexer.nextToken();
            op = toPrefixOperator(op);
            return new AstNode.BinaryOperator(unaryPrefixLocation, op, parseExpression(unaryOperatorPrecedence(op)), null);
        }
        
        AstNode primary;
        if (op == Token.OPEN_PARENTHESIS) {
            lexer.nextToken();
            primary = parseExpression();
            expect(Token.CLOSE_PARENTHESIS);
        } else if (op == Token.INT_LITERAL || op == Token.REAL_LITERAL) {
            AstNode constant = new AstNode.Constant(location, lexer.numericLiteral(), (op == Token.INT_LITERAL) ? TalcType.INT : TalcType.REAL);
            lexer.nextToken();
            primary = constant;
        } else if (op == Token.TRUE || op == Token.FALSE) {
            AstNode constant = new AstNode.Constant(location, op == Token.TRUE ? BooleanValue.TRUE : BooleanValue.FALSE, TalcType.BOOL);
            lexer.nextToken();
            primary = constant;
        } else if (op == Token.NULL) {
            AstNode constant = new AstNode.Constant(location, null, TalcType.NULL);
            lexer.nextToken();
            primary = constant;
        } else if (op == Token.STRING_LITERAL) {
            AstNode constant = new AstNode.Constant(location, lexer.identifier(), TalcType.STRING);
            lexer.nextToken();
            primary = constant;
        } else if (op == Token.OPEN_BRACKET) {
            // List literal "[ <expression>... ]".
            primary = new AstNode.ListLiteral(location, parseExpressionList(Token.OPEN_BRACKET, Token.CLOSE_BRACKET, "list literal"));
        } else if (op == Token.NEW) {
            // New expression "new <type-name> ( <expression> ... )".
            expect(Token.NEW);
            // FIXME: would be nice to allow "word:list<string> = new list();".
            TalcTypeDescriptor typeDescriptor = parseType();
            List<AstNode> arguments = parseExpressionList(Token.OPEN_PARENTHESIS, Token.CLOSE_PARENTHESIS, "arguments to call of \"" + typeDescriptor + "\" constructor");
            primary = new AstNode.FunctionCall(location, typeDescriptor.rawName(), typeDescriptor, arguments.toArray(new AstNode[arguments.size()]));
        } else if (op == Token.IDENTIFIER) {
            String identifier = lexer.identifier();
            lexer.nextToken();
            if (lexer.token() == Token.COLON) {
                primary = parseVariableDefinition(location, identifier);
            } else if (lexer.token() == Token.OPEN_PARENTHESIS) {
                // Function call "<identifier> ( <expression>... )".
                List<AstNode> arguments = parseExpressionList(Token.OPEN_PARENTHESIS, Token.CLOSE_PARENTHESIS, "arguments to call of \"" + identifier + "\"");
                primary = new AstNode.FunctionCall(location, identifier, arguments.toArray(new AstNode[arguments.size()]));
            } else {
                primary = new AstNode.VariableName(location, identifier);
            }
        } else {
            throw new TalcError(lexer, "didn't expect to see a " + op + " in factor");
        }
        
        while (isUnaryOperator(op = lexer.token()) && isPostfixOperator(op)) {
            primary = new AstNode.BinaryOperator(location, toPostfixOperator(op), primary, null);
            lexer.nextToken();
        }
        
        while (lexer.token() == Token.DOT || lexer.token() == Token.OPEN_BRACKET) {
            if (lexer.token() == Token.DOT) {
                // Method call ". <identifier> ( <expression> ... )".
                expect(Token.DOT);
                if (lexer.token() != Token.IDENTIFIER) {
                    expectIdentifier("method name");
                }
                String identifier = lexer.identifier();
                lexer.nextToken();
                if (lexer.token() != Token.OPEN_PARENTHESIS) {
                    expect(Token.OPEN_PARENTHESIS);
                }
                List<AstNode> arguments = parseExpressionList(Token.OPEN_PARENTHESIS, Token.CLOSE_PARENTHESIS, "arguments to call of \"" + identifier + "\"");
                primary = new AstNode.FunctionCall(location, identifier, primary, arguments.toArray(new AstNode[arguments.size()]));
            } else {
                // Handle operator[] and operator[]=.
                expect(Token.OPEN_BRACKET);
                AstNode indexExpression = parseExpression();
                expect(Token.CLOSE_BRACKET);
                if (lexer.token() == Token.ASSIGN) {
                    expect(Token.ASSIGN);
                    AstNode valueExpression = parseExpression();
                    primary = new AstNode.FunctionCall(location, "__set_item__", primary, new AstNode[] { indexExpression, valueExpression });
                    // You can't index after a set.
                    break;
                } else {
                    primary = new AstNode.FunctionCall(location, "__get_item__", primary, new AstNode[] { indexExpression });
                }
            }
        }
        
        return primary;
    }
}
