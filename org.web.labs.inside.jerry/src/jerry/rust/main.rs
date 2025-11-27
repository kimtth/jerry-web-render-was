use std::default::Default;
use std::io::Read;
use std::fs::File;
use std::env;
use std::path::Path;

pub mod css;
pub mod dom;
pub mod html;
pub mod layout;
pub mod style;
pub mod painting;

fn main() {
    // Simple argument parsing
    let args: Vec<String> = env::args().collect();
    
    let mut html_file = String::from("examples/test.html");
    let mut css_file = String::from("examples/test.css");
    let mut output_file = String::from("output.png");
    
    let mut i = 1;
    while i < args.len() {
        match args[i].as_str() {
            "-h" | "--html" => {
                if i + 1 < args.len() {
                    html_file = args[i + 1].clone();
                    i += 1;
                }
            }
            "-c" | "--css" => {
                if i + 1 < args.len() {
                    css_file = args[i + 1].clone();
                    i += 1;
                }
            }
            "-o" | "--output" => {
                if i + 1 < args.len() {
                    output_file = args[i + 1].clone();
                    i += 1;
                }
            }
            "--help" => {
                println!("Usage: jerry-render [options]");
                println!();
                println!("Options:");
                println!("  -h, --html <file>    HTML input file");
                println!("  -c, --css <file>     CSS input file");
                println!("  -o, --output <file>  Output PNG file");
                println!("      --help           Show this help message");
                return;
            }
            _ => {}
        }
        i += 1;
    }

    // Read input files:
    let html_content = read_source(&html_file);
    let css_content = read_source(&css_file);

    // Since we don't have an actual window, hard-code the "viewport" size.
    let mut viewport: layout::Dimensions = Default::default();
    viewport.content.width = 800.0;
    viewport.content.height = 600.0;

    // Parsing and rendering:
    let root_node = html::parse(html_content);
    let stylesheet = css::parse(css_content);
    let style_root = style::style_tree(&root_node, &stylesheet);
    let layout_root = layout::layout_tree(&style_root, viewport);

    // Paint to canvas
    let canvas = painting::paint(&layout_root, viewport.content);
    let (w, h) = (canvas.width as u32, canvas.height as u32);
    
    // Create image buffer
    let img = image::ImageBuffer::from_fn(w, h, |x, y| {
        let color = canvas.pixels[(y * w + x) as usize];
        image::Rgba([color.r, color.g, color.b, color.a])
    });
    
    // Save output
    match img.save(Path::new(&output_file)) {
        Ok(_) => println!("Saved output as {}", output_file),
        Err(e) => println!("Error saving output: {}", e),
    }
}

fn read_source(filename: &str) -> String {
    let mut content = String::new();
    File::open(filename)
        .expect(&format!("Failed to open file: {}", filename))
        .read_to_string(&mut content)
        .expect(&format!("Failed to read file: {}", filename));
    content
}
