/* Generated by: ParserGeneratorCC: Do not edit this line. SimpleCharStream.java Version 2.0 */
/* ParserGeneratorCCOptions:SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
/**
 * An implementation of interface CharStream, where the stream is assumed to
 * contain only ASCII characters (without unicode processing).
 */
public
class SimpleCharStream extends AbstractCharStream
{
  private java.io.Reader m_aIS;

  /** Constructor. */
  public SimpleCharStream(final java.io.Reader dstream,
                          final int startline,
                          final int startcolumn, 
                          final int buffersize)
  {
    super (startline, startcolumn, buffersize);
    m_aIS = dstream;
  }

  /** Constructor. */
  public SimpleCharStream(final java.io.Reader dstream,
                          final int startline,
                          final int startcolumn)
  {
    this(dstream, startline, startcolumn, DEFAULT_BUF_SIZE);
  }

  /** Constructor. */
  public SimpleCharStream(final java.io.Reader dstream)
  {
    this(dstream, 1, 1, DEFAULT_BUF_SIZE);
  }

  /** Reinitialise. */
  public void reInit(final java.io.Reader dstream, 
                     final int startline,
                     final int startcolumn, 
                     final int buffersize)
  {
    m_aIS = dstream;
    super.reInit (startline, startcolumn, buffersize);
  }

  /** Reinitialise. */
  public void reInit(final java.io.Reader dstream, 
                     final int startline,
                     final int startcolumn)
  {
    reInit(dstream, startline, startcolumn, DEFAULT_BUF_SIZE);
  }

  /** Reinitialise. */
  public void reInit(final java.io.Reader dstream)
  {
    reInit(dstream, 1, 1, DEFAULT_BUF_SIZE);
  }
  
  /** Constructor. */
  public SimpleCharStream(final java.io.InputStream dstream, 
                          final String encoding, 
                          final int startline,
                          final int startcolumn,
                          final int buffersize) throws java.io.UnsupportedEncodingException
  {
    this(new java.io.InputStreamReader(dstream, encoding), startline, startcolumn, buffersize);
  }

  /** Constructor. */
  public SimpleCharStream(final java.io.InputStream dstream,
                          final String encoding, 
                          final int startline,
                          final int startcolumn) throws java.io.UnsupportedEncodingException
  {
    this(dstream, encoding, startline, startcolumn, DEFAULT_BUF_SIZE);
  }

  /** Constructor. */
  public SimpleCharStream(final java.io.InputStream dstream, 
                          final String encoding) throws java.io.UnsupportedEncodingException
  {
    this(dstream, encoding, 1, 1, DEFAULT_BUF_SIZE);
  }

  /** Reinitialise. */
  public void reInit(final java.io.InputStream dstream, 
                     final String encoding) throws java.io.UnsupportedEncodingException
  {
    reInit(dstream, encoding, 1, 1, DEFAULT_BUF_SIZE);
  }

  /** Reinitialise. */
  public void reInit(final java.io.InputStream dstream, 
                     final String encoding, 
                     final int startline,
                     final int startcolumn) throws java.io.UnsupportedEncodingException
  {
    reInit(dstream, encoding, startline, startcolumn, DEFAULT_BUF_SIZE);
  }

  /** Reinitialise. */
  public void reInit(final java.io.InputStream dstream, 
                     final String encoding, 
                     final int startline,
                     final int startcolumn, 
                     final int buffersize) throws java.io.UnsupportedEncodingException
  {
    reInit(new java.io.InputStreamReader(dstream, encoding), startline, startcolumn, buffersize);
  }

  @Override
  protected int streamRead (final char[] aBuf, final int nOfs, final int nLen) throws java.io.IOException
  {
    return m_aIS.read (aBuf, nOfs, nLen); 
  }
  
  @Override
  protected void streamClose () throws java.io.IOException
  {
    if (m_aIS != null)
      m_aIS.close (); 
  }
}
/* ParserGeneratorCC - OriginalChecksum=a636d4e6c01360f1f8ff44ccc038e096 (do not edit this line) */