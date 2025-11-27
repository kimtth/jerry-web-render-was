///! Basic CSS block layout.
/**
The layout tree is a collection of boxes. 
which takes the style tree and translates it into a bunch of rectangles in a two-dimensional space. 

Layout is all about boxes. A box is a rectangular section of a web page. 
It has a width, a height, and a position on the page. 
This rectangle is called the content area because it's where the box's content is drawn. 
The content may be text, image, video, or other boxes.

A box may also have padding, borders, and margins surrounding its content area. 

Note that content grows vertically by default. 
That is, adding children to a container generally makes it taller, not wider.
Another way to say this is that, by default, the width of a block or line depends on its container's width, 
while the height of a container depends on its children's heights.

**/

package org.web.labs.inside.jerry.render;

import java.util.ArrayList;

interface BoxType {
}

// CSS box model. All sizes are in px.
class Rect {
	double x;
	double y;
	double width;
	double height;

	public Rect(double x, double y, double width, double height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public Rect() {
	}

	public Rect expanded_by(EdgeSizes edge) {
		// Return a new Rect without mutating the original
		return new Rect(
			this.x - edge.left,
			this.y - edge.top,
			this.width + edge.left + edge.right,
			this.height + edge.top + edge.bottom
		);
	}
}

class Dimensions {
	/// Position of the content area relative to the document origin:
	Rect content;
	// Surrounding edges:
	EdgeSizes padding;
	EdgeSizes border;
	EdgeSizes margin;

	Dimensions() {
		this.content = new Rect();
		this.padding = new EdgeSizes();
		this.border = new EdgeSizes();
		this.margin = new EdgeSizes();
	}

	/// The area covered by the content area plus its padding.
	public Rect padding_box() {
		return this.content.expanded_by(this.padding);
	}

	/// The area covered by the content area plus padding and borders.
	public Rect border_box() {
		return this.padding_box().expanded_by(this.border);
	}

	public Rect margin_box() {
		return this.border_box().expanded_by(this.margin);
	}
}

class EdgeSizes {
	double left;
	double right;
	double top;
	double bottom;
}

/// A node in the layout tree.
class LayoutBox {
	Dimensions dimensions;
	BoxType box_type;
	ArrayList<LayoutBox> children;

	public LayoutBox(BoxType box_type) {
		this.dimensions = new Dimensions();
		this.box_type = box_type;
		this.children = new ArrayList<LayoutBox>();
	}

	public StyledNode get_style_node() {
		StyledNode stynode = null;

		if (this.box_type instanceof BlockNode) {
			return ((BlockNode) box_type).stynode;
		}
		if (this.box_type instanceof InlineNode) {
			return ((InlineNode) box_type).stynode;
		}
		if (this.box_type instanceof AnonymousBlock) {
			System.err.println("Anonymous block box has no style node");
		}

		return stynode;
	}

	public void layout(Dimensions containing_block) {

		/// Lay out a box and its descendants.
		if (this.box_type instanceof BlockNode) {
			layout_block(containing_block);
		}
		if (this.box_type instanceof InlineNode) {
			// TODO
		}
		if (this.box_type instanceof AnonymousBlock) {
			// TODO
		}
	}

	private void layout_block(Dimensions containing_block) {
		// Child width can depend on parent width, so we need to calculate this
		// box's width before
		// laying out its children.
		calculate_block_width(containing_block);

		// Determine where the box is located within its container.
		calculate_block_position(containing_block);

		// Recursively lay out the children of this box.
		layout_block_children();

		// Parent height can depend on child height, so `calculate_height` must
		// be called after the
		// children are laid out.
		calculate_block_height();
	}

