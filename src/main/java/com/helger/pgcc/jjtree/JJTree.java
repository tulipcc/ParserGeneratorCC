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

package com.helger.pgcc.jjtree;

import com.helger.pgcc.parser.JavaCCGlobals;
import com.helger.pgcc.parser.Options;

public class JJTree
{
  private JJTreeIO io;

  private void _println (final String s)
  {
    io.getMsg ().println (s);
  }

  private void help_message ()
  {
    _println ("Usage:");
    _println ("    jjtree option-settings inputfile");
    _println ("");
    _println ("\"option-settings\" is a sequence of settings separated by spaces.");
    _println ("Each option setting must be of one of the following forms:");
    _println ("");
    _println ("    -optionname=value (e.g., -STATIC=false)");
    _println ("    -optionname:value (e.g., -STATIC:false)");
    _println ("    -optionname       (equivalent to -optionname=true.  e.g., -STATIC)");
    _println ("    -NOoptionname     (equivalent to -optionname=false. e.g., -NOSTATIC)");
    _println ("");
    _println ("Option settings are not case-sensitive, so one can say \"-nOsTaTiC\" instead");
    _println ("of \"-NOSTATIC\".  Option values must be appropriate for the corresponding");
    _println ("option, and must be either an integer or a string value.");
    _println ("");

    _println ("The boolean valued options are:");
    _println ("");
    _println ("    STATIC                   (default true)");
    _println ("    MULTI                    (default false)");
    _println ("    NODE_DEFAULT_VOID        (default false)");
    _println ("    NODE_SCOPE_HOOK          (default false)");
    _println ("    NODE_USES_PARSER         (default false)");
    _println ("    BUILD_NODE_FILES         (default true)");
    _println ("    TRACK_TOKENS             (default false)");
    _println ("    VISITOR                  (default false)");
    _println ("");
    _println ("The string valued options are:");
    _println ("");
    _println ("    JDK_VERSION              (default \"1.5\")");
    _println ("    NODE_CLASS               (default \"\")");
    _println ("    NODE_PREFIX              (default \"AST\")");
    _println ("    NODE_PACKAGE             (default \"\")");
    _println ("    NODE_EXTENDS             (default \"\")");
    _println ("    NODE_FACTORY             (default \"\")");
    _println ("    OUTPUT_FILE              (default remove input file suffix, add .jj)");
    _println ("    OUTPUT_DIRECTORY         (default \"\")");
    _println ("    JJTREE_OUTPUT_DIRECTORY  (default value of OUTPUT_DIRECTORY option)");
    _println ("    VISITOR_DATA_TYPE        (default \"\")");
    _println ("    VISITOR_RETURN_TYPE      (default \"Object\")");
    _println ("    VISITOR_EXCEPTION        (default \"\")");
    _println ("");
    _println ("JJTree also accepts JavaCC options, which it inserts into the generated file.");
    _println ("");

    _println ("EXAMPLES:");
    _println ("    jjtree -STATIC=false mygrammar.jjt");
    _println ("");
    _println ("ABOUT JJTree:");
    _println ("    JJTree is a preprocessor for JavaCC that inserts actions into a");
    _println ("    JavaCC grammar to build parse trees for the input.");
    _println ("");
    _println ("    For more information, see the online JJTree documentation at ");
    _println ("    https://javacc.dev.java.net/doc/JJTree.html ");
    _println ("");
  }

  /**
   * A main program that exercises the parser.
   */
  public int main (final String args[])
  {
    // initialize static state for allowing repeat runs without exiting
    ASTNodeDescriptor.reInit ();
    com.helger.pgcc.parser.Main.reInitAll ();

    JavaCCGlobals.bannerLine ("Tree Builder", "");

    io = new JJTreeIO ();

    try
    {

      initializeOptions ();
      if (args.length == 0)
      {
        _println ("");
        help_message ();
        return 1;
      }
      _println ("(type \"jjtree\" with no arguments for help)");

      final String fn = args[args.length - 1];
      if (Options.isOption (fn))
      {
        _println ("Last argument \"" + fn + "\" is not a filename");
        return 1;
      }
      for (int arg = 0; arg < args.length - 1; arg++)
      {
        if (!Options.isOption (args[arg]))
        {
          _println ("Argument \"" + args[arg] + "\" must be an option setting.");
          return 1;
        }
        Options.setCmdLineOption (args[arg]);
      }

      JJTreeOptions.validate ();

      try
      {
        io.setInput (fn);
      }
      catch (final JJTreeIOException ioe)
      {
        _println ("Error setting input: " + ioe.getMessage ());
        return 1;
      }
      _println ("Reading from file " + io.getInputFilename () + " . . .");

      JJTreeGlobals.toolList = JavaCCGlobals.getToolNames (fn);
      JJTreeGlobals.toolList.add ("JJTree");

      try
      {
        final JJTreeParser parser = new JJTreeParser (io.getIn ());
        parser.javacc_input ();

        final ASTGrammar root = (ASTGrammar) parser.jjtree.rootNode ();
        if (Boolean.getBoolean ("jjtree-dump"))
        {
          root.dump (" ");
        }
        try
        {
          io.setOutput ();
        }
        catch (final JJTreeIOException ioe)
        {
          _println ("Error setting output: " + ioe.getMessage ());
          return 1;
        }
        root.generate (io);
        io.getOut ().close ();

        // TODO :: Not yet tested this in GWT/Modern mode (disabled by default
        // in 6.1)
        switch (Options.getOutputLanguage ())
        {
          case JAVA:
            NodeFilesJava.generateTreeConstants_java ();
            NodeFilesJava.generateVisitor_java ();
            NodeFilesJava.generateDefaultVisitor_java ();
            JJTreeStateJava.generateTreeState_java ();
            break;
          case CPP:
            NodeFilesCpp.generateTreeConstants ();
            NodeFilesCpp.generateVisitors ();
            // CPPNodeFiles.generateDefaultVisitor();
            JJTreeStateCpp.generateTreeState ();
            // CPPNodeFiles.generateJJTreeH();
            break;
          default:
            _println ("Unsupported JJTree output language : " + Options.getOutputLanguage ());
            return 1;
        }

        _println ("Annotated grammar generated successfully in " + io.getOutputFilename ());
      }
      catch (final ParseException pe)
      {
        _println ("Error parsing input: " + pe.toString ());
        return 1;
      }
      catch (final Exception e)
      {
        _println ("Error parsing input: " + e.toString ());
        e.printStackTrace (io.getMsg ());
        return 1;
      }

      return 0;

    }
    finally
    {
      io.closeAll ();
    }
  }

  /**
   * Initialize for JJTree
   */
  private void initializeOptions ()
  {
    JJTreeOptions.init ();
    JJTreeGlobals.initialize ();
  }
}
