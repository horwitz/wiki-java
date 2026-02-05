/**
 *  @(#)UsersTest.java 0.01 23/06/2018
 *  Copyright (C) 2018-20XX MER-C and contributors
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
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Tests for {@link org.wikipedia.Users}.
 *  @author MER-C
 */
public class UsersTest
{
    private final Wiki testWiki, enWiki;
    private final Users testWikiUsers, enWikiUsers;
    
    public UsersTest()
    {
        testWiki = Wiki.newSession("test.wikipedia.org");
        testWiki.setMaxLag(-1);
        enWiki = Wiki.newSession("en.wikipedia.org");
        enWiki.setMaxLag(-1);
        testWikiUsers = Users.of(testWiki);
        enWikiUsers = Users.of(enWiki);
    }
    
    @Test
    public void createdPages() throws Exception
    {
        // no pages created
        assertTrue(enWikiUsers.createdPages(List.of("Constance94S"), null).isEmpty(), "no pages created");
        Wiki.RequestHelper rh = enWiki.new RequestHelper().inNamespaces(Wiki.MEDIAWIKI_NAMESPACE);
        List<Wiki.Revision> pages = enWikiUsers.createdPages(List.of("Watsonboy12"), rh);
        assertTrue(pages.isEmpty(), "no pages created in namespace");

        // verify functionality
        rh = testWiki.new RequestHelper().inNamespaces(Wiki.MAIN_NAMESPACE);
        pages = testWikiUsers.createdPages(List.of("MER-C"), rh);
        Wiki.Revision last = pages.get(pages.size() - 1);
        assertEquals("Wiki.java Test Page", last.getTitle());
        assertEquals(28164L, last.getID());
    }
    
    @Test
    public void createdPagesWithText() throws Exception
    {
        // no articles created
        assertTrue(enWikiUsers.createdPagesWithText(List.of("Constance94S"), null).isEmpty(), "no pages created");
        Wiki.RequestHelper rh = enWiki.new RequestHelper().inNamespaces(Wiki.MEDIAWIKI_NAMESPACE);
        Map<Wiki.Revision, String> creations = enWikiUsers.createdPagesWithText(List.of("Watsonboy12"), rh);
        assertTrue(creations.isEmpty(), "no pages created in namespace");
                
        // verify functionality
        creations = testWikiUsers.createdPagesWithText(List.of("81.245.42.185"), null);
        Wiki.Revision revision = testWiki.getRevision(24764L);
        String text = revision.getText();
        assertEquals(text, creations.get(revision));
    }
    
    @Test
    public void formatShortLinks() throws Exception
    {
        Users.ShortLinks sl = new Users.ShortLinks(testWiki, "MER-C");
        String expected = "[[User:MER-C|MER-C]] ([[User talk:MER-C|talk]] &middot; "
            + "[[Special:Contributions/MER-C|contribs]])";
        assertEquals(expected, sl.format(Writable.Format.WIKITEXT), "Simple, wikitext");
        expected = 
              "<a href=\"" + testWiki.getPageUrl("User:MER-C") + "\">MER-C</a> ("
            + "<a href=\"" + testWiki.getPageUrl("User talk:MER-C") + "\">talk</a> &middot; "
            + "<a href=\"" + testWiki.getPageUrl("Special:Contributions/MER-C") + "\">contribs</a>)";
        assertEquals(expected, sl.format(Writable.Format.HTML), "Simple, HTML");
        assertThrows(UnsupportedOperationException.class, () -> sl.format(Writable.Format.CSV), "Unsupported format");
        
        Users.ShortLinks sl2 = new Users.ShortLinks(testWiki, null);
        assertEquals(Events.DELETED_EVENT_HTML, sl2.format(Writable.Format.WIKITEXT), "Null user, wikitext");
        assertEquals(Events.DELETED_EVENT_HTML, sl2.format(Writable.Format.HTML), "Null user, HTML");
        
        sl2 = new Users.ShortLinks(testWiki, "A B の");
        expected = "[[User:A B の|A B の]] ([[User talk:A B の|talk]] &middot; "
            + "[[Special:Contributions/A B の|contribs]])";
        assertEquals(expected, sl2.format(Writable.Format.WIKITEXT), "Unicode, wikitext");
        
        expected = "<a href=\"" + testWiki.getPageUrl("User:A_B_の") + "\">A B の</a> ("
            + "<a href=\"" + testWiki.getPageUrl("User_talk:A_B_の") + "\">talk</a> &middot; "
            + "<a href=\"" + testWiki.getPageUrl("Special:Contributions/A_B_の") + "\">contribs</a>)";
        assertEquals(expected, sl2.format(Writable.Format.HTML), "Unicode, HTML");
    }
    
