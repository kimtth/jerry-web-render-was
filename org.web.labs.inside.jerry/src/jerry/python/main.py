"""Main module - entry point for the web renderer."""

import sys
from pathlib import Path

# Add current directory to path for imports
sys.path.insert(0, str(Path(__file__).parent))

from dom import Node
from html_parser import parse as parse_html
from css_parser import parse as parse_css
from style import style_tree, StyledNode
from layout import layout_tree, Dimensions, Rect, LayoutBox, BoxType
from painting import paint, Canvas


def read_source(filename: str) -> str:
    """Read a file and return its contents."""
    with open(filename, 'r', encoding='utf-8') as f:
        return f.read()


def save_image(canvas: Canvas, filename: str):
    """Save the canvas to a PNG file."""
    try:
        from PIL import Image
        
        img = Image.new('RGBA', (canvas.width, canvas.height))
        pixels = []
        
        for color in canvas.pixels:
            pixels.append((color.r, color.g, color.b, color.a))
        
        img.putdata(pixels)
        img.save(filename, 'PNG')
        print(f"Saved output as {filename}")
    except ImportError:
        print("PIL/Pillow not installed. Cannot save image.")
        print("Install with: pip install Pillow")


def print_styled_node(node: StyledNode, indent: int = 0):
    """Print a styled node tree for debugging."""
    prefix = "  " * indent
    
    from dom import Element, Text
    if isinstance(node.node, Text):
        print(f"{prefix}StyledNode(text): {node.node.text[:50]}...")
    elif isinstance(node.node, Element):
        print(f"{prefix}StyledNode(element): {node.node.element.tag_name}")
    
    print(f"{prefix}  specified_values:")
    for name, value in node.specified_values.items():
        print(f"{prefix}    {name}: {value}")
    
    for child in node.children:
        print_styled_node(child, indent + 1)


def print_layout_box(box: LayoutBox, indent: int = 0):
    """Print a layout box tree for debugging."""
    prefix = "  " * indent
    
    box_type_str = box.box_type.value
    print(f"{prefix}LayoutBox: {box_type_str}")
    
    if box.style_node:
        from dom import Element, Text
        if isinstance(box.style_node.node, Element):
            print(f"{prefix}  tag: {box.style_node.node.element.tag_name}")
        elif isinstance(box.style_node.node, Text):
            print(f"{prefix}  text: {box.style_node.node.text[:30]}...")
    
    d = box.dimensions
    print(f"{prefix}  border: {d.border.top}/{d.border.left}/{d.border.right}/{d.border.bottom}")
    print(f"{prefix}  margin: {d.margin.top}/{d.margin.left}/{d.margin.right}/{d.margin.bottom}")
    print(f"{prefix}  padding: {d.padding.top}/{d.padding.left}/{d.padding.right}/{d.padding.bottom}")
    print(f"{prefix}  content: x={d.content.x}, y={d.content.y}, w={d.content.width}, h={d.content.height}")
    
    for child in box.children:
        print_layout_box(child, indent + 1)


def main():
    """Main entry point."""
    import argparse
    
    parser = argparse.ArgumentParser(description='A toy web renderer')
    parser.add_argument('-H', '--html', default='test.html', help='HTML document')
    parser.add_argument('-c', '--css', default='test.css', help='CSS stylesheet')
    parser.add_argument('-o', '--output', default='output.png', help='Output file')
    parser.add_argument('-v', '--verbose', action='store_true', help='Verbose output')
    
    args = parser.parse_args()
    
    # Read input files
    html_source = read_source(args.html)
    css_source = read_source(args.css)
    
    # Parse HTML and CSS
    root_node = parse_html(html_source)
    stylesheet = parse_css(css_source)
    
    if args.verbose:
        print("=== HTML Parsed ===")
        print(root_node)
    
    # Apply styles
    style_root = style_tree(root_node, stylesheet)
    
    if args.verbose:
        print("\n=== Style Tree ===")
        print_styled_node(style_root)
    
    # Set up viewport
    viewport = Dimensions()
    viewport.content = Rect(x=0, y=0, width=800, height=600)
    
    # Build layout tree
    layout_root = layout_tree(style_root, viewport)
    
    if args.verbose:
        print("\n=== Layout Tree ===")
        print_layout_box(layout_root)
    
    # Paint to canvas
    canvas = paint(layout_root, layout_root.dimensions.content)
    
    print(f"Canvas size: {canvas.width}x{canvas.height}")
    print(f"Total pixels: {len(canvas.pixels)}")
    
    # Save output
    save_image(canvas, args.output)


if __name__ == '__main__':
    main()
