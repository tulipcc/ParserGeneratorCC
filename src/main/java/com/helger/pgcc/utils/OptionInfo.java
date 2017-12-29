package com.helger.pgcc.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.compare.IComparable;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;

/**
 * @author Chris Ainsley
 */
public class OptionInfo implements IComparable <OptionInfo>
{
  private final String m_name;
  private final EOptionType m_type;
  private final Comparable <?> m_default;

  public OptionInfo (@Nonnull final String name,
                     @Nonnull final EOptionType type,
                     @Nullable final Comparable <?> default1)
  {
    m_name = name;
    m_type = type;
    m_default = default1;
  }

  @Nonnull
  public String getName ()
  {
    return m_name;
  }

  @Nonnull
  public EOptionType getType ()
  {
    return m_type;
  }

  @Nullable
  public Comparable <?> getDefault ()
  {
    return m_default;
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_name).append (m_type).append (m_default).getHashCode ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final OptionInfo rhs = (OptionInfo) o;
    return m_name.equals (rhs.m_name) && m_type.equals (rhs.m_type) && EqualsHelper.equals (m_default, rhs.m_default);
  }

  @Override
  public int compareTo (final OptionInfo o)
  {
    return m_name.compareTo (o.m_name);
  }

}
