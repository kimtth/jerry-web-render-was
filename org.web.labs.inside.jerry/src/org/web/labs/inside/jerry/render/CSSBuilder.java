//! A simple parser for a tiny subset of CSS.
//!
//! To support more CSS syntax, it would probably be easiest to replace this
//! hand-rolled parser with one based on a library or parser generator.

package org.web.labs.inside.jerry.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Pattern;

interface Selector {
	ArrayList<Integer> specificity();
}

interface Value {
	/// Return the size of a length in px, or zero for non-lengths.
	double to_px();
}

interface Unit {
}

class Stylesheet {
	ArrayList<Rule> rules;
}

class Rule {
	ArrayList<Selector> selectors;
	ArrayList<Declaration> declarations;

	public Rule(ArrayList<Selector> selectors, ArrayList<Declaration> declarations) {
		this.selectors = selectors;
		this.declarations = declarations;
	}
}

class SimpleSelector implements Selector {
	String tag_name;
	String id;
	ArrayList<String> clazz;

	public SimpleSelector(String tag_name, String id, ArrayList<String> classAttr) {
		this.tag_name = tag_name;
		this.id = id;
		this.clazz = classAttr;
	}

	// [1i32, 2i32, 3i32] # array: “¯‚¶?^
	// (1i32, "hello", true) # tuple: ?^‚ÍŽ©—R
	// pub type Specificity = (usize, usize, usize);

	@Override
	public ArrayList<Integer> specificity() {
		// http://www.w3.org/TR/selectors/#specificity
		ArrayList<Integer> Specificity = new ArrayList<Integer>();

		// https://stackoverflow.com/questions/27461750/what-is-the-equivalent-of-rusts-optioniter-in-java
		// you can also think of Option as a collection that can only hold 0 or
		// 1 values.
		int a = this.id == null ? 0 : this.id.isEmpty() ? 0 : 1;
		int b = this.clazz.size();
		int c = this.tag_name == null ? 0 : this.tag_name.isEmpty() ? 0 : 1;

		Specificity.add(a);
		Specificity.add(b);
		Specificity.add(c);

		return Specificity;
	}
}

class Declaration {
	String prop_name;
	Value value;

	public Declaration(String property_name, Value value) {
		this.prop_name = property_name;
		this.value = value;
	}
}

class Keyword implements Value {
	String keyword;

	public Keyword(String parse_identifier) {
		this.keyword = parse_identifier;
	}

	@Override
	public double to_px() {
		return 0;
	}
}

// u8 is an 8-bit unsigned integer, and f32 is a 32-bit float.
class Length implements Value {
	double len;
	Unit unit;

	public Length(double parse_float, Unit parse_unit) {
		this.len = parse_float;
		this.unit = parse_unit;
	}

	public double to_px() {
		return this.len;
	}
}

class ColorValue implements Value {
	Color color;

	public ColorValue(Color color) {
		this.color = color;
	}

	@Override
	public double to_px() {
		return 0;
	}
}

class Px implements Unit {
}

class Color {
	int r;
	int g;
	int b;
	int a;

	public Color(int r, int g, int b, int a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}
}

/**
 * Specificity is one of the ways a rendering engine decides which style
 * overrides the other in a conflict. If a stylesheet contains two rules that
 * match an element, the rule with the matching selector of higher specificity
 * can override values from the one with lower specificity.
 *
 */
public class CSSBuilder {

	/// Parse a whole CSS stylesheet.
	public ArrayList<Rule> parse(String source) {
		CssParser parser = new CssParser(0, source);
		ArrayList<Rule> stylesheet = parser.parse_rules();
		return stylesheet;
	}
}

class CssParser {
	int position;
	String input;

	public CssParser(int position, String source) {
		this.position = position;
		this.input = source;
	}

	/// Parse a list of rule sets, separated by optional whitespace.
	public ArrayList<Rule> parse_rules() {
		ArrayList<Rule> stylesheet = new ArrayList<Rule>();

		do {
			consume_whitespace();

			if (eof()) {
				break;
			}

			stylesheet.add(parser_rule());
		} while (true);

		return stylesheet;
	}

	/// Parse a rule set: `<selectors> { <declarations> }`.
	private Rule parser_rule() {
		ArrayList<Selector> selectors = parse_selectors();
		ArrayList<Declaration> declarations = parse_declarations();

		Rule rule = new Rule(selectors, declarations);

		return rule;
	}

