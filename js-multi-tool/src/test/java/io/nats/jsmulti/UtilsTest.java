package io.nats.jsmulti;

import org.junit.jupiter.api.Test;

import static io.nats.jsmulti.shared.Stats.format3;
import static io.nats.jsmulti.shared.Utils.parseInt;
import static io.nats.jsmulti.shared.Utils.parseLong;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UtilsTest
{
    @Test
    public void testParseInt() {
        assertEquals(2 * 1000, parseInt("2K"));
        assertEquals(2 * 1000, parseInt("2k"));
        assertEquals(2 * 1024, parseInt("2ki"));
        assertEquals(2 * 1_000_000, parseInt("2m"));
        assertEquals(2 * 1024 * 1024, parseInt("2mi"));
        assertEquals(2 * 1_000_000_000, parseInt("2g"));
        assertEquals(2147483647, parseInt("2147483647"));

        assertEquals(-2 * 1000, parseInt("-2K"));
        assertEquals(-2 * 1000, parseInt("-2k"));
        assertEquals(-2 * 1024, parseInt("-2ki"));
        assertEquals(-2 * 1_000_000, parseInt("-2m"));
        assertEquals(-2 * 1024 * 1024, parseInt("-2mi"));
        assertEquals(-2 * 1_000_000_000, parseInt("-2g"));
        assertEquals(-2147483647, parseInt("-2147483647"));

        assertEquals(1_000_000_000, parseInt("1_000,000.000"));
        assertEquals(-1_000_000_000, parseInt("-1_000,000.000"));

        assertThrows(NumberFormatException.class, () -> parseInt(""));
        assertThrows(NumberFormatException.class, () -> parseInt("not"));
        assertThrows(NumberFormatException.class, () -> parseInt("2gi"));
        assertThrows(NumberFormatException.class, () -> parseInt("2147483648"));
        assertThrows(NumberFormatException.class, () -> parseInt("-2147483649"));
    }

    @Test
    public void testParseLong() {
        assertEquals(2 * 1000, parseLong("2K"));
        assertEquals(2 * 1000, parseLong("2k"));
        assertEquals(2 * 1024, parseLong("2ki"));
        assertEquals(2 * 1_000_000, parseLong("2m"));
        assertEquals(2 * 1024 * 1024, parseLong("2mi"));
        assertEquals(2 * 1_000_000_000, parseLong("2g"));
        assertEquals(2L * 1024 * 1024 * 1024, parseLong("2gi"));
        assertEquals(2147483648L, parseLong("2147483648"));

        assertEquals(-2 * 1000, parseLong("-2K"));
        assertEquals(-2 * 1000, parseLong("-2k"));
        assertEquals(-2 * 1024, parseLong("-2ki"));
        assertEquals(-2 * 1_000_000, parseLong("-2m"));
        assertEquals(-2 * 1024 * 1024, parseLong("-2mi"));
        assertEquals(-2 * 1_000_000_000, parseLong("-2g"));
        assertEquals(-2L * 1024 * 1024 * 1024, parseLong("-2gi"));
        assertEquals(-2147483649L, parseLong("-2147483649"));

        assertEquals(1_000_000_000, parseLong("1_000,000.000"));
        assertEquals(-1_000_000_000, parseLong("-1_000,000.000"));

        assertThrows(NumberFormatException.class, () -> parseLong(""));
        assertThrows(NumberFormatException.class, () -> parseLong("not"));
    }

    @Test
    public void testFormat3() {
        assertEquals("1", format3(1));
        assertEquals("12", format3(12));
        assertEquals("123", format3(123));
        assertEquals("1,234", format3(1234));
        assertEquals("12,345", format3(12345));
        assertEquals("123,456", format3(123456));
        assertEquals("1,234,567", format3(1234567));
        assertEquals("12,345,678", format3(12345678));
        assertEquals("123,456,789", format3(123456789));
        assertEquals("1.100", format3(1.1));
        assertEquals("12.100", format3(12.1));
        assertEquals("123.100", format3(123.1));
        assertEquals("1,234.100", format3(1234.1));
        assertEquals("12,345.100", format3(12345.1));
        assertEquals("123,456.100", format3(123456.1));
        assertEquals("1,234,567.100", format3(1234567.1));
        assertEquals("12,345,678.100", format3(12345678.1));
        assertEquals("123,456,789.100", format3(123456789.1));
        assertEquals("1.120", format3(1.12));
        assertEquals("12.120", format3(12.12));
        assertEquals("123.120", format3(123.12));
        assertEquals("1,234.120", format3(1234.12));
        assertEquals("12,345.120", format3(12345.12));
        assertEquals("123,456.120", format3(123456.12));
        assertEquals("1,234,567.120", format3(1234567.12));
        assertEquals("12,345,678.120", format3(12345678.12));
        assertEquals("123,456,789.120", format3(123456789.12));
        assertEquals("1.123", format3(1.123));
        assertEquals("12.123", format3(12.123));
        assertEquals("123.123", format3(123.123));
        assertEquals("1,234.123", format3(1234.123));
        assertEquals("12,345.123", format3(12345.123));
        assertEquals("123,456.123", format3(123456.123));
        assertEquals("1,234,567.123", format3(1234567.123));
        assertEquals("12,345,678.123", format3(12345678.123));
        assertEquals("123,456,789.123", format3(123456789.123));
        assertEquals("1.123", format3(1.1234));
        assertEquals("12.123", format3(12.1234));
        assertEquals("123.123", format3(123.1234));
        assertEquals("1,234.123", format3(1234.1234));
        assertEquals("12,345.123", format3(12345.1234));
        assertEquals("123,456.123", format3(123456.1234));
        assertEquals("1,234,567.123", format3(1234567.1234));
        assertEquals("12,345,678.123", format3(12345678.1234));
        assertEquals("123,456,789.123", format3(123456789.1234));
        assertEquals("0.100", format3(.1));
        assertEquals("0.120", format3(.12));
        assertEquals("0.123", format3(.123));
        assertEquals("0.123", format3(.1234));
    }
}
