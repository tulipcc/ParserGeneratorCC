package com.helger.pgcc.output;

import javax.annotation.Nonnull;

public class UnsupportedOutputLanguageException extends RuntimeException
{
  public UnsupportedOutputLanguageException (@Nonnull final EOutputLanguage eOutputLanguage)
  {
    super ("Unsupported output language: " + eOutputLanguage);
  }
}
