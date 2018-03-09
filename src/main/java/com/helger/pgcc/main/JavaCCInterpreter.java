package com.helger.pgcc.main;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.helger.commons.io.file.SimpleFileIO;
import com.helger.pgcc.PGPrinter;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.JavaCCParser;
import com.helger.pgcc.parser.LexGenJava;
import com.helger.pgcc.parser.Main;
import com.helger.pgcc.parser.MetaParseException;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.Semanticize;
import com.helger.pgcc.parser.StringProvider;
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
        PGPrinter.info ("Argument \"" + args[arg] + "\" must be an option setting.");
        System.exit (1);
      }
      Options.setCmdLineOption (args[arg]);
    }

    final File fp = new File (args[args.length - 2]);
    final String grammar = SimpleFileIO.getFileAsString (fp, Options.getGrammarEncoding ());

    final File inputFile = new File (args[args.length - 1]);
    final String input = SimpleFileIO.getFileAsString (inputFile, Options.getGrammarEncoding ());

    final long l = System.currentTimeMillis ();
    new JavaCCInterpreter ().runTokenizer (grammar, input);
    PGPrinter.error ("Tokenized in: " + (System.currentTimeMillis () - l));
  }

  public void runTokenizer (final String grammar, final String input)
  {
    try
    {
      final JavaCCParser parser = new JavaCCParser (new StringProvider (grammar));
      parser.javacc_input ();
      Semanticize.start ();
      final LexGenJava lg = new LexGenJava ();
      LexGenJava.s_generateDataOnly = true;
      lg.start ();
      final TokenizerData td = LexGenJava.s_tokenizerData;
      if (JavaCCErrors.getErrorCount () == 0)
      {
        _tokenize (td, input);
      }
    }
    catch (final MetaParseException e)
    {
      PGPrinter.info ("Detected " +
                      JavaCCErrors.getErrorCount () +
                      " errors and " +
                      JavaCCErrors.getWarningCount () +
                      " warnings.");
    }
    catch (final Exception e)
    {
      PGPrinter.info (e.toString ());
      PGPrinter.info ("Detected " +
                      (JavaCCErrors.getErrorCount () + 1) +
                      " errors and " +
                      JavaCCErrors.getWarningCount () +
                      " warnings.");
    }
  }

  private static void _tokenize (final TokenizerData td, final String input)
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
          PGPrinter.error ("Actions not implemented (yet) in intererpreted mode");
        }
        if (matchInfo.m_matchType == TokenizerData.EMatchType.TOKEN)
        {
          PGPrinter.error ("Token: " + matchedKind + "; image: \"" + input.substring (beg, matchedPos + 1) + "\"");
        }
        if (matchInfo.m_newLexState != -1)
        {
          curLexState = matchInfo.m_newLexState;
        }
        curPos = matchedPos + 1;
      }
      else
      {
        PGPrinter.error ("Encountered token error at char: " + input.charAt (curPos));
        return;
      }
    }
    PGPrinter.error ("Matched EOF");
  }
}