	/// Calculate the width of a block-level non-replaced element in normal
	/// flow.
	///
	/// http://www.w3.org/TR/CSS2/visudet.html#blockwidth
	///
	/// Sets the horizontal margin/padding/border dimensions, and the `width`.
	private void calculate_block_width(Dimensions containing_block) {
		StyledNode style = get_style_node();

		// `width` has initial value `auto`.
		Value auto = new Keyword("auto");
		Value width = style.value("width") == null ? auto : style.value("width");

		// margin, border, and padding have initial value 0.
		Value zero = new Length(0.0, new Px());

		Value margin_left = style.lookup("margin-left", "margin", zero);
		Value margin_right = style.lookup("margin-right", "margin", zero);

		Value border_left = style.lookup("border-left-width", "border-width", zero);
		Value border_right = style.lookup("border-right-width", "border-width", zero);

		Value padding_left = style.lookup("padding-left", "padding", zero);
		Value padding_right = style.lookup("padding-right", "padding", zero);

		Double total = filterAuto(margin_left) + filterAuto(margin_right) + filterAuto(border_left)
				+ filterAuto(border_right) + filterAuto(padding_left) + filterAuto(padding_right) + filterAuto(width);

		// If width is not auto and the total is wider than the container, treat
		// auto margins as 0.
		if (!width.equals(auto) && total > containing_block.content.width) {
			if (margin_left == auto) {
				margin_left = new Length(0.0, new Px());
			}
			if (margin_right == auto) {
				margin_right = new Length(0.0, new Px());
			}
		}

		// Adjust used values so that the above sum equals
		// `containing_block.width`.
		// Each arm of the `match` should increase the total width by exactly
		// `underflow`,
		// and afterward all values should be absolute lengths in px.
		Double underflow = containing_block.content.width - total;

		boolean isFlag_1 = width == auto;
		boolean isFlag_2 = margin_left == auto;
		boolean isFlag_3 = margin_right == auto;

		// If the values are over constrained, calculate margin_right.
		if (isFlag_1 == false && isFlag_2 == false && isFlag_3 == false) {
			margin_right = new Length(margin_right.to_px() + underflow, new Px());
		}

		// If exactly one size is auto, its used value follows from the
		// equality.
		if (isFlag_1 == false && isFlag_2 == false && isFlag_3 == true) {
			margin_right = new Length(underflow, new Px());
		}
		if (isFlag_1 == false && isFlag_2 == true && isFlag_3 == false) {
			margin_left = new Length(underflow, new Px());
		}

		// If width is set to auto, any other auto values become 0.
		if (isFlag_1 == true) {
			if (margin_left == auto) {
				margin_left = new Length(0.0, new Px());
			}
			if (margin_right == auto) {
				margin_right = new Length(0.0, new Px());
			}

			if (underflow >= 0.0) {
				// Expand width to fill the underflow.
				width = new Length(underflow, new Px());
			} else {
				// Width can't be negative. Adjust the right margin instead.
				width = new Length(0.0, new Px());
				margin_right = new Length(margin_right.to_px() + underflow, new Px());
			}
		}

		// If margin-left and margin-right are both auto, their used values are
		// equal.
		if (isFlag_1 == false && isFlag_2 == true && isFlag_3 == true) {
			margin_left = new Length(underflow / 2.0, new Px());
			margin_right = new Length(underflow / 2.0, new Px());
		}

		Dimensions d = this.dimensions;
		d.content.width = width.to_px();

		d.padding.left = padding_left.to_px();
		d.padding.right = padding_right.to_px();

		d.border.left = border_left.to_px();
		d.border.right = border_right.to_px();

		d.margin.left = margin_left.to_px();
		d.margin.right = margin_right.to_px();
	}

	private Double filterAuto(Value value) {
		Value auto = new Keyword("auto");
		if (value.equals(auto) == false) {
			return value.to_px();
		}
		return 0.0;
	}

	/// Finish calculating the block's edge sizes, and position it within its
	/// containing block.
	///
	/// http://www.w3.org/TR/CSS2/visudet.html#normal-block
	///
	/// Sets the vertical margin/padding/border dimensions, and the `x`, `y`
	/// values.
	private void calculate_block_position(Dimensions containing_block) {
		StyledNode style = get_style_node();
		Dimensions d = this.dimensions;

		// margin, border, and padding have initial value 0.
		Value zero = new Length(0.0, new Px());

		// If margin-top or margin-bottom is `auto`, the used value is zero.
		d.margin.top = style.lookup("margin-top", "margin", zero).to_px();
		d.margin.bottom = style.lookup("margin-bottom", "margin", zero).to_px();

		d.border.top = style.lookup("border-top-width", "border-width", zero).to_px();
		d.border.bottom = style.lookup("border-bottom-width", "border-width", zero).to_px();

		d.padding.top = style.lookup("padding-top", "padding", zero).to_px();
		d.padding.bottom = style.lookup("padding-bottom", "padding", zero).to_px();

		d.content.x = containing_block.content.x + d.margin.left + d.border.left + d.padding.left;

		// Position the box below all the previous boxes in the container.
		d.content.y = containing_block.content.height + containing_block.content.y + d.margin.top + d.border.top
				+ d.padding.top;

	}

