/* ELXelExpression.java

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

import org.zkoss.xel.Expression;
import org.zkoss.xel.XelContext;
import org.zkforge.apache.commons.el.ELExpression;

/** 
 * A XEL expression based on zcommons-el latest version.
 *
 * @author tomyeh
 */
/*package*/ class ELXelExpression implements Expression {
	private final ELExpression _expr;
	/**
	 * @param expr the expression. It cannot be null.
	 */
	public ELXelExpression(ELExpression expr) {
		if (expr == null)
			throw new IllegalArgumentException();
		_expr = expr;
	}
	public Object evaluate(XelContext ctx) {
		//Test case: B30-1957661.zul where a function mapper is created
		//by zscript so it is different from one page to page
		return _expr.evaluate(ctx);
	}

	//Object//
	public boolean equals(Object o) {
		if (this == o) return true;
		return o instanceof ELXelExpression &&
			((ELXelExpression)o)._expr.equals(_expr);
	}
	public int hashCode() {
		return _expr.hashCode();
	}
	public String toString() {
		return _expr.toString();
	}
}
