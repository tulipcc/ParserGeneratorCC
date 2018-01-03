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

import static com.helger.pgcc.parser.JavaCCGlobals.addEscapes;
import static com.helger.pgcc.parser.JavaCCGlobals.getIdString;
import static com.helger.pgcc.parser.JavaCCGlobals.printToken;
import static com.helger.pgcc.parser.JavaCCGlobals.printTokenSetup;
import static com.helger.pgcc.parser.JavaCCGlobals.printTrailingComments;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_name;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_to_insertion_point_1;
import static com.helger.pgcc.parser.JavaCCGlobals.s_ordered_named_tokens;
import static com.helger.pgcc.parser.JavaCCGlobals.s_rexprlist;
import static com.helger.pgcc.parser.JavaCCGlobals.s_toolName;
import static com.helger.pgcc.parser.JavaCCGlobals.s_toolNames;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.helger.commons.io.file.FileHelper;
import com.helger.commons.string.StringHelper;
import com.helger.pgcc.output.java.FilesJava;
import com.helger.pgcc.output.java.IJavaResourceTemplateLocations;

/**
 * Generates the Constants file.
 */
public class OtherFilesGen
{
  private static final String CONSTANTS_FILENAME_SUFFIX = "Constants.java";

  public static void start (final boolean isJavaModern) throws MetaParseException
  {
    final IJavaResourceTemplateLocations templateLoc = isJavaModern ? FilesJava.RESOURCES_JAVA_MODERN
                                                                    : FilesJava.RESOURCES_JAVA_CLASSIC;

    Token t = null;

    if (JavaCCErrors.getErrorCount () != 0)
      throw new MetaParseException ();

    // Added this if condition -- 2012/10/17 -- cba
    if (Options.isGenerateBoilerplateCode ())
    {

      if (isJavaModern)
      {
        FilesJava.gen_JavaModernFiles ();
      }

      FilesJava.gen_TokenMgrError (templateLoc);
      FilesJava.gen_ParseException (templateLoc);
      FilesJava.gen_Token (templateLoc);
    }

    if (Options.isUserTokenManager ())
    {
      // CBA -- I think that Token managers are unique so will always be
      // generated
      FilesJava.gen_TokenManager (templateLoc);
    }
    else
      if (Options.isUserCharStream ())
      {
        // Added this if condition -- 2012/10/17 -- cba
        if (Options.isGenerateBoilerplateCode ())
        {
          FilesJava.gen_CharStream (templateLoc);
        }
      }
      else
      {
        // Added this if condition -- 2012/10/17 -- cba

        if (Options.isGenerateBoilerplateCode ())
        {
          if (Options.isJavaUnicodeEscape ())
          {
            FilesJava.gen_JavaCharStream (templateLoc);
          }
          else
          {
            FilesJava.gen_SimpleCharStream (templateLoc);
          }
        }
      }

    final Writer w = FileHelper.getBufferedWriter (new File (Options.getOutputDirectory (),
                                                             s_cu_name + CONSTANTS_FILENAME_SUFFIX),
                                                   Options.getOutputEncoding ());
    if (w == null)
    {
      JavaCCErrors.semantic_error ("Could not open file " + s_cu_name + CONSTANTS_FILENAME_SUFFIX + " for writing.");
      return;
    }

    try (PrintWriter ostr = new PrintWriter (w))
    {
      final List <String> tn = new ArrayList <> (s_toolNames);
      tn.add (s_toolName);
      ostr.println ("/* " + getIdString (tn, s_cu_name + CONSTANTS_FILENAME_SUFFIX) + " */");

      if (s_cu_to_insertion_point_1.size () != 0 &&
          s_cu_to_insertion_point_1.get (0).kind == JavaCCParserConstants.PACKAGE)
      {
        for (int i = 1; i < s_cu_to_insertion_point_1.size (); i++)
        {
          if (s_cu_to_insertion_point_1.get (i).kind == JavaCCParserConstants.SEMICOLON)
          {
            t = s_cu_to_insertion_point_1.get (0);
            printTokenSetup (t);
            for (int j = 0; j <= i; j++)
            {
              t = s_cu_to_insertion_point_1.get (j);
              printToken (t, ostr);
            }
            printTrailingComments (t);
            ostr.println ("");
            ostr.println ("");
            break;
          }
        }
      }
      ostr.println ("");
      ostr.println ("/**");
      ostr.println (" * Token literal values and constants.");
      ostr.println (" * Generated by " + OtherFilesGen.class.getName () + "#start()");
      ostr.println (" */");

      if (Options.isSupportClassVisibilityPublic ())
      {
        ostr.print ("public ");
      }
      ostr.println ("interface " + s_cu_name + "Constants {");
      ostr.println ("");

      AbstractExpRegularExpression re;
      ostr.println ("  /** End of File. */");
      ostr.println ("  int EOF = 0;");
      for (final java.util.Iterator <AbstractExpRegularExpression> it = s_ordered_named_tokens.iterator (); it.hasNext ();)
      {
        re = it.next ();
        ostr.println ("  /** RegularExpression Id. */");
        ostr.println ("  int " + re.m_label + " = " + re.m_ordinal + ";");
      }
      ostr.println ("");
      if (!Options.isUserTokenManager () && Options.isBuildTokenManager ())
      {
        for (int i = 0; i < LexGenJava.lexStateName.length; i++)
        {
          ostr.println ("  /** Lexical state. */");
          ostr.println ("  int " + LexGenJava.lexStateName[i] + " = " + i + ";");
        }
        ostr.println ("");
      }
      ostr.println ("  /** Literal token values. */");
      ostr.println ("  String[] tokenImage = {");
      ostr.println ("    \"<EOF>\",");

      for (final TokenProduction aTokenProduction : s_rexprlist)
      {
        final TokenProduction tp = (aTokenProduction);
        final List <RegExprSpec> respecs = tp.m_respecs;
        for (final RegExprSpec aRegExprSpec : respecs)
        {
          final RegExprSpec res = (aRegExprSpec);
          re = res.rexp;
          ostr.print ("    ");
          if (re instanceof ExpRStringLiteral)
          {
            ostr.println ("\"\\\"" + addEscapes (addEscapes (((ExpRStringLiteral) re).m_image)) + "\\\"\",");
          }
          else
            if (StringHelper.hasText (re.m_label))
            {
              ostr.println ("\"<" + re.m_label + ">\",");
            }
            else
            {
              if (re.m_tpContext.m_kind == TokenProduction.TOKEN)
              {
                JavaCCErrors.warning (re, "Consider giving this non-string token a label for better error reporting.");
              }
              ostr.println ("\"<token of kind " + re.m_ordinal + ">\",");
            }

        }
      }
      ostr.println ("  };");
      ostr.println ("");
      ostr.println ("}");
    }
  }

  public static void reInit ()
  {
    // empty
  }
}
