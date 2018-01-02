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
  JAVA ("java")
  {
    @Override
    public String getTypeLong ()
    {
      return "long";
    }

    @Override
    public String getLongValueSuffix ()
    {
      return "L";
    }

    @Override
    public String getTypeBoolean ()
    {
      return "boolean";
    }
  },
  CPP ("c++")
  {
    @Override
    public String getTypeLong ()
    {
      return "unsigned long long";
    }

    @Override
    public String getLongValueSuffix ()
    {
      return "ULL";
    }

    @Override
    public String getTypeBoolean ()
    {
      return "bool";
    }
  };

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

  /**
   * @return The native data type for "long" values.
   * @see #getLongValueSuffix()
   */
  @Nonnull
  @Nonempty
  public abstract String getTypeLong ();

  /**
   * @return The value suffix to be used for long values.
   * @see #getTypeLong()
   */
  @Nonnull
  @Nonempty
  public abstract String getLongValueSuffix ();

  /**
   * @return The native data type for "boolean" values.
   */
  @Nonnull
  @Nonempty
  public abstract String getTypeBoolean ();

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
