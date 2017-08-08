//! Code for applying CSS styles to the DOM.
//!
//! This is not very interesting at the moment.  It will get much more
//! complicated if I add support for compound selectors.

package org.web.labs.inside.jerry.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

interface Display {
}

class Inline implements Display {
}

class Block implements Display {
}

class None implements Display {
}

class StyledNode {
	Node node;
	/// Map from CSS property names to values.
	HashMap<String, Value> specified_values;
	ArrayList<StyledNode> children;

	public StyledNode(Node node, HashMap<String, Value> stylesheet, ArrayList<StyledNode> children) {
		this.node = node;
		this.specified_values = stylesheet;
		this.children = children;
	}

	/// Return the specified value of a property if it exists, otherwise `None`.
	public Value value(String name) {
		return this.specified_values.get(name);
	}

	/// Return the specified value of property `name`, or property
	/// `fallback_name` if that doesn't
	/// exist. or value `default` if neither does.
	public Value lookup(String name, String fallback_name, Value defaultVal) {
		if (value(name) != null) {
			return value(name);
		} else if (value(fallback_name) != null) {
			return value(fallback_name);
		} else {
			return defaultVal;
		}
	}

	/// The value of the `display` property (defaults to inline).
	public Display display() {
		Value display = value("display");

		if (display instanceof Keyword) {
			if (((Keyword) display).keyword.equals("block")) {
				return new Block();
			} else if (((Keyword) display).keyword.equals("none")) {
				return new None();
			} else {
				return new Inline();
			}
		} else {
			return new Inline();
		}
	}
}

class MatchedRule {
	ArrayList<Integer> specificity;
	Rule cssRule;

	public Rule getCssRule() {
		return cssRule;
	}

	public ArrayList<Integer> getSpecificity() {
		return specificity;
	}

	public MatchedRule(ArrayList<Integer> specificity, Rule rule) {
		this.specificity = specificity;
		this.cssRule = rule;
	}

}

public class StyleTree {

	/// Apply a stylesheet to an entire DOM tree, returning a StyledNode tree.
	///
	/// This finds only the specified values at the moment. Eventually it should
	/// be extended to find the
	/// computed values too, including inherited values.
	public StyledNode style_tree(Node node, ArrayList<Rule> stylesheet) {
		HashMap<String, Value> stylesheetMap = null;

		if (node instanceof Element) {
			ElementData eledata = ((Element) node).element;
			stylesheetMap = specified_values((ElementData) eledata, stylesheet);
		}
		if (node instanceof Text) {
			stylesheetMap = new HashMap<String, Value>();
		}

		ArrayList<Node> childNode = null;
		if (node instanceof Element) {
			childNode = ((Element) node).children;
		}
		ArrayList<StyledNode> children = new ArrayList<StyledNode>();
		if (childNode != null) {
			for (Node child : childNode) {
				StyledNode styleNode = style_tree(child, stylesheet);
				children.add(styleNode);
			}
		}

		return new StyledNode(node, stylesheetMap, children);
	}

	/// Apply styles to a single element, returning the specified styles.
	///
	/// To do: Allow multiple UA/author/user stylesheets, and implement the
	/// cascade.
	private HashMap<String, Value> specified_values(ElementData node, ArrayList<Rule> stylesheet) {
		HashMap<String, Value> values = new HashMap<String, Value>();
		ArrayList<MatchedRule> rules = matching_rules(node, stylesheet);

		// Go through the rules from lowest to highest specificity.
		Collections.sort(rules, new Comparator<MatchedRule>() {
			@Override
			public int compare(MatchedRule o1, MatchedRule o2) {
				ArrayList<Integer> lhs = o1.getSpecificity();
				ArrayList<Integer> rhs = o2.getSpecificity();

				return compare(lhs, rhs);
			}

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

		for (MatchedRule mrule : rules) {
			ArrayList<Declaration> declarations = mrule.cssRule.declarations;
			for (Declaration dec : declarations) {
				values.put(dec.prop_name, dec.value);
			}
		}

		return values;
	}

	/// A single CSS rule and the specificity of its most specific matching
	/// selector.
	private ArrayList<MatchedRule> matching_rules(ElementData node, ArrayList<Rule> stylesheet) {
		ArrayList<MatchedRule> filtered = new ArrayList<MatchedRule>();

		for (Rule rule : stylesheet) {
			MatchedRule match_rule = match_rule(node, rule);

			if (match_rule != null) {
				filtered.add(match_rule);
			}
		}

		return filtered;
	}

	/// MatchedRule: A single CSS rule and the specificity of its most specific
	/// matching selector.
	/// Find all CSS rules that match the given element.
	/// If `rule` matches `elem`, return a `MatchedRule`. Otherwise return
	/// `None`.
	private MatchedRule match_rule(ElementData node, Rule rule) {
		for (Selector selector : rule.selectors) {
			if (selector != null & matches(node, selector)) {
				return new MatchedRule(selector.specificity(), rule);
			}
		}
		return null;
	}

	/// Selector matching:
	private boolean matches(ElementData node, Selector selector) {

		if (selector instanceof SimpleSelector) {
			return matches_simple_selector(node, (SimpleSelector) selector);
		}

		return false;
	}

	private boolean matches_simple_selector(ElementData eld, SimpleSelector selector) {
		// Check type selector
		// @add kim => don't use == / use equals.
		if (selector.tag_name != null && eld.tag_name.equals(selector.tag_name) == false) {
			return false;
		}

		// Check ID selector
		if (eld.id() != null && eld.id().equals(selector.id) == false) {
			return false;
		}

		//TODO => Maybe selectivity or clazz
		// Check class selectors
		if (selector.clazz.size() > 0) {
			for (String clazz : selector.clazz) {
				if (eld.classes().contains(clazz) == false) {
					return false;
				}
			}
		}

		// We didn't find any non-matching selector components.
		return true;
	}

}
