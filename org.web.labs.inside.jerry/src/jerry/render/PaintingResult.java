package org.web.labs.inside.jerry.render;

import java.util.ArrayList;

class Canvas {
	Color[] pixels;
	int width;
	int height;

	/// Create a blank canvas
	public Canvas(Color[] pixels, int width, int height) {
		Color white = new Color(255, 255, 255, 255);  // Alpha should be 255 (opaque)

		this.pixels = pixels;
		// create a Vec<T> by using the vec! macro:
		// let v = vec![0; 10]; // ten zeroes
		for (int i = 0; i < width * height; i++) {
			this.pixels[i] = white;
		}
		this.width = width;
		this.height = height;
	}

	public void paint_item(DisplayCommand item) {
		if (item instanceof SolidColor) {
			// f32: The 32-bit floating point type.
			// usize: The pointer-sized unsigned integer type.
			SolidColor itm = (SolidColor) item;

			double d_width = Double.valueOf(width);
			double d_height = Double.valueOf(height);
			double s_width = Double.valueOf(itm.rect.x) + Double.valueOf(itm.rect.width);
			double s_height = Double.valueOf(itm.rect.y) + Double.valueOf(itm.rect.height);

			int x0 = (int) clamp(0.0, d_width, itm.rect.x);
			int y0 = (int) clamp(0.0, d_height, itm.rect.y);
			int x1 = (int) clamp(0.0, d_width, s_width);
			int y1 = (int) clamp(0.0, d_height, s_height);

			for (int y = y0; y < y1; y++) {
				for (int x = x0; x < x1; x++) {
					// TODO: alpha compositing with existing pixel
					int idx = y * width + x;
					pixels[idx] = itm.color == null ? pixels[idx] : itm.color;
				}
			}

		}
	}

	// input.max(min).min(max);
	// https://internals.rust-lang.org/t/clamp-function-for-primitive-types/4999
	private double clamp(double min, double max, double clampVal) {
		if (clampVal < min) {
			return min;
		} else if (clampVal > max) {
			return max;
		} else {
			return clampVal;
		}
	}
}

class DisplayCommand {
	Color color;
	Rect rect;
}

class SolidColor extends DisplayCommand {
	public SolidColor(Color color, Rect rect) {
		this.color = color;
		this.rect = rect;
	}
}

//add kim
class DrawText extends DisplayCommand {
	String text;
	
	public DrawText(String text, Rect rect) {
		this.text = text;
		this.rect = rect;
	}
}

public class PaintingResult {

	/// Paint a tree of LayoutBoxes to an array of pixels.
	public Canvas paint(LayoutBox layout_root, Rect bounds) {
		ArrayList<DisplayCommand> display_list = build_display_list(layout_root);
		int arraySize = (int) bounds.width * (int) bounds.height;
		
		Canvas canvas = new Canvas(new Color[arraySize], (int) bounds.width, (int) bounds.height);
		
		for (DisplayCommand item : display_list) {
			if (item != null)
				canvas.paint_item(item);
		}
		return canvas;
	}

	public ArrayList<DisplayCommand> build_display_list(LayoutBox layout_root) {
		ArrayList<DisplayCommand> list = new ArrayList<DisplayCommand>();
		render_layout_box(list, layout_root);
		return list;
	}

	private void render_layout_box(ArrayList<DisplayCommand> list, LayoutBox layout_box) {
		render_background(list, layout_box);
		render_borders(list, layout_box);

		for (LayoutBox child : layout_box.children) {
			render_layout_box(list, child);
		}
	}

	private void render_background(ArrayList<DisplayCommand> list, LayoutBox layout_box) {
		Color color = get_color(layout_box, "background");
		// Only add background if color is specified (matching Rust's Option.map behavior)
		if (color != null) {
			list.add(new SolidColor(color, layout_box.dimensions.border_box()));
		}
	}

	private void render_borders(ArrayList<DisplayCommand> list, LayoutBox layout_box) {
		Color color = get_color(layout_box, "border-color");
		if (color == null) {
			return;
		}

		Dimensions d = layout_box.dimensions;
		Rect border_box = d.border_box();

		// Left border
		list.add(new SolidColor(color, new Rect(border_box.x, border_box.y, d.border.left, border_box.height)));

		// Right border
		list.add(new SolidColor(color, new Rect(border_box.x + border_box.width - d.border.right, border_box.y,
				d.border.right, border_box.height)));

		// Top border
		list.add(new SolidColor(color, new Rect(border_box.x, border_box.y, border_box.width, d.border.top)));

		// Bottom border
		list.add(new SolidColor(color, new Rect(border_box.x, border_box.y + border_box.height - d.border.bottom,
				border_box.width, d.border.bottom)));
	}

	private Color get_color(LayoutBox layout_box, String name) {
		BoxType boxtype = layout_box.box_type;
		Color color;
		Object colval;

		// Return null if no color specified (matching Rust's Option<Color>)
		if (boxtype instanceof BlockNode) {
			colval = ((BlockNode) boxtype).stynode.value(name);
			color = (colval instanceof ColorValue) ? ((ColorValue) colval).color : null;
		} else if (boxtype instanceof InlineNode) {
			colval = ((InlineNode) boxtype).stynode.value(name);
			color = (colval instanceof ColorValue) ? ((ColorValue) colval).color : null;
		} else if (boxtype instanceof AnonymousBlock) {
			color = null;
		} else {
			color = null;
		}

		return color;
	}

}
