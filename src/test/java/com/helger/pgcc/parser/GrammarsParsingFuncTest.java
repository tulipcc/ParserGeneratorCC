package com.helger.pgcc.parser;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.file.FileSystemIterator;
import com.helger.commons.io.file.IFileFilter;

public final class GrammarsParsingFuncTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (GrammarsParsingFuncTest.class);

  @Test
  public void testParse () throws Exception
  {
    final File fDest = new File ("target/grammars");
    fDest.mkdirs ();

    for (final File f : new FileSystemIterator (new File ("grammars")).withFilter (IFileFilter.filenameEndsWith (".jj")))
    {
      s_aLogger.info ("Parsing " + f.getName ());
      Main.mainProgram (new String [] { "-OUTPUT_DIRECTORY=" + fDest.getAbsolutePath (), f.getAbsolutePath () });
    }
  }
}
