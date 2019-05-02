/**
 * Copyright 2017-2019 Philip Helger, pgcc@helger.com
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
package com.helger.pgcc.parser;

import javax.annotation.Nonnull;

/**
 * Describes character range descriptors in a character list.
 */
public class CharacterRange implements ICCCharacter
{
  /**
   * The line and column number of the construct that corresponds most closely
   * to this node.
   */
  private int m_nColumn;

  private int m_nLine;

  /**
   * The leftmost and the rightmost characters in this character range.
   */
  private char m_nRight;

  private char m_nLeft;

  CharacterRange (final char l, final char r)
  {
    if (l > r)
      JavaCCErrors.semantic_error (this,
                                   "Invalid range : \"" +
                                         (int) l +
                                         "\" - \"" +
                                         (int) r +
                                         "\". First character shoud be less than or equal to the second one in a range.");

    setLeft (l);
    setRight (r);
  }

  /**
   * @param line
   *        the line to set
   */
  void setLine (final int line)
  {
    m_nLine = line;
  }

  /**
   * @return the line
   */
  int getLine ()
  {
    return m_nLine;
  }

  /**
   * @param column
   *        the column to set
   */
  void setColumn (final int column)
  {
    m_nColumn = column;
  }

  /**
   * @return the column
   */
  int getColumn ()
  {
    return m_nColumn;
  }

  /**
   * @param left
   *        the left to set
   */
  public void setLeft (final char left)
  {
    m_nLeft = left;
  }

  /**
   * @return the left
   */
  public char getLeft ()
  {
    return m_nLeft;
  }

  /**
   * @param right
   *        the right to set
   */
  public void setRight (final char right)
  {
    m_nRight = right;
  }

  /**
   * @return the right
   */
  public char getRight ()
  {
    return m_nRight;
  }

  public boolean isInRange (final char c)
  {
    return c >= m_nLeft && c <= m_nRight;
  }

  public boolean isSubRangeOf (@Nonnull final CharacterRange r2)
  {
    return m_nLeft >= r2.getLeft () && m_nRight <= r2.getRight ();
  }
}
