/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util;

import static net.sourceforge.pmd.util.StringUtil.getCommonPrefix;
import static net.sourceforge.pmd.util.StringUtil.getCommonSuffix;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilTest {

    @Test
    public void testColumnNumber() {
        assertEquals(-1, StringUtil.columnNumberAt("f\rah\nb", -1));
        assertEquals(1, StringUtil.columnNumberAt("f\rah\nb", 0));
        assertEquals(2, StringUtil.columnNumberAt("f\rah\nb", 1));
        assertEquals(1, StringUtil.columnNumberAt("f\rah\nb", 2));
        assertEquals(2, StringUtil.columnNumberAt("f\rah\nb", 3));
        assertEquals(3, StringUtil.columnNumberAt("f\rah\nb", 4));
        assertEquals(1, StringUtil.columnNumberAt("f\rah\nb", 5));
        assertEquals(2, StringUtil.columnNumberAt("f\rah\nb", 6));
        assertEquals(-1, StringUtil.columnNumberAt("f\rah\nb", 7));
    }

    @Test
    public void testColumnNumberCrLf() {
        assertEquals(-1, StringUtil.columnNumberAt("f\r\nb", -1));
        assertEquals(1, StringUtil.columnNumberAt("f\r\nb", 0));
        assertEquals(2, StringUtil.columnNumberAt("f\r\nb", 1));
        assertEquals(3, StringUtil.columnNumberAt("f\r\nb", 2));
        assertEquals(1, StringUtil.columnNumberAt("f\r\nb", 3));
        assertEquals(2, StringUtil.columnNumberAt("f\r\nb", 4));
        assertEquals(-1, StringUtil.columnNumberAt("f\r\nb", 5));
    }

    @Test
    public void testColumnNumberTrailing() {
        assertEquals(1, StringUtil.columnNumberAt("\n", 0));
        assertEquals(2, StringUtil.columnNumberAt("\n", 1));
        assertEquals(-1, StringUtil.columnNumberAt("\n", 2));
    }

    @Test
    public void testColumnNumberEmpty() {
        assertEquals(1, StringUtil.columnNumberAt("", 0));
        assertEquals(-1, StringUtil.columnNumberAt("", 1));
    }

    @Test
    public void testUTF8NotSupported() {
        StringBuilder sb = new StringBuilder();
        String test = "é";
        StringUtil.appendXmlEscaped(sb, test, false);
        assertEquals("&#xe9;", sb.toString());
    }

    @Test
    public void testUTF8NotSupportedSurrogates() {
        // D8 34 DD 1E -> U+1D11E
        StringBuilder sb = new StringBuilder();
        String test = new String(new char[] {0xd834, 0xdd1e});
        StringUtil.appendXmlEscaped(sb, test, false);
        assertEquals("&#x1d11e;", sb.toString());
    }

    @Test
    public void testUTF8Supported() {
        StringBuilder sb = new StringBuilder();
        String test = "é";
        StringUtil.appendXmlEscaped(sb, test, true);
        assertEquals("é", sb.toString());
    }

    @Test
    public void testRemoveSurrounding() {
        assertThat(StringUtil.removeSurrounding("", 'q'), equalTo(""));
        assertThat(StringUtil.removeSurrounding("q", 'q'), equalTo("q"));
        assertThat(StringUtil.removeSurrounding("qq", 'q'), equalTo(""));
        assertThat(StringUtil.removeSurrounding("qqq", 'q'), equalTo("q"));
    }

    @Test
    public void testElide() {
        assertThat(StringUtil.elide("abc", 2, ""), equalTo("ab"));
        assertThat(StringUtil.elide("abc", 2, "."), equalTo("a."));
        assertThat(StringUtil.elide("abc", 2, ".."), equalTo(".."));
        assertThat(StringUtil.elide("abc", 3, ".."), equalTo("abc"));
    }

    @Test
    public void testCommonSuffix() {
        assertThat(getCommonSuffix("a", "a"), equalTo("a"));
        assertThat(getCommonSuffix("a", ""), equalTo(""));
        assertThat(getCommonSuffix("", "a"), equalTo(""));
        assertThat(getCommonSuffix("abc", ""), equalTo(""));
        assertThat(getCommonSuffix("", "abc"), equalTo(""));
        assertThat(getCommonSuffix("abc", "ab"), equalTo(""));
        assertThat(getCommonSuffix("abc", "abc"), equalTo("abc"));
        assertThat(getCommonSuffix("abc", "bc"), equalTo("bc"));
        assertThat(getCommonSuffix("bc", "abc"), equalTo("bc"));
        assertThat(getCommonSuffix("abc", "xbc"), equalTo("bc"));
        assertThat(getCommonSuffix("xbc", "abc"), equalTo("bc"));
    }

    @Test
    public void testCommonPrefix() {
        assertThat(getCommonPrefix("a", "a"), equalTo("a"));
        assertThat(getCommonPrefix("a", ""), equalTo(""));
        assertThat(getCommonPrefix("", "a"), equalTo(""));
        assertThat(getCommonPrefix("abc", ""), equalTo(""));
        assertThat(getCommonPrefix("", "abc"), equalTo(""));
        assertThat(getCommonPrefix("abc", "ab"), equalTo("ab"));
        assertThat(getCommonPrefix("abc", "abc"), equalTo("abc"));
        assertThat(getCommonPrefix("abc", "bc"), equalTo(""));
        assertThat(getCommonPrefix("bc", "abc"), equalTo(""));
        assertThat(getCommonPrefix("abc", "abcx"), equalTo("abc"));
        assertThat(getCommonPrefix("abcx", "abc"), equalTo("abc"));
    }
}
