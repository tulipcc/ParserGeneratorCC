package com.helger.pgcc.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.file.FileSystemRecursiveIterator;
import com.helger.commons.io.file.IFileFilter;
import com.helger.commons.io.stream.NonBlockingStringWriter;

public final class OutputFileGeneratorTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (OutputFileGeneratorTest.class);

  @Test
  public void testReadTemplates () throws IOException
  {
    final File fBaseDir = new File ("src/main/resources").getAbsoluteFile ();
    // Find all template files
    for (final File f : new FileSystemRecursiveIterator (new File (fBaseDir,
                                                                   "templates/")).withFilter (IFileFilter.filenameEndsWith (".template")))
    {
      final String sTemplateName = f.getAbsolutePath ().substring (fBaseDir.getAbsolutePath ().length () + 1);
      s_aLogger.info ("Parsing template file " + sTemplateName);
      final Map <String, Object> aOptions = new HashMap <> ();
      aOptions.put ("AT_LEAST_JDK6", Boolean.TRUE);
      aOptions.put ("AT_LEAST_JDK7", Boolean.FALSE);

      // Main parsing
      final OutputFileGenerator aOutputGenerator = new OutputFileGenerator (sTemplateName, aOptions);
      try (final NonBlockingStringWriter sw = new NonBlockingStringWriter ())
      {
        aOutputGenerator.generate (sw);
      }
    }
  }
}
