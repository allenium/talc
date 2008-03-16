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

public class Talc {
    private static final boolean[] debuggingFlags = new boolean[127];
    private static final String[] debuggingFlagNames = new String[127];
    static {
        debuggingFlagNames['c'] = "turns on the JVM bytecode compiler";
        debuggingFlagNames['i'] = "shows each inferred type as it's fixed up";
        debuggingFlagNames['l'] = "shows each token returned by the lexer";
        debuggingFlagNames['p'] = "shows information about parsing as it progresses, and the AST for each completed parse";
        debuggingFlagNames['t'] = "shows timing information for each phase of compilation/execution";
        debuggingFlagNames['T'] = "shows information helpful when debugging the type checker";
        debuggingFlagNames['S'] = "shows the generated JVM bytecodes";
        debuggingFlagNames['s'] = "saves the generated code to /tmp";
        debuggingFlagNames['v'] = "verifies the generated code with ASM's verifier";
        debuggingFlagNames['V'] = "verifies the generated code with BCEL's JustIce verifier (implies 's')";
    }
    
    public Talc() {
    }
    
    // For interactive use, as a calculator.
    public String parseAndEvaluate(String text) {
        if (text.endsWith(";") == false) {
            // For interactive use, supply a trailing semicolon so the user isn't forced to type it.
            text += ";";
        }
        
        List<Value> values = parseAndEvaluate(null, new ListValue(), new Lexer(text));
        StringBuilder result = new StringBuilder();
        for (Value value : values) {
            result.append(value);
            if (value instanceof IntegerValue) {
                IntegerValue integerValue = (IntegerValue) value;
                if (integerValue.compareTo(IntegerValue.NINE) > 0) {
                    result.append(" (0x" + integerValue.to_base(new IntegerValue(16)) + ")");
                }
            }
            result.append('\n');
        }
        return result.toString();
    }
    
    private void reportTime(String task, long ns) {
        if (Talc.debugging('t')) {
            double s = ns/1000000000.0;
            System.err.println("[talc] " + new java.text.DecimalFormat("#.####").format(s) + "s " + task);
            // GCJ's String.format seems to be broken at the moment, preventing me from using this:
            //System.err.println(String.format("%s took %.4f s", task, s);
        }
    }
    
