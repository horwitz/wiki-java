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

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for org.wikipedia.WikitextUtils
 *  @author MER-C
 */
public class WikitextUtilsTest
{
    @Test
    public void parseWikilink()
    {
        Wiki wiki = Wiki.newSession("en.wikipedia.org");
        assertEquals(new WikitextUtils.WikiLink(wiki, "Link", null), WikitextUtils.parseWikiLink(wiki, "[[ Link ]]"));
        assertEquals(new WikitextUtils.WikiLink(wiki, "Link", null), WikitextUtils.parseWikiLink(wiki, "[[:Link]]"));
        assertEquals(new WikitextUtils.WikiLink(wiki, "Link", "Description"), WikitextUtils.parseWikiLink(wiki, "[[ Link | Description ]]"));
        assertEquals(new WikitextUtils.WikiLink(wiki, "Link", "Description"), WikitextUtils.parseWikiLink(wiki, "[[:Link|Description]]"));
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
        Wiki enWiki = Wiki.newSession("en.wikipedia.org");
        WikitextUtils.WikiLink wl = new WikitextUtils.WikiLink(enWiki, "Test1", "Test2");
        assertEquals("<a href=\"https://en.wikipedia.org/wiki/Test1\">Test2</a>", wl.format(Writable.Format.HTML));
        assertEquals("[[Test1|Test2]]", wl.format(Writable.Format.WIKITEXT));
        assertThrows(UnsupportedOperationException.class, () -> wl.format(Writable.Format.CSV), "CSV not supported");
        
        WikitextUtils.WikiLink wl2 = new WikitextUtils.WikiLink(enWiki, "Test1", null);
        assertEquals("<a href=\"https://en.wikipedia.org/wiki/Test1\">Test1</a>", wl2.format(Writable.Format.HTML));
        assertEquals("[[Test1]]", wl2.format(Writable.Format.WIKITEXT));
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
}
