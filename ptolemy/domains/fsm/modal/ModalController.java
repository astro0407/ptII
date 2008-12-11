/* Controller for modal models.

 Copyright (c) 1999-2005 The Regents of the University of California.
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
 */
package ptolemy.domains.fsm.modal;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import ptolemy.actor.TypedActor;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.domains.fsm.kernel.FSMActor;
import ptolemy.domains.fsm.kernel.Group;
import ptolemy.domains.fsm.kernel.RefinementActor;
import ptolemy.domains.fsm.kernel.State;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Entity;
import ptolemy.kernel.Port;
import ptolemy.kernel.Relation;
import ptolemy.kernel.util.DropTargetHandler;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.KernelException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.MoMLChangeRequest;

// NOTE: This class duplicates code in Refinement, but
// because of the inheritance hierarchy, there appears to be no convenient
// way to share the code.
//////////////////////////////////////////////////////////////////////////
//// ModalController

/**
 This FSM actor supports mirroring of its ports in its container
 (which is required to be a ModalModel), which in turn assures
 mirroring of ports in each of the refinements.
 <p>
 Note that this actor has no attributes of its own.
 Requests for attributes are delegated to the container.

 @author Edward A. Lee
 @version $Id$
 @since Ptolemy II 2.0
 @Pt.ProposedRating Red (eal)
 @Pt.AcceptedRating Red (reviewmoderator)
 */
