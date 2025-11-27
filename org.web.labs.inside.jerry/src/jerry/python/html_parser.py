"""HTML Parser module - parses HTML into a DOM tree."""

import re
from typing import Dict, List, Tuple
from dom import Node, Text, Element, ElementData, elem, text


class HTMLParser:
    """A simple parser for a tiny subset of HTML.
    
    Can parse basic opening and closing tags, and text nodes.
    
    Not yet supported:
    - Comments
    - Doctypes and processing instructions
    - Self-closing tags
    - Non-well-formed markup
    - Character entities
    """
    
    def __init__(self, pos: int, input_str: str):
        self.pos = pos
        self.input = input_str
    
    def parse_nodes(self) -> List[Node]:
        """Parse a sequence of sibling nodes."""
        nodes: List[Node] = []
        
        while True:
            self.consume_whitespace()
            
            if self.eof() or self.starts_with("</"):
                break
            
            nodes.append(self.parse_node())
        
        return nodes
    
    def parse_node(self) -> Node:
        """Parse a single node."""
        if self.next_char() == '<':
            return self.parse_element()
        else:
            return self.parse_text()
    
    def parse_element(self) -> Element:
        """Parse a single element, including its open tag, contents, and closing tag."""
        # Opening tag
        assert self.consume_char() == '<'
        tag_name = self.parse_tag_name()
        attrs = self.parse_attributes()
        assert self.consume_char() == '>'
        
        # Contents
        children = self.parse_nodes()
        
        # Closing tag
        assert self.consume_char() == '<'
        assert self.consume_char() == '/'
        assert self.parse_tag_name() == tag_name
        assert self.consume_char() == '>'
        
        return elem(tag_name, attrs, children)
    
    def parse_tag_name(self) -> str:
        """Parse a tag or attribute name."""
        return self.consume_while(lambda c: re.match(r'[a-zA-Z0-9]', c) is not None)
    
    def parse_attributes(self) -> Dict[str, str]:
        """Parse a list of name="value" pairs, separated by whitespace."""
        attributes: Dict[str, str] = {}
        
        while True:
            self.consume_whitespace()
            if self.next_char() == '>':
                break
            name, value = self.parse_attr()
            attributes[name] = value
        
        return attributes
    
    def parse_attr(self) -> Tuple[str, str]:
        """Parse a single name="value" pair."""
        name = self.parse_tag_name()
        assert self.consume_char() == '='
        value = self.parse_attr_value()
        return (name, value)
    
    def parse_attr_value(self) -> str:
        """Parse a quoted value."""
        open_quote = self.consume_char()
        assert open_quote in ('"', "'")
        value = self.consume_while(lambda c: c != open_quote)
        assert self.consume_char() == open_quote
        return value
    
    def parse_text(self) -> Text:
        """Parse a text node."""
        content = self.consume_while(lambda c: c != '<')
        return text(content)
    
    def consume_whitespace(self) -> str:
        """Consume and discard zero or more whitespace characters."""
        return self.consume_while(lambda c: c.isspace())
    
    def consume_while(self, test) -> str:
        """Consume characters until test returns false."""
        result = ""
        while not self.eof() and test(self.next_char()):
            result += self.consume_char()
        return result
    
    def consume_char(self) -> str:
        """Return the current character and advance position."""
        char = self.input[self.pos]
        self.pos += 1
        return char
    
    def next_char(self) -> str:
        """Read the current character without consuming it."""
        return self.input[self.pos]
    
    def starts_with(self, prefix: str) -> bool:
        """Check if current input starts with the given string."""
        return self.input[self.pos:].startswith(prefix)
    
    def eof(self) -> bool:
        """Return true if all input is consumed."""
        return self.pos >= len(self.input)


def parse(source: str) -> Node:
    """Parse an HTML document and return the root element."""
    parser = HTMLParser(0, source)
    nodes = parser.parse_nodes()
    
    # If the document contains a root element, just return it.
    # Otherwise, create one.
    if len(nodes) == 1:
        return nodes[0]
    else:
        return elem("html", {}, nodes)
