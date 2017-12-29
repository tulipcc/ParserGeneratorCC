package com.helger.pgcc.parser;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that implements a table driven code generator for the token manager in
 * java.
 */
public class TableDrivenJavaCodeGenerator implements TokenManagerCodeGenerator
{
  private static final String TokenManagerTemplate = "/templates/TableDrivenTokenManager.template";
  private final CodeGenerator m_codeGenerator = new CodeGenerator ();

  @Override
  public void generateCode (final TokenizerData tokenizerData)
  {
    final String superClass = (String) Options.getOptions ().get (Options.USEROPTION__TOKEN_MANAGER_SUPER_CLASS);
    final Map <String, Object> options = Options.getOptions ();
    options.put ("maxOrdinal", tokenizerData.allMatches.size ());
    options.put ("maxLexStates", tokenizerData.lexStateNames.length);
    options.put ("stateSetSize", tokenizerData.nfa.size ());
    options.put ("parserName", tokenizerData.parserName);
    options.put ("maxLongs", tokenizerData.allMatches.size () / 64 + 1);
    options.put ("parserName", tokenizerData.parserName);
    options.put ("charStreamName", CodeGenerator.getCharStreamName ());
    options.put ("defaultLexState", tokenizerData.defaultLexState);
    options.put ("decls", tokenizerData.decls);
    options.put ("superClass", (superClass == null || superClass.equals ("")) ? "" : "extends " + superClass);
    options.put ("noDfa", Options.getNoDfa ());
    options.put ("generatedStates", tokenizerData.nfa.size ());
    try
    {
      m_codeGenerator.writeTemplate (TokenManagerTemplate, options);
      _dumpDfaTables (m_codeGenerator, tokenizerData);
      dumpNfaTables (m_codeGenerator, tokenizerData);
      _dumpMatchInfo (m_codeGenerator, tokenizerData);
    }
    catch (final IOException ioe)
    {
      assert (false);
    }
  }

  public void finish (final TokenizerData tokenizerData)
  {
    // TODO(sreeni) : Fix this mess.
    m_codeGenerator.genCodeLine ("\n}");
    if (!Options.getBuildParser ())
      return;
    final String fileName = Options.getOutputDirectory () +
                            File.separator +
                            tokenizerData.parserName +
                            "TokenManager.java";
    m_codeGenerator.saveOutput (fileName);
  }

  private void _dumpDfaTables (final CodeGenerator codeGenerator, final TokenizerData tokenizerData)
  {
    final Map <Integer, int []> startAndSize = new HashMap <> ();
    int i = 0;

    codeGenerator.genCodeLine ("private static final int[] stringLiterals = {");
    for (final int key : tokenizerData.literalSequence.keySet ())
    {
      final int [] arr = new int [2];
      final List <String> l = tokenizerData.literalSequence.get (key);
      final List <Integer> kinds = tokenizerData.literalKinds.get (key);
      arr[0] = i;
      arr[1] = l.size ();
      int j = 0;
      if (i > 0)
        codeGenerator.genCodeLine (", ");
      for (final String s : l)
      {
        if (j > 0)
          codeGenerator.genCodeLine (", ");
        codeGenerator.genCode (Integer.toString (s.length ()));
        for (int k = 0; k < s.length (); k++)
        {
          codeGenerator.genCode (", ");
          codeGenerator.genCode (Integer.toString (s.charAt (k)));
          i++;
        }
        final int kind = kinds.get (j);
        codeGenerator.genCode (", " + kind);
        codeGenerator.genCode (", " + tokenizerData.kindToNfaStartState.get (kind));
        i += 3;
        j++;
      }
      startAndSize.put (key, arr);
    }
    codeGenerator.genCodeLine ("};");

    codeGenerator.genCodeLine ("private static final java.util.Map<Integer, int[]> startAndSize =\n" +
                               "    new java.util.HashMap<Integer, int[]>();");

    // Static block to actually initialize the map from the int array above.
    codeGenerator.genCodeLine ("static {");
    for (final int key : tokenizerData.literalSequence.keySet ())
    {
      final int [] arr = startAndSize.get (key);
      codeGenerator.genCodeLine ("startAndSize.put(" + key + ", new int[]{" + arr[0] + ", " + arr[1] + "});");
    }
    codeGenerator.genCodeLine ("}");
  }

