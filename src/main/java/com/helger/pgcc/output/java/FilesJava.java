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
package com.helger.pgcc.output.java;

import static com.helger.pgcc.parser.JavaCCGlobals.printToken;
import static com.helger.pgcc.parser.JavaCCGlobals.s_ccol;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cline;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_to_insertion_point_1;
import static com.helger.pgcc.parser.JavaCCParserConstants.PACKAGE;
import static com.helger.pgcc.parser.JavaCCParserConstants.SEMICOLON;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;

import com.helger.pgcc.EJDKVersion;
import com.helger.pgcc.PGVersion;
import com.helger.pgcc.output.OutputFile;
import com.helger.pgcc.parser.JavaCCErrors;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.utils.OutputFileGenerator;

/**
 * Generate CharStream, TokenManager and Exceptions.
 */
public class FilesJava
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

  private static Map <String, Object> _getDefaultOptions ()
  {
    final EJDKVersion eJDKVersion = Options.getJdkVersion ();
    final Map <String, Object> ret = Options.getAllOptions ();
    ret.put ("AT_LEAST_JDK7", Boolean.valueOf (eJDKVersion.isNewerOrEqualsThan (EJDKVersion.JDK_17)));
    ret.put ("BEFORE_JDK7", Boolean.valueOf (eJDKVersion.isOlderThan (EJDKVersion.JDK_17)));
    return ret;
  }

  private static void _writePackageName (@Nonnull @WillNotClose final PrintWriter ostr)
  {
    if (s_cu_to_insertion_point_1.size () != 0 && s_cu_to_insertion_point_1.get (0).kind == PACKAGE)
    {
      for (int i = 1; i < s_cu_to_insertion_point_1.size (); i++)
      {
        if (s_cu_to_insertion_point_1.get (i).kind == SEMICOLON)
        {
          s_cline = s_cu_to_insertion_point_1.get (0).beginLine;
          s_ccol = s_cu_to_insertion_point_1.get (0).beginColumn;
          for (int j = 0; j <= i; j++)
          {
            printToken (s_cu_to_insertion_point_1.get (j), ostr);
          }
          ostr.println ();
          ostr.println ();
          break;
        }
      }
    }
  }

  public static void gen_CharStream (final IJavaResourceTemplateLocations locations)
  {
    final File file = new File (Options.getOutputDirectory (), "CharStream.java");
    try (final OutputFile outputFile = new OutputFile (file,
                                                       charStreamVersion,
                                                       new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC }))
    {
      if (!outputFile.needToWrite ())
        return;

      try (final PrintWriter ostr = outputFile.getPrintWriter ())
      {
        _writePackageName (ostr);

        final Map <String, Object> options = _getDefaultOptions ();
        final OutputFileGenerator generator = new OutputFileGenerator (locations.getCharStreamTemplateResourceUrl (),
                                                                       options);

        generator.generate (ostr);
      }
    }
    catch (final IOException e)
    {
      System.err.println ("Failed to create CharStream " + e);
      JavaCCErrors.semantic_error ("Could not open file CharStream.java for writing.");
      throw new UncheckedIOException (e);
    }
  }

  public static void gen_AbstractCharStream (final IJavaResourceTemplateLocations locations)
  {
    final File file = new File (Options.getOutputDirectory (), "AbstractCharStream.java");
    try (final OutputFile outputFile = new OutputFile (file,
                                                       charStreamVersion,
                                                       new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC }))
    {
      if (!outputFile.needToWrite ())
        return;

      try (final PrintWriter ostr = outputFile.getPrintWriter ())
      {
        // Copy package name
        _writePackageName (ostr);

        final Map <String, Object> options = _getDefaultOptions ();
        final OutputFileGenerator generator = new OutputFileGenerator (locations.getAbstractCharStreamTemplateResourceUrl (),
                                                                       options);

        generator.generate (ostr);
      }
    }
    catch (final IOException e)
    {
      System.err.println ("Failed to create AbstractCharStream " + e);
      JavaCCErrors.semantic_error ("Could not open file AbstractCharStream.java for writing.");
      throw new UncheckedIOException (e);
    }
  }

  public static void gen_JavaCharStream (final IJavaResourceTemplateLocations locations)
  {
    final File file = new File (Options.getOutputDirectory (), "JavaCharStream.java");
    try (final OutputFile outputFile = new OutputFile (file,
                                                       charStreamVersion,
                                                       new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC }))
    {
      if (!outputFile.needToWrite ())
        return;

      try (final PrintWriter ostr = outputFile.getPrintWriter ())
      {
        // Copy package name
        _writePackageName (ostr);

        final Map <String, Object> options = _getDefaultOptions ();
        final OutputFileGenerator generator = new OutputFileGenerator (locations.getJavaCharStreamTemplateResourceUrl (),
                                                                       options);

        generator.generate (ostr);
      }
    }
    catch (final IOException e)
    {
      System.err.println ("Failed to create JavaCharStream " + e);
      JavaCCErrors.semantic_error ("Could not open file JavaCharStream.java for writing.");
      throw new UncheckedIOException (e);
    }
  }

  public static void gen_SimpleCharStream (final IJavaResourceTemplateLocations locations)
  {
    final File file = new File (Options.getOutputDirectory (), "SimpleCharStream.java");
    try (final OutputFile outputFile = new OutputFile (file,
                                                       charStreamVersion,
                                                       new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC }))
    {
      if (!outputFile.needToWrite ())
        return;

      try (final PrintWriter ostr = outputFile.getPrintWriter ())
      {
        _writePackageName (ostr);

        final Map <String, Object> options = _getDefaultOptions ();
        final OutputFileGenerator generator = new OutputFileGenerator (locations.getSimpleCharStreamTemplateResourceUrl (),
                                                                       options);

        generator.generate (ostr);
      }
    }
    catch (final IOException e)
    {
      System.err.println ("Failed to create SimpleCharStream " + e);
      JavaCCErrors.semantic_error ("Could not open file SimpleCharStream.java for writing.");
      throw new UncheckedIOException (e);
    }
  }

  public static void gen_JavaModernFiles ()
  {
    // Abstraction for char reader
    _genMiscFile ("Provider.java", "/templates/stream/java/modern/Provider.template");
    _genMiscFile ("StringProvider.java", "/templates/stream/java/modern/StringProvider.template");
    _genMiscFile ("StreamProvider.java", "/templates/stream/java/modern/StreamProvider.template");
  }

  private static void _genMiscFile (final String fileName, final String templatePath) throws Error
  {
    final File file = new File (Options.getOutputDirectory (), fileName);
    try (final OutputFile outputFile = new OutputFile (file,
                                                       parseExceptionVersion,
                                                       new String [] { Options.USEROPTION__KEEP_LINE_COLUMN }))
    {
      if (!outputFile.needToWrite ())
        return;

      try (final PrintWriter ostr = outputFile.getPrintWriter ())
      {
        _writePackageName (ostr);

        final Map <String, Object> options = _getDefaultOptions ();
        final OutputFileGenerator generator = new OutputFileGenerator (templatePath, options);

        generator.generate (ostr);
      }
    }
    catch (final IOException e)
    {
      System.err.println ("Failed to create " + fileName + " " + e);
      JavaCCErrors.semantic_error ("Could not open file " + fileName + " for writing.");
      throw new UncheckedIOException (e);
    }
  }

  public static void gen_ParseException (final IJavaResourceTemplateLocations locations)
  {
    final File file = new File (Options.getOutputDirectory (), "ParseException.java");
    try (final OutputFile outputFile = new OutputFile (file,
                                                       parseExceptionVersion,
                                                       new String [] { Options.USEROPTION__KEEP_LINE_COLUMN }))
    {
      if (!outputFile.needToWrite ())
        return;

      try (final PrintWriter ostr = outputFile.getPrintWriter ())
      {
        _writePackageName (ostr);

        final Map <String, Object> options = _getDefaultOptions ();
        final OutputFileGenerator generator = new OutputFileGenerator (locations.getParseExceptionTemplateResourceUrl (),
                                                                       options);

        generator.generate (ostr);
      }
    }
    catch (final IOException e)
    {
      System.err.println ("Failed to create ParseException " + e);
      JavaCCErrors.semantic_error ("Could not open file ParseException.java for writing.");
      throw new UncheckedIOException (e);
    }
  }

  public static void gen_TokenMgrError (final IJavaResourceTemplateLocations locations)
  {
    final String filename = Options.getTokenMgrErrorClass () + ".java";
    final File file = new File (Options.getOutputDirectory (), filename);

    try (final OutputFile outputFile = new OutputFile (file, tokenMgrErrorVersion, new String [0]))
    {
      if (!outputFile.needToWrite ())
        return;

      try (final PrintWriter ostr = outputFile.getPrintWriter ())
      {
        _writePackageName (ostr);

        final Map <String, Object> options = _getDefaultOptions ();
        final OutputFileGenerator generator = new OutputFileGenerator (locations.getTokenMgrErrorTemplateResourceUrl (),
                                                                       options);

        generator.generate (ostr);
      }
    }
    catch (final IOException e)
    {
      System.err.println ("Failed to create " + filename + " " + e);
      JavaCCErrors.semantic_error ("Could not open file " + filename + " for writing.");
      throw new UncheckedIOException (e);
    }
  }

  public static void gen_Token (final IJavaResourceTemplateLocations locations)
  {
    final File file = new File (Options.getOutputDirectory (), "Token.java");
    try (final OutputFile outputFile = new OutputFile (file,
                                                       tokenVersion,
                                                       new String [] { Options.USEROPTION__TOKEN_EXTENDS,
                                                                       Options.USEROPTION__KEEP_LINE_COLUMN,
                                                                       Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC }))
    {
      if (!outputFile.needToWrite ())
        return;

      try (final PrintWriter ostr = outputFile.getPrintWriter ())
      {
        _writePackageName (ostr);

        final Map <String, Object> options = _getDefaultOptions ();
        final OutputFileGenerator generator = new OutputFileGenerator (locations.getTokenTemplateResourceUrl (),
                                                                       options);

        generator.generate (ostr);
      }
    }
    catch (final IOException e)
    {
      System.err.println ("Failed to create Token " + e);
      JavaCCErrors.semantic_error ("Could not open file Token.java for writing.");
      throw new UncheckedIOException (e);
    }
  }

  public static void gen_TokenManager (final IJavaResourceTemplateLocations locations)
  {
    final File file = new File (Options.getOutputDirectory (), "TokenManager.java");
    try (final OutputFile outputFile = new OutputFile (file,
                                                       tokenManagerVersion,
                                                       new String [] { Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC }))
    {
      if (!outputFile.needToWrite ())
        return;

      try (final PrintWriter ostr = outputFile.getPrintWriter ())
      {
        _writePackageName (ostr);

        final Map <String, Object> options = Options.getAllOptions ();
        final OutputFileGenerator generator = new OutputFileGenerator (locations.getTokenManagerTemplateResourceUrl (),
                                                                       options);
        generator.generate (ostr);
      }
    }
    catch (final IOException e)
    {
      System.err.println ("Failed to create TokenManager " + e);
      JavaCCErrors.semantic_error ("Could not open file TokenManager.java for writing.");
      throw new UncheckedIOException (e);
    }
  }

  public static void reInit ()
  {
    // empty
  }
}
