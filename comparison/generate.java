///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//COMPILE_OPTIONS --enable-preview --release 25
//RUNTIME_OPTIONS --enable-preview
//REPOS mavenCentral,mavenLocal
//REPOS css4j=https://css4j.github.io/maven/
//DEPS com.jairosvg:jairosvg:1.0.0-SNAPSHOT
//DEPS io.sf.carte:echosvg-transcoder:2.4

import com.jairosvg.JairoSVG;
import io.sf.carte.echosvg.transcoder.TranscoderInput;
import io.sf.carte.echosvg.transcoder.TranscoderOutput;
import io.sf.carte.echosvg.transcoder.image.PNGTranscoder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class generate {

    // ── 01: Basic shapes ───────────────────────────────────────────────
    static final String SVG_01_BASIC_SHAPES = """
            <svg xmlns="http://www.w3.org/2000/svg" width="300" height="300">
              <rect x="10" y="10" width="120" height="80" fill="#4A90D9" rx="8"/>
              <circle cx="230" cy="60" r="50" fill="#E74C3C"/>
              <ellipse cx="80" cy="200" rx="70" ry="40" fill="#2ECC71"/>
              <line x1="160" y1="160" x2="290" y2="290" stroke="#8E44AD" stroke-width="4"/>
              <rect x="170" y="180" width="100" height="100" fill="none" stroke="#F39C12" stroke-width="3"/>
              <circle cx="220" cy="230" r="30" fill="#1ABC9C" stroke="#2C3E50" stroke-width="2"/>
            </svg>
            """;

    // ── 02: Gradients ──────────────────────────────────────────────────
    static final String SVG_02_GRADIENTS = """
            <svg xmlns="http://www.w3.org/2000/svg" width="300" height="300">
              <defs>
                <linearGradient id="lg1" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#ff6b6b;stop-opacity:1"/>
                  <stop offset="50%" style="stop-color:#feca57;stop-opacity:1"/>
                  <stop offset="100%" style="stop-color:#48dbfb;stop-opacity:1"/>
                </linearGradient>
                <radialGradient id="rg1" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" style="stop-color:white;stop-opacity:1"/>
                  <stop offset="100%" style="stop-color:#0abde3;stop-opacity:1"/>
                </radialGradient>
                <linearGradient id="lg2" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stop-color="#6c5ce7"/>
                  <stop offset="100%" stop-color="#a29bfe"/>
                </linearGradient>
              </defs>
              <rect width="300" height="300" fill="url(#lg1)"/>
              <circle cx="150" cy="150" r="100" fill="url(#rg1)"/>
              <rect x="30" y="220" width="240" height="50" rx="10" fill="url(#lg2)" opacity="0.8"/>
            </svg>
            """;

    // ── 03: Complex paths ──────────────────────────────────────────────
    static final String SVG_03_COMPLEX_PATHS = """
            <svg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300">
              <rect width="400" height="300" fill="#f8f9fa"/>
              <path d="M50,150 C50,50 150,50 150,150 S250,250 250,150" fill="none" stroke="#e17055" stroke-width="3"/>
              <path d="M280,30 L340,120 L220,120 Z" fill="#d63031"/>
              <path d="M30,250 Q100,180 170,250 T310,250" fill="none" stroke="#00b894" stroke-width="3"/>
              <path d="M300,180 A60,40 0 1,0 380,220" fill="none" stroke="#6c5ce7" stroke-width="3"/>
              <path d="M200,20 C220,60 260,60 260,100 C260,140 200,140 200,180 C200,140 140,140 140,100 C140,60 180,60 200,20Z" fill="#fdcb6e"/>
              <path d="M350,30 l20,40 l-40,0 Z" fill="#0984e3" stroke="#2d3436" stroke-width="1"/>
            </svg>
            """;

    // ── 04: Text rendering ─────────────────────────────────────────────
    static final String SVG_04_TEXT_RENDERING = """
            <svg xmlns="http://www.w3.org/2000/svg" width="400" height="300">
              <rect width="400" height="300" fill="#2d3436"/>
              <text x="200" y="50" text-anchor="middle" font-size="32" font-weight="bold" fill="#dfe6e9">Title Text</text>
              <text x="200" y="90" text-anchor="middle" font-size="16" fill="#b2bec3">Subtitle in lighter gray</text>
              <text x="30" y="140" font-size="20" fill="#e17055">Left aligned</text>
              <text x="370" y="140" text-anchor="end" font-size="20" fill="#00cec9">Right aligned</text>
              <text x="200" y="190" text-anchor="middle" font-size="18" fill="#ffeaa7">
                <tspan font-weight="bold">Bold</tspan>
                <tspan fill="#fab1a0"> and </tspan>
                <tspan font-style="italic" fill="#74b9ff">Italic</tspan>
              </text>
              <text x="30" y="240" font-size="14" fill="#a29bfe" font-family="monospace">monospace: 0123456789</text>
              <text x="30" y="275" font-size="12" fill="#55efc4" letter-spacing="4">S P A C E D   O U T</text>
            </svg>
            """;

    // ── 05: Transforms ─────────────────────────────────────────────────
    static final String SVG_05_TRANSFORMS = """
            <svg xmlns="http://www.w3.org/2000/svg" width="350" height="350">
              <rect width="350" height="350" fill="#dfe6e9"/>
              <g transform="translate(175,175)">
                <rect x="-30" y="-30" width="60" height="60" fill="#e74c3c"/>
                <g transform="rotate(45)">
                  <rect x="-25" y="-25" width="50" height="50" fill="#3498db" opacity="0.7"/>
                </g>
                <g transform="rotate(22.5)">
                  <rect x="-20" y="-20" width="40" height="40" fill="#2ecc71" opacity="0.7"/>
                </g>
              </g>
              <g transform="translate(60,60) scale(1.5)">
                <circle cx="0" cy="0" r="25" fill="#f39c12"/>
              </g>
              <g transform="translate(280,60) scale(0.8)">
                <rect width="60" height="60" fill="#9b59b6"/>
              </g>
              <g transform="skewX(20) translate(100,280)">
                <rect width="80" height="30" fill="#1abc9c"/>
              </g>
              <g transform="translate(250,250) rotate(30) scale(1.2)">
                <polygon points="0,-30 26,15 -26,15" fill="#e67e22"/>
              </g>
            </svg>
            """;

    // ── 06: Stroke styles ──────────────────────────────────────────────
    static final String SVG_06_STROKE_STYLES = """
            <svg xmlns="http://www.w3.org/2000/svg" width="400" height="300">
              <rect width="400" height="300" fill="#f5f6fa"/>
              <line x1="30" y1="30" x2="370" y2="30" stroke="#e74c3c" stroke-width="6" stroke-linecap="round"/>
              <line x1="30" y1="60" x2="370" y2="60" stroke="#3498db" stroke-width="4" stroke-linecap="square"/>
              <line x1="30" y1="90" x2="370" y2="90" stroke="#2ecc71" stroke-width="3" stroke-linecap="butt"/>
              <line x1="30" y1="120" x2="370" y2="120" stroke="#9b59b6" stroke-width="3" stroke-dasharray="20,10"/>
              <line x1="30" y1="150" x2="370" y2="150" stroke="#e67e22" stroke-width="3" stroke-dasharray="5,5,15,5"/>
              <line x1="30" y1="180" x2="370" y2="180" stroke="#1abc9c" stroke-width="2" stroke-dasharray="2,4"/>
              <polyline points="30,220 100,210 170,240 240,200 310,250 370,220" fill="none"
                        stroke="#e74c3c" stroke-width="4" stroke-linejoin="round"/>
              <polyline points="30,260 100,250 170,280 240,240 310,290 370,260" fill="none"
                        stroke="#3498db" stroke-width="4" stroke-linejoin="bevel"/>
            </svg>
            """;

    // ── 07: Opacity and blending ───────────────────────────────────────
    static final String SVG_07_OPACITY_BLEND = """
            <svg xmlns="http://www.w3.org/2000/svg" width="300" height="300">
              <rect width="300" height="300" fill="#2c3e50"/>
              <circle cx="120" cy="120" r="80" fill="#e74c3c" fill-opacity="0.7"/>
              <circle cx="180" cy="120" r="80" fill="#3498db" fill-opacity="0.7"/>
              <circle cx="150" cy="180" r="80" fill="#2ecc71" fill-opacity="0.7"/>
              <rect x="20" y="240" width="260" height="40" rx="6" fill="white" opacity="0.15"/>
              <rect x="30" y="248" width="80" height="24" rx="4" fill="#e74c3c" stroke="#c0392b" stroke-width="2" stroke-opacity="0.5"/>
              <rect x="120" y="248" width="80" height="24" rx="4" fill="#3498db" opacity="0.6"/>
              <rect x="210" y="248" width="60" height="24" rx="4" fill="#f39c12" fill-opacity="0.4" stroke="#e67e22" stroke-width="2"/>
            </svg>
            """;

    // ── 08: ViewBox and preserveAspectRatio ────────────────────────────
    static final String SVG_08_VIEWBOX_ASPECT = """
            <svg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 200 200" preserveAspectRatio="xMidYMid meet">
              <rect width="200" height="200" fill="#ecf0f1"/>
              <rect x="5" y="5" width="190" height="190" fill="none" stroke="#bdc3c7" stroke-width="2" stroke-dasharray="6,3"/>
              <circle cx="100" cy="100" r="80" fill="#3498db" opacity="0.4"/>
              <rect x="40" y="40" width="120" height="120" fill="#e74c3c" opacity="0.4"/>
              <line x1="0" y1="100" x2="200" y2="100" stroke="#2c3e50" stroke-width="1" stroke-dasharray="4,2"/>
              <line x1="100" y1="0" x2="100" y2="200" stroke="#2c3e50" stroke-width="1" stroke-dasharray="4,2"/>
              <text x="100" y="20" text-anchor="middle" font-size="10" fill="#7f8c8d">viewBox 200x200</text>
              <text x="100" y="195" text-anchor="middle" font-size="10" fill="#7f8c8d">output 400x300</text>
            </svg>
            """;

    // ── 09: CSS styling ────────────────────────────────────────────────
    static final String SVG_09_CSS_STYLING = """
            <svg xmlns="http://www.w3.org/2000/svg" width="300" height="300">
              <style>
                .bg { fill: #1e272e; }
                .card { fill: #485460; rx: 10; }
                .accent { fill: #ff6b6b; }
                .highlight { fill: #feca57; }
                #title { font-size: 22px; fill: #dfe6e9; font-weight: bold; }
                #subtitle { font-size: 13px; fill: #b2bec3; }
                .dot { fill: #0be881; }
                .outline { fill: none; stroke: #feca57; stroke-width: 2; }
              </style>
              <rect class="bg" width="300" height="300"/>
              <rect class="card" x="20" y="20" width="260" height="100" rx="10"/>
              <text id="title" x="150" y="60" text-anchor="middle">Dashboard</text>
              <text id="subtitle" x="150" y="85" text-anchor="middle">CSS styled SVG</text>
              <circle class="accent" cx="60" cy="180" r="30"/>
              <circle class="highlight" cx="150" cy="180" r="30"/>
              <circle class="dot" cx="240" cy="180" r="30"/>
              <rect class="outline" x="30" y="230" width="240" height="50" rx="8"/>
              <circle class="accent" cx="60" cy="255" r="8"/>
              <circle class="highlight" cx="90" cy="255" r="8"/>
              <circle class="dot" cx="120" cy="255" r="8"/>
            </svg>
            """;

    // ── 10: Use and defs ───────────────────────────────────────────────
    static final String SVG_10_USE_AND_DEFS = """
            <svg xmlns="http://www.w3.org/2000/svg" width="350" height="300">
              <defs>
                <circle id="dot" r="15"/>
                <rect id="card" width="60" height="40" rx="6"/>
                <clipPath id="circClip">
                  <circle cx="175" cy="150" r="120"/>
                </clipPath>
              </defs>
              <rect width="350" height="300" fill="#dfe6e9"/>
              <g clip-path="url(#circClip)">
                <rect width="350" height="300" fill="#6c5ce7"/>
                <line x1="0" y1="0" x2="350" y2="300" stroke="white" stroke-width="2"/>
                <line x1="350" y1="0" x2="0" y2="300" stroke="white" stroke-width="2"/>
              </g>
              <use href="#dot" x="60" y="60" fill="#e74c3c"/>
              <use href="#dot" x="175" y="60" fill="#f39c12"/>
              <use href="#dot" x="290" y="60" fill="#2ecc71"/>
              <use href="#card" x="25" y="230" fill="#e74c3c"/>
              <use href="#card" x="105" y="230" fill="#3498db"/>
              <use href="#card" x="185" y="230" fill="#f39c12"/>
              <use href="#card" x="265" y="230" fill="#2ecc71"/>
            </svg>
            """;

    // ── 11: Star polygon with fill-rule evenodd ────────────────────────
    static final String SVG_11_STAR_POLYGON = """
            <svg xmlns="http://www.w3.org/2000/svg" width="300" height="300">
              <rect width="300" height="300" fill="#2c3e50"/>
              <polygon points="150,20 180,110 275,110 195,165 220,260 150,200 80,260 105,165 25,110 120,110"
                       fill="#f1c40f" fill-rule="evenodd" stroke="#f39c12" stroke-width="2"/>
              <polygon points="150,70 165,120 215,120 175,150 190,200 150,170 110,200 125,150 85,120 135,120"
                       fill="#e74c3c" fill-rule="nonzero" stroke="#c0392b" stroke-width="1"/>
              <circle cx="150" cy="145" r="20" fill="white" opacity="0.9"/>
              <circle cx="150" cy="145" r="10" fill="#2c3e50"/>
            </svg>
            """;

    // ── 12: Nested SVGs ────────────────────────────────────────────────
    static final String SVG_12_NESTED_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="400" height="300">
              <rect width="400" height="300" fill="#ecf0f1"/>
              <text x="200" y="25" text-anchor="middle" font-size="14" fill="#2c3e50">Nested SVG Elements</text>
              <svg x="10" y="40" width="180" height="120" viewBox="0 0 100 100">
                <rect width="100" height="100" fill="#3498db"/>
                <circle cx="50" cy="50" r="40" fill="#2ecc71"/>
                <text x="50" y="55" text-anchor="middle" font-size="14" fill="white">A</text>
              </svg>
              <svg x="210" y="40" width="180" height="120" viewBox="0 0 200 200">
                <rect width="200" height="200" fill="#e74c3c"/>
                <rect x="20" y="20" width="160" height="160" fill="#f39c12" rx="20"/>
                <text x="100" y="110" text-anchor="middle" font-size="28" fill="white">B</text>
              </svg>
              <svg x="60" y="170" width="280" height="120" viewBox="0 0 300 100">
                <rect width="300" height="100" fill="#9b59b6" rx="10"/>
                <circle cx="50" cy="50" r="30" fill="#f1c40f"/>
                <circle cx="150" cy="50" r="30" fill="#1abc9c"/>
                <circle cx="250" cy="50" r="30" fill="#e74c3c"/>
                <text x="150" y="90" text-anchor="middle" font-size="12" fill="white">C: wide viewport</text>
              </svg>
            </svg>
            """;

    record TestCase(String name, String svg) {}

    static final List<TestCase> TEST_CASES = List.of(
        new TestCase("01_basic_shapes",    SVG_01_BASIC_SHAPES),
        new TestCase("02_gradients",       SVG_02_GRADIENTS),
        new TestCase("03_complex_paths",   SVG_03_COMPLEX_PATHS),
        new TestCase("04_text_rendering",  SVG_04_TEXT_RENDERING),
        new TestCase("05_transforms",      SVG_05_TRANSFORMS),
        new TestCase("06_stroke_styles",   SVG_06_STROKE_STYLES),
        new TestCase("07_opacity_blend",   SVG_07_OPACITY_BLEND),
        new TestCase("08_viewbox_aspect",  SVG_08_VIEWBOX_ASPECT),
        new TestCase("09_css_styling",     SVG_09_CSS_STYLING),
        new TestCase("10_use_and_defs",    SVG_10_USE_AND_DEFS),
        new TestCase("11_star_polygon",    SVG_11_STAR_POLYGON),
        new TestCase("12_nested_svg",      SVG_12_NESTED_SVG)
    );

    static final Path BASE_DIR = Path.of("comparison");
    static final Path SVG_DIR = BASE_DIR.resolve("svg");
    static final Path PNG_JAIRO_DIR = BASE_DIR.resolve("png/jairosvg");
    static final Path PNG_ECHO_DIR = BASE_DIR.resolve("png/echosvg");
    static final Path PNG_CAIRO_DIR = BASE_DIR.resolve("png/cairosvg");

    static byte[] renderWithEchoSVG(String svg) throws Exception {
        var transcoder = new PNGTranscoder();
        var input = new TranscoderInput(new StringReader(svg));
        var baos = new ByteArrayOutputStream();
        transcoder.transcode(input, new TranscoderOutput(baos));
        return baos.toByteArray();
    }

    static byte[] renderWithCairoSVG(Path svgPath, Path pngPath) throws Exception {
        Process process = new ProcessBuilder(
                "python3", "-m", "cairosvg",
                svgPath.toString(),
                "-f", "png",
                "-o", pngPath.toString()
        ).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("python3 -m cairosvg exited with code " + exit
                    + (output.isBlank() ? "" : ": " + output.strip()));
        }
        return Files.readAllBytes(pngPath);
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(SVG_DIR);
        Files.createDirectories(PNG_JAIRO_DIR);
        Files.createDirectories(PNG_ECHO_DIR);
        Files.createDirectories(PNG_CAIRO_DIR);

        System.out.println("=".repeat(72));
        System.out.println("  SVG Comparison Generator — JairoSVG vs EchoSVG vs CairoSVG");
        System.out.printf("  Test cases: %d%n", TEST_CASES.size());
        System.out.println("=".repeat(72));
        System.out.println();

        // Track results: name -> [jairo ok, echo ok, cairo ok]
        var results = new LinkedHashMap<String, boolean[]>();

        for (var tc : TEST_CASES) {
            System.out.println("▸ " + tc.name());

            // Save input SVG
            Path svgPath = SVG_DIR.resolve(tc.name() + ".svg");
            Files.writeString(svgPath, tc.svg(), StandardCharsets.UTF_8);
            System.out.println("    SVG saved  → " + svgPath);

            boolean jairoOk = false;
            boolean echoOk = false;
            boolean cairoOk = false;

            // Render with JairoSVG
            try {
                byte[] png = JairoSVG.svg2png(tc.svg().getBytes(StandardCharsets.UTF_8));
                Path out = PNG_JAIRO_DIR.resolve(tc.name() + ".png");
                Files.write(out, png);
                System.out.printf("    JairoSVG   → %s (%,d bytes)%n", out, png.length);
                jairoOk = true;
            } catch (Exception e) {
                System.out.println("    JairoSVG   ✗ FAILED: " + e.getMessage());
            }

            // Render with EchoSVG
            try {
                byte[] png = renderWithEchoSVG(tc.svg());
                Path out = PNG_ECHO_DIR.resolve(tc.name() + ".png");
                Files.write(out, png);
                System.out.printf("    EchoSVG    → %s (%,d bytes)%n", out, png.length);
                echoOk = true;
            } catch (Exception e) {
                System.out.println("    EchoSVG    ✗ FAILED: " + e.getMessage());
            }

            // Render with CairoSVG
            try {
                Path out = PNG_CAIRO_DIR.resolve(tc.name() + ".png");
                byte[] png = renderWithCairoSVG(svgPath, out);
                System.out.printf("    CairoSVG   → %s (%,d bytes)%n", out, png.length);
                cairoOk = true;
            } catch (Exception e) {
                System.out.println("    CairoSVG   ✗ FAILED: " + e.getMessage());
            }

            results.put(tc.name(), new boolean[]{jairoOk, echoOk, cairoOk});
            System.out.println();
        }

        // Summary table
        System.out.println("=".repeat(72));
        System.out.println("  SUMMARY");
        System.out.println("=".repeat(72));
        System.out.printf("  %-28s  %-10s  %-10s  %-10s%n", "Test Case", "JairoSVG", "EchoSVG", "CairoSVG");
        System.out.println("  " + "-".repeat(64));

        int jairoPass = 0, echoPass = 0, cairoPass = 0;
        for (var entry : results.entrySet()) {
            boolean[] r = entry.getValue();
            String jStatus = r[0] ? "✓ OK" : "✗ FAIL";
            String eStatus = r[1] ? "✓ OK" : "✗ FAIL";
            String cStatus = r[2] ? "✓ OK" : "✗ FAIL";
            System.out.printf("  %-28s  %-10s  %-10s  %-10s%n", entry.getKey(), jStatus, eStatus, cStatus);
            if (r[0]) jairoPass++;
            if (r[1]) echoPass++;
            if (r[2]) cairoPass++;
        }

        System.out.println("  " + "-".repeat(64));
        System.out.printf("  %-28s  %d/%d        %d/%d        %d/%d%n", "TOTAL",
                jairoPass, results.size(), echoPass, results.size(), cairoPass, results.size());
        System.out.println("=".repeat(72));
    }
}
