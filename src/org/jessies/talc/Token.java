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

public enum Token {
    // Special.
    
    END_OF_INPUT("end of input", false),
    IDENTIFIER("identifier", false),
    INT_LITERAL("int literal", false),
    REAL_LITERAL("real literal", false),
    STRING_LITERAL("string literal", false),
    
    // Punctuation.
    
    COLON(":"),
    SEMICOLON(";"),
    COMMA(","),
    DOT("."),
    
    // Ambiguous.
    
    PLING("!"), // May be either L_NOT or FACTORIAL, depending.
    MINUS("-"), // May be either SUB or NEG, depending.
    PLUS_PLUS("++"), // May be either PRE_INCREMENT or POST_INCREMENT.
    MINUS_MINUS("--"), // May be either PRE_DECREMENT or POST_DECREMENT.
    
    // Binary.
    
    PLUS("+"),
    PLUS_ASSIGN("+="),
    
    SUB("binary -"),
    SUB_ASSIGN("-="),
    
    MUL("*"),
    MUL_ASSIGN("*="),
    POW("**"),
    POW_ASSIGN("**="),
    
    DIV("/"),
    DIV_ASSIGN("/="),
    
    MOD("%"),
    MOD_ASSIGN("%="),
    
    LT("<"),
    LE("<="),
    SHL("<<"),
    SHL_ASSIGN("<<="),
    
    GT(">"),
    GE(">="),
    SHR(">>"),
    SHR_ASSIGN(">>="),
    
    ASSIGN("="),
    EQ("=="),
    
    L_NOT("prefix !"),
    NE("!="),
    
    B_AND("&"),
    L_AND("&&"),
    AND_ASSIGN("&="),
    
    B_OR("|"),
    L_OR("||"),
    OR_ASSIGN("|="),
    
    B_XOR("^"),
    XOR_ASSIGN("^="),
    
    // Prefix unary.
    
    B_NOT("unary ~"),
    NEG("unary -"),
    PRE_INCREMENT("prefix ++"),
    PRE_DECREMENT("prefix --"),
    
    // Postifx unary.
    
    FACTORIAL("postfix !"),
    POST_INCREMENT("postfix ++"),
    POST_DECREMENT("postfix --"),
    
    // Brackets.
    
    OPEN_PARENTHESIS("("),
    CLOSE_PARENTHESIS(")"),
    OPEN_BRACE("{"),
    CLOSE_BRACE("}"),
    OPEN_BRACKET("["),
    CLOSE_BRACKET("]"),
    
    // Keywords.
    
    // We don't actually use all of these yet, but we're likely to use most of them sooner or later...
    ASSERT("assert"),
    BREAK("break"),
    CLASS("class"),
    CONTINUE("continue"),
    DO("do"),
    ELSE("else"),
    EXTENDS("extends"),
    EXTERN("extern"),
    FALSE("false"),
    FINAL("final"),
    FOR("for"),
    FUNCTION("function"),
    IF("if"),
    IMPLEMENTS("implements"),
    IMPORT("import"),
    IN("in"),
    NEW("new"),
    NULL("null"),
    RETURN("return"),
    STATIC("static"),
    TRUE("true"),
    VOID("void"),
    WHILE("while")
    ;
    
    public final String name;
    public final boolean isKeyword;
    
    Token(String name) {
        this(name, true);
    }
    
    Token(String name, boolean isKeyword) {
        this.name = name;
        this.isKeyword = isKeyword;
    }
    
    public String toString() {
        if (isKeyword) {
            return "\"" + name + "\"";
        } else {
            return name;
        }
    }
}
