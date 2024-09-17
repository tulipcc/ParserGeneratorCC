/* Generated by: ParserGeneratorCC: Do not edit this line. FaqConstants.java */

/**
 * Token literal values and constants.
 * Generated by com.helger.pgcc.output.java.OtherFilesGenJava#start()
 */
public interface FaqConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int EOL = 1;
  /** RegularExpression Id. */
  int TWOEOLS = 2;
  /** RegularExpression Id. */
  int NOT_EOL = 3;
  /** RegularExpression Id. */
  int _TWOEOLS = 6;
  /** RegularExpression Id. */
  int SUBJECT = 11;
  /** RegularExpression Id. */
  int _EOL1 = 12;
  /** RegularExpression Id. */
  int FROM = 13;
  /** RegularExpression Id. */
  int _EOL2 = 14;
  /** RegularExpression Id. */
  int DATE = 15;
  /** RegularExpression Id. */
  int _EOL3 = 16;
  /** RegularExpression Id. */
  int BODY = 17;
  /** RegularExpression Id. */
  int END = 18;

  /** Lexical state. */
  int DEFAULT = 0;
  /** Lexical state. */
  int MAILHEADER = 1;
  /** Lexical state. */
  int MAILSUBJECT = 2;
  /** Lexical state. */
  int MAILFROM = 3;
  /** Lexical state. */
  int MAILDATE = 4;
  /** Lexical state. */
  int MAILBODY = 5;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "<EOL>",
    "<TWOEOLS>",
    "<NOT_EOL>",
    "<token of kind 4>",
    "<token of kind 5>",
    "<_TWOEOLS>",
    "\"Subject: \"",
    "\"From: \"",
    "\"Date: \"",
    "<token of kind 10>",
    "<SUBJECT>",
    "<_EOL1>",
    "<FROM>",
    "<_EOL2>",
    "<DATE>",
    "<_EOL3>",
    "<BODY>",
    "\"\\u001f\"",
  };

}
