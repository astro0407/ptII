/** A class representing the type of a RecordToken.

 Copyright (c) 1997-2000 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

                                        PT_COPYRIGHT_VERSION_2
                                        COPYRIGHTENDKEY

@ProposedRating Red (neuendor@eecs.berkeley.edu)
@AcceptedRating Red (cxh@eecs.berkeley.edu)

*/

package ptolemy.data.type;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.graph.CPO;
import ptolemy.graph.InequalityTerm;
import ptolemy.data.Token;
import ptolemy.data.RecordToken;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

//////////////////////////////////////////////////////////////////////////
//// RecordType
/**
A class representing the type of a RecordToken.

@author Yuhong Xiong
$Id$
*/

public class RecordType extends StructuredType {

    /** Construct a new RecordType with the specified labels and types.
     *  To leave the types of some fields undeclared, use BaseType.ANY.
     *  The labels and the types are specified in two arrays. These two
     *  arrays must have the same length, and their elements have one to
     *  one correspondence. That is, the i'th entry in the types array is
     *  the type for the i'th label in the labels array. To construct the
     *  empty record type, which is the greatest record type, set the length
     *  of the argument arrays to 0.
     *  @param labels A String array.
     *  @param types An array of Type.
     *  @exception IllegalArgumentException If the two arrays do not have
     *   the same size.
     *  @exception NullPointerException If one of the arguments is null.
     */
    public RecordType(String[] labels, Type[] types) {
        if (labels.length != types.length) {
            throw new IllegalArgumentException("RecordType: the labels " +
                "and types arrays do not have the same size.");
        }

        for (int i=0; i<labels.length; i++) {
            FieldType fieldType = new FieldType(this, types[i]);
            _fields.put(labels[i], fieldType);
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Return a deep copy of this RecordType if it is a variable, or
     *  itself if it is a constant.
     *  @return A RecordType.
     */
    public Object clone() {
        if (isConstant()) {
            return this;
        } else {
            // empty record is a constant, so this record type is not empty.

            // construct the labels and declared types array
            String[] labels = (String[])_fields.keySet().toArray();
            Type[] types = new Type[labels.length];
            for (int i=0; i<labels.length; i++) {
              FieldType fieldType = (FieldType)_fields.get(labels[i]);
              types[i] = fieldType._declaredType;
            }
            RecordType newObj = new RecordType(labels, types);
            try {
              newObj.updateType(this);
            } catch (IllegalActionException ex) {
                throw new InternalErrorException("RecordType.clone: Cannot " +
                        "update new instance. " + ex.getMessage());
            }
            return newObj;
        }
    }

    /** Convert the argument token into a RecordToken having this type,
     *  if losslessly conversion can be done.  The argument must be an
     *  RecordToken, and its type must be a subtype of this record type.
     *  If this type is a variable, convert the the argument into a
     *  substitution instance of this variable.
     *  @param t A token.
     *  @return An RecordToken.
     *  @exception IllegalActionException If lossless conversion
     *   cannot be done.
     */
    public Token convert(Token t) throws IllegalActionException {
        if ( !isCompatible(t)) {
            throw new IllegalArgumentException("RecordType.convert: " +
                    "Cannot convert the argument token to this type.");
        }

        RecordToken argRecTok = (RecordToken)t;
        String[] labels = (String[])_fields.keySet().toArray();
        Token[] values = new Token[labels.length];
        for (int i=0; i<labels.length; i++) {
            Token orgToken = argRecTok.get(labels[i]);
            FieldType fieldType = (FieldType)_fields.get(labels[i]);
            Type type = fieldType._resolvedType;
            values[i] = type.convert(orgToken);
        }

        return new RecordToken(labels, values);
    }

    /** Return the type of the specified label. If this type does not
     *  contain the specified label, return null.
     *  @return a Type.
     */
    public Type get(String label) {
        FieldType fieldType = (FieldType)_fields.get(label);
        if (fieldType == null) {
            return null;
        }
        return fieldType._resolvedType;
    }

    /** Return the InequalityTerm representing the type of the specified
     *  label.
     *  @return An InequalityTerm.
     *  @see ptolemy.graph.InequalityTerm
     */
    public InequalityTerm getTypeTerm(String label) {
        return (InequalityTerm)_fields.get(label);
    }

    /** Return the user of this StructuredType. If the user is not set,
     *  return null.
     *  @return An Object.
     */
    // FIXME: This method will be removed after verifying "user" is not
    // needed.
    public Object getUser() {
        return null;
    }

    /** Test if the argument token is compatible with this type.
     *  If this type is a constant, the argument is compatible if it can be
     *  converted losslessly to a token of this type; If this type is a
     *  variable, the argument is compatible if its type is a substitution
     *  instance of this type, or if it can be converted losslessly to a
     *  substitution instance of this type. 
     *  @param t A Token.
     *  @return True if the argument is compatible with this type.
     */
    public boolean isCompatible(Token t) {
        if ( !(t.getType() instanceof RecordToken)) {
            return false;
        }

        RecordToken argRecTok = (RecordToken)t;
        Iterator iter = _fields.keySet().iterator();
        while (iter.hasNext()) {
            String label = (String)iter.next();
            if (argRecTok.get(label) == null) {
                // argument token does not contain this label
                return false;
            }
            FieldType fieldType = (FieldType)_fields.get(label);
            Type type = fieldType._declaredType;
            Token value = (Token)argRecTok.get(label);
            if ( !type.isCompatible(value)) {
                return false;
            }
        }

        return true;
    }

    /** Test if this RecordType is a constant. A RecordType is a constant if
     *  it does not contain BaseType.ANY in any level.
     *  @return True if this type is a constant.
     */
    public boolean isConstant() {
        Iterator iter = _fields.values().iterator();
        while (iter.hasNext()) {
            FieldType fieldType = (FieldType)iter.next();
            Type type = fieldType._declaredType;
            if ( !type.isConstant()) {
                return false;
            }
        }
        return true;
    }

    /** Determine if the argument represents the same RecordType as this
     *  object.
     *  @param t A Type.
     *  @return True if the argument represents the same RecordType as
     *   this object; false otherwise.
     */
    public boolean isEqualTo(Type t) {
        if ( !(t instanceof RecordType)) {
            return false;
        }

        RecordType argRecType = (RecordType)t;

        // check my label set is the same as that of the argument
        Set myLabelSet = _fields.keySet();
        Set argLabelSet = argRecType._fields.keySet();
        if ( !myLabelSet.equals(argLabelSet)) {
            return false;
        }

        Iterator iter = myLabelSet.iterator();
        while (iter.hasNext()) {
            String label = (String)iter.next();
            Type myType = this.get(label);
            Type argType = argRecType.get(label);
            if ( !myType.isEqualTo(argType)) {
                return false;
            }
        }

        return true;
    }

    /** Determine if this type corresponds to an instantiable token
     *  class. A RecordType is instantiable if its element types are
     *  instantiable.
     *  @return True if this type is instantiable.
     */
    public boolean isInstantiable() {
        Iterator iter = _fields.values().iterator();
        while (iter.hasNext()) {
            FieldType fieldType = (FieldType)iter.next();
            Type type = fieldType._resolvedType;
            if ( !type.isInstantiable()) {
                return false;
            }
        }

        return true;
    }

    /** Return true if the specified type is a substitution instance of this
     *  type. For the argument to be a substitution instance, this type must
     *  be a variable, and the argument must be a type that can be obtained
     *  by replacing the BaseType.ANY component of the declared type by
     *  another type. This means that these two record types must have the
     *  same set of labels.
     *  @parameter type A Type.
     *  @return True if the argument is a substitution instance of this type.
     */
    public boolean isSubstitutionInstance(Type type) {
        if (isConstant() || ( !(type instanceof RecordType))) {
            return false;
        }

        RecordType argRecType = (RecordType)type;

        // check if this record type and the argument have the same label set
        Set myLabelSet = _fields.keySet();
        Set argLabelSet = argRecType._fields.keySet();
        if ( !myLabelSet.equals(argLabelSet)) {
            return false;
        }

        Iterator iter = myLabelSet.iterator();
        while (iter.hasNext()) {
            String label = (String)iter.next();
            FieldType fieldType = (FieldType)_fields.get(label);
            Type myDecType = fieldType._declaredType;
            Type argType = argRecType.get(label);
            if ( !myDecType.isSubstitutionInstance(argType)) {
                return false;
            }
        }

        return true;
    }

    /** Set the user of this RecordType. The user can only be set once,
     *  otherwise an exception will be thrown.
     *  @param Object The user.
     *  @exception IllegalActionException If the user is already set, or
     *   if the argument is null.
     */
    // FIXME: This method will be removed after verifying "user" is not
    // needed.
    public void setUser(Object user)
            throws IllegalActionException {
        throw new IllegalActionException("RecordType.setUser: " +
                    "This method should not be called.");
    }

    /** Return the string representation of this type. The format is
     *  {<lable>:<type>, <label>:<type>, ...}.
     *  @return A String.
     */
    public String toString() {
        String s = "{";
        Iterator iter = _fields.keySet().iterator();
        boolean first = true;
        while (iter.hasNext()) {
            String label = (String)iter.next();
            String type = this.get(label).toString();
            if (first) {
                first = false;
            } else {
                s += ", ";
            }
            s += label + ":" + type;
        }
        return s + "}";
    }

    /** Set the elements that have declared type BaseType.ANY (the leaf
     *  type variable) to the specified type.
     *  This method is called at the beginning of type resolution.
     *  @param t the type to set the leaf type variable to.
     */
    public void initialize(Type t) {
        try {
            Iterator iter = _fields.keySet().iterator();
            while (iter.hasNext()) {
                String label = (String)iter.next();
                FieldType fieldType = (FieldType)get(label);
                if (fieldType.isSettable()) {
                    fieldType.initialize(t);
                }
            }
        } catch (IllegalActionException iae) {
            throw new InternalErrorException("RecordType.initialize: Cannot " +
                "initialize the element type to " + t + " " +
                iae.getMessage());
        }
    }

    /** Update this Type to the specified RecordType.
     *  The specified type must be a substitution instance of this type.
     *  This method will only update the component whose declared type is
     *  BaseType.ANY, and leave the constant part of this type intact.
     *  @param st A StructuredType.
     *  @exception IllegalActionException If the specified type is not a
     *   substitution instance of this type.
     */
    public void updateType(StructuredType newType)
            throws IllegalActionException {
        if ( !this.isSubstitutionInstance(newType)) {
            throw new IllegalActionException("RecordType.updateType: " +
                "The argument is not a substitution instance of this type.");
        }

        Iterator iter = _fields.keySet().iterator();
        while (iter.hasNext()) {
            String label = (String)iter.next();
            FieldType fieldType = (FieldType)this.get(label);
            if (fieldType.isSettable()) {
                Type newFieldType = ((RecordType)newType).get(label);
                fieldType.setValue(newFieldType);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                       protected methods                   ////

    /** Compare this type with the specified type. The specified type
     *  must be a RecordType, otherwise an exception will be thrown.
     *
     *  This method returns one of ptolemy.graph.CPO.LOWER,
     *  ptolemy.graph.CPO.SAME, ptolemy.graph.CPO.HIGHER,
     *  ptolemy.graph.CPO.INCOMPARABLE, indicating this type is lower
     *  than, equal to, higher than, or incomparable with the
     *  specified type in the type hierarchy, respectively.
     *  @param t a RecordType.
     *  @return An integer.
     *  @exception IllegalArgumentException If the specified type is
     *   not a RecordType.
     */
    protected int _compare(StructuredType t) {
        if ( !(t instanceof RecordType)) {
            throw new IllegalArgumentException("RecordType.compare: " +
                    "The argument is not a RecordType.");
        }

        if (this.isEqualTo(t)) {
            return CPO.SAME;
        }

        if (_isLessThanOrEqualTo(this, (RecordType)t)) {
            return CPO.LOWER;
        }

        if (_isLessThanOrEqualTo((RecordType)t, this)) {
            return CPO.HIGHER;
        }

        return CPO.INCOMPARABLE;
    }

    /** Determine if the specified StructuredType is this object, or
     *  a user of this type, or a user of a higher level.
     *  @return True if the above condition is true.
     */
    // FIXME: this method will be removed.
    protected boolean _deepIsUser(Object st) {
//        if (st == this) {
//            return true;
//        }

//        if (_user != null && (_user instanceof StructuredType)) {
//            return ((StructuredType)_user)._deepIsUser(st);
//        }

        return false;
    }

    /** Return a static instance of RecordType.
     *  @return a RecordType.
     */
    protected StructuredType _getRepresentative() {
        return _representative;
    }

    /** Return the greatest lower bound of this type with the specified
     *  type. The specified type must be a RecordType, otherwise an
     *  exception will be thrown.
     *  @param t a RecordType.
     *  @return a RecordType.
     *  @exception IllegalArgumentException If the specified type is
     *   not a RecordType.
     */
    protected StructuredType _greatestLowerBound(StructuredType t) {
        if ( !(t instanceof RecordType)) {
            throw new IllegalArgumentException(
                "RecordType.greatestLowerBound: The argument is not a " +
                "RecordType.");
        }

        RecordType argRecType = (RecordType)t;

        // the label set of the GLB is the union of the two label sets.
        Set unionSet = new HashSet();
        Set myLabelSet = _fields.keySet();
        Set argLabelSet = argRecType._fields.keySet();

        unionSet.addAll(myLabelSet);
        unionSet.addAll(argLabelSet);

        // construct the GLB RecordToken
        int size = unionSet.size();
        String[] labels = new String[size];
        Type[] types = new Type[size];

        Iterator iter = unionSet.iterator();
        int i = 0;
        while (iter.hasNext()) {
            labels[i] = (String)iter.next();
            Type type1 = this.get(labels[i]);
            Type type2 = argRecType.get(labels[i]);
            if (type1 == null) {
                types[i] = type2;
            } else if (type2 == null) {
                types[i] = type1;
            } else {
                types[i] = (Type)TypeLattice.lattice().greatestLowerBound(
                                                           type1, type2);
            }
            i++;
        }

        return new RecordType(labels, types);
    }

    /** Return the least Upper bound of this type with the specified
     *  type. The specified type must be a RecordType, otherwise an
     *  exception will be thrown.
     *  @param t a RecordType.
     *  @return a RecordType.
     *  @exception IllegalArgumentException If the specified type is
     *   not a RecordType.
     */
    protected StructuredType _leastUpperBound(StructuredType t) {
        if ( !(t instanceof RecordType)) {
            throw new IllegalArgumentException("RecordType.leastUpperBound: "
                    + "The argument is not a RecordType.");
        }

        RecordType argRecType = (RecordType)t;

        // the label set of the LUB is the intersection of the two label sets.
        Set intersectionSet = new HashSet();
        Set myLabelSet = _fields.keySet();
        Set argLabelSet = argRecType._fields.keySet();

        intersectionSet.addAll(myLabelSet);
        intersectionSet.retainAll(argLabelSet);

        // construct the GLB RecordToken
        int size = intersectionSet.size();
        String[] labels = new String[size];
        Type[] types = new Type[size];

        Iterator iter = intersectionSet.iterator();
        int i = 0;
        while (iter.hasNext()) {
            labels[i] = (String)iter.next();
            Type type1 = this.get(labels[i]);
            Type type2 = argRecType.get(labels[i]);
            types[i] = (Type)TypeLattice.lattice().leastUpperBound(
                                                           type1, type2);
            i++;
        }

        return new RecordType(labels, types);
    }

    ///////////////////////////////////////////////////////////////////
    ////                        private methods                    ////

    // Test if the first RecordType is less than or equal to the second
    private boolean _isLessThanOrEqualTo(RecordType t1, RecordType t2) {
        Set labelSet1 = t1._fields.keySet();
        Set labelSet2 = t2._fields.keySet();
        if ( !labelSet1.containsAll(labelSet2)) {
            return false;
        }

        // iterate over the labels of the second type
        Iterator iter = labelSet2.iterator();
        while (iter.hasNext()) {
            String label = (String)iter.next();
            Type type1 = t1.get(label);
            Type type2 = t2.get(label);
            int result = TypeLattice.compare(type1, type2);
            if (result == CPO.HIGHER || result == CPO.INCOMPARABLE) {
                return false;
            }
        }

        return true;
    }

    ///////////////////////////////////////////////////////////////////
    ////                        private variables                  ////

    // Mapping from label to field information.
    private Map _fields = new HashMap();

    // the representative in the type lattice is the empty record.
    private static RecordType _representative =
                        new RecordType(new String[0], new Type[0]);

    ///////////////////////////////////////////////////////////////////
    ////                           inner class                     ////

    // A class that encapsulats the declared and resolved types of a
    // field and implements the InequalityTerm interface.
    private class FieldType implements InequalityTerm {

        // Pass the RecordType reference in the constructor so it can be
        // returned by getAssociatedObject().
        private FieldType(RecordType rt, Type declaredType) {
            try {
                _recordType = rt;
                _declaredType = (Type)declaredType.clone();
                _resolvedType = _declaredType;
            } catch (CloneNotSupportedException cnse) {
                throw new InternalErrorException("RecordType.FieldType: " +
                    "The specified type cannot be cloned.");
            }
        }

        ///////////////////////////////////////////////////////////////
        ////                   public inner methods                ////

        /** Return this RecordType.
         *  @return a RecordType.
         */
            public Object getAssociatedObject() {
            return _recordType;
        }

        /** Return the resolved type.
         *  @return a Type.
         */
        public Object getValue() {
            return _resolvedType;
        }

        /** Return this FieldType in an array if it represents a type
         *  variable. Otherwise, return an array of size zero.
         *  @return An array of InequalityTerm.
         */
            public InequalityTerm[] getVariables() {
            if (isSettable()) {
                InequalityTerm[] variable = new InequalityTerm[1];
                variable[0] = this;
                return variable;
            }
            return (new InequalityTerm[0]);
        }

        /** Reset the variable part of the element type to the specified
         *  type.
         *  @parameter e A Type.
         *  @exception IllegalActionException If this type is not settable,
         *   or the argument is not a Type.
         */
        public void initialize(Object e)
                throws IllegalActionException {
            if ( !isSettable()) {
                throw new IllegalActionException("RecordType$FieldType." +
                        "initialize: The type is not settable.");
            }

            if ( !(e instanceof Type)) {
                throw new IllegalActionException("FieldType.initialize: "
                        + "The argument is not a Type.");
            }

            if (_declaredType == BaseType.NAT) {
                _resolvedType = (Type)e;
            } else {
                // this field type is a structured type.
                ((StructuredType)_resolvedType).initialize((Type)e);
            }
        }

        /** Test if this field type is a type variable.
         *  @return True if this field type is a type variable.
              */
            public boolean isSettable() {
            return (!_declaredType.isConstant());
        }

        /** Check whether the current element type is acceptable.
         *  The element type is acceptable if it represents an
         *  instantiable object.
         *  @return True if the element type is acceptable.
         */
        public boolean isValueAcceptable() {
            return isInstantiable();
        }

        /** Set the element type to the specified type.
         *  @param e a Type.
         *  @exception IllegalActionException If the specified type is not
         *   a substitution instance of the field type.
         */
        public void setValue(Object e) throws IllegalActionException {
            if ( !isSettable()) {
                throw new IllegalActionException(
                        "RecordType$FieldType.setValue: The type is not " +
                        "settable.");
            }

            if ( !_declaredType.isSubstitutionInstance((Type)e)) {
                    throw new IllegalActionException("FieldType.setValue:" +
                    " The new type is not a substitution instance of the " +
                    "field type. field type: " +
                    _declaredType.toString() + "new type: " +
                    e.toString());
            }

            if (_declaredType == BaseType.NAT) {
                _resolvedType = (Type)e;
            } else {
                ((StructuredType)_resolvedType).updateType((StructuredType)e);
            }
        }

        /** Return a string representation of this term.
         *  @return A String.
         */
        public String toString() {
            return "(RecordFieldType, " + getValue() + ")";
        }

        ///////////////////////////////////////////////////////////////
        ////                  private inner variables              ////

        private RecordType _recordType = null;
        private Type _declaredType = null;
        private Type _resolvedType = null;
    }
}

