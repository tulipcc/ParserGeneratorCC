/**
 * Copyright 2017-2018 Philip Helger, pgcc@helger.com
 *
 * Copyright 2011 Google Inc. All Rights Reserved.
 * Author: sreeni@google.com (Sreeni Viswanadha)
 *
 * Copyright (c) 2006, Sun Microsystems, Inc.
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
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.JavaCCParser;
import com.helger.pgcc.parser.LexGenJava;
import com.helger.pgcc.parser.Main;
import com.helger.pgcc.parser.MetaParseException;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.Semanticize;
import com.helger.pgcc.parser.TokenizerData;

public class JavaCCInterpreter
{
  public static void main (final String [] args) throws Exception
  {
    // Initialize all static state
    Main.reInitAll ();
    for (int arg = 0; arg < args.length - 2; arg++)
    {
      if (!Options.isOption (args[arg]))
      {
        System.out.println ("Argument \"" + args[arg] + "\" must be an option setting.");
        System.exit (1);
      }
      Options.setCmdLineOption (args[arg]);
    }

    String input = "";
    String grammar = "";
    try
    {
      final File fp = new File (args[args.length - 2]);
      byte [] buf = new byte [(int) fp.length ()];
      try (DataInputStream aDIS = new DataInputStream (new BufferedInputStream (new FileInputStream (fp))))
      {
        aDIS.readFully (buf);
      }
      grammar = new String (buf);
      final File inputFile = new File (args[args.length - 1]);
      buf = new byte [(int) inputFile.length ()];
      try (DataInputStream aDIS = new DataInputStream (new BufferedInputStream (new FileInputStream (inputFile))))
      {
        aDIS.readFully (buf);
      }
      input = new String (buf);
    }
    catch (final FileNotFoundException e)
    {
      e.printStackTrace ();
      System.exit (1);
    }
    catch (final Throwable t)
    {
      System.exit (1);
    }
    final long l = System.currentTimeMillis ();
    new JavaCCInterpreter ().runTokenizer (grammar, input);
    System.err.println ("Tokenized in: " + (System.currentTimeMillis () - l));
  }

  public void runTokenizer (final String grammar, final String input)
  {
    try
    {
      final JavaCCParser parser = new JavaCCParser (new StringReader (grammar));
      parser.javacc_input ();
      Semanticize.start ();
      final LexGenJava lg = new LexGenJava ();
      LexGenJava.s_generateDataOnly = true;
      lg.start ();
      final TokenizerData td = LexGenJava.s_tokenizerData;
      if (JavaCCErrors.getErrorCount () == 0)
      {
        tokenize (td, input);
      }
    }
    catch (final MetaParseException e)
    {
      System.out.println ("Detected " +
                          JavaCCErrors.getErrorCount () +
                          " errors and " +
                          JavaCCErrors.getWarningCount () +
                          " warnings.");
      System.exit (1);
    }
    catch (final Exception e)
    {
      System.out.println (e.toString ());
      System.out.println ("Detected " +
                          (JavaCCErrors.getErrorCount () + 1) +
                          " errors and " +
                          JavaCCErrors.getWarningCount () +
                          " warnings.");
      System.exit (1);
    }
  }

  public static void tokenize (final TokenizerData td, final String input)
  {
    // First match the string literals.
    final int input_size = input.length ();
    int curPos = 0;
    int curLexState = td.m_defaultLexState;
    Set <Integer> curStates = new HashSet <> ();
    Set <Integer> newStates = new HashSet <> ();
    while (curPos < input_size)
    {
      final int beg = curPos;
      int matchedPos = beg;
      int matchedKind = Integer.MAX_VALUE;
      int nfaStartState = td.m_initialStates.get (curLexState);

      char c = input.charAt (curPos);
      if (Options.isIgnoreCase ())
        c = Character.toLowerCase (c);
      final int key = curLexState << 16 | c;
      final List <String> literals = td.m_literalSequence.get (key);
      if (literals != null)
      {
        // We need to go in order so that the longest match works.
        int litIndex = 0;
        for (final String s : literals)
        {
          int index = 1;
          // See which literal matches.
          while (index < s.length () && curPos + index < input_size)
          {
            c = input.charAt (curPos + index);
            if (Options.isIgnoreCase ())
              c = Character.toLowerCase (c);
            if (c != s.charAt (index))
              break;
            index++;
          }
          if (index == s.length ())
          {
            // Found a string literal match.
            matchedKind = td.m_literalKinds.get (key).get (litIndex);
            matchedPos = curPos + index - 1;
            nfaStartState = td.m_kindToNfaStartState.get (matchedKind);
            curPos += index;
            break;
          }
          litIndex++;
        }
      }

      if (nfaStartState != -1)
      {
        // We need to add the composite states first.
        int kind = Integer.MAX_VALUE;
        curStates.add (nfaStartState);
        curStates.addAll (td.m_nfa.get (nfaStartState).m_compositeStates);
        do
        {
          c = input.charAt (curPos);
          if (Options.isIgnoreCase ())
            c = Character.toLowerCase (c);
          for (final int state : curStates)
          {
            final TokenizerData.NfaState nfaState = td.m_nfa.get (state);
            if (nfaState.m_characters.contains (c))
            {
              if (kind > nfaState.m_kind)
              {
                kind = nfaState.m_kind;
              }
              newStates.addAll (nfaState.m_nextStates);
            }
          }
          final Set <Integer> tmp = newStates;
          newStates = curStates;
          curStates = tmp;
          newStates.clear ();
          if (kind != Integer.MAX_VALUE)
          {
            matchedKind = kind;
            matchedPos = curPos;
            kind = Integer.MAX_VALUE;
          }
        } while (!curStates.isEmpty () && ++curPos < input_size);
      }
      if (matchedPos == beg && matchedKind > td.m_wildcardKind.get (curLexState))
      {
        matchedKind = td.m_wildcardKind.get (curLexState);
      }
      if (matchedKind != Integer.MAX_VALUE)
      {
        final TokenizerData.MatchInfo matchInfo = td.m_allMatches.get (matchedKind);
        if (matchInfo.m_action != null)
        {
          System.err.println ("Actions not implemented (yet) in intererpreted mode");
        }
        if (matchInfo.m_matchType == TokenizerData.EMatchType.TOKEN)
        {
          System.err.println ("Token: " + matchedKind + "; image: \"" + input.substring (beg, matchedPos + 1) + "\"");
        }
        if (matchInfo.m_newLexState != -1)
        {
          curLexState = matchInfo.m_newLexState;
        }
        curPos = matchedPos + 1;
      }
      else
      {
        System.err.println ("Encountered token error at char: " + input.charAt (curPos));
        System.exit (1);
      }
    }
    System.err.println ("Matched EOF");
  }
}
