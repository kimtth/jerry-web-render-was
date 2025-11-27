"""Painting module - renders the layout tree to a canvas."""

from dataclasses import dataclass, field
from typing import List, Optional

from layout import LayoutBox, BoxType, Rect
from css import Color, ColorValue


@dataclass
class Canvas:
    """A canvas that holds pixel data."""
    pixels: List[Color] = field(default_factory=list)
    width: int = 0
    height: int = 0
    
    @classmethod
    def new(cls, width: int, height: int) -> 'Canvas':
        """Create a blank canvas."""
        white = Color(r=255, g=255, b=255, a=255)
        return cls(
            pixels=[white for _ in range(width * height)],
            width=width,
            height=height
        )
    
    def paint_item(self, item: 'DisplayCommand'):
        """Paint a display command to the canvas."""
        if isinstance(item, SolidColor):
            x0 = int(self.clamp(item.rect.x, 0.0, float(self.width)))
            y0 = int(self.clamp(item.rect.y, 0.0, float(self.height)))
            x1 = int(self.clamp(item.rect.x + item.rect.width, 0.0, float(self.width)))
            y1 = int(self.clamp(item.rect.y + item.rect.height, 0.0, float(self.height)))
            
            for y in range(y0, y1):
                for x in range(x0, x1):
                    # TODO: alpha compositing with existing pixel
                    self.pixels[y * self.width + x] = item.color
    
    def clamp(self, val: float, min_val: float, max_val: float) -> float:
        """Clamp a value between min and max."""
        return max(min_val, min(val, max_val))


@dataclass
class DisplayCommand:
    """Base class for display commands."""
    pass


@dataclass
class SolidColor(DisplayCommand):
    """A solid color display command."""
    color: Color = field(default_factory=Color)
    rect: Rect = field(default_factory=Rect)


DisplayList = List[DisplayCommand]


def paint(layout_root: LayoutBox, bounds: Rect) -> Canvas:
    """Paint a tree of LayoutBoxes to an array of pixels."""
    display_list = build_display_list(layout_root)
    canvas = Canvas.new(int(bounds.width), int(bounds.height))
    
    for item in display_list:
        canvas.paint_item(item)
    
    return canvas


def build_display_list(layout_root: LayoutBox) -> DisplayList:
    """Build a display list from a layout tree."""
    display_list: DisplayList = []
    render_layout_box(display_list, layout_root)
    return display_list


def render_layout_box(display_list: DisplayList, layout_box: LayoutBox):
    """Render a layout box and its children."""
    render_background(display_list, layout_box)
    render_borders(display_list, layout_box)
    
    for child in layout_box.children:
        render_layout_box(display_list, child)


def render_background(display_list: DisplayList, layout_box: LayoutBox):
    """Render the background of a layout box."""
    color = get_color(layout_box, "background")
    if color is not None:
        display_list.append(SolidColor(color=color, rect=layout_box.dimensions.border_box()))


def render_borders(display_list: DisplayList, layout_box: LayoutBox):
    """Render the borders of a layout box."""
    color = get_color(layout_box, "border-color")
    if color is None:
        return
    
    d = layout_box.dimensions
    border_box = d.border_box()
    
    # Left border
    display_list.append(SolidColor(
        color=color,
        rect=Rect(x=border_box.x, y=border_box.y, width=d.border.left, height=border_box.height)
    ))
    
    # Right border
    display_list.append(SolidColor(
        color=color,
        rect=Rect(x=border_box.x + border_box.width - d.border.right, y=border_box.y,
                  width=d.border.right, height=border_box.height)
    ))
    
    # Top border
    display_list.append(SolidColor(
        color=color,
        rect=Rect(x=border_box.x, y=border_box.y, width=border_box.width, height=d.border.top)
    ))
    
    # Bottom border
    display_list.append(SolidColor(
        color=color,
        rect=Rect(x=border_box.x, y=border_box.y + border_box.height - d.border.bottom,
                  width=border_box.width, height=d.border.bottom)
    ))


def get_color(layout_box: LayoutBox, name: str) -> Optional[Color]:
    """Return the specified color for CSS property name, or None if no color was specified."""
    if layout_box.box_type in (BoxType.Block, BoxType.Inline):
        if layout_box.style_node is None:
            return None
        value = layout_box.style_node.value(name)
        if isinstance(value, ColorValue):
            return value.color
        return None
    elif layout_box.box_type == BoxType.Anonymous:
        return None
    return None
