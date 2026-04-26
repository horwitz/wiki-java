/**
 *  @(#)Users.java 0.02 01/02/2026
 *  Copyright (C) 2018-2026 MER-C and contributors
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

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.security.auth.login.FailedLoginException;

/**
 *  Utility methods for wiki users.
 *  @author MER-C
 *  @version 0.02
 *  @see org.wikipedia.Wiki.User
 */
public class Users
{
    private final Wiki wiki;
    
    private Users(Wiki wiki)
    {
        this.wiki = wiki;
    }
    
    /**
     *  Represents user links of the form <samp>User (talk &middot; contribs)</samp>.
     *  @param wiki the wiki 
     *  @param username the username (may be null, assumed to be RevisionDeleted)
     *  @see Users.Links
     *  @since 0.02
     */
    public record ShortLinks(Wiki wiki, String username) implements Writable
    {
        /**
         *  Generates short user links in wikitext or HTML. CSV or other formats
         *  are not supported. <strong>Inputs are not sanitized</strong>.
         *  @param format {@link Writable.Format#WIKITEXT} or {@link
         *  Writable.Format#HTML}
         *  @return the formatted user links
         *  @throws UnsupportedOperationException if other formats are supplied
         */
        @Override
        public String format(Writable.Format format)
        {
            if (username == null)
                return Events.DELETED_EVENT_HTML;
            return new WikitextUtils.WikiLink(wiki, "User:" + username, username).format(format) +
                " (" + new WikitextUtils.WikiLink(wiki, "User talk:" + username, "talk").format(format) +
                " &middot; " + new WikitextUtils.WikiLink(wiki, "Special:Contributions/" + username, "contribs").format(format) + ")";
        }
    }
    
    /**
     *  Represents user links of the form <samp>User (talk &middot; contribs &middot;
     *  deleted contribs &middot; logs &middot; block &middot; block log)</samp>.
     *  @param wiki the wiki 
     *  @param username the username (may be null, assumed to be RevisionDeleted)
     *  @see Users.ShortLinks
     *  @since 0.02
     */
    public record Links(Wiki wiki, String username) implements Writable
    {
        /**
         *  Generates user links in wikitext or HTML. CSV or other formats
         *  are not supported. <strong>Inputs are not sanitized</strong>.
         *  @param format {@link Writable.Format#WIKITEXT} or {@link
         *  Writable.Format#HTML}
         *  @return the formatted user links
         *  @throws UnsupportedOperationException if other formats are supplied
         */
        @Override
        public String format(Writable.Format format)
        {
            if (username == null)
                return Events.DELETED_EVENT_HTML;
            String indexPHPURL = wiki.getIndexPhpUrl();
            String userenc = URLEncoder.encode(username, StandardCharsets.UTF_8);
            return new WikitextUtils.WikiLink(wiki, "User:" + username, username).format(format) + " (" +
                new WikitextUtils.WikiLink(wiki, "User talk:" + username, "talk").format(format) + " &middot; " +
                new WikitextUtils.WikiLink(wiki, "Special:Contributions/" + username, "contribs").format(format) + " &middot; " +
                new WikitextUtils.WikiLink(wiki, "Special:DeletedContributions/" + username, "deleted contribs").format(format) + " &middot; " +
                new WikitextUtils.ExternalLink(indexPHPURL + "?title=Special:Log&user=" + userenc, "logs").format(format) + " &middot; " + 
                new WikitextUtils.WikiLink(wiki, "Special:Block/" + username, "block").format(format) + " &middot; " +
                new WikitextUtils.ExternalLink(indexPHPURL + "?title=Special:Log&type=block&page=User:" + userenc, "block log").format(format) + ")";
        }
    }
    
    /**
     *  Creates an instance of this class bound to a particular wiki (required
     *  for methods that make network requests to a wiki).
     * 
     *  @param wiki the wiki to bind to
     *  @return an instance of this utility class that is bound to that wiki
     */
    public static Users of(Wiki wiki)
    {
        return new Users(wiki);
    }
    
    /**
     *  Returns a list of pages created by this user in the given namespaces
     *  with full revision metadata.
     *  @param users the users to fetch page creations for
     *  @param rh a {@link Wiki.RequestHelper} object that is passed to {@link
     *  Wiki#contribs(SequencedCollection, String, Wiki.RequestHelper)}
     *  @return the list of pages created by this user with revision metadata
     *  for the corresponding revisions
     *  @throws IOException if a network error occurs
     *  @see #createdPagesWithText(SequencedCollection, Wiki.RequestHelper) 
     */
    public List<Wiki.Revision> createdPages(SequencedCollection<String> users, Wiki.RequestHelper rh) throws IOException
    {
        rh = Objects.requireNonNullElse(rh, wiki.new RequestHelper())
            .filterBy(Map.of("new", Boolean.TRUE));
        List<List<Wiki.Revision>> contribs = wiki.contribs(users, null, rh);
        List<Wiki.Revision> ret = new ArrayList<>();
        for (List<Wiki.Revision> rev : contribs)
            ret.addAll(rev);
        return ret;
    }
    
    /**
     *  Fetches the pages created by the given users and the text of the current
     *  revision of those pages.
     *  @param users the users to fetch page creations for
     *  @param rh a {@link Wiki.RequestHelper} object that is passed to {@link
     *  Wiki#contribs(SequencedCollection, String, Wiki.RequestHelper)}
     *  @return a map containing revision the page was created &#8594; current 
     *  text of that page
     *  @throws IOException if a network error occurs
     *  @see #createdPages(SequencedCollection, Wiki.RequestHelper) 
     */
    public Map<Wiki.Revision, String> createdPagesWithText(SequencedCollection<String> users, Wiki.RequestHelper rh) throws IOException
    {
        rh = Objects.requireNonNullElse(rh, wiki.new RequestHelper())
            .filterBy(Map.of("new", Boolean.TRUE));
        List<List<Wiki.Revision>> contribs = wiki.contribs(users, null, rh);
        
        // get text of all pages
        List<Wiki.Revision> temp = new ArrayList<>();
        for (List<Wiki.Revision> rev : contribs)
            temp.addAll(rev);
        List<String> pages = ArrayUtils.transform(temp, ArrayList::new, r -> r.getTitle());
        List<String> pagetexts = wiki.getPageText(pages);
        Map<Wiki.Revision, String> ret = new HashMap<>();
        for (int i = 0; i < temp.size(); i++)
        {
            Wiki.Revision revision = temp.get(i);
            ret.putIfAbsent(revision, pagetexts.get(i));
        }
        return ret;
    }
    
    /**
     *  Generates a CLI login prompt and logs in if successful. Exits with exit
     *  code 1 if login is unsuccessful or code 2 if a network error occurs.
     */
    public void cliLogin()
    {
        try
        {
            Console console = System.console();
            wiki.login(console.readLine("Username: "), console.readPassword("Password: "));
        }
        catch (FailedLoginException ex)
        {
            System.err.println("Invalid username or password.");
            System.exit(1);
        }
        catch (IOException ex)
        {
            System.err.println("A network error occurred.");
            ex.printStackTrace();
            System.exit(2);
        }
    }
}
