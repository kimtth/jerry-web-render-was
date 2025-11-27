"""Layout module - handles CSS block layout."""

from dataclasses import dataclass, field
from typing import List, Optional
from enum import Enum

from style import StyledNode, Display
from css import Keyword, Length, Unit


@dataclass
class Rect:
    """CSS box model rectangle. All sizes are in px."""
    x: float = 0.0
    y: float = 0.0
    width: float = 0.0
    height: float = 0.0
    
    def expanded_by(self, edge: 'EdgeSizes') -> 'Rect':
        """Return a new Rect expanded by the given edge sizes."""
        return Rect(
            x=self.x - edge.left,
            y=self.y - edge.top,
            width=self.width + edge.left + edge.right,
            height=self.height + edge.top + edge.bottom
        )


@dataclass
class EdgeSizes:
    """Edge sizes for padding, border, and margin."""
    left: float = 0.0
    right: float = 0.0
    top: float = 0.0
    bottom: float = 0.0


@dataclass
class Dimensions:
    """CSS box model dimensions."""
    content: Rect = field(default_factory=Rect)
    padding: EdgeSizes = field(default_factory=EdgeSizes)
    border: EdgeSizes = field(default_factory=EdgeSizes)
    margin: EdgeSizes = field(default_factory=EdgeSizes)
    
    def padding_box(self) -> Rect:
        """The area covered by the content area plus its padding."""
        return self.content.expanded_by(self.padding)
    
    def border_box(self) -> Rect:
        """The area covered by the content area plus padding and borders."""
        return self.padding_box().expanded_by(self.border)
    
    def margin_box(self) -> Rect:
        """The area covered by the content area plus padding, borders, and margin."""
        return self.border_box().expanded_by(self.margin)


class BoxType(Enum):
    """Layout box types."""
    Block = "block"
    Inline = "inline"
    Anonymous = "anonymous"


