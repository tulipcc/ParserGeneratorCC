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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.helger.pgcc.output.UnsupportedOutputLanguageException;

/**
 * Describes string literals.
 */
public class RStringLiteral extends RegularExpression
{
  private static final class KindInfo
  {
    final long [] m_validKinds;
    final long [] m_finalKinds;
    int m_validKindCnt = 0;
    int m_finalKindCnt = 0;
    final Set <Integer> m_finalKindSet = new HashSet <> ();
    final Set <Integer> m_validKindSet = new HashSet <> ();

    KindInfo (final int maxKind)
    {
      m_validKinds = new long [maxKind / 64 + 1];
      m_finalKinds = new long [maxKind / 64 + 1];
    }

    public void insertValidKind (final int kind)
    {
      m_validKinds[kind / 64] |= (1L << (kind % 64));
      m_validKindCnt++;
      m_validKindSet.add (kind);
    }

    public void insertFinalKind (final int kind)
    {
      m_finalKinds[kind / 64] |= (1L << (kind % 64));
      m_finalKindCnt++;
      m_finalKindSet.add (kind);
    }
  }

  /**
   * The string image of the literal.
   */
  public String m_image;

  public RStringLiteral ()
  {}

  public RStringLiteral (final Token t, final String image)
  {
    this.setLine (t.beginLine);
    this.setColumn (t.beginColumn);
    this.m_image = image;
  }

  private static int s_maxStrKind = 0;
  private static int s_maxLen = 0;
  private static int s_charCnt = 0;
  private static List <Map <String, KindInfo>> s_charPosKind = new ArrayList <> ();

  // with single char keys;
  private static int [] s_maxLenForActive = new int [100]; // 6400 tokens
  public static String [] s_allImages;
  private static int [] [] s_intermediateKinds;
  private static int [] [] s_intermediateMatchedPos;

  private static boolean [] s_subString;
  private static boolean [] s_subStringAtPos;

  private static Map <String, long []> [] s_statesForPos;

  /**
   * Initialize all the static variables, so that there is no interference
   * between the various states of the lexer. Need to call this method after
   * generating code for each lexical state.
   */
  public static void reInitStatic ()
  {
    s_maxStrKind = 0;
    s_maxLen = 0;
    s_charPosKind = new ArrayList <> ();
    s_maxLenForActive = new int [100]; // 6400 tokens
    s_intermediateKinds = null;
    s_intermediateMatchedPos = null;
    s_subString = null;
    s_subStringAtPos = null;
    s_statesForPos = null;
  }

  public static void dumpStrLiteralImages (final CodeGenerator codeGenerator)
  {
    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    switch (Options.getOutputLanguage ())
    {
      case JAVA:
        dumpStrLiteralImagesForJava (codeGenerator);
        return;
      case CPP:
        // For C++
        String image;
        int i;
        s_charCnt = 0; // Set to zero in reInit() but just to be sure

        codeGenerator.genCodeLine ("");
        codeGenerator.genCodeLine ("/** Token literal values. */");
        int literalCount = 0;
        codeGenerator.switchToStaticsFile ();

        if (s_allImages == null || s_allImages.length == 0)
        {
          codeGenerator.genCodeLine ("static const JJString jjstrLiteralImages[] = {};");
          return;
        }

        s_allImages[0] = "";
        for (i = 0; i < s_allImages.length; i++)
        {
          if ((image = s_allImages[i]) == null ||
              ((LexGenJava.toSkip[i / 64] & (1L << (i % 64))) == 0L &&
               (LexGenJava.toMore[i / 64] & (1L << (i % 64))) == 0L &&
               (LexGenJava.toToken[i / 64] & (1L << (i % 64))) == 0L) ||
              (LexGenJava.toSkip[i / 64] & (1L << (i % 64))) != 0L ||
              (LexGenJava.toMore[i / 64] & (1L << (i % 64))) != 0L ||
              LexGenJava.canReachOnMore[LexGenJava.lexStates[i]] ||
              ((Options.getIgnoreCase () || LexGenJava.ignoreCase[i]) &&
               (!image.equals (image.toLowerCase ()) || !image.equals (image.toUpperCase ()))))
          {
            s_allImages[i] = null;
            if ((s_charCnt += 6) > 80)
            {
              codeGenerator.genCodeLine ("");
              s_charCnt = 0;
            }

            codeGenerator.genCodeLine ("static JJChar jjstrLiteralChars_" + literalCount++ + "[] = {0};");
            continue;
          }

          String toPrint = "static JJChar jjstrLiteralChars_" + literalCount++ + "[] = {";
          for (int j = 0; j < image.length (); j++)
          {
            final String hexVal = Integer.toHexString (image.charAt (j));
            toPrint += "0x" + hexVal + ", ";
          }

          // Null char
          toPrint += "0};";

          if ((s_charCnt += toPrint.length ()) >= 80)
          {
            codeGenerator.genCodeLine ("");
            s_charCnt = 0;
          }

          codeGenerator.genCodeLine (toPrint);
        }

        while (++i < LexGenJava.maxOrdinal)
        {
          if ((s_charCnt += 6) > 80)
          {
            codeGenerator.genCodeLine ("");
            s_charCnt = 0;
          }

          codeGenerator.genCodeLine ("static JJChar jjstrLiteralChars_" + literalCount++ + "[] = {0};");
          continue;
        }

        // Generate the array here.
        codeGenerator.genCodeLine ("static const JJString " + "jjstrLiteralImages[] = {");
        for (int j = 0; j < literalCount; j++)
        {
          codeGenerator.genCodeLine ("jjstrLiteralChars_" + j + ", ");
        }
        codeGenerator.genCodeLine ("};");
        break;
      default:
        throw new RuntimeException ("Output language type not fully implemented : " + Options.getOutputLanguage ());
    }
  }

  public static void dumpStrLiteralImagesForJava (final CodeGenerator codeGenerator)
  {
    String image;
    int i;
    s_charCnt = 0; // Set to zero in reInit() but just to be sure

    codeGenerator.genCodeLine ("");
    codeGenerator.genCodeLine ("/** Token literal values. */");
    codeGenerator.genCodeLine ("public static final String[] jjstrLiteralImages = {");

    if (s_allImages == null || s_allImages.length == 0)
    {
      codeGenerator.genCodeLine ("};");
      return;
    }

    s_allImages[0] = "";
    for (i = 0; i < s_allImages.length; i++)
    {
      if ((image = s_allImages[i]) == null ||
          ((LexGenJava.toSkip[i / 64] & (1L << (i % 64))) == 0L &&
           (LexGenJava.toMore[i / 64] & (1L << (i % 64))) == 0L &&
           (LexGenJava.toToken[i / 64] & (1L << (i % 64))) == 0L) ||
          (LexGenJava.toSkip[i / 64] & (1L << (i % 64))) != 0L ||
          (LexGenJava.toMore[i / 64] & (1L << (i % 64))) != 0L ||
          LexGenJava.canReachOnMore[LexGenJava.lexStates[i]] ||
          ((Options.getIgnoreCase () || LexGenJava.ignoreCase[i]) &&
           (!image.equals (image.toLowerCase ()) || !image.equals (image.toUpperCase ()))))
      {
        s_allImages[i] = null;
        if ((s_charCnt += 6) > 80)
        {
          codeGenerator.genCodeLine ("");
          s_charCnt = 0;
        }

        codeGenerator.genCode ("null, ");
        continue;
      }

      String toPrint = "\"";
      for (int j = 0; j < image.length (); j++)
      {
        if (codeGenerator.isJavaLanguage () && image.charAt (j) <= 0xff)
          toPrint += ("\\" + Integer.toOctalString (image.charAt (j)));
        else
        {
          String hexVal = Integer.toHexString (image.charAt (j));
          if (hexVal.length () == 3)
            hexVal = "0" + hexVal;
          toPrint += ("\\u" + hexVal);
        }
      }

      toPrint += ("\", ");

      if ((s_charCnt += toPrint.length ()) >= 80)
      {
        codeGenerator.genCodeLine ("");
        s_charCnt = 0;
      }

      codeGenerator.genCode (toPrint);
    }

    while (++i < LexGenJava.maxOrdinal)
    {
      if ((s_charCnt += 6) > 80)
      {
        codeGenerator.genCodeLine ("");
        s_charCnt = 0;
      }

      codeGenerator.genCode ("null, ");
      continue;
    }

    codeGenerator.genCodeLine ("};");
  }

