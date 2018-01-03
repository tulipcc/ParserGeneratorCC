package com.helger.pgcc.output.java;

public final class JavaModernResourceTemplateLocationImpl implements IJavaResourceTemplateLocations
{
  public String getTokenMgrErrorTemplateResourceUrl ()
  {
    // Same as Java
    return "/templates/TokenMgrError.template";
  }

  public String getCharStreamTemplateResourceUrl ()
  {
    // Same as Java
    return "/templates/CharStream.template";
  }

  public String getTokenManagerTemplateResourceUrl ()
  {
    // Same as Java
    return "/templates/TokenManager.template";
  }

  public String getTokenTemplateResourceUrl ()
  {
    // Same as Java
    return "/templates/Token.template";
  }

  public String getSimpleCharStreamTemplateResourceUrl ()
  {
    return "/templates/gwt/SimpleCharStream.template";
  }

  public String getJavaCharStreamTemplateResourceUrl ()
  {
    return "/templates/gwt/JavaCharStream.template";
  }

  public String getParseExceptionTemplateResourceUrl ()
  {
    return "/templates/gwt/ParseException.template";
  }
}
