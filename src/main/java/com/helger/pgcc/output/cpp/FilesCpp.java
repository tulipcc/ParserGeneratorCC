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

package com.helger.pgcc.output.cpp;

import static com.helger.pgcc.parser.JavaCCGlobals.getIdString;
import static com.helger.pgcc.parser.JavaCCGlobals.replaceBackslash;
import static com.helger.pgcc.parser.JavaCCGlobals.s_toolName;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

import com.helger.commons.io.stream.NonBlockingBufferedReader;
import com.helger.pgcc.PGVersion;
import com.helger.pgcc.output.OutputFile;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.utils.OutputFileGenerator;

/**
 * Generate CharStream, TokenManager and Exceptions.
 */
public class FilesCpp
{
  /**
   * ID of the latest version (of JavaCC) in which one of the CharStream classes
   * or the CharStream interface is modified.
   */
  static final String charStreamVersion = PGVersion.MAJOR_DOT_MINOR;

  /**
   * ID of the latest version (of JavaCC) in which the TokenManager interface is
   * modified.
   */
  static final String tokenManagerVersion = PGVersion.MAJOR_DOT_MINOR;

  /**
   * ID of the latest version (of JavaCC) in which the Token class is modified.
   */
  static final String tokenVersion = PGVersion.MAJOR_DOT_MINOR;

  /**
   * ID of the latest version (of JavaCC) in which the ParseException class is
   * modified.
   */
  static final String parseExceptionVersion = PGVersion.MAJOR_DOT_MINOR;

  /**
   * ID of the latest version (of JavaCC) in which the TokenMgrError class is
   * modified.
   */
  static final String tokenMgrErrorVersion = PGVersion.MAJOR_DOT_MINOR;

  /**
   * Read the version from the comment in the specified file. This method does
   * not try to recover from invalid comment syntax, but rather returns version
   * 0.0 (which will always be taken to mean the file is out of date).
   *
   * @param fileName
   *        eg Token.java
   * @return The version as a double, eg 4.1
   * @since 4.1
   */
  public static double getVersion (final String fileName)
  {
    final String commentHeader = "/* " + getIdString (s_toolName, fileName) + " Version ";
    final File file = new File (Options.getOutputDirectory (), replaceBackslash (fileName));

    if (!file.exists ())
    {
      // Has not yet been created, so it must be up to date.
      try
      {
        final String majorVersion = PGVersion.s_versionNumber.replaceAll ("[^0-9.]+.*", "");
        return Double.parseDouble (majorVersion);
      }
      catch (final NumberFormatException e)
      {
        return 0.0; // Should never happen
      }
    }

    try (final NonBlockingBufferedReader reader = new NonBlockingBufferedReader (new FileReader (file)))
    {
      String str;
      double version = 0.0;

      // Although the version comment should be the first line, sometimes the
      // user might have put comments before it.
      while ((str = reader.readLine ()) != null)
      {
        if (str.startsWith (commentHeader))
        {
          str = str.substring (commentHeader.length ());
          final int pos = str.indexOf (' ');
          if (pos >= 0)
            str = str.substring (0, pos);
          if (str.length () > 0)
          {
            try
            {
              version = Double.parseDouble (str);
            }
            catch (final NumberFormatException nfe)
            {
              // Ignore - leave version as 0.0
            }
          }

          break;
        }
      }

      return version;
    }
    catch (final IOException ioe)
    {
      return 0.0;
    }
  }

  private static void genFile (final String name, final String version, final String [] parameters)
  {
    final File file = new File (Options.getOutputDirectory (), name);
    try (final OutputFile outputFile = new OutputFile (file, version, parameters))
    {
      if (!outputFile.needToWrite)
        return;

      try (final PrintWriter ostr = outputFile.getPrintWriter ())
      {
        final OutputFileGenerator generator = new OutputFileGenerator ("/templates/cpp/" + name + ".template",
                                                                       Options.getAllOptions ());
        generator.generate (ostr);
      }
    }
    catch (final IOException e)
    {
      System.err.println ("Failed to create file: " + file);
      e.printStackTrace ();
      JavaCCErrors.semantic_error ("Could not open file: " + file + " for writing.");
      throw new UncheckedIOException (e);
    }
  }

  public static void gen_CharStream ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__STATIC,
                                                 Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    genFile ("CharStream.h", charStreamVersion, parameters);
    genFile ("CharStream.cc", charStreamVersion, parameters);
  }

  public static void gen_ParseException ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__STATIC,
                                                 Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    genFile ("ParseException.h", parseExceptionVersion, parameters);
    genFile ("ParseException.cc", parseExceptionVersion, parameters);
  }

  public static void gen_TokenMgrError ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__STATIC,
                                                 Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    genFile ("TokenMgrError.h", tokenMgrErrorVersion, parameters);
    genFile ("TokenMgrError.cc", tokenMgrErrorVersion, parameters);
  }

  public static void gen_Token ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__STATIC,
                                                 Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC,
                                                 Options.USEROPTION__CPP_TOKEN_INCLUDES,
                                                 Options.USEROPTION__TOKEN_EXTENDS };
    genFile ("Token.h", tokenMgrErrorVersion, parameters);
    genFile ("Token.cc", tokenMgrErrorVersion, parameters);
  }

  public static void gen_TokenManager ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__STATIC,
                                                 Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    genFile ("TokenManager.h", tokenManagerVersion, parameters);
  }

  public static void gen_JavaCCDefs ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__STATIC,
                                                 Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    genFile ("JavaCC.h", tokenManagerVersion, parameters);
  }

  public static void gen_ErrorHandler ()
  {
    final String [] parameters = new String [] { Options.USEROPTION__STATIC,
                                                 Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC,
                                                 Options.USEROPTION__BUILD_PARSER,
                                                 Options.USEROPTION__BUILD_TOKEN_MANAGER };
    genFile ("ErrorHandler.h", parseExceptionVersion, parameters);
  }

  public static void reInit ()
  {}

}
