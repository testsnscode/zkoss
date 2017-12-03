/* FacadeNodeList.java


	Purpose: 
	Description: 
	History:
	2001/10/02 16:33:29, Create, Tom M. Yeh.

Copyright (C) 2001 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under LGPL Version 2.1 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.idom.impl;

import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The NodeList which is a facade of another java List.
 *
 * @author tomyeh
 */
public class FacadeNodeList implements NodeList {
	/** The java List to facade. */
	protected List _list;

	/** Constructor.
	 *
	 * @param list the list to facade; never null
	 */
	public FacadeNodeList(List list) {
		_list = list;
	}

	//-- NodeList --//
	public final int getLength() {
		return _list.size();
	}
	public final Node item(int j) {
		return j>=0 && j<_list.size() ? (Node)_list.get(j): null;
	}
}
