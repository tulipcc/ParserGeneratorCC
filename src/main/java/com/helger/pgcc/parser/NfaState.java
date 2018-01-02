// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

/* Copyright (c) 2006, Sun Microsystems, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.helger.pgcc.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.helger.pgcc.output.UnsupportedOutputLanguageException;

/**
 * The state of a Non-deterministic Finite Automaton.
 */
public class NfaState
{
  public static boolean s_unicodeWarningGiven = false;
  public static int s_generatedStates = 0;

  private static int s_idCnt = 0;
  private static int s_lohiByteCnt;
  private static int s_dummyStateIndex = -1;
  private static boolean s_done;
  private static boolean [] s_mark;
  private static boolean [] s_stateDone;

  private static List <NfaState> s_allStates = new ArrayList <> ();
  private static List <NfaState> s_indexedAllStates = new ArrayList <> ();
  private static List <NfaState> s_nonAsciiTableForMethod = new ArrayList <> ();
  private static Map <String, NfaState> s_equivStatesTable = new HashMap <> ();
  private static Map <String, int []> s_allNextStates = new HashMap <> ();
  private static Map <String, Integer> s_lohiByteTab = new HashMap <> ();
  private static Map <String, Integer> s_stateNameForComposite = new HashMap <> ();
  private static Map <String, int []> s_compositeStateTable = new HashMap <> ();
  private static Map <String, String> s_stateBlockTable = new HashMap <> ();
  private static Map <String, int []> s_stateSetsToFix = new HashMap <> ();

  private static boolean s_jjCheckNAddStatesUnaryNeeded = false;
  private static boolean s_jjCheckNAddStatesDualNeeded = false;

  public static void reInitStatic ()
  {
    s_generatedStates = 0;
    s_idCnt = 0;
    s_dummyStateIndex = -1;
    s_done = false;
    s_mark = null;
    s_stateDone = null;

    s_allStates.clear ();
    s_indexedAllStates.clear ();
    s_equivStatesTable.clear ();
    s_allNextStates.clear ();
    s_compositeStateTable.clear ();
    s_stateBlockTable.clear ();
    s_stateNameForComposite.clear ();
    s_stateSetsToFix.clear ();
  }

  long [] asciiMoves = new long [2];
  char [] charMoves = null;
  private char [] m_rangeMoves = null;
  NfaState next = null;
  private NfaState m_stateForCase;
  final List <NfaState> epsilonMoves = new ArrayList <> ();
  private String m_epsilonMovesString;

  private final int m_id;
  int stateName = -1;
  int kind = Integer.MAX_VALUE;
  private int m_lookingFor;
  private int m_usefulEpsilonMoves = 0;
  int inNextOf;
  private int m_lexState;
  private int m_nonAsciiMethod = -1;
  private int m_kindToPrint = Integer.MAX_VALUE;
  boolean dummy = false;
  private boolean m_isComposite = false;
  private int [] m_compositeStates = null;
  boolean isFinal = false;
  private List <Integer> m_loByteVec;
  private int [] m_nonAsciiMoveIndices;
  private int m_round = 0;
  private int m_onlyChar = 0;
  private char m_matchSingleChar;

  NfaState ()
  {
    m_id = s_idCnt++;
    s_allStates.add (this);
    m_lexState = LexGenJava.lexStateIndex;
    m_lookingFor = LexGenJava.curKind;
  }

  NfaState createClone ()
  {
    final NfaState retVal = new NfaState ();

    retVal.isFinal = isFinal;
    retVal.kind = kind;
    retVal.m_lookingFor = m_lookingFor;
    retVal.m_lexState = m_lexState;
    retVal.inNextOf = inNextOf;

    retVal.mergeMoves (this);

    return retVal;
  }

  private static void _insertInOrder (final List <NfaState> v, final NfaState s)
  {
    int j = 0;
    for (; j < v.size (); j++)
    {
      final NfaState tmp = v.get (j);
      if (tmp.m_id > s.m_id)
        break;
      if (tmp.m_id == s.m_id)
        return;
    }

    v.add (j, s);
  }

  private static char [] _expandCharArr (final char [] oldArr, final int incr)
  {
    final char [] ret = new char [oldArr.length + incr];
    System.arraycopy (oldArr, 0, ret, 0, oldArr.length);
    return ret;
  }

  void addMove (final NfaState newState)
  {
    if (!epsilonMoves.contains (newState))
      _insertInOrder (epsilonMoves, newState);
  }

  private final void _addASCIIMove (final char c)
  {
    asciiMoves[c / 64] |= (1L << (c % 64));
  }

  void addChar (final char c)
  {
    m_onlyChar++;
    m_matchSingleChar = c;
    char temp;
    char temp1;

    if (c < 128) // ASCII char
    {
      _addASCIIMove (c);
      return;
    }

    if (charMoves == null)
      charMoves = new char [10];

    int len = charMoves.length;

    if (charMoves[len - 1] != 0)
    {
      charMoves = _expandCharArr (charMoves, 10);
      len += 10;
    }

    int i = 0;
    for (; i < len; i++)
      if (charMoves[i] == 0 || charMoves[i] > c)
        break;

    if (!s_unicodeWarningGiven && c > 0xff && !Options.getJavaUnicodeEscape () && !Options.getUserCharStream ())
    {
      s_unicodeWarningGiven = true;
      JavaCCErrors.warning (LexGenJava.curRE,
                            "Non-ASCII characters used in regular expression.\n" +
                                              "Please make sure you use the correct Reader when you create the parser, " +
                                              "one that can handle your character set.");
    }

    temp = charMoves[i];
    charMoves[i] = c;

    for (i++; i < len; i++)
    {
      if (temp == 0)
        break;

      temp1 = charMoves[i];
      charMoves[i] = temp;
      temp = temp1;
    }
  }

  void addRange (final char pleft, final char right)
  {
    char left = pleft;
    m_onlyChar = 2;
    char tempLeft1, tempLeft2, tempRight1, tempRight2;

    if (left < 128)
    {
      if (right < 128)
      {
        for (; left <= right; left++)
          _addASCIIMove (left);

        return;
      }

      for (; left < 128; left++)
        _addASCIIMove (left);
    }

    if (!s_unicodeWarningGiven &&
        (left > 0xff || right > 0xff) &&
        !Options.getJavaUnicodeEscape () &&
        !Options.getUserCharStream ())
    {
      s_unicodeWarningGiven = true;
      JavaCCErrors.warning (LexGenJava.curRE,
                            "Non-ASCII characters used in regular expression.\n" +
                                              "Please make sure you use the correct Reader when you create the parser, " +
                                              "one that can handle your character set.");
    }

    if (m_rangeMoves == null)
      m_rangeMoves = new char [20];

    int len = m_rangeMoves.length;

    if (m_rangeMoves[len - 1] != 0)
    {
      m_rangeMoves = _expandCharArr (m_rangeMoves, 20);
      len += 20;
    }

    int i = 0;
    for (; i < len; i += 2)
      if (m_rangeMoves[i] == 0 ||
          (m_rangeMoves[i] > left) ||
          ((m_rangeMoves[i] == left) && (m_rangeMoves[i + 1] > right)))
        break;

    tempLeft1 = m_rangeMoves[i];
    tempRight1 = m_rangeMoves[i + 1];
    m_rangeMoves[i] = left;
    m_rangeMoves[i + 1] = right;

    for (i += 2; i < len; i += 2)
    {
      if (tempLeft1 == 0)
        break;

      tempLeft2 = m_rangeMoves[i];
      tempRight2 = m_rangeMoves[i + 1];
      m_rangeMoves[i] = tempLeft1;
      m_rangeMoves[i + 1] = tempRight1;
      tempLeft1 = tempLeft2;
      tempRight1 = tempRight2;
    }
  }

  private static boolean _equalCharArr (final char [] arr1, final char [] arr2)
  {
    if (arr1 == arr2)
      return true;

    if (arr1 != null && arr2 != null && arr1.length == arr2.length)
    {
      for (int i = arr1.length; i-- > 0;)
        if (arr1[i] != arr2[i])
          return false;

      return true;
    }

    return false;
  }

  // From hereon down all the functions are used for code generation

  private boolean m_closureDone = false;

  /**
   * This function computes the closure and also updates the kind so that any
   * time there is a move to this state, it can go on epsilon to a new state in
   * the epsilon moves that might have a lower kind of token number for the same
   * length.
   */

  private void _recursiveEpsilonClosure ()
  {
    if (m_closureDone || s_mark[m_id])
      return;

    s_mark[m_id] = true;

    // Recursively do closure
    for (final NfaState tmp : epsilonMoves)
      tmp._recursiveEpsilonClosure ();

    // Operate on copy!
    for (final NfaState tmp : new ArrayList <> (epsilonMoves))
    {
      for (final NfaState tmp1 : tmp.epsilonMoves)
      {
        if (tmp1._isUsefulState () && !epsilonMoves.contains (tmp1))
        {
          _insertInOrder (epsilonMoves, tmp1);
          s_done = false;
        }
      }

      if (kind > tmp.kind)
        kind = tmp.kind;
    }

    if (hasTransitions () && !epsilonMoves.contains (this))
      _insertInOrder (epsilonMoves, this);
  }

  private boolean _isUsefulState ()
  {
    return isFinal || hasTransitions ();
  }

  public boolean hasTransitions ()
  {
    return (asciiMoves[0] != 0L ||
            asciiMoves[1] != 0L ||
            (charMoves != null && charMoves[0] != 0) ||
            (m_rangeMoves != null && m_rangeMoves[0] != 0));
  }

  void mergeMoves (final NfaState other)
  {
    // Warning : This function does not merge epsilon moves
    if (asciiMoves == other.asciiMoves)
    {
      JavaCCErrors.semantic_error ("Bug in JavaCC : Please send " +
                                   "a report along with the input that caused this. Thank you.");
      throw new Error ();
    }

    asciiMoves[0] = asciiMoves[0] | other.asciiMoves[0];
    asciiMoves[1] = asciiMoves[1] | other.asciiMoves[1];

    if (other.charMoves != null)
    {
      if (charMoves == null)
        charMoves = other.charMoves;
      else
      {
        final char [] tmpCharMoves = new char [charMoves.length + other.charMoves.length];
        System.arraycopy (charMoves, 0, tmpCharMoves, 0, charMoves.length);
        charMoves = tmpCharMoves;

        for (final char aCharMove : other.charMoves)
          addChar (aCharMove);
      }
    }

    if (other.m_rangeMoves != null)
    {
      if (m_rangeMoves == null)
        m_rangeMoves = other.m_rangeMoves;
      else
      {
        final char [] tmpRangeMoves = new char [m_rangeMoves.length + other.m_rangeMoves.length];
        System.arraycopy (m_rangeMoves, 0, tmpRangeMoves, 0, m_rangeMoves.length);
        m_rangeMoves = tmpRangeMoves;
        for (int i = 0; i < other.m_rangeMoves.length; i += 2)
          addRange (other.m_rangeMoves[i], other.m_rangeMoves[i + 1]);
      }
    }

    if (other.kind < kind)
      kind = other.kind;

    if (other.m_kindToPrint < m_kindToPrint)
      m_kindToPrint = other.m_kindToPrint;

    isFinal |= other.isFinal;
  }

  NfaState createEquivState (final List <NfaState> states)
  {
    final NfaState newState = states.get (0).createClone ();

    newState.next = new NfaState ();

    _insertInOrder (newState.next.epsilonMoves, states.get (0).next);

    for (int i = 1; i < states.size (); i++)
    {
      final NfaState tmp2 = (states.get (i));

      if (tmp2.kind < newState.kind)
        newState.kind = tmp2.kind;

      newState.isFinal |= tmp2.isFinal;

      _insertInOrder (newState.next.epsilonMoves, tmp2.next);
    }

    return newState;
  }

  private NfaState _getEquivalentRunTimeState ()
  {
    Outer: for (int i = s_allStates.size (); i-- > 0;)
    {
      final NfaState other = s_allStates.get (i);

      if (this != other &&
          other.stateName != -1 &&
          m_kindToPrint == other.m_kindToPrint &&
          asciiMoves[0] == other.asciiMoves[0] &&
          asciiMoves[1] == other.asciiMoves[1] &&
          _equalCharArr (charMoves, other.charMoves) &&
          _equalCharArr (m_rangeMoves, other.m_rangeMoves))
      {
        if (next == other.next)
          return other;
        else
          if (next != null && other.next != null)
          {
            if (next.epsilonMoves.size () == other.next.epsilonMoves.size ())
            {
              for (int j = 0; j < next.epsilonMoves.size (); j++)
                if (next.epsilonMoves.get (j) != other.next.epsilonMoves.get (j))
                  continue Outer;

              return other;
            }
          }
      }
    }

    return null;
  }

  // generates code (without outputting it) and returns the name used.
  void generateCode ()
  {
    if (stateName != -1)
      return;

    if (next != null)
    {
      next.generateCode ();
      if (next.kind != Integer.MAX_VALUE)
        m_kindToPrint = next.kind;
    }

    if (stateName == -1 && hasTransitions ())
    {
      final NfaState tmp = _getEquivalentRunTimeState ();

      if (tmp != null)
      {
        stateName = tmp.stateName;
        // ????
        // tmp.inNextOf += inNextOf;
        // ????
        dummy = true;
        return;
      }

      stateName = s_generatedStates++;
      s_indexedAllStates.add (this);
      generateNextStatesCode ();
    }
  }

  public static void computeClosures ()
  {
    // Back to front
    for (int i = s_allStates.size () - 1; i >= 0; --i)
    {
      final NfaState tmp = s_allStates.get (i);
      if (!tmp.m_closureDone)
        tmp._optimizeEpsilonMoves (true);
    }

    // Operate on copy!
    for (final NfaState tmp : new ArrayList <> (s_allStates))
      if (!tmp.m_closureDone)
        tmp._optimizeEpsilonMoves (false);

    if (false)
    {
      for (int i = 0; i < s_allStates.size (); i++)
      {
        final NfaState tmp = s_allStates.get (i);
        final NfaState [] epsilonMoveArray = new NfaState [tmp.epsilonMoves.size ()];
        tmp.epsilonMoves.toArray (epsilonMoveArray);
      }
    }
  }

