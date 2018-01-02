/**
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
 *
 * Copyright 2011 Google Inc. All Rights Reserved.
 * Author: sreeni@google.com (Sreeni Viswanadha)
 *
 * Copyright 2017-2018 Philip Helger, pgcc@helger.com
 */
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

import static com.helger.pgcc.parser.JavaCCGlobals.s_bnfproductions;
import static com.helger.pgcc.parser.JavaCCGlobals.s_ccol;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cline;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_name;
import static com.helger.pgcc.parser.JavaCCGlobals.s_jj2index;
import static com.helger.pgcc.parser.JavaCCGlobals.s_lookaheadNeeded;
import static com.helger.pgcc.parser.JavaCCGlobals.s_maskVals;
import static com.helger.pgcc.parser.JavaCCGlobals.s_maskindex;
import static com.helger.pgcc.parser.JavaCCGlobals.s_names_of_tokens;
import static com.helger.pgcc.parser.JavaCCGlobals.s_production_table;
import static com.helger.pgcc.parser.JavaCCGlobals.s_tokenCount;
import static com.helger.pgcc.parser.JavaCCGlobals.staticOpt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ParseEngine
{
  private int m_gensymindex = 0;
  private int m_indentamt;
  private boolean m_bJJ2LA;
  private CodeGenerator m_codeGenerator;
  @Deprecated
  private final boolean m_isJavaDialect = Options.isOutputLanguageJava ();

  /**
   * These lists are used to maintain expansions for which code generation in
   * phase 2 and phase 3 is required. Whenever a call is generated to a phase 2
   * or phase 3 routine, a corresponding entry is added here if it has not
   * already been added. The phase 3 routines have been optimized in version
   * 0.7pre2. Essentially only those methods (and only those portions of these
   * methods) are generated that are required. The lookahead amount is used to
   * determine this. This change requires the use of a hash table because it is
   * now possible for the same phase 3 routine to be requested multiple times
   * with different lookaheads. The hash table provides a easily searchable
   * capability to determine the previous requests. The phase 3 routines now are
   * performed in a two step process - the first step gathers the requests
   * (replacing requests with lower lookaheads with those requiring larger
   * lookaheads). The second step then generates these methods. This
   * optimization and the hashtable makes it look like we do not need the flag
   * "phase3done" any more. But this has not been removed yet.
   */
  private final List <Lookahead> m_phase2list = new ArrayList <> ();
  private final List <Phase3Data> m_phase3list = new ArrayList <> ();
  private final Map <Expansion, Phase3Data> m_phase3table = new HashMap <> ();

  /**
   * The phase 1 routines generates their output into String's and dumps these
   * String's once for each method. These String's contain the special
   * characters '\u0001' to indicate a positive indent, and '\u0002' to indicate
   * a negative indent. '\n' is used to indicate a line terminator. The
   * characters '\u0003' and '\u0004' are used to delineate portions of text
   * where '\n's should not be followed by an indentation.
   */

  /**
   * Returns true if there is a JAVACODE production that the argument expansion
   * may directly expand to (without consuming tokens or encountering
   * lookahead).
   */
  private boolean javaCodeCheck (final Expansion exp)
  {
    if (exp instanceof RegularExpression)
    {
      return false;
    }
    else
      if (exp instanceof NonTerminal)
      {
        final NormalProduction prod = ((NonTerminal) exp).getProd ();
        if (prod instanceof AbstractCodeProduction)
        {
          return true;
        }
        return javaCodeCheck (prod.getExpansion ());
      }
      else
        if (exp instanceof Choice)
        {
          final Choice ch = (Choice) exp;
          for (int i = 0; i < ch.getChoices ().size (); i++)
          {
            if (javaCodeCheck ((ch.getChoices ().get (i))))
            {
              return true;
            }
          }
          return false;
        }
        else
          if (exp instanceof Sequence)
          {
            final Sequence seq = (Sequence) exp;
            for (int i = 0; i < seq.m_units.size (); i++)
            {
              final Expansion [] units = seq.m_units.toArray (new Expansion [seq.m_units.size ()]);
              if (units[i] instanceof Lookahead && ((Lookahead) units[i]).isExplicit ())
              {
                // An explicit lookahead (rather than one generated implicitly).
                // Assume
                // the user knows what he / she is doing, e.g.
                // "A" ( "B" | LOOKAHEAD("X") jcode() | "C" )* "D"
                return false;
              }
              else
                if (javaCodeCheck ((units[i])))
                {
                  return true;
                }
                else
                  if (!Semanticize.emptyExpansionExists (units[i]))
                  {
                    return false;
                  }
            }
            return false;
          }
          else
            if (exp instanceof OneOrMore)
            {
              final OneOrMore om = (OneOrMore) exp;
              return javaCodeCheck (om.expansion);
            }
            else
              if (exp instanceof ZeroOrMore)
              {
                final ZeroOrMore zm = (ZeroOrMore) exp;
                return javaCodeCheck (zm.expansion);
              }
              else
                if (exp instanceof ZeroOrOne)
                {
                  final ZeroOrOne zo = (ZeroOrOne) exp;
                  return javaCodeCheck (zo.expansion);
                }
                else
                  if (exp instanceof TryBlock)
                  {
                    final TryBlock tb = (TryBlock) exp;
                    return javaCodeCheck (tb.exp);
                  }
                  else
                  {
                    return false;
                  }
  }

  /**
   * An array used to store the first sets generated by the following method. A
   * true entry means that the corresponding token is in the first set.
   */
  private boolean [] firstSet;

  /**
   * Sets up the array "firstSet" above based on the Expansion argument passed
   * to it. Since this is a recursive function, it assumes that "firstSet" has
   * been reset before the first call.
   */
  private void genFirstSet (final Expansion exp)
  {
    if (exp instanceof RegularExpression)
    {
      firstSet[((RegularExpression) exp).m_ordinal] = true;
    }
    else
      if (exp instanceof NonTerminal)
      {
        if (!(((NonTerminal) exp).getProd () instanceof AbstractCodeProduction))
        {
          genFirstSet (((BNFProduction) (((NonTerminal) exp).getProd ())).getExpansion ());
        }
      }
      else
        if (exp instanceof Choice)
        {
          final Choice ch = (Choice) exp;
          for (int i = 0; i < ch.getChoices ().size (); i++)
          {
            genFirstSet ((ch.getChoices ().get (i)));
          }
        }
        else
          if (exp instanceof Sequence)
          {
            final Sequence seq = (Sequence) exp;
            final Object obj = seq.m_units.get (0);
            if ((obj instanceof Lookahead) && (((Lookahead) obj).getActionTokens ().size () != 0))
            {
              m_bJJ2LA = true;
            }
            for (int i = 0; i < seq.m_units.size (); i++)
            {
              final Expansion unit = seq.m_units.get (i);
              // Javacode productions can not have FIRST sets. Instead we
              // generate the FIRST set
              // for the preceding LOOKAHEAD (the semantic checks should have
              // made sure that
              // the LOOKAHEAD is suitable).
              if (unit instanceof NonTerminal && ((NonTerminal) unit).getProd () instanceof AbstractCodeProduction)
              {
                if (i > 0 && seq.m_units.get (i - 1) instanceof Lookahead)
                {
                  final Lookahead la = (Lookahead) seq.m_units.get (i - 1);
                  genFirstSet (la.getLaExpansion ());
                }
              }
              else
              {
                genFirstSet ((seq.m_units.get (i)));
              }
              if (!Semanticize.emptyExpansionExists ((seq.m_units.get (i))))
              {
                break;
              }
            }
          }
          else
            if (exp instanceof OneOrMore)
            {
              final OneOrMore om = (OneOrMore) exp;
              genFirstSet (om.expansion);
            }
            else
              if (exp instanceof ZeroOrMore)
              {
                final ZeroOrMore zm = (ZeroOrMore) exp;
                genFirstSet (zm.expansion);
              }
              else
                if (exp instanceof ZeroOrOne)
                {
                  final ZeroOrOne zo = (ZeroOrOne) exp;
                  genFirstSet (zo.expansion);
                }
                else
                  if (exp instanceof TryBlock)
                  {
                    final TryBlock tb = (TryBlock) exp;
                    genFirstSet (tb.exp);
                  }
  }

  /**
   * Constants used in the following method "buildLookaheadChecker".
   */
  static enum EState
  {
    NOOPENSTM,
    OPENIF,
    OPENSWITCH
  }

  @SuppressWarnings ("unused")
  private void _dumpLookaheads (final Lookahead [] conds, final String [] actions)
  {
    for (int i = 0; i < conds.length; i++)
    {
      System.err.println ("Lookahead: " + i);
      System.err.println (conds[i].dump (0, new HashSet <> ()));
      System.err.println ();
    }
  }

  /**
   * This method takes two parameters - an array of Lookahead's "conds", and an
   * array of String's "actions". "actions" contains exactly one element more
   * than "conds". "actions" are Java source code, and "conds" translate to
   * conditions - so lets say "f(conds[i])" is true if the lookahead required by
   * "conds[i]" is indeed the case. This method returns a string corresponding
   * to the Java code for: if (f(conds[0]) actions[0] else if (f(conds[1])
   * actions[1] . . . else actions[action.length-1] A particular action entry
   * ("actions[i]") can be null, in which case, a noop is generated for that
   * action.
   */
  String buildLookaheadChecker (final Lookahead [] conds, final String [] actions)
  {

    // The state variables.
    EState state = EState.NOOPENSTM;
    int indentAmt = 0;
    final boolean [] casedValues = new boolean [s_tokenCount];
    String retval = "";
    Lookahead la;
    Token t = null;
    final int tokenMaskSize = (s_tokenCount - 1) / 32 + 1;
    int [] tokenMask = null;

    // Iterate over all the conditions.
    int index = 0;
    while (index < conds.length)
    {

      la = conds[index];
      m_bJJ2LA = false;

      if ((la.getAmount () == 0) ||
          Semanticize.emptyExpansionExists (la.getLaExpansion ()) ||
          javaCodeCheck (la.getLaExpansion ()))
      {

        // This handles the following cases:
        // . If syntactic lookahead is not wanted (and hence explicitly
        // specified
        // as 0).
        // . If it is possible for the lookahead expansion to recognize the
        // empty
        // string - in which case the lookahead trivially passes.
        // . If the lookahead expansion has a JAVACODE production that it
        // directly
        // expands to - in which case the lookahead trivially passes.
        if (la.getActionTokens ().size () == 0)
        {
          // In addition, if there is no semantic lookahead, then the
          // lookahead trivially succeeds. So break the main loop and
          // treat this case as the default last action.
          break;
        }
        // This case is when there is only semantic lookahead
        // (without any preceding syntactic lookahead). In this
        // case, an "if" statement is generated.
        switch (state)
        {
          case NOOPENSTM:
            retval += "\n" + "if (";
            indentAmt++;
            break;
          case OPENIF:
            retval += "\u0002\n" + "} else if (";
            break;
          case OPENSWITCH:
            retval += "\u0002\n" + "default:" + "\u0001";
            if (Options.getErrorReporting ())
            {
              retval += "\njj_la1[" + s_maskindex + "] = jj_gen;";
              s_maskindex++;
            }
            s_maskVals.add (tokenMask);
            retval += "\n" + "if (";
            indentAmt++;
        }
        m_codeGenerator.printTokenSetup ((la.getActionTokens ().get (0)));
        for (final Object aElement : la.getActionTokens ())
        {
          t = (Token) aElement;
          retval += m_codeGenerator.getStringToPrint (t);
        }
        retval += m_codeGenerator.getTrailingComments (t);
        retval += ") {\u0001" + actions[index];
        state = EState.OPENIF;
      }
      else
        if (la.getAmount () == 1 && la.getActionTokens ().size () == 0)
        {
          // Special optimal processing when the lookahead is exactly 1, and
          // there
          // is no semantic lookahead.

          if (firstSet == null)
          {
            firstSet = new boolean [s_tokenCount];
          }
          for (int i = 0; i < s_tokenCount; i++)
          {
            firstSet[i] = false;
          }
          // jj2LA is set to false at the beginning of the containing "if"
          // statement.
          // It is checked immediately after the end of the same statement to
          // determine
          // if lookaheads are to be performed using calls to the jj2 methods.
          genFirstSet (la.getLaExpansion ());
          // genFirstSet may find that semantic attributes are appropriate for
          // the next
          // token. In which case, it sets jj2LA to true.
          if (!m_bJJ2LA)
          {

            // This case is if there is no applicable semantic lookahead and the
            // lookahead
            // is one (excluding the earlier cases such as JAVACODE, etc.).
            switch (state)
            {
              case OPENIF:
                retval += "\u0002\n" + "} else {\u0001";
                // Control flows through to next case.
              case NOOPENSTM:
                retval += "\n" + "switch (";
                if (Options.getCacheTokens ())
                {
                  retval += "jj_nt.kind) {\u0001";
                }
                else
                {
                  retval += "(jj_ntk==-1)?jj_ntk_f():jj_ntk) {\u0001";
                }
                for (int i = 0; i < s_tokenCount; i++)
                {
                  casedValues[i] = false;
                }
                indentAmt++;
                tokenMask = new int [tokenMaskSize];
                for (int i = 0; i < tokenMaskSize; i++)
                {
                  tokenMask[i] = 0;
                }
                // Don't need to do anything if state is OPENSWITCH.
            }
            for (int i = 0; i < s_tokenCount; i++)
            {
              if (firstSet[i])
              {
                if (!casedValues[i])
                {
                  casedValues[i] = true;
                  retval += "\u0002\ncase ";
                  final int j1 = i / 32;
                  final int j2 = i % 32;
                  tokenMask[j1] |= 1 << j2;
                  final String s = (s_names_of_tokens.get (Integer.valueOf (i)));
                  if (s == null)
                  {
                    retval += i;
                  }
                  else
                  {
                    retval += s;
                  }
                  retval += ":\u0001";
                }
              }
            }
            retval += "{";
            retval += actions[index];
            retval += "\nbreak;\n}";
            state = EState.OPENSWITCH;
          }
        }
        else
        {
          // This is the case when lookahead is determined through calls to
          // jj2 methods. The other case is when lookahead is 1, but semantic
          // attributes need to be evaluated. Hence this crazy control
          // structure.
          m_bJJ2LA = true;
        }

      if (m_bJJ2LA)
      {
        // In this case lookahead is determined by the jj2 methods.

        switch (state)
        {
          case NOOPENSTM:
            retval += "\n" + "if (";
            indentAmt++;
            break;
          case OPENIF:
            retval += "\u0002\n" + "} else if (";
            break;
          case OPENSWITCH:
            retval += "\u0002\n" + "default:" + "\u0001";
            if (Options.getErrorReporting ())
            {
              retval += "\njj_la1[" + s_maskindex + "] = jj_gen;";
              s_maskindex++;
            }
            s_maskVals.add (tokenMask);
            retval += "\n" + "if (";
            indentAmt++;
        }
        s_jj2index++;
        // At this point, la.la_expansion.internal_name must be "".
        la.getLaExpansion ().m_internal_name = "_" + s_jj2index;
        la.getLaExpansion ().m_internal_index = s_jj2index;
        m_phase2list.add (la);
        retval += "jj_2" + la.getLaExpansion ().m_internal_name + "(" + la.getAmount () + ")";
        if (la.getActionTokens ().size () != 0)
        {
          // In addition, there is also a semantic lookahead. So concatenate
          // the semantic check with the syntactic one.
          retval += " && (";
          m_codeGenerator.printTokenSetup ((la.getActionTokens ().get (0)));
          for (final Object aElement : la.getActionTokens ())
          {
            t = (Token) aElement;
            retval += m_codeGenerator.getStringToPrint (t);
          }
          retval += m_codeGenerator.getTrailingComments (t);
          retval += ")";
        }
        retval += ") {\u0001" + actions[index];
        state = EState.OPENIF;
      }

      index++;
    }

    // Generate code for the default case. Note this may not
    // be the last entry of "actions" if any condition can be
    // statically determined to be always "true".

    switch (state)
    {
      case NOOPENSTM:
        retval += actions[index];
        break;
      case OPENIF:
        retval += "\u0002\n" + "} else {\u0001" + actions[index];
        break;
      case OPENSWITCH:
        retval += "\u0002\n" + "default:" + "\u0001";
        if (Options.getErrorReporting ())
        {
          retval += "\njj_la1[" + s_maskindex + "] = jj_gen;";
          s_maskVals.add (tokenMask);
          s_maskindex++;
        }
        retval += actions[index];
    }
    for (int i = 0; i < indentAmt; i++)
    {
      retval += "\u0002\n}";
    }

    return retval;

  }

  void dumpFormattedString (final String str)
  {
    char ch = ' ';
    char prevChar;
    boolean indentOn = true;
    for (int i = 0; i < str.length (); i++)
    {
      prevChar = ch;
      ch = str.charAt (i);
      if (ch == '\n' && prevChar == '\r')
      {
        // do nothing - we've already printed a new line for the '\r'
        // during the previous iteration.
      }
      else
        if (ch == '\n' || ch == '\r')
        {
          if (indentOn)
          {
            phase1NewLine ();
          }
          else
          {
            m_codeGenerator.genCodeLine ("");
          }
        }
        else
          if (ch == '\u0001')
          {
            m_indentamt += 2;
          }
          else
            if (ch == '\u0002')
            {
              m_indentamt -= 2;
            }
            else
              if (ch == '\u0003')
              {
                indentOn = false;
              }
              else
                if (ch == '\u0004')
                {
                  indentOn = true;
                }
                else
                {
                  m_codeGenerator.genCode (Character.toString (ch));
                }
    }
  }

  // Print CPPCODE method header.
  private String generateCPPMethodheader (final CodeProductionCpp p)
  {
    final StringBuilder sig = new StringBuilder ();
    String ret, params;
    Token t = null;

    if (false)
    {
      m_codeGenerator.printTokenSetup (t);
      s_ccol = 1;
      final String comment1 = m_codeGenerator.getLeadingComments (t);
      s_cline = t.beginLine;
      s_ccol = t.beginColumn;
      sig.append (t.image);
    }

    for (int i = 0; i < p.getReturnTypeTokens ().size (); i++)
    {
      t = (p.getReturnTypeTokens ().get (i));
      final String s = m_codeGenerator.getStringToPrint (t);
      sig.append (t.toString ());
      sig.append (" ");
    }

    String comment2 = "";
    if (t != null)
      comment2 = m_codeGenerator.getTrailingComments (t);
    ret = sig.toString ();

    sig.setLength (0);
    sig.append ("(");
    if (p.getParameterListTokens ().size () != 0)
    {
      m_codeGenerator.printTokenSetup ((p.getParameterListTokens ().get (0)));
      for (final Object aElement : p.getParameterListTokens ())
      {
        t = (Token) aElement;
        sig.append (m_codeGenerator.getStringToPrint (t));
      }
      sig.append (m_codeGenerator.getTrailingComments (t));
    }
    sig.append (")");
    params = sig.toString ();

    // For now, just ignore comments
    m_codeGenerator.generateMethodDefHeader (ret, s_cu_name, p.getLhs () + params, sig.toString ());

    return "";
  }

  // Print method header and return the ERROR_RETURN string.
  private String generateCPPMethodheader (final BNFProduction p, final Token t2)
  {
    final StringBuilder sig = new StringBuilder ();
    Token t = t2;

    final String method_name = p.getLhs ();
    boolean void_ret = false;
    boolean ptr_ret = false;

    m_codeGenerator.printTokenSetup (t);
    s_ccol = 1;
    final String comment1 = m_codeGenerator.getLeadingComments (t);
    s_cline = t.beginLine;
    s_ccol = t.beginColumn;
    sig.append (t.image);
    if (t.kind == JavaCCParserConstants.VOID)
      void_ret = true;
    if (t.kind == JavaCCParserConstants.STAR)
      ptr_ret = true;

    for (int i = 1; i < p.getReturnTypeTokens ().size (); i++)
    {
      t = p.getReturnTypeTokens ().get (i);
      sig.append (m_codeGenerator.getStringToPrint (t));
      if (t.kind == JavaCCParserConstants.VOID)
        void_ret = true;
      if (t.kind == JavaCCParserConstants.STAR)
        ptr_ret = true;
    }

    final String comment2 = m_codeGenerator.getTrailingComments (t);
    final String ret = sig.toString ();

    sig.setLength (0);
    sig.append ("(");
    if (p.getParameterListTokens ().size () != 0)
    {
      m_codeGenerator.printTokenSetup (p.getParameterListTokens ().get (0));
      for (final Object aElement : p.getParameterListTokens ())
      {
        t = (Token) aElement;
        sig.append (m_codeGenerator.getStringToPrint (t));
      }
      sig.append (m_codeGenerator.getTrailingComments (t));
    }
    sig.append (")");
    final String params = sig.toString ();

    // For now, just ignore comments
    m_codeGenerator.generateMethodDefHeader (ret, s_cu_name, p.getLhs () + params, sig.toString ());

    // Generate a default value for error return.
    String default_return;
    if (ptr_ret)
      default_return = "NULL";
    else
      if (void_ret)
        default_return = "";
      else
        default_return = "0"; // 0 converts to most (all?) basic types.

    final StringBuilder ret_val = new StringBuilder ("\n#if !defined ERROR_RET_" + method_name + "\n");
    ret_val.append ("#define ERROR_RET_" + method_name + " " + default_return + "\n");
    ret_val.append ("#endif\n");
    ret_val.append ("#define __ERROR_RET__ ERROR_RET_" + method_name + "\n");

    return ret_val.toString ();
  }

  void genStackCheck (final boolean voidReturn)
  {
    if (Options.getDepthLimit () > 0)
    {
      if (m_isJavaDialect)
      {
        m_codeGenerator.genCodeLine ("if(++jj_depth > " + Options.getDepthLimit () + ") {");
        m_codeGenerator.genCodeLine ("  jj_consume_token(-1);");
        m_codeGenerator.genCodeLine ("  throw new ParseException();");
        m_codeGenerator.genCodeLine ("}");
        m_codeGenerator.genCodeLine ("try {");
      }
      else
      {
        if (!voidReturn)
        {
          m_codeGenerator.genCodeLine ("if(jj_depth_error){ return __ERROR_RET__; }");
        }
        else
        {
          m_codeGenerator.genCodeLine ("if(jj_depth_error){ return; }");
        }
        m_codeGenerator.genCodeLine ("__jj_depth_inc __jj_depth_counter(this);");
        m_codeGenerator.genCodeLine ("if(jj_depth > " + Options.getDepthLimit () + ") {");
        m_codeGenerator.genCodeLine ("  jj_depth_error = true;");
        m_codeGenerator.genCodeLine ("  jj_consume_token(-1);");
        m_codeGenerator.genCodeLine ("  errorHandler->handleParseError(token, getToken(1), __FUNCTION__, this), hasError = true;");
        if (!voidReturn)
        {
          m_codeGenerator.genCodeLine ("  return __ERROR_RET__;"); // Non-recoverable
          // error
        }
        else
        {
          m_codeGenerator.genCodeLine ("  return;"); // Non-recoverable error
        }
        m_codeGenerator.genCodeLine ("}");
      }
    }
  }

  void genStackCheckEnd ()
  {
    if (Options.getDepthLimit () > 0)
    {
      if (m_isJavaDialect)
      {
        m_codeGenerator.genCodeLine (" } finally {");
        m_codeGenerator.genCodeLine ("   --jj_depth;");
        m_codeGenerator.genCodeLine (" }");
      }
    }
  }

  void buildPhase1Routine (final BNFProduction p)
  {
    Token t = p.getReturnTypeTokens ().get (0);
    boolean voidReturn = false;
    if (t.kind == JavaCCParserConstants.VOID)
    {
      voidReturn = true;
    }
    String error_ret = null;
    if (m_isJavaDialect)
    {
      m_codeGenerator.printTokenSetup (t);
      s_ccol = 1;
      m_codeGenerator.printLeadingComments (t);
      m_codeGenerator.genCode ("  " +
                               staticOpt () +
                               "final " +
                               (p.getAccessMod () != null ? p.getAccessMod () : "public") +
                               " ");
      s_cline = t.beginLine;
      s_ccol = t.beginColumn;
      m_codeGenerator.printTokenOnly (t);
      for (int i = 1; i < p.getReturnTypeTokens ().size (); i++)
      {
        t = (p.getReturnTypeTokens ().get (i));
        m_codeGenerator.printToken (t);
      }
      m_codeGenerator.printTrailingComments (t);
      m_codeGenerator.genCode (" " + p.getLhs () + "(");
      if (p.getParameterListTokens ().size () != 0)
      {
        m_codeGenerator.printTokenSetup ((p.getParameterListTokens ().get (0)));
        for (final Object aElement : p.getParameterListTokens ())
        {
          t = (Token) aElement;
          m_codeGenerator.printToken (t);
        }
        m_codeGenerator.printTrailingComments (t);
      }
      m_codeGenerator.genCode (")");
      m_codeGenerator.genCode (" throws ParseException");

      for (final List <Token> name : p.getThrowsList ())
      {
        m_codeGenerator.genCode (", ");
        for (final Token t2 : name)
          m_codeGenerator.genCode (t2.image);
      }
    }
    else
    {
      error_ret = generateCPPMethodheader (p, t);
    }

    m_codeGenerator.genCode (" {");

    if ((Options.booleanValue (Options.USEROPTION__CPP_STOP_ON_FIRST_ERROR) && error_ret != null) ||
        (Options.getDepthLimit () > 0 && !voidReturn && !m_isJavaDialect))
    {
      m_codeGenerator.genCode (error_ret);
    }
    else
    {
      error_ret = null;
    }

    genStackCheck (voidReturn);

    m_indentamt = 4;
    if (Options.getDebugParser ())
    {
      m_codeGenerator.genCodeLine ("");
      if (m_isJavaDialect)
      {
        m_codeGenerator.genCodeLine ("    trace_call(\"" + JavaCCGlobals.addUnicodeEscapes (p.getLhs ()) + "\");");
      }
      else
      {
        m_codeGenerator.genCodeLine ("    JJEnter<std::function<void()>> jjenter([this]() {trace_call  (\"" +
                                     JavaCCGlobals.addUnicodeEscapes (p.getLhs ()) +
                                     "\"); });");
        m_codeGenerator.genCodeLine ("    JJExit <std::function<void()>> jjexit ([this]() {trace_return(\"" +
                                     JavaCCGlobals.addUnicodeEscapes (p.getLhs ()) +
                                     "\"); });");
      }
      m_codeGenerator.genCodeLine ("    try {");
      m_indentamt = 6;
    }

    if (!Options.booleanValue (Options.USEROPTION__CPP_IGNORE_ACTIONS) && p.getDeclarationTokens ().size () != 0)
    {
      m_codeGenerator.printTokenSetup ((p.getDeclarationTokens ().get (0)));
      s_cline--;
      for (final Object aElement : p.getDeclarationTokens ())
      {
        t = (Token) aElement;
        m_codeGenerator.printToken (t);
      }
      m_codeGenerator.printTrailingComments (t);
    }

    final String code = phase1ExpansionGen (p.getExpansion ());
    dumpFormattedString (code);
    m_codeGenerator.genCodeLine ("");

    if (p.isJumpPatched () && !voidReturn)
    {
      if (m_isJavaDialect)
      {
        // This line is required for Java!
        m_codeGenerator.genCodeLine ("    throw new " +
                                     (Options.isLegacyExceptionHandling () ? "Error" : "RuntimeException") +
                                     "(\"Missing return statement in function\");");
      }
      else
      {
        m_codeGenerator.genCodeLine ("    throw \"Missing return statement in function\";");
      }
    }
    if (Options.getDebugParser ())
    {
      if (m_isJavaDialect)
      {
        m_codeGenerator.genCodeLine ("    } finally {");
        m_codeGenerator.genCodeLine ("      trace_return(\"" + JavaCCGlobals.addUnicodeEscapes (p.getLhs ()) + "\");");
      }
      else
      {
        m_codeGenerator.genCodeLine ("    } catch(...) { }");
      }
      if (m_isJavaDialect)
      {
        m_codeGenerator.genCodeLine ("    }");
      }
    }
    if (!m_isJavaDialect && !voidReturn)
    {
      m_codeGenerator.genCodeLine ("assert(false);");
    }

    if (error_ret != null)
    {
      m_codeGenerator.genCodeLine ("\n#undef __ERROR_RET__\n");
    }
    genStackCheckEnd ();
    m_codeGenerator.genCodeLine ("}");
    m_codeGenerator.genCodeLine ("");
  }

  void phase1NewLine ()
  {
    m_codeGenerator.genCodeLine ("");
    for (int i = 0; i < m_indentamt; i++)
    {
      m_codeGenerator.genCode (" ");
    }
  }

  String phase1ExpansionGen (final Expansion e)
  {
    String retval = "";
    Token t = null;
    Lookahead [] conds;
    String [] actions;
    if (e instanceof RegularExpression)
    {
      final RegularExpression e_nrw = (RegularExpression) e;
      retval += "\n";
      if (e_nrw.m_lhsTokens.size () != 0)
      {
        m_codeGenerator.printTokenSetup ((e_nrw.m_lhsTokens.get (0)));
        for (final Object aElement : e_nrw.m_lhsTokens)
        {
          t = (Token) aElement;
          retval += m_codeGenerator.getStringToPrint (t);
        }
        retval += m_codeGenerator.getTrailingComments (t);
        retval += " = ";
      }
      final String tail = e_nrw.m_rhsToken == null ? ");"
                                                   : (m_isJavaDialect ? ")." : ")->") + e_nrw.m_rhsToken.image + ";";
      if (e_nrw.m_label.length () == 0)
      {
        final Object label = s_names_of_tokens.get (Integer.valueOf (e_nrw.m_ordinal));
        if (label != null)
        {
          retval += "jj_consume_token(" + (String) label + tail;
        }
        else
        {
          retval += "jj_consume_token(" + e_nrw.m_ordinal + tail;
        }
      }
      else
      {
        retval += "jj_consume_token(" + e_nrw.m_label + tail;
      }

      if (!m_isJavaDialect && Options.booleanValue (Options.USEROPTION__CPP_STOP_ON_FIRST_ERROR))
      {
        retval += "\n    { if (hasError) { return __ERROR_RET__; } }\n";
      }

    }
    else
      if (e instanceof NonTerminal)
      {
        final NonTerminal e_nrw = (NonTerminal) e;
        retval += "\n";
        if (e_nrw.getLhsTokens ().size () != 0)
        {
          m_codeGenerator.printTokenSetup ((e_nrw.getLhsTokens ().get (0)));
          for (final Object aElement : e_nrw.getLhsTokens ())
          {
            t = (Token) aElement;
            retval += m_codeGenerator.getStringToPrint (t);
          }
          retval += m_codeGenerator.getTrailingComments (t);
          retval += " = ";
        }
        retval += e_nrw.getName () + "(";
        if (e_nrw.getArgumentTokens ().size () != 0)
        {
          m_codeGenerator.printTokenSetup ((e_nrw.getArgumentTokens ().get (0)));
          for (final Object aElement : e_nrw.getArgumentTokens ())
          {
            t = (Token) aElement;
            retval += m_codeGenerator.getStringToPrint (t);
          }
          retval += m_codeGenerator.getTrailingComments (t);
        }
        retval += ");";
        if (!m_isJavaDialect && Options.booleanValue (Options.USEROPTION__CPP_STOP_ON_FIRST_ERROR))
        {
          retval += "\n    { if (hasError) { return __ERROR_RET__; } }\n";
        }
      }
      else
        if (e instanceof Action)
        {
          final Action e_nrw = (Action) e;
          retval += "\u0003\n";
          if (!Options.booleanValue (Options.USEROPTION__CPP_IGNORE_ACTIONS) && e_nrw.getActionTokens ().size () != 0)
          {
            m_codeGenerator.printTokenSetup ((e_nrw.getActionTokens ().get (0)));
            s_ccol = 1;
            for (final Object aElement : e_nrw.getActionTokens ())
            {
              t = (Token) aElement;
              retval += m_codeGenerator.getStringToPrint (t);
            }
            retval += m_codeGenerator.getTrailingComments (t);
          }
          retval += "\u0004";
        }
        else
          if (e instanceof Choice)
          {
            final Choice e_nrw = (Choice) e;
            conds = new Lookahead [e_nrw.getChoices ().size ()];
            actions = new String [e_nrw.getChoices ().size () + 1];
            actions[e_nrw.getChoices ()
                         .size ()] = "\n" +
                                     "jj_consume_token(-1);\n" +
                                     (m_isJavaDialect ? "throw new ParseException();"
                                                      : ("errorHandler->handleParseError(token, getToken(1), __FUNCTION__, this), hasError = true;" +
                                                         (Options.booleanValue (Options.USEROPTION__CPP_STOP_ON_FIRST_ERROR) ? "return __ERROR_RET__;\n"
                                                                                                                             : "")));

            // In previous line, the "throw" never throws an exception since the
            // evaluation of jj_consume_token(-1) causes ParseException to be
            // thrown first.
            Sequence nestedSeq;
            for (int i = 0; i < e_nrw.getChoices ().size (); i++)
            {
              nestedSeq = (Sequence) (e_nrw.getChoices ().get (i));
              actions[i] = phase1ExpansionGen (nestedSeq);
              conds[i] = (Lookahead) (nestedSeq.m_units.get (0));
            }
            retval = buildLookaheadChecker (conds, actions);
          }
          else
            if (e instanceof Sequence)
            {
              final Sequence e_nrw = (Sequence) e;
              // We skip the first element in the following iteration since it
              // is the
              // Lookahead object.
              for (int i = 1; i < e_nrw.m_units.size (); i++)
              {
                // For C++, since we are not using exceptions, we will protect
                // all the
                // expansion choices with if (!error)
                boolean wrap_in_block = false;
                if (!JavaCCGlobals.s_jjtreeGenerated && !m_isJavaDialect)
                {
                  // for the last one, if it's an action, we will not protect
                  // it.
                  final Expansion elem = e_nrw.m_units.get (i);
                  if (!(elem instanceof Action) ||
                      !(e.m_parent instanceof BNFProduction) ||
                      i != e_nrw.m_units.size () - 1)
                  {
                    wrap_in_block = true;
                    retval += "\nif (" + (m_isJavaDialect ? "true" : "!hasError") + ") {";
                  }
                }
                retval += phase1ExpansionGen ((e_nrw.m_units.get (i)));
                if (wrap_in_block)
                {
                  retval += "\n}";
                }
              }
            }
            else
              if (e instanceof OneOrMore)
              {
                final OneOrMore e_nrw = (OneOrMore) e;
                final Expansion nested_e = e_nrw.expansion;
                Lookahead la;
                if (nested_e instanceof Sequence)
                {
                  la = (Lookahead) (((Sequence) nested_e).m_units.get (0));
                }
                else
                {
                  la = new Lookahead ();
                  la.setAmount (Options.getLookahead ());
                  la.setLaExpansion (nested_e);
                }
                retval += "\n";
                final int labelIndex = ++m_gensymindex;
                if (m_isJavaDialect)
                {
                  retval += "label_" + labelIndex + ":\n";
                }
                retval += "while (" + (m_isJavaDialect ? "true" : "!hasError") + ") {\u0001";
                retval += phase1ExpansionGen (nested_e);
                conds = new Lookahead [1];
                conds[0] = la;
                actions = new String [2];
                actions[0] = "\n;";

                if (m_isJavaDialect)
                {
                  actions[1] = "\nbreak label_" + labelIndex + ";";
                }
                else
                {
                  actions[1] = "\ngoto end_label_" + labelIndex + ";";
                }

                retval += buildLookaheadChecker (conds, actions);
                retval += "\u0002\n" + "}";
                if (!m_isJavaDialect)
                {
                  retval += "\nend_label_" + labelIndex + ": ;";
                }
              }
              else
                if (e instanceof ZeroOrMore)
                {
                  final ZeroOrMore e_nrw = (ZeroOrMore) e;
                  final Expansion nested_e = e_nrw.expansion;
                  Lookahead la;
                  if (nested_e instanceof Sequence)
                  {
                    la = (Lookahead) (((Sequence) nested_e).m_units.get (0));
                  }
                  else
                  {
                    la = new Lookahead ();
                    la.setAmount (Options.getLookahead ());
                    la.setLaExpansion (nested_e);
                  }
                  retval += "\n";
                  final int labelIndex = ++m_gensymindex;
                  if (m_isJavaDialect)
                  {
                    retval += "label_" + labelIndex + ":\n";
                  }
                  retval += "while (" + (m_isJavaDialect ? "true" : "!hasError") + ") {\u0001";
                  conds = new Lookahead [1];
                  conds[0] = la;
                  actions = new String [2];
                  actions[0] = "\n;";
                  if (m_isJavaDialect)
                  {
                    actions[1] = "\nbreak label_" + labelIndex + ";";
                  }
                  else
                  {
                    actions[1] = "\ngoto end_label_" + labelIndex + ";";
                  }
                  retval += buildLookaheadChecker (conds, actions);
                  retval += phase1ExpansionGen (nested_e);
                  retval += "\u0002\n" + "}";
                  if (!m_isJavaDialect)
                  {
                    retval += "\nend_label_" + labelIndex + ": ;";
                  }
                }
                else
                  if (e instanceof ZeroOrOne)
                  {
                    final ZeroOrOne e_nrw = (ZeroOrOne) e;
                    final Expansion nested_e = e_nrw.expansion;
                    Lookahead la;
                    if (nested_e instanceof Sequence)
                    {
                      la = (Lookahead) (((Sequence) nested_e).m_units.get (0));
                    }
                    else
                    {
                      la = new Lookahead ();
                      la.setAmount (Options.getLookahead ());
                      la.setLaExpansion (nested_e);
                    }
                    conds = new Lookahead [1];
                    conds[0] = la;
                    actions = new String [2];
                    actions[0] = phase1ExpansionGen (nested_e);
                    actions[1] = "\n;";
                    retval += buildLookaheadChecker (conds, actions);
                  }
                  else
                    if (e instanceof TryBlock)
                    {
                      final TryBlock e_nrw = (TryBlock) e;
                      final Expansion nested_e = e_nrw.exp;
                      List <Token> list;
                      retval += "\n";
                      retval += "try {\u0001";
                      retval += phase1ExpansionGen (nested_e);
                      retval += "\u0002\n" + "}";
                      for (int i = 0; i < e_nrw.m_catchblks.size (); i++)
                      {
                        retval += " catch (";
                        list = (e_nrw.types.get (i));
                        if (list.size () != 0)
                        {
                          m_codeGenerator.printTokenSetup ((list.get (0)));
                          for (final Object aElement : list)
                          {
                            t = (Token) aElement;
                            retval += m_codeGenerator.getStringToPrint (t);
                          }
                          retval += m_codeGenerator.getTrailingComments (t);
                        }
                        retval += " ";
                        t = (e_nrw.m_ids.get (i));
                        m_codeGenerator.printTokenSetup (t);
                        retval += m_codeGenerator.getStringToPrint (t);
                        retval += m_codeGenerator.getTrailingComments (t);
                        retval += ") {\u0003\n";
                        list = (e_nrw.m_catchblks.get (i));
                        if (list.size () != 0)
                        {
                          m_codeGenerator.printTokenSetup ((list.get (0)));
                          s_ccol = 1;
                          for (final Object aElement : list)
                          {
                            t = (Token) aElement;
                            retval += m_codeGenerator.getStringToPrint (t);
                          }
                          retval += m_codeGenerator.getTrailingComments (t);
                        }
                        retval += "\u0004\n" + "}";
                      }
                      if (e_nrw.m_finallyblk != null)
                      {
                        if (m_isJavaDialect)
                        {
                          retval += " finally {\u0003\n";
                        }
                        else
                        {
                          retval += " finally {\u0003\n";
                        }

                        if (e_nrw.m_finallyblk.size () != 0)
                        {
                          m_codeGenerator.printTokenSetup ((e_nrw.m_finallyblk.get (0)));
                          s_ccol = 1;
                          for (final Object aElement : e_nrw.m_finallyblk)
                          {
                            t = (Token) aElement;
                            retval += m_codeGenerator.getStringToPrint (t);
                          }
                          retval += m_codeGenerator.getTrailingComments (t);
                        }
                        retval += "\u0004\n" + "}";
                      }
                    }
    return retval;
  }

  void buildPhase2Routine (final Lookahead la)
  {
    final Expansion e = la.getLaExpansion ();
    if (m_isJavaDialect)
    {
      m_codeGenerator.genCodeLine ("  " +
                                   staticOpt () +
                                   "private " +
                                   Options.getBooleanType () +
                                   " jj_2" +
                                   e.m_internal_name +
                                   "(int xla)");
    }
    else
    {
      m_codeGenerator.genCodeLine (" inline bool jj_2" + e.m_internal_name + "(int xla)");
    }
    m_codeGenerator.genCodeLine (" {");
    m_codeGenerator.genCodeLine ("    jj_la = xla; jj_lastpos = jj_scanpos = token;");

    String ret_suffix = "";
    if (Options.getDepthLimit () > 0)
    {
      ret_suffix = " && !jj_depth_error";
    }

    if (m_isJavaDialect)
    {
      m_codeGenerator.genCodeLine ("    try { return (!jj_3" + e.m_internal_name + "()" + ret_suffix + "); }");
      m_codeGenerator.genCodeLine ("    catch(LookaheadSuccess ls) { return true; }");
    }
    else
    {
      m_codeGenerator.genCodeLine ("    jj_done = false;");
      m_codeGenerator.genCodeLine ("    return (!jj_3" + e.m_internal_name + "() || jj_done)" + ret_suffix + ";");
    }
    if (Options.getErrorReporting ())
    {
      m_codeGenerator.genCodeLine ((m_isJavaDialect ? "    finally " : " ") +
                                   "{ jj_save(" +
                                   (Integer.parseInt (e.m_internal_name.substring (1)) - 1) +
                                   ", xla); }");
    }
    m_codeGenerator.genCodeLine ("  }");
    m_codeGenerator.genCodeLine ("");
    final Phase3Data p3d = new Phase3Data (e, la.getAmount ());
    m_phase3list.add (p3d);
    m_phase3table.put (e, p3d);
  }

  private boolean xsp_declared;

  Expansion jj3_expansion;

  String genReturn (final boolean value)
  {
    final String retval = (value ? "true" : "false");
    if (Options.getDebugLookahead () && jj3_expansion != null)
    {
      String tracecode = "trace_return(\"" +
                         JavaCCGlobals.addUnicodeEscapes (((NormalProduction) jj3_expansion.m_parent).getLhs ()) +
                         "(LOOKAHEAD " +
                         (value ? "FAILED" : "SUCCEEDED") +
                         ")\");";
      if (Options.getErrorReporting ())
      {
        tracecode = "if (!jj_rescan) " + tracecode;
      }
      return "{ " + tracecode + " return " + retval + "; }";
    }
    return "return " + retval + ";";
  }

  private void generate3R (final Expansion e, final Phase3Data inf)
  {
    Expansion seq = e;
    if (e.m_internal_name.length () == 0)
    {
      while (true)
      {
        if (seq instanceof Sequence && ((Sequence) seq).m_units.size () == 2)
        {
          seq = ((Sequence) seq).m_units.get (1);
        }
        else
          if (seq instanceof NonTerminal)
          {
            final NonTerminal e_nrw = (NonTerminal) seq;
            final NormalProduction ntprod = (s_production_table.get (e_nrw.getName ()));
            if (ntprod instanceof AbstractCodeProduction)
            {
              break; // nothing to do here
            }
            seq = ntprod.getExpansion ();
          }
          else
            break;
      }

      if (seq instanceof RegularExpression)
      {
        e.m_internal_name = "jj_scan_token(" + ((RegularExpression) seq).m_ordinal + ")";
        return;
      }

      m_gensymindex++;
      // if (gensymindex == 100)
      // {
      // new Error().codeGenerator.printStackTrace();
      // System.out.println(" ***** seq: " + seq.internal_name + "; size: " +
      // ((Sequence)seq).units.size());
      // }
      e.m_internal_name = "R_" + m_gensymindex;
      e.m_internal_index = m_gensymindex;
    }
    Phase3Data p3d = (m_phase3table.get (e));
    if (p3d == null || p3d.count < inf.count)
    {
      p3d = new Phase3Data (e, inf.count);
      m_phase3list.add (p3d);
      m_phase3table.put (e, p3d);
    }
  }

  void setupPhase3Builds (final Phase3Data inf)
  {
    final Expansion e = inf.exp;
    if (e instanceof RegularExpression)
    {
      // nothing to here
    }
    else
      if (e instanceof NonTerminal)
      {
        // All expansions of non-terminals have the "name" fields set. So
        // there's no need to check it below for "e_nrw" and "ntexp". In
        // fact, we rely here on the fact that the "name" fields of both these
        // variables are the same.
        final NonTerminal e_nrw = (NonTerminal) e;
        final NormalProduction ntprod = (s_production_table.get (e_nrw.getName ()));
        if (ntprod instanceof AbstractCodeProduction)
        {
          // nothing to do here
        }
        else
        {
          generate3R (ntprod.getExpansion (), inf);
        }
      }
      else
        if (e instanceof Choice)
        {
          final Choice e_nrw = (Choice) e;
          for (int i = 0; i < e_nrw.getChoices ().size (); i++)
          {
            generate3R ((e_nrw.getChoices ().get (i)), inf);
          }
        }
        else
          if (e instanceof Sequence)
          {
            final Sequence e_nrw = (Sequence) e;
            // We skip the first element in the following iteration since it is
            // the
            // Lookahead object.
            int cnt = inf.count;
            for (int i = 1; i < e_nrw.m_units.size (); i++)
            {
              final Expansion eseq = (e_nrw.m_units.get (i));
              setupPhase3Builds (new Phase3Data (eseq, cnt));
              cnt -= minimumSize (eseq);
              if (cnt <= 0)
                break;
            }
          }
          else
            if (e instanceof TryBlock)
            {
              final TryBlock e_nrw = (TryBlock) e;
              setupPhase3Builds (new Phase3Data (e_nrw.exp, inf.count));
            }
            else
              if (e instanceof OneOrMore)
              {
                final OneOrMore e_nrw = (OneOrMore) e;
                generate3R (e_nrw.expansion, inf);
              }
              else
                if (e instanceof ZeroOrMore)
                {
                  final ZeroOrMore e_nrw = (ZeroOrMore) e;
                  generate3R (e_nrw.expansion, inf);
                }
                else
                  if (e instanceof ZeroOrOne)
                  {
                    final ZeroOrOne e_nrw = (ZeroOrOne) e;
                    generate3R (e_nrw.expansion, inf);
                  }
  }

  private String getTypeForToken ()
  {
    return m_isJavaDialect ? "Token" : "Token *";
  }

  private String genjj_3Call (final Expansion e)
  {
    if (e.m_internal_name.startsWith ("jj_scan_token"))
      return e.m_internal_name;
    return "jj_3" + e.m_internal_name + "()";
  }

  void buildPhase3Routine (final Phase3Data inf, final boolean recursive_call)
  {
    final Expansion e = inf.exp;
    Token t = null;
    if (e.m_internal_name.startsWith ("jj_scan_token"))
      return;

    if (!recursive_call)
    {
      if (m_isJavaDialect)
      {
        m_codeGenerator.genCodeLine ("  " +
                                     staticOpt () +
                                     "private " +
                                     Options.getBooleanType () +
                                     " jj_3" +
                                     e.m_internal_name +
                                     "()");
      }
      else
      {
        m_codeGenerator.genCodeLine (" inline bool jj_3" + e.m_internal_name + "()");
      }

      m_codeGenerator.genCodeLine (" {");
      if (!m_isJavaDialect)
      {
        m_codeGenerator.genCodeLine ("    if (jj_done) return true;");
        if (Options.getDepthLimit () > 0)
        {
          m_codeGenerator.genCodeLine ("#define __ERROR_RET__ true");
        }
      }
      genStackCheck (false);
      xsp_declared = false;
      if (Options.getDebugLookahead () && e.m_parent instanceof NormalProduction)
      {
        m_codeGenerator.genCode ("    ");
        if (Options.getErrorReporting ())
        {
          m_codeGenerator.genCode ("if (!jj_rescan) ");
        }
        m_codeGenerator.genCodeLine ("trace_call(\"" +
                                     JavaCCGlobals.addUnicodeEscapes (((NormalProduction) e.m_parent).getLhs ()) +
                                     "(LOOKING AHEAD...)\");");
        jj3_expansion = e;
      }
      else
      {
        jj3_expansion = null;
      }
    }
    if (e instanceof RegularExpression)
    {
      final RegularExpression e_nrw = (RegularExpression) e;
      if (e_nrw.m_label.length () == 0)
      {
        final Object label = s_names_of_tokens.get (Integer.valueOf (e_nrw.m_ordinal));
        if (label != null)
        {
          m_codeGenerator.genCodeLine ("    if (jj_scan_token(" + (String) label + ")) " + genReturn (true));
        }
        else
        {
          m_codeGenerator.genCodeLine ("    if (jj_scan_token(" + e_nrw.m_ordinal + ")) " + genReturn (true));
        }
      }
      else
      {
        m_codeGenerator.genCodeLine ("    if (jj_scan_token(" + e_nrw.m_label + ")) " + genReturn (true));
      }
      // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos == jj_lastpos)
      // " + genReturn(false));
    }
    else
      if (e instanceof NonTerminal)
      {
        // All expansions of non-terminals have the "name" fields set. So
        // there's no need to check it below for "e_nrw" and "ntexp". In
        // fact, we rely here on the fact that the "name" fields of both these
        // variables are the same.
        final NonTerminal e_nrw = (NonTerminal) e;
        final NormalProduction ntprod = (s_production_table.get (e_nrw.getName ()));
        if (ntprod instanceof AbstractCodeProduction)
        {
          m_codeGenerator.genCodeLine ("    if (true) { jj_la = 0; jj_scanpos = jj_lastpos; " +
                                       genReturn (false) +
                                       "}");
        }
        else
        {
          final Expansion ntexp = ntprod.getExpansion ();
          // codeGenerator.genCodeLine(" if (jj_3" + ntexp.internal_name + "())
          // " + genReturn(true));
          m_codeGenerator.genCodeLine ("    if (" + genjj_3Call (ntexp) + ") " + genReturn (true));
          // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
          // jj_lastpos) " + genReturn(false));
        }
      }
      else
        if (e instanceof Choice)
        {
          Sequence nested_seq;
          final Choice e_nrw = (Choice) e;
          if (e_nrw.getChoices ().size () != 1)
          {
            if (!xsp_declared)
            {
              xsp_declared = true;
              m_codeGenerator.genCodeLine ("    " + getTypeForToken () + " xsp;");
            }
            m_codeGenerator.genCodeLine ("    xsp = jj_scanpos;");
          }
          for (int i = 0; i < e_nrw.getChoices ().size (); i++)
          {
            nested_seq = (Sequence) (e_nrw.getChoices ().get (i));
            final Lookahead la = (Lookahead) (nested_seq.m_units.get (0));
            if (la.getActionTokens ().size () != 0)
            {
              // We have semantic lookahead that must be evaluated.
              s_lookaheadNeeded = true;
              m_codeGenerator.genCodeLine ("    jj_lookingAhead = true;");
              m_codeGenerator.genCode ("    jj_semLA = ");
              m_codeGenerator.printTokenSetup ((la.getActionTokens ().get (0)));
              for (final Object aElement : la.getActionTokens ())
              {
                t = (Token) aElement;
                m_codeGenerator.printToken (t);
              }
              m_codeGenerator.printTrailingComments (t);
              m_codeGenerator.genCodeLine (";");
              m_codeGenerator.genCodeLine ("    jj_lookingAhead = false;");
            }
            m_codeGenerator.genCode ("    if (");
            if (la.getActionTokens ().size () != 0)
            {
              m_codeGenerator.genCode ("!jj_semLA || ");
            }
            if (i != e_nrw.getChoices ().size () - 1)
            {
              // codeGenerator.genCodeLine("jj_3" + nested_seq.internal_name +
              // "()) {");
              m_codeGenerator.genCodeLine (genjj_3Call (nested_seq) + ") {");
              m_codeGenerator.genCodeLine ("    jj_scanpos = xsp;");
            }
            else
            {
              // codeGenerator.genCodeLine("jj_3" + nested_seq.internal_name +
              // "()) " + genReturn(true));
              m_codeGenerator.genCodeLine (genjj_3Call (nested_seq) + ") " + genReturn (true));
              // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
              // jj_lastpos) " + genReturn(false));
            }
          }
          for (int i = 1; i < e_nrw.getChoices ().size (); i++)
          {
            // codeGenerator.genCodeLine(" } else if (jj_la == 0 && jj_scanpos
            // == jj_lastpos) " + genReturn(false));
            m_codeGenerator.genCodeLine ("    }");
          }
        }
        else
          if (e instanceof Sequence)
          {
            final Sequence e_nrw = (Sequence) e;
            // We skip the first element in the following iteration since it is
            // the
            // Lookahead object.
            int cnt = inf.count;
            for (int i = 1; i < e_nrw.m_units.size (); i++)
            {
              final Expansion eseq = (e_nrw.m_units.get (i));
              buildPhase3Routine (new Phase3Data (eseq, cnt), true);

              // System.out.println("minimumSize: line: " + eseq.line + ",
              // column: " + eseq.column + ": " +
              // minimumSize(eseq));//Test Code

              cnt -= minimumSize (eseq);
              if (cnt <= 0)
                break;
            }
          }
          else
            if (e instanceof TryBlock)
            {
              final TryBlock e_nrw = (TryBlock) e;
              buildPhase3Routine (new Phase3Data (e_nrw.exp, inf.count), true);
            }
            else
              if (e instanceof OneOrMore)
              {
                if (!xsp_declared)
                {
                  xsp_declared = true;
                  m_codeGenerator.genCodeLine ("    " + getTypeForToken () + " xsp;");
                }
                final OneOrMore e_nrw = (OneOrMore) e;
                final Expansion nested_e = e_nrw.expansion;
                // codeGenerator.genCodeLine(" if (jj_3" +
                // nested_e.internal_name + "()) " + genReturn(true));
                m_codeGenerator.genCodeLine ("    if (" + genjj_3Call (nested_e) + ") " + genReturn (true));
                // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
                // jj_lastpos) " + genReturn(false));
                m_codeGenerator.genCodeLine ("    while (true) {");
                m_codeGenerator.genCodeLine ("      xsp = jj_scanpos;");
                // codeGenerator.genCodeLine(" if (jj_3" +
                // nested_e.internal_name + "()) { jj_scanpos = xsp; break; }");
                m_codeGenerator.genCodeLine ("      if (" + genjj_3Call (nested_e) + ") { jj_scanpos = xsp; break; }");
                // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
                // jj_lastpos) " + genReturn(false));
                m_codeGenerator.genCodeLine ("    }");
              }
              else
                if (e instanceof ZeroOrMore)
                {
                  if (!xsp_declared)
                  {
                    xsp_declared = true;
                    m_codeGenerator.genCodeLine ("    " + getTypeForToken () + " xsp;");
                  }
                  final ZeroOrMore e_nrw = (ZeroOrMore) e;
                  final Expansion nested_e = e_nrw.expansion;
                  m_codeGenerator.genCodeLine ("    while (true) {");
                  m_codeGenerator.genCodeLine ("      xsp = jj_scanpos;");
                  // codeGenerator.genCodeLine(" if (jj_3" +
                  // nested_e.internal_name + "()) { jj_scanpos = xsp; break;
                  // }");
                  m_codeGenerator.genCodeLine ("      if (" +
                                               genjj_3Call (nested_e) +
                                               ") { jj_scanpos = xsp; break; }");
                  // codeGenerator.genCodeLine(" if (jj_la == 0 && jj_scanpos ==
                  // jj_lastpos) " + genReturn(false));
                  m_codeGenerator.genCodeLine ("    }");
                }
                else
                  if (e instanceof ZeroOrOne)
                  {
                    if (!xsp_declared)
                    {
                      xsp_declared = true;
                      m_codeGenerator.genCodeLine ("    " + getTypeForToken () + " xsp;");
                    }
                    final ZeroOrOne e_nrw = (ZeroOrOne) e;
                    final Expansion nested_e = e_nrw.expansion;
                    m_codeGenerator.genCodeLine ("    xsp = jj_scanpos;");
                    // codeGenerator.genCodeLine(" if (jj_3" +
                    // nested_e.internal_name + "()) jj_scanpos = xsp;");
                    m_codeGenerator.genCodeLine ("    if (" + genjj_3Call (nested_e) + ") jj_scanpos = xsp;");
                    // codeGenerator.genCodeLine(" else if (jj_la == 0 &&
                    // jj_scanpos == jj_lastpos) " + genReturn(false));
                  }
    if (!recursive_call)
    {
      m_codeGenerator.genCodeLine ("    " + genReturn (false));
      genStackCheckEnd ();
      if (!m_isJavaDialect && Options.getDepthLimit () > 0)
      {
        m_codeGenerator.genCodeLine ("#undef __ERROR_RET__");
      }
      m_codeGenerator.genCodeLine ("  }");
      m_codeGenerator.genCodeLine ("");
    }
  }

  int minimumSize (final Expansion e)
  {
    return minimumSize (e, Integer.MAX_VALUE);
  }

  /*
   * Returns the minimum number of tokens that can parse to this expansion.
   */
  int minimumSize (final Expansion e, final int oldMin)
  {
    int retval = 0; // should never be used. Will be bad if it is.
    if (e.m_inMinimumSize)
    {
      // recursive search for minimum size unnecessary.
      return Integer.MAX_VALUE;
    }
    e.m_inMinimumSize = true;
    if (e instanceof RegularExpression)
    {
      retval = 1;
    }
    else
      if (e instanceof NonTerminal)
      {
        final NonTerminal e_nrw = (NonTerminal) e;
        final NormalProduction ntprod = (s_production_table.get (e_nrw.getName ()));
        if (ntprod instanceof AbstractCodeProduction)
        {
          retval = Integer.MAX_VALUE;
          // Make caller think this is unending (for we do not go beyond
          // JAVACODE during
          // phase3 execution).
        }
        else
        {
          final Expansion ntexp = ntprod.getExpansion ();
          retval = minimumSize (ntexp);
        }
      }
      else
        if (e instanceof Choice)
        {
          int min = oldMin;
          Expansion nested_e;
          final Choice e_nrw = (Choice) e;
          for (int i = 0; min > 1 && i < e_nrw.getChoices ().size (); i++)
          {
            nested_e = (e_nrw.getChoices ().get (i));
            final int min1 = minimumSize (nested_e, min);
            if (min > min1)
              min = min1;
          }
          retval = min;
        }
        else
          if (e instanceof Sequence)
          {
            int min = 0;
            final Sequence e_nrw = (Sequence) e;
            // We skip the first element in the following iteration since it is
            // the
            // Lookahead object.
            for (int i = 1; i < e_nrw.m_units.size (); i++)
            {
              final Expansion eseq = (e_nrw.m_units.get (i));
              final int mineseq = minimumSize (eseq);
              if (min == Integer.MAX_VALUE || mineseq == Integer.MAX_VALUE)
              {
                min = Integer.MAX_VALUE; // Adding infinity to something results
                                         // in infinity.
              }
              else
              {
                min += mineseq;
                if (min > oldMin)
                  break;
              }
            }
            retval = min;
          }
          else
            if (e instanceof TryBlock)
            {
              final TryBlock e_nrw = (TryBlock) e;
              retval = minimumSize (e_nrw.exp);
            }
            else
              if (e instanceof OneOrMore)
              {
                final OneOrMore e_nrw = (OneOrMore) e;
                retval = minimumSize (e_nrw.expansion);
              }
              else
                if (e instanceof ZeroOrMore)
                {
                  retval = 0;
                }
                else
                  if (e instanceof ZeroOrOne)
                  {
                    retval = 0;
                  }
                  else
                    if (e instanceof Lookahead)
                    {
                      retval = 0;
                    }
                    else
                      if (e instanceof Action)
                      {
                        retval = 0;
                      }
    e.m_inMinimumSize = false;
    return retval;
  }

  void build (final CodeGenerator codeGenerator)
  {
    NormalProduction p;
    CodeProductionJava jp;
    CodeProductionCpp cp;
    Token t = null;

    this.m_codeGenerator = codeGenerator;
    for (final Object aElement : s_bnfproductions)
    {
      p = (NormalProduction) aElement;
      if (p instanceof CodeProductionCpp)
      {
        cp = (CodeProductionCpp) p;

        generateCPPMethodheader (cp);
        // t = (Token)(cp.getReturnTypeTokens().get(0));
        // codeGenerator.printTokenSetup(t); ccol = 1;
        // codeGenerator.printLeadingComments(t);
        // codeGenerator.genCode(" " + staticOpt() + (p.getAccessMod() != null ?
        // p.getAccessMod() + " " : ""));
        // cline = t.beginLine; ccol = t.beginColumn;
        // codeGenerator.printTokenOnly(t);
        // for (int i = 1; i < cp.getReturnTypeTokens().size(); i++) {
        // t = (Token)(cp.getReturnTypeTokens().get(i));
        // codeGenerator.printToken(t);
        // }
        // codeGenerator.printTrailingComments(t);
        // codeGenerator.genCode(" " + cp.getLhs() + "(");
        // if (cp.getParameterListTokens().size() != 0) {
        // codeGenerator.printTokenSetup((Token)(cp.getParameterListTokens().get(0)));
        // for (Iterator it = cp.getParameterListTokens().iterator();
        // it.hasNext();) {
        // t = (Token)it.next();
        // codeGenerator.printToken(t);
        // }
        // codeGenerator.printTrailingComments(t);
        // }
        // codeGenerator.genCode(")");
        // for (Iterator it = cp.getThrowsList().iterator();
        // it.hasNext();) {
        // codeGenerator.genCode(", ");
        // List name = (List)it.next();
        // for (Iterator it2 = name.iterator(); it2.hasNext();) {
        // t = (Token)it2.next();
        // codeGenerator.genCode(t.image);
        // }
        // }
        codeGenerator.genCodeLine (" {");
        if (Options.getDebugParser ())
        {
          codeGenerator.genCodeLine ("");
          if (m_isJavaDialect)
          {
            codeGenerator.genCodeLine ("    trace_call(\"" + JavaCCGlobals.addUnicodeEscapes (cp.getLhs ()) + "\");");
          }
          else
          {
            codeGenerator.genCodeLine ("    JJEnter<std::function<void()>> jjenter([this]() {trace_call  (\"" +
                                       JavaCCGlobals.addUnicodeEscapes (cp.getLhs ()) +
                                       "\"); });");
            codeGenerator.genCodeLine ("    JJExit <std::function<void()>> jjexit ([this]() {trace_return(\"" +
                                       JavaCCGlobals.addUnicodeEscapes (cp.getLhs ()) +
                                       "\"); });");
          }
          codeGenerator.genCodeLine ("    try {");
        }
        if (cp.getCodeTokens ().size () != 0)
        {
          codeGenerator.printTokenSetup ((cp.getCodeTokens ().get (0)));
          s_cline--;
          codeGenerator.printTokenList (cp.getCodeTokens ());
        }
        codeGenerator.genCodeLine ("");
        if (Options.getDebugParser ())
        {
          codeGenerator.genCodeLine ("    } catch(...) { }");
        }
        codeGenerator.genCodeLine ("  }");
        codeGenerator.genCodeLine ("");
      }
      else
        if (p instanceof CodeProductionJava)
        {
          if (!m_isJavaDialect)
          {
            JavaCCErrors.semantic_error ("Cannot use JAVACODE productions with C++ output (yet).");
            continue;
          }
          jp = (CodeProductionJava) p;
          t = (jp.getReturnTypeTokens ().get (0));
          codeGenerator.printTokenSetup (t);
          s_ccol = 1;
          codeGenerator.printLeadingComments (t);
          codeGenerator.genCode ("  " + staticOpt () + (p.getAccessMod () != null ? p.getAccessMod () + " " : ""));
          s_cline = t.beginLine;
          s_ccol = t.beginColumn;
          codeGenerator.printTokenOnly (t);
          for (int i = 1; i < jp.getReturnTypeTokens ().size (); i++)
          {
            t = (jp.getReturnTypeTokens ().get (i));
            codeGenerator.printToken (t);
          }
          codeGenerator.printTrailingComments (t);
          codeGenerator.genCode (" " + jp.getLhs () + "(");
          if (jp.getParameterListTokens ().size () != 0)
          {
            codeGenerator.printTokenSetup ((jp.getParameterListTokens ().get (0)));
            for (final Object aElement2 : jp.getParameterListTokens ())
            {
              t = (Token) aElement2;
              codeGenerator.printToken (t);
            }
            codeGenerator.printTrailingComments (t);
          }
          codeGenerator.genCode (")");
          if (m_isJavaDialect)
          {
            codeGenerator.genCode (" throws ParseException");
          }
          for (final List <Token> aElement2 : jp.getThrowsList ())
          {
            codeGenerator.genCode (", ");
            for (final Token x : aElement2)
            {
              t = x;
              codeGenerator.genCode (t.image);
            }
          }
          codeGenerator.genCode (" {");
          if (Options.getDebugParser ())
          {
            codeGenerator.genCodeLine ("");
            codeGenerator.genCodeLine ("    trace_call(\"" + JavaCCGlobals.addUnicodeEscapes (jp.getLhs ()) + "\");");
            codeGenerator.genCode ("    try {");
          }
          if (jp.getCodeTokens ().size () != 0)
          {
            codeGenerator.printTokenSetup ((jp.getCodeTokens ().get (0)));
            s_cline--;
            codeGenerator.printTokenList (jp.getCodeTokens ());
          }
          codeGenerator.genCodeLine ("");
          if (Options.getDebugParser ())
          {
            codeGenerator.genCodeLine ("    } finally {");
            codeGenerator.genCodeLine ("      trace_return(\"" +
                                       JavaCCGlobals.addUnicodeEscapes (jp.getLhs ()) +
                                       "\");");
            codeGenerator.genCodeLine ("    }");
          }
          codeGenerator.genCodeLine ("  }");
          codeGenerator.genCodeLine ("");
        }
        else
        {
          buildPhase1Routine ((BNFProduction) p);
        }
    }

    codeGenerator.switchToIncludeFile ();

    for (int i = 0; i < m_phase2list.size (); i++)
    {
      buildPhase2Routine (m_phase2list.get (i));
    }

    int phase3index = 0;
    while (phase3index < m_phase3list.size ())
    {
      for (; phase3index < m_phase3list.size (); phase3index++)
      {
        setupPhase3Builds (m_phase3list.get (phase3index));
      }
    }

    for (final Phase3Data data : m_phase3table.values ())
    {
      buildPhase3Routine (data, false);
    }
    // for (Enumeration enumeration = phase3table.elements();
    // enumeration.hasMoreElements();) {
    // Phase3Data inf = (Phase3Data)(enumeration.nextElement());
    // System.err.println("**** Table for: " + inf.exp.internal_name);
    // buildPhase3Table(inf);
    // System.err.println("**** END TABLE *********");
    // }

    codeGenerator.switchToMainFile ();
  }

  public void reInit ()
  {
    m_gensymindex = 0;
    m_indentamt = 0;
    m_bJJ2LA = false;
    m_phase2list.clear ();
    m_phase3list.clear ();
    m_phase3table.clear ();
    firstSet = null;
    xsp_declared = false;
    jj3_expansion = null;
  }

  // Table driven.
  void buildPhase3Table (final Phase3Data inf)
  {
    final Expansion e = inf.exp;
    if (e instanceof RegularExpression)
    {
      final RegularExpression e_nrw = (RegularExpression) e;
      System.err.println ("TOKEN, " + e_nrw.m_ordinal);
    }
    else
      if (e instanceof NonTerminal)
      {
        final NonTerminal e_nrw = (NonTerminal) e;
        final NormalProduction ntprod = (s_production_table.get (e_nrw.getName ()));
        if (ntprod instanceof AbstractCodeProduction)
        {
          // javacode, true - always (warn?)
          System.err.println ("JAVACODE_PROD, true");
        }
        else
        {
          final Expansion ntexp = ntprod.getExpansion ();
          // nt exp's table.
          System.err.println ("PRODUCTION, " + ntexp.m_internal_index);
          // buildPhase3Table(new Phase3Data(ntexp, inf.count));
        }
      }
      else
        if (e instanceof Choice)
        {
          Sequence nested_seq;
          final Choice e_nrw = (Choice) e;
          System.err.print ("CHOICE, ");
          for (int i = 0; i < e_nrw.getChoices ().size (); i++)
          {
            if (i > 0)
              System.err.print ("\n|");
            nested_seq = (Sequence) (e_nrw.getChoices ().get (i));
            final Lookahead la = (Lookahead) (nested_seq.m_units.get (0));
            if (la.getActionTokens ().size () != 0)
            {
              System.err.print ("SEMANTIC,");
            }
            else
            {
              buildPhase3Table (new Phase3Data (nested_seq, inf.count));
            }
          }
          System.err.println ();
        }
        else
          if (e instanceof Sequence)
          {
            final Sequence e_nrw = (Sequence) e;
            int cnt = inf.count;
            if (e_nrw.m_units.size () > 2)
            {
              System.err.println ("SEQ, " + cnt);
              for (int i = 1; i < e_nrw.m_units.size (); i++)
              {
                System.err.print ("   ");
                final Expansion eseq = (e_nrw.m_units.get (i));
                buildPhase3Table (new Phase3Data (eseq, cnt));
                cnt -= minimumSize (eseq);
                if (cnt <= 0)
                  break;
              }
            }
            else
            {
              Expansion tmp = e_nrw.m_units.get (1);
              while (tmp instanceof NonTerminal)
              {
                final NormalProduction ntprod = (s_production_table.get (((NonTerminal) tmp).getName ()));
                if (ntprod instanceof AbstractCodeProduction)
                  break;
                tmp = ntprod.getExpansion ();
              }
              buildPhase3Table (new Phase3Data (tmp, cnt));
            }
            System.err.println ();
          }
          else
            if (e instanceof TryBlock)
            {
              final TryBlock e_nrw = (TryBlock) e;
              buildPhase3Table (new Phase3Data (e_nrw.exp, inf.count));
            }
            else
              if (e instanceof OneOrMore)
              {
                final OneOrMore e_nrw = (OneOrMore) e;
                System.err.println ("SEQ PROD " + e_nrw.expansion.m_internal_index);
                System.err.println ("ZEROORMORE " + e_nrw.expansion.m_internal_index);
              }
              else
                if (e instanceof ZeroOrMore)
                {
                  final ZeroOrMore e_nrw = (ZeroOrMore) e;
                  System.err.print ("ZEROORMORE, " + e_nrw.expansion.m_internal_index);
                }
                else
                  if (e instanceof ZeroOrOne)
                  {
                    final ZeroOrOne e_nrw = (ZeroOrOne) e;
                    System.err.println ("ZERORONE, " + e_nrw.expansion.m_internal_index);
                  }
                  else
                  {
                    assert (false);
                    // table for nested_e - optional
                  }
  }
}

/**
 * This class stores information to pass from phase 2 to phase 3.
 */
class Phase3Data
{

  /*
   * This is the expansion to generate the jj3 method for.
   */
  Expansion exp;

  /*
   * This is the number of tokens that can still be consumed. This number is
   * used to limit the number of jj3 methods generated.
   */
  int count;

  Phase3Data (final Expansion e, final int c)
  {
    exp = e;
    count = c;
  }
}
