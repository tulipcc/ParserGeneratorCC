/* Copyright (c) 2006, Sun Microsystems, Inc.
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
package com.helger.pgcc.parser;

/**
 * Output error messages and keep track of totals.
 */
public final class JavaCCErrors
{
  private static int s_parse_error_count = 0;
  private static int s_semantic_error_count = 0;
  private static int s_warning_count = 0;

  private JavaCCErrors ()
  {}

  private static void printLocationInfo (final Object node)
  {
    if (node instanceof NormalProduction)
    {
      final NormalProduction n = (NormalProduction) node;
      System.err.print ("Line " + n.getLine () + ", Column " + n.getColumn () + ": ");
    }
    else
      if (node instanceof TokenProduction)
      {
        final TokenProduction n = (TokenProduction) node;
        System.err.print ("Line " + n.getLine () + ", Column " + n.getColumn () + ": ");
      }
      else
        if (node instanceof Expansion)
        {
          final Expansion n = (Expansion) node;
          System.err.print ("Line " + n.getLine () + ", Column " + n.getColumn () + ": ");
        }
        else
          if (node instanceof CharacterRange)
          {
            final CharacterRange n = (CharacterRange) node;
            System.err.print ("Line " + n.getLine () + ", Column " + n.getColumn () + ": ");
          }
          else
            if (node instanceof SingleCharacter)
            {
              final SingleCharacter n = (SingleCharacter) node;
              System.err.print ("Line " + n.getLine () + ", Column " + n.getColumn () + ": ");
            }
            else
              if (node instanceof Token)
              {
                final Token t = (Token) node;
                System.err.print ("Line " + t.beginLine + ", Column " + t.beginColumn + ": ");
              }
  }

  public static void parse_error (final Object node, final String mess)
  {
    System.err.print ("Error: ");
    printLocationInfo (node);
    System.err.println (mess);
    s_parse_error_count++;
  }

  public static void parse_error (final String mess)
  {
    System.err.print ("Error: ");
    System.err.println (mess);
    s_parse_error_count++;
  }

  public static int get_parse_error_count ()
  {
    return s_parse_error_count;
  }

  public static void semantic_error (final Object node, final String mess)
  {
    System.err.print ("Error: ");
    printLocationInfo (node);
    System.err.println (mess);
    s_semantic_error_count++;
  }

  public static void semantic_error (final String mess)
  {
    System.err.print ("Error: ");
    System.err.println (mess);
    s_semantic_error_count++;
  }

  public static int get_semantic_error_count ()
  {
    return s_semantic_error_count;
  }

  public static void warning (final Object node, final String mess)
  {
    System.err.print ("Warning: ");
    printLocationInfo (node);
    System.err.println (mess);
    s_warning_count++;
  }

  public static void warning (final String mess)
  {
    System.err.print ("Warning: ");
    System.err.println (mess);
    s_warning_count++;
  }

  public static int getWarningCount ()
  {
    return s_warning_count;
  }

  public static int getErrorCount ()
  {
    return s_parse_error_count + s_semantic_error_count;
  }

  public static void reInit ()
  {
    s_parse_error_count = 0;
    s_semantic_error_count = 0;
    s_warning_count = 0;
  }

  public static void fatal (final String message)
  {
    System.err.println ("Fatal Error: " + message);
    throw new RuntimeException ("Fatal Error: " + message);
  }
}