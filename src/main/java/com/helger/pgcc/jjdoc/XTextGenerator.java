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
package com.helger.pgcc.jjdoc;

import java.io.IOException;
import java.io.Writer;

import com.helger.pgcc.parser.CodeProductionCpp;
import com.helger.pgcc.parser.CodeProductionJava;
import com.helger.pgcc.parser.NormalProduction;
import com.helger.pgcc.parser.RegExprSpec;
import com.helger.pgcc.parser.TokenProduction;
import com.helger.pgcc.parser.exp.AbstractExpRegularExpression;
import com.helger.pgcc.parser.exp.ExpNonTerminal;
import com.helger.pgcc.parser.exp.Expansion;

/**
 * Output BNF in XText format.
 */
public class XTextGenerator implements IDocGenerator
{
  private Writer m_aPW;

  public XTextGenerator ()
  {}

  public void handleTokenProduction (final TokenProduction tp)
  {
    final StringBuilder sb = new StringBuilder ();

    for (final RegExprSpec res : tp.m_respecs)
    {
      final String regularExpressionText = JJDoc.emitRE (res.rexp);
      sb.append (regularExpressionText);

      if (res.nsTok != null)
      {
        sb.append (" : " + res.nsTok.image);
      }

      sb.append ("\n");
    }
  }

  private void println (final String s) throws IOException
  {
    print (s + "\n");
  }

  public void text (final String s) throws IOException
  {
    print (s);
  }

  public void print (final String s) throws IOException
  {
    m_aPW.write (s);
  }

  public void documentStart () throws IOException
  {
    m_aPW = TextGenerator.createPrintWriter ();
    println ("grammar " + JJDocGlobals.s_input_file + " with org.eclipse.xtext.common.Terminals");
    println ("import \"http://www.eclipse.org/emf/2002/Ecore\" as ecore");
    println ("");
  }

  public void documentEnd () throws IOException
  {
    m_aPW.close ();
  }

  /**
   * Prints out comments, used for tokens and non-terminals. {@inheritDoc}
   */

  public void specialTokens (final String s) throws IOException
  {
    print (s);
  }

  public void nonterminalsStart ()
  {}

  public void nonterminalsEnd ()
  {}

  public void tokensStart ()
  {}

  public void tokensEnd ()
  {}

  public void javacode (final CodeProductionJava jp)
  {}

  public void cppcode (final CodeProductionCpp cp)
  {}

  public void productionStart (final NormalProduction np)
  {}

  public void productionEnd (final NormalProduction np)
  {}

  public void expansionStart (final Expansion e, final boolean first)
  {}

  public void expansionEnd (final Expansion e, final boolean first) throws IOException
  {
    println (";");
  }

  public void nonTerminalStart (final ExpNonTerminal nt) throws IOException
  {
    print ("terminal ");
  }

  public void nonTerminalEnd (final ExpNonTerminal nt) throws IOException
  {
    print (";");
  }

  public void reStart (final AbstractExpRegularExpression r)
  {}

  public void reEnd (final AbstractExpRegularExpression r)
  {}
}
