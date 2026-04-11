/**
 *  @(#)WikitextUtilsTest.java 0.02 23/12/2016
 *  Copyright (C) 2017 - 2018 MER-C
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version. Additionally
 *  this file is subject to the "Classpath" exception.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.wikipedia;

import java.util.*;
import java.util.function.BiFunction;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for org.wikipedia.WikitextUtils
 *  @author MER-C
 */
public class WikitextUtilsTest
{
    Wiki enWiki = Wiki.newSession("en.wikipedia.org");
    
    @Test
    public void parseWikilink()
    {
        assertEquals(new WikitextUtils.WikiLink(enWiki, "Link", null), WikitextUtils.parseWikiLink(enWiki, "[[ Link ]]"));
        assertEquals(new WikitextUtils.WikiLink(enWiki, "Link", null), WikitextUtils.parseWikiLink(enWiki, "[[:Link]]"));
        assertEquals(new WikitextUtils.WikiLink(enWiki, "Link", "Description"), WikitextUtils.parseWikiLink(enWiki, "[[ Link | Description ]]"));
        assertEquals(new WikitextUtils.WikiLink(enWiki, "Link", "Description"), WikitextUtils.parseWikiLink(enWiki, "[[:Link|Description]]"));
    }
    
    @Test
    public void removeComments()
    {
        assertEquals("A  B",        WikitextUtils.removeComments("A <!-- comment --> B"));
        assertEquals("Blah ",       WikitextUtils.removeComments("Blah <!-- Unbalanced comment"));
        assertEquals("A  B  C",     WikitextUtils.removeComments("A <!-- Two --> B <!-- Comments --> C"));
        assertEquals("A  end2 -->", WikitextUtils.removeComments("A <!-- Two ends --> end2 -->"));
        assertEquals("-->End at 0", WikitextUtils.removeComments("-->End at 0<!--"));
    }
    
    @Test
    public void formatWikiLink()
    {
        WikitextUtils.WikiLink wl = new WikitextUtils.WikiLink(enWiki, "Test1", "Test2");
        assertEquals("<a href=\"https://en.wikipedia.org/wiki/Test1\">Test2</a>", wl.format(Writable.Format.HTML));
        assertEquals("[[Test1|Test2]]", wl.format(Writable.Format.WIKITEXT));
        assertThrows(UnsupportedOperationException.class, () -> wl.format(Writable.Format.CSV), "CSV not supported");
        
        WikitextUtils.WikiLink wl2 = new WikitextUtils.WikiLink(enWiki, "Test1", null);
        assertEquals("<a href=\"https://en.wikipedia.org/wiki/Test1\">Test1</a>", wl2.format(Writable.Format.HTML));
        assertEquals("[[Test1]]", wl2.format(Writable.Format.WIKITEXT));
        
        wl2 = new WikitextUtils.WikiLink(enWiki, "File:Example.png", null);
        assertEquals("[[:File:Example.png]]", wl2.format(Writable.Format.WIKITEXT), "wikitext, image");
        wl2 = new WikitextUtils.WikiLink(enWiki, "Category:Example", null);
        assertEquals("[[:Category:Example]]", wl2.format(Writable.Format.WIKITEXT), "wikitext, category");
    }
    
    @Test
    public void formatExternalLink()
    {
        WikitextUtils.ExternalLink el = new WikitextUtils.ExternalLink("https://example.com", "Test2");
        assertEquals("<a href=\"https://example.com\">Test2</a>", el.format(Writable.Format.HTML));
        assertEquals("[https://example.com Test2]", el.format(Writable.Format.WIKITEXT));
        assertThrows(UnsupportedOperationException.class, () -> el.format(Writable.Format.CSV), "CSV not supported");
        
        WikitextUtils.ExternalLink el2 = new WikitextUtils.ExternalLink("https://example.com",  null);
        assertEquals("<a href=\"https://example.com\"></a>", el2.format(Writable.Format.HTML));
        assertEquals("[https://example.com]", el2.format(Writable.Format.WIKITEXT));
    }
    
    @Test
    public void formatHeading()
    {
        WikitextUtils.Heading hdr = new WikitextUtils.Heading("Test", 1);
        assertEquals("=Test=", hdr.format(Writable.Format.WIKITEXT), "Wikitext level 1");
        assertEquals("<h1>Test</h1>", hdr.format(Writable.Format.HTML), "HTML level 1");
        
        WikitextUtils.Heading hdr2 = new WikitextUtils.Heading("Test", 3);
        assertEquals("===Test===", hdr2.format(Writable.Format.WIKITEXT), "Wikitext level 3");
        assertEquals("<h3>Test</h3>", hdr2.format(Writable.Format.HTML), "HTML level 3");
        
        assertThrows(UnsupportedOperationException.class, () -> hdr2.format(Writable.Format.CSV), "CSV not supported");
        assertThrows(IllegalArgumentException.class, () -> new WikitextUtils.Heading("Test", 0), "Zero heading level");
        assertThrows(IllegalArgumentException.class, () -> new WikitextUtils.Heading("Test", 7), "HTML only supports 6 heading levels");
    }
    
