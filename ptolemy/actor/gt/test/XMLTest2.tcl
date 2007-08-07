# Test DepthFirstTransformer.
#
# @Author: Thomas Huining Feng
#
# @Version: $Id$
#
# @Copyright (c) 1997-2005 The Regents of the University of California.
# All rights reserved.
#
# Permission is hereby granted, without written agreement and without
# license or royalty fees, to use, copy, modify, and distribute this
# software and its documentation for any purpose, provided that the
# above copyright notice and the following two paragraphs appear in all
# copies of this software.
#
# IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
# FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
# ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
# THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
#
# THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
# INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
# PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
# CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
# ENHANCEMENTS, OR MODIFICATIONS.
#
#                       PT_COPYRIGHT_VERSION_2
#                       COPYRIGHTENDKEY
#######################################################################

# Ptolemy II test bed, see $PTII/doc/coding/testing.html for more information.

# Load up the test definitions.
if {[string compare test [info procs test]] == 1} then {
    source testDefs.tcl
} {}

######################################################################
####
#
test XMLTest-2.1 {Test rule2.xml with host2.1.xml} {
    set matchResult [java::call ptolemy.actor.gt.RecursiveGraphMatcher match rule2.xml host2.1.xml]
    [$matchResult getMatchResult] toString
} {{}}

test XMLTest-2.2 {Test rule2.xml with host2.2.xml} {
    set matchResult [java::call ptolemy.actor.gt.RecursiveGraphMatcher match rule2.xml host2.2.xml]
    [$matchResult getMatchResult] toString
} {{.rule2.Left Hand Side.AtomicActorMatcher.output:[.rule2.Left Hand Side.relation, .rule2.Left Hand Side.AtomicActorMatcher.output] = .host2.Const.output:[.host2.relation, .host2.Const.output], .rule2.Left Hand Side.AtomicActorMatcher.output:[.rule2.Left Hand Side.relation, .rule2.Left Hand Side.CompositeActor.port] = .host2.Const.output:[.host2.relation, .host2.CompositeActor.port], .rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input:[.rule2.Left Hand Side.CompositeActor.relation, .rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input] = .host2.CompositeActor.Display.input:[.host2.CompositeActor.relation, .host2.CompositeActor.Display.input], .rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input:[.rule2.Left Hand Side.CompositeActor.relation, .rule2.Left Hand Side.CompositeActor.port] = .host2.CompositeActor.Display.input:[.host2.CompositeActor.relation, .host2.CompositeActor.port], .rule2.Left Hand Side.CompositeActor.port:[.rule2.Left Hand Side.CompositeActor.relation, .rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input] = .host2.CompositeActor.port:[.host2.CompositeActor.relation, .host2.CompositeActor.Display.input], .rule2.Left Hand Side.CompositeActor.port:[.rule2.Left Hand Side.relation, .rule2.Left Hand Side.AtomicActorMatcher.output] = .host2.CompositeActor.port:[.host2.relation, .host2.Const.output], .rule2.Left Hand Side.CompositeActor.port:[.rule2.Left Hand Side.relation, .rule2.Left Hand Side.CompositeActor.port] = .host2.CompositeActor.port:[.host2.relation, .host2.CompositeActor.port], ptolemy.actor.TypedCompositeActor {.rule2.Left Hand Side.CompositeActor} = ptolemy.actor.TypedCompositeActor {.host2.CompositeActor}, ptolemy.actor.TypedIOPort {.rule2.Left Hand Side.AtomicActorMatcher.output} = ptolemy.actor.TypedIOPort {.host2.Const.output}, ptolemy.actor.TypedIOPort {.rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input} = ptolemy.actor.TypedIOPort {.host2.CompositeActor.Display.input}, ptolemy.actor.TypedIOPort {.rule2.Left Hand Side.CompositeActor.port} = ptolemy.actor.TypedIOPort {.host2.CompositeActor.port}, ptolemy.actor.gt.AtomicActorMatcher {.rule2.Left Hand Side.AtomicActorMatcher} = ptolemy.actor.lib.Const {.host2.Const}, ptolemy.actor.gt.AtomicActorMatcher {.rule2.Left Hand Side.CompositeActor.AtomicActorMatcher} = ptolemy.actor.lib.gui.Display {.host2.CompositeActor.Display}, ptolemy.actor.gt.CompositeActorMatcher {.rule2.Left Hand Side} = ptolemy.actor.TypedCompositeActor {.host2}, ptolemy.domains.sdf.kernel.SDFDirector {.rule2.Left Hand Side.CompositeActor.SDF Director} = ptolemy.domains.sdf.kernel.SDFDirector {.host2.CompositeActor.SDF Director}}}

