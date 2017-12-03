/* ELXelExpression100.java

	Purpose:
		
	Description:
		
	History:
		Fri Aug 31 17:12:56     2007, Created by tomyeh

Copyright (C) 2007 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under LGPL Version 2.1 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.xel.el;

import org.zkoss.lang.Objects;
import org.zkoss.xel.Expression;
import org.zkoss.xel.XelContext;
import org.zkoss.xel.FunctionMapper;
import org.zkforge.apache.commons.el.ExpressionApi;

/** 
 * A XEL expression based on zcommons-el 1.0.1 or 1.0.0.
 *
 * @author tomyeh
 */
/*package*/ class ELXelExpression100 implements Expression {
	private final ExpressionApi _expr;
	private final String _rawexpr;
	private final FunctionMapper _mapper;
	private final Class _expected;
	/**
	 * @param expr the expression. It cannot be null.
	 */
	public ELXelExpression100(ExpressionApi expr,
	String rawexpr, FunctionMapper mapper, Class expectedType) {
		if (expr == null)
			throw new IllegalArgumentException();
		_rawexpr = rawexpr;
		_expr = expr;
		_mapper = mapper;
		_expected = expectedType;
	}
	public Object evaluate(XelContext ctx) {
		//Test case: B30-1957661.zul where a function mapper is created
		//by zscript so it is different from one page to page
		//In this case, we cannot reuse parsed expression.
		final FunctionMapper nfm = ctx.getFunctionMapper();
		if (!Objects.equals(_mapper, nfm))
			return new ELFactory().evaluate(ctx, _rawexpr, _expected);
		return _expr.evaluate(ctx.getVariableResolver());
	}

	//Object//
	public boolean equals(Object o) {
		if (this == o) return true;
		return o instanceof ELXelExpression100 &&
			((ELXelExpression100)o)._expr.equals(_expr);
	}
	public int hashCode() {
		return _expr.hashCode();
	}
	public String toString() {
		return _expr.toString();
	}
}
