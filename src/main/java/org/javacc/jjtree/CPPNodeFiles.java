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

package org.javacc.jjtree;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javacc.Version;
import org.javacc.parser.Options;
import org.javacc.parser.OtherFilesGenCPP;
import org.javacc.parser.OutputFile;
import org.javacc.utils.OutputFileGenerator;

final class CPPNodeFiles
{
  private CPPNodeFiles ()
  {}

  private static List <String> headersForJJTreeH = new ArrayList <> ();
  /**
   * ID of the latest version (of JJTree) in which one of the Node classes was
   * modified.
   */
  static final String nodeVersion = Version.majorDotMinor;

  static Set <String> nodesToGenerate = new HashSet <> ();

  static void addType (final String type)
  {
    if (!type.equals ("Node") && !type.equals ("SimpleNode"))
    {
      nodesToGenerate.add (type);
    }
  }

  public static String nodeIncludeFile ()
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), "Node.h").getAbsolutePath ();
  }

  public static String simpleNodeIncludeFile ()
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), "SimpleNode.h").getAbsolutePath ();
  }

  public static String simpleNodeCodeFile ()
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), "SimpleNode.cc").getAbsolutePath ();
  }

  public static String jjtreeIncludeFile ()
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), JJTreeGlobals.parserName + "Tree.h").getAbsolutePath ();
  }

  public static String jjtreeImplFile ()
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (),
                     JJTreeGlobals.parserName + "Tree.cc").getAbsolutePath ();
  }

  public static String jjtreeIncludeFile (final String s)
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), s + ".h").getAbsolutePath ();
  }

  public static String jjtreeImplFile (final String s)
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), s + ".cc").getAbsolutePath ();
  }

  public static String jjtreeASTIncludeFile (final String ASTNode)
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), ASTNode + ".h").getAbsolutePath ();
  }

  public static String jjtreeASTCodeFile (final String ASTNode)
  {
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), ASTNode + ".cc").getAbsolutePath ();
  }

  private static String visitorIncludeFile ()
  {
    final String name = visitorClass ();
    return new File (JJTreeOptions.getJJTreeOutputDirectory (), name + ".h").getAbsolutePath ();
  }

  static void generateTreeClasses ()
  {
    generateNodeHeader ();
    generateSimpleNodeHeader ();
    generateSimpleNodeCode ();
    generateMultiTreeInterface ();
    generateMultiTreeImpl ();
    generateOneTreeInterface ();
    // generateOneTreeImpl();
  }

  private static void generateNodeHeader ()
  {
    final File file = new File (nodeIncludeFile ());
    OutputFile outputFile = null;

    try
    {
      final String [] options = new String [] { "MULTI",
                                                "NODE_USES_PARSER",
                                                "VISITOR",
                                                "TRACK_TOKENS",
                                                "NODE_PREFIX",
                                                "NODE_EXTENDS",
                                                "NODE_FACTORY",
                                                Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
      outputFile = new OutputFile (file, nodeVersion, options);
      outputFile.setToolName ("JJTree");

      if (file.exists () && !outputFile.needToWrite)
      {
        return;
      }

      final Map <String, Object> optionMap = new HashMap <> (Options.getOptions ());
      optionMap.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.parserName);
      optionMap.put ("VISITOR_RETURN_TYPE", getVisitorReturnType ());
      optionMap.put ("VISITOR_DATA_TYPE", getVisitorArgumentType ());
      optionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (getVisitorReturnType ().equals ("void")));
      generateFile (outputFile, "/templates/cpp/Node.h.template", optionMap, false);
    }
    catch (final IOException e)
    {
      throw new Error (e.toString ());
    }
    finally
    {
      if (outputFile != null)
      {
        try
        {
          outputFile.close ();
        }
        catch (final IOException ioe)
        {}
      }
    }
  }

  private static void generateSimpleNodeHeader ()
  {
    final File file = new File (simpleNodeIncludeFile ());
    OutputFile outputFile = null;

    try
    {
      final String [] options = new String [] { "MULTI",
                                                "NODE_USES_PARSER",
                                                "VISITOR",
                                                "TRACK_TOKENS",
                                                "NODE_PREFIX",
                                                "NODE_EXTENDS",
                                                "NODE_FACTORY",
                                                Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
      outputFile = new OutputFile (file, nodeVersion, options);
      outputFile.setToolName ("JJTree");

      if (file.exists () && !outputFile.needToWrite)
      {
        return;
      }

      final Map <String, Object> optionMap = new HashMap <> (Options.getOptions ());
      optionMap.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.parserName);
      optionMap.put ("VISITOR_RETURN_TYPE", getVisitorReturnType ());
      optionMap.put ("VISITOR_DATA_TYPE", getVisitorArgumentType ());
      optionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (getVisitorReturnType ().equals ("void")));
      generateFile (outputFile, "/templates/cpp/SimpleNode.h.template", optionMap, false);
    }
    catch (final IOException e)
    {
      throw new Error (e.toString ());
    }
    finally
    {
      if (outputFile != null)
      {
        try
        {
          outputFile.close ();
        }
        catch (final IOException ioe)
        {}
      }
    }
  }

  private static void generateSimpleNodeCode ()
  {
    final File file = new File (simpleNodeCodeFile ());
    OutputFile outputFile = null;

    try
    {
      final String [] options = new String [] { "MULTI",
                                                "NODE_USES_PARSER",
                                                "VISITOR",
                                                "TRACK_TOKENS",
                                                "NODE_PREFIX",
                                                "NODE_EXTENDS",
                                                "NODE_FACTORY",
                                                Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
      outputFile = new OutputFile (file, nodeVersion, options);
      outputFile.setToolName ("JJTree");

      if (file.exists () && !outputFile.needToWrite)
      {
        return;
      }

      final Map <String, Object> optionMap = new HashMap <> (Options.getOptions ());
      optionMap.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.parserName);
      optionMap.put ("VISITOR_RETURN_TYPE", getVisitorReturnType ());
      optionMap.put ("VISITOR_DATA_TYPE", getVisitorArgumentType ());
      optionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (getVisitorReturnType ().equals ("void")));
      generateFile (outputFile, "/templates/cpp/SimpleNode.cc.template", optionMap, false);
    }
    catch (final IOException e)
    {
      throw new Error (e.toString ());
    }
    finally
    {
      if (outputFile != null)
      {
        try
        {
          outputFile.close ();
        }
        catch (final IOException ioe)
        {}
      }
    }
  }

  private static void generateMultiTreeInterface ()
  {
    OutputFile outputFile = null;

    try
    {
      for (final String aString : nodesToGenerate)
      {
        final String node = aString;
        final File file = new File (jjtreeIncludeFile (node));
        final String [] options = new String [] { "MULTI",
                                                  "NODE_USES_PARSER",
                                                  "VISITOR",
                                                  "TRACK_TOKENS",
                                                  "NODE_PREFIX",
                                                  "NODE_EXTENDS",
                                                  "NODE_FACTORY",
                                                  Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
        outputFile = new OutputFile (file, nodeVersion, options);
        outputFile.setToolName ("JJTree");

        if (file.exists () && !outputFile.needToWrite)
        {
          return;
        }

        final Map <String, Object> optionMap = new HashMap <> (Options.getOptions ());
        optionMap.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.parserName);
        optionMap.put ("VISITOR_RETURN_TYPE", getVisitorReturnType ());
        optionMap.put ("VISITOR_DATA_TYPE", getVisitorArgumentType ());
        optionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (getVisitorReturnType ().equals ("void")));

        final PrintWriter ostr = outputFile.getPrintWriter ();
        optionMap.put ("NODE_TYPE", node);
        generateFile (outputFile, "/templates/cpp/MultiNodeInterface.template", optionMap, false);

      }
    }
    catch (final IOException e)
    {
      throw new Error (e.toString ());
    }
    finally
    {
      if (outputFile != null)
      {
        try
        {
          outputFile.close ();
        }
        catch (final IOException ioe)
        {}
      }
    }
  }

  private static void generateMultiTreeImpl ()
  {
    OutputFile outputFile = null;

    try
    {
      for (final String aString : nodesToGenerate)
      {
        final String node = aString;
        final File file = new File (jjtreeImplFile (node));
        final String [] options = new String [] { "MULTI",
                                                  "NODE_USES_PARSER",
                                                  "VISITOR",
                                                  "TRACK_TOKENS",
                                                  "NODE_PREFIX",
                                                  "NODE_EXTENDS",
                                                  "NODE_FACTORY",
                                                  Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
        outputFile = new OutputFile (file, nodeVersion, options);
        outputFile.setToolName ("JJTree");

        if (file.exists () && !outputFile.needToWrite)
        {
          return;
        }

        final Map <String, Object> optionMap = new HashMap <> (Options.getOptions ());
        optionMap.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.parserName);
        optionMap.put ("VISITOR_RETURN_TYPE", getVisitorReturnType ());
        optionMap.put ("VISITOR_DATA_TYPE", getVisitorArgumentType ());
        optionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (getVisitorReturnType ().equals ("void")));

        final PrintWriter ostr = outputFile.getPrintWriter ();
        optionMap.put ("NODE_TYPE", node);
        generateFile (outputFile, "/templates/cpp/MultiNodeImpl.template", optionMap, false);

      }
    }
    catch (final IOException e)
    {
      throw new Error (e.toString ());
    }
    finally
    {
      if (outputFile != null)
      {
        try
        {
          outputFile.close ();
        }
        catch (final IOException ioe)
        {}
      }
    }
  }

  private static void generateOneTreeInterface ()
  {
    final File file = new File (jjtreeIncludeFile ());
    OutputFile outputFile = null;

    try
    {
      final String [] options = new String [] { "MULTI",
                                                "NODE_USES_PARSER",
                                                "VISITOR",
                                                "TRACK_TOKENS",
                                                "NODE_PREFIX",
                                                "NODE_EXTENDS",
                                                "NODE_FACTORY",
                                                Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
      outputFile = new OutputFile (file, nodeVersion, options);
      outputFile.setToolName ("JJTree");

      if (file.exists () && !outputFile.needToWrite)
      {
        return;
      }

      final Map <String, Object> optionMap = new HashMap <> (Options.getOptions ());
      optionMap.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.parserName);
      optionMap.put ("VISITOR_RETURN_TYPE", getVisitorReturnType ());
      optionMap.put ("VISITOR_DATA_TYPE", getVisitorArgumentType ());
      optionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (getVisitorReturnType ().equals ("void")));

      final PrintWriter ostr = outputFile.getPrintWriter ();
      final String includeName = file.getName ().replace ('.', '_').toUpperCase ();
      ostr.println ("#ifndef " + includeName);
      ostr.println ("#define " + includeName);
      ostr.println ("#include \"SimpleNode.h\"");
      for (final String aString : nodesToGenerate)
      {
        final String s = aString;
        ostr.println ("#include \"" + s + ".h\"");
      }
      ostr.println ("#endif");
    }
    catch (final IOException e)
    {
      throw new Error (e.toString ());
    }
    finally
    {
      if (outputFile != null)
      {
        try
        {
          outputFile.close ();
        }
        catch (final IOException ioe)
        {}
      }
    }
  }

  private static void generateOneTreeImpl ()
  {
    final File file = new File (jjtreeImplFile ());
    OutputFile outputFile = null;

    try
    {
      final String [] options = new String [] { "MULTI",
                                                "NODE_USES_PARSER",
                                                "VISITOR",
                                                "TRACK_TOKENS",
                                                "NODE_PREFIX",
                                                "NODE_EXTENDS",
                                                "NODE_FACTORY",
                                                Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
      outputFile = new OutputFile (file, nodeVersion, options);
      outputFile.setToolName ("JJTree");

      if (file.exists () && !outputFile.needToWrite)
      {
        return;
      }

      final Map <String, Object> optionMap = new HashMap <> (Options.getOptions ());
      optionMap.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.parserName);
      optionMap.put ("VISITOR_RETURN_TYPE", getVisitorReturnType ());
      optionMap.put ("VISITOR_DATA_TYPE", getVisitorArgumentType ());
      optionMap.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (getVisitorReturnType ().equals ("void")));
      generateFile (outputFile, "/templates/cpp/TreeImplHeader.template", optionMap, false);

      final boolean hasNamespace = Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0;
      if (hasNamespace)
      {
        outputFile.getPrintWriter ().println ("namespace " + Options.stringValue ("NAMESPACE_OPEN"));
      }

      for (final String aString : nodesToGenerate)
      {
        final String s = aString;
        optionMap.put ("NODE_TYPE", s);
        generateFile (outputFile, "/templates/cpp/MultiNodeImpl.template", optionMap, false);
      }

      if (hasNamespace)
      {
        outputFile.getPrintWriter ().println (Options.stringValue ("NAMESPACE_CLOSE"));
      }
    }
    catch (final IOException e)
    {
      throw new Error (e.toString ());
    }
    finally
    {
      if (outputFile != null)
      {
        try
        {
          outputFile.close ();
        }
        catch (final IOException ioe)
        {}
      }
    }
  }

  static void generatePrologue (final PrintWriter ostr)
  {
    // Output the node's namespace name?
  }

  static String nodeConstants ()
  {
    return JJTreeGlobals.parserName + "TreeConstants";
  }

  static void generateTreeConstants ()
  {
    final String name = nodeConstants ();
    final File file = new File (JJTreeOptions.getJJTreeOutputDirectory (), name + ".h");
    headersForJJTreeH.add (file.getName ());

    try
    {
      final OutputFile outputFile = new OutputFile (file);
      final PrintWriter ostr = outputFile.getPrintWriter ();

      final List <String> nodeIds = ASTNodeDescriptor.getNodeIds ();
      final List <String> nodeNames = ASTNodeDescriptor.getNodeNames ();

      generatePrologue (ostr);
      ostr.println ("#ifndef " + file.getName ().replace ('.', '_').toUpperCase ());
      ostr.println ("#define " + file.getName ().replace ('.', '_').toUpperCase ());

      ostr.println ("\n#include \"JavaCC.h\"");
      final boolean hasNamespace = Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0;
      if (hasNamespace)
      {
        ostr.println ("namespace " + Options.stringValue ("NAMESPACE_OPEN"));
      }
      ostr.println ("enum {");
      for (int i = 0; i < nodeIds.size (); ++i)
      {
        final String n = nodeIds.get (i);
        ostr.println ("  " + n + " = " + i + ",");
      }

      ostr.println ("};");
      ostr.println ();

      for (int i = 0; i < nodeNames.size (); ++i)
      {
        ostr.println ("  static JJChar jjtNodeName_arr_" + i + "[] = ");
        final String n = nodeNames.get (i);
        // ostr.println(" (JJChar*)\"" + n + "\",");
        OtherFilesGenCPP.printCharArray (ostr, n);
        ostr.println (";");
      }
      ostr.println ("  static JJString jjtNodeName[] = {");
      for (int i = 0; i < nodeNames.size (); i++)
      {
        ostr.println ("jjtNodeName_arr_" + i + ", ");
      }
      ostr.println ("  };");

      if (hasNamespace)
      {
        ostr.println (Options.stringValue ("NAMESPACE_CLOSE"));
      }

      ostr.println ("#endif");
      ostr.close ();

    }
    catch (final IOException e)
    {
      throw new Error (e.toString ());
    }
  }

  static String visitorClass ()
  {
    return JJTreeGlobals.parserName + "Visitor";
  }

  private static String getVisitMethodName (final String className)
  {
    final StringBuffer sb = new StringBuffer ("visit");
    if (Options.booleanValue ("VISITOR_METHOD_NAME_INCLUDES_TYPE_NAME"))
    {
      sb.append (Character.toUpperCase (className.charAt (0)));
      for (int i = 1; i < className.length (); i++)
      {
        sb.append (className.charAt (i));
      }
    }

    return sb.toString ();
  }

  private static String getVisitorArgumentType ()
  {
    final String ret = Options.stringValue ("VISITOR_DATA_TYPE");
    return ret == null || ret.equals ("") || ret.equals ("Object") ? "void *" : ret;
  }

  private static String getVisitorReturnType ()
  {
    final String ret = Options.stringValue ("VISITOR_RETURN_TYPE");
    return ret == null || ret.equals ("") || ret.equals ("Object") ? "void " : ret;
  }

  static void generateVisitors ()
  {
    if (!JJTreeOptions.getVisitor ())
    {
      return;
    }

    try
    {
      final String name = visitorClass ();
      final File file = new File (visitorIncludeFile ());
      final OutputFile outputFile = new OutputFile (file);
      final PrintWriter ostr = outputFile.getPrintWriter ();

      generatePrologue (ostr);
      ostr.println ("#ifndef " + file.getName ().replace ('.', '_').toUpperCase ());
      ostr.println ("#define " + file.getName ().replace ('.', '_').toUpperCase ());
      ostr.println ("\n#include \"JavaCC.h\"");
      ostr.println ("#include \"" + JJTreeGlobals.parserName + "Tree.h" + "\"");

      final boolean hasNamespace = Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0;
      if (hasNamespace)
      {
        ostr.println ("namespace " + Options.stringValue ("NAMESPACE_OPEN"));
      }

      generateVisitorInterface (ostr);
      generateDefaultVisitor (ostr);

      if (hasNamespace)
      {
        ostr.println (Options.stringValue ("NAMESPACE_CLOSE"));
      }

      ostr.println ("#endif");
      ostr.close ();
    }
    catch (final IOException ioe)
    {
      throw new Error (ioe.toString ());
    }
  }

  private static void generateVisitorInterface (final PrintWriter ostr)
  {
    final String name = visitorClass ();
    final List <String> nodeNames = ASTNodeDescriptor.getNodeNames ();

    ostr.println ("class " + name);
    ostr.println ("{");

    String argumentType = getVisitorArgumentType ();
    final String returnType = getVisitorReturnType ();
    if (!JJTreeOptions.getVisitorDataType ().equals (""))
    {
      argumentType = JJTreeOptions.getVisitorDataType ();
    }
    ostr.println ("  public:");

    ostr.println ("  virtual " + returnType + " visit(const SimpleNode *node, " + argumentType + " data) = 0;");
    if (JJTreeOptions.getMulti ())
    {
      for (int i = 0; i < nodeNames.size (); ++i)
      {
        final String n = nodeNames.get (i);
        if (n.equals ("void"))
        {
          continue;
        }
        final String nodeType = JJTreeOptions.getNodePrefix () + n;
        ostr.println ("  virtual " +
                      returnType +
                      " " +
                      getVisitMethodName (nodeType) +
                      "(const " +
                      nodeType +
                      " *node, " +
                      argumentType +
                      " data) = 0;");
      }
    }

    ostr.println ("  virtual ~" + name + "() { }");
    ostr.println ("};");
  }

  static String defaultVisitorClass ()
  {
    return JJTreeGlobals.parserName + "DefaultVisitor";
  }

  private static void generateDefaultVisitor (final PrintWriter ostr)
  {
    final String className = defaultVisitorClass ();
    final List <String> nodeNames = ASTNodeDescriptor.getNodeNames ();

    ostr.println ("class " + className + " : public " + visitorClass () + " {");

    final String argumentType = getVisitorArgumentType ();
    final String ret = getVisitorReturnType ();

    ostr.println ("public:");
    ostr.println ("  virtual " + ret + " defaultVisit(const SimpleNode *node, " + argumentType + " data) = 0;");
    // ostr.println(" node->childrenAccept(this, data);");
    // ostr.println(" return" + (ret.trim().equals("void") ? "" : " data") +
    // ";");
    // ostr.println(" }");

    ostr.println ("  virtual " + ret + " visit(const SimpleNode *node, " + argumentType + " data) {");
    ostr.println ("    " + (ret.trim ().equals ("void") ? "" : "return ") + "defaultVisit(node, data);");
    ostr.println ("}");

    if (JJTreeOptions.getMulti ())
    {
      for (int i = 0; i < nodeNames.size (); ++i)
      {
        final String n = nodeNames.get (i);
        if (n.equals ("void"))
        {
          continue;
        }
        final String nodeType = JJTreeOptions.getNodePrefix () + n;
        ostr.println ("  virtual " +
                      ret +
                      " " +
                      getVisitMethodName (nodeType) +
                      "(const " +
                      nodeType +
                      " *node, " +
                      argumentType +
                      " data) {");
        ostr.println ("    " + (ret.trim ().equals ("void") ? "" : "return ") + "defaultVisit(node, data);");
        ostr.println ("  }");
      }
    }
    ostr.println ("  ~" + className + "() { }");
    ostr.println ("};");
  }

  public static void generateFile (final OutputFile outputFile,
                                   final String template,
                                   final Map <String, Object> options) throws IOException
  {
    generateFile (outputFile, template, options, true);
  }

  public static void generateFile (final OutputFile outputFile,
                                   final String template,
                                   final Map <String, Object> options,
                                   final boolean close) throws IOException
  {
    final PrintWriter ostr = outputFile.getPrintWriter ();
    generatePrologue (ostr);
    OutputFileGenerator generator;
    generator = new OutputFileGenerator (template, options);
    generator.generate (ostr);
    if (close)
      ostr.close ();
  }
}