    @Test
    public void formatLinks() throws Exception
    {
        Users.Links links = new Users.Links(testWiki, "MER-C");
        String expected = "[[User:MER-C|MER-C]] ([[User talk:MER-C|talk]] &middot; " +
            "[[Special:Contributions/MER-C|contribs]] &middot; [[Special:DeletedContributions/MER-C|deleted contribs]] &middot; " +
            "[https://test.wikipedia.org/w/index.php?title=Special:Log&user=MER-C logs] &middot; " +
            "[[Special:Block/MER-C|block]] &middot; " +
            "[https://test.wikipedia.org/w/index.php?title=Special:Log&type=block&page=User:MER-C block log])";
        assertEquals(expected, links.format(Writable.Format.WIKITEXT), "Simple, wikitext");
        
        String indexPHPURL = testWiki.getIndexPhpUrl();
        expected = "<a href=\"" + testWiki.getPageUrl("User:MER-C") + "\">MER-C</a> ("
            + "<a href=\"" + testWiki.getPageUrl("User talk:MER-C") + "\">talk</a> &middot; "
            + "<a href=\"" + testWiki.getPageUrl("Special:Contributions/MER-C") + "\">contribs</a> &middot; "
            + "<a href=\"" + testWiki.getPageUrl("Special:DeletedContributions/MER-C") + "\">deleted contribs</a> &middot; "
            + "<a href=\"" + indexPHPURL + "?title=Special:Log&user=MER-C\">logs</a> &middot; "
            + "<a href=\"" + testWiki.getPageUrl("Special:Block/MER-C") + "\">block</a> &middot; "
            + "<a href=\"" + indexPHPURL + "?title=Special:Log&type=block&page=User:MER-C\">block log</a>)";
        assertEquals(expected, links.format(Writable.Format.HTML), "Simple, HTML");
        assertThrows(UnsupportedOperationException.class, () -> links.format(Writable.Format.CSV), "Unsupported format");

        Users.Links links2 = new Users.Links(testWiki, "A B の");
        expected = "[[User:A B の|A B の]] ([[User talk:A B の|talk]] &middot; " +
            "[[Special:Contributions/A B の|contribs]] &middot; [[Special:DeletedContributions/A B の|deleted contribs]] &middot; " +
            "[" + indexPHPURL + "?title=Special:Log&user=A+B+%E3%81%AE logs] &middot; [[Special:Block/A B の|block]] &middot; " + 
            "[" + indexPHPURL + "?title=Special:Log&type=block&page=User:A+B+%E3%81%AE block log])";
        assertEquals(expected, links2.format(Writable.Format.WIKITEXT), "Unicode, wikitext");
        
        expected = "<a href=\"" + testWiki.getPageUrl("User:A_B_の") + "\">A B の</a> ("
            + "<a href=\"" + testWiki.getPageUrl("User_talk:A_B_の") + "\">talk</a> &middot; "
            + "<a href=\"" + testWiki.getPageUrl("Special:Contributions/A_B_の") + "\">contribs</a> &middot; "
            + "<a href=\"" + testWiki.getPageUrl("Special:DeletedContributions/A_B_の") + "\">deleted contribs</a> &middot; "
            + "<a href=\"" + indexPHPURL + "?title=Special:Log&user=A+B+%E3%81%AE\">logs</a> &middot; "
            + "<a href=\"" + testWiki.getPageUrl("Special:Block/A_B_の") + "\">block</a> &middot; "
            + "<a href=\"" + indexPHPURL + "?title=Special:Log&type=block&page=User:A+B+%E3%81%AE\">block log</a>)";
        assertEquals(expected, links2.format(Writable.Format.HTML), "Unicode, HTML");
    }
}
