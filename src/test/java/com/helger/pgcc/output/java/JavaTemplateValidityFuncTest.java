
package com.helger.pgcc.output.java;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.file.FileOperationManager;
import com.helger.commons.io.file.FileSystemIterator;
import com.helger.commons.io.file.IFileFilter;
import com.helger.pgcc.parser.Options;

/**
 * Test class that creates the Java template files to disk and tries to compile
 * them with the JavaParser
 *
 * @author Philip Helger
 */
public final class JavaTemplateValidityFuncTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (JavaTemplateValidityFuncTest.class);

  @Before
  public void before ()
  {
    Options.init ();
  }

  @Test
  public void testGenerateAndCompile ()
  {
    final File aTargetDir = new File ("test/templates/java").getAbsoluteFile ();
    Options.setInputFileOption (null, null, Options.USEROPTION__OUTPUT_DIRECTORY, aTargetDir.getAbsolutePath ());

    for (int nClassicOrModern = 0; nClassicOrModern <= 1; ++nClassicOrModern)
    {
      final boolean bIsModern = nClassicOrModern == 0;
      final IJavaResourceTemplateLocations aLocation = bIsModern ? new JavaModernResourceTemplateLocationImpl ()
                                                                 : new JavaResourceTemplateLocationImpl ();

      // Create files
      FileOperationManager.INSTANCE.deleteDirRecursiveIfExisting (aTargetDir);
      FileOperationManager.INSTANCE.createDirRecursive (aTargetDir);

      if (bIsModern)
        FilesJava.gen_JavaModernFiles ();

      FilesJava.gen_ParseException (aLocation);
      FilesJava.gen_Token (aLocation);
      FilesJava.gen_TokenManager (aLocation);
      FilesJava.gen_TokenMgrError (aLocation);

      FilesJava.gen_CharStream (aLocation);
      FilesJava.gen_AbstractCharStream (aLocation);
      FilesJava.gen_JavaCharStream (aLocation);
      FilesJava.gen_SimpleCharStream (aLocation);

      // Now try compiling the files
      for (final File aFile : new FileSystemIterator (aTargetDir).withFilter (IFileFilter.fileOnly ()
                                                                                         .and (IFileFilter.filenameEndsWith (".java"))))
      {
        LOGGER.info ("Parsing " + (bIsModern ? "modern" : "classic") + " " + aFile.getName ());
        final ParseResult <CompilationUnit> aResult = new JavaParser ().parse (FileHelper.getInputStream (aFile),
                                                                               Options.getOutputEncoding ());
        assertTrue (aResult.isSuccessful ());
      }
    }
  }
}
