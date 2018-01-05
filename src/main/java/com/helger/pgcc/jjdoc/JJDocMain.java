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
package com.helger.pgcc.jjdoc;

import static com.helger.pgcc.jjdoc.JJDocGlobals.error;
import static com.helger.pgcc.jjdoc.JJDocGlobals.info;

import java.io.DataInputStream;
import java.io.File;
import java.io.Reader;

import com.helger.commons.io.file.FileHelper;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.JavaCCGlobals;
import com.helger.pgcc.parser.JavaCCParser;
import com.helger.pgcc.parser.Main;
import com.helger.pgcc.parser.MetaParseException;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.ParseException;

/**
 * Main class.
 */
public final class JJDocMain
{
  private JJDocMain ()
  {}

  static void help_message ()
  {
    info ("");
    info ("    jjdoc option-settings - (to read from standard input)");
    info ("OR");
    info ("    jjdoc option-settings inputfile (to read from a file)");
    info ("");
    info ("WHERE");
    info ("    \"option-settings\" is a sequence of settings separated by spaces.");
    info ("");

    info ("Each option setting must be of one of the following forms:");
    info ("");
    info ("    -optionname=value (e.g., -TEXT=false)");
    info ("    -optionname:value (e.g., -TEXT:false)");
    info ("    -optionname       (equivalent to -optionname=true.  e.g., -TEXT)");
    info ("    -NOoptionname     (equivalent to -optionname=false. e.g., -NOTEXT)");
    info ("");
    info ("Option settings are not case-sensitive, so one can say \"-nOtExT\" instead");
    info ("of \"-NOTEXT\".  Option values must be appropriate for the corresponding");
    info ("option, and must be either an integer, boolean or string value.");
    info ("");
    info ("The string valued options are:");
    info ("");
    info ("    OUTPUT_FILE");
    info ("    CSS");
    info ("");
    info ("The boolean valued options are:");
    info ("");
    info ("    ONE_TABLE              (default true)");
    info ("    TEXT                   (default false)");
    info ("    BNF                    (default false)");
    info ("");

    info ("");
    info ("EXAMPLES:");
    info ("    jjdoc -ONE_TABLE=false mygrammar.jj");
    info ("    jjdoc - < mygrammar.jj");
    info ("");
    info ("ABOUT JJDoc:");
    info ("    JJDoc generates JavaDoc documentation from JavaCC grammar files.");
    info ("");
    info ("    For more information, see the online JJDoc documentation at");
    info ("    https://javacc.dev.java.net/doc/JJDoc.html");
  }

  /**
   * A main program that exercises the parser.
   *
   * @param args
   *        Cmdline args
   * @throws Exception
   *         in case of error
   */
  public static void main (final String [] args) throws Exception
  {
    final int errorcode = mainProgram (args);
    System.exit (errorcode);
  }

  /**
   * The method to call to exercise the parser from other Java programs. It
   * returns an error code. See how the main program above uses this method.
   *
   * @param args
   *        Cmdline args
   * @return error code (0 == success)
   * @throws Exception
   *         in case of error
   */
  public static int mainProgram (final String [] args) throws Exception
  {
    Main.reInitAll ();
    JJDocOptions.init ();

    JavaCCGlobals.bannerLine ("Documentation Generator", "0.1.4");

    JavaCCParser parser = null;
    if (args.length == 0)
    {
      help_message ();
      return 1;
    }
    info ("(type \"jjdoc\" with no arguments for help)");

    if (Options.isOption (args[args.length - 1]))
    {
      error ("Last argument \"" + args[args.length - 1] + "\" is not a filename or \"-\".  ");
      return 1;
    }
    for (int arg = 0; arg < args.length - 1; arg++)
    {
      if (!Options.isOption (args[arg]))
      {
        error ("Argument \"" + args[arg] + "\" must be an option setting.");
        return 1;
      }
      Options.setCmdLineOption (args[arg]);
    }

    if (args[args.length - 1].equals ("-"))
    {
      info ("Reading from standard input . . .");
      parser = new JavaCCParser (new DataInputStream (System.in));
      JJDocGlobals.s_input_file = JJDocGlobals.STANDARD_INPUT;
      JJDocGlobals.s_output_file = JJDocGlobals.STANDARD_OUTPUT;
    }
    else
    {
      info ("Reading from file " + args[args.length - 1] + " . . .");
      try
      {
        final File fp = new File (args[args.length - 1]);
        if (!fp.exists ())
        {
          error ("File " + args[args.length - 1] + " not found.");
          return 1;
        }
        if (fp.isDirectory ())
        {
          error (args[args.length - 1] + " is a directory. Please use a valid file name.");
          return 1;
        }
        JJDocGlobals.s_input_file = fp.getName ();
        final Reader aReader = FileHelper.getBufferedReader (new File (args[args.length - 1]),
                                                             Options.getGrammarEncoding ());
        if (aReader == null)
        {
          error ("File " + args[args.length - 1] + " not found.");
          return 1;
        }
        parser = new JavaCCParser (aReader);
      }
      catch (final SecurityException se)
      {
        error ("Security violation while trying to open " + args[args.length - 1]);
        return 1;
      }
    }
    try
    {
      parser.javacc_input ();
      JJDoc.start ();

      if (JavaCCErrors.getErrorCount () == 0)
      {
        if (JavaCCErrors.getWarningCount () == 0)
        {
          info ("Grammar documentation generated successfully in " + JJDocGlobals.s_output_file);
        }
        else
        {
          info ("Grammar documentation generated with 0 errors and " + JavaCCErrors.getWarningCount () + " warnings.");
        }
        return 0;
      }

      error ("Detected " +
             JavaCCErrors.getErrorCount () +
             " errors and " +
             JavaCCErrors.getWarningCount () +
             " warnings.");
      return JavaCCErrors.getErrorCount () == 0 ? 0 : 1;
    }
    catch (final MetaParseException e)
    {
      error (e.toString ());
      error ("Detected " +
             JavaCCErrors.getErrorCount () +
             " errors and " +
             JavaCCErrors.getWarningCount () +
             " warnings.");
      return 1;
    }
    catch (final ParseException e)
    {
      error (e.toString ());
      error ("Detected " +
             (JavaCCErrors.getErrorCount () + 1) +
             " errors and " +
             JavaCCErrors.getWarningCount () +
             " warnings.");
      return 1;
    }
  }
}
