#!/bin/bash
# render_test.sh - Test script for running the web renderer
# Usage: ./render_test.sh [python|rust|all]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_DIR="$SCRIPT_DIR/org.web.labs.inside.jerry/src/jerry/test"
PYTHON_DIR="$SCRIPT_DIR/org.web.labs.inside.jerry/src/jerry/python"
RUST_DIR="$SCRIPT_DIR/org.web.labs.inside.jerry/src/jerry/rust"
OUTPUT_DIR="$SCRIPT_DIR/output"

# Default test files
HTML_FILE="${TEST_DIR}/test.html"
CSS_FILE="${TEST_DIR}/test.css"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Create output directory
mkdir -p "$OUTPUT_DIR"

print_header() {
    echo ""
    echo "========================================"
    echo "$1"
    echo "========================================"
}

run_python() {
    print_header "Running Python Renderer"
    
    cd "$PYTHON_DIR"
    
    if ! command -v python3 &> /dev/null && ! command -v python &> /dev/null; then
        echo -e "${RED}Error: Python is not installed${NC}"
        return 1
    fi
    
    PYTHON_CMD="python3"
    if ! command -v python3 &> /dev/null; then
        PYTHON_CMD="python"
    fi
    
    echo "Using: $PYTHON_CMD"
    echo "HTML: $HTML_FILE"
    echo "CSS: $CSS_FILE"
    echo ""
    
    $PYTHON_CMD main.py -H "$HTML_FILE" -c "$CSS_FILE" -o "$OUTPUT_DIR/output_python.png" -v
    
    if [ -f "$OUTPUT_DIR/output_python.png" ]; then
        echo -e "${GREEN}✓ Python output saved to: $OUTPUT_DIR/output_python.png${NC}"
    else
        echo -e "${YELLOW}! Output file not created (Pillow may not be installed)${NC}"
    fi
}

run_rust() {
    print_header "Running Rust Renderer"
    
    cd "$RUST_DIR"
    
    if ! command -v cargo &> /dev/null; then
        echo -e "${RED}Error: Rust/Cargo is not installed${NC}"
        return 1
    fi
    
    echo "Building Rust project..."
    cargo build --release 2>/dev/null || cargo build
    
    echo ""
    echo "HTML: $HTML_FILE"
    echo "CSS: $CSS_FILE"
    echo ""
    
    cargo run --release -- -h "$HTML_FILE" -c "$CSS_FILE" -o "$OUTPUT_DIR/output_rust.png" 2>/dev/null || \
    cargo run -- -h "$HTML_FILE" -c "$CSS_FILE" -o "$OUTPUT_DIR/output_rust.png"
    
    if [ -f "$OUTPUT_DIR/output_rust.png" ]; then
        echo -e "${GREEN}✓ Rust output saved to: $OUTPUT_DIR/output_rust.png${NC}"
    fi
}

run_all() {
    run_python
    run_rust
    
    print_header "Comparison"
    echo "Python output: $OUTPUT_DIR/output_python.png"
    echo "Rust output:   $OUTPUT_DIR/output_rust.png"
    echo ""
    echo "Compare the outputs to verify they match!"
}

show_help() {
    echo "Jerry Web Renderer - Test Script"
    echo ""
    echo "Usage: $0 [command] [options]"
    echo ""
    echo "Commands:"
    echo "  python    Run Python renderer only"
    echo "  rust      Run Rust renderer only"
    echo "  all       Run both renderers (default)"
    echo "  help      Show this help message"
    echo ""
    echo "Options:"
    echo "  --html <file>    Specify HTML file (default: test.html)"
    echo "  --css <file>     Specify CSS file (default: test.css)"
    echo ""
    echo "Examples:"
    echo "  $0 python"
    echo "  $0 rust --html custom.html --css custom.css"
    echo "  $0 all"
}

# Parse arguments
COMMAND="all"
while [[ $# -gt 0 ]]; do
    case $1 in
        python|rust|all)
            COMMAND=$1
            shift
            ;;
        help|--help|-h)
            show_help
            exit 0
            ;;
        --html)
            HTML_FILE="$2"
            shift 2
            ;;
        --css)
            CSS_FILE="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Verify test files exist
if [ ! -f "$HTML_FILE" ]; then
    echo -e "${RED}Error: HTML file not found: $HTML_FILE${NC}"
    exit 1
fi

if [ ! -f "$CSS_FILE" ]; then
    echo -e "${RED}Error: CSS file not found: $CSS_FILE${NC}"
    exit 1
fi

# Run the appropriate command
case $COMMAND in
    python)
        run_python
        ;;
    rust)
        run_rust
        ;;
    all)
        run_all
        ;;
esac

print_header "Done!"