test XMLTest-2.3 {Test rule2.xml with host2.2.xml} {
    set matchResult [java::call ptolemy.actor.gt.RecursiveGraphMatcher match rule2.xml host2.3.xml]
    [$matchResult getMatchResult] toString
} {{}}

test XMLTest-2.4 {Test rule2.xml with host2.2.xml} {
    set matchResult [java::call ptolemy.actor.gt.RecursiveGraphMatcher match rule2.xml host2.4.xml]
    [$matchResult getMatchResult] toString
} {{.rule2.Left Hand Side.AtomicActorMatcher.output:[.rule2.Left Hand Side.relation, .rule2.Left Hand Side.AtomicActorMatcher.output] = .host2.CompositeActor2.Const.output:[.host2.CompositeActor2.relation, .host2.CompositeActor2.Const.output], .rule2.Left Hand Side.AtomicActorMatcher.output:[.rule2.Left Hand Side.relation, .rule2.Left Hand Side.CompositeActor.port] = .host2.CompositeActor2.Const.output:[.host2.CompositeActor2.relation, .host2.CompositeActor2.port, .host2.relation, .host2.CompositeActor.port], .rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input:[.rule2.Left Hand Side.CompositeActor.relation, .rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input] = .host2.CompositeActor.Display.input:[.host2.CompositeActor.relation, .host2.CompositeActor.Display.input], .rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input:[.rule2.Left Hand Side.CompositeActor.relation, .rule2.Left Hand Side.CompositeActor.port] = .host2.CompositeActor.Display.input:[.host2.CompositeActor.relation, .host2.CompositeActor.port], .rule2.Left Hand Side.CompositeActor.port:[.rule2.Left Hand Side.CompositeActor.relation, .rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input] = .host2.CompositeActor.port:[.host2.CompositeActor.relation, .host2.CompositeActor.Display.input], .rule2.Left Hand Side.CompositeActor.port:[.rule2.Left Hand Side.relation, .rule2.Left Hand Side.AtomicActorMatcher.output] = .host2.CompositeActor.port:[.host2.relation, .host2.CompositeActor2.port, .host2.relation, .host2.CompositeActor2.Const.output], .rule2.Left Hand Side.CompositeActor.port:[.rule2.Left Hand Side.relation, .rule2.Left Hand Side.CompositeActor.port] = .host2.CompositeActor.port:[.host2.relation, .host2.CompositeActor.port], ptolemy.actor.TypedCompositeActor {.rule2.Left Hand Side.CompositeActor} = ptolemy.actor.TypedCompositeActor {.host2.CompositeActor}, ptolemy.actor.TypedIOPort {.rule2.Left Hand Side.AtomicActorMatcher.output} = ptolemy.actor.TypedIOPort {.host2.CompositeActor2.Const.output}, ptolemy.actor.TypedIOPort {.rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input} = ptolemy.actor.TypedIOPort {.host2.CompositeActor.Display.input}, ptolemy.actor.TypedIOPort {.rule2.Left Hand Side.CompositeActor.port} = ptolemy.actor.TypedIOPort {.host2.CompositeActor.port}, ptolemy.actor.gt.AtomicActorMatcher {.rule2.Left Hand Side.AtomicActorMatcher} = ptolemy.actor.lib.Const {.host2.CompositeActor2.Const}, ptolemy.actor.gt.AtomicActorMatcher {.rule2.Left Hand Side.CompositeActor.AtomicActorMatcher} = ptolemy.actor.lib.gui.Display {.host2.CompositeActor.Display}, ptolemy.actor.gt.CompositeActorMatcher {.rule2.Left Hand Side} = ptolemy.actor.TypedCompositeActor {.host2}, ptolemy.domains.sdf.kernel.SDFDirector {.rule2.Left Hand Side.CompositeActor.SDF Director} = ptolemy.domains.sdf.kernel.SDFDirector {.host2.CompositeActor.SDF Director}}}

