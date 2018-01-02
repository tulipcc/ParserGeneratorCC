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
import java.util.Iterator;
import java.util.List;

import com.helger.commons.ValueEnforcer;

/**
 * Describes regular expressions which are sequences of other regular
 * expressions.
 */

public class RSequence extends RegularExpression
{

  /**
   * The list of units in this regular expression sequence. Each list component
   * will narrow to RegularExpression.
   */
  final List <RegularExpression> m_units;

  RSequence ()
  {
    m_units = new ArrayList <> ();
  }

  RSequence (final List <RegularExpression> seq)
  {
    m_ordinal = Integer.MAX_VALUE;
    m_units = seq;
  }

  public void addUnit (final RegularExpression ex)
  {
    ValueEnforcer.notNull (ex, "RegEx");
    m_units.add (ex);
  }

  public Iterator <RegularExpression> iterator ()
  {
    return m_units.iterator ();
  }

  @Override
  public Nfa GenerateNfa (final boolean ignoreCase)
  {
    if (m_units.size () == 1)
      return m_units.get (0).GenerateNfa (ignoreCase);

    final Nfa retVal = new Nfa ();
    final NfaState startState = retVal.start;
    final NfaState finalState = retVal.end;
    Nfa temp1;
    Nfa temp2 = null;

    RegularExpression curRE;

    curRE = m_units.get (0);
    temp1 = curRE.GenerateNfa (ignoreCase);
    startState.addMove (temp1.start);

    for (int i = 1; i < m_units.size (); i++)
    {
      curRE = m_units.get (i);

      temp2 = curRE.GenerateNfa (ignoreCase);
      temp1.end.addMove (temp2.start);
      temp1 = temp2;
    }

    temp2.end.addMove (finalState);

    return retVal;
  }
}
