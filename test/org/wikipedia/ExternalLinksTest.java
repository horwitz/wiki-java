/**
 *  @(#)ExternalLinksTest.java 0.01 03/04/2018
 *  Copyright (C) 2018 - 20xx MER-C
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

import java.util.List;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Tests for {@link org.wikipedia.ExternalLinks}.
 *  @author MER-C
 */
public class ExternalLinksTest
{
    private final Wiki enWiki = WMFWikiFarm.instance().sharedSession("en.wikipedia.org");
    private final ExternalLinks el = ExternalLinks.of(enWiki);
    
    @Test
    public void extractDomain()
    {
        assertEquals("example.com.au", ExternalLinks.extractDomain("http://example.com.au"), "just domain");
        assertEquals("example.com", ExternalLinks.extractDomain("http://example.com/index.html"), "plain URL");
        assertEquals("example.com", ExternalLinks.extractDomain("https://www.example.com:443"), "port");
        assertEquals("example.com", ExternalLinks.extractDomain("https://www.example.com"), "www");
        assertEquals("example.com", ExternalLinks.extractDomain("//www.example.com/test.jsp?param=yes"),
            "protocol relative");
        // unfortunate, but necessary
        assertEquals("test.example.com", ExternalLinks.extractDomain("http://test.example.com/index.html"), 
            "other subdomains not stripped");
        // failures
        assertNull(ExternalLinks.extractDomain("gkskdgds"), "nonsense");
        assertNull(ExternalLinks.extractDomain("http://example.com,"), "ending comma");
        // documenting this common form of broken wikimarkup 
        assertEquals("http", ExternalLinks.extractDomain("http://http://example.com"), "duplicated http");
    }
    
    @Test
    public void isGloballyBlacklisted() throws Exception
    {
        assertFalse(ExternalLinks.isGloballyBlacklisted("example.com"));
        assertTrue(ExternalLinks.isGloballyBlacklisted("youtu.be"));
    }
    
    @Test
    public void isLocallyBlacklisted() throws Exception
    {
        assertFalse(el.isLocallyBlacklisted("example.com")); 
        assertTrue(el.isLocallyBlacklisted("youtu.be")); // local includes global blacklist
        assertTrue(el.isLocallyBlacklisted("roblox.com")); // local only
        assertTrue(el.isLocallyBlacklisted("testinvalid.invalid")); // blocked external domains
    }
    
    @Test
    public void formatLinkseachResults() throws Exception
    {
        List<Wiki.LinksearchResult> input = List.of(
            new Wiki.LinksearchResult(enWiki, "Test", "https://example.com"),
            new Wiki.LinksearchResult(enWiki, "Test2", "https://example.org"));
        assertEquals("""
            # [[Test]] ([[Special:Edit/Test|edit]] &middot; [[Special:PageHistory/Test|history]]) uses link [https://example.com https://example.com]
            # [[Test2]] ([[Special:Edit/Test2|edit]] &middot; [[Special:PageHistory/Test2|history]]) uses link [https://example.org https://example.org]
            """, ExternalLinks.formatLinksearchResults(input, Writable.Format.WIKITEXT));
        
        assertEquals("""
            <p>
            <ol>
            <li><a href="https://en.wikipedia.org/wiki/Test">Test</a> (<a href="https://en.wikipedia.org/wiki/Special%3AEdit%2FTest">edit</a> &middot; \
            <a href="https://en.wikipedia.org/wiki/Special%3APageHistory%2FTest">history</a>) uses link <a href="https://example.com">https://example.com</a>
            <li><a href="https://en.wikipedia.org/wiki/Test2">Test2</a> (<a href="https://en.wikipedia.org/wiki/Special%3AEdit%2FTest2">edit</a> &middot; \
            <a href="https://en.wikipedia.org/wiki/Special%3APageHistory%2FTest2">history</a>) uses link <a href="https://example.org">https://example.org</a>
            </ol>""", ExternalLinks.formatLinksearchResults(input, Writable.Format.HTML));
        
        assertEquals("""
            wiki,page,url
            en.wikipedia.org,Test,https://example.com
            en.wikipedia.org,Test2,https://example.org
            """, ExternalLinks.formatLinksearchResults(input, Writable.Format.CSV));
    }
}
