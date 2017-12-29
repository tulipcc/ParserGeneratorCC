package com.helger.pgcc.parser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Various constants relating to possible values for certain options
 */

public enum ELanguage implements IHasID <String>
{
  JAVA ("java"),
  CPP ("c++");

  private final String m_sID;

  private ELanguage (@Nonnull @Nonempty final String sID)
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

  @Nullable
  public static ELanguage getFromIDCaseInsensitiveOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDCaseInsensitiveOrNull (ELanguage.class, sID);
  }
}