  private void _optimizeEpsilonMoves (final boolean optReqd)
  {
    // First do epsilon closure
    s_done = false;
    while (!s_done)
    {
      if (s_mark == null || s_mark.length < s_allStates.size ())
        s_mark = new boolean [s_allStates.size ()];

      for (int i = s_allStates.size (); i-- > 0;)
        s_mark[i] = false;

      s_done = true;
      _recursiveEpsilonClosure ();
    }

    for (int i = s_allStates.size (); i-- > 0;)
    {
      final NfaState tmp = s_allStates.get (i);
      tmp.m_closureDone = s_mark[tmp.m_id];
    }

    // Warning : The following piece of code is just an optimization.
    // in case of trouble, just remove this piece.

    boolean sometingOptimized = true;

    NfaState newState = null;
    NfaState tmp1, tmp2;
    List <NfaState> equivStates = null;

    while (sometingOptimized)
    {
      sometingOptimized = false;
      for (int i = 0; optReqd && i < epsilonMoves.size (); i++)
      {
        tmp1 = epsilonMoves.get (i);
        if (tmp1.hasTransitions ())
        {
          for (int j = i + 1; j < epsilonMoves.size (); j++)
          {
            tmp2 = epsilonMoves.get (j);
            if (tmp2.hasTransitions () &&
                (tmp1.asciiMoves[0] == tmp2.asciiMoves[0] &&
                 tmp1.asciiMoves[1] == tmp2.asciiMoves[1] &&
                 _equalCharArr (tmp1.charMoves, tmp2.charMoves) &&
                 _equalCharArr (tmp1.m_rangeMoves, tmp2.m_rangeMoves)))
            {
              if (equivStates == null)
              {
                equivStates = new ArrayList <> ();
                equivStates.add (tmp1);
              }

              _insertInOrder (equivStates, tmp2);
              epsilonMoves.remove (j--);
            }
          }
        }

        if (equivStates != null)
        {
          sometingOptimized = true;
          String tmp = "";
          for (int l = 0; l < equivStates.size (); l++)
            tmp += String.valueOf (equivStates.get (l).m_id) + ", ";

          if ((newState = s_equivStatesTable.get (tmp)) == null)
          {
            newState = createEquivState (equivStates);
            s_equivStatesTable.put (tmp, newState);
          }

          epsilonMoves.remove (i--);
          epsilonMoves.add (newState);
          equivStates = null;
          newState = null;
        }
      }

      for (int i = 0; i < epsilonMoves.size (); i++)
      {
        // if ((tmp1 = (NfaState)epsilonMoves.elementAt(i)).next == null)
        // continue;
        tmp1 = epsilonMoves.get (i);

        for (int j = i + 1; j < epsilonMoves.size (); j++)
        {
          tmp2 = epsilonMoves.get (j);

          if (tmp1.next == tmp2.next)
          {
            if (newState == null)
            {
              newState = tmp1.createClone ();
              newState.next = tmp1.next;
              sometingOptimized = true;
            }

            newState.mergeMoves (tmp2);
            epsilonMoves.remove (j--);
          }
        }

        if (newState != null)
        {
          epsilonMoves.remove (i--);
          epsilonMoves.add (newState);
          newState = null;
        }
      }
    }

    // End Warning

    // Generate an array of states for epsilon moves (not vector)
    if (epsilonMoves.size () > 0)
    {
      for (int i = 0; i < epsilonMoves.size (); i++)
        // Since we are doing a closure, just epsilon moves are unncessary
        if (epsilonMoves.get (i).hasTransitions ())
          m_usefulEpsilonMoves++;
        else
          epsilonMoves.remove (i--);
    }
  }

  void generateNextStatesCode ()
  {
    if (next.m_usefulEpsilonMoves > 0)
      next.getEpsilonMovesString ();
  }

  String getEpsilonMovesString ()
  {
    final int [] stateNames = new int [m_usefulEpsilonMoves];
    int cnt = 0;

    if (m_epsilonMovesString != null)
      return m_epsilonMovesString;

    if (m_usefulEpsilonMoves > 0)
    {
      NfaState tempState;
      m_epsilonMovesString = "{ ";
      for (int i = 0; i < epsilonMoves.size (); i++)
      {
        if ((tempState = epsilonMoves.get (i)).hasTransitions ())
        {
          if (tempState.stateName == -1)
            tempState.generateCode ();

          s_indexedAllStates.get (tempState.stateName).inNextOf++;
          stateNames[cnt] = tempState.stateName;
          m_epsilonMovesString += tempState.stateName + ", ";
          if (cnt++ > 0 && cnt % 16 == 0)
            m_epsilonMovesString += "\n";
        }
      }

      m_epsilonMovesString += "};";
    }

    m_usefulEpsilonMoves = cnt;
    if (m_epsilonMovesString != null && s_allNextStates.get (m_epsilonMovesString) == null)
    {
      final int [] statesToPut = new int [m_usefulEpsilonMoves];

      System.arraycopy (stateNames, 0, statesToPut, 0, cnt);
      s_allNextStates.put (m_epsilonMovesString, statesToPut);
    }

    return m_epsilonMovesString;
  }

  public static boolean canStartNfaUsingAscii (final char c)
  {
    if (c >= 128)
      throw new Error ("JavaCC Bug: Please send mail to sankar@cs.stanford.edu");

    final String s = LexGenJava.initialState.getEpsilonMovesString ();

    if (s == null || s.equals ("null;"))
      return false;

    final int [] states = s_allNextStates.get (s);

    for (final int aState : states)
    {
      final NfaState tmp = s_indexedAllStates.get (aState);

      if ((tmp.asciiMoves[c / 64] & (1L << c % 64)) != 0L)
        return true;
    }

    return false;
  }

  final boolean canMoveUsingChar (final char c)
  {
    if (m_onlyChar == 1)
      return c == m_matchSingleChar;

    if (c < 128)
      return ((asciiMoves[c / 64] & (1L << c % 64)) != 0L);

    // Just check directly if there is a move for this char
    if (charMoves != null && charMoves[0] != 0)
    {
      for (final char aCharMove : charMoves)
      {
        if (c == aCharMove)
          return true;
        else
          if (c < aCharMove || aCharMove == 0)
            break;
      }
    }

    // For ranges, iterate thru the table to see if the current char
    // is in some range
    if (m_rangeMoves != null && m_rangeMoves[0] != 0)
      for (int i = 0; i < m_rangeMoves.length; i += 2)
        if (c >= m_rangeMoves[i] && c <= m_rangeMoves[i + 1])
          return true;
        else
          if (c < m_rangeMoves[i] || m_rangeMoves[i] == 0)
            break;

    // return (nextForNegatedList != null);
    return false;
  }

  public int getFirstValidPos (final String s, final int nPos, final int len)
  {
    int i = nPos;
    if (m_onlyChar == 1)
    {
      final char c = m_matchSingleChar;
      while (c != s.charAt (i) && ++i < len)
      {}
      return i;
    }

    do
    {
      if (canMoveUsingChar (s.charAt (i)))
        return i;
    } while (++i < len);

    return i;
  }

  public int moveFrom (final char c, final List <NfaState> newStates)
  {
    if (canMoveUsingChar (c))
    {
      for (int i = next.epsilonMoves.size (); i-- > 0;)
        _insertInOrder (newStates, next.epsilonMoves.get (i));

      return m_kindToPrint;
    }

    return Integer.MAX_VALUE;
  }

  public static int moveFromSet (final char c, final List <NfaState> states, final List <NfaState> newStates)
  {
    int retVal = Integer.MAX_VALUE;

    for (int i = states.size (); i-- > 0;)
    {
      final int tmp = states.get (i).moveFrom (c, newStates);
      if (retVal > tmp)
        retVal = tmp;
    }

    return retVal;
  }

  public static int moveFromSetForRegEx (final char c,
                                         final NfaState [] states,
                                         final NfaState [] newStates,
                                         final int round)
  {
    int start = 0;
    final int sz = states.length;

    for (int i = 0; i < sz; i++)
    {
      final NfaState tmp1 = states[i];
      if (tmp1 == null)
        break;

      if (tmp1.canMoveUsingChar (c))
      {
        if (tmp1.m_kindToPrint != Integer.MAX_VALUE)
        {
          newStates[start] = null;
          return 1;
        }

        final List <NfaState> v = tmp1.next.epsilonMoves;
        for (int j = v.size () - 1; j >= 0; j--)
        {
          final NfaState tmp2 = v.get (j);
          if (tmp2.m_round != round)
          {
            tmp2.m_round = round;
            newStates[start++] = tmp2;
          }
        }
      }
    }

    newStates[start] = null;
    return Integer.MAX_VALUE;
  }

  static List <String> allBitVectors = new ArrayList <> ();

  /*
   * This function generates the bit vectors of low and hi bytes for common bit
   * vectors and returns those that are not common with anything (in loBytes)
   * and returns an array of indices that can be used to generate the function
   * names for char matching using the common bit vectors. It also generates
   * code to match a char with the common bit vectors. (Need a better comment).
   */

  static int [] tmpIndices = new int [512]; // 2 * 256

  void generateNonAsciiMoves (final CodeGenerator codeGenerator)
  {
    int i = 0, j = 0;
    char hiByte;
    int cnt = 0;
    final long [] [] loBytes = new long [256] [4];

    if ((charMoves == null || charMoves[0] == 0) && (m_rangeMoves == null || m_rangeMoves[0] == 0))
      return;

    if (charMoves != null)
    {
      for (i = 0; i < charMoves.length; i++)
      {
        if (charMoves[i] == 0)
          break;

        hiByte = (char) (charMoves[i] >> 8);
        loBytes[hiByte][(charMoves[i] & 0xff) / 64] |= (1L << ((charMoves[i] & 0xff) % 64));
      }
    }

    if (m_rangeMoves != null)
    {
      for (i = 0; i < m_rangeMoves.length; i += 2)
      {
        if (m_rangeMoves[i] == 0)
          break;

        char c, r;

        r = (char) (m_rangeMoves[i + 1] & 0xff);
        hiByte = (char) (m_rangeMoves[i] >> 8);

        if (hiByte == (char) (m_rangeMoves[i + 1] >> 8))
        {
          for (c = (char) (m_rangeMoves[i] & 0xff); c <= r; c++)
            loBytes[hiByte][c / 64] |= (1L << (c % 64));

          continue;
        }

        for (c = (char) (m_rangeMoves[i] & 0xff); c <= 0xff; c++)
          loBytes[hiByte][c / 64] |= (1L << (c % 64));

        while (++hiByte < (char) (m_rangeMoves[i + 1] >> 8))
        {
          loBytes[hiByte][0] |= 0xffffffffffffffffL;
          loBytes[hiByte][1] |= 0xffffffffffffffffL;
          loBytes[hiByte][2] |= 0xffffffffffffffffL;
          loBytes[hiByte][3] |= 0xffffffffffffffffL;
        }

        for (c = 0; c <= r; c++)
          loBytes[hiByte][c / 64] |= (1L << (c % 64));
      }
    }

    long [] common = null;
    final boolean [] done = new boolean [256];

    for (i = 0; i <= 255; i++)
    {
      if (done[i] || (done[i] = loBytes[i][0] == 0 && loBytes[i][1] == 0 && loBytes[i][2] == 0 && loBytes[i][3] == 0))
        continue;

      for (j = i + 1; j < 256; j++)
      {
        if (done[j])
          continue;

        if (loBytes[i][0] == loBytes[j][0] &&
            loBytes[i][1] == loBytes[j][1] &&
            loBytes[i][2] == loBytes[j][2] &&
            loBytes[i][3] == loBytes[j][3])
        {
          done[j] = true;
          if (common == null)
          {
            done[i] = true;
            common = new long [4];
            common[i / 64] |= (1L << (i % 64));
          }

          common[j / 64] |= (1L << (j % 64));
        }
      }

      if (common != null)
      {
        Integer ind;
        String tmp;

        tmp = "{\n   0x" +
              Long.toHexString (common[0]) +
              "L, " +
              "0x" +
              Long.toHexString (common[1]) +
              "L, " +
              "0x" +
              Long.toHexString (common[2]) +
              "L, " +
              "0x" +
              Long.toHexString (common[3]) +
              "L\n};";
        if ((ind = s_lohiByteTab.get (tmp)) == null)
        {
          allBitVectors.add (tmp);

          if (!allBitsSet (tmp))
          {
            switch (codeGenerator.getOutputLanguage ())
            {
              case JAVA:
                codeGenerator.genCodeLine ("static final " +
                                           Options.getLongType () +
                                           "[] jjbitVec" +
                                           s_lohiByteCnt +
                                           " = " +
                                           tmp);
                break;
              case CPP:
                codeGenerator.switchToStaticsFile ();
                codeGenerator.genCodeLine ("static const " +
                                           Options.getLongType () +
                                           " jjbitVec" +
                                           s_lohiByteCnt +
                                           "[] = " +
                                           tmp);
                break;
              default:
                throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
            }
          }
          s_lohiByteTab.put (tmp, ind = Integer.valueOf (s_lohiByteCnt++));
        }

        tmpIndices[cnt++] = ind.intValue ();

        tmp = "{\n   0x" +
              Long.toHexString (loBytes[i][0]) +
              "L, " +
              "0x" +
              Long.toHexString (loBytes[i][1]) +
              "L, " +
              "0x" +
              Long.toHexString (loBytes[i][2]) +
              "L, " +
              "0x" +
              Long.toHexString (loBytes[i][3]) +
              "L\n};";
        if ((ind = s_lohiByteTab.get (tmp)) == null)
        {
          allBitVectors.add (tmp);

          if (!allBitsSet (tmp))
            switch (codeGenerator.getOutputLanguage ())
            {
              case JAVA:
                codeGenerator.genCodeLine ("static final " +
                                           Options.getLongType () +
                                           "[] jjbitVec" +
                                           s_lohiByteCnt +
                                           " = " +
                                           tmp);
                break;
              case CPP:
                codeGenerator.switchToStaticsFile ();
                codeGenerator.genCodeLine ("static const " +
                                           Options.getLongType () +
                                           " jjbitVec" +
                                           s_lohiByteCnt +
                                           "[] = " +
                                           tmp);
                codeGenerator.switchToMainFile ();
                break;
              default:
                throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
            }
          s_lohiByteTab.put (tmp, ind = Integer.valueOf (s_lohiByteCnt++));
        }

        tmpIndices[cnt++] = ind.intValue ();

        common = null;
      }
    }

    m_nonAsciiMoveIndices = new int [cnt];
    System.arraycopy (tmpIndices, 0, m_nonAsciiMoveIndices, 0, cnt);

    /*
     * System.out.println("state : " + stateName + " cnt : " + cnt); while (cnt
     * > 0) { System.out.print(nonAsciiMoveIndices[cnt - 1] + ", " +
     * nonAsciiMoveIndices[cnt - 2] + ", "); cnt -= 2; } System.out.println("");
     */

    for (i = 0; i < 256; i++)
    {
      if (done[i])
        loBytes[i] = null;
      else
      {
        // System.out.print(i + ", ");
        String tmp;
        Integer ind;

        tmp = "{\n   0x" +
              Long.toHexString (loBytes[i][0]) +
              "L, " +
              "0x" +
              Long.toHexString (loBytes[i][1]) +
              "L, " +
              "0x" +
              Long.toHexString (loBytes[i][2]) +
              "L, " +
              "0x" +
              Long.toHexString (loBytes[i][3]) +
              "L\n};";

        if ((ind = s_lohiByteTab.get (tmp)) == null)
        {
          allBitVectors.add (tmp);

          if (!allBitsSet (tmp))
            switch (codeGenerator.getOutputLanguage ())
            {
              case JAVA:
                codeGenerator.genCodeLine ("static final " +
                                           Options.getLongType () +
                                           "[] jjbitVec" +
                                           s_lohiByteCnt +
                                           " = " +
                                           tmp);
                break;
              case CPP:
                codeGenerator.switchToStaticsFile ();
                codeGenerator.genCodeLine ("static const " +
                                           Options.getLongType () +
                                           " jjbitVec" +
                                           s_lohiByteCnt +
                                           "[] = " +
                                           tmp);
                break;
              default:
                throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
            }
          s_lohiByteTab.put (tmp, ind = Integer.valueOf (s_lohiByteCnt++));
        }

        if (m_loByteVec == null)
          m_loByteVec = new ArrayList <> ();

        m_loByteVec.add (Integer.valueOf (i));
        m_loByteVec.add (ind);
      }
    }
    // System.out.println("");
    _updateDuplicateNonAsciiMoves ();
  }

