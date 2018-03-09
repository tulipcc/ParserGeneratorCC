package com.helger.pgcc;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.string.StringHelper;

public class PGPrinter
{
  public static interface IPrinter extends AutoCloseable
  {
    void println (@Nonnull String s);

    void flush ();
  }

  public static final class PSPrinter implements IPrinter
  {
    private final PrintStream m_aPS;
    private final boolean m_bCanClose;

    public PSPrinter (@Nonnull final PrintStream aPS, final boolean bCanClose)
    {
      m_aPS = aPS;
      m_bCanClose = bCanClose;
    }

    public void close () throws IOException
    {
      if (m_bCanClose)
        m_aPS.close ();
    }

    public void println (@Nonnull final String s)
    {
      if (StringHelper.hasNoText (s))
        m_aPS.println ();
      else
        m_aPS.println (s);
    }

    public void flush ()
    {
      m_aPS.flush ();
    }
  }

  private static IPrinter s_aOut = new PSPrinter (System.out, false);
  private static IPrinter s_aErr = new PSPrinter (System.err, false);

  public static void init (@Nonnull final IPrinter aPrinter)
  {
    init (aPrinter, aPrinter);
  }

  public static void init (@Nonnull final IPrinter aPrinterInfo, @Nonnull final IPrinter aPrinterError)
  {
    ValueEnforcer.notNull (aPrinterInfo, "PrinterInfo");
    ValueEnforcer.notNull (aPrinterError, "PrinterError");
    s_aOut = aPrinterInfo;
    s_aErr = aPrinterError;
  }

  private PGPrinter ()
  {}

  public static void debug (@Nonnull final String sMsg)
  {
    s_aOut.println (sMsg);
  }

  public static void info ()
  {
    s_aOut.println (null);
  }

  public static void info (@Nonnull final String sMsg)
  {
    s_aOut.println (sMsg);
  }

  public static void warn (@Nonnull final String sMsg)
  {
    warn (sMsg, null);
  }

  public static void warn (@Nonnull final String sMsg, @Nullable final Throwable t)
  {
    s_aErr.println (sMsg);
    if (t != null)
      s_aErr.println (StackTraceHelper.getStackAsString (t));
  }

  public static void error (@Nonnull final String sMsg)
  {
    error (sMsg, null);
  }

  public static void error (@Nonnull final String sMsg, @Nullable final Throwable t)
  {
    s_aErr.println (sMsg);
    if (t != null)
      s_aErr.println (StackTraceHelper.getStackAsString (t));
  }

  public static void flush ()
  {
    s_aOut.flush ();
    s_aErr.flush ();
  }

  public static void close () throws Exception
  {
    s_aOut.close ();
    s_aErr.close ();
  }

  @Nonnull
  public static PrintWriter getOutWriter ()
  {
    return new PrintWriter (new OutputStreamWriter (System.out, Charset.defaultCharset ()));
  }

  @Nonnull
  public static PrintWriter getErrWriter ()
  {
    return new PrintWriter (new OutputStreamWriter (System.err, Charset.defaultCharset ()));
  }
}
