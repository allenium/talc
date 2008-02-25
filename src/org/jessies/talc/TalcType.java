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

import java.util.*;

public class TalcType {
    private static final HashMap<String, TalcType> documentedTypes = new HashMap<String, TalcType>();
    
    // The primordial type.
    public static final TalcType OBJECT = new TalcType(null, "object");
    
    // Built-in simple types.
    public static final TalcType BOOLEAN = new TalcType(OBJECT, "boolean");
    public static final TalcType FILE = new TalcType(OBJECT, "file");
    public static final TalcType INT = new TalcType(OBJECT, "int");
    public static final TalcType MATCH = new TalcType(OBJECT, "match");
    public static final TalcType REAL = new TalcType(OBJECT, "real");
    public static final TalcType STRING = new TalcType(OBJECT, "string");
    public static final TalcType VOID = new TalcType(null, "void");
    
    // Type parameters.
    public static final TalcType K = makeTypeVariable("K");
    public static final TalcType V = makeTypeVariable("V");
    public static final TalcType T = makeTypeVariable("T");
    
    // Built-in parameterized types.
    public static final TalcType LIST_OF_T = new TalcType(OBJECT, "list", T, null);
    public static final TalcType MAP_OF_K_V = new TalcType(OBJECT, "map", K, V);
    
    // Commonly-used list types in the built-in library.
    public static final TalcType LIST_OF_OBJECT = instantiateType(LIST_OF_T, OBJECT, null);
    public static final TalcType LIST_OF_STRING = instantiateType(LIST_OF_T, STRING, null);
    public static final TalcType LIST_OF_K;
    public static final TalcType LIST_OF_V;
    
    // The type of "null".
    public static final TalcType NULL = new TalcType(null, "null-type");
    
    // The special type of list literals. The type checker will find a suitable more specific type.
    public static final TalcType LIST_OF_NOTHING = new TalcType(null, "empty-list");
    