  private void _updateDuplicateNonAsciiMoves ()
  {
    for (int i = 0; i < s_nonAsciiTableForMethod.size (); i++)
    {
      final NfaState tmp = s_nonAsciiTableForMethod.get (i);
      if (_equalLoByteVectors (m_loByteVec, tmp.m_loByteVec) &&
          _equalNonAsciiMoveIndices (m_nonAsciiMoveIndices, tmp.m_nonAsciiMoveIndices))
      {
        m_nonAsciiMethod = i;
        return;
      }
    }

    m_nonAsciiMethod = s_nonAsciiTableForMethod.size ();
    s_nonAsciiTableForMethod.add (this);
  }

  private static boolean _equalLoByteVectors (final List <Integer> vec1, final List <Integer> vec2)
  {
    if (vec1 == null || vec2 == null)
      return false;

    if (vec1 == vec2)
      return true;

    if (vec1.size () != vec2.size ())
      return false;

    for (int i = 0; i < vec1.size (); i++)
    {
      if (vec1.get (i).intValue () != vec2.get (i).intValue ())
        return false;
    }

    return true;
  }

  private static boolean _equalNonAsciiMoveIndices (final int [] moves1, final int [] moves2)
  {
    if (moves1 == moves2)
      return true;

    if (moves1 == null || moves2 == null)
      return false;

    if (moves1.length != moves2.length)
      return false;

    for (int i = 0; i < moves1.length; i++)
    {
      if (moves1[i] != moves2[i])
        return false;
    }

    return true;
  }

  static String allBits = "{\n   0xffffffffffffffffL, " +
                          "0xffffffffffffffffL, " +
                          "0xffffffffffffffffL, " +
                          "0xffffffffffffffffL\n};";

  static boolean allBitsSet (final String bitVec)
  {
    return bitVec.equals (allBits);
  }

  static int addStartStateSet (final String stateSetString)
  {
    return _addCompositeStateSet (stateSetString, true);
  }

  private static int _addCompositeStateSet (final String stateSetString, final boolean starts)
  {
    Integer stateNameToReturn;

    if ((stateNameToReturn = s_stateNameForComposite.get (stateSetString)) != null)
      return stateNameToReturn.intValue ();

    int toRet = 0;
    final int [] nameSet = s_allNextStates.get (stateSetString);

    if (!starts)
      s_stateBlockTable.put (stateSetString, stateSetString);

    if (nameSet == null)
      throw new Error ("JavaCC Bug: Please file a bug at: http://javacc.java.net");

    if (nameSet.length == 1)
    {
      stateNameToReturn = Integer.valueOf (nameSet[0]);
      s_stateNameForComposite.put (stateSetString, stateNameToReturn);
      return nameSet[0];
    }

    for (final int aElement : nameSet)
    {
      if (aElement == -1)
        continue;

      final NfaState st = s_indexedAllStates.get (aElement);
      st.m_isComposite = true;
      st.m_compositeStates = nameSet;
    }

    while (toRet < nameSet.length && (starts && s_indexedAllStates.get (nameSet[toRet]).inNextOf > 1))
      toRet++;

    for (final String s : s_compositeStateTable.keySet ())
    {
      if (!s.equals (stateSetString) && _intersect (stateSetString, s))
      {
        final int [] other = s_compositeStateTable.get (s);

        while (toRet < nameSet.length &&
               ((starts && s_indexedAllStates.get (nameSet[toRet]).inNextOf > 1) ||
                _elemOccurs (nameSet[toRet], other) >= 0))
          toRet++;
      }
    }

    int tmp;

    if (toRet >= nameSet.length)
    {
      if (s_dummyStateIndex == -1)
        tmp = s_dummyStateIndex = s_generatedStates;
      else
        tmp = ++s_dummyStateIndex;

      // TODO(sreeni) : Fix this
      if (Options.getTokenManagerCodeGenerator () != null)
      {
        final NfaState dummyState = new NfaState ();
        dummyState.m_isComposite = true;
        dummyState.m_compositeStates = nameSet;
        dummyState.stateName = tmp;
      }
    }
    else
      tmp = nameSet[toRet];

    stateNameToReturn = Integer.valueOf (tmp);
    s_stateNameForComposite.put (stateSetString, stateNameToReturn);
    s_compositeStateTable.put (stateSetString, nameSet);

    return tmp;
  }

  private static int _stateNameForComposite (final String stateSetString)
  {
    return s_stateNameForComposite.get (stateSetString).intValue ();
  }

  static int initStateName ()
  {
    final String s = LexGenJava.initialState.getEpsilonMovesString ();

    if (LexGenJava.initialState.m_usefulEpsilonMoves != 0)
      return _stateNameForComposite (s);
    return -1;
  }

  public int generateInitMoves ()
  {
    getEpsilonMovesString ();

    if (m_epsilonMovesString == null)
      m_epsilonMovesString = "null;";

    return addStartStateSet (m_epsilonMovesString);
  }

  static Map <String, int []> tableToDump = new HashMap <> ();
  static List <int []> orderedStateSet = new ArrayList <> ();

  static int lastIndex = 0;

  private static int [] _getStateSetIndicesForUse (final String arrayString)
  {
    int [] ret;
    final int [] set = s_allNextStates.get (arrayString);

    if ((ret = tableToDump.get (arrayString)) == null)
    {
      ret = new int [2];
      ret[0] = lastIndex;
      ret[1] = lastIndex + set.length - 1;
      lastIndex += set.length;
      tableToDump.put (arrayString, ret);
      orderedStateSet.add (set);
    }

    return ret;
  }

  public static void dumpStateSets (final CodeGenerator codeGenerator)
  {
    int cnt = 0;

    switch (codeGenerator.getOutputLanguage ())
    {
      case JAVA:
        codeGenerator.genCode ("static final int[] jjnextStates = {");
        break;
      case CPP:
        codeGenerator.switchToStaticsFile ();
        codeGenerator.genCode ("static const int jjnextStates[] = {");
        break;
      default:
        throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
    }
    if (orderedStateSet.size () > 0)
      for (int i = 0; i < orderedStateSet.size (); i++)
      {
        final int [] set = orderedStateSet.get (i);

        for (final int aElement : set)
        {
          if (cnt++ % 16 == 0)
            codeGenerator.genCode ("\n   ");

          codeGenerator.genCode (aElement + ", ");
        }
      }
    else
      codeGenerator.genCode ("0");

    codeGenerator.genCodeLine ("\n};");
    codeGenerator.switchToMainFile ();
  }

  static String getStateSetString (final int [] states)
  {
    String retVal = "{ ";
    for (int i = 0; i < states.length;)
    {
      retVal += states[i] + ", ";

      if (i++ > 0 && i % 16 == 0)
        retVal += "\n";
    }

    retVal += "};";
    s_allNextStates.put (retVal, states);
    return retVal;
  }

  static String getStateSetString (final List <NfaState> states)
  {
    if (states == null || states.size () == 0)
      return "null;";

    final int [] set = new int [states.size ()];
    String retVal = "{ ";
    for (int i = 0; i < states.size ();)
    {
      final int k = states.get (i).stateName;
      retVal += k + ", ";
      set[i] = k;

      if (i++ > 0 && (i % 16) == 0)
        retVal += "\n";
    }

    retVal += "};";
    s_allNextStates.put (retVal, set);
    return retVal;
  }

  static int numberOfBitsSet (final long l)
  {
    int ret = 0;
    for (int i = 0; i < 63; i++)
      if (((l >> i) & 1L) != 0L)
        ret++;

    return ret;
  }

  static int onlyOneBitSet (final long l)
  {
    int oneSeen = -1;
    for (int i = 0; i < 64; i++)
      if (((l >> i) & 1L) != 0L)
      {
        if (oneSeen >= 0)
          return -1;
        oneSeen = i;
      }

    return oneSeen;
  }

  private static int _elemOccurs (final int elem, final int [] arr)
  {
    for (int i = arr.length; i-- > 0;)
      if (arr[i] == elem)
        return i;

    return -1;
  }

  @SuppressWarnings ("unused")
  private boolean _findCommonBlocks ()
  {
    if (next == null || next.m_usefulEpsilonMoves <= 1)
      return false;

    if (s_stateDone == null)
      s_stateDone = new boolean [s_generatedStates];

    final String set = next.m_epsilonMovesString;

    final int [] nameSet = s_allNextStates.get (set);

    if (nameSet.length <= 2 || s_compositeStateTable.get (set) != null)
      return false;

    final int freq[] = new int [nameSet.length];
    final boolean live[] = new boolean [nameSet.length];
    final int [] count = new int [s_allNextStates.size ()];

    for (int i = 0; i < nameSet.length; i++)
    {
      if (nameSet[i] != -1)
      {
        live[i] = !s_stateDone[nameSet[i]];
        if (live[i])
          count[0]++;
      }
    }

    int blockLen = 0, commonFreq = 0;
    boolean needUpdate;

    for (final Map.Entry <String, int []> aEntry : s_allNextStates.entrySet ())
    {
      final int [] tmpSet = aEntry.getValue ();
      if (tmpSet == nameSet)
        continue;

      needUpdate = false;
      for (int j = 0; j < nameSet.length; j++)
      {
        if (nameSet[j] == -1)
          continue;

        if (live[j] && _elemOccurs (nameSet[j], tmpSet) >= 0)
        {
          if (!needUpdate)
          {
            needUpdate = true;
            commonFreq++;
          }

          count[freq[j]]--;
          count[commonFreq]++;
          freq[j] = commonFreq;
        }
      }

      if (needUpdate)
      {
        int foundFreq = -1;
        blockLen = 0;

        for (int j = 0; j <= commonFreq; j++)
          if (count[j] > blockLen)
          {
            foundFreq = j;
            blockLen = count[j];
          }

        if (blockLen <= 1)
          return false;

        for (int j = 0; j < nameSet.length; j++)
          if (nameSet[j] != -1 && freq[j] != foundFreq)
          {
            live[j] = false;
            count[freq[j]]--;
          }
      }
    }

    if (blockLen <= 1)
      return false;

    final int [] commonBlock = new int [blockLen];
    int cnt = 0;
    // System.out.println("Common Block for " + set + " :");
    for (int i = 0; i < nameSet.length; i++)
    {
      if (live[i])
      {
        if (s_indexedAllStates.get (nameSet[i]).m_isComposite)
          return false;

        s_stateDone[nameSet[i]] = true;
        commonBlock[cnt++] = nameSet[i];
        // System.out.print(nameSet[i] + ", ");
      }
    }

    // System.out.println("");

    final String s = getStateSetString (commonBlock);

    Outer: for (final Map.Entry <String, int []> aEntry : s_allNextStates.entrySet ())
    {
      boolean firstOne = true;
      final String stringToFix = aEntry.getKey ();
      final int [] setToFix = aEntry.getValue ();

      if (setToFix == commonBlock)
        continue;

      for (int k = 0; k < cnt; k++)
      {
        final int at = _elemOccurs (commonBlock[k], setToFix);
        if (at >= 0)
        {
          if (!firstOne)
            setToFix[at] = -1;
          firstOne = false;
        }
        else
          continue Outer;
      }

      if (s_stateSetsToFix.get (stringToFix) == null)
        s_stateSetsToFix.put (stringToFix, setToFix);
    }

    next.m_usefulEpsilonMoves -= blockLen - 1;
    _addCompositeStateSet (s, false);
    return true;
  }

