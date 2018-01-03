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

package com.helger.pgcc.parser;

import static com.helger.pgcc.parser.JavaCCGlobals.addUnicodeEscapes;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_name;
import static com.helger.pgcc.parser.JavaCCGlobals.s_jjtreeGenerated;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import com.helger.commons.io.stream.NonBlockingBufferedWriter;
import com.helger.commons.io.stream.NonBlockingStringWriter;
import com.helger.pgcc.output.EOutputLanguage;
import com.helger.pgcc.output.UnsupportedOutputLanguageException;
import com.helger.pgcc.utils.OutputFileGenerator;

public class CodeGenerator
{
  protected StringBuilder m_mainBuffer = new StringBuilder ();
  protected StringBuilder m_includeBuffer = new StringBuilder ();
  protected StringBuilder m_staticsBuffer = new StringBuilder ();
  protected StringBuilder m_outputBuffer = m_mainBuffer;

  protected int m_cline;
  protected int m_ccol;

  @Nonnull
  protected final EOutputLanguage getOutputLanguage ()
  {
    return Options.getOutputLanguage ();
  }

  public void switchToMainFile ()
  {
    m_outputBuffer = m_mainBuffer;
  }

  public void switchToStaticsFile ()
  {
    if (getOutputLanguage ().hasStaticsFile ())
    {
      m_outputBuffer = m_staticsBuffer;
    }
  }

  public void switchToIncludeFile ()
  {
    if (getOutputLanguage ().hasIncludeFile ())
    {
      m_outputBuffer = m_includeBuffer;
    }
  }

  public void genStringLiteralArrayCPP (final String varName, final String [] arr)
  {
    // First generate char array vars
    for (int i = 0; i < arr.length; i++)
    {
      genCodeLine ("static const JJChar " + varName + "_arr_" + i + "[] = ");
      genStringLiteralInCPP (arr[i]);
      genCodeLine (";");
    }

    genCodeLine ("static const JJString " + varName + "[] = {");
    for (int i = 0; i < arr.length; i++)
    {
      genCodeLine (varName + "_arr_" + i + ", ");
    }
    genCodeLine ("};");
  }

  public void genStringLiteralInCPP (final String s)
  {
    // String literals in CPP become char arrays
    m_outputBuffer.append ("{");
    for (final char c : s.toCharArray ())
    {
      m_outputBuffer.append ("0x").append (Integer.toHexString (c)).append (", ");
    }
    m_outputBuffer.append ("0}");
  }

  public void genCode (final String s)
  {
    m_outputBuffer.append (s);
  }

  public void genCodeLine ()
  {
    genCode ("\n");
  }

  public void genCodeLine (final String s)
  {
    genCode (s);
    genCodeLine ();
  }

  public void saveOutput (final String fileName)
  {
    if (getOutputLanguage ().hasIncludeFile ())
    {
      final String incfilePath = fileName.replace (".cc", ".h");
      final String incfileName = new File (incfilePath).getName ();

      final String sDefine = incfileName.replace ('.', '_').toUpperCase (Locale.US);
      m_includeBuffer.insert (0, "#define " + sDefine + "\n");
      m_includeBuffer.insert (0, "#ifndef " + sDefine + "\n");

      // dump the statics into the main file with the code.
      m_mainBuffer.insert (0, m_staticsBuffer);

      // Finally enclose the whole thing in the namespace, if specified.
      if (Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0)
      {
        m_mainBuffer.insert (0, "namespace " + Options.stringValue ("NAMESPACE_OPEN") + "\n");
        m_mainBuffer.append (Options.stringValue ("NAMESPACE_CLOSE") + "\n");
        m_includeBuffer.append (Options.stringValue ("NAMESPACE_CLOSE") + "\n");
      }

      if (s_jjtreeGenerated)
      {
        m_mainBuffer.insert (0, "#include \"SimpleNode.h\"\n");
      }
      if (Options.isTokenManagerUsesParser ())
        m_mainBuffer.insert (0, "#include \"" + s_cu_name + ".h\"\n");
      m_mainBuffer.insert (0, "#include \"TokenMgrError.h\"\n");
      m_mainBuffer.insert (0, "#include \"" + incfileName + "\"\n");
      m_includeBuffer.append ("#endif\n");
      saveOutput (incfilePath, m_includeBuffer);
    }

    m_mainBuffer.insert (0, "/* " + new File (fileName).getName () + " */\n");
    saveOutput (fileName, m_mainBuffer);
  }

