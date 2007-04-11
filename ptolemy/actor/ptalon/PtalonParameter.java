/* FIXME comment
  
 Copyright (c) 2006-2007 The Regents of the University of California.
 All rights reserved.

 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the
 above copyright notice and the following two paragraphs appear in all
 copies of this software.

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

 */
package ptolemy.actor.ptalon;

import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 This parameter corresponds to a parameter created in a Ptalon file.
 @see PtalonActor
  
 @author Adam Cataldo, Elaine Cheong
 @version $Id$
 @Pt.ProposedRating Red (cxh)
 @Pt.AcceptedRating Red (cxh)   
 */
public class PtalonParameter extends Parameter {

    /** Construct a parameter with the given name contained by the specified
     *  entity. The container argument must not be null, or a
     *  NullPointerException will be thrown.  This parameter will use the
     *  workspace of the container for synchronization and version counts.
     *  If the name argument is null, then the name is set to the empty string.
     *  The object is not added to the list of objects in the workspace
     *  unless the container is null.
     *  Increment the version of the workspace.
     *  @param container The container.
     *  @param name The name of the parameter.
     *  @exception IllegalActionException If the parameter is not of an
     *   acceptable class for the container.
     *  @exception NameDuplicationException If the name coincides with
     *   a parameter already in the container.
     */
    public PtalonParameter(NamedObj container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        setStringMode(true);
        _hasValue = false;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Return true if this parameter's value has been set.
     *  @return True if this parameter's value has been set.
     */
    public boolean hasValue() {
        return _hasValue;
    }

    /** Return true if this parameter's value has changed.
     *  @return True if this parameter's value has changed.
     */
    public boolean isValueChanged() {
        return _valueChanged;
    }

    /** Set indicator for whether this parameter's value has changed.
     *  @param value Value of indicator to set.
     */
    public void setValueChanged(boolean value) {
        _valueChanged = value;
    }   
    
    /** Set the expression and flag that the value has been set for
     *  this parameter and that the value has changed.
     *  @param expr The expression to set.
     */
    public void setExpression(String expr) {
        if ((expr == null) || (expr.trim().equals(""))) {
            return;
        }
        _hasValue = true;
        _valueChanged = true;
        
        super.setExpression(expr);
    }

    /** Set the token and flag that the value has been set for this
     *  parameter and that the value has changed.
     *  @param token The token to set.
     *  @throws IllegalActionException If the superclass throws one.
     */
    public void setToken(Token token) throws IllegalActionException {
        _hasValue = true;
        _valueChanged = true;
        
        super.setToken(token);
    }

    /** Set the token and flag that the value has been set for this
     *  parameter and that the value has changed.
     *  @param expression The expression for this token
     *  @throws IllegalActionException If the superclass throws one.
     */
    public void setToken(String expression) throws IllegalActionException {
        if ((expression == null) || (expression.trim().equals(""))) {
            return;
        }
        _hasValue = true;
        _valueChanged = true;
        
        super.setToken(expression);
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                       private variables                   ////

    /** True if this parameter has a value.
     */
    private boolean _hasValue;

    /** True if value has changed.
     */
    public boolean _valueChanged = false;
}