test XMLTest-2.5 {Test rule2.xml with host2.2.xml} {
    set matchResult [java::call ptolemy.actor.gt.RecursiveGraphMatcher match rule2.xml host2.5.xml]
    [$matchResult getMatchResult] toString
} {{.rule2.Left Hand Side.AtomicActorMatcher.output:[.rule2.Left Hand Side.relation, .rule2.Left Hand Side.AtomicActorMatcher.output] = .host2.CompositeActor2.CompositeActor.Const.output:[.host2.CompositeActor2.CompositeActor.relation, .host2.CompositeActor2.CompositeActor.Const.output], .rule2.Left Hand Side.AtomicActorMatcher.output:[.rule2.Left Hand Side.relation, .rule2.Left Hand Side.CompositeActor.port] = .host2.CompositeActor2.CompositeActor.Const.output:[.host2.CompositeActor2.CompositeActor.relation, .host2.CompositeActor2.CompositeActor.port, .host2.CompositeActor2.relation, .host2.CompositeActor2.port, .host2.relation, .host2.CompositeActor.port], .rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input:[.rule2.Left Hand Side.CompositeActor.relation, .rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input] = .host2.CompositeActor.CompositeActor.CompositeActor2.Display.input:[.host2.CompositeActor.CompositeActor.CompositeActor2.relation, .host2.CompositeActor.CompositeActor.CompositeActor2.Display.input], .rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input:[.rule2.Left Hand Side.CompositeActor.relation, .rule2.Left Hand Side.CompositeActor.port] = .host2.CompositeActor.CompositeActor.CompositeActor2.Display.input:[.host2.CompositeActor.CompositeActor.CompositeActor2.relation, .host2.CompositeActor.CompositeActor.CompositeActor2.port, .host2.CompositeActor.CompositeActor.relation2, .host2.CompositeActor.CompositeActor.CompositeActor.port2, .host2.CompositeActor.CompositeActor.relation2, .host2.CompositeActor.CompositeActor.CompositeActor.port, .host2.CompositeActor.CompositeActor.relation, .host2.CompositeActor.CompositeActor.port, .host2.CompositeActor.relation2, .host2.CompositeActor.port], .rule2.Left Hand Side.CompositeActor.port:[.rule2.Left Hand Side.CompositeActor.relation, .rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input] = .host2.CompositeActor.port:[.host2.CompositeActor.relation2, .host2.CompositeActor.CompositeActor.port, .host2.CompositeActor.relation2, .host2.CompositeActor.CompositeActor.CompositeActor.port, .host2.CompositeActor.CompositeActor.relation, .host2.CompositeActor.CompositeActor.CompositeActor.port2, .host2.CompositeActor.CompositeActor.relation2, .host2.CompositeActor.CompositeActor.CompositeActor2.port, .host2.CompositeActor.CompositeActor.relation2, .host2.CompositeActor.CompositeActor.CompositeActor2.Display.input], .rule2.Left Hand Side.CompositeActor.port:[.rule2.Left Hand Side.relation, .rule2.Left Hand Side.AtomicActorMatcher.output] = .host2.CompositeActor.port:[.host2.relation, .host2.CompositeActor2.port, .host2.relation, .host2.CompositeActor2.CompositeActor.port, .host2.CompositeActor2.relation, .host2.CompositeActor2.CompositeActor.Const.output], .rule2.Left Hand Side.CompositeActor.port:[.rule2.Left Hand Side.relation, .rule2.Left Hand Side.CompositeActor.port] = .host2.CompositeActor.port:[.host2.relation, .host2.CompositeActor.port], ptolemy.actor.TypedCompositeActor {.rule2.Left Hand Side.CompositeActor} = ptolemy.actor.TypedCompositeActor {.host2.CompositeActor}, ptolemy.actor.TypedIOPort {.rule2.Left Hand Side.AtomicActorMatcher.output} = ptolemy.actor.TypedIOPort {.host2.CompositeActor2.CompositeActor.Const.output}, ptolemy.actor.TypedIOPort {.rule2.Left Hand Side.CompositeActor.AtomicActorMatcher.input} = ptolemy.actor.TypedIOPort {.host2.CompositeActor.CompositeActor.CompositeActor2.Display.input}, ptolemy.actor.TypedIOPort {.rule2.Left Hand Side.CompositeActor.port} = ptolemy.actor.TypedIOPort {.host2.CompositeActor.port}, ptolemy.actor.gt.AtomicActorMatcher {.rule2.Left Hand Side.AtomicActorMatcher} = ptolemy.actor.lib.Const {.host2.CompositeActor2.CompositeActor.Const}, ptolemy.actor.gt.AtomicActorMatcher {.rule2.Left Hand Side.CompositeActor.AtomicActorMatcher} = ptolemy.actor.lib.gui.Display {.host2.CompositeActor.CompositeActor.CompositeActor2.Display}, ptolemy.actor.gt.CompositeActorMatcher {.rule2.Left Hand Side} = ptolemy.actor.TypedCompositeActor {.host2}, ptolemy.domains.sdf.kernel.SDFDirector {.rule2.Left Hand Side.CompositeActor.SDF Director} = ptolemy.domains.sdf.kernel.SDFDirector {.host2.CompositeActor.SDF Director}}}