  private void dumpNfaTables (final CodeGenerator codeGenerator, final TokenizerData tokenizerData)
  {
    // WE do the following for java so that the generated code is reasonable
    // size and can be compiled. May not be needed for other languages.
    codeGenerator.genCodeLine ("private static final long[][] jjCharData = {");
    final Map <Integer, TokenizerData.NfaState> nfa = tokenizerData.nfa;
    for (int i = 0; i < nfa.size (); i++)
    {
      final TokenizerData.NfaState tmp = nfa.get (i);
      if (i > 0)
        codeGenerator.genCodeLine (",");
      if (tmp == null)
      {
        codeGenerator.genCode ("{}");
        continue;
      }
      codeGenerator.genCode ("{");
      final BitSet bits = new BitSet ();
      for (final char c : tmp.characters)
      {
        bits.set (c);
      }
      final long [] longs = bits.toLongArray ();
      for (int k = 0; k < longs.length; k++)
      {
        int rep = 1;
        while (k + rep < longs.length && longs[k + rep] == longs[k])
          rep++;
        if (k > 0)
          codeGenerator.genCode (", ");
        codeGenerator.genCode (rep + ", ");
        codeGenerator.genCode ("0x" + Long.toHexString (longs[k]) + "L");
        k += rep - 1;
      }
      codeGenerator.genCode ("}");
    }
    codeGenerator.genCodeLine ("};");

    codeGenerator.genCodeLine ("private static final long[][] jjChars = ");
    codeGenerator.genCodeLine ("    new long[" + tokenizerData.nfa.size () + "][(Character.MAX_VALUE >> 6) + 1]; ");
    codeGenerator.genCodeLine ("static { ");
    codeGenerator.genCodeLine ("  for (int i = 0; i < " + tokenizerData.nfa.size () + "; i++) { ");
    codeGenerator.genCodeLine ("    int ind = 0; ");
    codeGenerator.genCodeLine ("    for (int j = 0; j < jjCharData[i].length; j += 2) { ");
    codeGenerator.genCodeLine ("      for (int k = 0; k < (int)jjCharData[i][j]; k++) { ");
    codeGenerator.genCodeLine ("        jjChars[i][ind++] = jjCharData[i][j + 1]; ");
    codeGenerator.genCodeLine ("      } ");
    codeGenerator.genCodeLine ("    } ");
    codeGenerator.genCodeLine ("  } ");
    codeGenerator.genCodeLine ("} ");

    codeGenerator.genCodeLine ("private static final int[][] jjcompositeState = {");
    for (int i = 0; i < nfa.size (); i++)
    {
      final TokenizerData.NfaState tmp = nfa.get (i);
      if (i > 0)
        codeGenerator.genCodeLine (", ");
      if (tmp == null)
      {
        codeGenerator.genCode ("{}");
        continue;
      }
      codeGenerator.genCode ("{");
      int k = 0;
      for (final Integer st : tmp.compositeStates)
      {
        if (k++ > 0)
          codeGenerator.genCode (", ");
        codeGenerator.genCode (st.toString ());
      }
      codeGenerator.genCode ("}");
    }
    codeGenerator.genCodeLine ("};");

    codeGenerator.genCodeLine ("private static final int[] jjmatchKinds = {");
    for (int i = 0; i < nfa.size (); i++)
    {
      final TokenizerData.NfaState tmp = nfa.get (i);
      if (i > 0)
        codeGenerator.genCodeLine (", ");
      // TODO(sreeni) : Fix this mess.
      if (tmp == null)
      {
        codeGenerator.genCode (Integer.toString (Integer.MAX_VALUE));
        continue;
      }
      codeGenerator.genCode (Integer.toString (tmp.kind));
    }
    codeGenerator.genCodeLine ("};");

    codeGenerator.genCodeLine ("private static final int[][] jjnextStateSet = {");
    for (int i = 0; i < nfa.size (); i++)
    {
      final TokenizerData.NfaState tmp = nfa.get (i);
      if (i > 0)
        codeGenerator.genCodeLine (", ");
      if (tmp == null)
      {
        codeGenerator.genCode ("{}");
        continue;
      }
      int k = 0;
      codeGenerator.genCode ("{");
      for (final Integer s : tmp.nextStates)
      {
        if (k++ > 0)
          codeGenerator.genCode (", ");
        codeGenerator.genCode (s.toString ());
      }
      codeGenerator.genCode ("}");
    }
    codeGenerator.genCodeLine ("};");

    codeGenerator.genCodeLine ("private static final int[] jjInitStates = {");
    int k = 0;
    for (final Integer a : tokenizerData.initialStates.values ())
    {
      if (k++ > 0)
        codeGenerator.genCode (", ");
      codeGenerator.genCode (a.toString ());
    }
    codeGenerator.genCodeLine ("};");

    codeGenerator.genCodeLine ("private static final int[] canMatchAnyChar = {");
    k = 0;
    for (final Integer a : tokenizerData.wildcardKind.values ())
    {
      if (k++ > 0)
        codeGenerator.genCode (", ");
      codeGenerator.genCode (a.toString ());
    }
    codeGenerator.genCodeLine ("};");
  }