public class ModalController extends FSMActor implements DropTargetHandler,
        RefinementActor {

    /** Construct a modal controller with a name and a container.
     *  The container argument must not be null, or a
     *  NullPointerException will be thrown.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the container is incompatible
     *   with this actor.
     *  @exception NameDuplicationException If the name coincides with
     *   an actor already in the container.
     */
    public ModalController(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        new ContainmentExtender(this, "_containmentExtender");
    }

    /** Construct a modal controller in the specified workspace with
     *  no container and an empty string as a name. You can then change
     *  the name with setName(). If the workspace argument is null, then
     *  use the default workspace.
     *  @param workspace The workspace that will list the actor.
     */
    public ModalController(Workspace workspace) {
        super(workspace);

        try {
        	new ContainmentExtender(this, "_containmentExtender");
        } catch (KernelException e) {
            // This should never happen.
            throw new InternalErrorException("Constructor error "
                    + e.getMessage());
        }
    }

    /** React to a list of objects being dropped onto a target.
     *
     *  @param target The target on which the objects are dropped.
     *  @param dropObjects The list of objects dropped onto the target.
     *  @param moml The moml string generated for the dropped objects.
     *  @exception IllegalActionException If the handling is unsuccessful.
     */
    public void dropObject(NamedObj target, List dropObjects, String moml)
            throws IllegalActionException {
        boolean merge = false;
        if (target instanceof State) {
            State state = (State) target;

            TreeMap<Class<? extends Entity>, String> map =
                _getRefinementClasses();
            String refinementClass = null;
            boolean conflict = false;
            RefinementSuggestion suggestion =
                (RefinementSuggestion) target.getAttribute(
                        "refinementSuggestion");
            for (Object dropObject : dropObjects) {
                if (suggestion != null) {
                    String className = suggestion.getRefinementClass(
                            (NamedObj) dropObject);
                    if (refinementClass == null) {
                        refinementClass = className;
                    } else if (!refinementClass.equals(className)) {
                        conflict = true;
                    }
                } else {
                    for (Class<? extends Entity> keyClass : map.keySet()) {
                        if (keyClass.isInstance(dropObject)) {
                            String value = map.get(keyClass);
                            if (refinementClass == null) {
                                refinementClass = value;
                                break;
                            } else if (!refinementClass.equals(value)) {
                                conflict = true;
                                break;
                            }
                        }
                    }
                }
                if (conflict) {
                    break;
                }
            }

            if (conflict || refinementClass == null) {
                throw new IllegalActionException(this, "Unable to determine " +
                        "the type of all the dropped objects.");
            }

            TypedActor[] refinements = state.getRefinement();
            TypedActor refinement = null;
            if (refinements != null) {
                for (TypedActor actor : refinements) {
                    if (((NamedObj) actor).getClassName().equals(
                            refinementClass)) {
                        refinement = actor;
                        break;
                    }
                }
            }
            if (refinement == null) {
                CompositeEntity containerContainer =
                    (CompositeEntity) state.getContainer().getContainer();
                String name = containerContainer.uniqueName(state.getName());
                Refinement.addRefinement(state, name, null, refinementClass,
                        null);
                merge = true;
                refinement = (TypedActor) containerContainer.getEntity(name);
            }
            target = (NamedObj)refinement;
        }
        if (dropObjects.size() == 1 && dropObjects.get(0) instanceof Group) {
            moml = "<group>" + moml + "</group>";
        } else {
            moml = "<group name=\"auto\">" + moml + "</group>";
        }
        MoMLChangeRequest request = new MoMLChangeRequest(this, target, moml);
        request.setUndoable(true);
        if (merge) {
            request.setMergeWithPreviousUndo(true);
        }
        target.requestChange(request);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Get the state in any ModalController within this ModalModel that has
     *  this ModalController as its refinement, if any. Return null if no such
     *  state is found.
     *
     *  @return The state with this ModalController as its refinement, or null.
     *  @exception IllegalActionException If the specified refinement cannot be
     *   found in a state, or if a comma-separated list is malformed.
     */
    public State getRefinedState() throws IllegalActionException {
        NamedObj container = getContainer();
        if (container instanceof ModalModel) {
            List<?> controllers = ((ModalModel) container).entityList(
                    ModalController.class);
            for (Object controllerObject : controllers) {
                ModalController controller = (ModalController) controllerObject;
                List<?> states = controller.entityList(State.class);
                for (Object stateObject : states) {
                    State state = (State) stateObject;
                    TypedActor[] refinements = state.getRefinement();
                    if (refinements != null) {
                        for (TypedActor refinement : refinements) {
                            if (refinement == this) {
                                return state;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Create a new port with the specified name in the container of
     *  this controller, which in turn creates a port in this controller
     *  and all the refinements.
     *  This method is write-synchronized on the workspace.
     *  @param name The name to assign to the newly created port.
     *  @return The new port.
     *  @exception NameDuplicationException If the entity already has a port
     *   with the specified name.
     */
    public Port newPort(String name) throws NameDuplicationException {
        try {
            _workspace.getWriteAccess();

            if (_mirrorDisable || (getContainer() == null)) {
                // Have already called the super class.
                // This time, process the request.
                RefinementPort port = new RefinementPort(this, name);

                // NOTE: Changed RefinementPort so mirroring
                // is enabled by default. This means mirroring
                // will occur during MoML parsing, but this
                // should be harmless.  EAL 12/04.
                // port._mirrorDisable = false;
                // Create the appropriate links.
                ModalModel container = (ModalModel) getContainer();

                if (container != null) {
                    String relationName = name + "Relation";
                    Relation relation = container.getRelation(relationName);

                    if (relation == null) {
                        relation = container.newRelation(relationName);

                        Port containerPort = container.getPort(name);
                        containerPort.link(relation);
                    }

                    port.link(relation);
                }

                return port;
            } else {
                _mirrorDisable = true;
                ((ModalModel) getContainer()).newPort(name);
                return getPort(name);
            }
        } catch (IllegalActionException ex) {
            // This exception should not occur, so we throw a runtime
            // exception.
            throw new InternalErrorException(
                    "ModalController.newPort: Internal error: "
                            + ex.getMessage());
        } finally {
            _mirrorDisable = false;
            _workspace.doneWriting();
        }
    }

    /** Control whether adding a port should be mirrored in the modal
     *  model and refinements.
     *  This is added to allow control by the UI.
     *  @param disable True if mirroring should not occur.
     */
    public void setMirrorDisable(boolean disable) {
        _mirrorDisable = disable;
    }

    /** Override the base class to ensure that the proposed container
     *  is a ModalModel or null.
     *  @param container The proposed container.
     *  @exception IllegalActionException If the proposed container is not a
     *   TypedActor, or if the base class throws it.
     */
    protected void _checkContainer(Entity container)
            throws IllegalActionException {
        if (!(container instanceof ModalModel) && (container != null)) {
            throw new IllegalActionException(container, this,
                    "ModalController can only be contained by "
                            + "ModalModel objects.");
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Return a map from the classes of the entities to be dropped into a state
     *  and the class names of the refinements that can be used to contain those
     *  entities.
     *
     *  @return The map.
     */
    protected TreeMap<Class<? extends Entity>, String> _getRefinementClasses() {
        TreeMap map = new TreeMap<Class<? extends Entity>, String>(
                new ClassComparator());
        map.put(State.class, ModalController.class.getName());
        map.put(ComponentEntity.class, TypedCompositeActor.class.getName());
        return map;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected variables               ////

    /** Indicator that we are processing a newPort request. */
    protected boolean _mirrorDisable = false;

    //////////////////////////////////////////////////////////////////////////
    //// ClassComparator

    /**
     A comparator to compare classes, which is used to sort the map returned by
     {@link #_getRefinementClasses()}.

     @author Thomas Huining Feng
     @version $Id$
     @since Ptolemy II 7.1
     @Pt.ProposedRating Red (tfeng)
     @Pt.AcceptedRating Red (tfeng)
     */
    private static class ClassComparator
            implements Comparator<Class<? extends Entity>> {

        /** Compare class1 and class2, and return -1 if class1 is a subclass of
         *  class2, 1 if class2 is a subclass of class1, and otherwise, the
         *  result of comparing the names of class1 and class2.
         *
         *  @param class1 The first class.
         *  @param class2 The second class.
         *  @return -1, 0, or 1 representing whether class1 is less than, equal
         *   to, or greater than class2.
         */
        public int compare(Class<? extends Entity> class1,
                Class<? extends Entity> class2) {
            if (!class1.equals(class2)) {
                if (class1.isAssignableFrom(class2)) {
                    return 1;
                } else if (class2.isAssignableFrom(class1)) {
                    return -1;
                }
            }
            return class1.getName().compareTo(class2.getName());
        }
    }
}
