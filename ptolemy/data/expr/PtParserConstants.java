/* Generated By:JJTree&JavaCC: Do not edit this line. PtParserConstants.java */
/* 
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

@ProposedRating Yellow (nsmyth@eecs.berkeley.edu)
@AcceptedRating Yellow (yuhong@eecs.berkeley.edu)

Created : May 1998

*/

package ptolemy.data.expr;

public interface PtParserConstants {

  int EOF = 0;
  int SINGLE_LINE_COMMENT = 3;
  int MULTI_LINE_COMMENT = 4;
  int EOL = 9;
  int PLUS = 10;
  int MINUS = 11;
  int MULTIPLY = 12;
  int DIVIDE = 13;
  int MODULO = 14;
  int GT = 15;
  int LT = 16;
  int GTE = 17;
  int LTE = 18;
  int NOTEQUALS = 19;
  int EQUALS = 20;
  int COND_AND = 21;
  int COND_OR = 22;
  int BOOL_NOT = 23;
  int BITWISE_NOT = 24;
  int AND = 25;
  int OR = 26;
  int XOR = 27;
  int INTEGER = 28;
  int DECIMAL_LITERAL = 29;
  int HEX_LITERAL = 30;
  int OCTAL_LITERAL = 31;
  int EXPONENT = 32;
  int DOUBLE = 33;
  int COMPLEX = 34;
  int BOOLEAN = 35;
  int ID = 36;
  int LETTER = 37;
  int STRING = 38;
  int QUOTE = 39;

  int DEFAULT = 0;
  int IN_SINGLE_LINE_COMMENT = 1;
  int IN_MULTI_LINE_COMMENT = 2;
  int IN_FORMAL_COMMENT = 3;

  String[] tokenImage = {
    "<EOF>",
    "\"//\"",
    "\"/*\"",
    "<SINGLE_LINE_COMMENT>",
    "\"*/\"",
    "<token of kind 5>",
    "\" \"",
    "\"\\r\"",
    "\"\\t\"",
    "\"\\n\"",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "\"/\"",
    "\"%\"",
    "\">\"",
    "\"<\"",
    "\">=\"",
    "\"<=\"",
    "\"!=\"",
    "\"==\"",
    "\"&&\"",
    "\"||\"",
    "\"!\"",
    "\"~\"",
    "\"&\"",
    "\"|\"",
    "\"^\"",
    "<INTEGER>",
    "<DECIMAL_LITERAL>",
    "<HEX_LITERAL>",
    "<OCTAL_LITERAL>",
    "<EXPONENT>",
    "<DOUBLE>",
    "<COMPLEX>",
    "<BOOLEAN>",
    "<ID>",
    "<LETTER>",
    "<STRING>",
    "\"\\\"\"",
    "\"?\"",
    "\":\"",
    "\".\"",
    "\"(\"",
    "\",\"",
    "\")\"",
    "\"[\"",
    "\";\"",
    "\"]\"",
  };

}
