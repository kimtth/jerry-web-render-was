"""Jerry Web Renderer - Python Implementation

A toy browser engine implementing HTML parsing, CSS styling, layout, and painting.
"""

from .dom import Node, Element, Text, ElementData, elem, text
from .css import (
    Color, Unit, Value, Keyword, Length, ColorValue,
    Selector, SimpleSelector, Declaration, Rule, Stylesheet
)
from .html_parser import parse as parse_html
from .css_parser import parse as parse_css
from .style import StyledNode, Display, style_tree
from .layout import Rect, EdgeSizes, Dimensions, BoxType, LayoutBox, layout_tree
from .painting import Canvas, DisplayCommand, SolidColor, paint

__all__ = [
    # DOM
    'Node', 'Element', 'Text', 'ElementData', 'elem', 'text',
    # CSS
    'Color', 'Unit', 'Value', 'Keyword', 'Length', 'ColorValue',
    'Selector', 'SimpleSelector', 'Declaration', 'Rule', 'Stylesheet',
    # Parsers
    'parse_html', 'parse_css',
    # Style
    'StyledNode', 'Display', 'style_tree',
    # Layout
    'Rect', 'EdgeSizes', 'Dimensions', 'BoxType', 'LayoutBox', 'layout_tree',
    # Painting
    'Canvas', 'DisplayCommand', 'SolidColor', 'paint',
]
