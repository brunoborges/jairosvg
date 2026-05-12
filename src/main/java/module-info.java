module io.brunoborges.jairosvg {
    requires java.desktop;
    requires java.net.http;
    requires java.xml;
    requires static org.apache.pdfbox;
    requires transitive color;

    exports io.brunoborges.jairosvg;
    exports io.brunoborges.jairosvg.cli;
}
