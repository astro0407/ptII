/* A modular Vergil package for Ptolemy models.

 Copyright (c) 1998-2000 The Regents of the University of California.
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

package ptolemy.vergil;

import ptolemy.actor.*;
import ptolemy.actor.gui.*;
import ptolemy.kernel.util.*;
import ptolemy.kernel.*;
import ptolemy.data.expr.Parameter;
import ptolemy.data.IntToken;
import ptolemy.gui.*;
import ptolemy.moml.MoMLParser;
import ptolemy.vergil.graph.*;
import ptolemy.vergil.toolbox.*;

import diva.graph.*;
import diva.graph.model.*;
import diva.graph.toolbox.GraphParser;
import diva.graph.toolbox.GraphWriter;
import diva.gui.*;
import diva.gui.toolbox.*;
import java.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.net.URL;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * A modular package that can be plugged into Vergil that adds support for 
 * Ptolemy II.
 *
 * @author Steve Neuendorffer
 * @version $Id%
 */
public class PtolemyPackage implements Package {
    public PtolemyPackage(VergilApplication application) {
	_application = application;
	Action action;

        // Create the Devel menu
        JMenu menuDevel = new JMenu("Devel");
        menuDevel.setMnemonic('D');
        _application.addMenu(menuDevel);

        action = new AbstractAction ("Print document info") {
            public void actionPerformed(ActionEvent e) {
                Document d = _application.getCurrentDocument();
                if (d == null) {
                    System.out.println("Graph document is null");
                } else {
                    System.out.println(d.toString());
                }
            }
        };
	_application.addMenuItem(menuDevel, action, 'P',
				 "Print current document info");

        action = new AbstractAction("Execute System") {
            public void actionPerformed(ActionEvent e) {
                PtolemyDocument d = 
		(PtolemyDocument) _application.getCurrentDocument();
                if (d == null) {
                    return;
                }
                try {
		    CompositeActor toplevel =
                        (CompositeActor) d.getModel();

                    // FIXME there is alot of code in here that is similar
                    // to code in MoMLApplet and MoMLApplication.  I think
                    // this should all be in ModelPane.
                    // FIXME set the Director.  This is a hack, but it's the
                    // Simplest hack.
                    if(toplevel.getDirector() == null) {
                        ptolemy.domains.sdf.kernel.SDFDirector director =
                            new ptolemy.domains.sdf.kernel.SDFDirector(
                                    toplevel.workspace());
                        //		    _entityLibrary.getEntity(
                        //	(String)_directorComboBox.getSelectedItem());
                        toplevel.setDirector(director);
                        director.iterations.setExpression("25");
                    }

                    // Create a manager.
                    Manager manager = toplevel.getManager();
                    if(manager == null) {
                        manager =
                            new Manager(toplevel.workspace(), "Manager");
                        toplevel.setManager(manager);
                        // manager.addDebugListener(new StreamListener());
			manager.addExecutionListener(
			    new VergilExecutionListener());
                    }

                    if(_executionFrame != null) {
			_executionFrame.getContentPane().removeAll();
                    } else {
                        _executionFrame = new JFrame();
                    }

                    ModelPane modelPane = new ModelPane(toplevel);
                    _executionFrame.getContentPane().add(modelPane,
                            BorderLayout.NORTH);
                    // Create a panel to place placeable objects.
                    JPanel displayPanel = new JPanel();
                    displayPanel.setLayout(new BoxLayout(displayPanel,
                            BoxLayout.Y_AXIS));
                    modelPane.setDisplayPane(displayPanel);

                    // Put placeable objects in a reasonable place
                    for(Iterator i = toplevel.deepEntityList().iterator();
                        i.hasNext();) {
                        Object o = i.next();
                        if(o instanceof Placeable) {
                            ((Placeable) o).place(
                                    displayPanel);
                        }
                    }

                    if(_executionFrame != null) {
                        _executionFrame.setVisible(true);
                    }

                    //                    manager.startRun();

		    final JFrame packframe = _executionFrame;
		    Action packer = new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
			    packframe.getContentPane().doLayout();
			    packframe.repaint();
			    packframe.pack();
			}
		    };
		    javax.swing.Timer timer =
                        new javax.swing.Timer(200, packer);
		    timer.setRepeats(false);
		    timer.start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new GraphException(ex.getMessage());
                }

            }
        };
	_application.addMenuItem(menuDevel, action, 'E', "Execute System");

	JToolBar tb = new JToolBar();
	Container pane = 
	    ((DesktopFrame)_application.getApplicationFrame()).getToolBarPane();
	pane.add(tb);

	String dflt = "";
        // Layout combobox
        _layoutComboBox = new JComboBox();
        dflt = "Random layout";
        _layoutComboBox.addItem(dflt);
        _layoutComboBox.addItem("Levelized layout");
        _layoutComboBox.setSelectedItem(dflt);
        _layoutComboBox.setMaximumSize(_layoutComboBox.getMinimumSize());
        _layoutComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    VergilDocument d = (VergilDocument)
			_application.getCurrentDocument();
                    JGraph jg = (JGraph) _application.getView(d);
                    _application.redoLayout(jg, (String) e.getItem());
                }
            }
        });
	
	tb.add(_layoutComboBox);

	//FIXME find these names somehow.
	_directorComboBox = new JComboBox();
	dflt = "sdf.director";
        _directorComboBox.addItem(dflt);
        _directorComboBox.setSelectedItem(dflt);
        _directorComboBox.setMaximumSize(_directorComboBox.getMinimumSize());
        _directorComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // FIXME do something.
                }
            }
        });
        tb.add(_directorComboBox);
    }

    /**
     * An execution listener that updates the message bar.
     */
    public class VergilExecutionListener implements ExecutionListener {
	public void executionError(Manager manager, Exception exception) {
	    _application.showError(manager.getName(), exception);
	}

	public void executionFinished(Manager manager) {
	}

	public void managerStateChanged(Manager manager) {
	    DesktopFrame frame = (DesktopFrame) 
		_application.getApplicationFrame();
	    JStatusBar statusBar = frame.getStatusBar();
	    statusBar.setMessage(manager.getState().getDescription());
	}
    }

    /** The application that this package is associated with.
     */
    private VergilApplication _application;

    /** The frame in which any placeable objects create their output.
     *  This will be null until a model with something placeable is
     *  executed.
     */
    private JFrame _executionFrame = null;

    /** The director selection combobox
     */
    private JComboBox _directorComboBox;

    /** The layout selection combobox
     */
    private JComboBox _layoutComboBox;
}
