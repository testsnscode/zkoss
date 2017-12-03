/* SimpleResolver.java

	Purpose:
		
	Description:
		
	History:
		Thu Oct 28 15:15:04     2004, Created by tomyeh

Copyright (C) 2004 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under LGPL Version 2.1 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.xel.util;

import java.util.Map;

import org.zkoss.xel.VariableResolver;
import org.zkoss.xel.VariableResolverX;
import org.zkoss.xel.XelException;

/**
 * A simple resolver that retrieve variable from a map.
 * <p>Note: since 5.0.8, it becomes serializable. Thus, the parent and
 * vars parameter
 * of {@link #SimpleResolver(VariableResolver, Map)} have to serializable
 * in the clustering environment.
 *
 * @author tomyeh
 * @since 3.0.0
 */
public class SimpleResolver implements VariableResolver, java.io.Serializable {
	/** The parent resolver. */
	private VariableResolver _parent;
	/** The variable maps. */
	protected Map<String, ?> _vars;

	/** Constructs a resolver. */
	public SimpleResolver() {
		this(null, null);
	}
	/** Constructs a resolver with a parent.
	 * @param parent the parent resolver (null means ignored).
	 */
	public SimpleResolver(VariableResolver parent) {
		this(parent, null);
	}
	/** Constructs a resolver with a parent and an object map.
	 * @param parent the parent resolver (null means ignored).
	 * @param vars the object map (null means ignored)
	 */
	public SimpleResolver(VariableResolver parent, Map<String, ?> vars) {
		_parent = parent;
		_vars = vars;
	}
	/** Constructs a resolver with an object map.
	 * @param vars the object map (null means ignored)
	 */
	public SimpleResolver(Map<String, ?> vars) {
		this(null, vars);
	}

	/** Returns the parent, or null if no parent at all.
	 */
	public VariableResolver getParent() {
		return _parent;
	}
	/** Sets the parent.
	 *
	 * @param parent the parent resolver, or null if no parent.
	 */
	public void setParent(VariableResolver parent) {
		_parent = parent;
	}

	//-- VariableResolver --//
	public Object resolveVariable(String name) throws XelException {
		if (_vars != null) {
			final Object o = _vars.get(name);
			if (o != null)
				return o;
		}
		return _parent instanceof VariableResolverX ?
			((VariableResolverX)_parent).resolveVariable(null, null, name):
			_parent != null ? _parent.resolveVariable(name): null;
	}
}
