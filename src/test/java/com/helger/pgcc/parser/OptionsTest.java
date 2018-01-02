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
package com.helger.pgcc.parser;

import java.io.File;

import junit.framework.TestCase;

/**
 * Test cases to prod at the valitity of Options a little.
 *
 * @author Kees Jan Koster &lt;kjkoster@kjkoster.org&gt;
 */
public final class OptionsTest extends TestCase
{
  public void testDefaults ()
  {
    Options.init ();
    JavaCCErrors.reInit ();

    assertEquals (49, Options.s_optionValues.size ());

    assertEquals (true, Options.getBuildParser ());
    assertEquals (true, Options.getBuildTokenManager ());
    assertEquals (false, Options.getCacheTokens ());
    assertEquals (false, Options.getCommonTokenAction ());
    assertEquals (false, Options.getDebugLookahead ());
    assertEquals (false, Options.getDebugParser ());
    assertEquals (false, Options.getDebugTokenManager ());
    assertEquals (true, Options.getErrorReporting ());
    assertEquals (false, Options.getForceLaCheck ());
    assertEquals (false, Options.getIgnoreCase ());
    assertEquals (false, Options.getJavaUnicodeEscape ());
    assertEquals (true, Options.getKeepLineColumn ());
    assertEquals (true, Options.getSanityCheck ());
    assertEquals (true, Options.isStatic ());
    assertEquals (false, Options.getUnicodeInput ());
    assertEquals (false, Options.getUserCharStream ());
    assertEquals (false, Options.getUserTokenManager ());
    assertEquals (false, Options.getTokenManagerUsesParser ());

    assertEquals (2, Options.getChoiceAmbiguityCheck ());
    assertEquals (1, Options.getLookahead ());
    assertEquals (1, Options.getOtherAmbiguityCheck ());

    assertEquals ("1.5", Options.getJdkVersion ());
    assertEquals (new File ("."), Options.getOutputDirectory ());
    assertEquals ("", Options.getTokenExtends ());
    assertEquals ("", Options.getTokenFactory ());
    assertEquals (System.getProperties ().get ("file.encoding"), Options.getGrammarEncoding ());

    assertEquals (0, JavaCCErrors.getWarningCount ());
    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  public void testSetBooleanOption ()
  {
    Options.init ();
    JavaCCErrors.reInit ();

    assertEquals (true, Options.isStatic ());
    Options.setCmdLineOption ("-NOSTATIC");
    assertEquals (false, Options.isStatic ());

    assertEquals (false, Options.getJavaUnicodeEscape ());
    Options.setCmdLineOption ("-JAVA_UNICODE_ESCAPE:true");
    assertEquals (true, Options.getJavaUnicodeEscape ());

    assertEquals (true, Options.getSanityCheck ());
    Options.setCmdLineOption ("-SANITY_CHECK=false");
    assertEquals (false, Options.getSanityCheck ());

    assertEquals (0, JavaCCErrors.getWarningCount ());
    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  public void testIntBooleanOption ()
  {
    Options.init ();
    JavaCCErrors.reInit ();

    assertEquals (1, Options.getLookahead ());
    Options.setCmdLineOption ("LOOKAHEAD=2");
    assertEquals (2, Options.getLookahead ());
    assertEquals (0, JavaCCErrors.getWarningCount ());
    Options.setCmdLineOption ("LOOKAHEAD=0");
    assertEquals (2, Options.getLookahead ());
    assertEquals (0, JavaCCErrors.getWarningCount ());
    Options.setInputFileOption (null, null, Options.USEROPTION__LOOKAHEAD, Integer.valueOf (0));
    assertEquals (2, Options.getLookahead ());
    assertEquals (1, JavaCCErrors.getWarningCount ());

    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  public void testSetStringOption ()
  {
    Options.init ();
    JavaCCErrors.reInit ();

    assertEquals ("", Options.getTokenExtends ());
    Options.setCmdLineOption ("-TOKEN_EXTENDS=java.lang.Object");
    assertEquals ("java.lang.Object", Options.getTokenExtends ());
    Options.setInputFileOption (null, null, Options.USEROPTION__TOKEN_EXTENDS, "Object");
    // File option does not override cmd line
    assertEquals ("java.lang.Object", Options.getTokenExtends ());

    Options.init ();
    JavaCCErrors.reInit ();

    Options.setInputFileOption (null, null, Options.USEROPTION__TOKEN_EXTENDS, "Object");
    assertEquals ("Object", Options.getTokenExtends ());
    Options.setCmdLineOption ("-TOKEN_EXTENDS=java.lang.Object");
    assertEquals ("java.lang.Object", Options.getTokenExtends ());
  }

  public void testSetNonexistentOption ()
  {
    Options.init ();
    JavaCCErrors.reInit ();

    assertEquals (0, JavaCCErrors.getWarningCount ());
    Options.setInputFileOption (null, null, "NONEXISTENTOPTION", Boolean.TRUE);
    assertEquals (1, JavaCCErrors.getWarningCount ());

    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  public void testSetWrongTypeForOption ()
  {
    Options.init ();
    JavaCCErrors.reInit ();

    assertEquals (0, JavaCCErrors.getWarningCount ());
    assertEquals (0, JavaCCErrors.getErrorCount ());
    Options.setInputFileOption (null, null, Options.USEROPTION__STATIC, Integer.valueOf (8));
    assertEquals (1, JavaCCErrors.getWarningCount ());

    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  public void testNormalize ()
  {
    Options.init ();
    JavaCCErrors.reInit ();

    assertEquals (false, Options.getDebugLookahead ());
    assertEquals (false, Options.getDebugParser ());

    Options.setCmdLineOption ("-DEBUG_LOOKAHEAD=TRUE");
    Options.normalize ();

    assertEquals (true, Options.getDebugLookahead ());
    assertEquals (true, Options.getDebugParser ());

    assertEquals (0, JavaCCErrors.getWarningCount ());
    assertEquals (0, JavaCCErrors.getErrorCount ());
    assertEquals (0, JavaCCErrors.getParseErrorCount ());
    assertEquals (0, JavaCCErrors.getSemanticErrorCount ());
  }

  public void testOptionsString ()
  {
    Options.init ();
    JavaCCErrors.reInit ();

    Options.setCmdLineOption ("-STATIC=False");
    Options.setCmdLineOption ("-IGNORE_CASE=True");
    final String [] options = { Options.USEROPTION__STATIC, Options.USEROPTION__IGNORE_CASE };
    final String optionString = Options.getOptionsString (options);
    assertEquals ("STATIC=false,IGNORE_CASE=true", optionString);
  }
}
