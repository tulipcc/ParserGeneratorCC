/**
 * Copyright (c) 2006, Sun Microsystems, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2011 Google Inc. All Rights Reserved.
 * Author: sreeni@google.com (Sreeni Viswanadha)
 *
 * Copyright 2017-2018 Philip Helger, pgcc@helger.com
 */
// Copyright 2011 Google Inc. All Rights Reserved.
// Author: sreeni@google.com (Sreeni Viswanadha)

package com.helger.pgcc.parser;

import static com.helger.pgcc.parser.JavaCCGlobals.getFileExtension;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_from_insertion_point_2;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_name;
import static com.helger.pgcc.parser.JavaCCGlobals.s_cu_to_insertion_point_2;
import static com.helger.pgcc.parser.JavaCCGlobals.s_jj2index;
import static com.helger.pgcc.parser.JavaCCGlobals.s_jjtreeGenerated;
import static com.helger.pgcc.parser.JavaCCGlobals.s_lookaheadNeeded;
import static com.helger.pgcc.parser.JavaCCGlobals.s_maskVals;
import static com.helger.pgcc.parser.JavaCCGlobals.s_maskindex;
import static com.helger.pgcc.parser.JavaCCGlobals.s_tokenCount;
import static com.helger.pgcc.parser.JavaCCGlobals.s_toolName;
import static com.helger.pgcc.parser.JavaCCGlobals.s_toolNames;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Generate the parser.
 */
public class ParseGenCPP extends ParseGenJava
{

