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

package com.helger.pgcc.jjtree;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.helger.commons.string.StringHelper;
import com.helger.pgcc.Version;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.OutputFile;
import com.helger.pgcc.utils.OutputFileGenerator;

final class NodeFilesJava
{
  private NodeFilesJava ()
  {}

  /**
   * ID of the latest version (of JJTree) in which one of the Node classes was
   * modified.
   */
  static final String nodeVersion = Version.majorDotMinor;

  static Set <String> nodesGenerated = new HashSet <> ();

  static void ensure (final JJTreeIO io, final String nodeType)
  {
    final File file = new File (JJTreeOptions.getJJTreeOutputDirectory (), nodeType + ".java");

    if (nodeType.equals ("Node"))
    {}
    else
      if (nodeType.equals ("SimpleNode"))
      {
        ensure (io, "Node");
      }
      else
      {
        ensure (io, "SimpleNode");
      }

    /*
     * Only build the node file if we're dealing with Node.java, or the
     * NODE_BUILD_FILES option is set.
     */
    if (!(nodeType.equals ("Node") || JJTreeOptions.getBuildNodeFiles ()))
    {
      return;
    }

    if (file.exists () && nodesGenerated.contains (file.getName ()))
    {
      return;
    }

    final String [] options = new String [] { "MULTI",
                                              "NODE_USES_PARSER",
                                              "VISITOR",
                                              "TRACK_TOKENS",
                                              "NODE_PREFIX",
                                              "NODE_EXTENDS",
                                              "NODE_FACTORY",
                                              Options.USEROPTION__SUPPORT_CLASS_VISIBILITY_PUBLIC };
    try (final OutputFile outputFile = new OutputFile (file, nodeVersion, options))
    {
      outputFile.setToolName ("JJTree");

      nodesGenerated.add (file.getName ());

      if (!outputFile.needToWrite)
      {
        return;
      }

      if (nodeType.equals ("Node"))
      {
        _generateNode_java (outputFile);
      }
      else
        if (nodeType.equals ("SimpleNode"))
        {
          _generateSimpleNode_java (outputFile);
        }
        else
        {
          _generateMULTINode_java (outputFile, nodeType);
        }

      outputFile.close ();

    }
    catch (final IOException e)
    {
      throw new Error (e.toString ());
    }
  }

  static void generatePrologue (final PrintWriter ostr)
  {
    // Output the node's package name. JJTreeGlobals.nodePackageName
    // will be the value of NODE_PACKAGE in OPTIONS; if that wasn't set it
    // will default to the parser's package name.
    // If the package names are different we will need to import classes
    // from the parser's package.
    if (StringHelper.hasText (JJTreeGlobals.s_nodePackageName))
    {
      ostr.println ("package " + JJTreeGlobals.s_nodePackageName + ";");
      ostr.println ();
      if (!JJTreeGlobals.s_nodePackageName.equals (JJTreeGlobals.s_packageName))
      {
        ostr.println ("import " + JJTreeGlobals.s_packageName + ".*;");
        ostr.println ();
      }

    }
  }

  static String nodeConstants ()
  {
    return JJTreeGlobals.s_parserName + "TreeConstants";
  }

  static void generateTreeConstants_java ()
  {
    final String name = nodeConstants ();
    final File file = new File (JJTreeOptions.getJJTreeOutputDirectory (), name + ".java");

    try (final OutputFile outputFile = new OutputFile (file); final PrintWriter ostr = outputFile.getPrintWriter ())
    {
      final List <String> nodeIds = ASTNodeDescriptor.getNodeIds ();
      final List <String> nodeNames = ASTNodeDescriptor.getNodeNames ();

      generatePrologue (ostr);
      ostr.println ("public interface " + name);
      ostr.println ("{");

      for (int i = 0; i < nodeIds.size (); ++i)
      {
        final String n = nodeIds.get (i);
        ostr.println ("  public int " + n + " = " + i + ";");
      }

      ostr.println ();
      ostr.println ();

      ostr.println ("  public String[] jjtNodeName = {");
      for (int i = 0; i < nodeNames.size (); ++i)
      {
        final String n = nodeNames.get (i);
        ostr.println ("    \"" + n + "\",");
      }
      ostr.println ("  };");

      ostr.println ("}");
      ostr.close ();

    }
    catch (final IOException e)
    {
      throw new Error (e.toString ());
    }
  }

  static String visitorClass ()
  {
    return JJTreeGlobals.s_parserName + "Visitor";
  }

