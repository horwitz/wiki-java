/**
 *  @(#)SpamArchiveSearch.java 0.01 06/07/2011
 *  Copyright (C) 2011 - 2026 MER-C
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.

 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.wikipedia.tools;

import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;
import org.wikipedia.*;

/**
 *  A crude replacement for Eagle's spam archive search tool.
 *  @author MER-C
 *  @version 0.01
 */
public class SpamArchiveSearch
{
    /**
     *  Main for testing/offline stuff. 
     *  @param args command line arguments (ignored)
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        String query = JOptionPane.showInputDialog(null, "Enter query string");
        if (query == null)
            System.exit(0);
        WMFWikiFarm sessions = WMFWikiFarm.instance();
        sessions.setInitializer(wiki -> 
        {
            wiki.setMaxLag(-1);
            wiki.setUserAgent(WMFWikiFarm.TOOL_USER_AGENT);
        });
        StringBuilder buffer = new StringBuilder(10000);
        List<Wiki.SearchResult> results = archiveSearch(query);
        buffer.append("""
            <h2>Results for "%s"</h2>
            <ul>
            """.formatted(query));
        results.forEach(result ->
        {
            String page = result.title();
            buffer.append("""
                <li><a href="//%s.org/wiki/%s">%s</a>
                """.formatted(page.contains("Talk:Spam blacklist") ? "meta.wikimedia" : "en.wikipedia", page, page));
        });
        buffer.append("""
            </ul>
            <p>%d results.
            """.formatted(results.size()));
        System.out.println(buffer.toString());
    }

    /**
     *  Searches the following spam-related discussion archives for the given 
     *  query string.
     * 
     *  <ul>
     *  <li><a href="//meta.wikimedia.org/wiki/WM:SBL">Global spam blacklist</a>
     *  <li><a href="//meta.wikimedia.org/wiki/Wikiproject:Antispam">Wikiproject Antispam</a>
     *  <li><a href="//en.wikipedia.org/wiki/WP:SBL">en.wikipedia spam blacklist</a>
     *  <li><a href="//en.wikipedia.org/wiki/WP:BED">en.wikipedia blocked external domains</a>
     *  <li><a href="//en.wikipedia.org/wiki/MediaWiki_talk:Spam-whitelist">en.wikipedia spam whitelist</a>
     *  <li><a href="//en.wikipedia.org/wiki/WT:WPSPAM">en.wikipedia WikiProject Spam</a>
     *  <li><a href="//en.wikipedia.org/wiki/WP:RSN">en.wikipedia reliable sources noticeboard</a>
     *  <li><a href="//en.wikipedia.org/wiki/WP:ELN">en.wikipedia external links noticeboard</a>
     *  </ul>
     *  
     *  Domain names need to be enclosed in quotes.
     * 
     *  @param query a query string
     *  @return the spam archive search results for that query
     *  @throws IOException if a network error occurs
     */
    public static List<Wiki.SearchResult> archiveSearch(String query) throws IOException
    {
        WMFWikiFarm sessions = WMFWikiFarm.instance();
        Wiki enWiki = sessions.sharedSession("en.wikipedia.org");
        Wiki meta = sessions.sharedSession("meta.wikimedia.org");
        ArrayList<Wiki.SearchResult> results = new ArrayList<>(20);
        
        results.addAll(meta.search(query + " prefix:Spam_blacklist", Wiki.MAIN_NAMESPACE));
        results.addAll(meta.search(query + " prefix:Talk:Spam_blacklist", Wiki.TALK_NAMESPACE));
        results.addAll(meta.search(query + " prefix:Wikiproject:Antispam", Wiki.MAIN_NAMESPACE));
        results.addAll(meta.search(query + " prefix:Talk:Wikiproject:Antispam", Wiki.TALK_NAMESPACE));
        
        // TODO: allow any local project (if not en.wp, then skip third group which is en.wp specific)
        results.addAll(enWiki.search(query + " prefix:MediaWiki:Spam-blacklist", Wiki.MEDIAWIKI_NAMESPACE));
        results.addAll(enWiki.search(query + " prefix:MediaWiki_talk:Spam-blacklist", Wiki.MEDIAWIKI_TALK_NAMESPACE));
        results.addAll(enWiki.search(query + " prefix:MediaWiki:Spam-whitelist", Wiki.MEDIAWIKI_NAMESPACE));
        results.addAll(enWiki.search(query + " prefix:MediaWiki_talk:Spam-whitelist", Wiki.MEDIAWIKI_TALK_NAMESPACE));
        results.addAll(enWiki.search(query + " prefix:MediaWiki:BlockedExternalDomains.json", Wiki.MEDIAWIKI_NAMESPACE));
        
        results.addAll(enWiki.search(query + " prefix:Wikipedia:WikiProject_Spam", Wiki.PROJECT_NAMESPACE));
        results.addAll(enWiki.search(query + " prefix:Wikipedia_talk:WikiProject_Spam", Wiki.PROJECT_TALK_NAMESPACE));
        results.addAll(enWiki.search(query + " prefix:Wikipedia:Reliable_sources/Noticeboard", Wiki.PROJECT_NAMESPACE));
        results.addAll(enWiki.search(query + " prefix:Wikipedia:External_links/Noticeboard", Wiki.PROJECT_NAMESPACE));

        return results;
    }
}