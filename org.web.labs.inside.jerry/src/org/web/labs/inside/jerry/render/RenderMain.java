package org.web.labs.inside.jerry.render;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class RenderMain {

	public static void main(String[] args) throws IOException {

//		String htmlPath = "D:\\workspace\\workspace_mydsl\\org.web.labs.inside.jerry\\rsc\\perf-rainbow2.html";
//		String cssPath = "D:\\workspace\\workspace_mydsl\\org.web.labs.inside.jerry\\rsc\\perf-rainbow.css";
		
		String htmlPath = "D:\\workspace\\workspace_mydsl\\org.web.labs.inside.jerry\\rsc\\test.html";
		String cssPath = "D:\\workspace\\workspace_mydsl\\org.web.labs.inside.jerry\\rsc\\test.css";

		String htmlStr = read_source(htmlPath, StandardCharsets.UTF_8);
		String cssStr = read_source(cssPath, StandardCharsets.UTF_8);

		Node root_node = new HTMLBuilder().parse(htmlStr);
		print(root_node);
		ArrayList<Rule> stylesheet = new CSSBuilder().parse(cssStr);
		print(stylesheet);
		StyledNode style_root = new StyleTree().style_tree(root_node, stylesheet);
		System.out.println();
		print(style_root);

		Dimensions viewport = new Dimensions();
		viewport.content = new Rect(0,0,800,600);
		
		LayoutBox layout_root = new LayoutTree().layout_tree(style_root, viewport);
		System.out.println();
		print(layout_root);
		Canvas canvas = new PaintingResult().paint(layout_root, layout_root.dimensions.content);

		makeImageFromArray(canvas.pixels, canvas.width, canvas.height);
	}

	private static void print(LayoutBox layout_root) {

		if(layout_root == null){
			return;
		}
		
		System.out.print("LayoutBox: ");
		if(layout_root.box_type instanceof BlockNode){
			System.out.println("blockNode");
		}else if(layout_root.box_type instanceof InlineNode){
			System.out.println("InlineNode");
		}else{
			System.out.println("AnonymousBlock");	
		}
		
		StyledNode tmp = layout_root.get_style_node();
		if(tmp.node instanceof Element){
			ElementData e = ((Element)tmp.node).element;
			System.out.println(" -" + e.tag_name);
		}else{
			String txt = ((Text)tmp.node).text;
			System.out.println(" -" + txt);
		}
		
		System.out.println("  border:" + layout_root.dimensions.border.top + "/" + layout_root.dimensions.border.left + "/" +layout_root.dimensions.border.right + "/" +layout_root.dimensions.border.bottom);
		System.out.println("  margin:" + layout_root.dimensions.margin.top + "/" + layout_root.dimensions.margin.left + "/" +layout_root.dimensions.margin.right + "/" +layout_root.dimensions.margin.bottom);
		System.out.println("  padding:" + layout_root.dimensions.padding.top + "/" + layout_root.dimensions.padding.left + "/" +layout_root.dimensions.padding.right + "/" +layout_root.dimensions.padding.bottom);
		
		Rect r = layout_root.dimensions.content;
		System.out.println(" -Rec: " + r.x + "/" +  r.y + "/" +  r.width +  "/" + r.height);
		
		for(LayoutBox child :layout_root.children){
			print(child);
		}
		
	}

	private static void print(StyledNode style_root) {

		if (style_root.node instanceof Text) {
			Text txt = (Text) style_root.node;

			System.out.println("style node text: " + txt.text);
		}
		if (style_root.node instanceof Element) {
			Element ele = (Element) style_root.node;

			System.out.println("style node element: " + ele.element.tag_name);
		}

		HashMap<String, Value> spec_val = style_root.specified_values;

		System.out.println(" -specified_values");
		for (String name : spec_val.keySet()) {
			String key = name.toString();
			Value value = spec_val.get(name);
			System.out.println("  " + key + " " + realValue(value));
		}

		ArrayList<StyledNode> sts = style_root.children;
		for (StyledNode lb : sts) {
			print(lb);
		}
	}

	private static void print(ArrayList<Rule> stylesheet) {
		System.out.println();
		for (Rule r : stylesheet) {
			ArrayList<Declaration> dl = r.declarations;
			ArrayList<Selector> sl = r.selectors;

			System.out.println("css stylesheet");

			System.out.println(" -declaration");
			for (Declaration d : dl) {
				System.out.println("  " + d.prop_name + ":" + realValue(d.value));
			}
			System.out.print(" -selector");
			for (Selector s : sl) {
				String speVal = "";
				for (int spec : s.specificity()) {
					speVal = ":" + String.valueOf(spec);
				}
				System.out.println(speVal);
			}

		}
	}

	private static void print(Node node) {
		System.out.println("html node ");

		if (node instanceof Text) {
			return;
		}

		if (node instanceof Element) {
			ArrayList<Node> nodes = ((Element) node).children;
			for (Node nd : nodes) {
				if (nd instanceof Element) {
					Element nde = (Element) nd;
					System.out.print(nde.element.tag_name);
					System.out.println(" -attributes");
					for (String name : nde.element.attributes.keySet()) {
						String key = name.toString();
						String value = nde.element.attributes.get(name);
						System.out.println("  " + key + " " + value);
					}
					print(nd);
				} else {
					Text txe = (Text) nd;
					System.out.println(txe.text);
				}
			}
		}

	}

	public static void showImageDialog(Image img) {
		JLabel picLabel = new JLabel(new ImageIcon(img));
		JOptionPane.showMessageDialog(null, picLabel, "About", JOptionPane.PLAIN_MESSAGE, null);
	}

	private static String read_source(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	private static void makeImageFromArray(Color[] pixels, int width, int height) {
		// create buffered image object img
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		// file object
		File f = null;
		// create image pixel by pixel
		System.out.println("matrix length: " + pixels.length);

		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int r = pixels[i].r;
				int g = pixels[i].g;
				int b = pixels[i].b;
				int a = pixels[i].a;
				
				int p = (a << 24) | (r << 16) | (g << 8) | b; // pixel
				img.setRGB(x, y, p);
				
				++i;
			}
		}
		System.out.println("matrix i: " + i);

		// write image
		try {
			f = new File("C:\\Users\\kim\\Desktop\\Output.png");
			ImageIO.write(img, "png", f);
		} catch (IOException e) {
			System.out.println("Error: " + e);
		}
	}

	private static String realValue(Value val) {
		String rtn = "";
		if (val instanceof Keyword) {
			rtn = ((Keyword) val).keyword;
		}
		if (val instanceof Length) {
			double val_d = val.to_px();
			rtn = String.valueOf(val_d);
		}
		if (val instanceof ColorValue) {
			Color color = ((ColorValue) val).color;
			rtn = color.r + ":" + color.g + ":" + color.b + ":" + color.a;
		}
		return rtn;
	}

}