  @SuppressWarnings ("unused")
  private boolean _checkNextOccursTogether ()
  {
    if (next == null || next.m_usefulEpsilonMoves <= 1)
      return true;

    final String set = next.m_epsilonMovesString;

    final int [] nameSet = s_allNextStates.get (set);

    if (nameSet.length == 1 || s_compositeStateTable.get (set) != null || s_stateSetsToFix.get (set) != null)
      return false;

    final Map <String, int []> occursIn = new HashMap <> ();
    final NfaState tmp = s_allStates.get (nameSet[0]);

    for (int i = 1; i < nameSet.length; i++)
    {
      final NfaState tmp1 = s_allStates.get (nameSet[i]);

      if (tmp.inNextOf != tmp1.inNextOf)
        return false;
    }

    for (final Map.Entry <String, int []> aEntry : s_allNextStates.entrySet ())
    {
      final String s = aEntry.getKey ();
      final int [] tmpSet = aEntry.getValue ();

      if (tmpSet == nameSet)
        continue;

      int isPresent = 0;
      int j = 0;
      for (final int aElement : nameSet)
      {
        if (_elemOccurs (aElement, tmpSet) >= 0)
          isPresent++;
        else
          if (isPresent > 0)
            return false;
        j++;
      }

      if (isPresent == j)
      {
        if (tmpSet.length > nameSet.length)
          occursIn.put (s, tmpSet);

        // May not need. But safe.
        if (s_compositeStateTable.get (s) != null || s_stateSetsToFix.get (s) != null)
          return false;
      }
      else
        if (isPresent != 0)
          return false;
    }

    for (final Map.Entry <String, int []> aEntry : occursIn.entrySet ())
    {
      final String s = aEntry.getKey ();
      final int [] setToFix = aEntry.getValue ();

      if (!s_stateSetsToFix.containsKey (s))
        s_stateSetsToFix.put (s, setToFix);

      for (int k = 0; k < setToFix.length; k++)
      {
        // Not >= since need the first one (0)
        if (_elemOccurs (setToFix[k], nameSet) > 0)
          setToFix[k] = -1;
      }
    }

    next.m_usefulEpsilonMoves = 1;
    _addCompositeStateSet (next.m_epsilonMovesString, false);
    return true;
  }

  private static void _fixStateSets ()
  {
    final Map <String, int []> fixedSets = new HashMap <> ();
    final int [] tmp = new int [s_generatedStates];

    for (final Map.Entry <String, int []> aEntry : s_stateSetsToFix.entrySet ())
    {
      final String s = aEntry.getKey ();
      final int [] toFix = aEntry.getValue ();
      int cnt = 0;

      // System.out.print("Fixing : ");
      for (final int aElement : toFix)
      {
        // System.out.print(toFix[i] + ", ");
        if (aElement != -1)
          tmp[cnt++] = aElement;
      }

      final int [] fixed = new int [cnt];
      System.arraycopy (tmp, 0, fixed, 0, cnt);
      fixedSets.put (s, fixed);
      s_allNextStates.put (s, fixed);
      // System.out.println(" as " + GetStateSetString(fixed));
    }

    for (int i = 0; i < s_allStates.size (); i++)
    {
      final NfaState tmpState = s_allStates.get (i);

      if (tmpState.next == null || tmpState.next.m_usefulEpsilonMoves == 0)
        continue;

      /*
       * if (compositeStateTable.get(tmpState.next.epsilonMovesString) != null)
       * tmpState.next.usefulEpsilonMoves = 1; else
       */
      final int [] newSet = fixedSets.get (tmpState.next.m_epsilonMovesString);
      if (newSet != null)
        tmpState._fixNextStates (newSet);
    }
  }

  private final void _fixNextStates (final int [] newSet)
  {
    next.m_usefulEpsilonMoves = newSet.length;
    // next.epsilonMovesString = GetStateSetString(newSet);
  }

  private static boolean _intersect (final String set1, final String set2)
  {
    if (set1 == null || set2 == null)
      return false;

    final int [] nameSet1 = s_allNextStates.get (set1);
    final int [] nameSet2 = s_allNextStates.get (set2);

    if (nameSet1 == null || nameSet2 == null)
      return false;

    if (nameSet1 == nameSet2)
      return true;

    for (int i = nameSet1.length; i-- > 0;)
      for (int j = nameSet2.length; j-- > 0;)
        if (nameSet1[i] == nameSet2[j])
          return true;

    return false;
  }

  private static void _dumpHeadForCase (final CodeGenerator codeGenerator, final int byteNum)
  {
    if (byteNum == 0)
    {
      codeGenerator.genCodeLine ("         " + Options.getLongType () + " l = 1L << curChar;");
      switch (codeGenerator.getOutputLanguage ())
      {
        case CPP:
          codeGenerator.genCodeLine ("         (void)l;");
          break;
      }
    }
    else
      if (byteNum == 1)
      {
        codeGenerator.genCodeLine ("         " + Options.getLongType () + " l = 1L << (curChar & 077);");
        switch (codeGenerator.getOutputLanguage ())
        {
          case CPP:
            codeGenerator.genCodeLine ("         (void)l;");
            break;
        }
      }
      else
      {
        if (Options.getJavaUnicodeEscape () || s_unicodeWarningGiven)
        {
          codeGenerator.genCodeLine ("         int hiByte = (curChar >> 8);");
          codeGenerator.genCodeLine ("         int i1 = hiByte >> 6;");
          codeGenerator.genCodeLine ("         " + Options.getLongType () + " l1 = 1L << (hiByte & 077);");
        }

        codeGenerator.genCodeLine ("         int i2 = (curChar & 0xff) >> 6;");
        codeGenerator.genCodeLine ("         " + Options.getLongType () + " l2 = 1L << (curChar & 077);");
      }

    // codeGenerator.genCodeLine(" MatchLoop: do");
    codeGenerator.genCodeLine ("         do");
    codeGenerator.genCodeLine ("         {");

    codeGenerator.genCodeLine ("            switch(jjstateSet[--i])");
    codeGenerator.genCodeLine ("            {");
  }

  private static List <List <NfaState>> _partitionStatesSetForAscii (final int [] states, final int byteNum)
  {
    final int [] cardinalities = new int [states.length];
    List <NfaState> original = new ArrayList <> ();
    final List <List <NfaState>> partition = new ArrayList <> ();
    NfaState tmp;

    for (final int aState : states)
      original.add (null);

    int cnt = 0;
    for (int i = 0; i < states.length; i++)
    {
      tmp = s_allStates.get (states[i]);

      if (tmp.asciiMoves[byteNum] != 0L)
      {
        int j;
        final int p = numberOfBitsSet (tmp.asciiMoves[byteNum]);

        for (j = 0; j < i; j++)
          if (cardinalities[j] <= p)
            break;

        for (int k = i; k > j; k--)
          cardinalities[k] = cardinalities[k - 1];

        cardinalities[j] = p;

        original.add (j, tmp);
        cnt++;
      }
    }

    // original.setSize (cnt);
    while (original.size () < cnt)
      original.add (null);
    if (original.size () > cnt)
      original = original.subList (0, cnt);

    while (!original.isEmpty ())
    {
      tmp = original.remove (0);

      long bitVec = tmp.asciiMoves[byteNum];
      final List <NfaState> subSet = new ArrayList <> ();
      subSet.add (tmp);

      for (int j = 0; j < original.size (); j++)
      {
        final NfaState tmp1 = original.get (j);

        if ((tmp1.asciiMoves[byteNum] & bitVec) == 0L)
        {
          bitVec |= tmp1.asciiMoves[byteNum];
          subSet.add (tmp1);
          original.remove (j--);
        }
      }

      partition.add (subSet);
    }

    return partition;
  }

  private String _printNoBreak (final CodeGenerator codeGenerator, final int byteNum, final boolean [] dumped)
  {
    if (inNextOf != 1)
      throw new Error ("JavaCC Bug: Please send mail to sankar@cs.stanford.edu");

    dumped[stateName] = true;

    if (byteNum >= 0)
    {
      if (asciiMoves[byteNum] != 0L)
      {
        codeGenerator.genCodeLine ("               case " + stateName + ":");
        _dumpAsciiMoveForCompositeState (codeGenerator, byteNum, false);
        return "";
      }
    }
    else
      if (m_nonAsciiMethod != -1)
      {
        codeGenerator.genCodeLine ("               case " + stateName + ":");
        _dumpNonAsciiMoveForCompositeState (codeGenerator);
        return "";
      }

    return ("               case " + stateName + ":\n");
  }

  private static void _dumpCompositeStatesAsciiMoves (final CodeGenerator codeGenerator,
                                                      final String key,
                                                      final int byteNum,
                                                      final boolean [] dumped)
  {
    final int [] nameSet = s_allNextStates.get (key);

    if (nameSet.length == 1 || dumped[_stateNameForComposite (key)])
      return;

    NfaState toBePrinted = null;
    int neededStates = 0;
    NfaState stateForCase = null;
    String toPrint = "";
    final boolean stateBlock = (s_stateBlockTable.get (key) != null);

    for (final int aElement : nameSet)
    {
      final NfaState tmp = s_allStates.get (aElement);

      if (tmp.asciiMoves[byteNum] != 0L)
      {
        if (neededStates++ == 1)
          break;
        toBePrinted = tmp;
      }
      else
        dumped[tmp.stateName] = true;

      if (tmp.m_stateForCase != null)
      {
        if (stateForCase != null)
          throw new Error ("JavaCC Bug: Please send mail to sankar@cs.stanford.edu : ");

        stateForCase = tmp.m_stateForCase;
      }
    }

    if (stateForCase != null)
      toPrint = stateForCase._printNoBreak (codeGenerator, byteNum, dumped);

    if (neededStates == 0)
    {
      if (stateForCase != null && toPrint.equals (""))
        codeGenerator.genCodeLine ("                  break;");
      return;
    }

    if (neededStates == 1)
    {
      // if (byteNum == 1)
      // System.out.println(toBePrinted.stateName + " is the only state for "
      // + key + " ; and key is : " + StateNameForComposite(key));

      if (!toPrint.equals (""))
        codeGenerator.genCode (toPrint);

      codeGenerator.genCodeLine ("               case " + _stateNameForComposite (key) + ":");

      if (!dumped[toBePrinted.stateName] && !stateBlock && toBePrinted.inNextOf > 1)
        codeGenerator.genCodeLine ("               case " + toBePrinted.stateName + ":");

      dumped[toBePrinted.stateName] = true;
      toBePrinted._dumpAsciiMove (codeGenerator, byteNum, dumped);
      return;
    }

    final List <List <NfaState>> partition = _partitionStatesSetForAscii (nameSet, byteNum);

    if (!toPrint.equals (""))
      codeGenerator.genCode (toPrint);

    final int keyState = _stateNameForComposite (key);
    codeGenerator.genCodeLine ("               case " + keyState + ":");
    if (keyState < s_generatedStates)
      dumped[keyState] = true;

    for (int i = 0; i < partition.size (); i++)
    {
      final List <NfaState> subSet = partition.get (i);
      int nIndex = 0;
      for (final NfaState tmp : subSet)
      {
        if (stateBlock)
          dumped[tmp.stateName] = true;
        tmp._dumpAsciiMoveForCompositeState (codeGenerator, byteNum, nIndex != 0);
        ++nIndex;
      }
    }

    if (stateBlock)
      codeGenerator.genCodeLine ("                  break;");
    else
      codeGenerator.genCodeLine ("                  break;");
  }

  private boolean _selfLoop ()
  {
    if (next == null || next.m_epsilonMovesString == null)
      return false;

    final int [] set = s_allNextStates.get (next.m_epsilonMovesString);
    return _elemOccurs (stateName, set) >= 0;
  }

  private void _dumpAsciiMoveForCompositeState (final CodeGenerator codeGenerator,
                                                final int byteNum,
                                                final boolean elseNeeded)
  {
    boolean nextIntersects = _selfLoop ();

    for (int j = 0; j < s_allStates.size (); j++)
    {
      final NfaState temp1 = s_allStates.get (j);

      if (this == temp1 ||
          temp1.stateName == -1 ||
          temp1.dummy ||
          stateName == temp1.stateName ||
          temp1.asciiMoves[byteNum] == 0L)
        continue;

      if (!nextIntersects && _intersect (temp1.next.m_epsilonMovesString, next.m_epsilonMovesString))
      {
        nextIntersects = true;
        break;
      }
    }

    // System.out.println(stateName + " \'s nextIntersects : " +
    // nextIntersects);
    String prefix = "";
    if (asciiMoves[byteNum] != 0xffffffffffffffffL)
    {
      final int oneBit = onlyOneBitSet (asciiMoves[byteNum]);

      if (oneBit != -1)
        codeGenerator.genCodeLine ("                  " +
                                   (elseNeeded ? "else " : "") +
                                   "if (curChar == " +
                                   (64 * byteNum + oneBit) +
                                   ")");
      else
        codeGenerator.genCodeLine ("                  " +
                                   (elseNeeded ? "else " : "") +
                                   "if ((0x" +
                                   Long.toHexString (asciiMoves[byteNum]) +
                                   "L & l) != 0L)");
      prefix = "   ";
    }

    if (m_kindToPrint != Integer.MAX_VALUE)
    {
      if (asciiMoves[byteNum] != 0xffffffffffffffffL)
      {
        codeGenerator.genCodeLine ("                  {");
      }

      codeGenerator.genCodeLine (prefix + "                  if (kind > " + m_kindToPrint + ")");
      codeGenerator.genCodeLine (prefix + "                     kind = " + m_kindToPrint + ";");
    }

    if (next != null && next.m_usefulEpsilonMoves > 0)
    {
      final int [] stateNames = s_allNextStates.get (next.m_epsilonMovesString);
      if (next.m_usefulEpsilonMoves == 1)
      {
        final int name = stateNames[0];

        if (nextIntersects)
          codeGenerator.genCodeLine (prefix + "                  { jjCheckNAdd(" + name + "); }");
        else
          codeGenerator.genCodeLine (prefix + "                  jjstateSet[jjnewStateCnt++] = " + name + ";");
      }
      else
        if (next.m_usefulEpsilonMoves == 2 && nextIntersects)
        {
          codeGenerator.genCodeLine (prefix +
                                     "                  { jjCheckNAddTwoStates(" +
                                     stateNames[0] +
                                     ", " +
                                     stateNames[1] +
                                     "); }");
        }
        else
        {
          final int [] indices = _getStateSetIndicesForUse (next.m_epsilonMovesString);
          final boolean notTwo = (indices[0] + 1 != indices[1]);

          if (nextIntersects)
          {
            codeGenerator.genCode (prefix + "                  { jjCheckNAddStates(" + indices[0]);
            if (notTwo)
            {
              s_jjCheckNAddStatesDualNeeded = true;
              codeGenerator.genCode (", " + indices[1]);
            }
            else
            {
              s_jjCheckNAddStatesUnaryNeeded = true;
            }
            codeGenerator.genCodeLine ("); }");
          }
          else
            codeGenerator.genCodeLine (prefix +
                                       "                  { jjAddStates(" +
                                       indices[0] +
                                       ", " +
                                       indices[1] +
                                       "); }");
        }
    }

    if (asciiMoves[byteNum] != 0xffffffffffffffffL && m_kindToPrint != Integer.MAX_VALUE)
      codeGenerator.genCodeLine ("                  }");
  }

