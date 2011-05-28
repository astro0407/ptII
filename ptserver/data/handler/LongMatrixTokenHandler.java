/*
 LongMatrixTokenHandler converts LongMatrixToken to/from byte stream
 
 Copyright (c) 2011 The Regents of the University of California.
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
package ptserver.data.handler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ptolemy.data.LongMatrixToken;
import ptolemy.data.LongToken;
import ptolemy.kernel.util.IllegalActionException;
import ptserver.data.TokenParser;

//////////////////////////////////////////////////////////////////////////
//// LongMatrixTokenHandler
/**
 * LongMatrixTokenHandler converts LongMatrixToken to/from byte stream
 * 
 * @author ishwinde
 * @version $Id $ 
 * @since Ptolemy II 8.0
 * @Pt.ProposedRating Red (ishwinde)
 * @Pt.AcceptedRating Red (ishwinde)
 * 
 */
public class LongMatrixTokenHandler extends
        AbstractTokenHandler<LongMatrixToken> {
    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /**
     * Convert LongMatrixToken to a byte stream using an algorithm defined in the DataOutputStream.
     * @throws IllegalActionException 
     * @see ptserver.data.handler.TokenHandler#convertToBytes(ptolemy.data.Token, java.io.DataOutputStream)
     */
    @Override
    public void convertToBytes(LongMatrixToken token,
            DataOutputStream outputStream) throws IOException,
            IllegalActionException {

        int rowCount = token.getRowCount();
        int colunmCount = token.getColumnCount();

        outputStream.writeInt(rowCount);
        outputStream.writeInt(colunmCount);

        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < colunmCount; column++) {

                LongToken elementToken = (LongToken) token
                        .getElementAsToken(row, column);

                TokenParser.getInstance().convertToBytes(elementToken,
                        outputStream);

            }

        }

    }

    /** 
     * Reads from the inputStream and converts it to the LongMatrixToken.
     * @throws IllegalActionException 
     * @see ptserver.data.handler.TokenHandler#convertToToken(java.io.DataInputStream)
     */

    @Override
    public LongMatrixToken convertToToken(DataInputStream inputStream)
            throws IOException, IllegalActionException {

        int rowCount = inputStream.readInt();
        int colunmCount = inputStream.readInt();

        long[][] matrix = new long[rowCount][colunmCount];

        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < colunmCount; column++) {

                LongToken elementToken = (LongToken) TokenParser.getInstance()
                        .convertToToken(inputStream);

                matrix[row][column] = elementToken.longValue();

            }

        }
        return new LongMatrixToken(matrix);

    }
}
