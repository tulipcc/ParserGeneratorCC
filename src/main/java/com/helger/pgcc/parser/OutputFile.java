/**
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
 *
 * Copyright 2011 Google Inc. All Rights Reserved.
 * Author: sreeni@google.com (Sreeni Viswanadha)
 *
 * Copyright 2017-2018 Philip Helger, pgcc@helger.com
 */
package com.helger.pgcc.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.WillCloseWhenClosed;

import com.helger.commons.io.stream.NonBlockingBufferedOutputStream;
import com.helger.commons.io.stream.NonBlockingBufferedReader;
import com.helger.commons.io.stream.NullOutputStream;
import com.helger.pgcc.PGVersion;
import com.helger.security.messagedigest.EMessageDigestAlgorithm;

/**
 * This class handles the creation and maintenance of the boiler-plate classes,
 * such as Token.java, JavaCharStream.java etc. It is responsible for:
 * <ul>
 * <li>Writing the JavaCC header lines to the file.</li>
 * <li>Writing the checksum line.</li>
 * <li>Using the checksum to determine if an existing file has been changed by
 * the user (and so should be left alone).</li>
 * <li>Checking any existing file's version (if the file can not be
 * overwritten).</li>
 * <li>Checking any existing file's creation options (if the file can not be
 * overwritten).</li>
 * <li></li>
 * </ul>
 *
 * @author Paul Cager
 */
public class OutputFile implements AutoCloseable
{
  private static final String MD5_LINE_PART_1 = "/* JavaCC - OriginalChecksum=";
  private static final String MD5_LINE_PART_1q = "/\\* JavaCC - OriginalChecksum=";
  private static final String MD5_LINE_PART_2 = " (do not edit this line) */";
  private static final String MD5_LINE_PART_2q = " \\(do not edit this line\\) \\*/";

  TrapClosePrintWriter m_pw;
  DigestOutputStream dos;
  String m_toolName = JavaCCGlobals.s_toolName;
  final File m_file;
  final String m_compatibleVersion;
  final String [] m_options;
  public boolean needToWrite = true;

  /**
   * Create a new OutputFile.
   *
   * @param file
   *        the file to write to.
   * @param compatibleVersion
   *        the minimum compatible JavaCC version.
   * @param options
   *        if the file already exists, and cannot be overwritten, this is a
   *        list of options (such s STATIC=false) to check for changes.
   * @throws IOException
   */
  public OutputFile (final File file, final String compatibleVersion, final String [] options) throws IOException
  {
    this.m_file = file;
    this.m_compatibleVersion = compatibleVersion;
    this.m_options = options;

    if (file.exists ())
    {
      // Generate the checksum of the file, and compare with any value
      // stored
      // in the file.

      try (final NonBlockingBufferedReader br = new NonBlockingBufferedReader (new FileReader (file)))
      {
        MessageDigest digest;
        try
        {
          digest = MessageDigest.getInstance ("MD5");
        }
        catch (final NoSuchAlgorithmException e)
        {
          throw new IOException ("No MD5 implementation", e);
        }
        try (final DigestOutputStream digestStream = new DigestOutputStream (new NullOutputStream (), digest);
             final PrintWriter pw = new PrintWriter (digestStream))
        {
          String line;
          String existingMD5 = null;
          while ((line = br.readLine ()) != null)
          {
            if (line.startsWith (MD5_LINE_PART_1))
            {
              existingMD5 = line.replaceAll (MD5_LINE_PART_1q, "").replaceAll (MD5_LINE_PART_2q, "");
            }
            else
            {
              pw.println (line);
            }
          }

          final String calculatedDigest = toHexString (digestStream.getMessageDigest ().digest ());

          if (existingMD5 == null || !existingMD5.equals (calculatedDigest))
          {
            // No checksum in file, or checksum differs.
            needToWrite = false;

            if (compatibleVersion != null)
            {
              checkVersion (file, compatibleVersion);
            }

            if (options != null)
            {
              checkOptions (file, options);
            }

          }
          else
          {
            // The file has not been altered since JavaCC created it.
            // Rebuild it.
            System.out.println ("File \"" + file.getName () + "\" is being rebuilt.");
            needToWrite = true;
          }
        }
      }
    }
    else
    {
      // File does not exist
      System.out.println ("File \"" + file.getName () + "\" does not exist.  Will create one.");
      needToWrite = true;
    }
  }

  public OutputFile (final File file) throws IOException
  {
    this (file, null, null);
  }