  /**
   * Used for top level string literals.
   *
   * @param codeGenerator
   * @param kind
   */
  public void generateDfa ()
  {
    String s;
    Map <String, KindInfo> temp;

    if (s_maxStrKind <= m_ordinal)
      s_maxStrKind = m_ordinal + 1;

    final int len = m_image.length ();
    if (len > s_maxLen)
      s_maxLen = len;

    for (int i = 0; i < len; i++)
    {
      final char c = m_image.charAt (i);
      if (Options.getIgnoreCase ())
        s = Character.toString (Character.toLowerCase (c));
      else
        s = Character.toString (c);

      if (!NfaState.s_unicodeWarningGiven &&
          c > 0xff &&
          !Options.getJavaUnicodeEscape () &&
          !Options.getUserCharStream ())
      {
        NfaState.s_unicodeWarningGiven = true;
        JavaCCErrors.warning (LexGenJava.curRE,
                              "Non-ASCII characters used in regular expression." +
                                                "Please make sure you use the correct Reader when you create the parser, " +
                                                "one that can handle your character set.");
      }

      if (i >= s_charPosKind.size ()) // Kludge, but OK
      {
        temp = new HashMap <> ();
        s_charPosKind.add (temp);
      }
      else
        temp = s_charPosKind.get (i);

      KindInfo info = temp.computeIfAbsent (s, k -> new KindInfo (LexGenJava.maxOrdinal));

      if (i + 1 == len)
        info.insertFinalKind (m_ordinal);
      else
        info.insertValidKind (m_ordinal);

      if (!Options.getIgnoreCase () && LexGenJava.ignoreCase[m_ordinal] && c != Character.toLowerCase (c))
      {
        s = Character.toString (Character.toLowerCase (c));

        if (i >= s_charPosKind.size ()) // Kludge, but OK
        {
          temp = new HashMap <> ();
          s_charPosKind.add (temp);
        }
        else
          temp = s_charPosKind.get (i);

        info = temp.computeIfAbsent (s, k -> new KindInfo (LexGenJava.maxOrdinal));

        if (i + 1 == len)
          info.insertFinalKind (m_ordinal);
        else
          info.insertValidKind (m_ordinal);
      }

      if (!Options.getIgnoreCase () && LexGenJava.ignoreCase[m_ordinal] && c != Character.toUpperCase (c))
      {
        s = Character.toString (Character.toUpperCase (c));

        // Kludge, but OK
        if (i >= s_charPosKind.size ())
        {
          temp = new HashMap <> ();
          s_charPosKind.add (temp);
        }
        else
          temp = s_charPosKind.get (i);

        info = temp.computeIfAbsent (s, k -> new KindInfo (LexGenJava.maxOrdinal));

        if (i + 1 == len)
          info.insertFinalKind (m_ordinal);
        else
          info.insertValidKind (m_ordinal);
      }
    }

    s_maxLenForActive[m_ordinal / 64] = Math.max (s_maxLenForActive[m_ordinal / 64], len - 1);
    s_allImages[m_ordinal] = m_image;
  }

  @Override
  public Nfa generateNfa (final boolean ignoreCase)
  {
    if (m_image.length () == 1)
    {
      final RCharacterList temp = new RCharacterList (m_image.charAt (0));
      return temp.generateNfa (ignoreCase);
    }

    NfaState startState = new NfaState ();
    final NfaState theStartState = startState;
    NfaState finalState = null;

    if (m_image.length () == 0)
      return new Nfa (theStartState, theStartState);

    int i;

    for (i = 0; i < m_image.length (); i++)
    {
      finalState = new NfaState ();
      startState.charMoves = new char [1];
      startState.addChar (m_image.charAt (i));

      if (Options.getIgnoreCase () || ignoreCase)
      {
        startState.addChar (Character.toLowerCase (m_image.charAt (i)));
        startState.addChar (Character.toUpperCase (m_image.charAt (i)));
      }

      startState.next = finalState;
      startState = finalState;
    }

    return new Nfa (theStartState, finalState);
  }

  static void dumpNullStrLiterals (final CodeGenerator codeGenerator)
  {
    codeGenerator.genCodeLine ("{");

    if (NfaState.s_generatedStates != 0)
      codeGenerator.genCodeLine ("   return jjMoveNfa" +
                                 LexGenJava.lexStateSuffix +
                                 "(" +
                                 NfaState.initStateName () +
                                 ", 0);");
    else
      codeGenerator.genCodeLine ("   return 1;");

    codeGenerator.genCodeLine ("}");
  }

  private static int _getStateSetForKind (final int pos, final int kind)
  {
    if (LexGenJava.mixed[LexGenJava.lexStateIndex] || NfaState.s_generatedStates == 0)
      return -1;

    final Map <String, long []> allStateSets = s_statesForPos[pos];

    if (allStateSets == null)
      return -1;

    for (final Map.Entry <String, long []> aEntry : allStateSets.entrySet ())
    {
      String s = aEntry.getKey ();
      final long [] actives = aEntry.getValue ();

      s = s.substring (s.indexOf (", ") + 2);
      s = s.substring (s.indexOf (", ") + 2);

      if (s.equals ("null;"))
        continue;

      if (actives != null && (actives[kind / 64] & (1L << (kind % 64))) != 0L)
      {
        return NfaState.addStartStateSet (s);
      }
    }

    return -1;
  }

  static String getLabel (final int kind)
  {
    final RegularExpression re = LexGenJava.rexprs[kind];

    if (re instanceof RStringLiteral)
      return " \"" + JavaCCGlobals.addEscapes (((RStringLiteral) re).m_image) + "\"";
    else
      if (!re.m_label.equals (""))
        return " <" + re.m_label + ">";
      else
        return " <token of kind " + kind + ">";
  }

  static int getLine (final int kind)
  {
    return LexGenJava.rexprs[kind].getLine ();
  }

  static int getColumn (final int kind)
  {
    return LexGenJava.rexprs[kind].getColumn ();
  }

  /**
   * Returns true if s1 starts with s2 (ignoring case for each character).
   */
  static private boolean _startsWithIgnoreCase (final String s1, final String s2)
  {
    if (s1.length () < s2.length ())
      return false;

    for (int i = 0; i < s2.length (); i++)
    {
      final char c1 = s1.charAt (i), c2 = s2.charAt (i);

      if (c1 != c2 && Character.toLowerCase (c2) != c1 && Character.toUpperCase (c2) != c1)
        return false;
    }

    return true;
  }

