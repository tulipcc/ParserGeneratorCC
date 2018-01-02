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

/* Copyright (c) 2006, Sun Microsystems, Inc.
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
 */
package com.helger.pgcc.jjtree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ASTNodeDescriptor extends JJTreeNode
{
  static List <String> nodeIds = new ArrayList <> ();
  static List <String> nodeNames = new ArrayList <> ();
  static Map <String, String> nodeSeen = new HashMap <> ();

  static ASTNodeDescriptor indefinite (final String s)
  {
    final ASTNodeDescriptor nd = new ASTNodeDescriptor (JJTreeParserTreeConstants.JJTNODEDESCRIPTOR);
    nd.name = s;
    nd.setNodeIdValue ();
    nd.faked = true;
    return nd;
  }

  static List <String> getNodeIds ()
  {
    return nodeIds;
  }

  static List <String> getNodeNames ()
  {
    return nodeNames;
  }

  private boolean faked = false;
  String name;
  boolean m_isGT;
  ASTNodeDescriptorExpression expression;

  ASTNodeDescriptor (final int id)
  {
    super (id);
  }

  void setNodeIdValue ()
  {
    final String k = getNodeId ();
    if (!nodeSeen.containsKey (k))
    {
      nodeSeen.put (k, k);
      nodeNames.add (name);
      nodeIds.add (k);
    }
  }

  String getNodeId ()
  {
    return "JJT" + name.toUpperCase ().replace ('.', '_');
  }

  boolean isVoid ()
  {
    return name.equals ("void");
  }

  @Override
  public String toString ()
  {
    if (faked)
      return "(faked) " + name;
    return super.toString () + ": " + name;
  }

  String getDescriptor ()
  {
    if (expression == null)
    {
      return name;
    }
    return "#" + name + "(" + (m_isGT ? ">" : "") + expression_text () + ")";
  }

  String getNodeType ()
  {
    if (JJTreeOptions.getMulti ())
    {
      return JJTreeOptions.getNodePrefix () + name;
    }
    else
    {
      return "SimpleNode";
    }
  }

  String getNodeName ()
  {
    return name;
  }

  String openNode (final String nodeVar)
  {
    return "jjtree.openNodeScope(" + nodeVar + ");";
  }

  String expression_text ()
  {
    if (expression.getFirstToken ().image.equals (")") && expression.getLastToken ().image.equals ("("))
    {
      return "true";
    }

    String s = "";
    Token t = expression.getFirstToken ();
    while (true)
    {
      s += " " + t.image;
      if (t == expression.getLastToken ())
      {
        break;
      }
      t = t.next;
    }
    return s;
  }

  String closeNode (final String nodeVar)
  {
    if (expression == null)
    {
      return "jjtree.closeNodeScope(" + nodeVar + ", true);";
    }
    else
      if (m_isGT)
      {
        return "jjtree.closeNodeScope(" + nodeVar + ", jjtree.nodeArity() >" + expression_text () + ");";
      }
      else
      {
        return "jjtree.closeNodeScope(" + nodeVar + ", " + expression_text () + ");";
      }
  }

  @Override
  String translateImage (final Token t)
  {
    return whiteOut (t);
  }

  /** Accept the visitor. **/
  @Override
  public Object jjtAccept (final JJTreeParserVisitor visitor, final Object data)
  {
    return visitor.visit (this, data);
  }

}

/* end */
