package com.helger.pgcc.parser;

import javax.annotation.Nonnull;

import com.helger.pgcc.output.EOutputLanguage;

public class UnsupportedLanguageException extends IllegalStateException
{
  public UnsupportedLanguageException (@Nonnull final EOutputLanguage eLanguage)
  {
    super ("Unsupported output language " + eLanguage);
  }
}