    static {
        OBJECT.members().addFunction(new Functions.Object_to_s());
        addClass(OBJECT);
        
        addClass(BOOLEAN);
        
        FILE.members.addFunction(new Functions.File_append());
        FILE.members.addFunction(new Functions.File_exists());
        FILE.members.addFunction(new Functions.File_file());
        FILE.members.addFunction(new Functions.File_is_directory());
        FILE.members.addFunction(new Functions.File_is_executable());
        FILE.members.addFunction(new Functions.File_mkdir());
        FILE.members.addFunction(new Functions.File_mkdir_p());
        FILE.members.addFunction(new Functions.File_read());
        FILE.members.addFunction(new Functions.File_read_lines());
        FILE.members.addFunction(new Functions.File_realpath());
        FILE.members.addFunction(new Functions.File_write());
        addClass(FILE);
        
        INT.members.addFunction(new Functions.Int_abs());
        INT.members.addFunction(new Functions.Int_signum());
        INT.members.addFunction(new Functions.Int_to_base());
        INT.members.addFunction(new Functions.Int_to_char());
        INT.members.addFunction(new Functions.Numeric_to_i());
        INT.members.addFunction(new Functions.Numeric_to_r());
        addClass(INT);
        
        LIST_OF_T.members.addFunction(new Functions.List_add_all());
        LIST_OF_T.members.addFunction(new Functions.List_clear());
        LIST_OF_T.members.addFunction(new Functions.List_contains());
        LIST_OF_T.members.addFunction(new Functions.List_get());
        LIST_OF_T.members.addFunction(new Functions.List_is_empty());
        LIST_OF_T.members.addFunction(new Functions.List_join());
        LIST_OF_T.members.addFunction(new Functions.List_length());
        LIST_OF_T.members.addFunction(new Functions.List_list());
        LIST_OF_T.members.addFunction(new Functions.List_peek_back());
        LIST_OF_T.members.addFunction(new Functions.List_peek_front());
        LIST_OF_T.members.addFunction(new Functions.List_pop_back());
        LIST_OF_T.members.addFunction(new Functions.List_pop_front());
        LIST_OF_T.members.addFunction(new Functions.List_push_back());
        LIST_OF_T.members.addFunction(new Functions.List_push_front());
        LIST_OF_T.members.addFunction(new Functions.List_put());
        LIST_OF_T.members.addFunction(new Functions.List_remove_all());
        LIST_OF_T.members.addFunction(new Functions.List_remove_at());
        LIST_OF_T.members.addFunction(new Functions.List_remove_first());
        LIST_OF_T.members.addFunction(new Functions.List_reverse());
        LIST_OF_T.members.addFunction(new Functions.List_sort());
        LIST_OF_T.members.addFunction(new Functions.List_to_s());
        LIST_OF_T.members.addFunction(new Functions.List_uniq());
        LIST_OF_K = LIST_OF_T.duplicateWithDifferentKeyType(K);
        LIST_OF_V = LIST_OF_T.duplicateWithDifferentKeyType(V);
        addClass(LIST_OF_T);
        
        MAP_OF_K_V.members.addFunction(new Functions.Map_clear());
        MAP_OF_K_V.members.addFunction(new Functions.Map_get());
        MAP_OF_K_V.members.addFunction(new Functions.Map_has_key());
        MAP_OF_K_V.members.addFunction(new Functions.Map_has_value());
        MAP_OF_K_V.members.addFunction(new Functions.Map_keys());
        MAP_OF_K_V.members.addFunction(new Functions.Map_length());
        MAP_OF_K_V.members.addFunction(new Functions.Map_map());
        MAP_OF_K_V.members.addFunction(new Functions.Map_put());
        MAP_OF_K_V.members.addFunction(new Functions.Map_remove());
        MAP_OF_K_V.members.addFunction(new Functions.Map_values());
        addClass(MAP_OF_K_V);
        
        MATCH.members.addFunction(new Functions.Match_group());
        addClass(MATCH);
        
        REAL.members.addFunction(new Functions.Real_abs());
        REAL.members.addFunction(new Functions.Real_log());
        REAL.members.addFunction(new Functions.Real_log10());
        REAL.members.addFunction(new Functions.Real_logE());
        REAL.members.addFunction(new Functions.Real_signum());
        REAL.members.addFunction(new Functions.Real_sqrt());
        REAL.members.addFunction(new Functions.Numeric_to_i());
        REAL.members.addFunction(new Functions.Numeric_to_r());
        addClass(REAL);
        
        STRING.members.addFunction(new Functions.String_contains());
        STRING.members.addFunction(new Functions.String_ends_with());
        STRING.members.addFunction(new Functions.String_escape_html());
        STRING.members.addFunction(new Functions.String_gsub());
        STRING.members.addFunction(new Functions.String_lc());
        STRING.members.addFunction(new Functions.String_lc_first());
        STRING.members.addFunction(new Functions.String_length());
        STRING.members.addFunction(new Functions.String_match());
        STRING.members.addFunction(new Functions.String_replace());
        STRING.members.addFunction(new Functions.String_split());
        STRING.members.addFunction(new Functions.String_starts_with());
        STRING.members.addFunction(new Functions.String_sub());
        STRING.members.addFunction(new Functions.String_to_i());
        STRING.members.addFunction(new Functions.String_to_r());
        STRING.members.addFunction(new Functions.String_trim());
        STRING.members.addFunction(new Functions.String_uc());
        STRING.members.addFunction(new Functions.String_uc_first());
        addClass(STRING);
    }
    
    // Cached hash code.
    private volatile int hashCode = 0;
    
    private TalcType superclass;
    private String name;
    private TalcType uninstantiatedParametricType;
    private TalcType keyType;
    private TalcType valueType;
    private Scope members;
    private boolean isTypeVariable;
    
    private TalcType() {
    }
    
    // For simple (non-parametric) types.
    private TalcType(TalcType superclass, String name) {
        this.superclass = superclass;
        this.name = name;
        this.members = new Scope(superclass != null ? superclass.members : null);
    }
    
    // For uninstantiated parametric types.
    private TalcType(TalcType superclass, String name, TalcType keyType, TalcType valueType) {
        this.superclass = superclass;
        this.name = name;
        this.members = new Scope(superclass != null ? superclass.members : null);
        this.keyType = keyType;
        this.valueType = valueType;
    }
    
