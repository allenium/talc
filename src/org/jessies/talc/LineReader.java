/*
 * This file is part of Talc.
 * Copyright (C) 2007 Elliott Hughes <enh@jessies.org>.
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

public final class LineReader {
    private static boolean haveGnuReadline = false;
    private static java.lang.reflect.Method gnuReadLineMethod;
    private static java.lang.reflect.Method gnuCleanupMethod;
    private static BufferedReader in;
    
    static {
        // Does it look like libreadline-java is installed on this machine?
        String[] jars = new String[] { "/usr/share/java/libreadline-java.jar", "/usr/lib/java-ext/libreadline-java.jar" };
        for (String jar : jars) {
            File jarFile = new File(jar);
            if (jarFile.exists()) {
                try {
                    initGnuReadline(jarFile);
                    break;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (haveGnuReadline == false) {
            // Fall back to pure Java.
            in = new BufferedReader(new InputStreamReader(System.in));
        }
    }
    
    private static void initGnuReadline(File jarFile) throws Exception {
        // Find the relevant org.gnu.readline classes.
        ClassLoader readlineClassLoader = new java.net.URLClassLoader(new java.net.URL[] { jarFile.toURI().toURL() });
        Class<?> readlineClass = Class.forName("org.gnu.readline.Readline", true, readlineClassLoader);
        Class<?> readlineLibraryClass = Class.forName("org.gnu.readline.ReadlineLibrary", true, readlineClassLoader);
        
        // Readline.load(ReadlineLibrary.GnuReadline);
        Object field_ReadlineLibrary_GnuReadline = readlineLibraryClass.getDeclaredField("GnuReadline").get(null);
        readlineClass.getDeclaredMethod("load", readlineLibraryClass).invoke(null, field_ReadlineLibrary_GnuReadline);
        
        // Readline.initReadline("talc");
        readlineClass.getDeclaredMethod("initReadline", String.class).invoke(null, "talc");
        
        gnuCleanupMethod = readlineClass.getDeclaredMethod("cleanup");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                //Readline.cleanup();
                try { gnuCleanupMethod.invoke(null); } catch (Throwable th) {}
            }
        });
        
        gnuReadLineMethod = readlineClass.getDeclaredMethod("readline", String.class);
        haveGnuReadline = true;
    }
    
    public LineReader() {
    }
    
    public String readLine(String prompt) throws IOException {
        if (in != null) {
            System.out.print(prompt);
            System.out.flush();
            return in.readLine();
        } else {
            try {
                // Readline.readline(prompt);
                return (String) gnuReadLineMethod.invoke(null, prompt);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
