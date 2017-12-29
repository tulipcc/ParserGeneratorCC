package com.helger.pgcc.parser;

import javax.annotation.Nonnull;

public class UnsupportedLanguageException extends IllegalStateException
{
  public UnsupportedLanguageException (@Nonnull final ELanguage eLanguage)
  {
    super ("Unsupported output language " + eLanguage);
  }
}
