package com.helger.pgcc.parser;

/**
 * Definitions of constants that identify the kind of regular expression
 * production this is.
 *
 * @author Philip Helger
 * @since 1.1.0
 */
public enum ETokenKind
{
  TOKEN,
  SKIP,
  MORE,
  SPECIAL;

  public String getImage ()
  {
    return name ();
  }
}
