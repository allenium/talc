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

import java.io.*;
import java.util.*;

public class Lexer {
    private static final int EOF = -1;
    
    private static final HashMap<String, Token> KEYWORDS;
    static {
        KEYWORDS = new HashMap<String, Token>();
        KEYWORDS.put("break", Token.BREAK);
        KEYWORDS.put("class", Token.CLASS);
        KEYWORDS.put("continue", Token.CONTINUE);
        KEYWORDS.put("do", Token.DO);
        KEYWORDS.put("else", Token.ELSE);
        KEYWORDS.put("extends", Token.EXTENDS);
        KEYWORDS.put("false", Token.FALSE);
        KEYWORDS.put("final", Token.FINAL);
        KEYWORDS.put("for", Token.FOR);
        KEYWORDS.put("function", Token.FUNCTION);
        KEYWORDS.put("if", Token.IF);
        KEYWORDS.put("implements", Token.IMPLEMENTS);
        KEYWORDS.put("in", Token.IN);
        KEYWORDS.put("new", Token.NEW);
        KEYWORDS.put("null", Token.NULL);
        KEYWORDS.put("return", Token.RETURN);
        KEYWORDS.put("true", Token.TRUE);
        KEYWORDS.put("void", Token.VOID);
        KEYWORDS.put("while", Token.WHILE);
    }
    
    private MyPushbackReader reader;
    private Token token;
    private String identifier;
    private Value numericLiteral;
    private boolean DEBUG_LEXER = Talc.debugging('l');
    
    public Lexer(String expression) {
        this(new StringReader(expression));
    }
    
    public Lexer(File file) throws IOException {
        this(new InputStreamReader(new FileInputStream(file)));
        reader.setFile(file);
    }
    
    private Lexer(Reader reader) {
        this.reader = new MyPushbackReader(new BufferedReader(reader));
        nextToken();
    }
    
    public void nextToken() {
        try {
            token = nextToken0();
            if (DEBUG_LEXER) {
                System.err.println("nextToken() => " + token);
            }
        } catch (IOException ex) {
            throw new TalcError(this, ex.toString());
        }
    }
    
    private Token maybe(char expectedChar, Token yesToken, Token noToken) throws IOException {
        int ch = reader.read();
        if (ch == expectedChar) {
            return yesToken;
        }
        reader.unread(ch);
        return noToken;
    }
    
    private Token maybe2(char expectedChar1, Token yesToken1, char expectedChar2, Token yesToken2, Token neitherToken) throws IOException {
        int ch2 = reader.read();
        if (ch2 == expectedChar1) {
            return yesToken1;
        } else if (ch2 == expectedChar2) {
            return yesToken2;
        }
        reader.unread(ch2);
        return neitherToken;
    }
    
    private Token maybeDoubleOrAssign(char initialChar, Token singleToken, Token doubleToken, Token singleAssignToken, Token doubleAssignToken) throws IOException {
        Token result;
        int ch2 = reader.read();
        if (ch2 == initialChar) {
            int ch3 = reader.read();
            if (ch3 == '=') {
                result = doubleAssignToken;
            } else {
                result = doubleToken;
                reader.unread(ch3);
            }
        } else if (ch2 == '=') {
            result = singleAssignToken;
        } else {
            result = singleToken;
            reader.unread(ch2);
        }
        return result;
    }
    
