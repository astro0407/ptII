/* A Scheduler for the SDF domain

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

@ProposedRating Red (neuendor@eecs.berkeley.edu)
@AcceptedRating Red (neuendor@eecs.berkeley.edu)
*/

package ptolemy.domains.psdf.kernel;

import java.util.*;

import ptolemy.actor.Actor;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.Director;
import ptolemy.actor.IOPort;
import ptolemy.actor.IORelation;
import ptolemy.actor.Receiver;
import ptolemy.actor.parameters.ParameterPort;
import ptolemy.actor.sched.Firing;
import ptolemy.actor.sched.NotSchedulableException;
import ptolemy.actor.sched.Schedule;
import ptolemy.actor.sched.Scheduler;
import ptolemy.actor.sched.StaticSchedulingDirector;
import ptolemy.actor.util.ConstVariableModelAnalysis;
import ptolemy.actor.util.DependencyDeclaration;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.Token;
import ptolemy.data.expr.*;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.ComponentPort;
import ptolemy.kernel.Entity;
import ptolemy.kernel.Port;
import ptolemy.kernel.Relation;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.KernelException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.ValueListener;
import ptolemy.kernel.util.Workspace;
import ptolemy.math.Fraction;

///////////////////////////////////////////////////////////
//// PSDFScheduler
/**

A scheduler that implements basic scheduling of PSDF graphs.  PSDF
scheduling is similar to SDF scheduling, EXCEPT: 

<p> 1) Because parameter values may change, the solution to the
balance equation is computed symbolically.  i.e. the repetitions
vector is a function of the parameter values. 

<p> 2) Because the firing vector may change, the schedule determined
by this class can only be a quasi-static, or parameterized schedule.
Note that parameterized schedules cannot generally be constructed for
models with feedback or with unconstrained parameter values.

<p> This class uses a ConstVariableModelAnalysis to determine which
scheduling parameters are constants and which may change during
execution of the model.  Rate parameters that can change are checked
to ensure that their change context is not strictly contained by the
model being scheduled.  If this is the case, then the actor is not
locally synchronous, and cannot be statically scheduled.  Dynamic
parameters with a valid changed context are treated symbolically when
computing the repetitions vector.

<p> After computing a schedule, this scheduler determines the external
rate of each of the model's external ports.  Since the firing vector
is only computed symbolically, these rates can also only be computed
symbolically.  The dependence of these external rates on the rates of
ports in the model is declared using a DependenceDeclaration.  Higher
level directors may use this dependence information to determine the
change context of those rate variables and may refuse to schedule the
composite actor if those rates imply that this model is not locally
synchronous.

<p> FIXME: this class is not yet implemented.
@see ptolemy.actor.sched.Scheduler
@see ptolemy.domains.sdf.lib.SampleDelay
@see ptolemy.domains.sdf.kernel.SDFScheduler

@author Stephen Neuendorffer
@version $Id$
@since Ptolemy II 0.2
*/
public class PSDFScheduler extends ptolemy.domains.sdf.kernel.SDFScheduler {
    /** Construct a scheduler with no container(director)
     *  in the default workspace, the name of the scheduler is
     *  "Scheduler".
     */
    public PSDFScheduler() 
            throws IllegalActionException, NameDuplicationException {
        super();
    }

    /** Construct a scheduler in the given workspace with the name
     *  "Scheduler".
     *  If the workspace argument is null, use the default workspace.
     *  The scheduler is added to the list of objects in the workspace.
     *  Increment the version number of the workspace.
     *
     *  @param workspace Object for synchronization and version tracking.
     */
    public PSDFScheduler(Workspace workspace) 
            throws IllegalActionException, NameDuplicationException {
        super(workspace);
    }

