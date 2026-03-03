package com.jairosvg.util;

/** Parsed point with remaining unparsed string. */
public record ParsedPoint(double x, double y, String remainder) {
}
