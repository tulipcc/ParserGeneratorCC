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

import java.util.ArrayList;
import java.util.List;

/**
 * Describes regular expressions which are choices from from among included
 * regular expressions.
 */

public class ExpRChoice extends AbstractExpRegularExpression
{
  /**
   * The list of choices of this regular expression. Each list component will
   * narrow to RegularExpression.
   */
  private List <AbstractExpRegularExpression> m_choices = new ArrayList <> ();

  /**
   * @param choices
   *        the choices to set
   */
  public void setChoices (final List <AbstractExpRegularExpression> choices)
  {
    this.m_choices = choices;
  }

  /**
   * @return the choices
   */
  public List <AbstractExpRegularExpression> getChoices ()
  {
    return m_choices;
  }

  @Override
  public Nfa generateNfa (final boolean ignoreCase)
  {
    CompressCharLists ();

    if (getChoices ().size () == 1)
      return getChoices ().get (0).generateNfa (ignoreCase);

    final Nfa retVal = new Nfa ();
    final NfaState startState = retVal.start;
    final NfaState finalState = retVal.end;

    for (int i = 0; i < getChoices ().size (); i++)
    {
      Nfa temp;
      final AbstractExpRegularExpression curRE = getChoices ().get (i);

      temp = curRE.generateNfa (ignoreCase);

      startState.addMove (temp.start);
      temp.end.addMove (finalState);
    }

    return retVal;
  }

  void CompressCharLists ()
  {
    compressChoices (); // Unroll nested choices
    AbstractExpRegularExpression curRE;
    ExpRCharacterList curCharList = null;

    for (int i = 0; i < getChoices ().size (); i++)
    {
      curRE = getChoices ().get (i);

      while (curRE instanceof ExpRJustName)
        curRE = ((ExpRJustName) curRE).m_regexpr;

      if (curRE instanceof ExpRStringLiteral && ((ExpRStringLiteral) curRE).m_image.length () == 1)
      {
        curRE = new ExpRCharacterList (((ExpRStringLiteral) curRE).m_image.charAt (0));
        getChoices ().set (i, curRE);
      }

      if (curRE instanceof ExpRCharacterList)
      {
        if (((ExpRCharacterList) curRE).m_negated_list)
          ((ExpRCharacterList) curRE).removeNegation ();

        final List <ICCCharacter> tmp = ((ExpRCharacterList) curRE).m_descriptors;

        if (curCharList == null)
        {
          curCharList = new ExpRCharacterList ();
          curRE = curCharList;
          getChoices ().set (i, curRE);
        }
        else
          getChoices ().remove (i--);

        for (int j = tmp.size (); j-- > 0;)
          curCharList.m_descriptors.add (tmp.get (j));
      }

    }
  }

  void compressChoices ()
  {
    AbstractExpRegularExpression curRE;

    for (int i = 0; i < getChoices ().size (); i++)
    {
      curRE = getChoices ().get (i);

      while (curRE instanceof ExpRJustName)
        curRE = ((ExpRJustName) curRE).m_regexpr;

      if (curRE instanceof ExpRChoice)
      {
        getChoices ().remove (i--);
        for (int j = ((ExpRChoice) curRE).getChoices ().size (); j-- > 0;)
          getChoices ().add (((ExpRChoice) curRE).getChoices ().get (j));
      }
    }
  }

  public int checkUnmatchability ()
  {
    AbstractExpRegularExpression curRE;
    int numStrings = 0;

    for (int i = 0; i < getChoices ().size (); i++)
    {
      if (!(curRE = getChoices ().get (i)).m_private_rexp &&
          // curRE instanceof RJustName &&
          curRE.m_ordinal > 0 &&
          curRE.m_ordinal < m_ordinal &&
          LexGenJava.s_lexStates[curRE.m_ordinal] == LexGenJava.s_lexStates[m_ordinal])
      {
        if (m_label != null)
          JavaCCErrors.warning (this,
                                "Regular Expression choice : " +
                                      curRE.m_label +
                                      " can never be matched as : " +
                                      m_label);
        else
          JavaCCErrors.warning (this,
                                "Regular Expression choice : " +
                                      curRE.m_label +
                                      " can never be matched as token of kind : " +
                                      m_ordinal);
      }

      if (!curRE.m_private_rexp && curRE instanceof ExpRStringLiteral)
        numStrings++;
    }
    return numStrings;
  }
}
