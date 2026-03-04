module io.brunoborges.jairosvg {
    requires java.desktop;
    requires java.net.http;
    requires java.xml;
    requires static org.apache.pdfbox;

    exports io.brunoborges.jairosvg;
    exports io.brunoborges.jairosvg.cli;
    exports io.brunoborges.jairosvg.css;
    exports io.brunoborges.jairosvg.dom;
    exports io.brunoborges.jairosvg.draw;
    exports io.brunoborges.jairosvg.surface;
    exports io.brunoborges.jairosvg.util;
}
