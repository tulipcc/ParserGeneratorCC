package com.helger.pgcc.output;

import static com.helger.pgcc.parser.JavaCCGlobals.getIdString;
import static com.helger.pgcc.parser.JavaCCGlobals.replaceBackslash;
import static com.helger.pgcc.parser.JavaCCGlobals.s_toolName;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.helger.commons.io.stream.NonBlockingBufferedReader;
import com.helger.pgcc.PGVersion;
import com.helger.pgcc.parser.Options;

public class OutputHelper
{
  private OutputHelper ()
  {}

  /**
   * Read the version from the comment in the specified file. This method does
   * not try to recover from invalid comment syntax, but rather returns version
   * 0.0 (which will always be taken to mean the file is out of date). Works for
   * Java and CPP.
   *
   * @param fileName
   *        eg Token.java
   * @return The version as a double, eg 4.1
   * @since 4.1
   */
  public static double getVersionDashStar (final String fileName)
  {
    final String commentHeader = "/* " + getIdString (s_toolName, fileName) + " Version ";
    final File file = new File (Options.getOutputDirectory (), replaceBackslash (fileName));

    if (!file.exists ())
    {
      // Has not yet been created, so it must be up to date.
      try
      {
        final String majorVersion = PGVersion.s_versionNumber.replaceAll ("[^0-9.]+.*", "");
        return Double.parseDouble (majorVersion);
      }
      catch (final NumberFormatException e)
      {
        return 0.0; // Should never happen
      }
    }

    try (final NonBlockingBufferedReader reader = new NonBlockingBufferedReader (new FileReader (file)))
    {
      String str;
      double version = 0.0;

      // Although the version comment should be the first line, sometimes the
      // user might have put comments before it.
      while ((str = reader.readLine ()) != null)
      {
        if (str.startsWith (commentHeader))
        {
          str = str.substring (commentHeader.length ());
          final int pos = str.indexOf (' ');
          if (pos >= 0)
            str = str.substring (0, pos);
          if (str.length () > 0)
          {
            try
            {
              version = Double.parseDouble (str);
            }
            catch (final NumberFormatException nfe)
            {
              // Ignore - leave version as 0.0
            }
          }

          break;
        }
      }

      return version;
    }
    catch (final IOException ioe)
    {
      return 0.0;
    }
  }

}
