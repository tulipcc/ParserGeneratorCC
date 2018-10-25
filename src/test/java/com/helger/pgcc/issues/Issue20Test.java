package com.helger.pgcc.issues;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class Issue20Test
{
  @Test
  public void testParse () throws IOException
  {
    final File aSrc = new File ("src/test/resources/issues/20/grammar.jj");
    final File aOutDir = new File ("target/issue20");
    com.helger.pgcc.parser.Main.mainProgram ("-JDK_VERSION=1.8",
                                             "-OUTPUT_DIRECTORY=" + aOutDir.getAbsolutePath (),
                                             aSrc.getAbsolutePath ());
  }
}
