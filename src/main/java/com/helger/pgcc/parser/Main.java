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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Set;

import com.helger.commons.io.stream.NonBlockingBufferedReader;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.utils.EOptionType;
import com.helger.pgcc.utils.OptionInfo;

/**
 * Entry point.
 */
public class Main
{
  protected Main ()
  {}

  public static LexGenJava lg;

  static void help_message ()
  {

    System.out.println ("Usage:");
    System.out.println ("    javacc option-settings inputfile");
    System.out.println ("");
    System.out.println ("\"option-settings\" is a sequence of settings separated by spaces.");
    System.out.println ("Each option setting must be of one of the following forms:");
    System.out.println ("");
    System.out.println ("    -optionname=value (e.g., -STATIC=false)");
    System.out.println ("    -optionname:value (e.g., -STATIC:false)");
    System.out.println ("    -optionname       (equivalent to -optionname=true.  e.g., -STATIC)");
    System.out.println ("    -NOoptionname     (equivalent to -optionname=false. e.g., -NOSTATIC)");
    System.out.println ("");
    System.out.println ("Option settings are not case-sensitive, so one can say \"-nOsTaTiC\" instead");
    System.out.println ("of \"-NOSTATIC\".  Option values must be appropriate for the corresponding");
    System.out.println ("option, and must be either an integer, a boolean, or a string value.");
    System.out.println ("");

    // 2013/07/23 -- Changed this to auto-generate from metadata in Options so
    // that help is always in-sync with codebase
    printOptions ();

    System.out.println ("EXAMPLE:");
    System.out.println ("    javacc -STATIC=false -LOOKAHEAD:2 -debug_parser mygrammar.jj");
    System.out.println ("");
  }

  private static void printOptions ()
  {

    final Set <OptionInfo> options = Options.getUserOptions ();

    int maxLengthInt = 0;
    int maxLengthBool = 0;
    int maxLengthString = 0;

    for (final OptionInfo i : options)
    {
      final int length = i.getName ().length ();

      if (i.getType () == EOptionType.INTEGER)
      {
        maxLengthInt = length > maxLengthInt ? length : maxLengthInt;
      }
      else
        if (i.getType () == EOptionType.BOOLEAN)
        {
          maxLengthBool = length > maxLengthBool ? length : maxLengthBool;

        }
        else
          if (i.getType () == EOptionType.STRING)
          {
            maxLengthString = length > maxLengthString ? length : maxLengthString;

          }
          else
          {
            // Not interested
          }
    }

    if (maxLengthInt > 0)
    {
      System.out.println ("The integer valued options are:");
      System.out.println ("");
      for (final OptionInfo i : options)
      {
        printOptionInfo (EOptionType.INTEGER, i, maxLengthInt);
      }
      System.out.println ("");
    }

    if (maxLengthBool > 0)
    {
      System.out.println ("The boolean valued options are:");
      System.out.println ("");
      for (final OptionInfo i : options)
      {
        printOptionInfo (EOptionType.BOOLEAN, i, maxLengthBool);
      }
      System.out.println ("");
    }

    if (maxLengthString > 0)
    {
      System.out.println ("The string valued options are:");
      System.out.println ("");
      for (final OptionInfo i : options)
      {
        printOptionInfo (EOptionType.STRING, i, maxLengthString);
      }
      System.out.println ("");
    }
  }

  private static void printOptionInfo (final EOptionType filter, final OptionInfo optionInfo, final int padLength)
  {
    if (optionInfo.getType () == filter)
    {
      final Object default1 = optionInfo.getDefault ();
      System.out.println ("    " +
                          padRight (optionInfo.getName (), padLength + 1) +
                          (default1 == null ? ""
                                            : ("(default : " +
                                               (default1.toString ().length () == 0 ? "<<empty>>" : default1) +
                                               ")")));
    }
  }

  private static String padRight (final String name, final int maxLengthInt)
  {
    final int nameLength = name.length ();
    if (nameLength == maxLengthInt)
    {
      return name;
    }

    final int charsToPad = maxLengthInt - nameLength;
    final StringBuilder sb = new StringBuilder (charsToPad);
    sb.append (name);
    for (int i = 0; i < charsToPad; i++)
      sb.append (" ");

    return sb.toString ();
  }

  /**
   * A main program that exercises the parser.
   */
  public static void main (final String args[]) throws Exception
  {
    final int errorcode = mainProgram (args);
    System.exit (errorcode);
  }

