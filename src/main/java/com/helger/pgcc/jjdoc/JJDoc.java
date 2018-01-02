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
package com.helger.pgcc.jjdoc;

import java.util.Iterator;
import java.util.List;

import com.helger.commons.string.StringHelper;
import com.helger.pgcc.parser.*;

/**
 * The main entry point for JJDoc.
 */
public class JJDoc extends JJDocGlobals
{

  static void start ()
  {
    s_generator = getGenerator ();
    s_generator.documentStart ();
    _emitTokenProductions (s_generator, s_rexprlist);
    _emitNormalProductions (s_generator, s_bnfproductions);
    s_generator.documentEnd ();
  }

  private static Token _getPrecedingSpecialToken (final Token tok)
  {
    Token t = tok;
    while (t.specialToken != null)
    {
      t = t.specialToken;
    }
    return t != tok ? t : null;
  }

  private static void _emitTopLevelSpecialTokens (final Token aTok, final IDocGenerator gen)
  {
    if (aTok == null)
    {
      // Strange ...
      return;
    }
    Token tok = _getPrecedingSpecialToken (aTok);
    String s = "";
    if (tok != null)
    {
      s_cline = tok.beginLine;
      s_ccol = tok.beginColumn;
      while (tok != null)
      {
        s += printTokenOnly (tok);
        tok = tok.next;
      }
    }
    if (s.length () > 0)
      gen.specialTokens (s);
  }

  /*
   * private static boolean toplevelExpansion(Expansion exp) { return exp.parent
   * != null && ( (exp.parent instanceof NormalProduction) || (exp.parent
   * instanceof TokenProduction) ); }
   */

  private static void _emitTokenProductions (final IDocGenerator gen, final List <TokenProduction> prods)
  {
    gen.tokensStart ();
    // FIXME there are many empty productions here
    for (final TokenProduction aTokenProduction : prods)
    {
      final TokenProduction tp = aTokenProduction;
      _emitTopLevelSpecialTokens (tp.m_firstToken, gen);

      gen.handleTokenProduction (tp);

      // if (!token.equals("")) {
      // gen.tokenStart(tp);
      // String token = getStandardTokenProductionText(tp);
      // gen.text(token);
      // gen.tokenEnd(tp);
      // }
    }
    gen.tokensEnd ();
  }

  public static String getStandardTokenProductionText (final TokenProduction tp)
  {
    String token = "";
    if (tp.m_isExplicit)
    {
      if (tp.m_lexStates == null)
      {
        token += "<*> ";
      }
      else
      {
        token += "<";
        for (int i = 0; i < tp.m_lexStates.length; ++i)
        {
          token += tp.m_lexStates[i];
          if (i < tp.m_lexStates.length - 1)
          {
            token += ",";
          }
        }
        token += "> ";
      }
      token += TokenProduction.kindImage[tp.m_kind];
      if (tp.m_ignoreCase)
      {
        token += " [IGNORE_CASE]";
      }
      token += " : {\n";
      for (final Iterator <RegExprSpec> it2 = tp.m_respecs.iterator (); it2.hasNext ();)
      {
        final RegExprSpec res = it2.next ();

        token += emitRE (res.rexp);

        if (res.nsTok != null)
        {
          token += " : " + res.nsTok.image;
        }

        token += "\n";
        if (it2.hasNext ())
        {
          token += "| ";
        }
      }
      token += "}\n\n";
    }
    return token;
  }

  private static void _emitNormalProductions (final IDocGenerator gen, final List <NormalProduction> prods)
  {
    gen.nonterminalsStart ();
    for (final NormalProduction np : prods)
    {
      _emitTopLevelSpecialTokens (np.getFirstToken (), gen);
      if (np instanceof BNFProduction)
      {
        gen.productionStart (np);
        if (np.getExpansion () instanceof Choice)
        {
          boolean first = true;
          final Choice c = (Choice) np.getExpansion ();
          for (final Object aElement : c.getChoices ())
          {
            final Expansion e = (Expansion) (aElement);
            gen.expansionStart (e, first);
            _emitExpansionTree (e, gen);
            gen.expansionEnd (e, first);
            first = false;
          }
        }
        else
        {
          gen.expansionStart (np.getExpansion (), true);
          _emitExpansionTree (np.getExpansion (), gen);
          gen.expansionEnd (np.getExpansion (), true);
        }
        gen.productionEnd (np);
      }
      else
        if (np instanceof CodeProductionCpp)
        {
          gen.cppcode ((CodeProductionCpp) np);
        }
        else
          if (np instanceof CodeProductionJava)
          {
            gen.javacode ((CodeProductionJava) np);
          }
    }
    gen.nonterminalsEnd ();
  }