  /**
   * Output a warning if the file was created with an incompatible version of
   * JavaCC.
   *
   * @param fileName
   * @param versionId
   */
  private void checkVersion (final File file, final String versionId)
  {
    final String firstLine = "/* " + JavaCCGlobals.getIdString (m_toolName, file.getName ()) + " Version ";

    try (final BufferedReader reader = new BufferedReader (new FileReader (file)))
    {
      String line;
      while ((line = reader.readLine ()) != null)
      {
        if (line.startsWith (firstLine))
        {
          final String version = line.replaceFirst (".*Version ", "").replaceAll (" \\*/", "");
          if (!version.equals (versionId))
          {
            JavaCCErrors.warning (file.getName () +
                                  ": File is obsolete.  Please rename or delete this file so" +
                                  " that a new one can be generated for you.");
            JavaCCErrors.warning (file.getName () + " file   version: " + version + " javacc version: " + versionId);
          }
          return;
        }
      }
      // If no version line is found, do not output the warning.
    }
    catch (final FileNotFoundException e1)
    {
      // This should never happen
      JavaCCErrors.semantic_error ("Could not open file " + file.getName () + " for writing.");
      throw new UncheckedIOException (e1);
    }
    catch (final IOException e2)
    {}
  }

  /**
   * Read the options line from the file and compare to the options currently in
   * use. Output a warning if they are different.
   *
   * @param fileName
   * @param options
   */
  private void checkOptions (final File file, final String [] options)
  {
    try
    {
      final BufferedReader reader = new BufferedReader (new FileReader (file));

      String line;
      while ((line = reader.readLine ()) != null)
      {
        if (line.startsWith ("/* JavaCCOptions:"))
        {
          final String currentOptions = Options.getOptionsString (options);
          if (line.indexOf (currentOptions) == -1)
          {
            JavaCCErrors.warning (file.getName () +
                                  ": Generated using incompatible options. Please rename or delete this file so" +
                                  " that a new one can be generated for you.");
          }
          return;
        }
      }
    }
    catch (final FileNotFoundException e1)
    {
      // This should never happen
      JavaCCErrors.semantic_error ("Could not open file " + file.getName () + " for writing.");
      throw new UncheckedIOException (e1);
    }
    catch (final IOException e2)
    {}

    // Not found so cannot check
  }

  /**
   * Return a PrintWriter object that may be used to write to this file. Any
   * necessary header information is written by this method.
   *
   * @return
   * @throws IOException
   */
  @WillCloseWhenClosed
  public PrintWriter getPrintWriter () throws IOException
  {
    if (m_pw == null)
    {
      MessageDigest digest = EMessageDigestAlgorithm.MD5.createMessageDigest ();
      try
      {
        digest = MessageDigest.getInstance ("MD5");
      }
      catch (final NoSuchAlgorithmException e)
      {
        throw new IOException ("No MD5 implementation", e);
      }
      dos = new DigestOutputStream (new NonBlockingBufferedOutputStream (new FileOutputStream (m_file)), digest);
      m_pw = new TrapClosePrintWriter (dos);

      // Write the headers....
      final String version = m_compatibleVersion == null ? PGVersion.versionNumber : m_compatibleVersion;
      m_pw.println ("/* " + JavaCCGlobals.getIdString (m_toolName, m_file.getName ()) + " Version " + version + " */");
      if (m_options != null)
      {
        m_pw.println ("/* JavaCCOptions:" + Options.getOptionsString (m_options) + " */");
      }
    }

    return m_pw;
  }

  /**
   * Close the OutputFile, writing any necessary trailer information (such as a
   * checksum).
   */
  public void close ()
  {
    // Write the trailer (checksum).
    // Possibly rename the .java.tmp to .java??
    if (m_pw != null)
    {
      m_pw.println (MD5_LINE_PART_1 + _getMD5sum () + MD5_LINE_PART_2);
      m_pw.closePrintWriter ();
      // file.renameTo(dest)
    }
  }

  private String _getMD5sum ()
  {
    m_pw.flush ();
    final byte [] digest = dos.getMessageDigest ().digest ();
    return toHexString (digest);
  }

  private final static char [] HEX_DIGITS = new char [] { '0',
                                                          '1',
                                                          '2',
                                                          '3',
                                                          '4',
                                                          '5',
                                                          '6',
                                                          '7',
                                                          '8',
                                                          '9',
                                                          'a',
                                                          'b',
                                                          'c',
                                                          'd',
                                                          'e',
                                                          'f' };

  private static final String toHexString (final byte [] bytes)
  {
    final StringBuilder sb = new StringBuilder (bytes.length * 2);
    for (final byte b : bytes)
    {
      sb.append (HEX_DIGITS[(b & 0xF0) >> 4]).append (HEX_DIGITS[b & 0x0F]);
    }
    return sb.toString ();
  }

  private final class TrapClosePrintWriter extends PrintWriter
  {
    public TrapClosePrintWriter (final OutputStream os)
    {
      super (os);
    }

    void closePrintWriter ()
    {
      super.close ();
    }

    @Override
    public void close ()
    {
      OutputFile.this.close ();
    }
  }

  /**
   * @return the toolName
   */
  public String getToolName ()
  {
    return m_toolName;
  }

  /**
   * @param toolName
   *        the toolName to set
   */
  public void setToolName (final String toolName)
  {
    this.m_toolName = toolName;
  }

  public String getPath ()
  {
    return m_file.getAbsolutePath ();
  }
}
