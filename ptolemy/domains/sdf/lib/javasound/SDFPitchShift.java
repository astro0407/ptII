/* An actor that performs pitch shifting on an audio signal.

 Copyright (c) 1998-1999 The Regents of the University of California.
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

@ProposedRating Red (vogel@eecs.berkeley.edu)
@AcceptedRating
*/

package ptolemy.domains.sdf.lib.javasound;

import ptolemy.domains.sdf.test.pitchshift.*;
import ptolemy.math.*;
import ptolemy.math.SignalProcessing;
import ptolemy.actor.*;
import ptolemy.actor.lib.*;
import ptolemy.kernel.util.*;
import ptolemy.data.*;
import ptolemy.data.expr.Parameter;
import ptolemy.graph.Inequality;

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import collections.LinkedList;

//import ptolemy.media.*;
import javax.media.sound.sampled.*;

import ptolemy.domains.sdf.kernel.*;

//////////////////////////////////////////////////////////////////////////
//// SDFPitchShift
/**


@author Brian K. Vogel
@version
*/

public class SDFPitchShift extends SDFAtomicActor {

    /** 
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the actor cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public SDFPitchShift(TypedCompositeActor container, String name)
            throws NameDuplicationException, IllegalActionException  {
        super(container, name);
	consumptionProductionRate = new Parameter(this,
					"consumptionProductionRate",
						 new IntToken(512));
	consumptionProductionRate.setTypeEquals(IntToken.class);
	consumptionRate =
	    ((IntToken)consumptionProductionRate.getToken()).intValue();
	productionRate = consumptionRate;

	output = new SDFIOPort(this, "output", false, true);
        output.setTypeEquals(DoubleToken.class);
	output.setTokenProductionRate(productionRate);

	input = new SDFIOPort(this, "input", true, false);
        input.setTypeEquals(DoubleToken.class);
	input.setTokenConsumptionRate(consumptionRate);

	pitchIn = new SDFIOPort(this, "pitchIn", true, false);
        pitchIn.setTypeEquals(DoubleToken.class);
	pitchIn.setTokenConsumptionRate(consumptionRate);
		
	sampleRate = new Parameter(this, "sampleRate",
				     new DoubleToken(22050));
        sampleRate.setTypeEquals(DoubleToken.class);
	
	
    }

    ///////////////////////////////////////////////////////////////////
    ////                     ports and parameters                  ////

    /** The input port. */
    public SDFIOPort input;

    /** The pitch value input port. */
    public SDFIOPort pitchIn;

    /** The output port. */
    public SDFIOPort output;

     /** The sampling rate to use, in Hz. The default vaule is
      * 22050.
     */
    public Parameter sampleRate;

    /** Set the token consumption rate and production rate to use (they
     * are equal). This only effects execution speed. Large values may result
     * in faster execution at the expense of larger channel buffers.
     * The default value is 512.
     */
    public Parameter consumptionProductionRate;

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Notify the director when type changes in the parameters occur.
     *  This will cause type resolution to be redone at the next opportunity.
     *  It is assumed that type changes in the parameters are implemented
     *  by the director's change request mechanism, so they are implemented
     *  when it is safe to redo type resolution.
     *  If there is no director, then do nothing.
     */
    public void attributeTypeChanged(Attribute attribute) {
        Director dir = getDirector();
        if (dir != null) {
            dir.invalidateResolvedTypes();
        }
    }

    /** Clone the actor into the specified workspace. This calls the
     *  base class and then sets the <code>isPeriodic</code> and <code>pathName</code>
     *  public members to the parameters of the new actor.
     *  @param ws The workspace for the new object.
     *  @return A new actor.
     */
    public Object clone(Workspace ws) {
	 try {
	     SDFPitchShift newobj = (SDFPitchShift)super.clone(ws);
	     newobj.output = (SDFIOPort)newobj.getPort("output");
	     newobj.input = (SDFIOPort)newobj.getPort("input");
	     newobj.pitchIn = (SDFIOPort)newobj.getPort("pitchIn");
	     newobj.sampleRate = (Parameter)newobj.getAttribute("sampleRate");
	     newobj.consumptionProductionRate = (Parameter)newobj.getAttribute("consumptionProductionRate");
	     // set the type constraints.
	     return newobj;
	 } catch (CloneNotSupportedException ex) {
	     // Errors should not occur here...
	     throw new InternalErrorException(
			 "Clone failed: " + ex.getMessage());
	 }
    }

    /** Output the sample value of the sound file corresponding to the
     *  current index.
     */
    public void fire() throws IllegalActionException {
        

        input.getArray(0, audioTokenArray);
	pitchIn.getArray(0, pitchTokenArray);
	
        int i;
        for (i = 0; i < consumptionRate; i++) {
            audioInDoubleArray[i] = audioTokenArray[i].doubleValue();
        }
	for (i = 0; i < consumptionRate; i++) {
	    //System.out.println("pitchTokenArray.length = " + pitchTokenArray.length);        
            pitchInDoubleArray[i] = pitchTokenArray[i].doubleValue();
        }
	// Default pitch scale factor(s). FIXME: should get this
	// from an input port.
	double pitchScaleIn = 1.0;

	
	audioOutDoubleArray = ps.performPitchShift(audioInDoubleArray,
				      pitchInDoubleArray, pitchScaleIn);

	// Convert to DoubleToken[].
	for (i = 0; i < productionRate; i++) {
	    audioTokenArray[i] = new DoubleToken(audioOutDoubleArray[i]);
	}
	output.sendArray(0, audioTokenArray);
    }

    /** Read in the sound file specified by the <i>pathName</i> parameter
     *  and initialize the current sample index to 0.
     *  @exception IllegalActionException If the parent class throws it.
     */
    public void initialize() throws IllegalActionException {
        super.initialize();

	sampRate = ((DoubleToken)sampleRate.getToken()).doubleValue();
	
	ps = new PitchShift((float)sampRate);
	    
	audioTokenArray = new DoubleToken[consumptionRate];
	pitchTokenArray = new DoubleToken[consumptionRate];
	audioInDoubleArray = new double[consumptionRate];
	pitchInDoubleArray = new double[consumptionRate];
	audioOutDoubleArray = new double[consumptionRate];
    }


    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   //// 

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////
    
    private double sampRate;

    private PitchShift ps;

    private int productionRate;
    private int consumptionRate;

    private DoubleToken[] audioTokenArray;
    private DoubleToken[] pitchTokenArray;

    private double[] audioInDoubleArray;
    private double[] pitchInDoubleArray;

    private double[] audioOutDoubleArray;
}
