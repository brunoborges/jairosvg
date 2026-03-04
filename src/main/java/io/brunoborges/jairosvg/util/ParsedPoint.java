package io.brunoborges.jairosvg.util;

/** Parsed point with remaining unparsed string. */
public value record ParsedPoint(double x, double y, String remainder) {
}