    private List<Value> parseAndEvaluate(Value argv0, ListValue args, Lexer lexer) {
        // 1. Parse.
        long parse0 = System.nanoTime();
        List<AstNode> ast = new Parser(lexer).parse();
        reportTime("parsing", System.nanoTime() - parse0);
        
        // 1a. Insert values for built-in constants.
        Scope.initGlobalScope(argv0, args);
        
        // 2. Compile-time checking.
        // 2a. Set up the symbol table.
        SymbolTable symbolTable = new SymbolTable(ast);
        reportTime("symbol table construction", System.nanoTime() - symbolTable.creationTime());
        // 2b. Type checking.
        AstTypeChecker typeChecker = new AstTypeChecker(ast);
        reportTime("type checking", System.nanoTime() - typeChecker.creationTime());
        // 2c. Non-type checks (which can assume that type-checking passed).
        AstErrorChecker errorChecker = new AstErrorChecker(ast);
        reportTime("other checking", System.nanoTime() - errorChecker.creationTime());
        // 2d. Simplification.
        AstSimplifier simplifier = new AstSimplifier(ast);
        reportTime("simplification", System.nanoTime() - simplifier.creationTime());
        
        if (Talc.debugging('c') == false) {
            long execution0 = System.nanoTime();
            Environment rho = new Environment();
            AstEvaluator evaluator = new AstEvaluator(rho);
            ArrayList<Value> result = new ArrayList<Value>();
            for (AstNode node : ast) {
                result.add(node.accept(evaluator));
            }
            reportTime("interpretation", System.nanoTime() - execution0);
            return result;
        }
        
        // 3. Byte-code generation.
        long codeGeneration0 = System.nanoTime();
        TalcClassLoader loader = new TalcClassLoader();
        JvmCodeGenerator codeGenerator = new JvmCodeGenerator(loader, ast);
        reportTime("code generation", System.nanoTime() - codeGeneration0);
        
        // 4. Execution.
        long execution0 = System.nanoTime();
        try {
            Class<?> generatedClass = loader.getClass("GeneratedClass");
            Object program = generatedClass.getConstructor(new Class[0]).newInstance();
            //generatedClass.getMethod("main").invoke(program);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        reportTime("execution", System.nanoTime() - execution0);
        return null;
    }
    
    private void interactiveReadEvaluatePrintLoop() throws IOException {
        LineReader lineReader = new LineReader();
        String line;
        while ((line = lineReader.readLine("  ")) != null) {
            try {
                System.out.println("= " + parseAndEvaluate(line));
            } catch (Throwable th) {
                reportError(th);
            }
        }
        System.out.print("\b\b");
    }
    
    private void usage(int exitStatus) {
        System.err.println("usage: talc [--copyright] [-D flags] [--dump-class name] [--dump-classes] [-f file] [-e program]");
        System.err.println("where:");
        for (int i = 0; i < debuggingFlagNames.length; ++i) {
            if (debuggingFlagNames[i] != null) {
                System.err.println("  -D " + ((char) i) + "\t" + debuggingFlagNames[i]);
            }
        }
        System.exit(exitStatus);
    }
    
    private void parseArguments(String[] args) throws IOException {
        String scriptFilename = null;
        ListValue scriptArgs = new ListValue();
        boolean inScriptArgs = false;
        boolean didSomethingUseful = false;
        for (int i = 0; i < args.length; ++i) {
            if (inScriptArgs) {
                scriptArgs.push_back(new StringValue(args[i]));
            } else if (args[i].equals("--copyright")) {
                System.out.println("talc - Copyright (C) 2007-2008 Elliott Hughes.");
                System.out.println("ASM bytecode library copyright (C) 2000-2007 INRIA, France Telecom.");
                didSomethingUseful = true;
            } else if (args[i].equals("-h") || args[i].equals("--help")) {
                usage(0);
            } else if (args[i].startsWith("-D")) {
                String flags = args[i].substring(2);
                if (flags.length() == 0) {
                    if (i + 1 >= args.length) {
                        usage(1);
                    }
                    flags = args[++i];
                }
                parseDebuggingFlags(flags);
            } else if (args[i].equals("--dump-class")) {
                String typeName = args[++i];
                TalcType type = TalcType.byName(typeName);
                if (type == null) {
                    die("unknown type \"" + typeName + "\"");
                } else {
                    System.out.print(type.describeClass());
                }
                didSomethingUseful = true;
            } else if (args[i].equals("--dump-classes")) {
                for (TalcType type : TalcType.documentedTypes()) {
                    System.out.println(type.describeClass());
                }
                didSomethingUseful = true;
            } else if (args[i].equals("-e")) {
                String expression = args[++i];
                parseAndEvaluate(null, new ListValue(), new Lexer(expression));
                didSomethingUseful = true;
            } else if (args[i].equals("--")) {
                inScriptArgs = true;
            } else if (args[i].startsWith("-")) {
                die("unrecognized option \"" + args[i] + "\"");
            } else {
                scriptFilename = args[i];
                inScriptArgs = true;
            }
        }
        if (scriptFilename == null) {
            if (didSomethingUseful) {
                // Fair enough, then.
                System.exit(0);
            } else {
                die("no script filename supplied");
            }
        }
        parseAndEvaluate(new StringValue(scriptFilename), scriptArgs, new Lexer(new File(scriptFilename)));
    }
    
    private static void die(String message) {
        System.err.println("talc: error: " + message);
        System.exit(1);
    }
    
    private static void reportError(Throwable th) {
        if (th instanceof TalcError) {
            System.err.println(th.getMessage());
            System.err.println("Java stack:");
            for (StackTraceElement e : th.getStackTrace()) {
                System.err.println("    " + e);
            }
        } else {
            System.err.println("Unexpected internal error:");
            th.printStackTrace();
        }
    }
    
    public static boolean debugging(char ch) {
        if (ch > debuggingFlags.length || debuggingFlagNames[ch] == null) {
            throw new RuntimeException("unknown debugging flag '" + Lexer.ensurePrintable(ch) + "'");
        }
        return debuggingFlags[ch];
    }
    
    private void parseDebuggingFlags(String flags) {
        for (int i = 0; i < flags.length(); ++i) {
            char ch = flags.charAt(i);
            if (debuggingFlagNames[ch] == null) {
                usage(1);
            } else {
                debuggingFlags[ch] = true;
            }
        }
    }
    
    public static void main(String[] args) throws IOException {
        Talc talc = new Talc();
        if (args.length == 0) {
            talc.interactiveReadEvaluatePrintLoop();
        } else {
            try {
                talc.parseArguments(args);
            } catch (Throwable th) {
                reportError(th);
            }
        }
    }
}
