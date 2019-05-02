
package com.helger.pgcc.output.java;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
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

  @BeforeClass
  public static void before ()
  {
    Options.init ();
    // Read from file - to avoid reading template from old PGCC version
    FilesJava.setReadFromClassPath (false);
  }

  @AfterClass
  public static void afterClass ()
  {
    // Restore default!
    FilesJava.setReadFromClassPath (true);
  }

  @Test
  public void testGenerateAndCompile ()
  {
    final File aTargetDir = new File ("target/templates/java").getAbsoluteFile ();
    Options.setInputFileOption (null, null, Options.USEROPTION__OUTPUT_DIRECTORY, aTargetDir.getAbsolutePath ());

    for (int nClassicOrModern = 0; nClassicOrModern <= 1; ++nClassicOrModern)
    {
      final boolean bIsModern = nClassicOrModern == 0;
      final IJavaResourceTemplateLocations aLocation = bIsModern ? new JavaModernResourceTemplateLocationImpl ()
                                                                 : new JavaResourceTemplateLocationImpl ();
      final String sMode = bIsModern ? "modern " : "classic ";

      for (int nKeepLineColumn = 0; nKeepLineColumn <= 1; ++nKeepLineColumn)
      {
        final boolean bKeepLineColumn = Boolean.valueOf (nKeepLineColumn != 0);
        Options.setStringOption (Options.USEROPTION__KEEP_LINE_COLUMN, Boolean.toString (bKeepLineColumn));
        final String sKeepLineColumn = bKeepLineColumn ? "[keep line/col] " : "";

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
          LOGGER.info ("Parsing " + sMode + sKeepLineColumn + aFile.getName ());
          final ParseResult <CompilationUnit> aResult = new JavaParser ().parse (FileHelper.getInputStream (aFile),
                                                                                 Options.getOutputEncoding ());
          assertTrue (aResult.getProblems ().toString (), aResult.isSuccessful ());
        }
      }
    }

  }
}