	/// Lay out the block's children within its content area.
	///
	/// Sets `self.dimensions.height` to the total content height.
	private void layout_block_children() {
		Dimensions d = this.dimensions;
		for (LayoutBox child : this.children) {
			child.layout(d);
			// Increment the height so each child is laid out below the previous one.
			d.content.height = d.content.height + child.dimensions.margin_box().height;
		}
	}

	/// Height of a block-level non-replaced element in normal flow with
	/// overflow visible.
	private void calculate_block_height() {
		// If the height is set to an explicit length, use that exact length.
		// Otherwise, just keep the value set by `layout_block_children`.

		StyledNode stynode = this.get_style_node();
		Length h = (Length) stynode.value("height");
		if (h != null) {
			this.dimensions.content.height = h.to_px();
		}
	}

	/// Where a new inline child should go.
	public LayoutBox get_inline_container() {
		// If we've just generated an anonymous block box, keep using it.
		// Otherwise, create a new one.
		if (this.box_type instanceof BlockNode) {
			// Check if last child is an anonymous block
			if (!this.children.isEmpty()) {
				LayoutBox last = this.children.get(this.children.size() - 1);
				if (last.box_type instanceof AnonymousBlock) {
					return last;
				}
			}
			// Create a new anonymous block and return it
			LayoutBox anonymousBlock = new LayoutBox(new AnonymousBlock());
			this.children.add(anonymousBlock);
			return anonymousBlock;
		} else if (this.box_type instanceof InlineNode) {
			return this;
		} else if (this.box_type instanceof AnonymousBlock) {
			return this;
		} else {
			return null;
		}
	}
}

class BlockNode implements BoxType {
	StyledNode stynode;

	public BlockNode(StyledNode stylenode) {
		this.stynode = stylenode;
	}
}

class InlineNode implements BoxType {
	StyledNode stynode;

	public InlineNode(StyledNode stylenode) {
		this.stynode = stylenode;
	}
}

class AnonymousBlock implements BoxType {
}

public class LayoutTree {

	/// Transform a style tree into a layout tree.
	public LayoutBox layout_tree(StyledNode node, Dimensions containing_block) {
		// The layout algorithm expects the container height to start at 0.
		// TODO: Save the initial containing block height, for calculating
		// percent heights.
		containing_block.content.height = 0.0;

		LayoutBox root_box = build_layout_tree(node);
		root_box.layout(containing_block);
		return root_box;
	}

	/// Build the tree of LayoutBoxes, but don't perform any layout calculations
	/// yet.
	private LayoutBox build_layout_tree(StyledNode stylenode) {
		// Create the root box.
		Display display = stylenode.display();
		LayoutBox root_layout = null;

		if (display instanceof Block) {
			root_layout = new LayoutBox(new BlockNode(stylenode));
		} else if (display instanceof Inline) {
			root_layout = new LayoutBox(new InlineNode(stylenode));
		} else {
			System.err.println("Root node has display: none.");
		}

		// Create the descendant boxes.
		if (stylenode.children != null && stylenode.children.size() > 0) {
			for (StyledNode child : stylenode.children) {
				// Use child's display type, not parent's
				Display child_d = child.display();

				if (child_d instanceof Block) {
					root_layout.children.add(build_layout_tree(child));
				} else if (child_d instanceof Inline) {
					root_layout.get_inline_container().children.add(build_layout_tree(child));
				} else if (child_d instanceof None) {
					// Don't lay out nodes with display: none
				}
			}
		}
		return root_layout;
	}
}
