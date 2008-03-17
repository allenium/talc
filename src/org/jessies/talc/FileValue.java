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
import java.nio.*;

public class FileValue {
    private File file;
    
    public FileValue(StringValue filename) {
        this.file = new File(filename.toString());
    }
    
    public void append(StringValue content) {
        writeOrAppend(content, true);
    }
    
    public BooleanValue exists() {
        return BooleanValue.valueOf(file.exists());
    }
    
    public BooleanValue is_directory() {
        return BooleanValue.valueOf(file.isDirectory());
    }
    
    public BooleanValue is_executable() {
        return BooleanValue.valueOf(file.canExecute());
    }
    
    public BooleanValue mkdir() {
        return BooleanValue.valueOf(file.mkdir());
    }
    
    public BooleanValue mkdir_p() {
        return BooleanValue.valueOf(file.mkdirs());
    }
    
    public StringValue read() {
        // salma-hayek has a more sophisticated implementation (that copes with other charsets) but I'm still trying to avoid the dependency.
        try {
            DataInputStream dataInputStream = null;
            try {
                // Always read the whole file in rather than using memory mapping.
                dataInputStream = new DataInputStream(new FileInputStream(file));
                ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[(int) file.length()]);
                dataInputStream.readFully(byteBuffer.array());
                return new StringValue(new String(byteBuffer.array(), "UTF-8"));
            } finally {
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public ListValue read_lines() {
        String contents = read().toString();
        // The empty file clearly contains no lines but Java's split (unlike Ruby's) would give us a singleton array containing the empty string.
        // FIXME: should we modify Talc's "string.split" so this isn't necessary and users can just go "lines := f.read().split("\n");"?
        if (contents.length() == 0) {
            return new ListValue();
        }
        return new ListValue(contents.split("\n"));
    }
    
    public FileValue realpath() {
        try {
            return new FileValue(new StringValue(file.getCanonicalPath()));
        } catch (IOException ex) {
            // FIXME: what do we want to do with errors?
            throw new RuntimeException(ex);
        }
    }
    
    public void write(StringValue content) {
        writeOrAppend(content, false);
    }
    
    private void writeOrAppend(StringValue content, boolean shouldAppend) {
        try {
            Writer out = null;
            try {
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, shouldAppend), "UTF-8"));
                out.write(content.toString());
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public String toString() {
        return file.toString();
    }
    
    public TalcType type() {
        return TalcType.FILE;
    }
}
