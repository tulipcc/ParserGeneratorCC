// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

package com.helger.pgcc.parser;

import static com.helger.pgcc.parser.JavaCCGlobals.addUnicodeEscapes;
import static com.helger.pgcc.parser.JavaCCGlobals.cu_name;
import static com.helger.pgcc.parser.JavaCCGlobals.s_jjtreeGenerated;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import com.helger.commons.io.stream.NonBlockingBufferedWriter;
import com.helger.commons.io.stream.NonBlockingStringWriter;
import com.helger.pgcc.utils.OutputFileGenerator;

public class CodeGenerator
{
  protected StringBuilder m_mainBuffer = new StringBuilder ();
  protected StringBuilder m_includeBuffer = new StringBuilder ();
  protected StringBuilder m_staticsBuffer = new StringBuilder ();
  protected StringBuilder m_outputBuffer = m_mainBuffer;

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
      m_outputBuffer.append ("0x" + Integer.toHexString (c) + ", ");
    }
    m_outputBuffer.append ("0}");
  }

  public void genCodeLine (final Object... code)
  {
    genCode (code);
    genCode ("\n");
  }

  public void genCode (final Object... code)
  {
    for (final Object s : code)
      m_outputBuffer.append (s);
  }

  public void saveOutput (final String fileName)
  {
    if (!isJavaLanguage ())
    {
      final String incfilePath = fileName.replace (".cc", ".h");
      final String incfileName = new File (incfilePath).getName ();
      m_includeBuffer.insert (0, "#define " + incfileName.replace ('.', '_').toUpperCase () + "\n");
      m_includeBuffer.insert (0, "#ifndef " + incfileName.replace ('.', '_').toUpperCase () + "\n");

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
      if (Options.getTokenManagerUsesParser ())
        m_mainBuffer.insert (0, "#include \"" + cu_name + ".h\"\n");
      m_mainBuffer.insert (0, "#include \"TokenMgrError.h\"\n");
      m_mainBuffer.insert (0, "#include \"" + incfileName + "\"\n");
      m_includeBuffer.append ("#endif\n");
      saveOutput (incfilePath, m_includeBuffer);
    }

    m_mainBuffer.insert (0, "/* " + new File (fileName).getName () + " */\n");
    saveOutput (fileName, m_mainBuffer);
  }

  private static boolean isHexDigit (final char c)
  {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  // HACK
  private void fixupLongLiterals (final StringBuilder sb)
  {
    for (int i = 0; i < sb.length () - 1; i++)
    {
      final char c1 = sb.charAt (i);
      final char c2 = sb.charAt (i + 1);
      if (Character.isDigit (c1) || (c1 == '0' && c2 == 'x'))
      {
        i += c1 == '0' ? 2 : 1;
        while (isHexDigit (sb.charAt (i)))
          i++;
        if (sb.charAt (i) == 'L')
        {
          sb.insert (i, "UL");
        }
        i++;
      }
    }
  }

  public void saveOutput (final String fileName, final StringBuilder sb)
  {
    if (!isJavaLanguage ())
    {
      fixupLongLiterals (sb);
    }
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

  protected int cline, ccol;

  protected void printTokenSetup (final Token t)
  {
    Token tt = t;

    while (tt.specialToken != null)
    {
      tt = tt.specialToken;
    }

    cline = tt.beginLine;
    ccol = tt.beginColumn;
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
    for (; cline < t.beginLine; cline++)
    {
      retval += "\n";
      ccol = 1;
    }
    for (; ccol < t.beginColumn; ccol++)
    {
      retval += " ";
    }
    if (t.kind == JavaCCParserConstants.STRING_LITERAL || t.kind == JavaCCParserConstants.CHARACTER_LITERAL)
      retval += addUnicodeEscapes (t.image);
    else
      retval += t.image;
    cline = t.endLine;
    ccol = t.endColumn + 1;
    if (t.image.length () > 0)
    {
      final char last = t.image.charAt (t.image.length () - 1);
      if (last == '\n' || last == '\r')
      {
        cline++;
        ccol = 1;
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
    if (ccol != 1 && cline != t.beginLine)
    {
      retval += "\n";
      cline++;
      ccol = 1;
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
    switch (Options.getOutputLanguageType ())
    {
      case JAVA:
        genCode ("@" + ann);
        break;
      case CPP:
        // For now, it's only C++ for now
        genCode ("/*" + ann + "*/");
        break;
      default:
        throw new IllegalStateException ("Unknown language : " + Options.getOutputLanguageType ());
    }
  }

  /**
   * Generate a modifier
   */
  public void genModifier (final String mod)
  {
    final String origMod = mod.toLowerCase ();
    if (isJavaLanguage ())
    {
      genCode (mod);
    }
    else
    { // For now, it's only C++ for now
      if (origMod.equals ("public") || origMod.equals ("private"))
      {
        genCode (origMod + ": ");
      }
      // we don't care about other mods for now.
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
    final boolean isJavaLanguage = isJavaLanguage ();
    if (isJavaLanguage && mod != null)
    {
      genModifier (mod);
    }
    genCode ("class " + name);
    if (isJavaLanguage)
    {
      if (superClasses.length == 1 && superClasses[0] != null)
      {
        genCode (" extends " + superClasses[0]);
      }
      if (superInterfaces.length != 0)
      {
        genCode (" implements ");
      }
    }
    else
    {
      if (superClasses.length > 0 || superInterfaces.length > 0)
      {
        genCode (" : ");
      }

      genCommaSeperatedString (superClasses);
    }

    genCommaSeperatedString (superInterfaces);
    genCodeLine (" {");
    if (Options.isOutputLanguageCpp ())
    {
      genCodeLine ("public:");
    }
  }

  private void genCommaSeperatedString (final String [] strings)
  {
    for (int i = 0; i < strings.length; i++)
    {
      if (i > 0)
      {
        genCode (", ");
      }

      genCode (strings[i]);
    }
  }

  @Deprecated
  protected final boolean isJavaLanguage ()
  {
    // TODO :: CBA -- Require Unification of output language specific processing
    // into a single Enum class
    return Options.isOutputLanguageJava ();
  }

  protected static final ELanguage getLanguage ()
  {
    return Options.getOutputLanguageType ();
  }

  public void switchToMainFile ()
  {
    m_outputBuffer = m_mainBuffer;
  }

  public void switchToStaticsFile ()
  {
    if (!isJavaLanguage ())
    {
      m_outputBuffer = m_staticsBuffer;
    }
  }

  public void switchToIncludeFile ()
  {
    if (!isJavaLanguage ())
    {
      m_outputBuffer = m_includeBuffer;
    }
  }

  public void generateMethodDefHeader (final String modsAndRetType, final String className, final String nameAndParams)
  {
    generateMethodDefHeader (modsAndRetType, className, nameAndParams, null);
  }

  public void generateMethodDefHeader (String qualifiedModsAndRetType,
                                       final String className,
                                       final String nameAndParams,
                                       final String exceptions)
  {
    // for C++, we generate the signature in the header file and body in main
    // file
    if (isJavaLanguage ())
    {
      genCode (qualifiedModsAndRetType + " " + nameAndParams);
      if (exceptions != null)
      {
        genCode (" throws " + exceptions);
      }
      genCodeLine ("");
    }
    else
    {
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
      if (qualifiedModsAndRetType != null)
      {
        i = qualifiedModsAndRetType.lastIndexOf ("virtual");
        if (i >= 0)
          qualifiedModsAndRetType = qualifiedModsAndRetType.substring (i + "virtual".length ());
      }
      m_mainBuffer.append ("\n" + qualifiedModsAndRetType + " " + getClassQualifier (className) + nameAndParams);
      // if (exceptions != null)
      // mainBuffer.append(" throw( " + exceptions + ")");
      switchToMainFile ();
    }
  }

  protected String getClassQualifier (final String className)
  {
    return className == null ? "" : className + "::";
  }

  public static String getCharStreamName ()
  {
    if (Options.getUserCharStream ())
    {
      return "CharStream";
    }
    else
    {
      return Options.getJavaUnicodeEscape () ? "JavaCharStream" : "SimpleCharStream";
    }
  }

  @SuppressWarnings ("unchecked")
  protected void writeTemplate (final String name,
                                final Map <String, Object> options,
                                final Object... additionalOptions) throws IOException
  {

    // options.put("", .valueOf(maxOrdinal));

    for (int i = 0; i < additionalOptions.length; i++)
    {
      final Object o = additionalOptions[i];

      if (o instanceof Map <?, ?>)
      {
        options.putAll ((Map <String, ?>) o);
      }
      else
      {
        if (i == additionalOptions.length - 1)
          throw new IllegalArgumentException ("Must supply pairs of [name value] args");

        options.put ((String) o, additionalOptions[i + 1]);
        i++;
      }
    }

    final OutputFileGenerator gen = new OutputFileGenerator (name, options);
    try (final NonBlockingStringWriter sw = new NonBlockingStringWriter ())
    {
      gen.generate (sw);
      genCode (sw.getAsString ());
    }
  }
}
