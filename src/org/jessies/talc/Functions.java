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

public class Functions {
    public static BooleanValue eq(Object lhs, Object rhs) {
        if (lhs == null) {
            return BooleanValue.valueOf(rhs == null);
        }
        return BooleanValue.valueOf(lhs.equals(rhs));
    }
    
    public static BooleanValue ne(Object lhs, Object rhs) {
        if (lhs == null) {
            return BooleanValue.valueOf(rhs != null);
        }
        return BooleanValue.valueOf(!lhs.equals(rhs));
    }
    
    public static StringValue backquote(StringValue command) {
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command.toString());
        StringWriter output = new StringWriter();
        IntegerValue status = runProcessBuilder(processBuilder, output);
        // FIXME: make the status available somehow.
        return new StringValue(output.toString());
    }
    
    public static void exit(IntegerValue status) {
        System.exit(status.intValue());
    }
    
    public static StringValue getenv(StringValue name) {
        String value = System.getenv(name.toString());
        return (value != null) ? new StringValue(value) : null;
    }
    
    private static java.io.BufferedReader stdin; // FIXME: is there a better home for this?
    public static StringValue gets() {
        // You might like the idea of using System.console() here, but it only works for /dev/tty.
        // It's pretty normal for scripts to work on redirected input, so System.console() is no good to us.
        String result = null;
        try {
            if (stdin == null) {
                stdin = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
            }
            result = stdin.readLine();
        } catch (java.io.IOException ex) {
            // FIXME: does swallowing the exception and returning null make sense?
        }
        return (result != null) ? new StringValue(result) : null;
    }
    
    public static void print(Object value) {
        Object printable = value;
        if (value == null) {
            printable = "null";
        }
        System.out.print(printable);
    }
    
    public static void print(Object[] values) {
        for (Object value : values) {
            print(value);
        }
    }
    
    public static void puts(Object value) {
        print(value);
        System.out.println();
    }
    
    public static void puts(Object[] values) {
        print(values);
        System.out.println();
    }
    
    private static IntegerValue runProcessBuilder(ProcessBuilder processBuilder, Appendable output) {
        processBuilder.redirectErrorStream(true);
        int status = -1;
        try {
            Process process = processBuilder.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                output.append(line);
                output.append('\n');
            }
            in.close();
            // GCJ/Classpath hangs in waitFor unless we've explicitly closed all three streams.
            process.getOutputStream().close();
            process.getErrorStream().close();
            status = process.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new IntegerValue(status);
    }
    
    public static IntegerValue shell(StringValue command) {
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command.toString());
        return runProcessBuilder(processBuilder, System.out);
    }
    
    public static IntegerValue system(ListValue talcArgs) {
        // Convert the talc arguments into native arguments.
        List<String> args = new ArrayList<String>();
        // FIXME: we need a better way to iterate over a ListValue.
        final int max = talcArgs.length().intValue();
        for (int i = 0; i < max; ++i) {
            args.add(talcArgs.__get_item__(new IntegerValue(i)).toString());
        }
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        return runProcessBuilder(processBuilder, System.out);
    }
    
    public static IntegerValue time_ms() {
        return new IntegerValue(System.currentTimeMillis());
    }
    
    
    public static class File_file extends BuiltInFunction {
        public File_file() {
            super("file", Arrays.asList("filename"), Arrays.asList(TalcType.STRING), TalcType.FILE);
            markAsConstructor();
        }
    }
    
    public static class List_list extends BuiltInFunction {
        public List_list() {
            super("list", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.LIST_OF_T);
            markAsConstructor();
        }
    }
    
    public static class Map_map extends BuiltInFunction {
        public Map_map() {
            super("map", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.MAP_OF_K_V);
            markAsConstructor();
        }
    }
    
    public static class Numeric_to_i extends BuiltInFunction {
        public Numeric_to_i() {
            super("to_i", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.INT);
        }
    }
    
    public static class Numeric_to_r extends BuiltInFunction {
        public Numeric_to_r() {
            super("to_r", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.REAL);
        }
    }
    
    public static class backquote extends BuiltInFunction {
        public backquote() {
            super("backquote", Arrays.asList("command"), Arrays.asList(TalcType.STRING), TalcType.STRING);
        }
    }
    
    public static class Exit extends BuiltInFunction {
        public Exit() {
            super("exit", Arrays.asList("status"), Arrays.asList(TalcType.INT), TalcType.VOID);
        }
    }
    
    public static class Gets extends BuiltInFunction {
        public Gets() {
            super("gets", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.STRING);
        }
    }
    
    public static class Getenv extends BuiltInFunction {
        public Getenv() {
            super("getenv", Arrays.asList("name"), Arrays.asList(TalcType.STRING), TalcType.STRING);
        }
    }
    
    public static class Print extends BuiltInFunction {
        public Print() {
            super("print", null, null, TalcType.VOID);
        }
    }
    
    public static class Puts extends BuiltInFunction {
        public Puts() {
            super("puts", null, null, TalcType.VOID);
        }
    }
    
    public static class shell extends BuiltInFunction {
        public shell() {
            super("shell", Arrays.asList("command"), Arrays.asList(TalcType.STRING), TalcType.INT);
        }
    }
    
    public static class system extends BuiltInFunction {
        public system() {
            super("system", Arrays.asList("command"), Arrays.asList(TalcType.LIST_OF_STRING), TalcType.INT);
        }
    }
    
    public static class time_ms extends BuiltInFunction {
        public time_ms() {
            super("time_ms", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.INT);
        }
    }
}
