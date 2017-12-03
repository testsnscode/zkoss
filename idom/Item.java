/* Item.java


	Purpose: 
	Description: 
	History:
	2001/10/21 15:54:59, Create, Tom M. Yeh.

Copyright (C) 2001 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under LGPL Version 2.1 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.idom;

import org.zkoss.xml.Locator;

/**
 * Represents an item (a.k.a., node) of a iDOM tree. A iDOM tree is not necessary
 * also a W3C/DOM tree. However, in iDOM's implement, it is
 * because all Vertices also implement related interface, such as Node.
 *
 * <p>Some vertices, currently only Element, might support attributes
 * by implementing Attributable.
 *
 * <p>Due to the implementation of both Item and W3C/DOM's interfaces,
 * many methods seem redundant (e.g., parent vs. getParentNode, and
 * children vs. getChildNodes).
 * Caller could use them interchangeably . However, it is
 * suggested to use Item's API instead of Node's, because, like JDOM,
 * Item's API is based Java collection classes and more consistent
 * (from my point of view). The W3C/DOM API is used to work with utilities
 * that work only with W3C/DOM.
 *
 * <p>Be careful that some methods look similar, but behave different.
 * Refer to package.html.
 *
 * @author tomyeh
 * @see Attributable
 * @see Binable
 */
public interface Item extends Cloneable {
	/** Indicates the searching is based on regular expression
	 * (upon the name argument).
	 * If not specified, exact-match is required.
	 */
	public static final int FIND_BY_REGEX = 0x0001;
	/** Indicates the searching is case insensitive.
	 * This flag is ignored if FIND_BY_REGEX is specified.
	 */
	public static final int FIND_IGNORE_CASE = 0x0002;
	/** Indicates the name argument is a tag name
	 * rather than local name.
	 */
	public static final int FIND_BY_TAGNAME = 0x0004;
	/** Indicates the namespace argument is a prefix rather
	 * than URI.
	 */
	public static final int FIND_BY_PREFIX = 0x0008;
	/** Indicates the searching looks for all descendants.
	 * If not specified, only the children (not children of children)
	 * is searched.
	 */
	public static final int FIND_RECURSIVE = 0x0100;

	/**
	 * Gets the name of the item.
	 * For vertices that support namespace (implements Namespaceable),
	 * it is the same as getTagName.
	 *
	 * @see Namespaceable#getTagName
	 */
	public String getName();
	/**
	 * Sets the name of the item.
	 * For vertices that support namespace (implements Namespaceable),
	 * it is the same as setTagName.
	 *
	 * @exception DOMException with NOT_SUPPORTED_ERR if this item
	 * doesn't allow to change the name, e.g., Document and DocType
	 *
	 * @see Namespaceable#setTagName
	 */
	public void setName(String name);

	/**
	 * Gets the text of this item, or null if it is neither {@link Textual}
	 * nor {@link Element}.
	 * For Element, the text is the concatenation of all its textual
	 * children, including Text, CDATA, and Binary.
	 *
	 * <p>Besides String-type value, some item, e.g., Binary, allows
	 * any type of objects. Caller could test whether a item implements
	 * the Binable interface, and use Binable.getValue instead.
	 * For binable vertices, getText is actually getValue().toString().
	 *
	 * <p>The returned value is neither trimmed nor normalized.
	 */
	public String getText();
	/**
	 * Sets the text of this item.
	 *
	 * @exception DOMException with NOT_SUPPORTED_ERR if this item
	 * doesn't allow to change the value, e.g., Document and Element
	 */
	public void setText(String obj);

	/**
	 * Gets the document that owns this item.
	 * The owning document is the first document in its ancestor.
	 * For DOM, the document can only be the root, so the owning documents
	 * of vertices in a DOM tree are all the same.
	 */
	public Document getDocument();

	/**
	 * Detach this item from its parent.
	 *
	 * <p>Because each item can belong to at most one parent at a time, it
	 * is important to detach it first, before added to another tree -- even
	 * if it is the same tree/parent. 
	 *
	 * <p>It has the similar effect as:<br>
	 * getParent().getChildren().remove(this).
	 *
	 * <p>Naming reason: we don't call this method as getChildren() to be
	 * compatible with the naming style of Attributable.attributes -- which
	 * is limited to org.w3c.dom.Attr.getAttributes.
	 * Also, it doesn't have the setter and it is "live", so it
	 * 'seem' better to call it getChildren().
	 *
	 * @return this item
	 */
	public Item detach();

	/**
	 * Gets the parent item.
	 */
	public Group getParent();

	/**
	 * Gets the locator of this item.
	 * @return the locator; null if not available (default)
	 */
	public Locator getLocator();
	/**
	 * Sets the locator of this item.
	 *
	 * <p>Unlike other methods, it won't change the modification flag.
	 *
	 * @param loc the locator; null if not available
	 */
	public void setLocator(Locator loc);

	/** Clones this item. Unlike other objects, it does a deep cloning.
	 * @since 5.0,8
	 */
	public Object clone();

	//-- Internal Methods --//
	/**
	 * Sets the parent item.
	 *
	 * <p><b><i>DO NOT</i></b> call this method. It is used internally.
	 * Instead, use detach or thru getChildren().
	 */
	public void setParent(Group parent);
}
