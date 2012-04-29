package org.busbrothers.anystop.agencytoken.parser;

/*
 * @(#)SimpleElement.java
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * <code>SimpleElement</code> is the only node type for
 * simplified DOM model.
 */
public class SimpleElement {
	private String tagName;
	private String text;
	private HashMap attributes;
	private ArrayList<SimpleElement> childElements;

	public SimpleElement(String tagName) {
		this.tagName = tagName;
		attributes = new HashMap();
		childElements = new ArrayList<SimpleElement> ();
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getAttribute(String name) {
		return (String)attributes.get(name);
	}

	public void setAttribute(String name, String value) {
		attributes.put(name, value);
	}

	public void addChildElement(SimpleElement element) {
		childElements.add(element);
	}

	public ArrayList<SimpleElement> getChildElements() {
		return childElements;
	}
}
