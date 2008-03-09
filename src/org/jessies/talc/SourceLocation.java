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

import java.io.File;

public final class SourceLocation {
    private File file;
    private int lineNumber;
    private int columnNumber;
    
    public SourceLocation(File file, int lineNumber, int columnNumber) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }
    
    // This is what gets embedded in the debugging information.
    // If we support back-ends other than the JVM, we might have to expose the File and let the back-end decide what's suitable.
    public String getSourceFilename() {
        return (file != null) ? file.getName() : "<stdin>";
    }
    
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (file != null) {
            result.append(file);
        } else {
            result.append("-");
        }
        result.append(":");
        result.append(lineNumber);
        result.append(":");
        result.append(columnNumber);
        result.append(": ");
        return result.toString();
    }
}