  /**
   * The method to call to exercise the parser from other Java programs. It
   * returns an error code. See how the main program above uses this method.
   */
  public static int mainProgram (final String args[]) throws Exception
  {

    // Initialize all static state
    reInitAll ();

    JavaCCGlobals.bannerLine ("Parser Generator", "");

    JavaCCParser parser = null;
    if (args.length == 0)
    {
      System.out.println ("");
      help_message ();
      return 1;
    }
    System.out.println ("(type \"javacc\" with no arguments for help)");

    if (Options.isOption (args[args.length - 1]))
    {
      System.out.println ("Last argument \"" + args[args.length - 1] + "\" is not a filename.");
      return 1;
    }
    for (int arg = 0; arg < args.length - 1; arg++)
    {
      if (!Options.isOption (args[arg]))
      {
        System.out.println ("Argument \"" + args[arg] + "\" must be an option setting.");
        return 1;
      }
      Options.setCmdLineOption (args[arg]);
    }

    try
    {
      final File fp = new File (args[args.length - 1]);
      if (!fp.exists ())
      {
        System.out.println ("File " + args[args.length - 1] + " not found.");
        return 1;
      }
      if (fp.isDirectory ())
      {
        System.out.println (args[args.length - 1] + " is a directory. Please use a valid file name.");
        return 1;
      }
      parser = new JavaCCParser (new NonBlockingBufferedReader (new InputStreamReader (new FileInputStream (args[args.length -
                                                                                                                 1]),
                                                                                       Options.getGrammarEncoding ())));
    }
    catch (final SecurityException se)
    {
      System.out.println ("Security violation while trying to open " + args[args.length - 1]);
      return 1;
    }
    catch (final FileNotFoundException e)
    {
      System.out.println ("File " + args[args.length - 1] + " not found.");
      return 1;
    }

    try
    {
      System.out.println ("Reading from file " + args[args.length - 1] + " . . .");
      JavaCCGlobals.s_fileName = JavaCCGlobals.s_origFileName = args[args.length - 1];
      JavaCCGlobals.s_jjtreeGenerated = JavaCCGlobals.isGeneratedBy ("JJTree", args[args.length - 1]);
      JavaCCGlobals.s_toolNames = JavaCCGlobals.getToolNames (args[args.length - 1]);
      parser.javacc_input ();

      // 2012/05/02 - Moved this here as cannot evaluate output language
      // until the cc file has been processed. Was previously setting the 'lg'
      // variable
      // to a lexer before the configuration override in the cc file had been
      // read.
      final EOutputLanguage outputLanguage = Options.getOutputLanguage ();

      // 2013/07/22 Java Modern is a
      final boolean isJavaModern = outputLanguage.isJava () &&
                                   Options.getJavaTemplateType ().equals (Options.JAVA_TEMPLATE_TYPE_MODERN);

      switch (outputLanguage)
      {
        case JAVA:
          lg = new LexGenJava ();
          break;
        case CPP:
          lg = new LexGenCpp ();
          break;
        default:
          return unhandledLanguageExit (outputLanguage);
      }

      JavaCCGlobals.createOutputDir (Options.getOutputDirectory ());

      if (Options.isUnicodeInput ())
      {
        NfaState.s_unicodeWarningGiven = true;
        System.out.println ("Note: UNICODE_INPUT option is specified. " +
                            "Please make sure you create the parser/lexer using a Reader with the correct character encoding.");
      }

      Semanticize.start ();
      final boolean isBuildParser = Options.isBuildParser ();

      // 2012/05/02 -- This is not the best way to add-in GWT support, really
      // the code needs to turn supported languages into enumerations
      // and have the enumerations describe the deltas between the outputs. The
      // current approach means that per-langauge configuration is distributed
      // and small changes between targets does not benefit from inheritance.
      switch (outputLanguage)
      {
        case JAVA:
          if (isBuildParser)
          {
            new ParseGenJava ().start (isJavaModern);
          }

          // Must always create the lexer object even if not building a parser.
          new LexGenJava ().start ();

          Options.setStringOption (Options.NONUSER_OPTION__PARSER_NAME, JavaCCGlobals.s_cu_name);
          OtherFilesGen.start (isJavaModern);
          break;
        case CPP:
          // C++ for now
          if (isBuildParser)
          {
            new ParseGenCPP ().start ();
          }
          if (isBuildParser)
          {
            new LexGenCpp ().start ();
          }
          Options.setStringOption (Options.NONUSER_OPTION__PARSER_NAME, JavaCCGlobals.s_cu_name);
          OtherFilesGenCPP.start ();
          break;
        default:
          unhandledLanguageExit (outputLanguage);
          break;
      }

      if ((JavaCCErrors.getErrorCount () == 0) && (isBuildParser || Options.isBuildTokenManager ()))
      {
        if (JavaCCErrors.getWarningCount () == 0)
        {
          if (isBuildParser)
            System.out.println ("Parser generated successfully.");
        }
        else
        {
          System.out.println ("Parser generated with 0 errors and " + JavaCCErrors.getWarningCount () + " warnings.");
        }
        return 0;
      }
      System.out.println ("Detected " +
                          JavaCCErrors.getErrorCount () +
                          " errors and " +
                          JavaCCErrors.getWarningCount () +
                          " warnings.");
      return (JavaCCErrors.getErrorCount () == 0) ? 0 : 1;
    }
    catch (final MetaParseException e)
    {
      System.out.println ("Detected " +
                          JavaCCErrors.getErrorCount () +
                          " errors and " +
                          JavaCCErrors.getWarningCount () +
                          " warnings.");
      return 1;
    }
    catch (final ParseException e)
    {
      System.out.println (e.toString ());
      System.out.println ("Detected " +
                          (JavaCCErrors.getErrorCount () + 1) +
                          " errors and " +
                          JavaCCErrors.getWarningCount () +
                          " warnings.");
      return 1;
    }
  }

  private static int unhandledLanguageExit (final EOutputLanguage outputLanguage)
  {
    System.out.println ("Invalid '" + Options.USEROPTION__OUTPUT_LANGUAGE + "' specified : " + outputLanguage);
    return 1;
  }

  public static void reInitAll ()
  {
    com.helger.pgcc.parser.Expansion.reInit ();
    com.helger.pgcc.parser.JavaCCErrors.reInit ();
    com.helger.pgcc.parser.JavaCCGlobals.reInitStatic ();
    com.helger.pgcc.parser.Options.init ();
    com.helger.pgcc.parser.JavaCCParserInternals.reInit ();
    com.helger.pgcc.parser.RStringLiteral.reInit ();
    com.helger.pgcc.parser.FilesJava.reInit ();
    com.helger.pgcc.parser.NfaState.reInit ();
    com.helger.pgcc.parser.MatchInfo.reInitStatic ();
    com.helger.pgcc.parser.LookaheadWalk.reInit ();
    com.helger.pgcc.parser.Semanticize.reInit ();
    com.helger.pgcc.parser.OtherFilesGen.reInit ();
    com.helger.pgcc.parser.LexGenJava.reInit ();
  }

}
