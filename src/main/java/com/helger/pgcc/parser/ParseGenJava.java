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

import static com.helger.pgcc.parser.JavaCCGlobals.getFileExtension;
import static com.helger.pgcc.parser.JavaCCGlobals.getIdString;
import static com.helger.pgcc.parser.JavaCCGlobals.javaStaticOpt;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_from_insertion_point_2;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_name;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_to_insertion_point_1;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_to_insertion_point_2;
import static com.helger.pgcc.parser.JavaCCGlobals.s_jj2index;
import static com.helger.pgcc.parser.JavaCCGlobals.s_jjtreeGenerated;
import static com.helger.pgcc.parser.JavaCCGlobals.s_lookaheadNeeded;
import static com.helger.pgcc.parser.JavaCCGlobals.s_maskVals;
import static com.helger.pgcc.parser.JavaCCGlobals.s_maskindex;
import static com.helger.pgcc.parser.JavaCCGlobals.s_tokenCount;
import static com.helger.pgcc.parser.JavaCCGlobals.s_toolName;
import static com.helger.pgcc.parser.JavaCCGlobals.s_toolNames;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.helger.pgcc.EJDKVersion;
import com.helger.pgcc.output.EOutputLanguage;

/**
 * Generate the parser.
 */
public class ParseGenJava extends CodeGenerator
{
  public void start (final boolean isJavaModernMode) throws MetaParseException
  {
    if (JavaCCErrors.getErrorCount () != 0)
      throw new MetaParseException ();

    if (!Options.isBuildParser ())
      return;

    final EOutputLanguage eOutputLanguage = getOutputLanguage ();
    final EJDKVersion eJavaVersion = Options.getJdkVersion ();
    final boolean bExceptionWithCause = eJavaVersion.isNewerOrEqualsThan (EJDKVersion.JDK_14);
    final boolean bGenerateAnnotations = eJavaVersion.isNewerOrEqualsThan (EJDKVersion.JDK_15);
    final boolean bGenerateGenerics = eJavaVersion.isNewerOrEqualsThan (EJDKVersion.JDK_15);
    final boolean bEmptyTypeVar = eJavaVersion.isNewerOrEqualsThan (EJDKVersion.JDK_17);

    final List <String> tn = new ArrayList <> (s_toolNames);
    tn.add (s_toolName);

    // This is the first line generated -- the the comment line at the top of
    // the generated parser
    genCodeLine ("/* " + getIdString (tn, s_cu_name + ".java") + " */");

    boolean implementsExists = false;

    if (s_cu_to_insertion_point_1.size () != 0)
    {
      final Token firstToken = s_cu_to_insertion_point_1.get (0);
      printTokenSetup (firstToken);
      m_ccol = 1;
      Token t;
      for (final Token aToken : s_cu_to_insertion_point_1)
      {
        t = aToken;
        if (t.kind == JavaCCParserConstants.IMPLEMENTS)
        {
          implementsExists = true;
        }
        else
          if (t.kind == JavaCCParserConstants.CLASS)
          {
            implementsExists = false;
          }

        printToken (t);
      }
    }

    if (implementsExists)
    {
      genCode (", ");
    }
    else
    {
      genCode (" implements ");
    }
    genCode (s_cu_name + "Constants ");
    if (s_cu_to_insertion_point_2.size () != 0)
    {
      printTokenSetup (s_cu_to_insertion_point_2.get (0));
      for (final Token aToken : s_cu_to_insertion_point_2)
      {
        printToken (aToken);
      }
    }

    genCodeLine ();
    genCodeLine ();

    new ParseEngine ().build (this);

    if (Options.isStatic ())
    {
      genCodeLine ("  private static " + eOutputLanguage.getTypeBoolean () + " jj_initialized_once = false;");
    }
    if (Options.isUserTokenManager ())
    {
      genCodeLine ("  /** User defined Token Manager. */");
      genCodeLine ("  " + javaStaticOpt () + "public TokenManager token_source;");
    }
    else
    {
      genCodeLine ("  /** Generated Token Manager. */");
      genCodeLine ("  " + javaStaticOpt () + "public " + s_cu_name + "TokenManager token_source;");
      if (!Options.isJavaUserCharStream ())
      {
        if (Options.isJavaUnicodeEscape ())
        {
          genCodeLine ("  " + javaStaticOpt () + "JavaCharStream jj_input_stream;");
        }
        else
        {
          genCodeLine ("  " + javaStaticOpt () + "SimpleCharStream jj_input_stream;");
        }
      }
    }
    genCodeLine ("  /** Current token. */");
    genCodeLine ("  " + javaStaticOpt () + "public Token token;");
    genCodeLine ("  /** Next token. */");
    genCodeLine ("  " + javaStaticOpt () + "public Token jj_nt;");
    if (!Options.isCacheTokens ())
    {
      genCodeLine ("  " + javaStaticOpt () + "private int jj_ntk;");
    }
    if (Options.hasDepthLimit ())
    {
      genCodeLine ("  /** current depth */");
      genCodeLine ("  " + javaStaticOpt () + "private int jj_depth;");
    }
    if (s_jj2index != 0)
    {
      genCodeLine ("  " + javaStaticOpt () + "private Token jj_scanpos, jj_lastpos;");
      genCodeLine ("  " + javaStaticOpt () + "private int jj_la;");
      if (s_lookaheadNeeded)
      {
        genCodeLine ("  /** Whether we are looking ahead. */");
        genCodeLine ("  " +
                     javaStaticOpt () +
                     "private " +
                     eOutputLanguage.getTypeBoolean () +
                     " jj_lookingAhead = false;");
        genCodeLine ("  " + javaStaticOpt () + "private " + eOutputLanguage.getTypeBoolean () + " jj_semLA;");
      }
    }
    if (Options.isErrorReporting ())
    {
      genCodeLine ("  " + javaStaticOpt () + "private int jj_gen;");
      genCodeLine ("  " + javaStaticOpt () + "final private int[] jj_la1 = new int[" + s_maskindex + "];");
      final int tokenMaskSize = (s_tokenCount - 1) / 32 + 1;
      for (int i = 0; i < tokenMaskSize; i++)
      {
        genCodeLine ("  static private int[] jj_la1_" + i + ";");
      }
      genCodeLine ("  static {");
      for (int i = 0; i < tokenMaskSize; i++)
      {
        genCodeLine ("	   jj_la1_init_" + i + "();");
      }
      genCodeLine ("	}");
      for (int i = 0; i < tokenMaskSize; i++)
      {
        genCodeLine ("	private static void jj_la1_init_" + i + "() {");
        genCode ("	   jj_la1_" + i + " = new int[] {");
        for (final int [] tokenMask : s_maskVals)
          genCode ("0x" + Integer.toHexString (tokenMask[i]) + ",");
        genCodeLine ("};");
        genCodeLine ("	}");
      }
    }
    if (s_jj2index != 0 && Options.isErrorReporting ())
    {
      genCodeLine ("  " + javaStaticOpt () + "private final JJCalls[] jj_2_rtns = new JJCalls[" + s_jj2index + "];");
      genCodeLine ("  " + javaStaticOpt () + "private " + eOutputLanguage.getTypeBoolean () + " jj_rescan = false;");
      genCodeLine ("  " + javaStaticOpt () + "private int jj_gc = 0;");
    }
    genCodeLine ();

    if (!Options.isUserTokenManager ())
    {
      if (Options.isJavaUserCharStream ())
      {
        genCodeLine ("  /** Constructor with user supplied CharStream. */");
        genCodeLine ("  public " + s_cu_name + "(final CharStream stream) {");
        if (Options.isStatic ())
        {
          genCodeLine ("	 if (jj_initialized_once) {");
          genCodeLine ("	   System.out.println(\"ERROR: Second call to constructor of static parser.  \");");
          genCodeLine ("	   System.out.println(\"	   You must either use ReInit() " +
                       "or set the JavaCC option STATIC to false\");");
          genCodeLine ("	   System.out.println(\"	   during parser generation.\");");
          genCodeLine ("	   throw new IllegalStateException();");
          genCodeLine ("	 }");
          genCodeLine ("	 jj_initialized_once = true;");
        }
        if (Options.isTokenManagerUsesParser ())
        {
          genCodeLine ("	 token_source = new " + s_cu_name + "TokenManager(this, stream);");
        }
        else
        {
          genCodeLine ("	 token_source = new " + s_cu_name + "TokenManager(stream);");
        }
        genCodeLine ("	 token = new Token();");
        if (Options.isCacheTokens ())
        {
          genCodeLine ("   jj_nt = token_source.getNextToken();");
          genCodeLine ("   token.next = jj_nt;");
        }
        else
        {
          genCodeLine ("	 jj_ntk = -1;");
        }
        if (Options.hasDepthLimit ())
        {
          genCodeLine ("    jj_depth = -1;");
        }
        if (Options.isErrorReporting ())
        {
          genCodeLine ("	 jj_gen = 0;");
          if (s_maskindex > 0)
          {
            genCodeLine ("	 for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
          }
          if (s_jj2index != 0)
          {
            genCodeLine ("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
          }
        }
        genCodeLine ("  }");
        genCodeLine ();
        genCodeLine ("  /** Reinitialise. */");
        genCodeLine ("  " + javaStaticOpt () + "public void ReInit(CharStream stream) {");

        if (Options.isTokenManagerRequiresParserAccess ())
        {
          genCodeLine ("	 token_source.ReInit(this,stream);");
        }
        else
        {
          genCodeLine ("	 token_source.ReInit(stream);");
        }

        genCodeLine ("	 token = new Token();");
        if (Options.isCacheTokens ())
        {
          genCodeLine ("   jj_nt = token_source.getNextToken();");
          genCodeLine ("   token.next = jj_nt;");
        }
        else
        {
          genCodeLine ("	 jj_ntk = -1;");
        }
        if (Options.hasDepthLimit ())
        {
          genCodeLine ("    jj_depth = -1;");
        }
        if (s_lookaheadNeeded)
        {
          genCodeLine ("	 jj_lookingAhead = false;");
        }
        if (s_jjtreeGenerated)
        {
          genCodeLine ("	 jjtree.reset();");
        }
        if (Options.isErrorReporting ())
        {
          genCodeLine ("	 jj_gen = 0;");
          if (s_maskindex > 0)
          {
            genCodeLine ("	 for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
          }
          if (s_jj2index != 0)
          {
            genCodeLine ("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
          }
        }
        genCodeLine ("  }");
      }
      else
      {
        if (!isJavaModernMode)
        {
          genCodeLine ("  /** Constructor with InputStream. */");
          genCodeLine ("  public " + s_cu_name + "(final java.io.InputStream stream) {");
          genCodeLine ("	  this(stream, null);");
          genCodeLine ("  }");
          genCodeLine ();
          genCodeLine ("  /** Constructor with InputStream and supplied encoding */");
          genCodeLine ("  public " + s_cu_name + "(final java.io.InputStream stream, final String encoding) {");
          if (Options.isStatic ())
          {
            genCodeLine ("	 if (jj_initialized_once) {");
            genCodeLine ("	   System.out.println(\"ERROR: Second call to constructor of static parser.  \");");
            genCodeLine ("	   System.out.println(\"	   You must either use ReInit() or " +
                         "set the JavaCC option STATIC to false\");");
            genCodeLine ("	   System.out.println(\"	   during parser generation.\");");
            genCodeLine ("	   throw new IllegalStateException ();");
            genCodeLine ("	 }");
            genCodeLine ("	 jj_initialized_once = true;");
          }

          final String sInitIS = "   try { jj_input_stream = new " +
                                 (Options.isJavaUnicodeEscape () ? "JavaCharStream" : "SimpleCharStream") +
                                 "(stream, encoding, 1, 1); }" +
                                 " catch(final java.io.UnsupportedEncodingException e) {" +
                                 " throw new IllegalStateException(" +
                                 (bExceptionWithCause ? "e" : "e.getMessage()") +
                                 "); }";
          genCodeLine (sInitIS);
          if (Options.isTokenManagerUsesParser () && !Options.isStatic ())
          {
            genCodeLine ("	 token_source = new " + s_cu_name + "TokenManager(this, jj_input_stream);");
          }
          else
          {
            genCodeLine ("	 token_source = new " + s_cu_name + "TokenManager(jj_input_stream);");
          }
          genCodeLine ("	 token = new Token();");
          if (Options.isCacheTokens ())
          {
            genCodeLine ("   jj_nt = token_source.getNextToken();");
            genCodeLine ("   token.next = jj_nt;");
          }
          else
          {
            genCodeLine ("	 jj_ntk = -1;");
          }
          if (Options.hasDepthLimit ())
          {
            genCodeLine ("    jj_depth = -1;");
          }
          if (Options.isErrorReporting ())
          {
            genCodeLine ("	 jj_gen = 0;");
            if (s_maskindex > 0)
            {
              genCodeLine ("	 for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
            }
            if (s_jj2index != 0)
            {
              genCodeLine ("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
            }
          }
          genCodeLine ("  }");
          genCodeLine ();

          genCodeLine ("  /** Reinitialise. */");
          genCodeLine ("  " + javaStaticOpt () + "public void ReInit(final java.io.InputStream stream) {");
          genCodeLine ("	  ReInit(stream, null);");
          genCodeLine ("  }");

          genCodeLine ("  /** Reinitialise. */");
          genCodeLine ("  " +
                       javaStaticOpt () +
                       "public void ReInit(final java.io.InputStream stream, final String encoding) {");
          genCodeLine ("	 try { jj_input_stream.ReInit(stream, encoding, 1, 1); } " +
                       "catch(final java.io.UnsupportedEncodingException e) { " +
                       "throw new IllegalStateException(" +
                       (bExceptionWithCause ? "e" : "e.getMessage()") +
                       "); }");

          if (Options.isTokenManagerRequiresParserAccess ())
          {
            genCodeLine ("	 token_source.ReInit(this,jj_input_stream);");
          }
          else
          {
            genCodeLine ("	 token_source.ReInit(jj_input_stream);");
          }

          genCodeLine ("	 token = new Token();");
          if (Options.isCacheTokens ())
          {
            genCodeLine ("   jj_nt = token_source.getNextToken();");
            genCodeLine ("   token.next = jj_nt;");
          }
          else
          {
            genCodeLine ("	 jj_ntk = -1;");
          }
          if (Options.hasDepthLimit ())
          {
            genCodeLine ("    jj_depth = -1;");
          }
          if (s_jjtreeGenerated)
          {
            genCodeLine ("	 jjtree.reset();");
          }
          if (Options.isErrorReporting ())
          {
            genCodeLine ("	 jj_gen = 0;");
            genCodeLine ("	 for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
            if (s_jj2index != 0)
            {
              genCodeLine ("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
            }
          }
          genCodeLine ("  }");
          genCodeLine ();

        }

        final String readerInterfaceName = isJavaModernMode ? "Provider" : "java.io.Reader";
        final String stringReaderClass = isJavaModernMode ? "StringProvider" : "java.io.StringReader";

        genCodeLine ("  /** Constructor. */");
        genCodeLine ("  public " + s_cu_name + "(" + readerInterfaceName + " stream) {");
        if (Options.isStatic ())
        {
          genCodeLine ("	 if (jj_initialized_once) {");
          genCodeLine ("	   System.out.println(\"ERROR: Second call to constructor of static parser. \");");
          genCodeLine ("	   System.out.println(\"	   You must either use ReInit() or " +
                       "set the JavaCC option STATIC to false\");");
          genCodeLine ("	   System.out.println(\"	   during parser generation.\");");
          genCodeLine ("	   throw new IllegalStateException ();");
          genCodeLine ("	 }");
          genCodeLine ("	 jj_initialized_once = true;");
        }
        if (Options.isJavaUnicodeEscape ())
        {
          genCodeLine ("	 jj_input_stream = new JavaCharStream(stream, 1, 1);");
        }
        else
        {
          genCodeLine ("	 jj_input_stream = new SimpleCharStream(stream, 1, 1);");
        }
        if (Options.isTokenManagerUsesParser () && !Options.isStatic ())
        {
          genCodeLine ("	 token_source = new " + s_cu_name + "TokenManager(this, jj_input_stream);");
        }
        else
        {
          genCodeLine ("	 token_source = new " + s_cu_name + "TokenManager(jj_input_stream);");
        }
        genCodeLine ("	 token = new Token();");
        if (Options.isCacheTokens ())
        {
          genCodeLine ("	 token.next = jj_nt = token_source.getNextToken();");
        }
        else
        {
          genCodeLine ("	 jj_ntk = -1;");
        }
        if (Options.hasDepthLimit ())
        {
          genCodeLine ("    jj_depth = -1;");
        }
        if (Options.isErrorReporting ())
        {
          genCodeLine ("	 jj_gen = 0;");
          if (s_maskindex > 0)
          {
            genCodeLine ("	 for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
          }
          if (s_jj2index != 0)
          {
            genCodeLine ("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
          }
        }
        genCodeLine ("  }");
        genCodeLine ();

        // Add-in a string based constructor because its convenient (modern
        // only to prevent regressions)
        if (isJavaModernMode)
        {
          genCodeLine ("  /** Constructor. */");
          genCodeLine ("  public " +
                       s_cu_name +
                       "(String dsl) throws ParseException, " +
                       Options.getTokenMgrErrorClass () +
                       " {");
          genCodeLine ("	   this(new " + stringReaderClass + "(dsl));");
          genCodeLine ("  }");
          genCodeLine ();

          genCodeLine ("  public void ReInit(String s) {");
          genCodeLine ("	  ReInit(new " + stringReaderClass + "(s));");
          genCodeLine ("  }");

        }

        genCodeLine ("  /** Reinitialise. */");
        genCodeLine ("  " + javaStaticOpt () + "public void ReInit(" + readerInterfaceName + " stream) {");
        if (Options.isJavaUnicodeEscape ())
        {
          genCodeLine ("	if (jj_input_stream == null) {");
          genCodeLine ("	   jj_input_stream = new JavaCharStream(stream, 1, 1);");
          genCodeLine ("	} else {");
          genCodeLine ("	   jj_input_stream.ReInit(stream, 1, 1);");
          genCodeLine ("	}");
        }
        else
        {
          genCodeLine ("	if (jj_input_stream == null) {");
          genCodeLine ("	   jj_input_stream = new SimpleCharStream(stream, 1, 1);");
          genCodeLine ("	} else {");
          genCodeLine ("	   jj_input_stream.ReInit(stream, 1, 1);");
          genCodeLine ("	}");
        }

        genCodeLine ("	if (token_source == null) {");

        if (Options.isTokenManagerUsesParser () && !Options.isStatic ())
        {
          genCodeLine (" token_source = new " + s_cu_name + "TokenManager(this, jj_input_stream);");
        }
        else
        {
          genCodeLine (" token_source = new " + s_cu_name + "TokenManager(jj_input_stream);");
        }

        genCodeLine ("	}");
        genCodeLine ();

        if (Options.isTokenManagerRequiresParserAccess ())
        {
          genCodeLine ("	 token_source.ReInit(this,jj_input_stream);");
        }
        else
        {
          genCodeLine ("	 token_source.ReInit(jj_input_stream);");
        }

        genCodeLine ("	 token = new Token();");
        if (Options.isCacheTokens ())
        {
          genCodeLine ("	 token.next = jj_nt = token_source.getNextToken();");
        }
        else
        {
          genCodeLine ("	 jj_ntk = -1;");
        }
        if (Options.hasDepthLimit ())
        {
          genCodeLine ("    jj_depth = -1;");
        }
        if (s_jjtreeGenerated)
        {
          genCodeLine ("	 jjtree.reset();");
        }
        if (Options.isErrorReporting ())
        {
          genCodeLine ("	 jj_gen = 0;");
          if (s_maskindex > 0)
          {
            genCodeLine ("	 for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
          }
          if (s_jj2index != 0)
          {
            genCodeLine ("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
          }
        }
        genCodeLine ("  }");

      }
    }
    genCodeLine ();
    if (Options.isUserTokenManager ())
    {
      genCodeLine ("  /** Constructor with user supplied Token Manager. */");
      genCodeLine ("  public " + s_cu_name + "(TokenManager tm) {");
    }
    else
    {
      genCodeLine ("  /** Constructor with generated Token Manager. */");
      genCodeLine ("  public " + s_cu_name + "(" + s_cu_name + "TokenManager tm) {");
    }
    if (Options.isStatic ())
    {
      genCodeLine ("	 if (jj_initialized_once) {");
      genCodeLine ("	   System.out.println(\"ERROR: Second call to constructor of static parser. \");");
      genCodeLine ("	   System.out.println(\"	   You must either use ReInit() or " +
                   "set the JavaCC option STATIC to false\");");
      genCodeLine ("	   System.out.println(\"	   during parser generation.\");");
      genCodeLine ("	   throw new IllegalStateException ();");
      genCodeLine ("	 }");
      genCodeLine ("	 jj_initialized_once = true;");
    }
    genCodeLine ("	 token_source = tm;");
    genCodeLine ("	 token = new Token();");
    if (Options.isCacheTokens ())
    {
      genCodeLine ("	 token.next = jj_nt = token_source.getNextToken();");
    }
    else
    {
      genCodeLine ("	 jj_ntk = -1;");
    }
    if (Options.hasDepthLimit ())
    {
      genCodeLine ("    jj_depth = -1;");
    }
    if (Options.isErrorReporting ())
    {
      genCodeLine ("	 jj_gen = 0;");
      if (s_maskindex > 0)
      {
        genCodeLine ("	 for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
      }
      if (s_jj2index != 0)
      {
        genCodeLine ("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
      }
    }
    genCodeLine ("  }");
    genCodeLine ();
    if (Options.isUserTokenManager ())
    {
      genCodeLine ("  /** Reinitialise. */");
      genCodeLine ("  public void ReInit(TokenManager tm) {");
    }
    else
    {
      genCodeLine ("  /** Reinitialise. */");
      genCodeLine ("  public void ReInit(" + s_cu_name + "TokenManager tm) {");
    }
    genCodeLine ("	 token_source = tm;");
    genCodeLine ("	 token = new Token();");
    if (Options.isCacheTokens ())
    {
      genCodeLine ("	 token.next = jj_nt = token_source.getNextToken();");
    }
    else
    {
      genCodeLine ("	 jj_ntk = -1;");
    }
    if (Options.hasDepthLimit ())
    {
      genCodeLine ("    jj_depth = -1;");
    }
    if (s_jjtreeGenerated)
    {
      genCodeLine ("	 jjtree.reset();");
    }
    if (Options.isErrorReporting ())
    {
      genCodeLine ("	 jj_gen = 0;");
      if (s_maskindex > 0)
      {
        genCodeLine ("	 for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
      }
      if (s_jj2index != 0)
      {
        genCodeLine ("	 for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
      }
    }
    genCodeLine ("  }");
    genCodeLine ();
    genCodeLine ("  " + javaStaticOpt () + "private Token jj_consume_token(final int kind) throws ParseException {");
    genCodeLine ("    final Token oldToken = token;");
    if (Options.isCacheTokens ())
    {
      genCodeLine ("    token = jj_nt;");
      genCodeLine ("    if (token.next != null)");
      genCodeLine ("      jj_nt = jj_nt.next;");
      genCodeLine ("    else {");
      genCodeLine ("      jj_nt.next = token_source.getNextToken();");
      genCodeLine ("      jj_nt = jj_nt.next;");
      genCodeLine ("    }");
    }
    else
    {
      genCodeLine ("    if (token.next != null)");
      genCodeLine ("      token = token.next;");
      genCodeLine ("    else {");
      genCodeLine ("      token.next = token_source.getNextToken();");
      genCodeLine ("      token = token.next;");
      genCodeLine ("    }");
      genCodeLine ("    jj_ntk = -1;");
    }
    genCodeLine ("    if (token.kind == kind) {");
    if (Options.isErrorReporting ())
    {
      genCodeLine ("      jj_gen++;");
      if (s_jj2index != 0)
      {
        genCodeLine ("      if (++jj_gc > 100) {");
        genCodeLine ("        jj_gc = 0;");
        genCodeLine ("        for (int i = 0; i < jj_2_rtns.length; i++) {");
        genCodeLine ("          JJCalls c = jj_2_rtns[i];");
        genCodeLine ("          while (c != null) {");
        genCodeLine ("            if (c.gen < jj_gen)");
        genCodeLine ("              c.first = null;");
        genCodeLine ("            c = c.next;");
        genCodeLine ("          }");
        genCodeLine ("        }");
        genCodeLine ("      }");
      }
    }
    if (Options.isDebugParser ())
    {
      genCodeLine ("      trace_token(token, \"\");");
    }
    genCodeLine ("      return token;");
    genCodeLine ("    }");
    if (Options.isCacheTokens ())
    {
      genCodeLine ("    jj_nt = token;");
    }
    genCodeLine ("    token = oldToken;");
    if (Options.isErrorReporting ())
    {
      genCodeLine ("    jj_kind = kind;");
    }
    genCodeLine ("    throw generateParseException();");
    genCodeLine ("  }");
    genCodeLine ();
    if (s_jj2index != 0)
    {
      if (bGenerateAnnotations)
        genCodeLine ("  @SuppressWarnings(\"serial\")");
      genCodeLine ("  private static final class LookaheadSuccess extends IllegalStateException { }");
      genCodeLine ("  " + javaStaticOpt () + "private final LookaheadSuccess jj_ls = new LookaheadSuccess();");
      genCodeLine ("  " +
                   javaStaticOpt () +
                   "private " +
                   eOutputLanguage.getTypeBoolean () +
                   " jj_scan_token(int kind) {");
      genCodeLine ("	 if (jj_scanpos == jj_lastpos) {");
      genCodeLine ("	   jj_la--;");
      genCodeLine ("	   if (jj_scanpos.next == null) {");
      genCodeLine ("		   jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();");
      genCodeLine ("	   } else {");
      genCodeLine ("		   jj_lastpos = jj_scanpos = jj_scanpos.next;");
      genCodeLine ("	   }");
      genCodeLine ("	 } else {");
      genCodeLine ("	   jj_scanpos = jj_scanpos.next;");
      genCodeLine ("	 }");
      if (Options.isErrorReporting ())
      {
        genCodeLine ("	 if (jj_rescan) {");
        genCodeLine ("	   int i = 0; Token tok = token;");
        genCodeLine ("	   while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }");
        genCodeLine ("	   if (tok != null) jj_add_error_token(kind, i);");
        if (Options.isDebugLookahead ())
        {
          genCodeLine ("	 } else {");
          genCodeLine ("	   trace_scan(jj_scanpos, kind);");
        }
        genCodeLine ("	 }");
      }
      else
        if (Options.isDebugLookahead ())
        {
          genCodeLine ("	 trace_scan(jj_scanpos, kind);");
        }
      genCodeLine ("	 if (jj_scanpos.kind != kind) return true;");
      genCodeLine ("	 if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;");
      genCodeLine ("	 return false;");
      genCodeLine ("  }");
      genCodeLine ();
    }
    genCodeLine ();
    genCodeLine ("  /** Get the next Token. */");
    genCodeLine ("  " + javaStaticOpt () + "public final Token getNextToken() {");
    if (Options.isCacheTokens ())
    {
      genCodeLine ("	 if ((token = jj_nt).next != null) jj_nt = jj_nt.next;");
      genCodeLine ("	 else jj_nt = jj_nt.next = token_source.getNextToken();");
    }
    else
    {
      genCodeLine ("	 if (token.next != null) token = token.next;");
      genCodeLine ("	 else token = token.next = token_source.getNextToken();");
      genCodeLine ("	 jj_ntk = -1;");
    }
    if (Options.isErrorReporting ())
    {
      genCodeLine ("	 jj_gen++;");
    }
    if (Options.isDebugParser ())
    {
      genCodeLine ("	   trace_token(token, \" (in getNextToken)\");");
    }
    genCodeLine ("	 return token;");
    genCodeLine ("  }");
    genCodeLine ();
    genCodeLine ("/** Get the specific Token. */");
    genCodeLine ("  " + javaStaticOpt () + "public final Token getToken(int index) {");
    if (s_lookaheadNeeded)
      genCodeLine ("    Token t = jj_lookingAhead ? jj_scanpos : token;");
    else
      genCodeLine ("    Token t = token;");
    genCodeLine ("    for (int i = 0; i < index; i++) {");
    genCodeLine ("      if (t.next == null)");
    genCodeLine ("        t.next = token_source.getNextToken();");
    genCodeLine ("      t = t.next;");
    genCodeLine ("    }");
    genCodeLine ("    return t;");
    genCodeLine ("  }");
    genCodeLine ();
    if (!Options.isCacheTokens ())
    {
      genCodeLine ("  " + javaStaticOpt () + "private int jj_ntk_f() {");
      genCodeLine ("    jj_nt = token.next;");
      genCodeLine ("    if (jj_nt == null) {");
      genCodeLine ("      token.next = token_source.getNextToken();");
      genCodeLine ("      jj_ntk = token.next.kind;");
      genCodeLine ("      return jj_ntk;");
      genCodeLine ("    }");
      genCodeLine ("    jj_ntk = jj_nt.kind;");
      genCodeLine ("    return jj_ntk;");
      genCodeLine ("  }");
      genCodeLine ();
    }
    if (Options.isErrorReporting ())
    {
      if (bGenerateGenerics)
      {
        genCodeLine ("  " +
                     javaStaticOpt () +
                     "private java.util.List<int[]> jj_expentries = new java.util.ArrayList<" +
                     (bEmptyTypeVar ? "" : "int[]") +
                     ">();");
      }
      else
      {
        genCodeLine ("  " + javaStaticOpt () + "private java.util.List jj_expentries = new java.util.ArrayList();");
      }
      genCodeLine ("  " + javaStaticOpt () + "private int[] jj_expentry;");
      genCodeLine ("  " + javaStaticOpt () + "private int jj_kind = -1;");
      if (s_jj2index != 0)
      {
        genCodeLine ("  " + javaStaticOpt () + "private int[] jj_lasttokens = new int[100];");
        genCodeLine ("  " + javaStaticOpt () + "private int jj_endpos;");
        genCodeLine ();
        genCodeLine ("  " + javaStaticOpt () + "private void jj_add_error_token(int kind, int pos) {");
        genCodeLine ("  if (pos >= 100) {");
        genCodeLine ("    return;");
        genCodeLine ("  }");
        genCodeLine ();
        genCodeLine ("  if (pos == jj_endpos + 1) {");
        genCodeLine ("    jj_lasttokens[jj_endpos++] = kind;");
        genCodeLine ("  } else if (jj_endpos != 0) {");
        genCodeLine ("    jj_expentry = new int[jj_endpos];");
        genCodeLine ();
        genCodeLine ("    for (int i = 0; i < jj_endpos; i++) {");
        genCodeLine ("      jj_expentry[i] = jj_lasttokens[i];");
        genCodeLine ("    }");
        genCodeLine ();
        if (bGenerateGenerics)
        {
          genCodeLine ("    for (final int[] oldentry : jj_expentries) {");
        }
        else
        {
          genCodeLine ("    for (final java.util.Iterator it = jj_expentries.iterator(); it.hasNext();) {");
          genCodeLine ("      int[] oldentry = (int[])(it.next());");
        }

        genCodeLine ("      if (oldentry.length == jj_expentry.length) {");
        genCodeLine ("        boolean isMatched = true;");
        genCodeLine ("        for (int i = 0; i < jj_expentry.length; i++) {");
        genCodeLine ("          if (oldentry[i] != jj_expentry[i]) {");
        genCodeLine ("            isMatched = false;");
        genCodeLine ("            break;");
        genCodeLine ("          }");
        genCodeLine ("        }");
        genCodeLine ("        if (isMatched) {");
        genCodeLine ("          jj_expentries.add(jj_expentry);");
        genCodeLine ("          break;");
        genCodeLine ("        }");
        genCodeLine ("      }");
        genCodeLine ("    }");
        genCodeLine ();
        genCodeLine ("    if (pos != 0) {");
        genCodeLine ("      jj_endpos = pos;");
        genCodeLine ("      jj_lasttokens[jj_endpos - 1] = kind;");
        genCodeLine ("    }");
        genCodeLine ("  }");
        genCodeLine ("}");
      }
      genCodeLine ();
      genCodeLine ("  /** Generate ParseException. */");
      genCodeLine ("  " + javaStaticOpt () + "public ParseException generateParseException() {");
      genCodeLine ("    jj_expentries.clear();");
      genCodeLine ("    " +
                   eOutputLanguage.getTypeBoolean () +
                   "[] la1tokens = new " +
                   eOutputLanguage.getTypeBoolean () +
                   "[" +
                   s_tokenCount +
                   "];");
      genCodeLine ("    if (jj_kind >= 0) {");
      genCodeLine ("      la1tokens[jj_kind] = true;");
      genCodeLine ("      jj_kind = -1;");
      genCodeLine ("    }");
      genCodeLine ("    for (int i = 0; i < " + s_maskindex + "; i++) {");
      genCodeLine ("      if (jj_la1[i] == jj_gen) {");
      genCodeLine ("        for (int j = 0; j < 32; j++) {");
      for (int i = 0; i < (s_tokenCount - 1) / 32 + 1; i++)
      {
        genCodeLine ("          if ((jj_la1_" + i + "[i] & (1<<j)) != 0) {");
        genCode ("            la1tokens[");
        if (i != 0)
          genCode ((32 * i) + "+");
        genCodeLine ("j] = true;");
        genCodeLine ("          }");
      }
      genCodeLine ("        }");
      genCodeLine ("      }");
      genCodeLine ("    }");
      genCodeLine ("    for (int i = 0; i < " + s_tokenCount + "; i++) {");
      genCodeLine ("      if (la1tokens[i]) {");
      genCodeLine ("        jj_expentry = new int[1];");
      genCodeLine ("        jj_expentry[0] = i;");
      genCodeLine ("        jj_expentries.add(jj_expentry);");
      genCodeLine ("      }");
      genCodeLine ("    }");
      if (s_jj2index != 0)
      {
        genCodeLine ("    jj_endpos = 0;");
        genCodeLine ("    jj_rescan_token();");
        genCodeLine ("    jj_add_error_token(0, 0);");
      }
      genCodeLine ("    int[][] exptokseq = new int[jj_expentries.size()][];");
      genCodeLine ("    for (int i = 0; i < jj_expentries.size(); i++) {");
      if (bGenerateGenerics)
      {
        genCodeLine ("      exptokseq[i] = jj_expentries.get(i);");
      }
      else
      {
        genCodeLine ("      exptokseq[i] = (int[])jj_expentries.get(i);");
      }
      genCodeLine ("    }");

      if (isJavaModernMode)
      {
        // Add the lexical state onto the exception message
        genCodeLine ("    return new ParseException(token, exptokseq, tokenImage, token_source == null ? null : " +
                     s_cu_name +
                     "TokenManager.lexStateNames[token_source.curLexState]);");
      }
      else
      {
        genCodeLine ("    return new ParseException(token, exptokseq, tokenImage);");
      }

      genCodeLine ("  }");
    }
    else
    {
      genCodeLine ("  /** Generate ParseException. */");
      genCodeLine ("  " + javaStaticOpt () + "public ParseException generateParseException() {");
      genCodeLine ("  final Token errortok = token.next;");
      if (Options.isKeepLineColumn ())
      {
        genCodeLine ("  final int line = errortok.beginLine;");
        genCodeLine ("  final int column = errortok.beginColumn;");
      }
      genCodeLine ("  final String mess = errortok.kind == 0 ? tokenImage[0] : errortok.image;");
      if (Options.isKeepLineColumn ())
      {
        genCodeLine ("  return new ParseException(" +
                     "\"Parse error at line \" + line + \", column \" + column + \".  " +
                     "Encountered: \" + mess);");
      }
      else
      {
        genCodeLine ("  return new ParseException(\"Parse error at <unknown location>.  " + "Encountered: \" + mess);");
      }
      genCodeLine ("  }");
    }
    genCodeLine ();

    if (Options.isDebugParser ())
    {
      genCodeLine ("  " + javaStaticOpt () + "private int trace_indent = 0;");
    }
    genCodeLine ("  " +
                 javaStaticOpt () +
                 "private " +
                 eOutputLanguage.getTypeBoolean () +
                 " trace_enabled = " +
                 (Options.isDebugParser () ? "true" : "false") +
                 ";");
    genCodeLine ();
    genCodeLine ("  /** Trace enabled. */");
    genCodeLine ("  " + javaStaticOpt () + "public final boolean trace_enabled() {");
    genCodeLine ("    return trace_enabled;");
    genCodeLine ("  }");
    genCodeLine ();

    if (Options.isDebugParser ())
    {
      genCodeLine ("  /** Enable tracing. */");
      genCodeLine ("  " + javaStaticOpt () + "public final void enable_tracing() {");
      genCodeLine ("    trace_enabled = true;");
      genCodeLine ("  }");
      genCodeLine ();
      genCodeLine ("  /** Disable tracing. */");
      genCodeLine ("  " + javaStaticOpt () + "public final void disable_tracing() {");
      genCodeLine ("    trace_enabled = false;");
      genCodeLine ("  }");
      genCodeLine ();
      genCodeLine ("  " + javaStaticOpt () + "protected void trace_call(final String s) {");
      genCodeLine ("    if (trace_enabled) {");
      genCodeLine ("      for (int i = 0; i < trace_indent; i++) {");
      genCodeLine ("        System.out.print(\" \");");
      genCodeLine ("      }");
      genCodeLine ("      System.out.println(\"Call:	\" + s);");
      genCodeLine ("    }");
      genCodeLine ("    trace_indent = trace_indent + 2;");
      genCodeLine ("  }");
      genCodeLine ();
      genCodeLine ("  " + javaStaticOpt () + "protected void trace_return(String s) {");
      genCodeLine ("    trace_indent = trace_indent - 2;");
      genCodeLine ("    if (trace_enabled) {");
      genCodeLine ("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
      genCodeLine ("      System.out.println(\"Return: \" + s);");
      genCodeLine ("    }");
      genCodeLine ("  }");
      genCodeLine ();
      genCodeLine ("  " + javaStaticOpt () + "protected void trace_token(Token t, String where) {");
      genCodeLine ("    if (trace_enabled) {");
      genCodeLine ("      for (int i = 0; i < trace_indent; i++) {");
      genCodeLine ("        System.out.print(\" \");");
      genCodeLine ("      }");
      genCodeLine ("      System.out.print(\"Consumed token: <\" + tokenImage[t.kind]);");
      genCodeLine ("      if (t.kind != 0 && !tokenImage[t.kind].equals(\"\\\"\" + t.image + \"\\\"\")) {");
      genCodeLine ("        System.out.print(\": \\\"\" + " +
                   Options.getTokenMgrErrorClass () +
                   ".addEscapes(" +
                   "t.image) + \"\\\"\");");
      genCodeLine ("      }");
      genCodeLine ("      System.out.println(\" at line \" + t.beginLine + " +
                   "\" column \" + t.beginColumn + \">\" + where);");
      genCodeLine ("    }");
      genCodeLine ("  }");
      genCodeLine ();
      genCodeLine ("  " + javaStaticOpt () + "protected void trace_scan(Token t1, int t2) {");
      genCodeLine ("    if (trace_enabled) {");
      genCodeLine ("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
      genCodeLine ("      System.out.print(\"Visited token: <\" + tokenImage[t1.kind]);");
      genCodeLine ("      if (t1.kind != 0 && !tokenImage[t1.kind].equals(\"\\\"\" + t1.image + \"\\\"\")) {");
      genCodeLine ("        System.out.print(\": \\\"\" + " +
                   Options.getTokenMgrErrorClass () +
                   ".addEscapes(" +
                   "t1.image) + \"\\\"\");");
      genCodeLine ("      }");
      genCodeLine ("      System.out.println(\" at line \" + t1.beginLine + \"" +
                   " column \" + t1.beginColumn + \">; Expected token: <\" + tokenImage[t2] + \">\");");
      genCodeLine ("    }");
      genCodeLine ("  }");
      genCodeLine ();
    }
    else
    {
      genCodeLine ("  /** Enable tracing. */");
      genCodeLine ("  " + javaStaticOpt () + "public final void enable_tracing() {}");
      genCodeLine ();
      genCodeLine ("  /** Disable tracing. */");
      genCodeLine ("  " + javaStaticOpt () + "public final void disable_tracing() {}");
      genCodeLine ();
    }

    if (s_jj2index != 0 && Options.isErrorReporting ())
    {
      genCodeLine ("  " + javaStaticOpt () + "private void jj_rescan_token() {");
      genCodeLine ("    jj_rescan = true;");
      genCodeLine ("    for (int i = 0; i < " + s_jj2index + "; i++) {");
      genCodeLine ("      try {");
      genCodeLine ("        JJCalls p = jj_2_rtns[i];");
      genCodeLine ("        do {");
      genCodeLine ("          if (p.gen > jj_gen) {");
      genCodeLine ("            jj_la = p.arg;");
      genCodeLine ("            jj_scanpos = p.first;");
      genCodeLine ("            jj_lastpos = p.first;");
      genCodeLine ("            switch (i) {");
      for (int i = 0; i < s_jj2index; i++)
      {
        genCodeLine ("              case " + i + ": jj_3_" + (i + 1) + "(); break;");
      }
      genCodeLine ("            }");
      genCodeLine ("          }");
      genCodeLine ("          p = p.next;");
      genCodeLine ("        } while (p != null);");
      genCodeLine ("      } catch(LookaheadSuccess ls) { /* ignore */ }");
      genCodeLine ("    }");
      genCodeLine ("    jj_rescan = false;");
      genCodeLine ("  }");
      genCodeLine ();
      genCodeLine ("  " + javaStaticOpt () + "private void jj_save(int index, int xla) {");
      genCodeLine ("    JJCalls p = jj_2_rtns[index];");
      genCodeLine ("    while (p.gen > jj_gen) {");
      genCodeLine ("      if (p.next == null) {");
      genCodeLine ("        p.next = new JJCalls();");
      genCodeLine ("        p = p.next;");
      genCodeLine ("        break;");
      genCodeLine ("      }");
      genCodeLine ("      p = p.next;");
      genCodeLine ("    }");
      genCodeLine ("    p.gen = jj_gen + xla - jj_la; ");
      genCodeLine ("    p.first = token;");
      genCodeLine ("    p.arg = xla;");
      genCodeLine ("  }");
      genCodeLine ();
    }

    if (s_jj2index != 0 && Options.isErrorReporting ())
    {
      genCodeLine ("  static final class JJCalls {");
      genCodeLine ("	 int gen;");
      genCodeLine ("	 Token first;");
      genCodeLine ("	 int arg;");
      genCodeLine ("	 JJCalls next;");
      genCodeLine ("  }");
      genCodeLine ();
    }

    if (s_cu_from_insertion_point_2.size () != 0)
    {
      printTokenSetup (s_cu_from_insertion_point_2.get (0));
      m_ccol = 1;
      Token t = null;
      for (final Token aElement : s_cu_from_insertion_point_2)
      {
        t = aElement;
        printToken (t);
      }
      printTrailingComments (t);
    }
    genCodeLine ();

    saveOutput (Options.getOutputDirectory () + File.separator + s_cu_name + getFileExtension ());
  }

  public static void reInit ()
  {
    s_lookaheadNeeded = false;
  }
}
