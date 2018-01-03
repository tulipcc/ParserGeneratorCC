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

package com.helger.pgcc.jjtree;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.helger.pgcc.PGVersion;
import com.helger.pgcc.parser.Options;
import com.helger.pgcc.parser.OutputFile;

/**
 * Generate the State of a tree.
 */
final class JJTreeStateCpp
{
  static final String JJTStateVersion = PGVersion.MAJOR_DOT_MINOR;

  private JJTreeStateCpp ()
  {}

  static void generateTreeState () throws IOException
  {
    final Map <String, Object> options = Options.getAllOptions ();
    options.put (Options.NONUSER_OPTION__PARSER_NAME, JJTreeGlobals.s_parserName);

    final String filePrefix = new File (JJTreeOptions.getJJTreeOutputDirectory (),
                                        "JJT" + JJTreeGlobals.s_parserName + "State").getAbsolutePath ();

    OutputFile outputFile = new OutputFile (new File (filePrefix + ".h"), JJTStateVersion, new String [0]);
    NodeFilesCpp.generateFile (outputFile, "/templates/cpp/JJTTreeState.h.template", options, true);

    outputFile = new OutputFile (new File (filePrefix + ".cc"), JJTStateVersion, new String [0]);
    NodeFilesCpp.generateFile (outputFile, "/templates/cpp/JJTTreeState.cc.template", options, true);
  }
}
