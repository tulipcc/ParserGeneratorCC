package com.helger.pgcc.output.java;

public final class JavaResourceTemplateLocationImpl implements IJavaResourceTemplateLocations
{
  public String getTokenTemplateResourceUrl ()
  {
    return "/templates/Token.template";
  }

  public String getTokenManagerTemplateResourceUrl ()
  {
    return "/templates/TokenManager.template";
  }

  public String getTokenMgrErrorTemplateResourceUrl ()
  {
    return "/templates/TokenMgrError.template";
  }

  public String getJavaCharStreamTemplateResourceUrl ()
  {
    return "/templates/JavaCharStream.template";
  }

  public String getCharStreamTemplateResourceUrl ()
  {
    return "/templates/CharStream.template";
  }

  public String getSimpleCharStreamTemplateResourceUrl ()
  {
    return "/templates/SimpleCharStream.template";
  }

  public String getParseExceptionTemplateResourceUrl ()
  {
    return "/templates/ParseException.template";
  }

}
