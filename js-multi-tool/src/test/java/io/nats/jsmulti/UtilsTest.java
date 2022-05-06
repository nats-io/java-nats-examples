package io.nats.jsmulti;

import org.junit.jupiter.api.Test;

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

        assertEquals(1_000_000_000, parseInt(" 1_000,000.000"));
        assertEquals(-1_000_000_000, parseInt(" -1_000,000.000"));

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

        assertEquals(1_000_000_000, parseLong(" 1_000,000.000"));
        assertEquals(-1_000_000_000, parseLong(" -1_000,000.000"));

        assertThrows(NumberFormatException.class, () -> parseLong(""));
        assertThrows(NumberFormatException.class, () -> parseLong("not"));
    }
}
