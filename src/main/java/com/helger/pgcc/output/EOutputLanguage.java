package com.helger.pgcc.output;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Various constants relating to possible values for certain options
 */

public enum EOutputLanguage implements IHasID <String>
{
  JAVA ("java"),
  CPP ("c++");

  private final String m_sID;

  private EOutputLanguage (@Nonnull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  public boolean isJava ()
  {
    return this == JAVA;
  }

  public boolean isCpp ()
  {
    return this == CPP;
  }

  public boolean hasStaticsFile ()
  {
    return this == CPP;
  }

  public boolean hasIncludeFile ()
  {
    return this == CPP;
  }

  @Nullable
  public static EOutputLanguage getFromIDCaseInsensitiveOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDCaseInsensitiveOrNull (EOutputLanguage.class, sID);
  }
}
