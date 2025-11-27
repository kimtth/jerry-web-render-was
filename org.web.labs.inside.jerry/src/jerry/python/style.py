"""Style module - applies CSS styles to DOM nodes."""

from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple
from enum import Enum

from dom import Node, Element, Text
from css import Stylesheet, Rule, Selector, SimpleSelector, Declaration, Value, Keyword


# Map from CSS property names to values
PropertyMap = Dict[str, Value]


class Display(Enum):
    """CSS display property values."""
    Inline = "inline"
    Block = "block"
    DisplayNone = "none"


@dataclass
class StyledNode:
    """A node with associated style data."""
    node: Node
    specified_values: PropertyMap = field(default_factory=dict)
    children: List['StyledNode'] = field(default_factory=list)
    
    def value(self, name: str) -> Optional[Value]:
        """Return the specified value of a property if it exists, otherwise None."""
        return self.specified_values.get(name)
    
    def lookup(self, name: str, fallback_name: str, default: Value) -> Value:
        """Return the specified value of property name, or fallback_name, or default."""
        val = self.value(name)
        if val is not None:
            return val
        val = self.value(fallback_name)
        if val is not None:
            return val
        return default
    
    def display(self) -> Display:
        """The value of the `display` property (defaults to inline)."""
        display_val = self.value("display")
        if isinstance(display_val, Keyword):
            if display_val.keyword == "block":
                return Display.Block
            elif display_val.keyword == "none":
                return Display.DisplayNone
        return Display.Inline


# Type alias for matched rules
MatchedRule = Tuple[Tuple[int, int, int], Rule]


def style_tree(root: Node, stylesheet: Stylesheet) -> StyledNode:
    """Apply a stylesheet to an entire DOM tree, returning a StyledNode tree.
    
    This finds only the specified values at the moment. Eventually it should be
    extended to find the computed values too, including inherited values.
    """
    specified_values: PropertyMap = {}
    
    if isinstance(root, Element):
        specified_values = get_specified_values(root.element, stylesheet)
    elif isinstance(root, Text):
        specified_values = {}
    
    children = [style_tree(child, stylesheet) for child in root.children]
    
    return StyledNode(
        node=root,
        specified_values=specified_values,
        children=children
    )


def get_specified_values(elem_data, stylesheet: Stylesheet) -> PropertyMap:
    """Apply styles to a single element, returning the specified styles."""
    values: PropertyMap = {}
    rules = matching_rules(elem_data, stylesheet)
    
    # Go through the rules from lowest to highest specificity.
    rules.sort(key=lambda r: r[0])
    
    for _, rule in rules:
        for declaration in rule.declarations:
            values[declaration.name] = declaration.value
    
    return values


def matching_rules(elem_data, stylesheet: Stylesheet) -> List[MatchedRule]:
    """Find all CSS rules that match the given element."""
    matched = []
    for rule in stylesheet.rules:
        result = match_rule(elem_data, rule)
        if result is not None:
            matched.append(result)
    return matched


def match_rule(elem_data, rule: Rule) -> Optional[MatchedRule]:
    """If `rule` matches `elem`, return a MatchedRule. Otherwise return None."""
    for selector in rule.selectors:
        if matches(elem_data, selector):
            return (selector.specificity(), rule)
    return None


def matches(elem_data, selector: Selector) -> bool:
    """Check if a selector matches an element."""
    return matches_simple_selector(elem_data, selector.simple)


def matches_simple_selector(elem_data, selector: SimpleSelector) -> bool:
    """Check if a simple selector matches an element."""
    # Check type selector
    if selector.tag_name is not None:
        if elem_data.tag_name != selector.tag_name:
            return False
    
    # Check ID selector
    if selector.id is not None:
        if elem_data.id() != selector.id:
            return False
    
    # Check class selectors
    elem_classes = elem_data.classes()
    for class_name in selector.class_names:
        if class_name not in elem_classes:
            return False
    
    # We didn't find any non-matching selector components.
    return True