  public void saveOutput (final String fileName, final StringBuilder sb)
  {
    final File tmp = new File (fileName);
    try (final Writer fw = new NonBlockingBufferedWriter (new FileWriter (tmp)))
    {
      fw.write (sb.toString ());
    }
    catch (final IOException ioe)
    {
      JavaCCErrors.fatal ("Could not create output file: " + fileName);
    }
  }

  protected void printTokenSetup (final Token t)
  {
    Token tt = t;

    while (tt.specialToken != null)
    {
      tt = tt.specialToken;
    }

    m_cline = tt.beginLine;
    m_ccol = tt.beginColumn;
  }

  protected void printTokenList (final List <Token> list)
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

  protected void printTokenOnly (final Token t)
  {
    genCode (getStringForTokenOnly (t));
  }

  protected String getStringForTokenOnly (final Token t)
  {
    String retval = "";
    for (; m_cline < t.beginLine; m_cline++)
    {
      retval += "\n";
      m_ccol = 1;
    }
    for (; m_ccol < t.beginColumn; m_ccol++)
    {
      retval += " ";
    }
    if (t.kind == JavaCCParserConstants.STRING_LITERAL || t.kind == JavaCCParserConstants.CHARACTER_LITERAL)
      retval += addUnicodeEscapes (t.image);
    else
      retval += t.image;
    m_cline = t.endLine;
    m_ccol = t.endColumn + 1;
    if (t.image.length () > 0)
    {
      final char last = t.image.charAt (t.image.length () - 1);
      if (last == '\n' || last == '\r')
      {
        m_cline++;
        m_ccol = 1;
      }
    }

    return retval;
  }

  protected void printToken (final Token t)
  {
    genCode (getStringToPrint (t));
  }

  protected String getStringToPrint (final Token t)
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

  protected void printLeadingComments (final Token t)
  {
    genCode (getLeadingComments (t));
  }

  protected String getLeadingComments (final Token t)
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
    if (m_ccol != 1 && m_cline != t.beginLine)
    {
      retval += "\n";
      m_cline++;
      m_ccol = 1;
    }