  static void generateVisitor_java ()
  {
    if (!JJTreeOptions.getVisitor ())
    {
      return;
    }

    final String name = visitorClass ();
    final File file = new File (JJTreeOptions.getJJTreeOutputDirectory (), name + ".java");

    try (final OutputFile outputFile = new OutputFile (file); final PrintWriter ostr = outputFile.getPrintWriter ())
    {
      final List <String> nodeNames = ASTNodeDescriptor.getNodeNames ();

      generatePrologue (ostr);
      ostr.println ("public interface " + name);
      ostr.println ("{");

      final String ve = _mergeVisitorException ();

      String argumentType = "Object";
      if (!JJTreeOptions.getVisitorDataType ().equals (""))
      {
        argumentType = JJTreeOptions.getVisitorDataType ();
      }

      ostr.println ("  public " +
                    JJTreeOptions.getVisitorReturnType () +
                    " visit(SimpleNode node, " +
                    argumentType +
                    " data)" +
                    ve +
                    ";");
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
          ostr.println ("  public " +
                        JJTreeOptions.getVisitorReturnType () +
                        " " +
                        _getVisitMethodName (nodeType) +
                        "(" +
                        nodeType +
                        " node, " +
                        argumentType +
                        " data)" +
                        ve +
                        ";");
        }
      }
      ostr.println ("}");
      ostr.close ();

    }
    catch (final IOException e)
    {
      throw new Error (e.toString ());
    }
  }

  static String defaultVisitorClass ()
  {
    return JJTreeGlobals.s_parserName + "DefaultVisitor";
  }

  private static String _getVisitMethodName (final String className)
  {
    final StringBuilder sb = new StringBuilder ("visit");
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

  static void generateDefaultVisitor_java ()
  {
    if (!JJTreeOptions.getVisitor ())
    {
      return;
    }

    final String className = defaultVisitorClass ();
    final File file = new File (JJTreeOptions.getJJTreeOutputDirectory (), className + ".java");

    try (final OutputFile outputFile = new OutputFile (file); final PrintWriter ostr = outputFile.getPrintWriter ())
    {
      final List <String> nodeNames = ASTNodeDescriptor.getNodeNames ();

      generatePrologue (ostr);
      ostr.println ("public class " + className + " implements " + visitorClass () + "{");

      final String ve = _mergeVisitorException ();

      String argumentType = "Object";
      if (!JJTreeOptions.getVisitorDataType ().equals (""))
      {
        argumentType = JJTreeOptions.getVisitorDataType ();
      }

      final String ret = JJTreeOptions.getVisitorReturnType ();
      ostr.println ("  public " + ret + " defaultVisit(SimpleNode node, " + argumentType + " data)" + ve + "{");
      ostr.println ("    node.childrenAccept(this, data);");
      ostr.println ("    return" + (ret.trim ().equals ("void") ? "" : " data") + ";");
      ostr.println ("  }");

      ostr.println ("  public " + ret + " visit(SimpleNode node, " + argumentType + " data)" + ve + "{");
      ostr.println ("    " + (ret.trim ().equals ("void") ? "" : "return ") + "defaultVisit(node, data);");
      ostr.println ("  }");

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
          ostr.println ("  public " +
                        ret +
                        " " +
                        _getVisitMethodName (nodeType) +
                        "(" +
                        nodeType +
                        " node, " +
                        argumentType +
                        " data)" +
                        ve +
                        "{");
          ostr.println ("    " + (ret.trim ().equals ("void") ? "" : "return ") + "defaultVisit(node, data);");
          ostr.println ("  }");
        }
      }
      ostr.println ("}");
      ostr.close ();

    }
    catch (final IOException e)
    {
      throw new Error (e.toString ());
    }
  }

  private static String _mergeVisitorException ()
  {
    String ve = JJTreeOptions.getVisitorException ();
    if (!"".equals (ve))
    {
      ve = " throws " + ve;
    }
    return ve;
  }

  private static void _generateNode_java (final OutputFile outputFile) throws IOException
  {
    try (final PrintWriter ostr = outputFile.getPrintWriter ())
    {
      generatePrologue (ostr);

      final Map <String, Object> options = new HashMap <> (Options.getOptions ());
      options.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.s_parserName);

      final OutputFileGenerator generator = new OutputFileGenerator ("/templates/Node.template", options);

      generator.generate (ostr);
    }
  }

  private static void _generateSimpleNode_java (final OutputFile outputFile) throws IOException
  {
    try (final PrintWriter ostr = outputFile.getPrintWriter ())
    {
      generatePrologue (ostr);

      final Map <String, Object> options = new HashMap <> (Options.getOptions ());
      options.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.s_parserName);
      options.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (JJTreeOptions.getVisitorReturnType ().equals ("void")));

      final OutputFileGenerator generator = new OutputFileGenerator ("/templates/SimpleNode.template", options);

      generator.generate (ostr);
    }
  }

  private static void _generateMULTINode_java (final OutputFile outputFile, final String nodeType) throws IOException
  {
    try (final PrintWriter ostr = outputFile.getPrintWriter ())
    {
      generatePrologue (ostr);

      final Map <String, Object> options = new HashMap <> (Options.getOptions ());
      options.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.s_parserName);
      options.put ("NODE_TYPE", nodeType);
      options.put ("VISITOR_RETURN_TYPE_VOID", Boolean.valueOf (JJTreeOptions.getVisitorReturnType ().equals ("void")));

      final OutputFileGenerator generator = new OutputFileGenerator ("/templates/MultiNode.template", options);

      generator.generate (ostr);
    }
  }

}