  private void _dumpAsciiMove (final CodeGenerator codeGenerator, final int byteNum, final boolean dumped[])
  {
    boolean nextIntersects = _selfLoop () && m_isComposite;
    boolean onlyState = true;

    for (int j = 0; j < s_allStates.size (); j++)
    {
      final NfaState temp1 = s_allStates.get (j);

      if (this == temp1 ||
          temp1.stateName == -1 ||
          temp1.dummy ||
          stateName == temp1.stateName ||
          temp1.asciiMoves[byteNum] == 0L)
        continue;

      if (onlyState && (asciiMoves[byteNum] & temp1.asciiMoves[byteNum]) != 0L)
        onlyState = false;

      if (!nextIntersects && _intersect (temp1.next.m_epsilonMovesString, next.m_epsilonMovesString))
        nextIntersects = true;

      if (!dumped[temp1.stateName] &&
          !temp1.m_isComposite &&
          asciiMoves[byteNum] == temp1.asciiMoves[byteNum] &&
          m_kindToPrint == temp1.m_kindToPrint &&
          (next.m_epsilonMovesString == temp1.next.m_epsilonMovesString ||
           (next.m_epsilonMovesString != null &&
            temp1.next.m_epsilonMovesString != null &&
            next.m_epsilonMovesString.equals (temp1.next.m_epsilonMovesString))))
      {
        dumped[temp1.stateName] = true;
        codeGenerator.genCodeLine ("               case " + temp1.stateName + ":");
      }
    }

    // if (onlyState)
    // nextIntersects = false;

    final int oneBit = onlyOneBitSet (asciiMoves[byteNum]);
    if (asciiMoves[byteNum] != 0xffffffffffffffffL)
    {
      if ((next == null || next.m_usefulEpsilonMoves == 0) && m_kindToPrint != Integer.MAX_VALUE)
      {
        String kindCheck = "";

        if (!onlyState)
          kindCheck = " && kind > " + m_kindToPrint;

        if (oneBit != -1)
          codeGenerator.genCodeLine ("                  if (curChar == " + (64 * byteNum + oneBit) + kindCheck + ")");
        else
          codeGenerator.genCodeLine ("                  if ((0x" +
                                     Long.toHexString (asciiMoves[byteNum]) +
                                     "L & l) != 0L" +
                                     kindCheck +
                                     ")");

        codeGenerator.genCodeLine ("                     kind = " + m_kindToPrint + ";");

        if (onlyState)
          codeGenerator.genCodeLine ("                  break;");
        else
          codeGenerator.genCodeLine ("                  break;");

        return;
      }
    }

    String prefix = "";
    if (m_kindToPrint != Integer.MAX_VALUE)
    {

      if (oneBit != -1)
      {
        codeGenerator.genCodeLine ("                  if (curChar != " + (64 * byteNum + oneBit) + ")");
        codeGenerator.genCodeLine ("                     break;");
      }
      else
        if (asciiMoves[byteNum] != 0xffffffffffffffffL)
        {
          codeGenerator.genCodeLine ("                  if ((0x" +
                                     Long.toHexString (asciiMoves[byteNum]) +
                                     "L & l) == 0L)");
          codeGenerator.genCodeLine ("                     break;");
        }

      if (onlyState)
      {
        codeGenerator.genCodeLine ("                  kind = " + m_kindToPrint + ";");
      }
      else
      {
        codeGenerator.genCodeLine ("                  if (kind > " + m_kindToPrint + ")");
        codeGenerator.genCodeLine ("                     kind = " + m_kindToPrint + ";");
      }
    }
    else
    {
      if (oneBit != -1)
      {
        codeGenerator.genCodeLine ("                  if (curChar == " + (64 * byteNum + oneBit) + ")");
        prefix = "   ";
      }
      else
        if (asciiMoves[byteNum] != 0xffffffffffffffffL)
        {
          codeGenerator.genCodeLine ("                  if ((0x" +
                                     Long.toHexString (asciiMoves[byteNum]) +
                                     "L & l) != 0L)");
          prefix = "   ";
        }
    }

    if (next != null && next.m_usefulEpsilonMoves > 0)
    {
      final int [] stateNames = s_allNextStates.get (next.m_epsilonMovesString);
      if (next.m_usefulEpsilonMoves == 1)
      {
        final int name = stateNames[0];
        if (nextIntersects)
          codeGenerator.genCodeLine (prefix + "                  { jjCheckNAdd(" + name + "); }");
        else
          codeGenerator.genCodeLine (prefix + "                  jjstateSet[jjnewStateCnt++] = " + name + ";");
      }
      else
        if (next.m_usefulEpsilonMoves == 2 && nextIntersects)
        {
          codeGenerator.genCodeLine (prefix +
                                     "                  { jjCheckNAddTwoStates(" +
                                     stateNames[0] +
                                     ", " +
                                     stateNames[1] +
                                     "); }");
        }
        else
        {
          final int [] indices = _getStateSetIndicesForUse (next.m_epsilonMovesString);
          final boolean notTwo = (indices[0] + 1 != indices[1]);

          if (nextIntersects)
          {
            codeGenerator.genCode (prefix + "                  { jjCheckNAddStates(" + indices[0]);
            if (notTwo)
            {
              s_jjCheckNAddStatesDualNeeded = true;
              codeGenerator.genCode (", " + indices[1]);
            }
            else
            {
              s_jjCheckNAddStatesUnaryNeeded = true;
            }
            codeGenerator.genCodeLine ("); }");
          }
          else
            codeGenerator.genCodeLine (prefix +
                                       "                  { jjAddStates(" +
                                       indices[0] +
                                       ", " +
                                       indices[1] +
                                       "); }");
        }
    }

    if (onlyState)
      codeGenerator.genCodeLine ("                  break;");
    else
      codeGenerator.genCodeLine ("                  break;");
  }

  private static void _dumpAsciiMoves (final CodeGenerator codeGenerator, final int byteNum)
  {
    final boolean [] dumped = new boolean [Math.max (s_generatedStates, s_dummyStateIndex + 1)];

    _dumpHeadForCase (codeGenerator, byteNum);

    for (final String s : s_compositeStateTable.keySet ())
      _dumpCompositeStatesAsciiMoves (codeGenerator, s, byteNum, dumped);

    for (int i = 0; i < s_allStates.size (); i++)
    {
      final NfaState temp = s_allStates.get (i);

      if (dumped[temp.stateName] ||
          temp.m_lexState != LexGenJava.lexStateIndex ||
          !temp.hasTransitions () ||
          temp.dummy ||
          temp.stateName == -1)
        continue;

      String toPrint = "";

      if (temp.m_stateForCase != null)
      {
        if (temp.inNextOf == 1)
          continue;

        if (dumped[temp.m_stateForCase.stateName])
          continue;

        toPrint = (temp.m_stateForCase._printNoBreak (codeGenerator, byteNum, dumped));

        if (temp.asciiMoves[byteNum] == 0L)
        {
          if (toPrint.equals (""))
            codeGenerator.genCodeLine ("                  break;");

          continue;
        }
      }

      if (temp.asciiMoves[byteNum] == 0L)
        continue;

      if (!toPrint.equals (""))
        codeGenerator.genCode (toPrint);

      dumped[temp.stateName] = true;
      codeGenerator.genCodeLine ("               case " + temp.stateName + ":");
      temp._dumpAsciiMove (codeGenerator, byteNum, dumped);
    }

    if (byteNum != 0 && byteNum != 1)
    {
      codeGenerator.genCodeLine ("               default : if (i1 == 0 || l1 == 0 || i2 == 0 ||  l2 == 0) break; else break;");
    }
    else
    {
      codeGenerator.genCodeLine ("               default : break;");
    }

    codeGenerator.genCodeLine ("            }");
    codeGenerator.genCodeLine ("         } while(i != startsAt);");
  }

  private static void _dumpCompositeStatesNonAsciiMoves (final CodeGenerator codeGenerator,
                                                         final String key,
                                                         final boolean [] dumped)
  {
    final int [] nameSet = s_allNextStates.get (key);

    if (nameSet.length == 1 || dumped[_stateNameForComposite (key)])
      return;

    NfaState toBePrinted = null;
    int neededStates = 0;
    NfaState tmp;
    NfaState stateForCase = null;
    String toPrint = "";
    final boolean stateBlock = (s_stateBlockTable.get (key) != null);

    for (final int aElement : nameSet)
    {
      tmp = s_allStates.get (aElement);

      if (tmp.m_nonAsciiMethod != -1)
      {
        if (neededStates++ == 1)
          break;
        toBePrinted = tmp;
      }
      else
        dumped[tmp.stateName] = true;

      if (tmp.m_stateForCase != null)
      {
        if (stateForCase != null)
          throw new Error ("JavaCC Bug: Please send mail to sankar@cs.stanford.edu : ");

        stateForCase = tmp.m_stateForCase;
      }
    }

    if (stateForCase != null)
      toPrint = stateForCase._printNoBreak (codeGenerator, -1, dumped);

    if (neededStates == 0)
    {
      if (stateForCase != null && toPrint.equals (""))
        codeGenerator.genCodeLine ("                  break;");

      return;
    }

    if (neededStates == 1)
    {
      if (!toPrint.equals (""))
        codeGenerator.genCode (toPrint);

      codeGenerator.genCodeLine ("               case " + _stateNameForComposite (key) + ":");

      if (!dumped[toBePrinted.stateName] && !stateBlock && toBePrinted.inNextOf > 1)
        codeGenerator.genCodeLine ("               case " + toBePrinted.stateName + ":");

      dumped[toBePrinted.stateName] = true;
      toBePrinted._dumpNonAsciiMove (codeGenerator, dumped);
      return;
    }

    if (!toPrint.equals (""))
      codeGenerator.genCode (toPrint);

    final int keyState = _stateNameForComposite (key);
    codeGenerator.genCodeLine ("               case " + keyState + ":");
    if (keyState < s_generatedStates)
      dumped[keyState] = true;

    for (final int aElement : nameSet)
    {
      tmp = s_allStates.get (aElement);

      if (tmp.m_nonAsciiMethod != -1)
      {
        if (stateBlock)
          dumped[tmp.stateName] = true;
        tmp._dumpNonAsciiMoveForCompositeState (codeGenerator);
      }
    }

    if (stateBlock)
      codeGenerator.genCodeLine ("                  break;");
    else
      codeGenerator.genCodeLine ("                  break;");
  }

  private final void _dumpNonAsciiMoveForCompositeState (final CodeGenerator codeGenerator)
  {
    boolean nextIntersects = _selfLoop ();
    for (int j = 0; j < s_allStates.size (); j++)
    {
      final NfaState temp1 = s_allStates.get (j);

      if (this == temp1 ||
          temp1.stateName == -1 ||
          temp1.dummy ||
          stateName == temp1.stateName ||
          (temp1.m_nonAsciiMethod == -1))
        continue;

      if (!nextIntersects && _intersect (temp1.next.m_epsilonMovesString, next.m_epsilonMovesString))
      {
        nextIntersects = true;
        break;
      }
    }

    if (!Options.getJavaUnicodeEscape () && !s_unicodeWarningGiven)
    {
      if (m_loByteVec != null && m_loByteVec.size () > 1)
        codeGenerator.genCodeLine ("                  if ((jjbitVec" +
                                   m_loByteVec.get (1).intValue () +
                                   "[i2" +
                                   "] & l2) != 0L)");
    }
    else
    {
      codeGenerator.genCodeLine ("                  if (jjCanMove_" + m_nonAsciiMethod + "(hiByte, i1, i2, l1, l2))");
    }

    if (m_kindToPrint != Integer.MAX_VALUE)
    {
      codeGenerator.genCodeLine ("                  {");
      codeGenerator.genCodeLine ("                     if (kind > " + m_kindToPrint + ")");
      codeGenerator.genCodeLine ("                        kind = " + m_kindToPrint + ";");
    }

    if (next != null && next.m_usefulEpsilonMoves > 0)
    {
      final int [] stateNames = s_allNextStates.get (next.m_epsilonMovesString);
      if (next.m_usefulEpsilonMoves == 1)
      {
        final int name = stateNames[0];
        if (nextIntersects)
          codeGenerator.genCodeLine ("                     { jjCheckNAdd(" + name + "); }");
        else
          codeGenerator.genCodeLine ("                     jjstateSet[jjnewStateCnt++] = " + name + ";");
      }
      else
        if (next.m_usefulEpsilonMoves == 2 && nextIntersects)
        {
          codeGenerator.genCodeLine ("                     { jjCheckNAddTwoStates(" +
                                     stateNames[0] +
                                     ", " +
                                     stateNames[1] +
                                     "); }");
        }
        else
        {
          final int [] indices = _getStateSetIndicesForUse (next.m_epsilonMovesString);
          final boolean notTwo = (indices[0] + 1 != indices[1]);

          if (nextIntersects)
          {
            codeGenerator.genCode ("                     { jjCheckNAddStates(" + indices[0]);
            if (notTwo)
            {
              s_jjCheckNAddStatesDualNeeded = true;
              codeGenerator.genCode (", " + indices[1]);
            }
            else
            {
              s_jjCheckNAddStatesUnaryNeeded = true;
            }
            codeGenerator.genCodeLine ("); }");
          }
          else
            codeGenerator.genCodeLine ("                     { jjAddStates(" + indices[0] + ", " + indices[1] + "); }");
        }
    }

    if (m_kindToPrint != Integer.MAX_VALUE)
      codeGenerator.genCodeLine ("                  }");
  }

