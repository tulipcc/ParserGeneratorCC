package com.helger.pgcc.parser.exp;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.helger.pgcc.parser.Token;

public final class ExpChoiceTest
{
  private Token m_aToken;
  private Expansion m_aExp;

  @Before
  public void setUp ()
  {
    m_aToken = new Token ();
    m_aToken.beginColumn = 2;
    m_aToken.beginLine = 3;
    m_aExp = new Expansion ();
    m_aExp.setColumn (5);
    m_aExp.setLine (6);
  }

  @Test
  public void testChoiceConstructor ()
  {
    ExpChoice c = new ExpChoice (m_aToken);
    assertEquals (m_aToken.beginColumn, c.getColumn ());
    assertEquals (m_aToken.beginLine, c.getLine ());
    assertEquals (0, c.getChoiceCount ());
    c = new ExpChoice (m_aExp);
    assertEquals (m_aExp.getColumn (), c.getColumn ());
    assertEquals (m_aExp.getLine (), c.getLine ());
    assertEquals (m_aExp, c.getChoiceAt (0));
  }
}
