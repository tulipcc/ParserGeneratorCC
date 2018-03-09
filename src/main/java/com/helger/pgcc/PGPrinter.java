package com.helger.pgcc;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

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

  private static IPrinter msg = new PSPrinter (System.out, false);
  private static IPrinter err = new PSPrinter (System.err, false);

  public static void init (@Nonnull final IPrinter aPrinter)
  {
    init (aPrinter, aPrinter);
  }

  public static void init (@Nonnull final IPrinter aPrinterInfo, @Nonnull final IPrinter aPrinterError)
  {
    ValueEnforcer.notNull (aPrinterInfo, "PrinterInfo");
    ValueEnforcer.notNull (aPrinterError, "PrinterError");
    msg = aPrinterInfo;
    err = aPrinterError;
  }

  private PGPrinter ()
  {}

  public static void debug (@Nonnull final String sMsg)
  {
    msg.println (sMsg);
  }

  public static void info ()
  {
    msg.println (null);
  }

  public static void info (@Nonnull final String sMsg)
  {
    msg.println (sMsg);
  }

  public static void warn (@Nonnull final String sMsg)
  {
    warn (sMsg, null);
  }

  public static void warn (@Nonnull final String sMsg, @Nullable final Throwable t)
  {
    err.println (sMsg);
    if (t != null)
      err.println (StackTraceHelper.getStackAsString (t));
  }

  public static void error (@Nonnull final String sMsg)
  {
    error (sMsg, null);
  }

  public static void error (@Nonnull final String sMsg, @Nullable final Throwable t)
  {
    err.println (sMsg);
    if (t != null)
      err.println (StackTraceHelper.getStackAsString (t));
  }

  public static void flush ()
  {
    msg.flush ();
    err.flush ();
  }

  public static void close () throws Exception
  {
    msg.close ();
    err.close ();
  }

  @Nonnull
  public static PrintWriter getOutWriter ()
  {
    return new PrintWriter (new OutputStreamWriter (System.out));
  }

  @Nonnull
  public static PrintWriter getErrWriter ()
  {
    return new PrintWriter (new OutputStreamWriter (System.err));
  }
}
