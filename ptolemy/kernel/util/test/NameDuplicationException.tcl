# Tests for the NameDuplicationException class
#
# @Author: Edward A. Lee
#
# @Version: $Id$
#
# @Copyright (c) 1997 The Regents of the University of California.
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
# 						PT_COPYRIGHT_VERSION_2
# 						COPYRIGHTENDKEY
#######################################################################

# Tycho test bed, see $TYCHO/doc/coding/testing.html for more information.

# Load up the test definitions.
if {[string compare test [info procs test]] == 1} then { 
    source testDefs.tcl
} {}

# Uncomment this to get a full report, or set in your Tcl shell window.
# set VERBOSE 1

# If a file contains non-graphical tests, then it should be named .tcl
# If a file contains graphical tests, then it should be called .itcl
# 
# It would be nice if the tests would work in a vanilla itkwish binary.
# Check for necessary classes and adjust the auto_path accordingly.
#

######################################################################
####
# 
test NameDuplicationException-1.1 \
        {Get information about an instance of NameDuplicationException} {
    # If anything changes, we want to know about it so we can write tests.
    set obj [java::new pt.kernel.NamedObj "object"]
    set n [java::new pt.kernel.NameDuplicationException $obj]
    list [getJavaInfo $n]
} {{
  class:         pt.kernel.NameDuplicationException
  fields:        
  methods:       {equals java.lang.Object} fillInStackTrace getClass get
    LocalizedMessage getMessage hashCode notify notifyAll p
    rintStackTrace {printStackTrace java.io.PrintStream} {p
    rintStackTrace java.io.PrintWriter} toString wait {wait
     long} {wait long int}
    
  constructors:  {pt.kernel.NameDuplicationException pt.kernel.Nameable}
     {pt.kernel.NameDuplicationException pt.kernel.Nameable
     java.lang.String} {pt.kernel.NameDuplicationException 
    pt.kernel.Nameable pt.kernel.Nameable} {pt.kernel.NameD
    uplicationException pt.kernel.Nameable pt.kernel.Nameab
    le java.lang.String}
    
  properties:    class localizedMessage message
    
  superclass:    pt.kernel.KernelException
    
}}

######################################################################
####
# 
test NameDuplicationException-2.1 {One named objects} {
    set containee [java::new pt.kernel.Port]
    $containee setName "wouldBeContainee"
    set pe [java::new {pt.kernel.NameDuplicationException \
            pt.kernel.Nameable} $containee]
    list [$pe getMessage] [$pe getLocalizedMessage]
} {{Attempt to insert object named "wouldBeContainee" into a container that already contains an object with that name.} {Attempt to insert object named "wouldBeContainee" into a container that already contains an object with that name.}}

######################################################################
####
# 
test NameDuplicationException-2.2 {One named object and one string} {
    set containee [java::new pt.kernel.Port]
    $containee setName "wouldBeContainee"
    set pe [java::new {pt.kernel.NameDuplicationException \
            pt.kernel.Nameable String} $containee {more info}]
    list [$pe getMessage] [$pe getLocalizedMessage]
} {{Attempt to insert object named "wouldBeContainee" into a container that already contains an object with that name. more info} {Attempt to insert object named "wouldBeContainee" into a container that already contains an object with that name. more info}}

######################################################################
####
# 
test NameDuplicationException-2.3 {Two named objects arguments} {
    set container [java::new pt.kernel.Entity "container"]
    set containee [java::new pt.kernel.Port]
    $containee setName "wouldBeContainee"
    set pe [java::new {pt.kernel.NameDuplicationException \
            pt.kernel.Nameable pt.kernel.Nameable} $container $containee]
    list [$pe getMessage] [$pe getLocalizedMessage]
} {{Attempt to insert object named "wouldBeContainee" into container named ".container", which already contains an object with that name.} {Attempt to insert object named "wouldBeContainee" into container named ".container", which already contains an object with that name.}}

######################################################################
####
# 
test NameDuplicationException-2.4 {two objects and a string} {
    set container [java::new pt.kernel.Entity "container"]
    set containee [java::new pt.kernel.Port]
    $containee setName "wouldBeContainee"
    set pe [java::new pt.kernel.NameDuplicationException \
            $container $containee "more info" ]
    list [$pe getMessage] [$pe getLocalizedMessage]
} {{Attempt to insert object named "wouldBeContainee" into container named ".container", which already contains an object with that name. more info} {Attempt to insert object named "wouldBeContainee" into container named ".container", which already contains an object with that name. more info}}
