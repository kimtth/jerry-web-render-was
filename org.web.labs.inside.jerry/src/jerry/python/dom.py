"""DOM module - represents the HTML document structure."""

from dataclasses import dataclass, field
from typing import Dict, List, Set, Optional, Union


@dataclass
class ElementData:
    """Represents an HTML element's tag name and attributes."""
    tag_name: str
    attributes: Dict[str, str] = field(default_factory=dict)
    
    def id(self) -> Optional[str]:
        """Return the element's id attribute if present."""
        return self.attributes.get("id")
    
    def classes(self) -> Set[str]:
        """Return the set of CSS classes on this element."""
        class_attr = self.attributes.get("class", "")
        return set(class_attr.split()) if class_attr else set()


@dataclass
class Node:
    """Base class for DOM nodes."""
    children: List['Node'] = field(default_factory=list)


@dataclass
class Text(Node):
    """A text node containing text content."""
    text: str = ""


@dataclass
class Element(Node):
    """An element node with tag name and attributes."""
    element: ElementData = field(default_factory=lambda: ElementData(""))


# Type alias for any DOM node
DOMNode = Union[Text, Element]


def text(data: str) -> Text:
    """Create a text node."""
    return Text(text=data, children=[])


def elem(tag_name: str, attrs: Dict[str, str], children: List[Node]) -> Element:
    """Create an element node."""
    return Element(
        element=ElementData(tag_name=tag_name, attributes=attrs),
        children=children
    )