    @Test
    public void formatPaginatedList()
    {
        BiFunction<Integer, Integer, Writable> paginator = (start, end) -> new WikitextUtils.Heading("Blah " 
            + start + " to " + end, 2);

        assertThrows(IllegalArgumentException.class,
            () -> new WikitextUtils.PaginatedList(List.of(new Writable.Identity("x")), 
                false, paginator, -4));
        
        String temp = new WikitextUtils.PaginatedList(Collections.EMPTY_LIST, false, paginator, 4).format(Writable.Format.WIKITEXT);
        assertTrue(temp.isEmpty());
                
        List<Writable> items = new ArrayList<>();
        for (int i = 1; i < 8; i++)
            items.add(new WikitextUtils.WikiLink(enWiki, "" + i, null));
        items.add(new WikitextUtils.WikiLink(enWiki, "File:Example.png", null));
        items.add(new WikitextUtils.WikiLink(enWiki, "Category:Example", null));
        items.add(new WikitextUtils.WikiLink(enWiki, "*-algebra", null));
        
        String expected = """
            ==Blah 1 to 4==
            #[[1]]
            #[[2]]
            #[[3]]
            #[[4]]
            
            ==Blah 5 to 8==
            #[[5]]
            #[[6]]
            #[[7]]
            #[[:File:Example.png]]
            
            ==Blah 9 to 10==
            #[[:Category:Example]]
            #[[*-algebra]]

            """;
        String actual = new WikitextUtils.PaginatedList(items, true, paginator, 4).format(Writable.Format.WIKITEXT);
        assertEquals(expected, actual);
        expected = expected.replace("#", "*");
        actual = new WikitextUtils.PaginatedList(items, false, paginator, 4).format(Writable.Format.WIKITEXT);
        assertEquals(expected, actual);
        expected = """
            *[[1]]
            *[[2]]
            *[[3]]
            *[[4]]
            *[[5]]
            *[[6]]
            *[[7]]
            *[[:File:Example.png]]
            *[[:Category:Example]]
            *[[*-algebra]]

            """;
        actual = new WikitextUtils.PaginatedList(items, false, null, 4).format(Writable.Format.WIKITEXT);
        assertEquals(expected, actual, "Wikitext, null paginator");
        
        expected = """
            <h2>Blah 1 to 4</h2>
            <ol>
            <li><a href="https://en.wikipedia.org/wiki/1">1</a>
            <li><a href="https://en.wikipedia.org/wiki/2">2</a>
            <li><a href="https://en.wikipedia.org/wiki/3">3</a>
            <li><a href="https://en.wikipedia.org/wiki/4">4</a>
            </ol>

            <h2>Blah 5 to 8</h2>
            <ol start=5>
            <li><a href="https://en.wikipedia.org/wiki/5">5</a>
            <li><a href="https://en.wikipedia.org/wiki/6">6</a>
            <li><a href="https://en.wikipedia.org/wiki/7">7</a>
            <li><a href="https://en.wikipedia.org/wiki/File%3AExample.png">File:Example.png</a>
            </ol>

            <h2>Blah 9 to 10</h2>
            <ol start=9>
            <li><a href="https://en.wikipedia.org/wiki/Category%3AExample">Category:Example</a>
            <li><a href="https://en.wikipedia.org/wiki/*-algebra">*-algebra</a>
            </ol>

            """;
        actual = new WikitextUtils.PaginatedList(items, true, paginator, 4).format(Writable.Format.HTML);
        assertEquals(expected, actual);
        expected = """
            <h2>Blah 1 to 4</h2>
            <ul>
            <li><a href="https://en.wikipedia.org/wiki/1">1</a>
            <li><a href="https://en.wikipedia.org/wiki/2">2</a>
            <li><a href="https://en.wikipedia.org/wiki/3">3</a>
            <li><a href="https://en.wikipedia.org/wiki/4">4</a>
            </ul>

            <h2>Blah 5 to 8</h2>
            <ul>
            <li><a href="https://en.wikipedia.org/wiki/5">5</a>
            <li><a href="https://en.wikipedia.org/wiki/6">6</a>
            <li><a href="https://en.wikipedia.org/wiki/7">7</a>
            <li><a href="https://en.wikipedia.org/wiki/File%3AExample.png">File:Example.png</a>
            </ul>

            <h2>Blah 9 to 10</h2>
            <ul>
            <li><a href="https://en.wikipedia.org/wiki/Category%3AExample">Category:Example</a>
            <li><a href="https://en.wikipedia.org/wiki/*-algebra">*-algebra</a>
            </ul>

            """;
        actual = new WikitextUtils.PaginatedList(items, false, paginator, 4).format(Writable.Format.HTML);
        assertEquals(expected, actual);
        
        expected = """
            <ul>
            <li><a href="https://en.wikipedia.org/wiki/1">1</a>
            <li><a href="https://en.wikipedia.org/wiki/2">2</a>
            <li><a href="https://en.wikipedia.org/wiki/3">3</a>
            <li><a href="https://en.wikipedia.org/wiki/4">4</a>
            <li><a href="https://en.wikipedia.org/wiki/5">5</a>
            <li><a href="https://en.wikipedia.org/wiki/6">6</a>
            <li><a href="https://en.wikipedia.org/wiki/7">7</a>
            <li><a href="https://en.wikipedia.org/wiki/File%3AExample.png">File:Example.png</a>
            <li><a href="https://en.wikipedia.org/wiki/Category%3AExample">Category:Example</a>
            <li><a href="https://en.wikipedia.org/wiki/*-algebra">*-algebra</a>
            </ul>

            """;
        actual = new WikitextUtils.PaginatedList(items, false, null, 4).format(Writable.Format.HTML);
        assertEquals(expected, actual, "HTML, null paginator");
    }
}
