/**
 * Copyright 2017-2020 Philip Helger, pgcc@helger.com
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
package com.helger.pgcc.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.helger.pgcc.parser.exp.ExpRStringLiteral;

// A simple class to hold the data generated by the tokenizer. This is passed to
// the code generators to produce code.
public class TokenizerData
{
  // Name of the parser as specified in the PARSER_BEGIN/PARSER_END block.
  public String m_parserName;

  // Decls coming from TOKEN_MGR_DECLS
  public String m_decls;

  // A map of <LexState, first char> to a sequence of literals indexed by:
  // ((int0LexicalState << 16 | (int)c)
  // The literals in the list are all guaranteed to start with the char and re
  // sorted by length so that the "longest-match" rule is done trivially by
  // just going through the sequence in the order.
  // Since they are all literals, there is no duplication (JavaCC checks that)
  // and hence if a longer match is matched, no need to check the shorter match.
  public Map <Integer, List <String>> m_literalSequence;

  // A map of list of kind values indexed by ((int0LexicalState << 16 | (int)c)
  // same key as before.
  public Map <Integer, List <Integer>> m_literalKinds;

  // The NFA start state for a given string literal match. We use this to start
  // the NFA if needed after a literal match is completed.
  public Map <Integer, Integer> m_kindToNfaStartState;

  // Class representing NFA state.
  public static class NfaState
  {
    // Index of the state.
    public final int m_index;
    // Set of allowed characters.
    public final Set <Character> m_characters;
    // Next state indices.
    public final Set <Integer> m_nextStates;
    // Initial state needs to transition to multiple states so the NFA will try
    // all possibilities.
    // TODO(sreeni) : Try and get rid of it at some point.
    public final Set <Integer> m_compositeStates;
    // match kind if any. Integer.MAX_VALUE if this is not a final state.
    public final int m_kind;

    NfaState (final int index,
              final Set <Character> characters,
              final Set <Integer> nextStates,
              final Set <Integer> compositeStates,
              final int kind)
    {
      this.m_index = index;
      this.m_characters = characters;
      this.m_nextStates = nextStates;
      this.m_kind = kind;
      this.m_compositeStates = compositeStates;
    }
  }

  // The main nfa.
  public final Map <Integer, NfaState> m_nfa = new HashMap <> ();

  public static enum EMatchType
  {
    SKIP,
    SPECIAL_TOKEN,
    MORE,
    TOKEN,
  }

  // Match info.
  public static class MatchInfo
  {
    // String literal image in case this string literal token, null otherwise.
    public final String m_image;
    // Kind index.
    public final int m_kind;
    // Type of match.
    public final EMatchType m_matchType;
    // Any lexical state transition specified.
    public final int m_newLexState;
    // Any lexical state transition specified.
    public final String m_action;

    public MatchInfo (final String image,
                      final int kind,
                      final EMatchType matchType,
                      final int newLexState,
                      final String action)
    {
      this.m_image = image;
      this.m_kind = kind;
      this.m_matchType = matchType;
      this.m_newLexState = newLexState;
      this.m_action = action;
    }
  }

  // On match info indexed by the match kind.
  public final Map <Integer, MatchInfo> m_allMatches = new HashMap <> ();

  // Initial nfa states indexed by lexical state.
  public Map <Integer, Integer> m_initialStates;

  // Kind of the wildcard match (~[]) indexed by lexical state.
  public Map <Integer, Integer> m_wildcardKind;

  // Name of lexical state - for debugging.
  public String [] m_lexStateNames;

  // DEFAULT lexical state index.
  public int m_defaultLexState;

  public void setParserName (final String parserName)
  {
    this.m_parserName = parserName;
  }

  public void setDecls (final String decls)
  {
    this.m_decls = decls;
  }

  public void setLiteralSequence (final Map <Integer, List <String>> literalSequence)
  {
    this.m_literalSequence = literalSequence;
  }

  public void setLiteralKinds (final Map <Integer, List <Integer>> literalKinds)
  {
    this.m_literalKinds = literalKinds;
  }

  public void setKindToNfaStartState (final Map <Integer, Integer> kindToNfaStartState)
  {
    this.m_kindToNfaStartState = kindToNfaStartState;
  }

  public void addNfaState (final int index,
                           final Set <Character> characters,
                           final Set <Integer> nextStates,
                           final Set <Integer> compositeStates,
                           final int kind)
  {
    final NfaState nfaState = new NfaState (index, characters, nextStates, compositeStates, kind);
    m_nfa.put (Integer.valueOf (index), nfaState);
  }

  public void setInitialStates (final Map <Integer, Integer> initialStates)
  {
    this.m_initialStates = initialStates;
  }

  public void setWildcardKind (final Map <Integer, Integer> wildcardKind)
  {
    this.m_wildcardKind = wildcardKind;
  }

  public void setLexStateNames (final String [] lexStateNames)
  {
    this.m_lexStateNames = lexStateNames;
  }

  public void setDefaultLexState (final int defaultLexState)
  {
    this.m_defaultLexState = defaultLexState;
  }

  public void updateMatchInfo (final Map <Integer, String> actions,
                               final int [] newLexStateIndices,
                               final long [] toSkip,
                               final long [] toSpecial,
                               final long [] toMore,
                               final long [] toToken)
  {
    for (int i = 0; i < newLexStateIndices.length; i++)
    {
      final int vectorIndex = i >> 6;
      final long bits = (1L << (i & 077));
      EMatchType matchType = EMatchType.TOKEN;
      if (toSkip.length > vectorIndex && (toSkip[vectorIndex] & bits) != 0L)
      {
        matchType = EMatchType.SKIP;
      }
      else
        if (toSpecial.length > vectorIndex && (toSpecial[vectorIndex] & bits) != 0L)
        {
          matchType = EMatchType.SPECIAL_TOKEN;
        }
        else
          if (toMore.length > vectorIndex && (toMore[vectorIndex] & bits) != 0L)
          {
            matchType = EMatchType.MORE;
          }
          else
          {
            assert (toToken.length > vectorIndex && (toToken[vectorIndex] & bits) != 0L);
            matchType = EMatchType.TOKEN;
          }
      final MatchInfo matchInfo = new MatchInfo (Options.isIgnoreCase () ? null : ExpRStringLiteral.s_allImages[i],
                                                 i,
                                                 matchType,
                                                 newLexStateIndices[i],
                                                 actions.get (Integer.valueOf (i)));
      m_allMatches.put (Integer.valueOf (i), matchInfo);
    }
  }
}