	/// Parse a comma-separated list of selectors.
	private ArrayList<Selector> parse_selectors() {
		ArrayList<Selector> selectors = new ArrayList<Selector>();

		do {
			selectors.add(parse_simple_selector());
			consume_whitespace();

			char c = next_char();
			if (c == ',') {
				consume_char();
				consume_whitespace();
			} else if (c == '{') {
				break;
			} else {
				System.err.println("Unexpected character {} in selector list " + next_char());
			}
		} while (true);

		// Return selectors with highest specificity first, for use in matching.
		// http://www.screaming.org/blog/2014/09/09/toy-layout-engine-3/
		Collections.sort(selectors, new Comparator<Selector>() {
			@Override
			public int compare(Selector o1, Selector o2) {
				ArrayList<Integer> lhs = o1.specificity();
				ArrayList<Integer> rhs = o2.specificity();

				return compare(lhs, rhs);
			}

			// -1 : o1 < o2
			// 0 : o1 == o2
			// +1 : o1 > o2
			public int compare(ArrayList<Integer> lhs, ArrayList<Integer> rhs) {
				if (lhs.get(0) == rhs.get(0)) {
					if (lhs.get(1) == rhs.get(1)) {
						return lhs.get(2) > rhs.get(2) ? 1 : lhs.get(2) < rhs.get(2) ? -1 : 0;
					} else {
						return lhs.get(1) > rhs.get(1) ? 1 : lhs.get(1) < rhs.get(1) ? -1 : 0;
					}
				} else {
					return lhs.get(0) > rhs.get(0) ? 1 : lhs.get(0) < rhs.get(0) ? -1 : 0;
				}
			}
		});

		return selectors;
	}

	// https://stackoverflow.com/questions/24771655/some-and-none-what-are-they
	// https://stackoverflow.com/questions/27457820/equivalent-of-rusts-match-statement-in-java
	// Some(self.parse_identifier());

	// This is the same as the core data Maybe a = Nothing | Just a type in
	// Haskell;
	// both represent an optional value, it's either there (Some/Just), or it's
	// not (None/Nothing).
	//
	/// Parse one simple selector, e.g.: `type#id.class1.class2.class3`
	private SimpleSelector parse_simple_selector() {
		SimpleSelector selector = new SimpleSelector(null, null, new ArrayList<String>());

		while (!eof()) {
			if (next_char() == '#') {
				consume_char();
				selector.id = parse_identifier();
			} else if (next_char() == '.') {
				consume_char();
				selector.clazz.add(parse_identifier());
			} else if (next_char() == '*') {
				// universal selector
				consume_char();
			} else if (valid_identifier_char(next_char())) {
				selector.tag_name = parse_identifier();
			//} else if (Character.isWhitespace(next_char())) {
			// consume_whitespace();
			} else {
				break;
			}
		}

		return selector;
	}

	/// Parse a list of declarations enclosed in `{ ... }
	private ArrayList<Declaration> parse_declarations() {
		consume_char(); // '{');
		ArrayList<Declaration> declarations = new ArrayList<Declaration>();

		do {
			consume_whitespace();

			if (next_char() == '}') {
				consume_char();
				break;
			}

			declarations.add(parse_declaration());
		} while (true);

		return declarations;
	}

	/// Parse one `<property>: <value>;` declaration.
	private Declaration parse_declaration() {
		String property_name = parse_identifier();
		consume_whitespace();
		consume_char(); // ':');
		consume_whitespace();
		Value value = parse_value();
		consume_whitespace();
		consume_char(); // ';');

		Declaration declaration = new Declaration(property_name, value);
		return declaration;
	}

	// Methods for parsing values:
	private Value parse_value() {
		Value value;
		char c = next_char();

		if (isDigit(c)) { // '0'...'9'
			value = parse_length();
		} else if (c == '#') {
			value = parse_color();
		} else {
			value = new Keyword(parse_identifier());
		}

		return value;
	}

	private Value parse_length() {
		Value value = new Length(parse_float(), parse_unit());
		return value;
	}

	private double parse_float() {
		String result = "";

		while (isDigit(next_char()) || next_char() == '.') {
			result += consume_char();
		}

		double d = Double.valueOf(result);
		return d;
	}

	private Unit parse_unit() {
		String str = parse_identifier();
		if (str.equalsIgnoreCase("px")) {
			return new Px();
		}
		System.err.println("unrecognized unit");

		return null;
	}

	private Value parse_color() {
		consume_char(); // '#');
		Value value = new ColorValue(new Color(parse_hex_pair(), parse_hex_pair(), parse_hex_pair(), 255));

		return value;
	}

	/// Parse two hexadecimal digits.
	private int parse_hex_pair() {
		String s = input.substring(position, position + 2);
		this.position += 2;
		int i = Integer.valueOf(s, 16);
		return i;
	}

	/// Parse a property name or keyword.
	private String parse_identifier() {
		String result = "";

		while (!eof() && valid_identifier_char(next_char())) {
			result += consume_char();
		}

		return result;
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

	/// Return true if all input is consumed.
	private boolean eof() {
		if (this.position >= input.length()) {
			return true;
		}
		return false;
	}

	private boolean valid_identifier_char(char next_char) {
		boolean result = false;

		String regex = "[0-9a-zA-Z_-]";
		Pattern p = Pattern.compile(regex);

		if (p.matcher(String.valueOf(next_char())).find()) {
			result = true;
		}

		return result;
	}

	// https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
	private boolean isDigit(char c) {
		String str = String.valueOf(c);
		next_char();
		try {
			Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

}
