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
package com.helger.pgcc.parser.output;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.helger.commons.io.file.FileOperations;
import com.helger.commons.io.file.FileSystemIterator;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.pgcc.AbstractJavaCCTestCase;
import com.helger.pgcc.parser.Main;

/**
 * Test cases running some examples and checking to output to support
 * output formating and refactorings.
 *
 * @author RBRi
 */
public final class OutputTest extends AbstractJavaCCTestCase
{
  @Test
  public void simple1 () throws Exception
  {
    test("SimpleExamples/Simple1.jj");
  }

  @Test
  public void simple2 () throws Exception
  {
    test("SimpleExamples/Simple2.jj");
  }

  @Test
  public void simple3 () throws Exception
  {
    test("SimpleExamples/Simple3.jj");
  }

  @Test
  public void simpleExamplesIdList () throws Exception
  {
    test("SimpleExamples/IdList.jj");
  }

  @Test
  public void simpleExamplesNLXlator () throws Exception
  {
    test("SimpleExamples/NL_Xlator.jj");
  }

  @Test
  public void javaCCGrammarJavaCC () throws Exception
  {
    test("JavaCCGrammar/JavaCC.jj");
  }

  @Test
  public void lookaheadExample1 () throws Exception
  {
    test("Lookahead/Example1.jj");
  }

  @Test
  public void mailProcessingDigest () throws Exception
  {
    test("MailProcessing/Digest.jj");
  }

  @Test
  public void mailProcessingFaq () throws Exception
  {
    test("MailProcessing/Faq.jj");
  }

  private void test (String inputFile) throws Exception
  {
    File outputDirectory = new File(getTargetDirectory() + "outputTest");
    FileOperations.deleteDirRecursiveIfExisting(outputDirectory);
    FileOperations.createDir(outputDirectory);

    String options = "-OUTPUT_DIRECTORY=" + outputDirectory.getAbsolutePath();

    Main.mainProgram(new String[] {options, getExamplesDirectory () + inputFile});

    // assert the result
    File expectationDir = new File(getTestResourcesDirectory () + "outputTest/" + inputFile.substring(0, inputFile.lastIndexOf('.')));

    FileSystemIterator generatedFiles = new FileSystemIterator(outputDirectory);
    for (File generatedFile : generatedFiles) {
        String generated = SimpleFileIO.getFileAsString(generatedFile, StandardCharsets.UTF_8);
        generated = generated.replaceAll("\\r\\n?", "\n");

        File expectedFile = new File(expectationDir, generatedFile.getName());
        String expected = SimpleFileIO.getFileAsString(expectedFile, StandardCharsets.UTF_8);
        expected = expected.replaceAll("\\r\\n?", "\n");

        assertEquals("The generated file '" + generatedFile.getName() + "' differs from the expected one.", expected, generated);
    }
  }
}
