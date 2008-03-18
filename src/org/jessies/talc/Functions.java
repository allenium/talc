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
    
    public static class Object_to_s extends BuiltInFunction {
        public Object_to_s() {
            super("to_s", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.STRING);
        }
    }
    
    public static class File_append extends BuiltInFunction {
        public File_append() {
            super("append", Arrays.asList("content"), Arrays.asList(TalcType.STRING), TalcType.VOID);
        }
    }
    
    public static class File_exists extends BuiltInFunction {
        public File_exists() {
            super("exists", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.BOOL);
        }
    }
    
    public static class File_file extends BuiltInFunction {
        public File_file() {
            super("file", Arrays.asList("filename"), Arrays.asList(TalcType.STRING), TalcType.FILE);
            markAsConstructor();
        }
    }
    
    public static class File_is_directory extends BuiltInFunction {
        public File_is_directory() {
            super("is_directory", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.BOOL);
        }
    }
    
    public static class File_is_executable extends BuiltInFunction {
        public File_is_executable() {
            super("is_executable", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.BOOL);
        }
    }
    
    public static class File_mkdir extends BuiltInFunction {
        public File_mkdir() {
            super("mkdir", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.BOOL);
        }
    }
    
    public static class File_mkdir_p extends BuiltInFunction {
        public File_mkdir_p() {
            super("mkdir_p", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.BOOL);
        }
    }
    
    public static class File_read extends BuiltInFunction {
        public File_read() {
            super("read", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.STRING);
        }
    }
    
    public static class File_read_lines extends BuiltInFunction {
        public File_read_lines() {
            super("read_lines", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.LIST_OF_STRING);
        }
    }
    
    public static class File_realpath extends BuiltInFunction {
        public File_realpath() {
            super("realpath", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.FILE);
        }
    }
    
    public static class File_write extends BuiltInFunction {
        public File_write() {
            super("write", Arrays.asList("content"), Arrays.asList(TalcType.STRING), TalcType.VOID);
        }
    }
    
    public static class Int_abs extends BuiltInFunction {
        public Int_abs() {
            super("abs", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.INT);
        }
    }
    
    public static class Int_signum extends BuiltInFunction {
        public Int_signum() {
            super("signum", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.INT);
        }
    }
    
    public static class Int_to_base extends BuiltInFunction {
        public Int_to_base() {
            super("to_base", Arrays.asList("base"), Arrays.asList(TalcType.INT), TalcType.STRING);
        }
    }
    
    public static class Int_to_char extends BuiltInFunction {
        public Int_to_char() {
            super("to_char", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.STRING);
        }
    }
    
    public static class List_add_all extends BuiltInFunction {
        public List_add_all() {
            super("add_all", Arrays.asList("others"), Arrays.asList(TalcType.LIST_OF_T), TalcType.LIST_OF_T);
        }
    }
    
    public static class List_clear extends BuiltInFunction {
        public List_clear() {
            super("clear", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.LIST_OF_T);
        }
    }
    
    public static class List_contains extends BuiltInFunction {
        public List_contains() {
            super("contains", Arrays.asList("value"), Arrays.asList(TalcType.T), TalcType.BOOL);
        }
    }
    
    public static class List___get_item__ extends BuiltInFunction {
        public List___get_item__() {
            super("__get_item__", Arrays.asList("index"), Arrays.asList(TalcType.INT), TalcType.T);
        }
    }
    
    public static class List_is_empty extends BuiltInFunction {
        public List_is_empty() {
            super("is_empty", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.BOOL);
        }
    }
    
    public static class List_join extends BuiltInFunction {
        public List_join() {
            super("join", Arrays.asList("separator"), Arrays.asList(TalcType.STRING), TalcType.STRING);
        }
    }
    
    public static class List_length extends BuiltInFunction {
        public List_length() {
            super("length", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.INT);
        }
    }
    
    public static class List_list extends BuiltInFunction {
        public List_list() {
            super("list", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.LIST_OF_T);
            markAsConstructor();
        }
    }
    
    public static class List_peek_back extends BuiltInFunction {
        public List_peek_back() {
            super("peek_back", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.T);
        }
    }
    
    public static class List_peek_front extends BuiltInFunction {
        public List_peek_front() {
            super("peek_front", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.T);
        }
    }
    
    public static class List_pop_back extends BuiltInFunction {
        public List_pop_back() {
            super("pop_back", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.T);
        }
    }
    
    public static class List_pop_front extends BuiltInFunction {
        public List_pop_front() {
            super("pop_front", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.T);
        }
    }
    
    public static class List_push_back extends BuiltInFunction {
        public List_push_back() {
            super("push_back", Arrays.asList("value"), Arrays.asList(TalcType.T), TalcType.LIST_OF_T);
        }
    }
    
    public static class List_push_front extends BuiltInFunction {
        public List_push_front() {
            super("push_front", Arrays.asList("value"), Arrays.asList(TalcType.T), TalcType.LIST_OF_T);
        }
    }
    
    public static class List___set_item__ extends BuiltInFunction {
        public List___set_item__() {
            super("__set_item__", Arrays.asList("index", "value"), Arrays.asList(TalcType.INT, TalcType.T), TalcType.T);
        }
    }
    
    public static class List_remove_all extends BuiltInFunction {
        public List_remove_all() {
            super("remove_all", Arrays.asList("others"), Arrays.asList(TalcType.LIST_OF_T), TalcType.LIST_OF_T);
        }
    }
    
    public static class List_remove_at extends BuiltInFunction {
        public List_remove_at() {
            super("remove_at", Arrays.asList("index"), Arrays.asList(TalcType.INT), TalcType.LIST_OF_T);
        }
    }
    
    public static class List_remove_first extends BuiltInFunction {
        public List_remove_first() {
            super("remove_first", Arrays.asList("value"), Arrays.asList(TalcType.T), TalcType.BOOL);
        }
    }
    
    public static class List_reverse extends BuiltInFunction {
        public List_reverse() {
            super("reverse", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.LIST_OF_T);
        }
    }
    
    public static class List_sort extends BuiltInFunction {
        public List_sort() {
            super("sort", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.LIST_OF_T);
        }
    }
    
    public static class List_uniq extends BuiltInFunction {
        public List_uniq() {
            super("uniq", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.LIST_OF_T);
        }
    }
    
    public static class List_to_s extends BuiltInFunction {
        public List_to_s() {
            super("to_s", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.STRING);
        }
    }
    
    public static class Map_clear extends BuiltInFunction {
        public Map_clear() {
            super("clear", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.MAP_OF_K_V);
        }
    }
    
    public static class Map___get_item__ extends BuiltInFunction {
        public Map___get_item__() {
            super("__get_item__", Arrays.asList("key"), Arrays.asList(TalcType.K), TalcType.V);
        }
    }
    
    public static class Map_has_key extends BuiltInFunction {
        public Map_has_key() {
            super("has_key", Arrays.asList("key"), Arrays.asList(TalcType.K), TalcType.BOOL);
        }
    }
    
    public static class Map_has_value extends BuiltInFunction {
        public Map_has_value() {
            super("has_value", Arrays.asList("value"), Arrays.asList(TalcType.V), TalcType.BOOL);
        }
    }
    
    public static class Map_keys extends BuiltInFunction {
        public Map_keys() {
            super("keys", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.LIST_OF_K);
        }
    }
    
    public static class Map_length extends BuiltInFunction {
        public Map_length() {
            super("length", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.INT);
        }
    }
    
    public static class Map_map extends BuiltInFunction {
        public Map_map() {
            super("map", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.MAP_OF_K_V);
            markAsConstructor();
        }
    }
    
    public static class Map___set_item__ extends BuiltInFunction {
        public Map___set_item__() {
            super("__set_item__", Arrays.asList("key", "value"), Arrays.asList(TalcType.K, TalcType.V), TalcType.V);
        }
    }
    
    public static class Map_remove extends BuiltInFunction {
        public Map_remove() {
            super("remove", Arrays.asList("key"), Arrays.asList(TalcType.K), TalcType.MAP_OF_K_V);
        }
    }
    
    public static class Map_values extends BuiltInFunction {
        public Map_values() {
            super("values", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.LIST_OF_V);
        }
    }
    
    public static class Match_group extends BuiltInFunction {
        public Match_group() {
            super("group", Arrays.asList("n"), Arrays.asList(TalcType.INT), TalcType.STRING);
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
    
    public static class Real_abs extends BuiltInFunction {
        public Real_abs() {
            super("abs", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.REAL);
        }
    }
    
    public static class Real_log extends BuiltInFunction {
        public Real_log() {
            super("log", Arrays.asList("base"), Arrays.asList(TalcType.REAL), TalcType.REAL);
        }
    }
    
    public static class Real_log10 extends BuiltInFunction {
        public Real_log10() {
            super("log10", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.REAL);
        }
    }
    
    public static class Real_logE extends BuiltInFunction {
        public Real_logE() {
            super("logE", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.REAL);
        }
    }
    
    public static class Real_signum extends BuiltInFunction {
        public Real_signum() {
            super("signum", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.REAL);
        }
    }
    
    public static class Real_sqrt extends BuiltInFunction {
        public Real_sqrt() {
            super("sqrt", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.REAL);
        }
    }
    
    public static class String_contains extends BuiltInFunction {
        public String_contains() {
            super("contains", Arrays.asList("substring"), Arrays.asList(TalcType.STRING), TalcType.BOOL);
        }
    }
    
    public static class String_ends_with extends BuiltInFunction {
        public String_ends_with() {
            super("ends_with", Arrays.asList("suffix"), Arrays.asList(TalcType.STRING), TalcType.BOOL);
        }
    }
    
    public static class String_escape_html extends BuiltInFunction {
        public String_escape_html() {
            super("escape_html", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.STRING);
        }
    }
    
    public static class String_gsub extends BuiltInFunction {
        public String_gsub() {
            super("gsub", Arrays.asList("pattern", "replacement"), Arrays.asList(TalcType.STRING, TalcType.STRING), TalcType.STRING);
        }
    }
    
    public static class String_lc extends BuiltInFunction {
        public String_lc() {
            super("lc", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.STRING);
        }
    }
    
    public static class String_lc_first extends BuiltInFunction {
        public String_lc_first() {
            super("lc_first", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.STRING);
        }
    }
    
    public static class String_length extends BuiltInFunction {
        public String_length() {
            super("length", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.INT);
        }
    }
    
    public static class String_match extends BuiltInFunction {
        public String_match() {
            super("match", Arrays.asList("pattern"), Arrays.asList(TalcType.STRING), TalcType.MATCH);
        }
    }
    
    public static class String_replace extends BuiltInFunction {
        public String_replace() {
            super("replace", Arrays.asList("old", "new"), Arrays.asList(TalcType.STRING, TalcType.STRING), TalcType.STRING);
        }
    }
    
    public static class String_split extends BuiltInFunction {
        public String_split() {
            super("split", Arrays.asList("pattern"), Arrays.asList(TalcType.STRING), TalcType.LIST_OF_STRING);
        }
    }
    
    public static class String_starts_with extends BuiltInFunction {
        public String_starts_with() {
            super("starts_with", Arrays.asList("prefix"), Arrays.asList(TalcType.STRING), TalcType.BOOL);
        }
    }
    
    public static class String_sub extends BuiltInFunction {
        public String_sub() {
            super("sub", Arrays.asList("pattern", "replacement"), Arrays.asList(TalcType.STRING, TalcType.STRING), TalcType.STRING);
        }
    }
    
    public static class String_to_i extends BuiltInFunction {
        public String_to_i() {
            super("to_i", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.INT);
        }
    }
    
    public static class String_to_r extends BuiltInFunction {
        public String_to_r() {
            super("to_r", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.REAL);
        }
    }
    
    public static class String_trim extends BuiltInFunction {
        public String_trim() {
            super("trim", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.STRING);
        }
    }
    
    public static class String_uc extends BuiltInFunction {
        public String_uc() {
            super("uc", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.STRING);
        }
    }
    
    public static class String_uc_first extends BuiltInFunction {
        public String_uc_first() {
            super("uc_first", Collections.<String>emptyList(), Collections.<TalcType>emptyList(), TalcType.STRING);
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