    private Token nextToken0() throws IOException {
        int ch;
        
        // Skip whitespace and control characters.
        while ((ch = reader.read()) != EOF && ch <= ' ') {
        }
        
        // Skip shell-like comments to end of line.
        while (ch == '#') {
            while ((ch = reader.read()) != EOF && ch != '\n') {
            }
            // Skip the whitespace from the terminating newline to the first non-whitespace character on the next line.
            while (ch != EOF && ch <= ' ') {
                ch = reader.read();
            }
        }
        
        switch (ch) {
            case EOF: return Token.END_OF_INPUT;
            
            case '\n': return nextToken0();
            
            case ':': return Token.COLON;
            case ';': return Token.SEMICOLON;
            case ',': return Token.COMMA;
            case '.': return Token.DOT;
            
            case '(': return Token.OPEN_PARENTHESIS;
            case ')': return Token.CLOSE_PARENTHESIS;
            case '{': return Token.OPEN_BRACE;
            case '}': return Token.CLOSE_BRACE;
            case '[': return Token.OPEN_BRACKET;
            case ']': return Token.CLOSE_BRACKET;
            
            case '=': return maybe('=', Token.EQ, Token.ASSIGN);
            case '+': return maybe2('+', Token.PLUS_PLUS, '=', Token.PLUS_ASSIGN, Token.PLUS);
            case '-': return maybe2('-', Token.MINUS_MINUS, '=', Token.SUB_ASSIGN, Token.MINUS);
            case '/': return maybe('=', Token.DIV_ASSIGN, Token.DIV);
            case '%': return maybe('=', Token.MOD_ASSIGN, Token.MOD);
            case '!': return maybe('=', Token.NE, Token.PLING);
            case '~': return Token.B_NOT;
            case '^': return maybe('=', Token.XOR_ASSIGN, Token.B_XOR);
            
            case '&': return maybe2('&', Token.L_AND, '=', Token.AND_ASSIGN, Token.B_AND);
            case '|': return maybe2('|', Token.L_OR, '=', Token.OR_ASSIGN, Token.B_OR);
            
            case '*': return maybeDoubleOrAssign('*', Token.MUL, Token.POW, Token.MUL_ASSIGN, Token.POW_ASSIGN);
            case '<': return maybeDoubleOrAssign('<', Token.LT, Token.SHL, Token.LE, Token.SHL_ASSIGN);
            case '>': return maybeDoubleOrAssign('>', Token.GT, Token.SHR, Token.GE, Token.SHR_ASSIGN);
            
            case '@': case '"': case '\'': return readStringLiteral(ch);
            
        default:
            if (ch >= '0' && ch <= '9') {
                // Integer literal.
                StringBuilder text = new StringBuilder();
                
                // Work out the base.
                int base = 10;
                if (ch == '0') {
                    int ch2 = reader.read();
                    if (ch2 == 'x') {
                        base = 16;
                    } else if (ch2 == 'o') {
                        base = 8;
                    } else if (ch2 == 'b') {
                        base = 2;
                    } else {
                        reader.unread(ch2);
                    }
                }
                
                boolean isReal = false;
                while (ch != EOF && (isValidDigit((char) ch, base) || (base == 10 && ch == '.'))) {
                    text.append((char) ch);
                    if (ch == '.') {
                        isReal = true;
                    }
                    ch = reader.read();
                }
                reader.unread(ch);
                
                if (isReal) {
                    numericLiteral = new RealValue(text.toString());
                } else {
                    numericLiteral = new IntegerValue(text.toString(), base);
                }
                return isReal ? Token.REAL_LITERAL : Token.INT_LITERAL;
            } else if (isIdentifierStartCharacter(ch)) {
                // Identifier.
                StringBuilder text = new StringBuilder();
                while (ch != EOF && isIdentifierCharacter(ch)) {
                    text.append((char) ch);
                    ch = reader.read();
                }
                reader.unread(ch);
                identifier = text.toString();
                
                Token keyword = KEYWORDS.get(identifier);
                return (keyword != null ? keyword : Token.IDENTIFIER);
            } else {
                throw new TalcError(this, "invalid character '" + ensurePrintable(ch) + "' in input");
            }
        }
    }
    
    private Token readStringLiteral(int ch) throws IOException {
        boolean raw = (ch == '@');
        if (raw) {
            ch = reader.read();
            if (ch != '"' && ch != '\'') {
                throw new TalcError(this, "invalid character '" + ensurePrintable(ch) + "' after @ at start of raw string literal (expected @\"text\")");
            }
        }
        int closeQuote = ch;
        StringBuilder text = new StringBuilder();
        while ((ch = reader.read()) != EOF) {
            if (raw) {
                if (ch == closeQuote) {
                    ch = reader.read();
                    if (ch != closeQuote) {
                        reader.unread(ch);
                        break;
                    }
                }
            } else if (ch == closeQuote) {
                break;
            } else if (ch == '\\') {
                ch = reader.read();
                switch (ch) {
                    case '"': break;
                    case '\'': break;
                    case '\\': break;
                    case 'b': ch = '\b'; break;
                    case 'e': ch = '\u001b'; break;
                    case 'f': ch = '\f'; break;
                    case 'n': ch = '\n'; break;
                    case 'r': ch = '\r'; break;
                    case 't': ch = '\t'; break;
                case 'u':
                    ch = readUnicodeEscape();
                    break;
                default:
                    throw new TalcError(this, "invalid escape character '" + ensurePrintable(ch) + "' in string literal");
                }
            }
            text.append((char) ch);
        }
        identifier = text.toString();
        return Token.STRING_LITERAL;
    }
    
    private char readUnicodeEscape() throws IOException {
        int u0 = readUnicodeEscapeDigit();
        int u1 = readUnicodeEscapeDigit();
        int u2 = readUnicodeEscapeDigit();
        int u3 = readUnicodeEscapeDigit();
        char ch = (char) ((u0 << 12) | (u1 << 8) | (u2 << 4) | u3);
        return ch;
    }
    
