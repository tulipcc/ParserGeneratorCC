package com.helger.pgcc.parser;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.helger.commons.io.file.FileSystemIterator;
import com.helger.commons.io.file.FileSystemRecursiveIterator;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.file.IFileFilter;
import com.helger.commons.state.ESuccess;

public final class GrammarsParsingFuncTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (GrammarsParsingFuncTest.class);

  @Test
  public void testParseDemoGrammars () throws Exception
  {
    final File fDest = new File ("target/grammars");
    fDest.mkdirs ();

    for (final File f : new FileSystemIterator (new File ("grammars")).withFilter (IFileFilter.filenameEndsWith (".jj")))
    {
      final String sBaseName = FilenameHelper.getBaseName (f);
      s_aLogger.info ("Parsing " + f.getName ());

      final File fGrammarDest = new File (fDest, sBaseName);
      fGrammarDest.mkdirs ();

      // Always Java
      final ESuccess eSuccess = Main.mainProgram (new String [] { "-OUTPUT_DIRECTORY=" +
                                                                  fGrammarDest.getAbsolutePath (),
                                                                  "-JDK_VERSION=1.8",
                                                                  "-OUTPUT_ENCODING=UTF-8",
                                                                  f.getAbsolutePath () });
      assertTrue (eSuccess.isSuccess ());

      // Parse all created Java files
      for (final File fJava : new FileSystemIterator (fGrammarDest).withFilter (IFileFilter.filenameEndsWith (".java")))
      {
        s_aLogger.info ("  Java Parsing " + fJava.getName ());
        final CompilationUnit aCU = JavaParser.parse (fJava, StandardCharsets.UTF_8);
        assertNotNull (aCU);
      }
    }
  }

  @Test
  public void testParseExamples () throws Exception
  {
    final File fDest = new File ("target/examples");
    fDest.mkdirs ();

    for (final File f : new FileSystemRecursiveIterator (new File ("examples")).withFilter (IFileFilter.filenameEndsWith (".jj")))
    {
      final String sBaseName = FilenameHelper.getBaseName (f);
      s_aLogger.info ("Parsing " + f.getParentFile ().getName () + "/" + f.getName ());

      final File fGrammarDest = new File (fDest, sBaseName);
      fGrammarDest.mkdirs ();

      // Also contains C++
      final ESuccess eSuccess = Main.mainProgram (new String [] { "-OUTPUT_DIRECTORY=" +
                                                                  fGrammarDest.getAbsolutePath (),
                                                                  "-JDK_VERSION=1.8",
                                                                  "-OUTPUT_ENCODING=UTF-8",
                                                                  f.getAbsolutePath () });
      assertTrue (eSuccess.isSuccess ());

      // Parse all created Java files
      for (final File fJava : new FileSystemIterator (fGrammarDest).withFilter (IFileFilter.filenameEndsWith (".java")))
      {
        s_aLogger.info ("  Java Parsing " + fJava.getName ());
        final CompilationUnit aCU = JavaParser.parse (fJava, StandardCharsets.UTF_8);
        assertNotNull (aCU);
      }
    }
  }
}
