"""CSS Parser module - parses CSS into a Stylesheet."""

import re
from typing import List
from css import (
    Stylesheet, Rule, Selector, SimpleSelector, Declaration,
    Value, Keyword, Length, ColorValue, Color, Unit
)


class CSSParser:
    """A simple parser for a tiny subset of CSS."""
    
    def __init__(self, pos: int, input_str: str):
        self.pos = pos
        self.input = input_str
    
    def parse_rules(self) -> List[Rule]:
        """Parse a list of rule sets, separated by optional whitespace."""
        rules: List[Rule] = []
        
        while True:
            self.consume_whitespace()
            if self.eof():
                break
            rules.append(self.parse_rule())
        
        return rules
    
    def parse_rule(self) -> Rule:
        """Parse a rule set: `<selectors> { <declarations> }`."""
        return Rule(
            selectors=self.parse_selectors(),
            declarations=self.parse_declarations()
        )
    
    def parse_selectors(self) -> List[Selector]:
        """Parse a comma-separated list of selectors."""
        selectors: List[Selector] = []
        
        while True:
            selectors.append(Selector(simple=self.parse_simple_selector()))
            self.consume_whitespace()
            
            c = self.next_char()
            if c == ',':
                self.consume_char()
                self.consume_whitespace()
            elif c == '{':
                break
            else:
                raise Exception(f"Unexpected character {c} in selector list")
        
        # Return selectors with highest specificity first, for use in matching.
        selectors.sort(key=lambda s: s.specificity(), reverse=True)
        return selectors
    
    def parse_simple_selector(self) -> SimpleSelector:
        """Parse one simple selector, e.g.: `type#id.class1.class2.class3`"""
        selector = SimpleSelector()
        
        while not self.eof():
            c = self.next_char()
            if c == '#':
                self.consume_char()
                selector.id = self.parse_identifier()
            elif c == '.':
                self.consume_char()
                selector.class_names.append(self.parse_identifier())
            elif c == '*':
                # universal selector
                self.consume_char()
            elif self.valid_identifier_char(c):
                selector.tag_name = self.parse_identifier()
            else:
                break
        
        return selector
    
    def parse_declarations(self) -> List[Declaration]:
        """Parse a list of declarations enclosed in `{ ... }`."""
        assert self.consume_char() == '{'
        declarations: List[Declaration] = []
        
        while True:
            self.consume_whitespace()
            if self.next_char() == '}':
                self.consume_char()
                break
            declarations.append(self.parse_declaration())
        
        return declarations
    
    def parse_declaration(self) -> Declaration:
        """Parse one `<property>: <value>;` declaration."""
        property_name = self.parse_identifier()
        self.consume_whitespace()
        assert self.consume_char() == ':'
        self.consume_whitespace()
        value = self.parse_value()
        self.consume_whitespace()
        assert self.consume_char() == ';'
        
        return Declaration(name=property_name, value=value)
    
    def parse_value(self) -> Value:
        """Parse a CSS value."""
        c = self.next_char()
        if c.isdigit():
            return self.parse_length()
        elif c == '#':
            return self.parse_color()
        else:
            return Keyword(keyword=self.parse_identifier())
    
    def parse_length(self) -> Length:
        """Parse a length value."""
        return Length(length=self.parse_float(), unit=self.parse_unit())
    
    def parse_float(self) -> float:
        """Parse a floating point number."""
        s = self.consume_while(lambda c: c.isdigit() or c == '.')
        return float(s)
    
    def parse_unit(self) -> Unit:
        """Parse a unit (only px supported)."""
        ident = self.parse_identifier().lower()
        if ident == "px":
            return Unit.Px
        else:
            raise Exception("Unrecognized unit")
    
    def parse_color(self) -> ColorValue:
        """Parse a hex color value."""
        assert self.consume_char() == '#'
        return ColorValue(color=Color(
            r=self.parse_hex_pair(),
            g=self.parse_hex_pair(),
            b=self.parse_hex_pair(),
            a=255
        ))
    
    def parse_hex_pair(self) -> int:
        """Parse two hexadecimal digits."""
        s = self.input[self.pos:self.pos + 2]
        self.pos += 2
        return int(s, 16)
    
    def parse_identifier(self) -> str:
        """Parse a property name or keyword."""
        return self.consume_while(self.valid_identifier_char)
    
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
    
    def eof(self) -> bool:
        """Return true if all input is consumed."""
        return self.pos >= len(self.input)
    
    def valid_identifier_char(self, c: str) -> bool:
        """Check if character is valid in an identifier."""
        return bool(re.match(r'[a-zA-Z0-9_-]', c))


def parse(source: str) -> Stylesheet:
    """Parse a whole CSS stylesheet."""
    parser = CSSParser(0, source)
    return Stylesheet(rules=parser.parse_rules())