  static void fillSubString ()
  {
    String image;
    s_subString = new boolean [s_maxStrKind + 1];
    s_subStringAtPos = new boolean [s_maxLen];

    for (int i = 0; i < s_maxStrKind; i++)
    {
      s_subString[i] = false;

      if ((image = s_allImages[i]) == null || LexGenJava.lexStates[i] != LexGenJava.lexStateIndex)
        continue;

      if (LexGenJava.mixed[LexGenJava.lexStateIndex])
      {
        // We will not optimize for mixed case
        s_subString[i] = true;
        s_subStringAtPos[image.length () - 1] = true;
        continue;
      }

      for (int j = 0; j < s_maxStrKind; j++)
      {
        if (j != i && LexGenJava.lexStates[j] == LexGenJava.lexStateIndex && (s_allImages[j]) != null)
        {
          if (s_allImages[j].indexOf (image) == 0)
          {
            s_subString[i] = true;
            s_subStringAtPos[image.length () - 1] = true;
            break;
          }
          else
            if (Options.getIgnoreCase () && _startsWithIgnoreCase (s_allImages[j], image))
            {
              s_subString[i] = true;
              s_subStringAtPos[image.length () - 1] = true;
              break;
            }
        }
      }
    }
  }

  static void dumpStartWithStates (final CodeGenerator codeGenerator)
  {
    switch (Options.getOutputLanguage ())
    {
      case JAVA:
        codeGenerator.genCodeLine ((Options.getStatic () ? "static " : "") +
                                   "private int " +
                                   "jjStartNfaWithStates" +
                                   LexGenJava.lexStateSuffix +
                                   "(int pos, int kind, int state)");
        break;
      case CPP:
        codeGenerator.generateMethodDefHeader ("int",
                                               LexGenJava.tokMgrClassName,
                                               "jjStartNfaWithStates" +
                                                                           LexGenJava.lexStateSuffix +
                                                                           "(int pos, int kind, int state)");
        break;
      default:
        throw new IllegalStateException ("Output language type not fully implemented : " +
                                         Options.getOutputLanguage ());
    }
    codeGenerator.genCodeLine ("{");
    codeGenerator.genCodeLine ("   jjmatchedKind = kind;");
    codeGenerator.genCodeLine ("   jjmatchedPos = pos;");

    if (Options.getDebugTokenManager ())
    {
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCodeLine ("   debugStream.println(\"   No more string literal token matches are possible.\");");
          codeGenerator.genCodeLine ("   debugStream.println(\"   Currently matched the first \" " +
                                     "+ (jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
          break;
        case CPP:
          codeGenerator.genCodeLine ("   fprintf(debugStream, \"   No more string literal token matches are possible.\");");
          codeGenerator.genCodeLine ("   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
      }
    }

    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    switch (codeGenerator.getOutputLanguage ())
    {
      case JAVA:
        codeGenerator.genCodeLine ("   try { curChar = input_stream.readChar(); }");
        codeGenerator.genCodeLine ("   catch(java.io.IOException e) { return pos + 1; }");
        break;
      case CPP:
        codeGenerator.genCodeLine ("   if (input_stream->endOfInput()) { return pos + 1; }");
        codeGenerator.genCodeLine ("   curChar = input_stream->readChar();");
        break;
      default:
        throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
    }
    if (Options.getDebugTokenManager ())
    {
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCodeLine ("   debugStream.println(" +
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

    codeGenerator.genCodeLine ("   return jjMoveNfa" + LexGenJava.lexStateSuffix + "(state, pos + 1);");
    codeGenerator.genCodeLine ("}");
  }

  private static boolean boilerPlateDumped = false;

  static void dumpBoilerPlate (final CodeGenerator codeGenerator)
  {
    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    switch (codeGenerator.getOutputLanguage ())
    {
      case JAVA:
        codeGenerator.genCodeLine ((Options.getStatic () ? "static " : "") +
                                   "private int " +
                                   "jjStopAtPos(int pos, int kind)");
        break;
      case CPP:
        codeGenerator.generateMethodDefHeader (" int ", LexGenJava.tokMgrClassName, "jjStopAtPos(int pos, int kind)");
        break;
      default:
        throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
    }
    codeGenerator.genCodeLine ("{");
    codeGenerator.genCodeLine ("   jjmatchedKind = kind;");
    codeGenerator.genCodeLine ("   jjmatchedPos = pos;");

    if (Options.getDebugTokenManager ())
    {
      // TODO :: CBA -- Require Unification of output language specific
      // processing into a single Enum class
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCodeLine ("   debugStream.println(\"   No more string literal token matches are possible.\");");
          codeGenerator.genCodeLine ("   debugStream.println(\"   Currently matched the first \" + (jjmatchedPos + 1) + " +
                                     "\" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
          break;
        case CPP:
          codeGenerator.genCodeLine ("   fprintf(debugStream, \"   No more string literal token matches are possible.\");");
          codeGenerator.genCodeLine ("   fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\",  (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
          break;
        default:
          throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
      }
    }

    codeGenerator.genCodeLine ("   return pos + 1;");
    codeGenerator.genCodeLine ("}");
  }

  static String [] reArrange (final Map <String, KindInfo> tab)
  {
    final String [] ret = new String [tab.size ()];
    int cnt = 0;

    for (final String s : tab.keySet ())
    {
      final char c = s.charAt (0);

      int i = 0;
      while (i < cnt && ret[i].charAt (0) < c)
        i++;

      if (i < cnt)
        for (int j = cnt - 1; j >= i; j--)
          ret[j + 1] = ret[j];

      ret[i] = s;
      cnt++;
    }

    return ret;
  }

  static void dumpDfaCode (final CodeGenerator codeGenerator)
  {
    Map <String, KindInfo> tab;
    String key;
    KindInfo info;
    final int maxLongsReqd = s_maxStrKind / 64 + 1;
    boolean ifGenerated;
    LexGenJava.maxLongsReqd[LexGenJava.lexStateIndex] = maxLongsReqd;

    if (s_maxLen == 0)
    {
      // TODO :: CBA -- Require Unification of output language specific
      // processing into a single Enum class
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCodeLine ((Options.getStatic () ? "static " : "") +
                                     "private int " +
                                     "jjMoveStringLiteralDfa0" +
                                     LexGenJava.lexStateSuffix +
                                     "()");
          break;
        case CPP:
          codeGenerator.generateMethodDefHeader (" int ",
                                                 LexGenJava.tokMgrClassName,
                                                 "jjMoveStringLiteralDfa0" + LexGenJava.lexStateSuffix + "()");
          break;
        default:
          throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
      }
      dumpNullStrLiterals (codeGenerator);
      return;
    }

    if (!boilerPlateDumped)
    {
      dumpBoilerPlate (codeGenerator);
      boilerPlateDumped = true;
    }

    boolean createStartNfa = false;
    for (int i = 0; i < s_maxLen; i++)
    {
      boolean atLeastOne = false;
      boolean startNfaNeeded = false;
      tab = s_charPosKind.get (i);
      final String [] keys = reArrange (tab);

      final StringBuilder params = new StringBuilder ();
      params.append ("(");
      if (i != 0)
      {
        if (i == 1)
        {
          int j = 0;
          for (; j < maxLongsReqd - 1; j++)
            if (i <= s_maxLenForActive[j])
            {
              if (atLeastOne)
                params.append (", ");
              else
                atLeastOne = true;
              params.append ("" + Options.getLongType () + " active" + j);
            }

          if (i <= s_maxLenForActive[j])
          {
            if (atLeastOne)
              params.append (", ");
            params.append ("" + Options.getLongType () + " active" + j);
          }
        }
        else
        {
          int j = 0;
          for (; j < maxLongsReqd - 1; j++)
            if (i <= s_maxLenForActive[j] + 1)
            {
              if (atLeastOne)
                params.append (", ");
              else
                atLeastOne = true;
              params.append ("" + Options.getLongType () + " old" + j + ", " + Options.getLongType () + " active" + j);
            }

          if (i <= s_maxLenForActive[j] + 1)
          {
            if (atLeastOne)
              params.append (", ");
            params.append ("" + Options.getLongType () + " old" + j + ", " + Options.getLongType () + " active" + j);
          }
        }
      }
      params.append (")");

      // TODO :: CBA -- Require Unification of output language specific
      // processing into a single Enum class
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCode ((Options.getStatic () ? "static " : "") +
                                 "private int " +
                                 "jjMoveStringLiteralDfa" +
                                 i +
                                 LexGenJava.lexStateSuffix +
                                 params);
          break;
        case CPP:
          codeGenerator.generateMethodDefHeader (" int ",
                                                 LexGenJava.tokMgrClassName,
                                                 "jjMoveStringLiteralDfa" + i + LexGenJava.lexStateSuffix + params);
          break;
        default:
          throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
      }

      codeGenerator.genCodeLine ("{");

      if (i != 0)
      {
        if (i > 1)
        {
          atLeastOne = false;
          codeGenerator.genCode ("   if ((");

          int j = 0;
          for (; j < maxLongsReqd - 1; j++)
            if (i <= s_maxLenForActive[j] + 1)
            {
              if (atLeastOne)
                codeGenerator.genCode (" | ");
              else
                atLeastOne = true;
              codeGenerator.genCode ("(active" + j + " &= old" + j + ")");
            }

          if (i <= s_maxLenForActive[j] + 1)
          {
            if (atLeastOne)
              codeGenerator.genCode (" | ");
            codeGenerator.genCode ("(active" + j + " &= old" + j + ")");
          }

          codeGenerator.genCodeLine (") == 0L)");
          if (!LexGenJava.mixed[LexGenJava.lexStateIndex] && NfaState.s_generatedStates != 0)
          {
            codeGenerator.genCode ("      return jjStartNfa" + LexGenJava.lexStateSuffix + "(" + (i - 2) + ", ");
            for (j = 0; j < maxLongsReqd - 1; j++)
              if (i <= s_maxLenForActive[j] + 1)
                codeGenerator.genCode ("old" + j + ", ");
              else
                codeGenerator.genCode ("0L, ");
            if (i <= s_maxLenForActive[j] + 1)
              codeGenerator.genCodeLine ("old" + j + ");");
            else
              codeGenerator.genCodeLine ("0L);");
          }
          else
            if (NfaState.s_generatedStates != 0)
              codeGenerator.genCodeLine ("      return jjMoveNfa" +
                                         LexGenJava.lexStateSuffix +
                                         "(" +
                                         NfaState.initStateName () +
                                         ", " +
                                         (i - 1) +
                                         ");");
            else
              codeGenerator.genCodeLine ("      return " + i + ";");
        }

        if (i != 0 && Options.getDebugTokenManager ())
        {
          switch (codeGenerator.getOutputLanguage ())
          {
            case JAVA:
              codeGenerator.genCodeLine ("   if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                         Integer.toHexString (Integer.MAX_VALUE) +
                                         ")");
              codeGenerator.genCodeLine ("      debugStream.println(\"   Currently matched the first \" + " +
                                         "(jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
              codeGenerator.genCodeLine ("   debugStream.println(\"   Possible string literal matches : { \"");
              break;
            case CPP:
              codeGenerator.genCodeLine ("   if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                         Integer.toHexString (Integer.MAX_VALUE) +
                                         ")");
              codeGenerator.genCodeLine ("      fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\", (jjmatchedPos + 1), addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
              codeGenerator.genCodeLine ("   fprintf(debugStream, \"   Possible string literal matches : { \");");
              break;
            default:
              throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
          }

          final StringBuilder fmt = new StringBuilder ();
          final StringBuilder args = new StringBuilder ();
          for (int vecs = 0; vecs < s_maxStrKind / 64 + 1; vecs++)
          {
            if (i <= s_maxLenForActive[vecs])
            {
              switch (codeGenerator.getOutputLanguage ())
              {
                case JAVA:
                  codeGenerator.genCodeLine (" +");
                  codeGenerator.genCode ("         jjKindsForBitVector(" + vecs + ", ");
                  codeGenerator.genCode ("active" + vecs + ") ");
                  break;
                case CPP:
                  if (fmt.length () > 0)
                  {
                    fmt.append (", ");
                    args.append (", ");
                  }
                  fmt.append ("%s");
                  args.append ("         jjKindsForBitVector(" + vecs + ", ");
                  args.append ("active" + vecs + ").c_str() ");
                  break;
                default:
                  throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
              }
            }
          }

          // TODO :: CBA -- Require Unification of output language specific
          // processing into a single Enum class
          switch (codeGenerator.getOutputLanguage ())
          {
            case JAVA:
              codeGenerator.genCodeLine (" + \" } \");");
              break;
            case CPP:
              fmt.append ("}\\n");
              codeGenerator.genCodeLine ("    fprintf(debugStream, \"" + fmt + "\"," + args + ");");
              break;
            default:
              throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
          }
        }

        // TODO :: CBA -- Require Unification of output language specific
        // processing into a single Enum class
        switch (codeGenerator.getOutputLanguage ())
        {
          case JAVA:
            codeGenerator.genCodeLine ("   try { curChar = input_stream.readChar(); }");
            codeGenerator.genCodeLine ("   catch(java.io.IOException e) {");
            break;
          case CPP:
            codeGenerator.genCodeLine ("   if (input_stream->endOfInput()) {");
            break;
          default:
            throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
        }

        if (!LexGenJava.mixed[LexGenJava.lexStateIndex] && NfaState.s_generatedStates != 0)
        {
          codeGenerator.genCode ("      jjStopStringLiteralDfa" + LexGenJava.lexStateSuffix + "(" + (i - 1) + ", ");

          int k = 0;
          for (; k < maxLongsReqd - 1; k++)
          {
            if (i <= s_maxLenForActive[k])
              codeGenerator.genCode ("active" + k + ", ");
            else
              codeGenerator.genCode ("0L, ");
          }

          if (i <= s_maxLenForActive[k])
          {
            codeGenerator.genCodeLine ("active" + k + ");");
          }
          else
          {
            codeGenerator.genCodeLine ("0L);");
          }

          if (i != 0 && Options.getDebugTokenManager ())
          {
            switch (codeGenerator.getOutputLanguage ())
            {
              case JAVA:
                codeGenerator.genCodeLine ("      if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                           Integer.toHexString (Integer.MAX_VALUE) +
                                           ")");
                codeGenerator.genCodeLine ("         debugStream.println(\"   Currently matched the first \" + " +
                                           "(jjmatchedPos + 1) + \" characters as a \" + tokenImage[jjmatchedKind] + \" token.\");");
                break;
              case CPP:
                codeGenerator.genCodeLine ("      if (jjmatchedKind != 0 && jjmatchedKind != 0x" +
                                           Integer.toHexString (Integer.MAX_VALUE) +
                                           ")");
                codeGenerator.genCodeLine ("      fprintf(debugStream, \"   Currently matched the first %d characters as a \\\"%s\\\" token.\\n\", (jjmatchedPos + 1),  addUnicodeEscapes(tokenImage[jjmatchedKind]).c_str());");
                break;
              default:
                throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
            }
          }

          codeGenerator.genCodeLine ("      return " + i + ";");
        }
        else
          if (NfaState.s_generatedStates != 0)
          {
            codeGenerator.genCodeLine ("   return jjMoveNfa" +
                                       LexGenJava.lexStateSuffix +
                                       "(" +
                                       NfaState.initStateName () +
                                       ", " +
                                       (i - 1) +
                                       ");");
          }
          else
          {
            codeGenerator.genCodeLine ("      return " + i + ";");
          }

        codeGenerator.genCodeLine ("   }");
      }

      if (i != 0)
      {
        // TODO :: CBA -- Require Unification of output language specific
        // processing into a single Enum class
        switch (codeGenerator.getOutputLanguage ())
        {
          case CPP:
            codeGenerator.genCodeLine ("   curChar = input_stream->readChar();");
            break;
        }

        if (Options.getDebugTokenManager ())
        {
          // TODO :: CBA -- Require Unification of output language specific
          // processing into a single Enum class
          switch (codeGenerator.getOutputLanguage ())
          {
            case JAVA:
              codeGenerator.genCodeLine ("   debugStream.println(" +
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
      }

      codeGenerator.genCodeLine ("   switch(curChar)");
      codeGenerator.genCodeLine ("   {");

      CaseLoop: for (int q = 0; q < keys.length; q++)
      {
        key = keys[q];
        info = tab.get (key);
        ifGenerated = false;
        final char c = key.charAt (0);

        if (i == 0 &&
            c < 128 &&
            info.m_finalKindCnt != 0 &&
            (NfaState.s_generatedStates == 0 || !NfaState.canStartNfaUsingAscii (c)))
        {
          int kind;
          int j = 0;
          for (; j < maxLongsReqd; j++)
            if (info.m_finalKinds[j] != 0L)
              break;

          for (int k = 0; k < 64; k++)
            if ((info.m_finalKinds[j] & (1L << k)) != 0L && !s_subString[kind = (j * 64 + k)])
            {
              if ((s_intermediateKinds != null &&
                   s_intermediateKinds[(j * 64 + k)] != null &&
                   s_intermediateKinds[(j * 64 + k)][i] < (j * 64 + k) &&
                   s_intermediateMatchedPos != null &&
                   s_intermediateMatchedPos[(j * 64 + k)][i] == i) ||
                  (LexGenJava.canMatchAnyChar[LexGenJava.lexStateIndex] >= 0 &&
                   LexGenJava.canMatchAnyChar[LexGenJava.lexStateIndex] < (j * 64 + k)))
                break;
              else
                if ((LexGenJava.toSkip[kind / 64] & (1L << (kind % 64))) != 0L &&
                    (LexGenJava.toSpecial[kind / 64] & (1L << (kind % 64))) == 0L &&
                    LexGenJava.actions[kind] == null &&
                    LexGenJava.newLexState[kind] == null)
                {
                  LexGenJava.addCharToSkip (c, kind);

                  if (Options.getIgnoreCase ())
                  {
                    if (c != Character.toUpperCase (c))
                      LexGenJava.addCharToSkip (Character.toUpperCase (c), kind);

                    if (c != Character.toLowerCase (c))
                      LexGenJava.addCharToSkip (Character.toLowerCase (c), kind);
                  }
                  continue CaseLoop;
                }
            }
        }

        // Since we know key is a single character ...
        if (Options.getIgnoreCase ())
        {
          if (c != Character.toUpperCase (c))
            codeGenerator.genCodeLine ("      case " + (int) Character.toUpperCase (c) + ":");

          if (c != Character.toLowerCase (c))
            codeGenerator.genCodeLine ("      case " + (int) Character.toLowerCase (c) + ":");
        }

        codeGenerator.genCodeLine ("      case " + (int) c + ":");

        long matchedKind;
        final String prefix = (i == 0) ? "         " : "            ";

        if (info.m_finalKindCnt != 0)
        {
          for (int j = 0; j < maxLongsReqd; j++)
          {
            if ((matchedKind = info.m_finalKinds[j]) == 0L)
              continue;

            for (int k = 0; k < 64; k++)
            {
              if ((matchedKind & (1L << k)) == 0L)
                continue;

              if (ifGenerated)
              {
                codeGenerator.genCode ("         else if ");
              }
              else
                if (i != 0)
                  codeGenerator.genCode ("         if ");

              ifGenerated = true;

              int kindToPrint;
              if (i != 0)
              {
                codeGenerator.genCodeLine ("((active" + j + " & 0x" + Long.toHexString (1L << k) + "L) != 0L)");
              }

              if (s_intermediateKinds != null &&
                  s_intermediateKinds[(j * 64 + k)] != null &&
                  s_intermediateKinds[(j * 64 + k)][i] < (j * 64 + k) &&
                  s_intermediateMatchedPos != null &&
                  s_intermediateMatchedPos[(j * 64 + k)][i] == i)
              {
                JavaCCErrors.warning (" \"" +
                                      JavaCCGlobals.addEscapes (s_allImages[j * 64 + k]) +
                                      "\" cannot be matched as a string literal token " +
                                      "at line " +
                                      getLine (j * 64 + k) +
                                      ", column " +
                                      getColumn (j * 64 + k) +
                                      ". It will be matched as " +
                                      getLabel (s_intermediateKinds[(j * 64 + k)][i]) +
                                      ".");
                kindToPrint = s_intermediateKinds[(j * 64 + k)][i];
              }
              else
                if (i == 0 &&
                    LexGenJava.canMatchAnyChar[LexGenJava.lexStateIndex] >= 0 &&
                    LexGenJava.canMatchAnyChar[LexGenJava.lexStateIndex] < (j * 64 + k))
                {
                  JavaCCErrors.warning (" \"" +
                                        JavaCCGlobals.addEscapes (s_allImages[j * 64 + k]) +
                                        "\" cannot be matched as a string literal token " +
                                        "at line " +
                                        getLine (j * 64 + k) +
                                        ", column " +
                                        getColumn (j * 64 + k) +
                                        ". It will be matched as " +
                                        getLabel (LexGenJava.canMatchAnyChar[LexGenJava.lexStateIndex]) +
                                        ".");
                  kindToPrint = LexGenJava.canMatchAnyChar[LexGenJava.lexStateIndex];
                }
                else
                  kindToPrint = j * 64 + k;

              if (!s_subString[(j * 64 + k)])
              {
                final int stateSetName = _getStateSetForKind (i, j * 64 + k);

                if (stateSetName != -1)
                {
                  createStartNfa = true;
                  codeGenerator.genCodeLine (prefix +
                                             "return jjStartNfaWithStates" +
                                             LexGenJava.lexStateSuffix +
                                             "(" +
                                             i +
                                             ", " +
                                             kindToPrint +
                                             ", " +
                                             stateSetName +
                                             ");");
                }
                else
                  codeGenerator.genCodeLine (prefix + "return jjStopAtPos" + "(" + i + ", " + kindToPrint + ");");
              }
              else
              {
                if ((LexGenJava.initMatch[LexGenJava.lexStateIndex] != 0 &&
                     LexGenJava.initMatch[LexGenJava.lexStateIndex] != Integer.MAX_VALUE) ||
                    i != 0)
                {
                  codeGenerator.genCodeLine ("         {");
                  codeGenerator.genCodeLine (prefix + "jjmatchedKind = " + kindToPrint + ";");
                  codeGenerator.genCodeLine (prefix + "jjmatchedPos = " + i + ";");
                  codeGenerator.genCodeLine ("         }");
                }
                else
                  codeGenerator.genCodeLine (prefix + "jjmatchedKind = " + kindToPrint + ";");
              }
            }
          }
        }

        if (info.m_validKindCnt != 0)
        {
          atLeastOne = false;

          if (i == 0)
          {
            codeGenerator.genCode ("         return ");

            codeGenerator.genCode ("jjMoveStringLiteralDfa" + (i + 1) + LexGenJava.lexStateSuffix + "(");
            int j = 0;
            for (; j < maxLongsReqd - 1; j++)
              if ((i + 1) <= s_maxLenForActive[j])
              {
                if (atLeastOne)
                  codeGenerator.genCode (", ");
                else
                  atLeastOne = true;

                codeGenerator.genCode ("0x" + Long.toHexString (info.m_validKinds[j]) + Options.getLongSuffix ());
              }

            if ((i + 1) <= s_maxLenForActive[j])
            {
              if (atLeastOne)
                codeGenerator.genCode (", ");

              codeGenerator.genCode ("0x" + Long.toHexString (info.m_validKinds[j]) + Options.getLongSuffix ());
            }
            codeGenerator.genCodeLine (");");
          }
          else
          {
            codeGenerator.genCode ("         return ");

            codeGenerator.genCode ("jjMoveStringLiteralDfa" + (i + 1) + LexGenJava.lexStateSuffix + "(");

            int j = 0;
            for (; j < maxLongsReqd - 1; j++)
              if ((i + 1) <= s_maxLenForActive[j] + 1)
              {
                if (atLeastOne)
                  codeGenerator.genCode (", ");
                else
                  atLeastOne = true;

                if (info.m_validKinds[j] != 0L)
                  codeGenerator.genCode ("active" +
                                         j +
                                         ", 0x" +
                                         Long.toHexString (info.m_validKinds[j]) +
                                         Options.getLongSuffix ());
                else
                  codeGenerator.genCode ("active" + j + ", 0L");
              }

            if ((i + 1) <= s_maxLenForActive[j] + 1)
            {
              if (atLeastOne)
                codeGenerator.genCode (", ");
              if (info.m_validKinds[j] != 0L)
                codeGenerator.genCode ("active" +
                                       j +
                                       ", 0x" +
                                       Long.toHexString (info.m_validKinds[j]) +
                                       Options.getLongSuffix ());
              else
                codeGenerator.genCode ("active" + j + ", 0L");
            }

            codeGenerator.genCodeLine (");");
          }
        }
        else
        {
          // A very special case.
          if (i == 0 && LexGenJava.mixed[LexGenJava.lexStateIndex])
          {

            if (NfaState.s_generatedStates != 0)
              codeGenerator.genCodeLine ("         return jjMoveNfa" +
                                         LexGenJava.lexStateSuffix +
                                         "(" +
                                         NfaState.initStateName () +
                                         ", 0);");
            else
              codeGenerator.genCodeLine ("         return 1;");
          }
          else
            if (i != 0) // No more str literals to look for
            {
              codeGenerator.genCodeLine ("         break;");
              startNfaNeeded = true;
            }
        }
      }

      /*
       * default means that the current character is not in any of the strings
       * at this position.
       */
      codeGenerator.genCodeLine ("      default :");

      if (Options.getDebugTokenManager ())
      {
        switch (codeGenerator.getOutputLanguage ())
        {
          case JAVA:
            codeGenerator.genCodeLine ("      debugStream.println(\"   No string literal matches possible.\");");
            break;
          case CPP:
            codeGenerator.genCodeLine ("      fprintf(debugStream, \"   No string literal matches possible.\\n\");");
            break;
          default:
            throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
        }
      }

      if (NfaState.s_generatedStates != 0)
      {
        if (i == 0)
        {
          /*
           * This means no string literal is possible. Just move nfa with this
           * guy and return.
           */
          codeGenerator.genCodeLine ("         return jjMoveNfa" +
                                     LexGenJava.lexStateSuffix +
                                     "(" +
                                     NfaState.initStateName () +
                                     ", 0);");
        }
        else
        {
          codeGenerator.genCodeLine ("         break;");
          startNfaNeeded = true;
        }
      }
      else
      {
        codeGenerator.genCodeLine ("         return " + (i + 1) + ";");
      }

      codeGenerator.genCodeLine ("   }");

      if (i != 0)
      {
        if (startNfaNeeded)
        {
          if (!LexGenJava.mixed[LexGenJava.lexStateIndex] && NfaState.s_generatedStates != 0)
          {
            /*
             * Here, a string literal is successfully matched and no more string
             * literals are possible. So set the kind and state set upto and
             * including this position for the matched string.
             */

            codeGenerator.genCode ("   return jjStartNfa" + LexGenJava.lexStateSuffix + "(" + (i - 1) + ", ");

            int k = 0;
            for (; k < maxLongsReqd - 1; k++)
              if (i <= s_maxLenForActive[k])
                codeGenerator.genCode ("active" + k + ", ");
              else
                codeGenerator.genCode ("0L, ");
            if (i <= s_maxLenForActive[k])
              codeGenerator.genCodeLine ("active" + k + ");");
            else
              codeGenerator.genCodeLine ("0L);");
          }
          else
            if (NfaState.s_generatedStates != 0)
              codeGenerator.genCodeLine ("   return jjMoveNfa" +
                                         LexGenJava.lexStateSuffix +
                                         "(" +
                                         NfaState.initStateName () +
                                         ", " +
                                         i +
                                         ");");
            else
              codeGenerator.genCodeLine ("   return " + (i + 1) + ";");
        }
      }

      codeGenerator.genCodeLine ("}");
    }

    if (!LexGenJava.mixed[LexGenJava.lexStateIndex] && NfaState.s_generatedStates != 0 && createStartNfa)
      dumpStartWithStates (codeGenerator);
  }

  static final int getStrKind (final String str)
  {
    for (int i = 0; i < s_maxStrKind; i++)
    {
      if (LexGenJava.lexStates[i] != LexGenJava.lexStateIndex)
        continue;

      final String image = s_allImages[i];
      if (image != null && image.equals (str))
        return i;
    }

    return Integer.MAX_VALUE;
  }

  static void generateNfaStartStates (final CodeGenerator codeGenerator, final NfaState initialState)
  {
    final boolean [] seen = new boolean [NfaState.s_generatedStates];
    final Map <String, String> stateSets = new HashMap <> ();
    String stateSetString = "";
    int i, j, kind, jjmatchedPos = 0;
    final int maxKindsReqd = s_maxStrKind / 64 + 1;
    long [] actives;
    List <NfaState> newStates = new ArrayList <> ();
    List <NfaState> oldStates = null;
    List <NfaState> jjtmpStates;

    s_statesForPos = new Map [s_maxLen];
    s_intermediateKinds = new int [s_maxStrKind + 1] [];
    s_intermediateMatchedPos = new int [s_maxStrKind + 1] [];

    for (i = 0; i < s_maxStrKind; i++)
    {
      if (LexGenJava.lexStates[i] != LexGenJava.lexStateIndex)
        continue;

      final String image = s_allImages[i];

      if (image == null || image.length () < 1)
        continue;

      try
      {
        oldStates = new ArrayList <> (initialState.epsilonMoves);
        if (oldStates.size () == 0)
        {
          dumpNfaStartStatesCode (s_statesForPos, codeGenerator);
          return;
        }
      }
      catch (final Exception e)
      {
        JavaCCErrors.semantic_error ("Error cloning state vector");
      }

      s_intermediateKinds[i] = new int [image.length ()];
      s_intermediateMatchedPos[i] = new int [image.length ()];
      jjmatchedPos = 0;
      kind = Integer.MAX_VALUE;

      for (j = 0; j < image.length (); j++)
      {
        if (oldStates == null || oldStates.size () <= 0)
        {
          // Here, j > 0
          kind = s_intermediateKinds[i][j] = s_intermediateKinds[i][j - 1];
          jjmatchedPos = s_intermediateMatchedPos[i][j] = s_intermediateMatchedPos[i][j - 1];
        }
        else
        {
          kind = NfaState.moveFromSet (image.charAt (j), oldStates, newStates);
          oldStates.clear ();

          if (j == 0 &&
              kind != Integer.MAX_VALUE &&
              LexGenJava.canMatchAnyChar[LexGenJava.lexStateIndex] != -1 &&
              kind > LexGenJava.canMatchAnyChar[LexGenJava.lexStateIndex])
            kind = LexGenJava.canMatchAnyChar[LexGenJava.lexStateIndex];

          if (getStrKind (image.substring (0, j + 1)) < kind)
          {
            s_intermediateKinds[i][j] = kind = Integer.MAX_VALUE;
            jjmatchedPos = 0;
          }
          else
            if (kind != Integer.MAX_VALUE)
            {
              s_intermediateKinds[i][j] = kind;
              jjmatchedPos = s_intermediateMatchedPos[i][j] = j;
            }
            else
              if (j == 0)
                kind = s_intermediateKinds[i][j] = Integer.MAX_VALUE;
              else
              {
                kind = s_intermediateKinds[i][j] = s_intermediateKinds[i][j - 1];
                jjmatchedPos = s_intermediateMatchedPos[i][j] = s_intermediateMatchedPos[i][j - 1];
              }

          stateSetString = NfaState.getStateSetString (newStates);
        }

        if (kind == Integer.MAX_VALUE && (newStates == null || newStates.size () == 0))
          continue;

        int p;
        if (stateSets.get (stateSetString) == null)
        {
          stateSets.put (stateSetString, stateSetString);
          for (p = 0; p < newStates.size (); p++)
          {
            if (seen[newStates.get (p).stateName])
              newStates.get (p).inNextOf++;
            else
              seen[newStates.get (p).stateName] = true;
          }
        }
        else
        {
          for (p = 0; p < newStates.size (); p++)
            seen[newStates.get (p).stateName] = true;
        }

        jjtmpStates = oldStates;
        oldStates = newStates;
        (newStates = jjtmpStates).clear ();

        if (s_statesForPos[j] == null)
          s_statesForPos[j] = new HashMap <> ();

        actives = s_statesForPos[j].computeIfAbsent (kind + ", " + jjmatchedPos + ", " + stateSetString,
                                                     k -> new long [maxKindsReqd]);

        actives[i / 64] |= 1L << (i % 64);
        // String name = NfaState.StoreStateSet(stateSetString);
      }
    }

    // TODO(Sreeni) : Fix this mess.
    if (Options.getTokenManagerCodeGenerator () == null)
    {
      dumpNfaStartStatesCode (s_statesForPos, codeGenerator);
    }
  }

  static void dumpNfaStartStatesCode (final Map <String, long []> [] statesForPos, final CodeGenerator codeGenerator)
  {
    if (s_maxStrKind == 0)
    { // No need to generate this function
      return;
    }

    int i;
    final int maxKindsReqd = s_maxStrKind / 64 + 1;
    boolean condGenerated = false;
    int ind = 0;

    final StringBuilder params = new StringBuilder ();
    for (i = 0; i < maxKindsReqd - 1; i++)
      params.append ("" + Options.getLongType () + " active" + i + ", ");
    params.append ("" + Options.getLongType () + " active" + i + ")");

    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    switch (codeGenerator.getOutputLanguage ())
    {
      case JAVA:
        codeGenerator.genCode ("private" +
                               (Options.getStatic () ? " static" : "") +
                               " final int jjStopStringLiteralDfa" +
                               LexGenJava.lexStateSuffix +
                               "(int pos, " +
                               params);
        break;
      case CPP:
        codeGenerator.generateMethodDefHeader (" int",
                                               LexGenJava.tokMgrClassName,
                                               "jjStopStringLiteralDfa" +
                                                                           LexGenJava.lexStateSuffix +
                                                                           "(int pos, " +
                                                                           params);
        break;
      default:
        throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
    }

    codeGenerator.genCodeLine ("{");

    if (Options.getDebugTokenManager ())
    {
      // TODO :: CBA -- Require Unification of output language specific
      // processing into a single Enum class
      switch (codeGenerator.getOutputLanguage ())
      {
        case JAVA:
          codeGenerator.genCodeLine ("      debugStream.println(\"   No more string literal token matches are possible.\");");
          break;
        case CPP:
          codeGenerator.genCodeLine ("      fprintf(debugStream, \"   No more string literal token matches are possible.\");");
          break;
        default:
          throw new UnsupportedOutputLanguageException (codeGenerator.getOutputLanguage ());
      }
    }

    codeGenerator.genCodeLine ("   switch (pos)");
    codeGenerator.genCodeLine ("   {");

    for (i = 0; i < s_maxLen - 1; i++)
    {
      if (statesForPos[i] == null)
        continue;

      codeGenerator.genCodeLine ("      case " + i + ":");

      for (final Map.Entry <String, long []> aEntry : statesForPos[i].entrySet ())
      {
        String stateSetString = aEntry.getKey ();
        final long [] actives = aEntry.getValue ();

        for (int j = 0; j < maxKindsReqd; j++)
        {
          if (actives[j] == 0L)
            continue;

          if (condGenerated)
            codeGenerator.genCode (" || ");
          else
            codeGenerator.genCode ("         if (");

          condGenerated = true;

          codeGenerator.genCode ("(active" + j + " & 0x" + Long.toHexString (actives[j]) + "L) != 0L");
        }

        if (condGenerated)
        {
          codeGenerator.genCodeLine (")");

          String kindStr = stateSetString.substring (0, ind = stateSetString.indexOf (", "));
          String afterKind = stateSetString.substring (ind + 2);
          final int jjmatchedPos = Integer.parseInt (afterKind.substring (0, afterKind.indexOf (", ")));

          if (!kindStr.equals (String.valueOf (Integer.MAX_VALUE)))
            codeGenerator.genCodeLine ("         {");

          if (!kindStr.equals (String.valueOf (Integer.MAX_VALUE)))
          {
            if (i == 0)
            {
              codeGenerator.genCodeLine ("            jjmatchedKind = " + kindStr + ";");

              if ((LexGenJava.initMatch[LexGenJava.lexStateIndex] != 0 &&
                   LexGenJava.initMatch[LexGenJava.lexStateIndex] != Integer.MAX_VALUE))
                codeGenerator.genCodeLine ("            jjmatchedPos = 0;");
            }
            else
              if (i == jjmatchedPos)
              {
                if (s_subStringAtPos[i])
                {
                  codeGenerator.genCodeLine ("            if (jjmatchedPos != " + i + ")");
                  codeGenerator.genCodeLine ("            {");
                  codeGenerator.genCodeLine ("               jjmatchedKind = " + kindStr + ";");
                  codeGenerator.genCodeLine ("               jjmatchedPos = " + i + ";");
                  codeGenerator.genCodeLine ("            }");
                }
                else
                {
                  codeGenerator.genCodeLine ("            jjmatchedKind = " + kindStr + ";");
                  codeGenerator.genCodeLine ("            jjmatchedPos = " + i + ";");
                }
              }
              else
              {
                if (jjmatchedPos > 0)
                  codeGenerator.genCodeLine ("            if (jjmatchedPos < " + jjmatchedPos + ")");
                else
                  codeGenerator.genCodeLine ("            if (jjmatchedPos == 0)");
                codeGenerator.genCodeLine ("            {");
                codeGenerator.genCodeLine ("               jjmatchedKind = " + kindStr + ";");
                codeGenerator.genCodeLine ("               jjmatchedPos = " + jjmatchedPos + ";");
                codeGenerator.genCodeLine ("            }");
              }
          }

          kindStr = stateSetString.substring (0, ind = stateSetString.indexOf (", "));
          afterKind = stateSetString.substring (ind + 2);
          stateSetString = afterKind.substring (afterKind.indexOf (", ") + 2);

          if (stateSetString.equals ("null;"))
            codeGenerator.genCodeLine ("            return -1;");
          else
            codeGenerator.genCodeLine ("            return " + NfaState.addStartStateSet (stateSetString) + ";");

          if (!kindStr.equals (String.valueOf (Integer.MAX_VALUE)))
            codeGenerator.genCodeLine ("         }");
          condGenerated = false;
        }
      }

      codeGenerator.genCodeLine ("         return -1;");
    }

    codeGenerator.genCodeLine ("      default :");
    codeGenerator.genCodeLine ("         return -1;");
    codeGenerator.genCodeLine ("   }");
    codeGenerator.genCodeLine ("}");

    params.setLength (0);
    params.append ("(int pos, ");
    for (i = 0; i < maxKindsReqd - 1; i++)
      params.append ("" + Options.getLongType () + " active" + i + ", ");
    params.append ("" + Options.getLongType () + " active" + i + ")");

    if (codeGenerator.isJavaLanguage ())
    {
      codeGenerator.genCode ("private" +
                             (Options.getStatic () ? " static" : "") +
                             " final int jjStartNfa" +
                             LexGenJava.lexStateSuffix +
                             params);
    }
    else
    {
      codeGenerator.generateMethodDefHeader ("int ",
                                             LexGenJava.tokMgrClassName,
                                             "jjStartNfa" + LexGenJava.lexStateSuffix + params);
    }
    codeGenerator.genCodeLine ("{");

    if (LexGenJava.mixed[LexGenJava.lexStateIndex])
    {
      if (NfaState.s_generatedStates != 0)
        codeGenerator.genCodeLine ("   return jjMoveNfa" +
                                   LexGenJava.lexStateSuffix +
                                   "(" +
                                   NfaState.initStateName () +
                                   ", pos + 1);");
      else
        codeGenerator.genCodeLine ("   return pos + 1;");

      codeGenerator.genCodeLine ("}");
      return;
    }

    codeGenerator.genCode ("   return jjMoveNfa" +
                           LexGenJava.lexStateSuffix +
                           "(" +
                           "jjStopStringLiteralDfa" +
                           LexGenJava.lexStateSuffix +
                           "(pos, ");
    for (i = 0; i < maxKindsReqd - 1; i++)
      codeGenerator.genCode ("active" + i + ", ");
    codeGenerator.genCode ("active" + i + ")");
    codeGenerator.genCodeLine (", pos + 1);");
    codeGenerator.genCodeLine ("}");
  }

  /**
   * Return to original state.
   */
  public static void reInit ()
  {
    reInitStatic ();

    s_charCnt = 0;
    s_allImages = null;
    boilerPlateDumped = false;
  }

  @Override
  public StringBuilder dump (final int indent, final Set <? super Expansion> alreadyDumped)
  {
    final StringBuilder sb = super.dump (indent, alreadyDumped).append (' ').append (m_image);
    return sb;
  }

  @Override
  public String toString ()
  {
    return super.toString () + " - " + m_image;
  }

  /*
   * static void GenerateData(TokenizerData tokenizerData) { Map tab; String
   * key; KindInfo info; for (int i = 0; i < maxLen; i++) { tab =
   * (Map)charPosKind.get(i); String[] keys = ReArrange(tab); if
   * (Options.getIgnoreCase()) { for (String s : keys) { char c = s.charAt(0);
   * tab.put(Character.toLowerCase(c), tab.get(c));
   * tab.put(Character.toUpperCase(c), tab.get(c)); } } for (int q = 0; q <
   * keys.length; q++) { key = keys[q]; info = (KindInfo)tab.get(key); char c =
   * key.charAt(0); for (int kind : info.finalKindSet) {
   * tokenizerData.addDfaFinalKindAndState( i, c, kind, GetStateSetForKind(i,
   * kind)); } for (int kind : info.validKindSet) {
   * tokenizerData.addDfaValidKind(i, c, kind); } } } for (int i = 0; i <
   * maxLen; i++) { Enumeration e = statesForPos[i].keys(); while
   * (e.hasMoreElements()) { String stateSetString = (String)e.nextElement();
   * long[] actives = (long[])statesForPos[i].get(stateSetString); int ind =
   * stateSetString.indexOf(", "); String kindStr = stateSetString.substring(0,
   * ind); String afterKind = stateSetString.substring(ind + 2); stateSetString
   * = afterKind.substring(afterKind.indexOf(", ") + 2); BitSet bits =
   * BitSet.valueOf(actives); for (int j = 0; j < bits.length(); j++) { if
   * (bits.get(j)) tokenizerData.addFinalDfaKind(j); } // Pos
   * codeGenerator.genCode( ", " + afterKind.substring(0,
   * afterKind.indexOf(", "))); // Kind codeGenerator.genCode(", " + kindStr);
   * // State if (stateSetString.equals("null;")) {
   * codeGenerator.genCodeLine(", -1"); } else { codeGenerator.genCodeLine( ", "
   * + NfaState.AddStartStateSet(stateSetString)); } }
   * codeGenerator.genCode("}"); } codeGenerator.genCodeLine("};"); }
   */

  static final Map <Integer, List <String>> literalsByLength = new HashMap <> ();
  static final Map <Integer, List <Integer>> literalKinds = new HashMap <> ();
  static final Map <Integer, Integer> kindToLexicalState = new HashMap <> ();
  static final Map <Integer, NfaState> nfaStateMap = new HashMap <> ();

  public static void updateStringLiteralData (final int lexStateIndex)
  {
    for (int kind = 0; kind < s_allImages.length; kind++)
    {
      if (s_allImages[kind] == null || s_allImages[kind].equals ("") || LexGenJava.lexStates[kind] != lexStateIndex)
      {
        continue;
      }
      String s = s_allImages[kind];
      int actualKind;
      if (s_intermediateKinds != null &&
          s_intermediateKinds[kind][s.length () - 1] != Integer.MAX_VALUE &&
          s_intermediateKinds[kind][s.length () - 1] < kind)
      {
        JavaCCErrors.warning ("Token: " +
                              s +
                              " will not be matched as " +
                              "specified. It will be matched as token " +
                              "of kind: " +
                              s_intermediateKinds[kind][s.length () - 1] +
                              " instead.");
        actualKind = s_intermediateKinds[kind][s.length () - 1];
      }
      else
      {
        actualKind = kind;
      }
      kindToLexicalState.put (actualKind, lexStateIndex);
      if (Options.getIgnoreCase ())
      {
        s = s.toLowerCase ();
      }
      final char c = s.charAt (0);
      final int key = LexGenJava.lexStateIndex << 16 | c;
      List <String> l = literalsByLength.get (key);
      List <Integer> kinds = literalKinds.get (key);
      int j = 0;
      if (l == null)
      {
        literalsByLength.put (key, l = new ArrayList <> ());
        assert (kinds == null);
        kinds = new ArrayList <> ();
        literalKinds.put (key, kinds = new ArrayList <> ());
      }
      while (j < l.size () && l.get (j).length () > s.length ())
        j++;
      l.add (j, s);
      kinds.add (j, actualKind);
      final int stateIndex = _getStateSetForKind (s.length () - 1, kind);
      if (stateIndex != -1)
      {
        nfaStateMap.put (actualKind, NfaState.getNfaState (stateIndex));
      }
      else
      {
        nfaStateMap.put (actualKind, null);
      }
    }
  }

  public static void BuildTokenizerData (final TokenizerData tokenizerData)
  {
    final Map <Integer, Integer> nfaStateIndices = new HashMap <> ();
    for (final int kind : nfaStateMap.keySet ())
    {
      if (nfaStateMap.get (kind) != null)
      {
        nfaStateIndices.put (kind, nfaStateMap.get (kind).stateName);
      }
      else
      {
        nfaStateIndices.put (kind, -1);
      }
    }
    tokenizerData.setLiteralSequence (literalsByLength);
    tokenizerData.setLiteralKinds (literalKinds);
    tokenizerData.setKindToNfaStartState (nfaStateIndices);
  }
}