    // For instantiated parametric types.
    private TalcType(TalcType superclass, TalcType uninstantiatedParametricType, TalcType keyType, TalcType valueType) {
        this.superclass = superclass;
        this.members = uninstantiatedParametricType.members;
        this.uninstantiatedParametricType = uninstantiatedParametricType;
        this.keyType = keyType;
        this.valueType = valueType;
    }
    
    // For type variables (currently just K, V, and T).
    private static TalcType makeTypeVariable(String name) {
        TalcType result = new TalcType();
        result.name = name;
        result.isTypeVariable = true;
        return result;
    }
    
    // For turning list<T> into list<K> or list<V>.
    private TalcType duplicateWithDifferentKeyType(TalcType newKeyType) {
        TalcType result = new TalcType();
        result.isTypeVariable = isTypeVariable;
        result.keyType = newKeyType;
        result.members = members;
        result.name = name;
        result.superclass = superclass;
        result.uninstantiatedParametricType = uninstantiatedParametricType;
        result.valueType = valueType;
        return result;
    }
    
    public static TalcType makeUserDefinedClass(TalcType superclass, String name) {
        return new TalcType(superclass, name);
    }
    
    public static void addClass(TalcType t) {
        documentedTypes.put(t.name, t);
    }
    
    public static TalcType byName(String name) {
        return documentedTypes.get(name);
    }
    
    public static Collection<TalcType> documentedTypes() {
        return new TreeMap<String, TalcType>(documentedTypes).values();
    }
    
    public static TalcType instantiateType(TalcType parametricType, TalcType keyType, TalcType valueType) {
        if (parametricType.uninstantiatedParametricType != null && (parametricType.keyType.isTypeVariable == false && parametricType.valueType.isTypeVariable == false)) {
            throw new RuntimeException("instantiateType(" + parametricType + ", " + keyType + ", " + valueType + "): alleged uninstantiated parametric type isn't!");
        }
        TalcType newTypeVariable1 = selectTypeVariable(parametricType.keyType, keyType, valueType);
        TalcType newTypeVariable2 = selectTypeVariable(parametricType.valueType, keyType, valueType);
        return new TalcType(parametricType.superclass(), parametricType, newTypeVariable1, newTypeVariable2);
    }
    
    private static TalcType selectTypeVariable(TalcType typeVariable, TalcType keyType, TalcType valueType) {
        TalcType result = null;
        if (typeVariable == T || typeVariable == K) {
            result = keyType;
        } else if (typeVariable == V) {
            result = valueType;
        } else if (typeVariable != null) {
            throw new RuntimeException("unknown type variable " + typeVariable + "!");
        }
        return result;
    }
    
    // Is this an instantiated parametric type?
    public boolean isInstantiatedParametricType() {
        return (uninstantiatedParametricType != null);
    }
    
    // Is this an uninstantiated parametric type?
    public boolean isUninstantiatedParametricType() {
        return isTypeVariable || (keyType != null && keyType.isUninstantiatedParametricType()) || (valueType != null && valueType.isUninstantiatedParametricType());
    }
    
    // Returns the uninstantiated parametric type for this instantiated parametric type.
    public TalcType uninstantiatedParametricType() {
        return uninstantiatedParametricType;
    }
    
    public TalcType superclass() {
        return superclass;
    }
    
    // Distance from root of class hierarchy.
    // object's depth is 0.
    public int depth() {
        int result = 0;
        for (TalcType type = this; type.superclass != null; type = type.superclass) {
            ++result;
        }
        return result;
    }
    
    // Returns the instantiated type parameter for this type, corresponding to the given type-variable TalcType.
    // That is:
    //   list<string>.typeParameter(T) == string
    //   map<string, color>.typeParameter(V) == color
    public TalcType typeParameter(TalcType typeVariable) {
        if (typeVariable.isTypeVariable == false) {
            throw new RuntimeException("alleged type variable " + typeVariable + " isn't a type variable");
        }
        if (typeVariable == T || typeVariable == K) {
            return keyType;
        } else if (typeVariable == V) {
            return valueType;
        } else {
            return null;
        }
    }
    
