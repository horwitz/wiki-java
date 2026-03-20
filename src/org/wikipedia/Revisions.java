/**
 *  @(#)Revisions.java 0.02 25/01/2026
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

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

/**
 *  Utility classes for dealing with (lists of) wiki revisions.
 *  @author MER-C
 *  @version 0.02
 *  @see org.wikipedia.Wiki.Revision
 */
public class Revisions
{
    private final Wiki wiki;
    
    /**
     *  A representation of a {@link Wiki.Revision} for output in tabular format.
     *  @param diff a link to the diff to the previous revision
     *  @param revlink a link to the revision whose text is the timestamp
     *  @param flag_new a formatted indication this is a new revision
     *  @param flag_minor a formatted indication this is a minor edit
     *  @param flag_bot a formatted indication this is a bot edit
     *  @param title the title of the relevant page
     *  @param user a formatted rendering of the user making the edit
     *  @param size the size of the revision
     *  @param sizediff a formatted indication of the bytes added
     *  @param comment the edit summary
     *  @see DataTable
     *  @see Wiki.Revision
     *  @since 0.02
     */
    public record RevisionRecord(WikitextUtils.WikiLink diff, WikitextUtils.WikiLink revlink, String flag_new, String flag_minor, String flag_bot,
        WikitextUtils.WikiLink title, Users.ShortLinks user, int size, String sizediff, Events.Comment comment) { }
    
    private Revisions(Wiki wiki)
    {
        this.wiki = wiki;
    }
    
    /**
     *  Creates an instance of this class bound to a particular wiki (required
     *  for methods that make network requests to a wiki or access wiki state).
     * 
     *  @param wiki the wiki to bind to
     *  @return an instance of this utility class that is bound to that wiki
     */
    public static Revisions of(Wiki wiki)
    {
        return new Revisions(wiki);
    }
    
    /**
     *  Removes reverts from a list of revisions. A revert is defined as any 
     *  revision on a page that has the same SHA-1 as any previous (as in time) 
     *  revision on that page. As a side effect, the returned list is sorted
     *  by timestamp with the earliest revision first and with duplicates 
     *  removed.
     * 
     *  @param revisions the revisions to remove reverts from
     *  @return a copy of the list of revisions with reverts removed
     */
    public static List<Wiki.Revision> removeReverts(List<Wiki.Revision> revisions)
    {
        // Group revisions by page, then sort so that the oldest edits are first.
        Map<String, Set<Wiki.Revision>> stuff = revisions.stream()
            .collect(Collectors.groupingBy(Wiki.Revision::getTitle, Collectors.toCollection(TreeSet::new)));
        Set<Wiki.Revision> ret = new LinkedHashSet<>();
        stuff.forEach((page, listofrevisions) ->
        {
            // Therefore, if a sha1 matches any previous revisions it is a revert.
            Set<String> hashes = new HashSet<>();
            Iterator<Wiki.Revision> iter = listofrevisions.iterator();
            while (iter.hasNext())
            {
                String sha1 = iter.next().getSha1();
                if (sha1 == null || sha1.equals(Wiki.Event.CONTENT_DELETED))
                    continue;
                if (hashes.contains(sha1))
                    iter.remove();
                hashes.add(sha1);
            }
            ret.addAll(listofrevisions);
        });
        return new ArrayList<>(ret);
    }
    
    /**
     *  Converts an {@code Iterable} of wiki revisions into a {@link DataTable}
     *  for output in tabular format.
     *  @param revisions the revisions to convert
     *  @param format the output format, "html" or "wikitext" (temporary parameter)
     *  @return (see above)
     *  @see Revisions.RevisionRecord
     *  @since 0.02
     */
    public DataTable<RevisionRecord> toDataTable(Iterable<Wiki.Revision> revisions, String format)
    {
        // TODO: the output of toWikitext and toDataTable isn't identical: there are
        // edit summaries that can be rendered in wikitext and HTML
        // also wiki format has class="wikitable sortable" already given
        
        List<RevisionRecord> rows = new ArrayList<>();
        for (Wiki.Revision rev : revisions)
        {
            String user = rev.getUser();
            int sizediff = rev.getSizeDiff();
            rows.add(new RevisionRecord(
                new WikitextUtils.WikiLink(wiki, "Special:Diff/" + rev.getID(), "prev"), 
                new WikitextUtils.WikiLink(wiki, "Special:Permanentlink/" + rev.getID(), DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(rev.getTimestamp())),
                rev.isNew() ? (format.equals("html") ? "<b>N</b>" : "'''N'''") : "",
                rev.isMinor() ? (format.equals("html") ? "<b>m</b>" : "'''m'''") : "",
                rev.isBot() ? (format.equals("html") ? "<b>b</b>" : "'''b'''") : "",
                new WikitextUtils.WikiLink(wiki, rev.getTitle(), null),
                new Users.ShortLinks(wiki, user == null || user.equals(Wiki.Event.USER_DELETED) ? null : user), 
                rev.getSize(),
                "<span class=\"" + (sizediff > 0 ? "sizeincreased" : "sizedecreased") + "\">" + sizediff + "</span>",
                new Events.Comment(rev)));
        }
        DataTable dt = DataTable.create(rows, null);
        dt.setTableClass(format.equals("html") ? "wikitable revisions" : "revisions");
        dt.setRowClasses((rr, i) -> "revision");
        dt.setHeaders(List.of("Previous", "Timestamp", "New", "Minor", "Bot", "Title", "User", "Size (bytes)", "Size change", "Comment"));
        dt.setColumnClasses(List.of("difflink", "date", "flag", "flag", "flag", "title", "user", "revsize", "revsizediff", "comment"));
        return dt;
    }
}