  @SuppressWarnings ("unchecked")
  public void start () throws MetaParseException
  {

    Token t = null;

    if (JavaCCErrors.getErrorCount () != 0)
      throw new MetaParseException ();

    final List <String> tn = new ArrayList <> (s_toolNames);
    tn.add (s_toolName);
    switchToStaticsFile ();

    switchToIncludeFile ();

    // standard includes
    genCodeLine ("#include \"JavaCC.h\"");
    genCodeLine ("#include \"CharStream.h\"");
    genCodeLine ("#include \"Token.h\"");
    genCodeLine ("#include \"TokenManager.h\"");

    final Object object = Options.objectValue (Options.USEROPTION__CPP_PARSER_INCLUDES);

    if (object instanceof String)
    {
      final String include = (String) object;
      if (include.length () > 0)
      {
        if (include.charAt (0) == '<')
          genCodeLine ("#include " + include);
        else
          genCodeLine ("#include \"" + include + "\"");
      }
    }
    else
      if (object instanceof List <?>)
      {
        for (final String include : (List <String>) object)
          if (include.length () > 0)
          {
            if (include.charAt (0) == '<')
              genCodeLine ("#include " + include);
            else
              genCodeLine ("#include \"" + include + "\"");
          }
      }

    genCodeLine ("#include \"" + s_cu_name + "Constants.h\"");

    if (s_jjtreeGenerated)
    {
      genCodeLine ("#include \"JJT" + s_cu_name + "State.h\"");
    }

    genCodeLine ("#include \"ErrorHandler.h\"");

    if (s_jjtreeGenerated)
    {
      genCodeLine ("#include \"" + s_cu_name + "Tree.h\"");
    }

    if (Options.stringValue (Options.USEROPTION__CPP_NAMESPACE).length () > 0)
    {
      genCodeLine ("namespace " + Options.stringValue ("NAMESPACE_OPEN"));
    }

    genCodeLine ("  struct JJCalls {");
    genCodeLine ("    int        gen;");
    genCodeLine ("    int        arg;");
    genCodeLine ("    JJCalls*   next;");
    genCodeLine ("    Token*     first;");
    genCodeLine ("    ~JJCalls() { if (next) delete next; }");
    genCodeLine ("     JJCalls() { next = nullptr; arg = 0; gen = -1; first = nullptr; }");
    genCodeLine ("  };");
    genCodeLine ("");

    final String superClass = Options.stringValue (Options.USEROPTION__PARSER_SUPER_CLASS);
    genClassStart ("",
                   s_cu_name,
                   new String [] {},
                   superClass == null ? new String [0] : new String [] { "public " + superClass });
    switchToMainFile ();
    if (s_cu_to_insertion_point_2.size () != 0)
    {
      printTokenSetup ((s_cu_to_insertion_point_2.get (0)));
      for (final Object name : s_cu_to_insertion_point_2)
      {
        t = (Token) name;
        printToken (t);
      }
    }

    switchToMainFile ();
    /*
     * genCodeLine("typedef class _LookaheadSuccess { } *LookaheadSuccess; // Dummy class"
     * );
     * genCodeLine("  static LookaheadSuccess jj_ls = new _LookaheadSuccess();"
     * );
     */

    genCodeLine ("");
    genCodeLine ("");

    new ParseEngine ().build (this);

    switchToIncludeFile ();
    genCodeLine ("");
    genCodeLine ("public: ");
    genCodeLine ("  void setErrorHandler(ErrorHandler *eh) {");
    genCodeLine ("    if (errorHandler) delete errorHandler;");
    genCodeLine ("    errorHandler = eh;");
    genCodeLine ("  }");
    genCodeLine ("");
    genCodeLine ("  TokenManager *token_source = nullptr;");
    genCodeLine ("  CharStream   *jj_input_stream = nullptr;");
    genCodeLine ("  /** Current token. */");
    genCodeLine ("  Token        *token = nullptr;");
    genCodeLine ("  /** Next token. */");
    genCodeLine ("  Token        *jj_nt = nullptr;");
    genCodeLine ("");
    genCodeLine ("private: ");
    genCodeLine ("  int           jj_ntk;");

    genCodeLine ("  JJCalls       jj_2_rtns[" + (s_jj2index + 1) + "];");
    genCodeLine ("  bool          jj_rescan;");
    genCodeLine ("  int           jj_gc;");
    genCodeLine ("  Token        *jj_scanpos, *jj_lastpos;");
    genCodeLine ("  int           jj_la;");
    genCodeLine ("  /** Whether we are looking ahead. */");
    genCodeLine ("  bool          jj_lookingAhead;");
    genCodeLine ("  bool          jj_semLA;");

    genCodeLine ("  int           jj_gen;");
    genCodeLine ("  int           jj_la1[" + (s_maskindex + 1) + "];");
    genCodeLine ("  ErrorHandler *errorHandler = nullptr;");
    genCodeLine ("");
    genCodeLine ("protected: ");
    genCodeLine ("  bool          hasError;");
    genCodeLine ("");
    final int tokenMaskSize = (s_tokenCount - 1) / 32 + 1;

    if (Options.getErrorReporting () && tokenMaskSize > 0)
    {
      switchToStaticsFile ();
      for (int i = 0; i < tokenMaskSize; i++)
      {
        if (s_maskVals.size () > 0)
        {
          genCodeLine ("  unsigned int jj_la1_" + i + "[] = {");
          for (final Object aElement : s_maskVals)
          {
            final int [] tokenMask = (int []) (aElement);
            genCode ("0x" + Integer.toHexString (tokenMask[i]) + ",");
          }
          genCodeLine ("};");
        }
      }
    }

    if (Options.getDepthLimit () > 0)
    {
      genCodeLine ("  private: int jj_depth;");
      genCodeLine ("  private: bool jj_depth_error;");
      genCodeLine ("  friend class __jj_depth_inc;");
      genCodeLine ("  class __jj_depth_inc {public:");
      genCodeLine ("    " + s_cu_name + "* parent;");
      genCodeLine ("    __jj_depth_inc(" + s_cu_name + "* p): parent(p) { parent->jj_depth++; };");
      genCodeLine ("    ~__jj_depth_inc(){ parent->jj_depth--; }");
      genCodeLine ("  };");
    }
    if (Options.hasCPPStackLimit ())
    {
      genCodeLine ("  public: size_t jj_stack_limit;");
      genCodeLine ("  private: void* jj_stack_base;");
      genCodeLine ("  private: bool jj_stack_error;");
    }

    genCodeLine ("");

    genCodeLine ("  /** Constructor with user supplied TokenManager. */");

    switchToIncludeFile (); // TEMP
    genCodeLine ("  Token *head; ");
    genCodeLine ("public: ");
    generateMethodDefHeader (" ", s_cu_name, s_cu_name + "(TokenManager *tokenManager)");
    if (superClass != null)
    {
      genCodeLine (" : " + superClass + "()");
    }
    genCodeLine ("{");
    genCodeLine ("    head = nullptr;");
    genCodeLine ("    ReInit(tokenManager);");
    if (Options.getTokenManagerUsesParser ())
      genCodeLine ("    tokenManager->setParser(this);");
    genCodeLine ("}");

    switchToIncludeFile ();
    genCodeLine ("  virtual ~" + s_cu_name + "();");
    switchToMainFile ();
    genCodeLine ("" + s_cu_name + "::~" + s_cu_name + "()");
    genCodeLine ("{");
    genCodeLine ("  clear();");
    genCodeLine ("}");
    generateMethodDefHeader ("void", s_cu_name, "ReInit(TokenManager* tokenManager)");
    genCodeLine ("{");
    genCodeLine ("    clear();");
    genCodeLine ("    errorHandler = new ErrorHandler();");
    genCodeLine ("    hasError = false;");
    genCodeLine ("    token_source = tokenManager;");
    genCodeLine ("    head = token = new Token();");
    genCodeLine ("    token->kind = 0;");
    genCodeLine ("    token->next = nullptr;");
    genCodeLine ("    jj_lookingAhead = false;");
    genCodeLine ("    jj_rescan = false;");
    genCodeLine ("    jj_done = false;");
    genCodeLine ("    jj_scanpos = jj_lastpos = nullptr;");
    genCodeLine ("    jj_gc = 0;");
    genCodeLine ("    jj_kind = -1;");
    genCodeLine ("    indent = 0;");
    genCodeLine ("    trace = " + Options.getDebugParser () + ";");
    if (Options.hasCPPStackLimit ())
    {
      genCodeLine ("    jj_stack_limit = " + Options.getCPPStackLimit () + ";");
      genCodeLine ("    jj_stack_error = jj_stack_check(true);");
    }

    if (Options.getCacheTokens ())
    {
      genCodeLine ("    token->next = jj_nt = token_source->getNextToken();");
    }
    else
    {
      genCodeLine ("    jj_ntk = -1;");
    }
    if (s_jjtreeGenerated)
    {
      genCodeLine ("    jjtree.reset();");
    }
    if (Options.getDepthLimit () > 0)
    {
      genCodeLine ("    jj_depth = 0;");
      genCodeLine ("    jj_depth_error = false;");
    }
    if (Options.getErrorReporting ())
    {
      genCodeLine ("    jj_gen = 0;");
      if (s_maskindex > 0)
      {
        genCodeLine ("    for (int i = 0; i < " + s_maskindex + "; i++) jj_la1[i] = -1;");
      }
    }
    genCodeLine ("  }");
    genCodeLine ("");

    generateMethodDefHeader ("void", s_cu_name, "clear()");
    genCodeLine ("{");
    genCodeLine ("  //Since token manager was generate from outside,");
    genCodeLine ("  //parser should not take care of deleting");
    genCodeLine ("  //if (token_source) delete token_source;");
    genCodeLine ("  if (head) {");
    genCodeLine ("    Token *next, *t = head;");
    genCodeLine ("    while (t) {");
    genCodeLine ("      next = t->next;");
    genCodeLine ("      delete t;");
    genCodeLine ("      t = next;");
    genCodeLine ("    }");
    genCodeLine ("  }");
    genCodeLine ("  if (errorHandler) {");
    genCodeLine ("    delete errorHandler, errorHandler = nullptr;");
    genCodeLine ("  }");
    if (Options.getDepthLimit () > 0)
    {
      genCodeLine ("  assert(jj_depth==0);");
    }
    genCodeLine ("}");
    genCodeLine ("");

    if (Options.hasCPPStackLimit ())
    {
      genCodeLine ("");
      switchToIncludeFile ();
      genCodeLine (" virtual");
      switchToMainFile ();
      generateMethodDefHeader ("bool ", s_cu_name, "jj_stack_check(bool init)");
      genCodeLine ("  {");
      genCodeLine ("     if(init) {");
      genCodeLine ("       jj_stack_base = nullptr;");
      genCodeLine ("       return false;");
      genCodeLine ("     } else {");
      genCodeLine ("       volatile int q = 0;");
      genCodeLine ("       if(!jj_stack_base) {");
      genCodeLine ("         jj_stack_base = (void*)&q;");
      genCodeLine ("         return false;");
      genCodeLine ("       } else {");
      genCodeLine ("         // Stack can grow in both directions, depending on arch");
      genCodeLine ("         std::ptrdiff_t used = (char*)jj_stack_base-(char*)&q;");
      genCodeLine ("         return (std::abs(used) > jj_stack_limit);");
      genCodeLine ("       }");
      genCodeLine ("     }");
      genCodeLine ("  }");
    }

    generateMethodDefHeader ("Token *", s_cu_name, "jj_consume_token(int kind)", "ParseException");
    genCodeLine ("  {");
    if (Options.hasCPPStackLimit ())
    {
      genCodeLine ("    if(kind != -1 && (jj_stack_error || jj_stack_check(false))) {");
      genCodeLine ("      if (!jj_stack_error) {");
      genCodeLine ("        errorHandler->handleOtherError(\"Stack overflow while trying to parse\", this);");
      genCodeLine ("        jj_stack_error=true;");
      genCodeLine ("      }");
      genCodeLine ("      return jj_consume_token(-1);");
      genCodeLine ("    }");
    }
    if (Options.getCacheTokens ())
    {
      genCodeLine ("    Token *oldToken = token;");
      genCodeLine ("    if ((token = jj_nt)->next != nullptr) jj_nt = jj_nt->next;");
      genCodeLine ("    else jj_nt = jj_nt->next = token_source->getNextToken();");
    }
    else
    {
      genCodeLine ("    Token *oldToken;");
      genCodeLine ("    if ((oldToken = token)->next != nullptr) token = token->next;");
      genCodeLine ("    else token = token->next = token_source->getNextToken();");
      genCodeLine ("    jj_ntk = -1;");
    }
    genCodeLine ("    if (token->kind == kind) {");
    if (Options.getErrorReporting ())
    {
      genCodeLine ("      jj_gen++;");
      if (s_jj2index != 0)
      {
        genCodeLine ("      if (++jj_gc > 100) {");
        genCodeLine ("        jj_gc = 0;");
        genCodeLine ("        for (int i = 0; i < " + s_jj2index + "; i++) {");
        genCodeLine ("          JJCalls *c = &jj_2_rtns[i];");
        genCodeLine ("          while (c != nullptr) {");
        genCodeLine ("            if (c->gen < jj_gen) c->first = nullptr;");
        genCodeLine ("            c = c->next;");
        genCodeLine ("          }");
        genCodeLine ("        }");
        genCodeLine ("      }");
      }
    }
    if (Options.getDebugParser ())
    {
      genCodeLine ("      trace_token(token, \"\");");
    }
    genCodeLine ("      return token;");
    genCodeLine ("    }");
    if (Options.getCacheTokens ())
    {
      genCodeLine ("    jj_nt = token;");
    }
    genCodeLine ("    token = oldToken;");
    if (Options.getErrorReporting ())
    {
      genCodeLine ("    jj_kind = kind;");
    }
    // genCodeLine(" throw generateParseException();");
    if (Options.hasCPPStackLimit ())
    {
      genCodeLine ("    if (!jj_stack_error) {");
    }
    genCodeLine ("    JJString image = kind >= 0 ? tokenImage[kind] : tokenImage[0];");
    genCodeLine ("    errorHandler->handleUnexpectedToken(kind, image.substr(1, image.size() - 2), getToken(1), this);");
    if (Options.hasCPPStackLimit ())
    {
      genCodeLine ("    }");
    }
    genCodeLine ("    hasError = true;");
    genCodeLine ("    return token;");
    genCodeLine ("  }");
    genCodeLine ("");

    if (s_jj2index != 0)
    {
      switchToMainFile ();
      generateMethodDefHeader ("bool ", s_cu_name, "jj_scan_token(int kind)");
      genCodeLine ("{");
      if (Options.hasCPPStackLimit ())
      {
        genCodeLine ("    if(kind != -1 && (jj_stack_error || jj_stack_check(false))) {");
        genCodeLine ("      if (!jj_stack_error) {");
        genCodeLine ("        errorHandler->handleOtherError(\"Stack overflow while trying to parse\", this);");
        genCodeLine ("        jj_stack_error=true;");
        genCodeLine ("      }");
        genCodeLine ("      return jj_consume_token(-1);");
        genCodeLine ("    }");
      }
      genCodeLine ("    if (jj_scanpos == jj_lastpos) {");
      genCodeLine ("      jj_la--;");
      genCodeLine ("      if (jj_scanpos->next == nullptr) {");
      genCodeLine ("        jj_lastpos = jj_scanpos = jj_scanpos->next = token_source->getNextToken();");
      genCodeLine ("      } else {");
      genCodeLine ("        jj_lastpos = jj_scanpos = jj_scanpos->next;");
      genCodeLine ("      }");
      genCodeLine ("    } else {");
      genCodeLine ("      jj_scanpos = jj_scanpos->next;");
      genCodeLine ("    }");
      if (Options.getErrorReporting ())
      {
        genCodeLine ("    if (jj_rescan) {");
        genCodeLine ("      int i = 0; Token *tok = token;");
        genCodeLine ("      while (tok != nullptr && tok != jj_scanpos) { i++; tok = tok->next; }");
        genCodeLine ("      if (tok != nullptr) jj_add_error_token(kind, i);");
        if (Options.getDebugLookahead ())
        {
          genCodeLine ("    } else {");
          genCodeLine ("      trace_scan(jj_scanpos, kind);");
        }
        genCodeLine ("    }");
      }
      else
        if (Options.getDebugLookahead ())
        {
          genCodeLine ("    trace_scan(jj_scanpos, kind);");
        }
      genCodeLine ("    if (jj_scanpos->kind != kind) return true;");
      // genCodeLine(" if (jj_la == 0 && jj_scanpos == jj_lastpos) throw
      // jj_ls;");
      genCodeLine ("    if (jj_la == 0 && jj_scanpos == jj_lastpos) { return jj_done = true; }");
      genCodeLine ("    return false;");
      genCodeLine ("  }");
      genCodeLine ("");
    }
    genCodeLine ("");
    genCodeLine ("/** Get the next Token. */");
    generateMethodDefHeader ("Token *", s_cu_name, "getNextToken()");
    genCodeLine ("{");
    if (Options.getCacheTokens ())
    {
      genCodeLine ("    if ((token = jj_nt)->next != nullptr) jj_nt = jj_nt->next;");
      genCodeLine ("    else jj_nt = jj_nt->next = token_source->getNextToken();");
    }
    else
    {
      genCodeLine ("    if (token->next != nullptr) token = token->next;");
      genCodeLine ("    else token = token->next = token_source->getNextToken();");
      genCodeLine ("    jj_ntk = -1;");
    }
    if (Options.getErrorReporting ())
    {
      genCodeLine ("    jj_gen++;");
    }
    if (Options.getDebugParser ())
    {
      genCodeLine ("      trace_token(token, \" (in getNextToken)\");");
    }
    genCodeLine ("    return token;");
    genCodeLine ("  }");
    genCodeLine ("");
    genCodeLine ("/** Get the specific Token. */");
    generateMethodDefHeader ("Token *", s_cu_name, "getToken(int index)");
    genCodeLine ("{");
    if (s_lookaheadNeeded)
    {
      genCodeLine ("    Token *t = jj_lookingAhead ? jj_scanpos : token;");
    }
    else
    {
      genCodeLine ("    Token *t = token;");
    }
    genCodeLine ("    for (int i = 0; i < index; i++) {");
    genCodeLine ("      if (t->next != nullptr) t = t->next;");
    genCodeLine ("      else t = t->next = token_source->getNextToken();");
    genCodeLine ("    }");
    genCodeLine ("    return t;");
    genCodeLine ("  }");
    genCodeLine ("");
    if (!Options.getCacheTokens ())
    {
      generateMethodDefHeader ("int", s_cu_name, "jj_ntk_f()");
      genCodeLine ("{");

      genCodeLine ("    if ((jj_nt=token->next) == nullptr)");
      genCodeLine ("      return (jj_ntk = (token->next=token_source->getNextToken())->kind);");
      genCodeLine ("    else");
      genCodeLine ("      return (jj_ntk = jj_nt->kind);");
      genCodeLine ("  }");
      genCodeLine ("");
    }

    switchToIncludeFile ();
    genCodeLine ("private:");
    genCodeLine ("  int jj_kind;");
    if (Options.getErrorReporting ())
    {
      genCodeLine ("  int **jj_expentries;");
      genCodeLine ("  int *jj_expentry;");
      if (s_jj2index != 0)
      {
        switchToStaticsFile ();
        // For now we don't support ERROR_REPORTING in the C++ version.
        // genCodeLine(" static int *jj_lasttokens = new int[100];");
        // genCodeLine(" static int jj_endpos;");
        genCodeLine ("");

        generateMethodDefHeader ("  void", s_cu_name, "jj_add_error_token(int kind, int pos)");
        genCodeLine ("  {");
        // For now we don't support ERROR_REPORTING in the C++ version.

        // genCodeLine(" if (pos >= 100) return;");
        // genCodeLine(" if (pos == jj_endpos + 1) {");
        // genCodeLine(" jj_lasttokens[jj_endpos++] = kind;");
        // genCodeLine(" } else if (jj_endpos != 0) {");
        // genCodeLine(" jj_expentry = new int[jj_endpos];");
        // genCodeLine(" for (int i = 0; i < jj_endpos; i++) {");
        // genCodeLine(" jj_expentry[i] = jj_lasttokens[i];");
        // genCodeLine(" }");
        // genCodeLine(" jj_entries_loop: for (java.util.Iterator it =
        // jj_expentries.iterator(); it.hasNext();) {");
        // genCodeLine(" int[] oldentry = (int[])(it->next());");
        // genCodeLine(" if (oldentry.length == jj_expentry.length) {");
        // genCodeLine(" for (int i = 0; i < jj_expentry.length; i++) {");
        // genCodeLine(" if (oldentry[i] != jj_expentry[i]) {");
        // genCodeLine(" continue jj_entries_loop;");
        // genCodeLine(" }");
        // genCodeLine(" }");
        // genCodeLine(" jj_expentries.add(jj_expentry);");
        // genCodeLine(" break jj_entries_loop;");
        // genCodeLine(" }");
        // genCodeLine(" }");
        // genCodeLine(" if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] =
        // kind;");
        // genCodeLine(" }");
        genCodeLine ("  }");
      }
      genCodeLine ("");

      switchToIncludeFile ();
      genCodeLine ("protected:");
      genCodeLine ("  /** Generate ParseException. */");
      generateMethodDefHeader ("  virtual void ", s_cu_name, "parseError()");
      genCodeLine ("   {");
      if (Options.getErrorReporting ())
      {
        genCodeLine ("      fprintf(stderr, \"Parse error at: %d:%d, after token: %s encountered: %s\\n\", token->beginLine, token->beginColumn, addUnicodeEscapes(token->image).c_str(), addUnicodeEscapes(getToken(1)->image).c_str());");
      }
      genCodeLine ("   }");
      /*
       * generateMethodDefHeader("ParseException", cu_name,
       * "generateParseException()"); genCodeLine("   {");
       * //genCodeLine("    jj_expentries.clear();");
       * //genCodeLine("    bool[] la1tokens = new boolean[" + tokenCount +
       * "];"); //genCodeLine("    if (jj_kind >= 0) {");
       * //genCodeLine("      la1tokens[jj_kind] = true;");
       * //genCodeLine("      jj_kind = -1;"); //genCodeLine("    }");
       * //genCodeLine("    for (int i = 0; i < " + maskindex + "; i++) {");
       * //genCodeLine("      if (jj_la1[i] == jj_gen) {");
       * //genCodeLine("        for (int j = 0; j < 32; j++) {"); //for (int i =
       * 0; i < (tokenCount-1)/32 + 1; i++) {
       * //genCodeLine("          if ((jj_la1_" + i + "[i] & (1<<j)) != 0) {");
       * //genCode("            la1tokens["); //if (i != 0) { //genCode((32*i) +
       * "+"); //} //genCodeLine("j] = true;"); //genCodeLine("          }");
       * //} //genCodeLine("        }"); //genCodeLine("      }");
       * //genCodeLine("    }"); //genCodeLine("    for (int i = 0; i < " +
       * tokenCount + "; i++) {"); //genCodeLine("      if (la1tokens[i]) {");
       * //genCodeLine("        jj_expentry = new int[1];");
       * //genCodeLine("        jj_expentry[0] = i;");
       * //genCodeLine("        jj_expentries.add(jj_expentry);");
       * //genCodeLine("      }"); //genCodeLine("    }"); //if (jj2index != 0)
       * { //genCodeLine("    jj_endpos = 0;");
       * //genCodeLine("    jj_rescan_token();");
       * //genCodeLine("    jj_add_error_token(0, 0);"); //}
       * //genCodeLine("    int exptokseq[][1] = new int[1];");
       * //genCodeLine("    for (int i = 0; i < jj_expentries.size(); i++) {");
       * //if (!Options.getGenerateGenerics())
       * //genCodeLine("      exptokseq[i] = (int[])jj_expentries.get(i);");
       * //else //genCodeLine("      exptokseq[i] = jj_expentries.get(i);");
       * //genCodeLine("    }");
       * genCodeLine("    return new _ParseException();");//token, nullptr,
       * tokenImage);"); genCodeLine(" }");
       */
    }
    else
    {
      genCodeLine ("protected:");
      genCodeLine ("  /** Generate ParseException. */");
      generateMethodDefHeader ("virtual void ", s_cu_name, "parseError()");
      genCodeLine ("   {");
      if (Options.getErrorReporting ())
      {
        genCodeLine ("      fprintf(stderr, \"Parse error at: %d:%d, after token: %s encountered: %s\\n\", token->beginLine, token->beginColumn, addUnicodeEscapes(token->image).c_str(), addUnicodeEscapes(getToken(1)->image).c_str());");
      }
      genCodeLine ("   }");
      /*
       * generateMethodDefHeader("ParseException", cu_name,
       * "generateParseException()"); genCodeLine("   {");
       * genCodeLine("    Token *errortok = token->next;"); if
       * (Options.getKeepLineColumn())
       * genCodeLine("    int line = errortok.beginLine, column = errortok.beginColumn;"
       * );
       * genCodeLine("    JJString mess = (errortok->kind == 0) ? tokenImage[0] : errortok->image;"
       * ); if (Options.getKeepLineColumn())
       * genCodeLine("    return new _ParseException();");// +
       * //"\"Parse error at line \" + line + \", column \" + column + \".  " +
       * //"Encountered: \" + mess);"); else
       * genCodeLine("    return new _ParseException();");//
       * \"Parse error at <unknown location>.  " +
       * //"Encountered: \" + mess);"); genCodeLine("  }");
       */
    }
    genCodeLine ("");

    switchToIncludeFile ();
    genCodeLine ("private:");
    genCodeLine ("  int  indent;	// trace indentation");
    genCodeLine ("  bool trace = " + Options.getDebugParser () + "; // trace enabled if true");
    genCodeLine ("");
    genCodeLine ("public:");
    generateMethodDefHeader ("  bool", s_cu_name, "trace_enabled()");
    genCodeLine ("  {");
    genCodeLine ("    return trace;");
    genCodeLine ("  }");
    genCodeLine ("");
    if (Options.getDebugParser ())
    {
      switchToIncludeFile ();
      generateMethodDefHeader ("  void", s_cu_name, "enable_tracing()");
      genCodeLine ("{");
      genCodeLine ("    trace = true;");
      genCodeLine ("}");
      genCodeLine ("");

      switchToIncludeFile ();
      generateMethodDefHeader ("  void", s_cu_name, "disable_tracing()");
      genCodeLine ("{");
      genCodeLine ("    trace = false;");
      genCodeLine ("}");
      genCodeLine ("");

      switchToIncludeFile ();
      generateMethodDefHeader ("  void", s_cu_name, "trace_call(const char *s)");
      genCodeLine ("  {");
      genCodeLine ("    if (trace_enabled()) {");
      genCodeLine ("      for (int i = 0; i < indent; i++) { printf(\" \"); }");
      genCodeLine ("      printf(\"Call:   %s\\n\", s);");
      genCodeLine ("    }");
      genCodeLine ("    indent = indent + 2;");
      genCodeLine ("  }");
      genCodeLine ("");

      switchToIncludeFile ();
      generateMethodDefHeader ("  void", s_cu_name, "trace_return(const char *s)");
      genCodeLine ("  {");
      genCodeLine ("    indent = indent - 2;");
      genCodeLine ("    if (trace_enabled()) {");
      genCodeLine ("      for (int i = 0; i < indent; i++) { printf(\" \"); }");
      genCodeLine ("      printf(\"Return: %s\\n\", s);");
      genCodeLine ("    }");
      genCodeLine ("  }");
      genCodeLine ("");

      switchToIncludeFile ();
      generateMethodDefHeader ("  void", s_cu_name, "trace_token(Token *t, const char *where)");
      genCodeLine ("  {");
      genCodeLine ("    if (trace_enabled()) {");
      genCodeLine ("      for (int i = 0; i < indent; i++) { printf(\" \"); }");
      genCodeLine ("      printf(\"Consumed token: <kind: %d(%s), \\\"%s\\\"\", t->kind, addUnicodeEscapes(tokenImage[t->kind]).c_str(), addUnicodeEscapes(t->image).c_str());");
      // genCodeLine(" if (t->kind != 0 && !tokenImage[t->kind].equals(\"\\\"\"
      // + t->image + \"\\\"\")) {");
      // genCodeLine(" System.out.print(\": \\\"\" + t->image + \"\\\"\");");
      // genCodeLine(" }");
      genCodeLine ("      printf(\" at line %d column %d> %s\\n\", t->beginLine, t->beginColumn, where);");
      genCodeLine ("    }");
      genCodeLine ("  }");
      genCodeLine ("");

      switchToIncludeFile ();
      generateMethodDefHeader ("  void", s_cu_name, "trace_scan(Token *t1, int t2)");
      genCodeLine ("  {");
      genCodeLine ("    if (trace_enabled()) {");
      genCodeLine ("      for (int i = 0; i < indent; i++) { printf(\" \"); }");
      genCodeLine ("      printf(\"Visited token: <Kind: %d(%s), \\\"%s\\\"\", t1->kind, addUnicodeEscapes(tokenImage[t1->kind]).c_str(), addUnicodeEscapes(t1->image).c_str());");
      // genCodeLine(" if (t1->kind != 0 &&
      // !tokenImage[t1->kind].equals(\"\\\"\" + t1->image + \"\\\"\")) {");
      // genCodeLine(" System.out.print(\": \\\"\" + t1->image + \"\\\"\");");
      // genCodeLine(" }");
      genCodeLine ("      printf(\" at line %d column %d>; Expected token: %s\\n\", t1->beginLine, t1->beginColumn, addUnicodeEscapes(tokenImage[t2]).c_str());");
      genCodeLine ("    }");
      genCodeLine ("  }");
      genCodeLine ("");
    }
    else
    {
      switchToIncludeFile ();
      generateMethodDefHeader ("  void", s_cu_name, "enable_tracing()");
      genCodeLine ("  {");
      genCodeLine ("  }");
      switchToIncludeFile ();
      generateMethodDefHeader ("  void", s_cu_name, "disable_tracing()");
      genCodeLine ("  {");
      genCodeLine ("  }");
      genCodeLine ("");
    }

    if (s_jj2index != 0 && Options.getErrorReporting ())
    {
      generateMethodDefHeader ("  void", s_cu_name, "jj_rescan_token()");
      genCodeLine ("{");
      genCodeLine ("    jj_rescan = true;");
      genCodeLine ("    for (int i = 0; i < " + s_jj2index + "; i++) {");
      // genCodeLine(" try {");
      genCodeLine ("      JJCalls *p = &jj_2_rtns[i];");
      genCodeLine ("      do {");
      genCodeLine ("        if (p->gen > jj_gen) {");
      genCodeLine ("          jj_la = p->arg; jj_lastpos = jj_scanpos = p->first;");
      genCodeLine ("          switch (i) {");
      for (int i = 0; i < s_jj2index; i++)
      {
        genCodeLine ("            case " + i + ": jj_3_" + (i + 1) + "(); break;");
      }
      genCodeLine ("          }");
      genCodeLine ("        }");
      genCodeLine ("        p = p->next;");
      genCodeLine ("      } while (p != nullptr);");
      // genCodeLine(" } catch(LookaheadSuccess ls) { }");
      genCodeLine ("    }");
      genCodeLine ("    jj_rescan = false;");
      genCodeLine ("  }");
      genCodeLine ("");

      generateMethodDefHeader ("  void", s_cu_name, "jj_save(int index, int xla)");
      genCodeLine ("{");
      genCodeLine ("    JJCalls *p = &jj_2_rtns[index];");
      genCodeLine ("    while (p->gen > jj_gen) {");
      genCodeLine ("      if (p->next == nullptr) { p = p->next = new JJCalls(); break; }");
      genCodeLine ("      p = p->next;");
      genCodeLine ("    }");
      genCodeLine ("    p->gen = jj_gen + xla - jj_la; p->first = token; p->arg = xla;");
      genCodeLine ("  }");
      genCodeLine ("");
    }

    if (s_cu_from_insertion_point_2.size () != 0)
    {
      printTokenSetup ((s_cu_from_insertion_point_2.get (0)));
      m_ccol = 1;
      for (final Object name : s_cu_from_insertion_point_2)
      {
        t = (Token) name;
        printToken (t);
      }
      printTrailingComments (t);
    }
    genCodeLine ("");

    // in the include file close the class signature
    switchToIncludeFile ();

    // copy other stuff
    Token t1 = JavaCCGlobals.s_otherLanguageDeclTokenBeg;
    final Token t2 = JavaCCGlobals.s_otherLanguageDeclTokenEnd;
    while (t1 != t2)
    {
      printToken (t1);
      t1 = t1.next;
    }
    genCodeLine ("\n");
    if (s_jjtreeGenerated)
    {
      genCodeLine ("  JJT" + s_cu_name + "State jjtree;");
    }
    genCodeLine ("private:");
    genCodeLine ("  bool jj_done;");

    genCodeLine ("};");

    saveOutput (Options.getOutputDirectory () + File.separator + s_cu_name + getFileExtension ());
  }

  public static void reInit ()
  {
    s_lookaheadNeeded = false;
  }

}
