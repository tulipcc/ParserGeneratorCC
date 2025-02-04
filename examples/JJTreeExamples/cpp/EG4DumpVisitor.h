/**
 * Copyright 2017-2025 Philip Helger, pgcc@helger.com
 *
 * Copyright 2011 Google Inc. All Rights Reserved.
 * Author: sreeni@google.com (Sreeni Viswanadha)
 *
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
 */
/*
 * EG4DumpVisitor.h
 *
 *  Created on: 28 mars 2014
 *      Author: FrancisANDRE
 */

#ifndef EG4DUMPVISITOR_H_
#define EG4DUMPVISITOR_H_
#include "ParserVisitor.h"
#include "JavaCC.h"

namespace @NAMESPACE@ {
class ASTMyID;
class ASTMyOtherID;

class EG4DumpVisitor: public ParserVisitor {
public:
	EG4DumpVisitor();
	virtual ~EG4DumpVisitor();

	/**
	 *  This is an example of how the Visitor pattern might be used to
	 *  implement the dumping code that comes with SimpleNode.  It's a bit
	 *  long-winded, but it does illustrate a couple of the main points.
	 *  <ol>
	 *  <li> the visitor can maintain state between the nodes that it visits
	 *  (for example the current indentation level).
	 *  </li>
	 *
	 *  <li>if you don't implement a jjtAccept() method for a subclass of
	 *  SimpleNode, then SimpleNode's acceptor will get called.
	 *  </li>
	 *  <li> the utility method childrenAccept() can be useful when
	 *  implementing preorder or postorder tree walks.
	 *  </li>
	 *  </ol>
	 *
	 */

private:
	int indent;
	JAVACC_SIMPLE_STRING indentString() const;
public:
	void* visit(const SimpleNode *node, void * data);
	void* visit(const ASTStart *node, void * data);
	void* visit(const ASTAdd *node, void * data);
	void* visit(const ASTMult *node, void * data);
	void* visit(const ASTMyID *node, void * data);
	void* visit(const ASTMyOtherID *node, void * data);
	void* visit(const ASTInteger *node, void * data);
};

} /* namespace @NAMESPACE@ */

#endif /* EG4DUMPVISITOR_H_ */