  private static void _emitExpansionTree (final Expansion exp, final IDocGenerator gen)
  {
    // gen.text("[->" + exp.getClass().getName() + "]");
    if (exp instanceof Action)
    {
      _emitExpansionAction ((Action) exp, gen);
    }
    else
      if (exp instanceof Choice)
      {
        _emitExpansionChoice ((Choice) exp, gen);
      }
      else
        if (exp instanceof Lookahead)
        {
          _emitExpansionLookahead ((Lookahead) exp, gen);
        }
        else
          if (exp instanceof NonTerminal)
          {
            _emitExpansionNonTerminal ((NonTerminal) exp, gen);
          }
          else
            if (exp instanceof OneOrMore)
            {
              _emitExpansionOneOrMore ((OneOrMore) exp, gen);
            }
            else
              if (exp instanceof RegularExpression)
              {
                _emitExpansionRegularExpression ((RegularExpression) exp, gen);
              }
              else
                if (exp instanceof Sequence)
                {
                  _emitExpansionSequence ((Sequence) exp, gen);
                }
                else
                  if (exp instanceof TryBlock)
                  {
                    _emitExpansionTryBlock ((TryBlock) exp, gen);
                  }
                  else
                    if (exp instanceof ZeroOrMore)
                    {
                      _emitExpansionZeroOrMore ((ZeroOrMore) exp, gen);
                    }
                    else
                      if (exp instanceof ZeroOrOne)
                      {
                        _emitExpansionZeroOrOne ((ZeroOrOne) exp, gen);
                      }
                      else
                      {
                        error ("Oops: Unknown expansion type.");
                      }
    // gen.text("[<-" + exp.getClass().getName() + "]");
  }

  private static void _emitExpansionAction (final Action a, final IDocGenerator gen)
  {
    gen.doNothing (a);
  }

  private static void _emitExpansionChoice (final Choice c, final IDocGenerator gen)
  {
    for (final Iterator <Expansion> it = c.getChoices ().iterator (); it.hasNext ();)
    {
      final Expansion e = it.next ();
      _emitExpansionTree (e, gen);
      if (it.hasNext ())
        gen.text (" | ");
    }
  }

  private static void _emitExpansionLookahead (final Lookahead l, final IDocGenerator gen)
  {
    gen.doNothing (l);
  }

  private static void _emitExpansionNonTerminal (final NonTerminal nt, final IDocGenerator gen)
  {
    gen.nonTerminalStart (nt);
    gen.text (nt.getName ());
    gen.nonTerminalEnd (nt);
  }

  private static void _emitExpansionOneOrMore (final OneOrMore o, final IDocGenerator gen)
  {
    gen.text ("( ");
    _emitExpansionTree (o.expansion, gen);
    gen.text (" )+");
  }

  private static void _emitExpansionRegularExpression (final RegularExpression r, final IDocGenerator gen)
  {
    final String reRendered = emitRE (r);
    if (StringHelper.hasText (reRendered))
    {
      gen.reStart (r);
      gen.text (reRendered);
      gen.reEnd (r);
    }
  }

  private static void _emitExpansionSequence (final Sequence s, final IDocGenerator gen)
  {
    boolean firstUnit = true;
    for (final Expansion e : s.units ())
    {
      if (e instanceof Lookahead || e instanceof Action)
      {
        continue;
      }
      if (!firstUnit)
      {
        gen.text (" ");
      }
      final boolean needParens = (e instanceof Choice) || (e instanceof Sequence);
      if (needParens)
      {
        gen.text ("( ");
      }
      _emitExpansionTree (e, gen);
      if (needParens)
      {
        gen.text (" )");
      }
      firstUnit = false;
    }
  }

  private static void _emitExpansionTryBlock (final TryBlock t, final IDocGenerator gen)
  {
    final boolean needParens = t.exp instanceof Choice;
    if (needParens)
    {
      gen.text ("( ");
    }
    _emitExpansionTree (t.exp, gen);
    if (needParens)
    {
      gen.text (" )");
    }
  }

  private static void _emitExpansionZeroOrMore (final ZeroOrMore z, final IDocGenerator gen)
  {
    gen.text ("( ");
    _emitExpansionTree (z.m_expansion, gen);
    gen.text (" )*");
  }

  private static void _emitExpansionZeroOrOne (final ZeroOrOne z, final IDocGenerator gen)
  {
    gen.text ("( ");
    _emitExpansionTree (z.expansion, gen);
    gen.text (" )?");
  }