  private final void _dumpNonAsciiMove (final CodeGenerator codeGenerator, final boolean dumped[])
  {
    boolean nextIntersects = _selfLoop () && m_isComposite;

    for (int j = 0; j < s_allStates.size (); j++)
    {
      final NfaState temp1 = s_allStates.get (j);

      if (this == temp1 ||
          temp1.stateName == -1 ||
          temp1.dummy ||
          stateName == temp1.stateName ||
          (temp1.m_nonAsciiMethod == -1))
        continue;

      if (!nextIntersects && _intersect (temp1.next.m_epsilonMovesString, next.m_epsilonMovesString))
        nextIntersects = true;

      if (!dumped[temp1.stateName] &&
          !temp1.m_isComposite &&
          m_nonAsciiMethod == temp1.m_nonAsciiMethod &&
          m_kindToPrint == temp1.m_kindToPrint &&
          (next.m_epsilonMovesString == temp1.next.m_epsilonMovesString ||
           (next.m_epsilonMovesString != null &&
            temp1.next.m_epsilonMovesString != null &&
            next.m_epsilonMovesString.equals (temp1.next.m_epsilonMovesString))))
      {
        dumped[temp1.stateName] = true;
        codeGenerator.genCodeLine ("               case " + temp1.stateName + ":");
      }
    }

    if (next == null || next.m_usefulEpsilonMoves <= 0)
    {
      final String kindCheck = " && kind > " + m_kindToPrint;

      if (!Options.getJavaUnicodeEscape () && !s_unicodeWarningGiven)
      {
        if (m_loByteVec != null && m_loByteVec.size () > 1)
          codeGenerator.genCodeLine ("                  if ((jjbitVec" +
                                     m_loByteVec.get (1).intValue () +
                                     "[i2" +
                                     "] & l2) != 0L" +
                                     kindCheck +
                                     ")");
      }
      else
      {
        codeGenerator.genCodeLine ("                  if (jjCanMove_" +
                                   m_nonAsciiMethod +
                                   "(hiByte, i1, i2, l1, l2)" +
                                   kindCheck +
                                   ")");
      }
      codeGenerator.genCodeLine ("                     kind = " + m_kindToPrint + ";");
      codeGenerator.genCodeLine ("                  break;");
      return;
    }

    String prefix = "   ";
    if (m_kindToPrint != Integer.MAX_VALUE)
    {
      if (!Options.getJavaUnicodeEscape () && !s_unicodeWarningGiven)
      {
        if (m_loByteVec != null && m_loByteVec.size () > 1)
        {
          codeGenerator.genCodeLine ("                  if ((jjbitVec" +
                                     m_loByteVec.get (1).intValue () +
                                     "[i2" +
                                     "] & l2) == 0L)");
          codeGenerator.genCodeLine ("                     break;");
        }
      }
      else
      {
        codeGenerator.genCodeLine ("                  if (!jjCanMove_" +
                                   m_nonAsciiMethod +
                                   "(hiByte, i1, i2, l1, l2))");
        codeGenerator.genCodeLine ("                     break;");
      }

      codeGenerator.genCodeLine ("                  if (kind > " + m_kindToPrint + ")");
      codeGenerator.genCodeLine ("                     kind = " + m_kindToPrint + ";");
      prefix = "";
    }
    else
      if (!Options.getJavaUnicodeEscape () && !s_unicodeWarningGiven)
      {
        if (m_loByteVec != null && m_loByteVec.size () > 1)
          codeGenerator.genCodeLine ("                  if ((jjbitVec" +
                                     m_loByteVec.get (1).intValue () +
                                     "[i2" +
                                     "] & l2) != 0L)");
      }
      else
      {
        codeGenerator.genCodeLine ("                  if (jjCanMove_" + m_nonAsciiMethod + "(hiByte, i1, i2, l1, l2))");
      }

    if (next != null && next.m_usefulEpsilonMoves > 0)
    {
      final int [] stateNames = s_allNextStates.get (next.m_epsilonMovesString);
      if (next.m_usefulEpsilonMoves == 1)
      {
        final int name = stateNames[0];
        if (nextIntersects)
          codeGenerator.genCodeLine (prefix + "                  { jjCheckNAdd(" + name + "); }");
        else
          codeGenerator.genCodeLine (prefix + "                  jjstateSet[jjnewStateCnt++] = " + name + ";");
      }
      else
        if (next.m_usefulEpsilonMoves == 2 && nextIntersects)
        {
          codeGenerator.genCodeLine (prefix +
                                     "                  { jjCheckNAddTwoStates(" +
                                     stateNames[0] +
                                     ", " +
                                     stateNames[1] +
                                     "); }");
        }
        else
        {
          final int [] indices = _getStateSetIndicesForUse (next.m_epsilonMovesString);
          final boolean notTwo = (indices[0] + 1 != indices[1]);

          if (nextIntersects)
          {
            codeGenerator.genCode (prefix + "                  { jjCheckNAddStates(" + indices[0]);
            if (notTwo)
            {
              s_jjCheckNAddStatesDualNeeded = true;
              codeGenerator.genCode (", " + indices[1]);
            }
            else
            {
              s_jjCheckNAddStatesUnaryNeeded = true;
            }
            codeGenerator.genCodeLine ("); }");
          }
          else
            codeGenerator.genCodeLine (prefix +
                                       "                  { jjAddStates(" +
                                       indices[0] +
                                       ", " +
                                       indices[1] +
                                       "); }");
        }
    }

    codeGenerator.genCodeLine ("                  break;");
  }

  public static void dumpCharAndRangeMoves (final CodeGenerator codeGenerator)
  {
    final boolean [] dumped = new boolean [Math.max (s_generatedStates, s_dummyStateIndex + 1)];

    _dumpHeadForCase (codeGenerator, -1);

    for (final String s : s_compositeStateTable.keySet ())
      _dumpCompositeStatesNonAsciiMoves (codeGenerator, s, dumped);

    for (final NfaState temp : s_allStates)
    {
      if (temp.stateName == -1 ||
          dumped[temp.stateName] ||
          temp.m_lexState != LexGenJava.lexStateIndex ||
          !temp.hasTransitions () ||
          temp.dummy)
        continue;

      String toPrint = "";

      if (temp.m_stateForCase != null)
      {
        if (temp.inNextOf == 1)
          continue;

        if (dumped[temp.m_stateForCase.stateName])
          continue;

        toPrint = (temp.m_stateForCase._printNoBreak (codeGenerator, -1, dumped));

        if (temp.m_nonAsciiMethod == -1)
        {
          if (toPrint.equals (""))
            codeGenerator.genCodeLine ("                  break;");

          continue;
        }
      }

      if (temp.m_nonAsciiMethod == -1)
        continue;

      if (!toPrint.equals (""))
        codeGenerator.genCode (toPrint);

      dumped[temp.stateName] = true;
      // System.out.println("case : " + temp.stateName);
      codeGenerator.genCodeLine ("               case " + temp.stateName + ":");
      temp._dumpNonAsciiMove (codeGenerator, dumped);
    }

    if (Options.getJavaUnicodeEscape () || s_unicodeWarningGiven)
    {
      codeGenerator.genCodeLine ("               default : if (i1 == 0 || l1 == 0 || i2 == 0 ||  l2 == 0) break; else break;");
    }
    else
    {
      codeGenerator.genCodeLine ("               default : break;");
    }
    codeGenerator.genCodeLine ("            }");
    codeGenerator.genCodeLine ("         } while(i != startsAt);");
  }

  public static void dumpNonAsciiMoveMethods (final CodeGenerator codeGenerator)
  {
    if (!Options.getJavaUnicodeEscape () && !s_unicodeWarningGiven)
      return;

    if (s_nonAsciiTableForMethod.size () <= 0)
      return;

    for (int i = 0; i < s_nonAsciiTableForMethod.size (); i++)
    {
      final NfaState tmp = s_nonAsciiTableForMethod.get (i);
      tmp.dumpNonAsciiMoveMethod (codeGenerator);
    }
  }

  void dumpNonAsciiMoveMethod (final CodeGenerator codeGenerator)
  {
    int j;
    switch (codeGenerator.getOutputLanguage ())
    {
      case JAVA:
        codeGenerator.genCodeLine ("private static final " +
                                   Options.getBooleanType () +
                                   " jjCanMove_" +
                                   m_nonAsciiMethod +
                                   "(int hiByte, int i1, int i2, " +
                                   Options.getLongType () +
                                   " l1, " +
                                   Options.getLongType () +
                                   " l2)");
        break;
      case CPP:
        codeGenerator.generateMethodDefHeader ("" + Options.getBooleanType () + "",
                                               LexGenJava.tokMgrClassName,
                                               "jjCanMove_" +
                                                                           m_nonAsciiMethod +
                                                                           "(int hiByte, int i1, int i2, " +
                                                                           Options.getLongType () +
                                                                           " l1, " +
                                                                           Options.getLongType () +
                                                                           " l2)");
        break;
      default:
        throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
    }
    codeGenerator.genCodeLine ("{");
    codeGenerator.genCodeLine ("   switch(hiByte)");
    codeGenerator.genCodeLine ("   {");

    if (m_loByteVec != null && m_loByteVec.size () > 0)
    {
      for (j = 0; j < m_loByteVec.size (); j += 2)
      {
        codeGenerator.genCodeLine ("      case " + m_loByteVec.get (j).intValue () + ":");
        if (!allBitsSet (allBitVectors.get (m_loByteVec.get (j + 1).intValue ())))
        {
          codeGenerator.genCodeLine ("         return ((jjbitVec" +
                                     m_loByteVec.get (j + 1).intValue () +
                                     "[i2" +
                                     "] & l2) != 0L);");
        }
        else
          codeGenerator.genCodeLine ("            return true;");
      }
    }

    codeGenerator.genCodeLine ("      default :");

    if (m_nonAsciiMoveIndices != null && (j = m_nonAsciiMoveIndices.length) > 0)
    {
      do
      {
        if (!allBitsSet (allBitVectors.get (m_nonAsciiMoveIndices[j - 2])))
          codeGenerator.genCodeLine ("         if ((jjbitVec" + m_nonAsciiMoveIndices[j - 2] + "[i1] & l1) != 0L)");
        if (!allBitsSet (allBitVectors.get (m_nonAsciiMoveIndices[j - 1])))
        {
          codeGenerator.genCodeLine ("            if ((jjbitVec" + m_nonAsciiMoveIndices[j - 1] + "[i2] & l2) == 0L)");
          codeGenerator.genCodeLine ("               return false;");
          codeGenerator.genCodeLine ("            else");
        }
        codeGenerator.genCodeLine ("            return true;");
      } while ((j -= 2) > 0);
    }

    codeGenerator.genCodeLine ("         return false;");
    codeGenerator.genCodeLine ("   }");
    codeGenerator.genCodeLine ("}");
  }

  private static void _reArrange ()
  {
    final List <NfaState> v = s_allStates;
    s_allStates = new ArrayList <> (Collections.nCopies (s_generatedStates, null));

    if (s_allStates.size () != s_generatedStates)
      throw new Error ("What??");

    for (int j = 0; j < v.size (); j++)
    {
      final NfaState tmp = v.get (j);
      if (tmp.stateName != -1 && !tmp.dummy)
        s_allStates.set (tmp.stateName, tmp);
    }
  }

  // private static boolean boilerPlateDumped = false;
  static void printBoilerPlateJava (final CodeGenerator codeGenerator)
  {
    codeGenerator.genCodeLine ((Options.getStatic () ? "static " : "") + "private void " + "jjCheckNAdd(int state)");
    codeGenerator.genCodeLine ("{");
    codeGenerator.genCodeLine ("   if (jjrounds[state] != jjround)");
    codeGenerator.genCodeLine ("   {");
    codeGenerator.genCodeLine ("      jjstateSet[jjnewStateCnt++] = state;");
    codeGenerator.genCodeLine ("      jjrounds[state] = jjround;");
    codeGenerator.genCodeLine ("   }");
    codeGenerator.genCodeLine ("}");

    codeGenerator.genCodeLine ((Options.getStatic () ? "static " : "") +
                               "private void " +
                               "jjAddStates(int start, int end)");
    codeGenerator.genCodeLine ("{");
    codeGenerator.genCodeLine ("   do {");
    codeGenerator.genCodeLine ("      jjstateSet[jjnewStateCnt++] = jjnextStates[start];");
    codeGenerator.genCodeLine ("   } while (start++ != end);");
    codeGenerator.genCodeLine ("}");

    codeGenerator.genCodeLine ((Options.getStatic () ? "static " : "") +
                               "private void " +
                               "jjCheckNAddTwoStates(int state1, int state2)");
    codeGenerator.genCodeLine ("{");
    codeGenerator.genCodeLine ("   jjCheckNAdd(state1);");
    codeGenerator.genCodeLine ("   jjCheckNAdd(state2);");
    codeGenerator.genCodeLine ("}");
    codeGenerator.genCodeLine ("");

    if (s_jjCheckNAddStatesDualNeeded)
    {
      codeGenerator.genCodeLine ((Options.getStatic () ? "static " : "") +
                                 "private void " +
                                 "jjCheckNAddStates(int start, int end)");
      codeGenerator.genCodeLine ("{");
      codeGenerator.genCodeLine ("   do {");
      codeGenerator.genCodeLine ("      jjCheckNAdd(jjnextStates[start]);");
      codeGenerator.genCodeLine ("   } while (start++ != end);");
      codeGenerator.genCodeLine ("}");
      codeGenerator.genCodeLine ("");
    }

    if (s_jjCheckNAddStatesUnaryNeeded)
    {
      codeGenerator.genCodeLine ((Options.getStatic () ? "static " : "") +
                                 "private void " +
                                 "jjCheckNAddStates(int start)");
      codeGenerator.genCodeLine ("{");
      codeGenerator.genCodeLine ("   jjCheckNAdd(jjnextStates[start]);");
      codeGenerator.genCodeLine ("   jjCheckNAdd(jjnextStates[start + 1]);");
      codeGenerator.genCodeLine ("}");
      codeGenerator.genCodeLine ("");
    }
  }