    public boolean canBeAssignedTo(TalcType declaredType) {
        // A "null" value can be assigned to any type.
        // All types' values can be assigned to the same type.
        if (this == NULL || this.equals(declaredType)) {
            return true;
        }
        
        // A type can be assigned to any of its superclasses.
        for (TalcType t = superclass; t != null; t = t.superclass) {
            if (t.canBeAssignedTo(declaredType)) {
                return true;
            }
        }
        
        // For a parametric type, both types must be parametric, their uninstantiated types must be the same, and the matching instantiated type parameters must be assignable.
        if (isInstantiatedParametricType()) {
            return declaredType.uninstantiatedParametricType() != null && uninstantiatedParametricType().equals(declaredType.uninstantiatedParametricType()) && (keyType== null || keyType.canBeAssignedTo(declaredType.keyType)) && (valueType == null || valueType.canBeAssignedTo(declaredType.valueType));
        }
        
        // The LIST_OF_NOTHING type, used for empty list literals, can be assigned to any list type.
        if (this == LIST_OF_NOTHING && declaredType.isInstantiatedParametricType() && declaredType.uninstantiatedParametricType().equals(LIST_OF_T)) {
            return true;
        }
        
        return false;
    }
    
    public Scope members() {
        return members;
    }
    
    public Value newInstance(AstEvaluator evaluator) {
        return members().initializeNewInstance(new UserDefinedClassValue(this), evaluator);
    }
    
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof TalcType == false) {
            return false;
        }
        TalcType other = (TalcType) o;
        if (isTypeVariable && other.isTypeVariable) {
            return true;
        }
        if (fieldEquals(superclass, other.superclass) == false) {
            return false;
        }
        if (fieldEquals(name, other.name) == false) {
            return false;
        }
        if (fieldEquals(uninstantiatedParametricType, other.uninstantiatedParametricType) == false) {
            return false;
        }
        if (fieldEquals(keyType, other.keyType) == false) {
            return false;
        }
        if (fieldEquals(valueType, other.valueType) == false) {
            return false;
        }
        if (fieldEquals(members, other.members) == false) {
            return false;
        }
        return true;
    }
    
    private <T> boolean fieldEquals(T field, T otherField) {
        return (field == null ? otherField == null : field.equals(otherField));
    }
    
    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37*result + (superclass == null ? 0 : superclass.hashCode());
            result = 37*result + (name == null ? 0 : name.hashCode());
            result = 37*result + (uninstantiatedParametricType == null ? 0 : uninstantiatedParametricType.hashCode());
            result = 37*result + (keyType == null ? 0 : keyType.hashCode());
            result = 37*result + (valueType == null ? 0 : valueType.hashCode());
            result = 37*result + (members == null ? 0 : members.hashCode());
            hashCode = result;
        }
        return hashCode;
    }
    
    public String describeClass() {
        StringBuilder result = new StringBuilder();
        result.append(this);
        if (superclass != null) {
            result.append(" : ");
            result.append(superclass);
        }
        result.append("\n");
        if (members != null) {
            result.append(members.describeScope());
        }
        return result.toString();
    }
    
    public String describe() {
        return "TalcType[name=" + name + ",keyType=" + keyType + ",valueType=" + valueType + ",superclass=" + superclass + ",unPT=" + uninstantiatedParametricType + ",isTypeVariable=" + isTypeVariable + ",members=" + members + "]";
    }
    
    public String toString() {
        StringBuilder result = new StringBuilder();
        String basicName = name;
        if (name == null && uninstantiatedParametricType != null) {
            basicName = uninstantiatedParametricType.name;
        }
        if (basicName == null) {
            result.append(describe());
        }
        result.append(basicName);
        if (keyType != null) {
            result.append("<");
            result.append(keyType);
            if (valueType != null) {
                result.append(",");
                result.append(valueType);
            }
            result.append(">");
        }
        return result.toString();
    }
}