    /** Construct a scheduler in the given container with the given name.
     *  The container argument must not be null, or a
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
    public PSDFScheduler(Director container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
    }

    // Evaluate the given parse tree in the scope of the the model
    // being scheduled, resolving "::" scoping syntax inside the
    // model.
    private Token _evaluateExpressionInModelScope(ASTPtRootNode node)
            throws IllegalActionException {
        if (_parseTreeEvaluator == null) {
            _parseTreeEvaluator = new ParseTreeEvaluator();
        }      
        if (_parserScope == null) {
            _parserScope = new ScheduleScope();
        } 
        Token result = _parseTreeEvaluator.evaluateParseTree(
                node, _parserScope);
        return result;
    }

    private class SymbolicFiring extends Firing {
        /** Construct a firing with the given actor.  The given actor
         *  is assumed to fire the number of times determined by
         *  evaluating the given expression.  Expression will probably
         *  be something like
         *  "a2::in::tokenConsumptionRate/gcd(a2::in::tokenConsumptionRate,
         *  a::out::tokenProductionRate)"
         *  @param actor The actor in the firing.
         */
        public SymbolicFiring(Actor actor, String expression)
                throws IllegalActionException {
            super(actor);
            PtParser parser = new PtParser();
            _parseTree = parser.generateParseTree(expression);
        }
        
        ///////////////////////////////////////////////////////////////////
        ////                         public methods                    ////
        
        /** Return the current iteration count of this firing.
         */
        public int getIterationCount() {
            try {
                IntToken token = (IntToken)
                    _evaluateExpressionInModelScope(_parseTree);
                return token.intValue();
            } catch (Exception ex) {
                // FIXME: this isn't very nice.
                throw new RuntimeException(ex.getMessage());
            }
        }
        private ASTPtRootNode _parseTree;
    }

    /** Scope implementation with local caching. */
    private class ScheduleScope extends ModelScope {
        
        /** Construct a scope consisting of the variables
         *  of the container of the the enclosing instance of
         *  Variable and its containers and their scope-extending
         *  attributes.
         */
        public ScheduleScope() {
        }

        /** Look up and return the attribute with the specified name in the
         *  scope. Return null if such an attribute does not exist.
         *  @return The attribute with the specified name in the scope.
         *  @exception IllegalActionException If a value in the scope
         *  exists with the given name, but cannot be evaluated.
         */
        public ptolemy.data.Token get(String name)
                throws IllegalActionException {
            NamedObj reference = (CompositeActor)
                PSDFScheduler.this.getContainer();
            Variable result;
            if(name.indexOf("::") != -1) {
                String insideName = name.replaceAll("::", ".");
                result = (Variable)reference.getAttribute(insideName);
            } else {
                result = getScopedVariable(
                        null,
                        reference,
                        name);
            }

            if (result != null) {
                return result.getToken();
            } else {
                return null;
            }
        }

        /** Look up and return the type of the attribute with the
         *  specified name in the scope. Return null if such an
         *  attribute does not exist.
         *  @return The attribute with the specified name in the scope.
         *  @exception IllegalActionException If a value in the scope
         *  exists with the given name, but cannot be evaluated.
         */
        public ptolemy.data.type.Type getType(String name)
                throws IllegalActionException {
            NamedObj reference = (CompositeActor)
                PSDFScheduler.this.getContainer();
            Variable result;
            if(name.indexOf("::") != -1) {
                String insideName = name.replaceAll("::", ".");
                result = (Variable)reference.getAttribute(insideName);
            } else {
                result = getScopedVariable(
                        null,
                        reference,
                        name);
            }

            if (result != null) {
                return result.getType();
            } else {
                return null;
            }
        }

        /** Return the list of identifiers within the scope.
         *  @return The list of variable names within the scope.
         */
        public Set identifierSet() {
            NamedObj reference = (CompositeActor)
                PSDFScheduler.this.getContainer();
            return getAllScopedVariableNames(null, reference);
        }
    }
    private ParseTreeEvaluator _parseTreeEvaluator;
    private ParserScope _parserScope;
}
