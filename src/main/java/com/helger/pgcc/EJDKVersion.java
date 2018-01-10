/**
 * Copyright 2017-2018 Philip Helger, pgcc@helger.com
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
package com.helger.pgcc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum EJDKVersion
{
  JDK_11 (1),
  JDK_12 (2),
  JDK_13 (3),
  JDK_14 (4),
  JDK_15 (5),
  JDK_16 (6),
  JDK_17 (7),
  JDK_18 (8),
  JDK_19 (9);

  private int m_nMajor;

  private EJDKVersion (final int nMajor)
  {
    m_nMajor = nMajor;
  }

  public boolean isNewerOrEqualsThan (@Nonnull final EJDKVersion aOther)
  {
    return m_nMajor >= aOther.m_nMajor;
  }

  public boolean isOlderThan (@Nonnull final EJDKVersion aOther)
  {
    return m_nMajor < aOther.m_nMajor;
  }

  private double _getAsDouble1x ()
  {
    return 1 + (m_nMajor / 10d);
  }

  @Nullable
  public static EJDKVersion getFromDoubleOrNull (final double dVersion)
  {
    if (dVersion >= 1.0 && dVersion <= 2)
    {
      // It's the 1.x writing => 1.6 = JDK 1.6
      for (final EJDKVersion e : values ())
        if (dVersion == e._getAsDouble1x ())
          return e;
      return null;
    }

    // It's the x writing => 6 = JDK 1.6
    final int nMajor = (int) dVersion;
    for (final EJDKVersion e : values ())
      if (nMajor == e.m_nMajor)
        return e;
    return null;
  }
}
