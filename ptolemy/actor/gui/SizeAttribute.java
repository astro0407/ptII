/* An attribute representing the size of a component.

 Copyright (c) 1998-2003 The Regents of the University of California.
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

@ProposedRating Red (eal@eecs.berkeley.edu)
@AcceptedRating Red (johnr@eecs.berkeley.edu)
*/

package ptolemy.actor.gui;

import ptolemy.kernel.util.*;
import ptolemy.data.IntMatrixToken;
import ptolemy.data.expr.Parameter;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JComponent;
//////////////////////////////////////////////////////////////////////////
//// SizeAttribute
/**
This attribute stores the width and height of a graphical component.
The token in this attribute is an IntMatrixToken containing a matrix
of dimension 1x2, containing the width and the height, in that order.
By default, this attribute has visibility NONE, so the user will not
see it in parameter editing dialogs.

@author Edward A. Lee
@version $Id$
@since Ptolemy II 1.0
*/
public class SizeAttribute extends Parameter {

    /** Construct an attribute with the given name contained by the specified
     *  entity. The container argument must not be null, or a
     *  NullPointerException will be thrown.  This attribute will use the
     *  workspace of the container for synchronization and version counts.
     *  If the name argument is null, then the name is set to the empty string.
     *  Increment the version of the workspace.
     *  @param container The container.
     *  @param name The name of this attribute.
     *  @exception IllegalActionException If the attribute is not of an
     *   acceptable class for the container, or if the name contains a period.
     *  @exception NameDuplicationException If the name coincides with
     *   an attribute already in the container.
     */
    public SizeAttribute(NamedObj container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        setVisibility(Settable.NONE);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Set the value of the attribute to match those of the specified
     *  component.
     *  @param component The component whose size is to be recorded.
     */
    public void recordSize(Component component) {
        try {
            Rectangle bounds = component.getBounds();
            int[][] boundsMatrix = new int[1][2];
            boundsMatrix[0][0] = bounds.width;
            boundsMatrix[0][1] = bounds.height;

            IntMatrixToken token = new IntMatrixToken(boundsMatrix);
            setToken(token);
        } catch (IllegalActionException ex) {
            throw new InternalErrorException("Can't set bounds value!");
        }
    }

    /** Set the size of the specified component to match the
     *  current value of the attribute.  If the value of the attribute
     *  has not been set, then do nothing.
     *  @param component The component whose size is to be set.
     *  @return True if successful.
     */
    public boolean setSize(Component component) {
        try {
            IntMatrixToken token = (IntMatrixToken)getToken();
            if (token != null) {
                int width = token.getElementAt(0, 0);
                int height = token.getElementAt(0, 1);
                // NOTE: As usual with swing, it's not obvious what the
                // right way to do this is. The following seems to work,
                // found by trial and error.  Even then, the layout
                // manager feels free to override it.
                Dimension dimension = new Dimension(width, height);
                component.setSize(dimension);

                // NOTE: If it's a JComponent, the setSize() is
                // insufficient to set the size (you will have to ask
                // Sun why this is so).  We also have to do the
                // following.
                if (component instanceof JComponent) {
                    ((JComponent)component).setPreferredSize(dimension);
                    ((JComponent)component).setMinimumSize(dimension);
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
