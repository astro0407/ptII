/* Director for dynamic dataflow.

 Copyright (c) 1999-2003 The Regents of the University of California.
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
@ProposedRating Red (janneck@eecs.berkeley.edu)
@AcceptedRating Red (reviewmoderator@eecs.berkeley.edu)
*/

package ptolemy.domains.ddf.kernel;


import ptolemy.actor.AbstractReceiver;
import ptolemy.actor.Actor;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.Director;
import ptolemy.actor.IOPort;
import ptolemy.actor.NoRoomException;
import ptolemy.actor.NoTokenException;
import ptolemy.actor.Receiver;
import ptolemy.data.IntToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * This director implements a variant of dynamic dataflow (DDF). Actors are fired in an unspecified order. In one
 * iteration of the model, each actor is fired at most once. The iteration ends when all remaining actors (i.e.
 * those that did not fire during the current iteration) cannot fire (i.e. they return false on prefire()).
 * <p>
 * This DDF implementation allows proper rollback, i.e. it may be repeatedly fired without intervening postfire()
 * and it restores the queues to their original state.
 *
 * @author J�rn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class DDFDirector extends Director {

    ///////////////////////////////////////////////////////////////////
    //// override:  Director                                       ////
    ///////////////////////////////////////////////////////////////////

    public void initialize() throws IllegalActionException {
        super.initialize();

        // get my contained actors
        CompositeActor container = (CompositeActor)this.getContainer();
        List entities = container.entityList();
        actors = new ArrayList();
        for (Iterator i = entities.iterator(); i.hasNext(); ) {
            Object e = i.next();
            if (e instanceof Actor) {
                actors.add(e);
            }
        }
        iterationCount = 0;
    }

    /**
     * Always return what <tt>super.prefire()</tt> returns, because an DDF model can
     * potentially always perform an iteration, regardless of the presence of input tokens.
     *
     * @return Always returns what <tt>super.prefire()</tt> returns.
     * @throws IllegalActionException If thrown by <tt>super.prefire()</tt>
     */

    public boolean prefire() throws IllegalActionException {
        reFire = false;
        return super.prefire();
    }

    public void fire() throws IllegalActionException {
        if (reFire) {
            rollbackReceivers();
        }
        reFire = true;

        List unfiredActors = new ArrayList(actors);
        firedActors = new HashSet();
        boolean hasFired = true;
        while (hasFired) {
            hasFired = false;
            for (ListIterator i = unfiredActors.listIterator(); i.hasNext(); ) {
                Actor a = (Actor)i.next();
                if (a.prefire()) {
                    a.fire();
                    i.remove();
                    firedActors.add(a);
                    hasFired = true;
                }
            }
        }
    }

    public boolean postfire() throws IllegalActionException {
        commitReceivers();
        for (Iterator i = firedActors.iterator(); i.hasNext(); ) {
            Actor a = (Actor)i.next();
            a.postfire();
        }

        iterationCount += 1;
        int iterationLimit = ((IntToken)(iterations.getToken())).intValue();
        if (iterationLimit > 0 && iterationCount >= iterationLimit) {
            iterationCount = 0;
            return false;
        }

        return super.postfire();
    }

    public Receiver newReceiver() {
        return new DDFReceiver();
    }


    ///////////////////////////////////////////////////////////////////
    ////                         DDFDirector                       ////
    ///////////////////////////////////////////////////////////////////

    public DDFDirector()
            throws IllegalActionException, NameDuplicationException {
        super();
        init();
    }

    public DDFDirector(Workspace workspace)
            throws IllegalActionException, NameDuplicationException {
        super(workspace);
        init();
    }

    public DDFDirector(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        init();
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                 ////

    /**
     * Inform the director that the specified receiver has changed its state
     * (i.e. that a token has been added to or removed from it).
     *
     * @param r The receiver.
     */

    private void notifyReceiverChange(DDFReceiver r) {
        modifiedReceivers.add(r);
    }

    /**
     * Undo all changes made to all receivers since the last time they
     * were committed.
     */

    private void rollbackReceivers() {
        for (Iterator i = modifiedReceivers.iterator(); i.hasNext(); ) {
            DDFReceiver r = (DDFReceiver)i.next();
            r.rollback();
        }
        modifiedReceivers.clear();
    }

    /**
     * Commit the changes made to all receivers instantiated by this director.
     */

    private void commitReceivers() {
        for (Iterator i = modifiedReceivers.iterator(); i.hasNext(); ) {
            DDFReceiver r = (DDFReceiver)i.next();
            r.commit();
        }
        modifiedReceivers.clear();
    }

    private void init() throws IllegalActionException, NameDuplicationException {
        iterations = new Parameter(this, "iterations", new IntToken(0));
        iterations.setTypeEquals(BaseType.INT);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         parameters                        ////


    /** A Parameter representing the number of times that postfire may be
     *  called before it returns false.  If the value is less than or
     *  equal to zero, then the execution will never return false in postfire,
     *  and thus the execution can continue forever.
     * <p>
     *  The default value is an IntToken with the value zero.
     */

    public Parameter iterations;

    ///////////////////////////////////////////////////////////////////
    ////                         private fields                    ////


    private Set modifiedReceivers = new HashSet();
    private List actors = null;

    private Collection firedActors = null;
    private boolean    reFire = false;
    private int iterationCount;

    ///////////////////////////////////////////////////////////////////
    ////                         nested & inner classes            ////



    /**
     * This receiver implements a queue that realizes a commit/rollback protocol. It notifies the
     * enclosing director if it changes its state. The director can commit the state or roll it
     * back to the last state that has been committed.
     */
    class DDFReceiver extends AbstractReceiver {

        ///////////////////////////////////////////////////////////////////
        //// implement: AbstractReceiver                               ////
        ///////////////////////////////////////////////////////////////////

        public Token get() throws NoTokenException {
            if (next >= queue.size())
                throw new NoTokenException("Attempt to read from an empty queue.");

            Token v = (Token)queue.get(next++);
            notifyReceiverChange(this);
            return v;
        }

        public boolean hasRoom() {
            return true;
        }

        public boolean hasRoom(int i) {
            return true;
        }

        public boolean hasToken() {
            return next < queue.size();
        }

        public boolean hasToken(int i) {
            return (next + i - 1) < queue.size();
        }

        public void put(Token token) throws NoRoomException {
            queue.add(token);
            added += 1;
            notifyReceiverChange(this);
        }

        ///////////////////////////////////////////////////////////////////
        ////                      DDFReceiver                          ////
        ///////////////////////////////////////////////////////////////////

        DDFReceiver() {
            super();
        }

        DDFReceiver(IOPort container) throws IllegalActionException {
            super(container);
        }

        void  rollback() {
            for (int i = 0; i < added; i++)
                queue.remove(queue.size() - 1);
            next = 0;
            added = 0;
        }

        void  commit() {
            for (int i = 0; i < next; i++)
                queue.remove(0);
            next = 0;
            added = 0;
        }

        private List queue = new ArrayList();
        private int  next = 0;
        private int  added = 0;
    }

    /**
     * This is a simplified version of the DDF receiver, which does not support rollback,
     * and which is more efficient as a result. Because it does not need access to the
     * director that instantiated it, it can be <tt>static</tt>.
     * <p>
     * Perhaps we should let the user choose whether rollback is needed?
     */

    static class SimpleDDFReceiver extends AbstractReceiver {

        ///////////////////////////////////////////////////////////////////
        //// implement: AbstractReceiver                               ////
        ///////////////////////////////////////////////////////////////////

        public Token get() throws NoTokenException {
            Token v = (Token)queue.get(0);
            queue.remove(0);
            return v;
        }

        public boolean hasRoom() {
            return true;
        }

        public boolean hasRoom(int i) {
            return true;
        }

        public boolean hasToken() {
            return 1 <= queue.size();
        }

        public boolean hasToken(int i) {
            return i <= queue.size();
        }

        public void put(Token token) throws NoRoomException {
            queue.add(token);
        }

        ///////////////////////////////////////////////////////////////////
        //// SimpleDDFReceiver                                         ////
        ///////////////////////////////////////////////////////////////////

        SimpleDDFReceiver() {
            super();
        }

        SimpleDDFReceiver(IOPort container) throws IllegalActionException {
            super(container);
        }

        private List queue = new ArrayList();
    }
}