@dataclass
class LayoutBox:
    """A node in the layout tree."""
    dimensions: Dimensions = field(default_factory=Dimensions)
    box_type: BoxType = BoxType.Block
    style_node: Optional[StyledNode] = None
    children: List['LayoutBox'] = field(default_factory=list)
    
    def get_style_node(self) -> StyledNode:
        """Get the styled node for this layout box."""
        if self.box_type == BoxType.Anonymous:
            raise Exception("Anonymous block box has no style node")
        return self.style_node
    
    def layout(self, containing_block: Dimensions):
        """Lay out a box and its descendants."""
        if self.box_type == BoxType.Block:
            self.layout_block(containing_block)
        # TODO: Inline and Anonymous layout
    
    def layout_block(self, containing_block: Dimensions):
        """Lay out a block-level element and its descendants."""
        # Child width can depend on parent width, so we need to calculate
        # this box's width before laying out its children.
        self.calculate_block_width(containing_block)
        
        # Determine where the box is located within its container.
        self.calculate_block_position(containing_block)
        
        # Recursively lay out the children of this box.
        self.layout_block_children()
        
        # Parent height can depend on child height, so calculate_height must
        # be called after the children are laid out.
        self.calculate_block_height()
    
    def calculate_block_width(self, containing_block: Dimensions):
        """Calculate the width of a block-level non-replaced element in normal flow.
        
        http://www.w3.org/TR/CSS2/visudet.html#blockwidth
        """
        style = self.get_style_node()
        
        # `width` has initial value `auto`.
        auto = Keyword(keyword="auto")
        width = style.value("width")
        if width is None:
            width = auto
        
        # margin, border, and padding have initial value 0.
        zero = Length(length=0.0, unit=Unit.Px)
        
        margin_left = style.lookup("margin-left", "margin", zero)
        margin_right = style.lookup("margin-right", "margin", zero)
        
        border_left = style.lookup("border-left-width", "border-width", zero)
        border_right = style.lookup("border-right-width", "border-width", zero)
        
        padding_left = style.lookup("padding-left", "padding", zero)
        padding_right = style.lookup("padding-right", "padding", zero)
        
        def to_px_or_zero(v):
            if v == auto:
                return 0.0
            return v.to_px()
        
        total = sum(to_px_or_zero(v) for v in [
            margin_left, margin_right, border_left, border_right,
            padding_left, padding_right, width
        ])
        
        # If width is not auto and the total is wider than the container,
        # treat auto margins as 0.
        if width != auto and total > containing_block.content.width:
            if margin_left == auto:
                margin_left = Length(length=0.0, unit=Unit.Px)
            if margin_right == auto:
                margin_right = Length(length=0.0, unit=Unit.Px)
        
        underflow = containing_block.content.width - total
        
        width_auto = (width == auto)
        margin_left_auto = (margin_left == auto)
        margin_right_auto = (margin_right == auto)
        
        if not width_auto and not margin_left_auto and not margin_right_auto:
            # If the values are overconstrained, calculate margin_right.
            margin_right = Length(length=margin_right.to_px() + underflow, unit=Unit.Px)
        elif not width_auto and not margin_left_auto and margin_right_auto:
            margin_right = Length(length=underflow, unit=Unit.Px)
        elif not width_auto and margin_left_auto and not margin_right_auto:
            margin_left = Length(length=underflow, unit=Unit.Px)
        elif width_auto:
            # If width is set to auto, any other auto values become 0.
            if margin_left_auto:
                margin_left = Length(length=0.0, unit=Unit.Px)
            if margin_right_auto:
                margin_right = Length(length=0.0, unit=Unit.Px)
            
            if underflow >= 0.0:
                # Expand width to fill the underflow.
                width = Length(length=underflow, unit=Unit.Px)
            else:
                # Width can't be negative. Adjust the right margin instead.
                width = Length(length=0.0, unit=Unit.Px)
                margin_right = Length(length=margin_right.to_px() + underflow, unit=Unit.Px)
        elif not width_auto and margin_left_auto and margin_right_auto:
            # If margin-left and margin-right are both auto, their used values are equal.
            margin_left = Length(length=underflow / 2.0, unit=Unit.Px)
            margin_right = Length(length=underflow / 2.0, unit=Unit.Px)
        
        d = self.dimensions
        d.content.width = width.to_px()
        
        d.padding.left = padding_left.to_px()
        d.padding.right = padding_right.to_px()
        
        d.border.left = border_left.to_px()
        d.border.right = border_right.to_px()
        
        d.margin.left = margin_left.to_px()
        d.margin.right = margin_right.to_px()
    
    def calculate_block_position(self, containing_block: Dimensions):
        """Finish calculating the block's edge sizes, and position it within its containing block.
        
        http://www.w3.org/TR/CSS2/visudet.html#normal-block
        """
        style = self.get_style_node()
        d = self.dimensions
        
        # margin, border, and padding have initial value 0.
        zero = Length(length=0.0, unit=Unit.Px)
        
        # If margin-top or margin-bottom is `auto`, the used value is zero.
        d.margin.top = style.lookup("margin-top", "margin", zero).to_px()
        d.margin.bottom = style.lookup("margin-bottom", "margin", zero).to_px()
        
        d.border.top = style.lookup("border-top-width", "border-width", zero).to_px()
        d.border.bottom = style.lookup("border-bottom-width", "border-width", zero).to_px()
        
        d.padding.top = style.lookup("padding-top", "padding", zero).to_px()
        d.padding.bottom = style.lookup("padding-bottom", "padding", zero).to_px()
        
        d.content.x = (containing_block.content.x + 
                       d.margin.left + d.border.left + d.padding.left)
        
        # Position the box below all the previous boxes in the container.
        d.content.y = (containing_block.content.height + containing_block.content.y +
                       d.margin.top + d.border.top + d.padding.top)
    
    def layout_block_children(self):
        """Lay out the block's children within its content area."""
        d = self.dimensions
        for child in self.children:
            child.layout(d)
            # Increment the height so each child is laid out below the previous one.
            d.content.height = d.content.height + child.dimensions.margin_box().height
    
    def calculate_block_height(self):
        """Height of a block-level non-replaced element in normal flow with overflow visible."""
        # If the height is set to an explicit length, use that exact length.
        # Otherwise, just keep the value set by layout_block_children.
        height = self.get_style_node().value("height")
        if isinstance(height, Length):
            self.dimensions.content.height = height.to_px()
    
    def get_inline_container(self) -> 'LayoutBox':
        """Where a new inline child should go."""
        if self.box_type == BoxType.Inline or self.box_type == BoxType.Anonymous:
            return self
        elif self.box_type == BoxType.Block:
            # If we've just generated an anonymous block box, keep using it.
            # Otherwise, create a new one.
            if self.children and self.children[-1].box_type == BoxType.Anonymous:
                return self.children[-1]
            anonymous = LayoutBox(box_type=BoxType.Anonymous)
            self.children.append(anonymous)
            return anonymous
        return self


def layout_tree(node: StyledNode, containing_block: Dimensions) -> LayoutBox:
    """Transform a style tree into a layout tree."""
    # The layout algorithm expects the container height to start at 0.
    containing_block.content.height = 0.0
    
    root_box = build_layout_tree(node)
    root_box.layout(containing_block)
    return root_box


def build_layout_tree(style_node: StyledNode) -> LayoutBox:
    """Build the tree of LayoutBoxes, but don't perform any layout calculations yet."""
    # Create the root box.
    display = style_node.display()
    
    if display == Display.Block:
        box_type = BoxType.Block
    elif display == Display.Inline:
        box_type = BoxType.Inline
    else:
        raise Exception("Root node has display: none.")
    
    root = LayoutBox(box_type=box_type, style_node=style_node)
    
    # Create the descendant boxes.
    for child in style_node.children:
        child_display = child.display()
        
        if child_display == Display.Block:
            root.children.append(build_layout_tree(child))
        elif child_display == Display.Inline:
            root.get_inline_container().children.append(build_layout_tree(child))
        # Display.DisplayNone: Don't lay out nodes with display: none
    
    return root
