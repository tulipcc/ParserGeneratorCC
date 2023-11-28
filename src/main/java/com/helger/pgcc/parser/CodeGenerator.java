/*
 * Copyright 2017-2023 Philip Helger, pgcc@helger.com
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

package com.helger.pgcc.parser;

import static com.helger.pgcc.parser.JavaCCGlobals.addUnicodeEscapes;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.NonBlockingBufferedWriter;
import com.helger.commons.io.stream.NonBlockingStringWriter;
import com.helger.pgcc.utils.OutputFileGenerator;

public class CodeGenerator
{
  private final StringBuilder m_aMainBuffer = new StringBuilder ();
  private StringBuilder m_aOutputBuffer = m_aMainBuffer;

  private int m_nLine;
  private int m_nCol;

  public CodeGenerator ()
  {}

  public final void switchToMainFile ()
  {
    m_aOutputBuffer = m_aMainBuffer;
  }

  protected final int getCol ()
  {
    return m_nCol;
  }

  protected final int getLine ()
  {
    return m_nLine;
  }

  protected final void setColToStart ()
  {
    m_nCol = 1;
  }

  protected final void setLineAndCol (final int nLine, final int nCol)
  {
    m_nLine = nLine;
    m_nCol = nCol;
  }

  public final void genCode (final char c)
  {
    m_aOutputBuffer.append (c);
  }

  public final void genCode (final String s)
  {
    m_aOutputBuffer.append (s);
  }

  public final void genCodeNewLine ()
  {
    genCode ("\n");
  }

  public final void genCodeLine (final String s)
  {
    genCode (s);
    genCodeNewLine ();
  }

  public final void saveOutput (final String fileName)
  {
    m_aMainBuffer.insert (0, "/* " + new File (fileName).getName () + " */\n");
    saveOutput (fileName, m_aMainBuffer);
  }

  public final void saveOutput (final String fileName, final StringBuilder sb)
  {
    try (final NonBlockingBufferedWriter fw = FileHelper.getBufferedWriter (new File (fileName), Options.getOutputEncoding ()))
    {
      fw.write (sb.toString ());
    }
    catch (final IOException ioe)
    {
      JavaCCErrors.fatal ("Could not create output file: " + fileName);
    }
  }

  protected final void printTokenSetup (final Token t)
  {
    Token tt = t;

    while (tt.specialToken != null)
    {
      tt = tt.specialToken;
    }

    m_nLine = tt.beginLine;
    m_nCol = tt.beginColumn;
  }

  protected final void printTokenList (final List <Token> list)
  {
    Token t = null;
    for (final Token aToken : list)
    {
      t = aToken;
      printToken (t);
    }

    if (t != null)
      printTrailingComments (t);
  }

  protected final void printTokenOnly (final Token t)
  {
    genCode (getStringForTokenOnly (t));
  }

  protected final String getStringForTokenOnly (final Token t)
  {
    String retval = "";
    for (; m_nLine < t.beginLine; m_nLine++)
    {
      retval += "\n";
      m_nCol = 1;
    }
    for (; m_nCol < t.beginColumn; m_nCol++)
    {
      retval += " ";
    }
    if (t.kind == JavaCCParserConstants.STRING_LITERAL || t.kind == JavaCCParserConstants.CHARACTER_LITERAL)
      retval += addUnicodeEscapes (t.image);
    else
      retval += t.image;
    m_nLine = t.endLine;
    m_nCol = t.endColumn + 1;
    if (t.image.length () > 0)
    {
      final char last = t.image.charAt (t.image.length () - 1);
      if (last == '\n' || last == '\r')
      {
        m_nLine++;
        m_nCol = 1;
      }
    }

    return retval;
  }

  protected final void printToken (@Nonnull final Token t)
  {
    genCode (getStringToPrint (t));
  }

  protected final String getStringToPrint (@Nonnull final Token t)
  {
    String retval = "";
    Token tt = t.specialToken;
    if (tt != null)
    {
      while (tt.specialToken != null)
        tt = tt.specialToken;
      while (tt != null)
      {
        retval += getStringForTokenOnly (tt);
        tt = tt.next;
      }
    }

    return retval + getStringForTokenOnly (t);
  }

  protected final void printLeadingComments (final Token t)
  {
    genCode (getLeadingComments (t));
  }

  protected final String getLeadingComments (final Token t)
  {
    String retval = "";
    if (t.specialToken == null)
      return retval;
    Token tt = t.specialToken;
    while (tt.specialToken != null)
      tt = tt.specialToken;
    while (tt != null)
    {
      retval += getStringForTokenOnly (tt);
      tt = tt.next;
    }
    if (m_nCol != 1 && m_nLine != t.beginLine)
    {
      retval += "\n";
      m_nLine++;
      m_nCol = 1;
    }

    return retval;
  }

  protected final void printTrailingComments (final Token t)
  {
    m_aOutputBuffer.append (getTrailingComments (t));
  }

  protected final String getTrailingComments (final Token t)
  {
    if (t.next == null)
      return "";
    return getLeadingComments (t.next);
  }

  /**
   * for testing
   *
   * @return the generated code + newline
   */
  public final String getGeneratedCode ()
  {
    return m_aOutputBuffer.toString () + "\n";
  }

  /**
   * Generate annotation. @XX syntax for java, comments in C++
   *
   * @param ann
   *        annotation name
   */
  public final void genAnnotation (final String ann)
  {
    genCode ("@" + ann);
  }

  /**
   * Generate a modifier
   *
   * @param mod
   *        modifier
   */
  public final void genModifier (final String mod)
  {
    genCode (mod);
  }

  /**
   * Generate a class with a given name, an array of superclass and another
   * array of super interfaces
   *
   * @param mod
   *        modifier
   * @param name
   *        name
   * @param superClasses
   *        super classes
   * @param superInterfaces
   *        super interfaces
   */
  public final void genClassStart (final String mod, final String name, final String [] superClasses, final String [] superInterfaces)
  {
    if (mod != null)
      genModifier (mod);
    genCode ("class " + name);
    if (superClasses.length == 1 && superClasses[0] != null)
      genCode (" extends " + superClasses[0]);
    if (superInterfaces.length != 0)
      genCode (" implements ");
    _genCommaSeperatedString (superInterfaces);
    genCodeLine (" {");
  }

  private void _genCommaSeperatedString (final String [] strings)
  {
    for (int i = 0; i < strings.length; i++)
    {
      if (i > 0)
        genCode (", ");
      genCode (strings[i]);
    }
  }

  public final void generateMethodDefHeader (final String modsAndRetType, final String className, final String nameAndParams)
  {
    generateMethodDefHeader (modsAndRetType, className, nameAndParams, null);
  }

  public final void generateMethodDefHeader (final String sQualifiedModsAndRetType,
                                             final String sClassName,
                                             final String sNameAndParams,
                                             final String sExceptions)
  {
    genCode (sQualifiedModsAndRetType + " " + sNameAndParams);
    if (sExceptions != null)
    {
      genCode (" throws " + sExceptions);
    }
    genCodeNewLine ();
  }

  protected final String getClassQualifier (final String className)
  {
    return className == null ? "" : className + "::";
  }

  @Nonnull
  public static String getLongHex (final long n)
  {
    return "0x" + Long.toHexString (n) + "L";
  }

  @Nonnull
  public static String getLongPlain (final long n)
  {
    return "0x" + Long.toString (n) + "L";
  }

  public static String getCharStreamName ()
  {
    if (Options.isJavaUserCharStream ())
    {
      // User interface name
      return "CharStream";
    }
    return Options.isJavaUnicodeEscape () ? "JavaCharStream" : "SimpleCharStream";
  }

  public void writeTemplate (final String name, final Map <String, Object> options) throws IOException
  {
    final OutputFileGenerator gen = new OutputFileGenerator (name, options);
    try (final NonBlockingStringWriter sw = new NonBlockingStringWriter ())
    {
      gen.generate (sw);
      genCode (sw.getAsString ());
    }
  }
}
