//! A simple parser for a tiny subset of HTML.
//!
//! Can parse basic opening and closing tags, and text nodes.
//!
//! Not yet supported:
//!
//! * Comments
//! * Doctypes and processing instructions
//! * Self-closing tags
//! * Non-well-formed markup
//! * Character entities

package org.web.labs.inside.jerry.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class HTMLBuilder {

	// Rust Vec == Java ArrayList
	/// Parse an HTML document and return the root element.
	public Node parse(String source) {

		Parser parse = new Parser(0, source);
		ArrayList<Node> nodes = parse.parse_nodes();

		// If the document contains a root element, just return it. Otherwise,
		// create one.
		if (nodes.size() == 1) {
			return nodes.get(0);
		} else {
			ElementData element_data = new ElementData("html", new HashMap<String, String>());
			return new Element(element_data, nodes);
		}
	}
}

// https://github.com/Nevon/gogel/blob/master/html.go

class Parser {
	int position;
	String input;

	public Parser(int postion, String source) {
		this.position = postion;
		this.input = source;
	}

	/// Parse a sequence of sibling nodes.
	public ArrayList<Node> parse_nodes() {

		ArrayList<Node> nodes = new ArrayList<Node>();

		do {
			consume_whitespace();

			if (eof() || starts_with("</")) {
				break;
			}

			nodes.add(parser_node());
		} while (true);

		return nodes;
	}

	/// Parse a single node.
	private Node parser_node() {
		Node node;
		if (next_char() == '<') {
			node = parse_element();
		} else {
			node = parse_text();
		}

		return node;
	}

	/// Parse a single element, including its open tag, contents, and closing
	/// tag.
	private Node parse_element() {
		// Opening tag.
		consume_char(); // '<';
		String tag_name = parse_tag_name();
		HashMap<String, String> attrs = parse_attributes();
		consume_char(); // '>';

		// Contents.
		ArrayList<Node> children = parse_nodes();

		// Closing tag.
		consume_char(); // '<';
		consume_char(); // '/';
		parse_tag_name(); // tag_name;
		consume_char(); // '>';

		Element elNd = new Element(new ElementData(tag_name, attrs), children);

		return elNd;
	}

	/// Parse a tag or attribute name.
	private String parse_tag_name() {
		String result = "";

		String regex = "[0-9a-zA-Z]";
		Pattern p = Pattern.compile(regex);

		while (!eof() && p.matcher(String.valueOf(next_char())).find()) {
			result += consume_char();
		}

		return result;
	}

	/// Parse a list of name="value" pairs, separated by whitespace.
	private HashMap<String, String> parse_attributes() {
		HashMap<String, String> attributes = new HashMap<String, String>();

		while (true) {
			consume_whitespace();
			if (next_char() == '>') {
				break;
			}
			String[] nameValue = parse_attr();
			attributes.put(nameValue[0], nameValue[1]);
		}

		return attributes;
	}

	/// Parse a single name="value" pair.
	private String[] parse_attr() {

		String[] nameValue = new String[2];

		nameValue[0] = parse_tag_name();
		consume_char(); // '=';
		nameValue[1] = parse_attr_value();

		return nameValue;
	}

	/// Parse a quoted value.
	private String parse_attr_value() {
		char open_quote = consume_char();
		// open_quote == '"' || open_quote == '\'';
		String value = "";

		while (!eof() && next_char() != open_quote) {
			value += consume_char();
		}

		consume_char(); // open_quote;

		return value;
	}

	/// Parse a text node.
	private Node parse_text() {
		String result = "";

		while (!eof() && '<' != next_char()) {
			result += consume_char();
		}

		Text txNd = new Text(result);

		return txNd;
	}

	/// Consume and discard zero or more whitespace characters.
	private String consume_whitespace() {
		String result = "";

		while (!eof() && Character.isWhitespace(next_char())) {
			result += consume_char();
		}

		return result;
	}

	/// Return the current character, and advance self.pos to the next
	/// character.
	private char consume_char() {
		char nextChar = input.charAt(position);
		this.position += 1;

		return nextChar;
	}

	/// Read the current character without consuming it.
	private char next_char() {
		return input.charAt(position);
	}

	/// Does the current input start with the given string?
	private boolean starts_with(String prefix) {
		String cutInput = input.substring(position, input.length());

		if (cutInput.startsWith(prefix)) {
			return true;
		}

		return false;
	}

	/// Return true if all input is consumed.
	private boolean eof() {
		if (this.position >= input.length()) {
			return true;
		}
		return false;
	}

}