  // private static boolean boilerPlateDumped = false;
  static void printBoilerPlateCPP (final CodeGenerator codeGenerator)
  {
    codeGenerator.switchToIncludeFile ();
    codeGenerator.genCodeLine ("#define jjCheckNAdd(state)\\");
    codeGenerator.genCodeLine ("{\\");
    codeGenerator.genCodeLine ("   if (jjrounds[state] != jjround)\\");
    codeGenerator.genCodeLine ("   {\\");
    codeGenerator.genCodeLine ("      jjstateSet[jjnewStateCnt++] = state;\\");
    codeGenerator.genCodeLine ("      jjrounds[state] = jjround;\\");
    codeGenerator.genCodeLine ("   }\\");
    codeGenerator.genCodeLine ("}");

    codeGenerator.genCodeLine ("#define jjAddStates(start, end)\\");
    codeGenerator.genCodeLine ("{\\");
    codeGenerator.genCodeLine ("   for (int x = start; x <= end; x++) {\\");
    codeGenerator.genCodeLine ("      jjstateSet[jjnewStateCnt++] = jjnextStates[x];\\");
    codeGenerator.genCodeLine ("   } /*while (start++ != end);*/\\");
    codeGenerator.genCodeLine ("}");

    codeGenerator.genCodeLine ("#define jjCheckNAddTwoStates(state1, state2)\\");
    codeGenerator.genCodeLine ("{\\");
    codeGenerator.genCodeLine ("   jjCheckNAdd(state1);\\");
    codeGenerator.genCodeLine ("   jjCheckNAdd(state2);\\");
    codeGenerator.genCodeLine ("}");
    codeGenerator.genCodeLine ("");

    if (s_jjCheckNAddStatesDualNeeded)
    {
      codeGenerator.genCodeLine ("#define jjCheckNAddStates(start, end)\\");
      codeGenerator.genCodeLine ("{\\");
      codeGenerator.genCodeLine ("   for (int x = start; x <= end; x++) {\\");
      codeGenerator.genCodeLine ("      jjCheckNAdd(jjnextStates[x]);\\");
      codeGenerator.genCodeLine ("   } /*while (start++ != end);*/\\");
      codeGenerator.genCodeLine ("}");
      codeGenerator.genCodeLine ("");
    }

    if (s_jjCheckNAddStatesUnaryNeeded)
    {
      codeGenerator.genCodeLine ("#define jjCheckNAddStates(start)\\");
      codeGenerator.genCodeLine ("{\\");
      codeGenerator.genCodeLine ("   jjCheckNAdd(jjnextStates[start]);\\");
      codeGenerator.genCodeLine ("   jjCheckNAdd(jjnextStates[start + 1]);\\");
      codeGenerator.genCodeLine ("}");
      codeGenerator.genCodeLine ("");
    }
    codeGenerator.switchToMainFile ();
  }

  @SuppressWarnings ("unused")
  private static void _findStatesWithNoBreak ()
  {
    final Map <String, String> printed = new HashMap <> ();
    final boolean [] put = new boolean [s_generatedStates];
    int cnt = 0;
    int foundAt = 0;

    Outer: for (int j = 0; j < s_allStates.size (); j++)
    {
      NfaState stateForCase = null;
      final NfaState tmpState = s_allStates.get (j);

      if (tmpState.stateName == -1 ||
          tmpState.dummy ||
          !tmpState._isUsefulState () ||
          tmpState.next == null ||
          tmpState.next.m_usefulEpsilonMoves < 1)
        continue;

      final String s = tmpState.next.m_epsilonMovesString;

      if (s_compositeStateTable.get (s) != null || printed.get (s) != null)
        continue;

      printed.put (s, s);
      final int [] nexts = s_allNextStates.get (s);

      if (nexts.length == 1)
        continue;

      int state = cnt;
      // System.out.println("State " + tmpState.stateName + " : " + s);
      for (int i = 0; i < nexts.length; i++)
      {
        if ((state = nexts[i]) == -1)
          continue;

        final NfaState tmp = s_allStates.get (state);

        if (!tmp.m_isComposite && tmp.inNextOf == 1)
        {
          if (put[state])
            throw new Error ("JavaCC Bug: Please send mail to sankar@cs.stanford.edu");

          foundAt = i;
          cnt++;
          stateForCase = tmp;
          put[state] = true;

          // System.out.print(state + " : " + tmp.inNextOf + ", ");
          break;
        }
      }
      // System.out.println("");

      if (stateForCase == null)
        continue;

      for (int i = 0; i < nexts.length; i++)
      {
        if ((state = nexts[i]) == -1)
          continue;

        final NfaState tmp = s_allStates.get (state);

        if (!put[state] && tmp.inNextOf > 1 && !tmp.m_isComposite && tmp.m_stateForCase == null)
        {
          cnt++;
          nexts[i] = -1;
          put[state] = true;

          final int toSwap = nexts[0];
          nexts[0] = nexts[foundAt];
          nexts[foundAt] = toSwap;

          tmp.m_stateForCase = stateForCase;
          stateForCase.m_stateForCase = tmp;
          s_stateSetsToFix.put (s, nexts);

          // System.out.println("For : " + s + "; " + stateForCase.stateName +
          // " and " + tmp.stateName);

          continue Outer;
        }
      }

      for (final int aNext : nexts)
      {
        if ((state = aNext) == -1)
          continue;

        final NfaState tmp = s_allStates.get (state);
        if (tmp.inNextOf <= 1)
          put[state] = false;
      }
    }
  }

  static int [] [] kinds;
  static int [] [] [] statesForState;

  public static void dumpMoveNfa (final CodeGenerator codeGenerator)
  {
    // if (!boilerPlateDumped)
    // PrintBoilerPlate(codeGenerator);

    // boilerPlateDumped = true;
    int [] kindsForStates = null;

    if (kinds == null)
    {
      kinds = new int [LexGenJava.maxLexStates] [];
      statesForState = new int [LexGenJava.maxLexStates] [] [];
    }

    _reArrange ();

    for (int i = 0; i < s_allStates.size (); i++)
    {
      final NfaState temp = s_allStates.get (i);

      if (temp.m_lexState != LexGenJava.lexStateIndex || !temp.hasTransitions () || temp.dummy || temp.stateName == -1)
        continue;

      if (kindsForStates == null)
      {
        kindsForStates = new int [s_generatedStates];
        statesForState[LexGenJava.lexStateIndex] = new int [Math.max (s_generatedStates, s_dummyStateIndex + 1)] [];
      }

      kindsForStates[temp.stateName] = temp.m_lookingFor;
      statesForState[LexGenJava.lexStateIndex][temp.stateName] = temp.m_compositeStates;

      temp.generateNonAsciiMoves (codeGenerator);
    }

    for (final Map.Entry <String, Integer> aEntry : s_stateNameForComposite.entrySet ())
    {
      final String s = aEntry.getKey ();
      final int state = aEntry.getValue ().intValue ();

      if (state >= s_generatedStates)
        statesForState[LexGenJava.lexStateIndex][state] = s_allNextStates.get (s);
    }

    if (s_stateSetsToFix.size () != 0)
      _fixStateSets ();

    kinds[LexGenJava.lexStateIndex] = kindsForStates;

    switch (codeGenerator.getOutputLanguage ())
    {
      case JAVA:
        codeGenerator.genCodeLine ((Options.getStatic () ? "static " : "") +
                                   "private int " +
                                   "jjMoveNfa" +
                                   LexGenJava.lexStateSuffix +
                                   "(int startState, int curPos)");
        break;
      case CPP:
        codeGenerator.generateMethodDefHeader ("int",
                                               LexGenJava.tokMgrClassName,
                                               "jjMoveNfa" +
                                                                           LexGenJava.lexStateSuffix +
                                                                           "(int startState, int curPos)");
        break;
      default:
        throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
    }
    codeGenerator.genCodeLine ("{");
    if (s_generatedStates == 0)
    {
      codeGenerator.genCodeLine ("   return curPos;");
      codeGenerator.genCodeLine ("}");
      return;
    }

    if (LexGenJava.mixed[LexGenJava.lexStateIndex])
    {
      codeGenerator.genCodeLine ("   int strKind = jjmatchedKind;");
      codeGenerator.genCodeLine ("   int strPos = jjmatchedPos;");
      codeGenerator.genCodeLine ("   int seenUpto;");
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCodeLine ("   input_stream.backup(seenUpto = curPos + 1);");
          codeGenerator.genCodeLine ("   try { curChar = input_stream.readChar(); }");
          codeGenerator.genCodeLine ("   catch(java.io.IOException e) { throw new Error(\"Internal Error\"); }");
          break;
        case CPP:
          codeGenerator.genCodeLine ("   input_stream->backup(seenUpto = curPos + 1);");
          codeGenerator.genCodeLine ("   assert(!input_stream->endOfInput());");
          codeGenerator.genCodeLine ("   curChar = input_stream->readChar();");
          break;
        default:
          throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
      }
      codeGenerator.genCodeLine ("   curPos = 0;");
    }

    codeGenerator.genCodeLine ("   int startsAt = 0;");
    codeGenerator.genCodeLine ("   jjnewStateCnt = " + s_generatedStates + ";");
    codeGenerator.genCodeLine ("   int i = 1;");
    codeGenerator.genCodeLine ("   jjstateSet[0] = startState;");

    if (Options.getDebugTokenManager ())
    {
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCodeLine ("      debugStream.println(\"   Starting NFA to match one of : \" + " +
                                     "jjKindsForStateVector(curLexState, jjstateSet, 0, 1));");
          break;
        case CPP:
          codeGenerator.genCodeLine ("      fprintf(debugStream, \"   Starting NFA to match one of : %s\\n\", jjKindsForStateVector(curLexState, jjstateSet, 0, 1).c_str());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
      }
    }

    if (Options.getDebugTokenManager ())
    {
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCodeLine ("      debugStream.println(" +
                                     (LexGenJava.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + "
                                                                  : "") +
                                     "\"Current character : \" + " +
                                     Options.getTokenMgrErrorClass () +
                                     ".addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") " +
                                     "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
          break;
        case CPP:
          codeGenerator.genCodeLine ("   fprintf(debugStream, " +
                                     "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                                     "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, " +
                                     "input_stream->getEndLine(), input_stream->getEndColumn());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
      }
    }

    codeGenerator.genCodeLine ("   int kind = 0x" + Integer.toHexString (Integer.MAX_VALUE) + ";");
    codeGenerator.genCodeLine ("   for (;;)");
    codeGenerator.genCodeLine ("   {");
    codeGenerator.genCodeLine ("      if (++jjround == 0x" + Integer.toHexString (Integer.MAX_VALUE) + ")");
    codeGenerator.genCodeLine ("         ReInitRounds();");
    codeGenerator.genCodeLine ("      if (curChar < 64)");
    codeGenerator.genCodeLine ("      {");

    _dumpAsciiMoves (codeGenerator, 0);

    codeGenerator.genCodeLine ("      }");

    codeGenerator.genCodeLine ("      else if (curChar < 128)");

    codeGenerator.genCodeLine ("      {");

    _dumpAsciiMoves (codeGenerator, 1);

    codeGenerator.genCodeLine ("      }");

    codeGenerator.genCodeLine ("      else");
    codeGenerator.genCodeLine ("      {");

    dumpCharAndRangeMoves (codeGenerator);

    codeGenerator.genCodeLine ("      }");

    codeGenerator.genCodeLine ("      if (kind != 0x" + Integer.toHexString (Integer.MAX_VALUE) + ")");
    codeGenerator.genCodeLine ("      {");
    codeGenerator.genCodeLine ("         jjmatchedKind = kind;");
    codeGenerator.genCodeLine ("         jjmatchedPos = curPos;");
    codeGenerator.genCodeLine ("         kind = 0x" + Integer.toHexString (Integer.MAX_VALUE) + ";");
    codeGenerator.genCodeLine ("      }");
    codeGenerator.genCodeLine ("      ++curPos;");

    if (Options.getDebugTokenManager ())
    {
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCodeLine ("      if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                     Integer.toHexString (Integer.MAX_VALUE) +
                                     ")");
          codeGenerator.genCodeLine ("         debugStream.println(" +
                                     "\"   Currently matched the first \" + (jjmatchedPos + 1) + \" characters as" +
                                     " a \" + tokenImage[jjmatchedKind] + \" token.\");");
          break;
        case CPP:
          codeGenerator.genCodeLine ("      if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                     Integer.toHexString (Integer.MAX_VALUE) +
                                     ")");
          codeGenerator.genCodeLine ("   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
      }
    }

    switch (codeGenerator.getOutputLanguage ())
    {
      case JAVA:
        codeGenerator.genCodeLine ("      if ((i = jjnewStateCnt) == (startsAt = " +
                                   s_generatedStates +
                                   " - (jjnewStateCnt = startsAt)))");
        break;
      case CPP:
        codeGenerator.genCodeLine ("      if ((i = jjnewStateCnt), (jjnewStateCnt = startsAt), (i == (startsAt = " +
                                   s_generatedStates +
                                   " - startsAt)))");
        break;
      default:
        throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
    }
    if (LexGenJava.mixed[LexGenJava.lexStateIndex])
      codeGenerator.genCodeLine ("         break;");
    else
      codeGenerator.genCodeLine ("         return curPos;");

