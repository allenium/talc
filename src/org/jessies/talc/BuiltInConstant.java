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

public class BuiltInConstant extends AstNode.VariableDefinition {
    // Pretend that built-in constants are user-defined constants, so they take advantage of the normal compile-time checks.
    public BuiltInConstant(String name, TalcType type, Object value) {
        super(null, name, type, new AstNode.Constant(null, value, type), true);
    }
}