  public static String emitRE (final RegularExpression re)
  {
    String returnString = "";
    final boolean hasLabel = StringHelper.hasText (re.m_label);
    final boolean justName = re instanceof RJustName;
    final boolean eof = re instanceof REndOfFile;
    final boolean isString = re instanceof RStringLiteral;
    final boolean toplevelRE = re.tpContext != null;
    final boolean needBrackets = justName || eof || hasLabel || (!isString && toplevelRE);
    if (needBrackets)
    {
      returnString += "<";
      if (!justName)
      {
        if (re.private_rexp)
        {
          returnString += "#";
        }
        if (hasLabel)
        {
          returnString += re.m_label;
          returnString += ": ";
        }
      }
    }
    if (re instanceof RCharacterList)
    {
      final RCharacterList cl = (RCharacterList) re;
      if (cl.negated_list)
      {
        returnString += "~";
      }
      returnString += "[";
      for (final Iterator <ICCCharacter> it = cl.m_descriptors.iterator (); it.hasNext ();)
      {
        final ICCCharacter o = it.next ();
        if (o instanceof SingleCharacter)
        {
          returnString += "\"";
          final char s[] = { ((SingleCharacter) o).getChar () };
          returnString += addEscapes (new String (s));
          returnString += "\"";
        }
        else
          if (o instanceof CharacterRange)
          {
            returnString += "\"";
            final char s[] = { ((CharacterRange) o).getLeft () };
            returnString += addEscapes (new String (s));
            returnString += "\"-\"";
            s[0] = ((CharacterRange) o).getRight ();
            returnString += addEscapes (new String (s));
            returnString += "\"";
          }
          else
          {
            error ("Oops: unknown character list element type.");
          }
        if (it.hasNext ())
        {
          returnString += ",";
        }
      }
      returnString += "]";
    }
    else
      if (re instanceof RChoice)
      {
        final RChoice c = (RChoice) re;
        for (final Iterator <RegularExpression> it = c.getChoices ().iterator (); it.hasNext ();)
        {
          final RegularExpression sub = (it.next ());
          returnString += emitRE (sub);
          if (it.hasNext ())
          {
            returnString += " | ";
          }
        }
      }
      else
        if (re instanceof REndOfFile)
        {
          returnString += "EOF";
        }
        else
          if (re instanceof RJustName)
          {
            final RJustName jn = (RJustName) re;
            returnString += jn.m_label;
          }
          else
            if (re instanceof ROneOrMore)
            {
              final ROneOrMore om = (ROneOrMore) re;
              returnString += "(";
              returnString += emitRE (om.regexpr);
              returnString += ")+";
            }
            else
              if (re instanceof RSequence)
              {
                final RSequence s = (RSequence) re;
                for (final Iterator <RegularExpression> it = s.iterator (); it.hasNext ();)
                {
                  final RegularExpression sub = (it.next ());
                  boolean needParens = false;
                  if (sub instanceof RChoice)
                  {
                    needParens = true;
                  }
                  if (needParens)
                  {
                    returnString += "(";
                  }
                  returnString += emitRE (sub);
                  if (needParens)
                  {
                    returnString += ")";
                  }
                  if (it.hasNext ())
                  {
                    returnString += " ";
                  }
                }
              }
              else
                if (re instanceof RStringLiteral)
                {
                  final RStringLiteral sl = (RStringLiteral) re;
                  returnString += ("\"" + JavaCCGlobals.addEscapes (sl.m_image) + "\"");
                }
                else
                  if (re instanceof RZeroOrMore)
                  {
                    final RZeroOrMore zm = (RZeroOrMore) re;
                    returnString += "(";
                    returnString += emitRE (zm.regexpr);
                    returnString += ")*";
                  }
                  else
                    if (re instanceof RZeroOrOne)
                    {
                      final RZeroOrOne zo = (RZeroOrOne) re;
                      returnString += "(";
                      returnString += emitRE (zo.regexpr);
                      returnString += ")?";
                    }
                    else
                      if (re instanceof RRepetitionRange)
                      {
                        final RRepetitionRange zo = (RRepetitionRange) re;
                        returnString += "(";
                        returnString += emitRE (zo.regexpr);
                        returnString += ")";
                        returnString += "{";
                        if (zo.hasMax)
                        {
                          returnString += zo.min;
                          returnString += ",";
                          returnString += zo.max;
                        }
                        else
                        {
                          returnString += zo.min;
                        }
                        returnString += "}";
                      }
                      else
                      {
                        error ("Oops: Unknown regular expression type.");
                      }
    if (needBrackets)
    {
      returnString += ">";
    }
    return returnString;
  }

  /*
   * private static String v2s(List v, boolean newLine) { String s = ""; boolean
   * firstToken = true; for (Enumeration enumeration = v.elements();
   * enumeration.hasMoreElements();) { Token tok =
   * (Token)enumeration.nextElement(); Token stok =
   * getPrecedingSpecialToken(tok); if (firstToken) { if (stok != null) { cline
   * = stok.beginLine; ccol = stok.beginColumn; } else { cline = tok.beginLine;
   * ccol = tok.beginColumn; } s = ws(ccol - 1); firstToken = false; } while
   * (stok != null) { s += printToken(stok); stok = stok.next; } s +=
   * printToken(tok); } return s; }
   */
  /**
   * A utility to produce a string of blanks.
   */

  /*
   * private static String ws(int len) { String s = ""; for (int i = 0; i < len;
   * ++i) { s += " "; } return s; }
   */

}
