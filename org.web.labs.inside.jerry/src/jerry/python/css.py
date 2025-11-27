"""CSS module - represents CSS stylesheets and values."""

from dataclasses import dataclass, field
from typing import List, Tuple, Optional
from enum import Enum


@dataclass
class Color:
    """RGBA color."""
    r: int = 0
    g: int = 0
    b: int = 0
    a: int = 255


class Unit(Enum):
    """CSS length units."""
    Px = "px"


@dataclass
class Value:
    """Base class for CSS values."""
    
    def to_px(self) -> float:
        """Return the size of a length in px, or zero for non-lengths."""
        return 0.0


@dataclass
class Keyword(Value):
    """A CSS keyword value."""
    keyword: str = ""
    
    def to_px(self) -> float:
        return 0.0
    
    def __eq__(self, other):
        if isinstance(other, Keyword):
            return self.keyword == other.keyword
        return False


@dataclass
class Length(Value):
    """A CSS length value."""
    length: float = 0.0
    unit: Unit = Unit.Px
    
    def to_px(self) -> float:
        return self.length
    
    def __eq__(self, other):
        if isinstance(other, Length):
            return self.length == other.length and self.unit == other.unit
        return False


@dataclass
class ColorValue(Value):
    """A CSS color value."""
    color: Color = field(default_factory=Color)
    
    def to_px(self) -> float:
        return 0.0


# Type alias for specificity (a, b, c) tuple
Specificity = Tuple[int, int, int]


@dataclass
class SimpleSelector:
    """A simple CSS selector."""
    tag_name: Optional[str] = None
    id: Optional[str] = None
    class_names: List[str] = field(default_factory=list)
    
    def specificity(self) -> Specificity:
        """Calculate selector specificity per http://www.w3.org/TR/selectors/#specificity"""
        a = 1 if self.id else 0
        b = len(self.class_names)
        c = 1 if self.tag_name else 0
        return (a, b, c)


@dataclass
class Selector:
    """A CSS selector (currently only simple selectors)."""
    simple: SimpleSelector = field(default_factory=SimpleSelector)
    
    def specificity(self) -> Specificity:
        return self.simple.specificity()


@dataclass
class Declaration:
    """A CSS declaration (property: value)."""
    name: str = ""
    value: Value = field(default_factory=Value)


@dataclass
class Rule:
    """A CSS rule (selectors + declarations)."""
    selectors: List[Selector] = field(default_factory=list)
    declarations: List[Declaration] = field(default_factory=list)


@dataclass
class Stylesheet:
    """A CSS stylesheet containing rules."""
    rules: List[Rule] = field(default_factory=list)
