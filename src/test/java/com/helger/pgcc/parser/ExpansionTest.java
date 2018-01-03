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
package com.helger.pgcc.parser;

import junit.framework.TestCase;

public final class ExpansionTest extends TestCase
{
  private Token t;
  private Expansion e;

  @Override
  public void setUp ()
  {
    t = new Token ();
    t.beginColumn = 2;
    t.beginLine = 3;
    e = new Expansion ();
    e.setColumn (5);
    e.setLine (6);
  }

  public void testZeroOrOneConstructor ()
  {
    final ExpZeroOrOne zoo = new ExpZeroOrOne (t, e);
    assertEquals (t.beginColumn, zoo.getColumn ());
    assertEquals (t.beginLine, zoo.getLine ());
    assertEquals (e, zoo.m_expansion);
    assertSame (e.m_parent, zoo);
  }

  public void testZeroOrMoreConstructor ()
  {
    final ExpZeroOrMore zom = new ExpZeroOrMore (t, e);
    assertEquals (t.beginColumn, zom.getColumn ());
    assertEquals (t.beginLine, zom.getLine ());
    assertEquals (e, zom.m_expansion);
    assertEquals (e.m_parent, zom);
  }

  public void testRZeroOrMoreConstructor ()
  {
    final AbstractExpRegularExpression r = new ExpRChoice ();
    final ExpRZeroOrMore rzom = new ExpRZeroOrMore (t, r);
    assertEquals (t.beginColumn, rzom.getColumn ());
    assertEquals (t.beginLine, rzom.getLine ());
    assertEquals (r, rzom.m_regexpr);
  }

  public void testROneOrMoreConstructor ()
  {
    final AbstractExpRegularExpression r = new ExpRChoice ();
    final ExpROneOrMore room = new ExpROneOrMore (t, r);
    assertEquals (t.beginColumn, room.getColumn ());
    assertEquals (t.beginLine, room.getLine ());
    assertEquals (r, room.m_regexpr);
  }

  public void testOneOrMoreConstructor ()
  {
    final Expansion rce = new ExpRChoice ();
    final ExpOneOrMore oom = new ExpOneOrMore (t, rce);
    assertEquals (t.beginColumn, oom.getColumn ());
    assertEquals (t.beginLine, oom.getLine ());
    assertEquals (rce, oom.m_expansion);
    assertEquals (rce.m_parent, oom);
  }

  public void testRStringLiteralConstructor ()
  {
    final ExpRStringLiteral r = new ExpRStringLiteral (t, "hey");
    assertEquals (t.beginColumn, r.getColumn ());
    assertEquals (t.beginLine, r.getLine ());
    assertEquals ("hey", r.m_image);
  }

  public void testChoiceConstructor ()
  {
    ExpChoice c = new ExpChoice (t);
    assertEquals (t.beginColumn, c.getColumn ());
    assertEquals (t.beginLine, c.getLine ());
    c = new ExpChoice (e);
    assertEquals (e.getColumn (), c.getColumn ());
    assertEquals (e.getLine (), c.getLine ());
    assertEquals (e, c.getChoices ().get (0));
  }

  public void testRJustNameConstructor ()
  {
    final ExpRJustName r = new ExpRJustName (t, "hey");
    assertEquals (t.beginColumn, r.getColumn ());
    assertEquals (t.beginLine, r.getLine ());
    assertEquals ("hey", r.m_label);
  }

  public void testSequenceConstructor ()
  {
    final ExpLookahead la = new ExpLookahead ();
    final ExpSequence s = new ExpSequence (t, la);
    assertEquals (t.beginColumn, s.getColumn ());
    assertEquals (t.beginLine, s.getLine ());
    assertSame (la, s.m_units.get (0));
  }
}