  private void _dumpMatchInfo (final CodeGenerator codeGenerator, final TokenizerData tokenizerData)
  {
    final Map <Integer, TokenizerData.MatchInfo> allMatches = tokenizerData.allMatches;

    // A bit ugly.

    final BitSet toSkip = new BitSet (allMatches.size ());
    final BitSet toSpecial = new BitSet (allMatches.size ());
    final BitSet toMore = new BitSet (allMatches.size ());
    final BitSet toToken = new BitSet (allMatches.size ());
    final int [] newStates = new int [allMatches.size ()];
    toSkip.set (allMatches.size () + 1, true);
    toToken.set (allMatches.size () + 1, true);
    toMore.set (allMatches.size () + 1, true);
    toSpecial.set (allMatches.size () + 1, true);
    // Kind map.
    codeGenerator.genCodeLine ("public static final String[] jjstrLiteralImages = {");

    int k = 0;
    for (final int i : allMatches.keySet ())
    {
      final TokenizerData.MatchInfo matchInfo = allMatches.get (i);
      switch (matchInfo.matchType)
      {
        case SKIP:
          toSkip.set (i);
          break;
        case SPECIAL_TOKEN:
          toSpecial.set (i);
          break;
        case MORE:
          toMore.set (i);
          break;
        case TOKEN:
          toToken.set (i);
          break;
      }
      newStates[i] = matchInfo.newLexState;
      final String image = matchInfo.image;
      if (k++ > 0)
        codeGenerator.genCodeLine (", ");
      if (image != null)
      {
        codeGenerator.genCode ("\"");
        for (int j = 0; j < image.length (); j++)
        {
          if (image.charAt (j) <= 0xff)
          {
            codeGenerator.genCode ("\\" + Integer.toOctalString (image.charAt (j)));
          }
          else
          {
            String hexVal = Integer.toHexString (image.charAt (j));
            if (hexVal.length () == 3)
              hexVal = "0" + hexVal;
            codeGenerator.genCode ("\\u" + hexVal);
          }
        }
        codeGenerator.genCode ("\"");
      }
      else
      {
        codeGenerator.genCodeLine ("null");
      }
    }
    codeGenerator.genCodeLine ("};");

    // Now generate the bit masks.
    generateBitVector ("jjtoSkip", toSkip, codeGenerator);
    generateBitVector ("jjtoSpecial", toSpecial, codeGenerator);
    generateBitVector ("jjtoMore", toMore, codeGenerator);
    generateBitVector ("jjtoToken", toToken, codeGenerator);

    codeGenerator.genCodeLine ("private static final int[] jjnewLexState = {");
    for (int i = 0; i < newStates.length; i++)
    {
      if (i > 0)
        codeGenerator.genCode (", ");
      codeGenerator.genCode ("0x" + Integer.toHexString (newStates[i]));
    }
    codeGenerator.genCodeLine ("};");

    // Action functions.

    final String staticString = Options.getStatic () ? "static " : "";
    // Token actions.
    codeGenerator.genCodeLine (staticString + "void TokenLexicalActions(Token matchedToken) {");
    _dumpLexicalActions (allMatches, TokenizerData.MatchType.TOKEN, "matchedToken.kind", codeGenerator);
    codeGenerator.genCodeLine ("}");

    // Skip actions.
    // TODO(sreeni) : Streamline this mess.

    codeGenerator.genCodeLine (staticString + "void SkipLexicalActions(Token matchedToken) {");
    _dumpLexicalActions (allMatches, TokenizerData.MatchType.SKIP, "jjmatchedKind", codeGenerator);
    _dumpLexicalActions (allMatches, TokenizerData.MatchType.SPECIAL_TOKEN, "jjmatchedKind", codeGenerator);
    codeGenerator.genCodeLine ("}");

    // More actions.
    codeGenerator.genCodeLine (staticString + "void MoreLexicalActions() {");
    codeGenerator.genCodeLine ("jjimageLen += (lengthOfMatch = jjmatchedPos + 1);");
    _dumpLexicalActions (allMatches, TokenizerData.MatchType.MORE, "jjmatchedKind", codeGenerator);
    codeGenerator.genCodeLine ("}");

    codeGenerator.genCodeLine ("public String[] lexStateNames = {");
    for (int i = 0; i < tokenizerData.lexStateNames.length; i++)
    {
      if (i > 0)
        codeGenerator.genCode (", ");
      codeGenerator.genCode ("\"" + tokenizerData.lexStateNames[i] + "\"");
    }
    codeGenerator.genCodeLine ("};");
  }

  private void _dumpLexicalActions (final Map <Integer, TokenizerData.MatchInfo> allMatches,
                                   final TokenizerData.MatchType matchType,
                                   final String kindString,
                                   final CodeGenerator codeGenerator)
  {
    codeGenerator.genCodeLine ("  switch(" + kindString + ") {");
    for (final int i : allMatches.keySet ())
    {
      final TokenizerData.MatchInfo matchInfo = allMatches.get (i);
      if (matchInfo.action == null || matchInfo.matchType != matchType)
      {
        continue;
      }
      codeGenerator.genCodeLine ("    case " + i + ": {\n");
      codeGenerator.genCodeLine ("      " + matchInfo.action);
      codeGenerator.genCodeLine ("      break;");
      codeGenerator.genCodeLine ("    }");
    }
    codeGenerator.genCodeLine ("    default: break;");
    codeGenerator.genCodeLine ("  }");
  }

  private static void generateBitVector (final String name, final BitSet bits, final CodeGenerator codeGenerator)
  {
    codeGenerator.genCodeLine ("private static final long[] " + name + " = {");
    final long [] longs = bits.toLongArray ();
    for (int i = 0; i < longs.length; i++)
    {
      if (i > 0)
        codeGenerator.genCode (", ");
      codeGenerator.genCode ("0x" + Long.toHexString (longs[i]) + "L");
    }
    codeGenerator.genCodeLine ("};");
  }
}
