package com.helger.pgcc.utils;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Test class for class {@link ConditionParser}.
 *
 * @author Philip Helger
 */
public final class ConditionParserTest
{
  @Test
  public void testBasic () throws ParseException
  {
    _test ("F", false);
    _test ("T", true);
    _test ("F || T", true);
    _test ("T || F", true);
    _test ("T || will not be compiled )", true);
    _test ("F && T", false);
    _test ("T && T", true);
    _test ("unknown", false);
  }

  private static void _test (final String input, final boolean expectedValue) throws ParseException
  {
    final ConditionParser cp = new ConditionParser (new StringReader (input));
    final Map <String, Object> values = new HashMap <> ();
    values.put ("F", Boolean.FALSE);
    values.put ("T", Boolean.TRUE);
    final boolean value = cp.CompilationUnit (values);
    assertEquals (expectedValue, value);
  }
}