    if (Options.getDebugTokenManager ())
    {
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCodeLine ("      debugStream.println(\"   Possible kinds of longer matches : \" + " +
                                     "jjKindsForStateVector(curLexState, jjstateSet, startsAt, i));");
          break;
        case CPP:
          codeGenerator.genCodeLine ("      fprintf(debugStream, \"   Possible kinds of longer matches : %s\\n\", jjKindsForStateVector(curLexState, jjstateSet, startsAt, i).c_str());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
      }
    }

    switch (codeGenerator.getOutputLanguage ())
    {
      case JAVA:
        codeGenerator.genCodeLine ("      try { curChar = input_stream.readChar(); }");
        if (LexGenJava.mixed[LexGenJava.lexStateIndex])
          codeGenerator.genCodeLine ("      catch(java.io.IOException e) { break; }");
        else
          codeGenerator.genCodeLine ("      catch(java.io.IOException e) { return curPos; }");
        break;
      case CPP:
        if (LexGenJava.mixed[LexGenJava.lexStateIndex])
          codeGenerator.genCodeLine ("      if (input_stream->endOfInput()) { break; }");
        else
          codeGenerator.genCodeLine ("      if (input_stream->endOfInput()) { return curPos; }");
        codeGenerator.genCodeLine ("      curChar = input_stream->readChar();");
        break;
      default:
        throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
    }

    if (Options.getDebugTokenManager ())
    {
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCodeLine ("      debugStream.println(" +
                                     (LexGenJava.maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + "
                                                                  : "") +
                                     "\"Current character : \" + " +
                                     Options.getTokenMgrErrorClass () +
                                     ".addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") " +
                                     "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
          break;
        case CPP:
          codeGenerator.genCodeLine ("   fprintf(debugStream, " +
                                     "\"<%s>Current character : %c(%d) at line %d column %d\\n\"," +
                                     "addUnicodeEscapes(lexStateNames[curLexState]).c_str(), curChar, (int)curChar, " +
                                     "input_stream->getEndLine(), input_stream->getEndColumn());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
      }
    }

    codeGenerator.genCodeLine ("   }");

    if (LexGenJava.mixed[LexGenJava.lexStateIndex])
    {
      codeGenerator.genCodeLine ("   if (jjmatchedPos > strPos)");
      codeGenerator.genCodeLine ("      return curPos;");
      codeGenerator.genCodeLine ("");
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCodeLine ("   int toRet = Math.max(curPos, seenUpto);");
          break;
        case CPP:
          codeGenerator.genCodeLine ("   int toRet = MAX(curPos, seenUpto);");
          break;
        default:
          throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
      }
      codeGenerator.genCodeLine ("");
      codeGenerator.genCodeLine ("   if (curPos < toRet)");
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCodeLine ("      for (i = toRet - Math.min(curPos, seenUpto); i-- > 0; )");
          codeGenerator.genCodeLine ("         try { curChar = input_stream.readChar(); }");
          codeGenerator.genCodeLine ("         catch(java.io.IOException e) { " +
                                     "throw new Error(\"Internal Error : Please send a bug report.\"); }");
          break;
        case CPP:
          codeGenerator.genCodeLine ("      for (i = toRet - MIN(curPos, seenUpto); i-- > 0; )");
          codeGenerator.genCodeLine ("        {  assert(!input_stream->endOfInput());");
          codeGenerator.genCodeLine ("           curChar = input_stream->readChar(); }");
          break;
        default:
          throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
      }
      codeGenerator.genCodeLine ("");
      codeGenerator.genCodeLine ("   if (jjmatchedPos < strPos)");
      codeGenerator.genCodeLine ("   {");
      codeGenerator.genCodeLine ("      jjmatchedKind = strKind;");
      codeGenerator.genCodeLine ("      jjmatchedPos = strPos;");
      codeGenerator.genCodeLine ("   }");
      codeGenerator.genCodeLine ("   else if (jjmatchedPos == strPos && jjmatchedKind > strKind)");
      codeGenerator.genCodeLine ("      jjmatchedKind = strKind;");
      codeGenerator.genCodeLine ("");
      codeGenerator.genCodeLine ("   return toRet;");
    }

    codeGenerator.genCodeLine ("}");
    s_allStates.clear ();
  }

  public static void dumpStatesForStateCPP (final CodeGenerator codeGenerator)
  {
    if (statesForState == null)
    {
      assert (false) : "This should never be null.";
      codeGenerator.genCodeLine ("null;");
      return;
    }

    codeGenerator.switchToStaticsFile ();
    for (int i = 0; i < LexGenJava.maxLexStates; i++)
    {
      if (statesForState[i] == null)
      {
        continue;
      }

      for (int j = 0; j < statesForState[i].length; j++)
      {
        final int [] stateSet = statesForState[i][j];

        codeGenerator.genCode ("const int stateSet_" + i + "_" + j + "[" + LexGenJava.stateSetSize + "] = ");
        if (stateSet == null)
        {
          codeGenerator.genCodeLine ("   { " + j + " };");
          continue;
        }

        codeGenerator.genCode ("   { ");

        for (final int aElement : stateSet)
          codeGenerator.genCode (aElement + ", ");

        codeGenerator.genCodeLine ("};");
      }

    }

    for (int i = 0; i < LexGenJava.maxLexStates; i++)
    {
      codeGenerator.genCodeLine ("const int *stateSet_" + i + "[] = {");
      if (statesForState[i] == null)
      {
        codeGenerator.genCodeLine (" NULL, ");
        codeGenerator.genCodeLine ("};");
        continue;
      }

      for (int j = 0; j < statesForState[i].length; j++)
      {
        codeGenerator.genCode ("stateSet_" + i + "_" + j + ",");
      }
      codeGenerator.genCodeLine ("};");
    }

    codeGenerator.genCode ("const int** statesForState[] = { ");
    for (int i = 0; i < LexGenJava.maxLexStates; i++)
    {
      codeGenerator.genCodeLine ("stateSet_" + i + ", ");
    }

    codeGenerator.genCodeLine ("\n};");
    codeGenerator.switchToMainFile ();
  }

  public static void dumpStatesForStateJava (final CodeGenerator codeGenerator)
  {
    codeGenerator.genCode ("protected static final int[][][] statesForState = ");

    if (statesForState == null)
    {
      if (false)
        assert false : "This should never be null.";
      codeGenerator.genCodeLine ("null;");
      return;
    }

    codeGenerator.genCodeLine ("{");

    for (int i = 0; i < LexGenJava.maxLexStates; i++)
    {
      if (statesForState[i] == null)
      {
        codeGenerator.genCodeLine (" {},");
        continue;
      }

      codeGenerator.genCodeLine (" {");

      for (int j = 0; j < statesForState[i].length; j++)
      {
        final int [] stateSet = statesForState[i][j];

        if (stateSet == null)
        {
          codeGenerator.genCodeLine ("   { " + j + " },");
          continue;
        }

        codeGenerator.genCode ("   { ");

        for (final int aElement : stateSet)
          codeGenerator.genCode (aElement + ", ");

        codeGenerator.genCodeLine ("},");
      }

      codeGenerator.genCodeLine ("},");
    }

    codeGenerator.genCodeLine ("\n};");
  }

  public static void dumpStatesForKind (final CodeGenerator codeGenerator)
  {
    switch (codeGenerator.getOutputLanguage ())
    {
      case JAVA:
        dumpStatesForStateJava (codeGenerator);
        break;
      case CPP:
        dumpStatesForStateCPP (codeGenerator);
        break;
      default:
        throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
    }
    boolean moreThanOne = false;
    int cnt = 0;

    switch (codeGenerator.getOutputLanguage ())
    {
      case JAVA:
        codeGenerator.genCode ("protected static final int[][] kindForState = ");
        break;
      case CPP:
        codeGenerator.switchToStaticsFile ();
        codeGenerator.genCode ("static const int kindForState[" +
                               LexGenJava.stateSetSize +
                               "][" +
                               LexGenJava.stateSetSize +
                               "] = ");
        break;
      default:
        throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
    }

    if (kinds == null)
    {
      codeGenerator.genCodeLine ("null;");
      return;
    }
    codeGenerator.genCodeLine ("{");

    for (final int [] aKind : kinds)
    {
      if (moreThanOne)
        codeGenerator.genCodeLine (",");
      moreThanOne = true;

      if (aKind == null)
        codeGenerator.genCodeLine ("{}");
      else
      {
        cnt = 0;
        codeGenerator.genCode ("{ ");
        for (final int aElement : aKind)
        {
          if (cnt % 15 == 0)
            codeGenerator.genCode ("\n  ");
          else
            if (cnt > 1)
              codeGenerator.genCode (" ");

          codeGenerator.genCode (Integer.toString (aElement));
          codeGenerator.genCode (", ");

        }

        codeGenerator.genCode ("}");
      }
    }
    codeGenerator.genCodeLine ("\n};");
    codeGenerator.switchToMainFile ();
  }

  public static void reInit ()
  {
    s_unicodeWarningGiven = false;
    s_generatedStates = 0;
    s_idCnt = 0;
    s_lohiByteCnt = 0;
    s_dummyStateIndex = -1;
    s_done = false;
    s_mark = null;
    s_stateDone = null;
    s_allStates = new ArrayList <> ();
    s_indexedAllStates = new ArrayList <> ();
    s_nonAsciiTableForMethod = new ArrayList <> ();
    s_equivStatesTable = new HashMap <> ();
    s_allNextStates = new HashMap <> ();
    s_lohiByteTab = new HashMap <> ();
    s_stateNameForComposite = new HashMap <> ();
    s_compositeStateTable = new HashMap <> ();
    s_stateBlockTable = new HashMap <> ();
    s_stateSetsToFix = new HashMap <> ();
    allBitVectors = new ArrayList <> ();
    tmpIndices = new int [512];
    allBits = "{\n   0xffffffffffffffffL, " +
              "0xffffffffffffffffL, " +
              "0xffffffffffffffffL, " +
              "0xffffffffffffffffL\n};";
    tableToDump = new HashMap <> ();
    orderedStateSet = new ArrayList <> ();
    lastIndex = 0;
    // boilerPlateDumped = false;
    s_jjCheckNAddStatesUnaryNeeded = false;
    s_jjCheckNAddStatesDualNeeded = false;
    kinds = null;
    statesForState = null;
  }

  private static final Map <Integer, NfaState> s_initialStates = new HashMap <> ();
  private static final Map <Integer, List <NfaState>> s_statesForLexicalState = new HashMap <> ();
  private static final Map <Integer, Integer> s_nfaStateOffset = new HashMap <> ();
  private static final Map <Integer, Integer> s_matchAnyChar = new HashMap <> ();

  static void updateNfaData (final int maxState,
                             final int startStateName,
                             final int lexicalStateIndex,
                             final int matchAnyCharKind)
  {
    // Cleanup the state set.
    final Set <Integer> done = new HashSet <> ();
    final List <NfaState> cleanStates = new ArrayList <> ();
    NfaState startState = null;
    for (int i = 0; i < s_allStates.size (); i++)
    {
      final NfaState tmp = s_allStates.get (i);
      if (tmp.stateName == -1)
        continue;
      if (done.contains (tmp.stateName))
        continue;
      done.add (tmp.stateName);
      cleanStates.add (tmp);
      if (tmp.stateName == startStateName)
      {
        startState = tmp;
      }
    }

    s_initialStates.put (lexicalStateIndex, startState);
    s_statesForLexicalState.put (lexicalStateIndex, cleanStates);
    s_nfaStateOffset.put (lexicalStateIndex, maxState);
    if (matchAnyCharKind > 0)
    {
      s_matchAnyChar.put (lexicalStateIndex, matchAnyCharKind);
    }
    else
    {
      s_matchAnyChar.put (lexicalStateIndex, Integer.MAX_VALUE);
    }
  }

  public static void buildTokenizerData (final TokenizerData tokenizerData)
  {
    NfaState [] cleanStates;
    final List <NfaState> cleanStateList = new ArrayList <> ();
    for (final int l : s_statesForLexicalState.keySet ())
    {
      final int offset = s_nfaStateOffset.get (l);
      final List <NfaState> states = s_statesForLexicalState.get (l);
      for (int i = 0; i < states.size (); i++)
      {
        final NfaState state = states.get (i);
        if (state.stateName == -1)
          continue;
        states.get (i).stateName += offset;
      }
      cleanStateList.addAll (states);
    }
    cleanStates = new NfaState [cleanStateList.size ()];
    for (final NfaState s : cleanStateList)
    {
      assert (cleanStates[s.stateName] == null);
      cleanStates[s.stateName] = s;
      final Set <Character> chars = new TreeSet <> ();
      for (int c = 0; c <= Character.MAX_VALUE; c++)
      {
        if (s.canMoveUsingChar ((char) c))
        {
          chars.add ((char) c);
        }
      }
      final Set <Integer> nextStates = new TreeSet <> ();
      if (s.next != null)
      {
        for (final NfaState next : s.next.epsilonMoves)
        {
          nextStates.add (next.stateName);
        }
      }
      final SortedSet <Integer> composite = new TreeSet <> ();
      if (s.m_isComposite)
      {
        for (final int c : s.m_compositeStates)
          composite.add (c);
      }
      tokenizerData.addNfaState (s.stateName, chars, nextStates, composite, s.m_kindToPrint);
    }
    final Map <Integer, Integer> initStates = new HashMap <> ();
    for (final int l : s_initialStates.keySet ())
    {
      if (s_initialStates.get (l) == null)
      {
        initStates.put (l, -1);
      }
      else
      {
        initStates.put (l, s_initialStates.get (l).stateName);
      }
    }
    tokenizerData.setInitialStates (initStates);
    tokenizerData.setWildcardKind (s_matchAnyChar);
  }

  static NfaState getNfaState (final int index)
  {
    if (index == -1)
      return null;
    for (final NfaState s : s_allStates)
    {
      if (s.stateName == index)
        return s;
    }
    assert (false);
    return null;
  }
}