    private int readUnicodeEscapeDigit() throws IOException {
        int ch = reader.read();
        if (ch == EOF) {
            throw new TalcError(this, "hit end of input during Unicode escape");
        }
        int digit = Character.digit(ch, 16);
        if (digit == -1) {
            throw new TalcError(this, "invalid character '" + ensurePrintable(ch) + "' in Unicode escape (expected hex digit)");
        }
        return digit;
    }
    
    public static String ensurePrintable(int ch) {
        if (ch >= ' ' && ch <= '~') {
            return  String.valueOf((char) ch);
        } else {
            return String.format("\\u%04x", ch);
        }
    }
    
    private static boolean isValidDigit(char ch, int base) {
        if (base <= 10) {
            return (ch >= '0' && ch < ('0' + base));
        } else {
            if (ch >= '0' && ch <= '9') {
                return true;
            } else {
                ch = Character.toLowerCase(ch);
                return (ch >= 'a' && ch < ('a' + base - 10));
            }
        }
    }
    
    private static boolean isIdentifierStartCharacter(int ch) {
        return Character.isJavaIdentifierStart(ch);
    }
    
    private static boolean isIdentifierCharacter(int ch) {
        return Character.isJavaIdentifierPart(ch);
    }
    
    public Token token() {
        return token;
    }
    
    public String identifier() {
        if (token != Token.IDENTIFIER && token != Token.STRING_LITERAL) {
            throw new TalcError(this, "Lexer.identifier called when current token was " + token);
        }
        return identifier;
    }
    
    public Value numericLiteral() {
        if (token != Token.INT_LITERAL && token != Token.REAL_LITERAL) {
            throw new TalcError(this, "Lexer.numericLiteral called when current token was " + token);
        }
        return numericLiteral;
    }
    
    public SourceLocation getLocation() {
        return reader.getLocation();
    }
    
    private void debugScanner() {
        while (token() != Token.END_OF_INPUT) {
            switch (token()) {
                case IDENTIFIER: System.err.println("identifier \"" + identifier() + "\""); break;
                case INT_LITERAL: System.err.println("integer literal " + numericLiteral()); break;
                case REAL_LITERAL: System.err.println("integer literal " + numericLiteral()); break;
                default: System.err.println(token()); break;
            }
            nextToken();
        }
        System.err.println("Exiting.");
        System.exit(1);
    }
    
    /**
     * Like the JDK PushbackReader, but with a larger default pushback buffer, and more intelligent behavior when pushing back EOF.
     */
    private static class MyPushbackReader extends FilterReader {
        private static final int BUFFER_SIZE = 8;
        private char[] buf;
        private int pos;
        
        private File file;
        private int lineNumber;
        private int columnNumber;
        
        public MyPushbackReader(Reader in) {
            super(in);
            this.buf = new char[BUFFER_SIZE];
            this.pos = buf.length;
            
            // Humans count lines from 1, and these are for error reporting.
            this.lineNumber = 1;
            this.columnNumber = 1;
        }
        
        @Override
        public int read() throws IOException {
            synchronized (lock) {
                int result = (pos < buf.length) ? buf[pos++] : super.read();
                ++columnNumber;
                if (result == '\n') {
                    ++lineNumber;
                    columnNumber = 1;
                }
                return result;
            }
        }
        
        public void unread(int c) {
            if (c == -1) {
                return;
            }
            synchronized (lock) {
                if (pos == 0) {
                    throw new TalcError(getLocation(), "pushback buffer overflow");
                }
                --columnNumber;
                if (c == '\n') {
                    --lineNumber;
                    throw new TalcError(getLocation(), "unread('\n') means we no longer know what column we're on"); // FIXME
                }
                buf[--pos] = (char) c;
            }
        }
        
        public void setFile(File file) {
            this.file = file;
        }
        
        public SourceLocation getLocation() {
            return new SourceLocation(file, lineNumber, columnNumber);
        }
        
        @Override
        public boolean ready() throws IOException {
            synchronized (lock) {
                return (pos < buf.length) || super.ready();
            }
        }
        
        @Override public void mark(int readAheadLimit) throws IOException { throw new UnsupportedOperationException(); }
        @Override public boolean markSupported() { return false; }
        @Override public int read(char cbuf[], int off, int len) throws IOException { throw new UnsupportedOperationException(); }
        @Override public void reset() throws IOException { throw new UnsupportedOperationException(); }
        @Override public long skip(long n) throws IOException { throw new UnsupportedOperationException(); }
    }
}