    return retval;
  }

  protected void printTrailingComments (final Token t)
  {
    m_outputBuffer.append (getTrailingComments (t));
  }

  protected String getTrailingComments (final Token t)
  {
    if (t.next == null)
      return "";
    return getLeadingComments (t.next);
  }

  /**
   * for testing
   */
  public String getGeneratedCode ()
  {
    return m_outputBuffer.toString () + "\n";
  }

  /**
   * Generate annotation. @XX syntax for java, comments in C++
   */
  public void genAnnotation (final String ann)
  {
    switch (getOutputLanguage ())
    {
      case JAVA:
        genCode ("@" + ann);
        break;
      case CPP:
        // For now, it's only C++ for now
        genCode ("/*" + ann + "*/");
        break;
      default:
        throw new UnsupportedOutputLanguageException (getOutputLanguage ());
    }
  }

  /**
   * Generate a modifier
   */
  public void genModifier (final String mod)
  {
    final String origMod = mod.toLowerCase (Locale.US);
    switch (getOutputLanguage ())
    {
      case JAVA:
        genCode (mod);
        break;
      case CPP:
        // For now, it's only C++ for now
        if (origMod.equals ("public") || origMod.equals ("protected") || origMod.equals ("private"))
          genCode (origMod + ": ");
        break;
      default:
        throw new UnsupportedOutputLanguageException (getOutputLanguage ());
    }
  }

  /**
   * Generate a class with a given name, an array of superclass and another
   * array of super interfaes
   */
  public void genClassStart (final String mod,
                             final String name,
                             final String [] superClasses,
                             final String [] superInterfaces)
  {
    switch (getOutputLanguage ())
    {
      case JAVA:
        if (mod != null)
          genModifier (mod);
        genCode ("class " + name);
        if (superClasses.length == 1 && superClasses[0] != null)
          genCode (" extends " + superClasses[0]);
        if (superInterfaces.length != 0)
          genCode (" implements ");
        _genCommaSeperatedString (superInterfaces);
        genCodeLine (" {");
        break;
      case CPP:
        genCode ("class " + name);
        if (superClasses.length > 0 || superInterfaces.length > 0)
          genCode (" : ");
        _genCommaSeperatedString (superClasses);
        _genCommaSeperatedString (superInterfaces);
        genCodeLine (" {");
        genCodeLine ("public:");
        break;
      default:
        throw new UnsupportedOutputLanguageException (getOutputLanguage ());
    }
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

  public void generateMethodDefHeader (final String modsAndRetType, final String className, final String nameAndParams)
  {
    generateMethodDefHeader (modsAndRetType, className, nameAndParams, null);
  }

  public void generateMethodDefHeader (final String qualifiedModsAndRetType,
                                       final String className,
                                       final String nameAndParams,
                                       final String exceptions)
  {
    switch (getOutputLanguage ())
    {
      case JAVA:
        genCode (qualifiedModsAndRetType + " " + nameAndParams);
        if (exceptions != null)
        {
          genCode (" throws " + exceptions);
        }
        genCodeLine ();
        break;
      case CPP:
        // for C++, we generate the signature in the header file and body in
        // main
        // file
        m_includeBuffer.append (qualifiedModsAndRetType + " " + nameAndParams);
        // if (exceptions != null)
        // includeBuffer.append(" throw(" + exceptions + ")");
        m_includeBuffer.append (";\n");

        String modsAndRetType = null;
        int i = qualifiedModsAndRetType.lastIndexOf (':');
        if (i >= 0)
          modsAndRetType = qualifiedModsAndRetType.substring (i + 1);

        if (modsAndRetType != null)
        {
          i = modsAndRetType.lastIndexOf ("virtual");
          if (i >= 0)
            modsAndRetType = modsAndRetType.substring (i + "virtual".length ());
        }

        String sNonVirtual = qualifiedModsAndRetType;
        i = sNonVirtual.lastIndexOf ("virtual");
        if (i >= 0)
          sNonVirtual = sNonVirtual.substring (i + "virtual".length ());
        m_mainBuffer.append ("\n" + sNonVirtual + " " + getClassQualifier (className) + nameAndParams);
        // if (exceptions != null)
        // mainBuffer.append(" throw( " + exceptions + ")");
        switchToMainFile ();
        break;
      default:
        throw new UnsupportedOutputLanguageException (getOutputLanguage ());
    }
  }

  protected String getClassQualifier (final String className)
  {
    return className == null ? "" : className + "::";
  }

  public static String getCharStreamName ()
  {
    if (Options.isJavaUserCharStream ())
    {
      return "CharStream";
    }
    return Options.isJavaUnicodeEscape () ? "JavaCharStream" : "SimpleCharStream";
  }

  protected void writeTemplate (final String name, final Map <String, Object> options) throws IOException
  {
    final OutputFileGenerator gen = new OutputFileGenerator (name, options);
    try (final NonBlockingStringWriter sw = new NonBlockingStringWriter ())
    {
      gen.generate (sw);
      genCode (sw.getAsString ());
    }
  }
}
