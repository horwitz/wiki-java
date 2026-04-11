/**
 *  @(#)WikitextUtils.java 0.03 31/01/2026
 *  Copyright (C) 2012-2026 MER-C
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

/**
 *  Utility methods for generating and parsing wikitext that don't belong in
 *  any of the specialist utility classes.
 *  @author MER-C
 *  @version 0.03
 */
public class WikitextUtils
{
    /**
     *  Represents a wikilink. In wikitext, this is [[<code>title</code>|<code>text</code>]].
     *  @param wiki the wiki on which the link exists/is intended for
     *  @param title the title linked to
     *  @param text the text to display for the link (can be {@code null}, 
     *  renders as title)
     *  @since 0.03
     */
    public record WikiLink(Wiki wiki, String title, String text) implements Writable
    {
        /**
         *  Formats this wikilink in wikitext or HTML. CSV or other formats are
         *  not supported. <strong>Inputs are not sanitized</strong>.
         *  @param format {@link Writable.Format#WIKITEXT} or {@link
         *  Writable.Format#HTML}
         *  @return this link formatted as wikitext or HTML
         *  @throws UnsupportedOperationException if other formats are supplied
         *  @throws UncheckedIOException if format is {@link Writable.Format#WIKITEXT},
         *  the wiki's namespace cache isn't populated, and there was a network
         *  error populating it. File and Category links must be escaped.
         */
        @Override
        public String format(Writable.Format format)
        {
            return switch (format)
            {
                case HTML -> "<a href=\"" + wiki.getPageUrl(title) + "\">" + Objects.requireNonNullElse(text, title) + "</a>";
                case WIKITEXT -> 
                {
                    int ns = wiki.namespace(title);
                    String s = "[[";
                    if (ns == Wiki.CATEGORY_NAMESPACE || ns == Wiki.FILE_NAMESPACE)
                        s = "[[:";
                    yield s + title + (text == null ? "" : "|" + text) + "]]";
                }
                default -> throw new UnsupportedOperationException("Cannot format a link as this format");
            };
        }
    }
    
    /**
     *  Parses a wikilink. Can also be used to get sortkeys from 
     *  categorizations. Use with caution on file uses because they can
     *  contain their own wikilinks.
     *  @param wiki the wiki on which the link appears
     *  @param wikitext the wikitext to parse
     *  @return the parsed wikilink
     *  @throws IllegalArgumentException if wikitext is not a valid wikilink
     */
    public static WikiLink parseWikiLink(Wiki wiki, String wikitext)
    {
        int wikilinkstart = wikitext.indexOf("[[");
        int wikilinkend = wikitext.indexOf("]]", wikilinkstart);
        if (wikilinkstart < 0 || wikilinkend < 0)
            throw new IllegalArgumentException("\"" + wikitext + "\" is not a valid wikilink.");
        // strip escaping of categories and files
        String linktext = wikitext.substring(wikilinkstart + 2, wikilinkend).trim();
        if (linktext.startsWith(":"))
            linktext = linktext.substring(1);
        // check for description, if not there then set it to the target
        int pipe = linktext.indexOf('|');
        if (pipe >= 0)
            return new WikiLink(wiki, linktext.substring(0, pipe).trim(), linktext.substring(pipe + 1).trim());
        else
            return new WikiLink(wiki, linktext.trim(), null);
    }
        
    /**
     *  Reverse of Wiki.decode()
     *  @param in input string
     *  @return recoded input string
     */
    public static String recode(String in)
    {
        in = in.replace("&", "&amp;");
        in = in.replace("<", "&lt;").replace(">", "&gt;"); // html tags
        in = in.replace("\"", "&quot;");
        in = in.replace("'", "&#039;");
        return in;
    }
    
    /**
     *  Removes HTML comments from the supplied string. 
     *  @param delta the string to strip HTML comments from
     *  @return the string minus HTML comments
     *  @since 0.02
     */
    public static String removeComments(String delta)
    {
        while (delta.contains("<!--"))
        {
            int a = delta.indexOf("<!--");
            int b = delta.indexOf("-->", a);
            if (b < 0)
                delta = delta.substring(0, a);
            else
                delta = delta.substring(0, a) + delta.substring(b + 3);
        }
        return delta;
    }
    
    /**
     *  Represents an external link.
     *  @param url the URL to link to
     *  @param text the text to display for the link (can be null)
     *  @since 0.03
     */
    public record ExternalLink(String url, String text) implements Writable
    {
        /**
         *  Formats this external link in wikitext or HTML. CSV or other formats
         *  are not supported. <strong>Inputs are not sanitized</strong>.
         *  @param format {@link Writable.Format#WIKITEXT} or {@link
         *  Writable.Format#HTML}
         *  @return this link formatted as wikitext or HTML
         *  @throws UnsupportedOperationException if other formats are supplied
         */
        @Override
        public String format(Writable.Format format)
        {
            return switch (format)
            {
                case HTML -> "<a href=\"" + url + "\">" + (text == null ? "" : text) + "</a>";
                case WIKITEXT -> "[" + url + (text == null ? "" : " " + text) + "]";
                default -> throw new UnsupportedOperationException("Cannot format a link as this format");
            };
        }
    }
    
    /**
     *  Represents a heading.
     *  @param text the heading text
     *  @param level the heading level, between 1 and 6 inclusive
     *  @since 0.03
     */
    public record Heading(String text, int level) implements Writable
    {
        /**
         *  Constructs a new format-independent heading.
         *  @param text the heading text
         *  @param level the heading level, between 1 and 6 inclusive
         *  @throws IllegalArgumentException if {@code level} is not between 1 and 6
         */
        public Heading
        {
            if (level < 1 || level > 6)
                throw new IllegalArgumentException("Heading level must be between 1 and 6");
        }
        
        /**
         *  Formats this heading in wikitext or HTML. CSV or other formats are 
         *  not supported. <strong>Inputs are not sanitized</strong>.
         *  @param format {@link Writable.Format#WIKITEXT} or {@link
         *  Writable.Format#HTML}
         *  @return this heading formatted as wikitext or HTML
         *  @throws UnsupportedOperationException if other formats are supplied
         */
        @Override
        public String format(Writable.Format format)
        {
            return switch (format)
            {
                case HTML -> "<h" + level + ">" + text + "</h" + level + ">";
                case WIKITEXT -> "=".repeat(level) + text + "=".repeat(level);
                default -> throw new UnsupportedOperationException("Cannot format a heading as this format");    
            };
        }
    }
    
    /**
     *  Represents an optionally paginated, format-independent textual list.
     *  @param list the constituents of the list
     *  @param numbered whether this is a numbered list
     *  @param paginator the delimiter for each paginated segment, {@code null} 
     *  disables pagination
     *  @param itemspersegment the number of items per segment
     *  @since 0.03
     */
    public record PaginatedList(List<Writable> list, boolean numbered, BiFunction<Integer, Integer, Writable> paginator,
        int itemspersegment) implements Writable
    {
        public PaginatedList
        {
            if (itemspersegment < 1)
                throw new IllegalArgumentException("There must be at least one page per section.");
            itemspersegment = paginator == null ? Integer.MAX_VALUE : itemspersegment;
        }
        
        /**
         *  Formats this list in wikitext or HTML. CSV or other formats are 
         *  not supported. <strong>Inputs are not sanitized</strong>.
         *  @param format {@link Writable.Format#WIKITEXT} or {@link
         *  Writable.Format#HTML}
         *  @return this list formatted as wikitext or HTML
         *  @throws UnsupportedOperationException if other formats are supplied
         */
        @Override
        public String format(Writable.Format format)
        {
            if (list.isEmpty())
                return "";
            StringBuilder builder = new StringBuilder();
            String delimiter = switch (format)
            {
                case Writable.Format.WIKITEXT:
                    yield numbered ? "#" : "*";
                case Writable.Format.HTML:
                    yield "<li>";
                default:
                    yield "";
            };
            String start = "", end = "\n";
            if (format.equals(Writable.Format.HTML))
            {
                start = numbered ? "<ol" : "<ul";
                end   = numbered ? "</ol>\n\n" : "</ul>\n\n";
            }
            int max = list.size();
            for (int i = 0; i < max; i++)
            {
                int sectionmax = Math.min(max, i + itemspersegment);
                if (i % itemspersegment == 0)
                {
                    if (paginator != null)
                    {
                        builder.append(paginator.apply(i + 1, sectionmax).format(format));
                        builder.append("\n");
                    }
                    builder.append(start);
                    if (format.equals(Writable.Format.HTML))
                    {
                        if (numbered && i > 0)
                            builder.append(" start=").append(i + 1);
                        builder.append(">\n");
                    }
                }
                builder.append(delimiter);
                builder.append(list.get(i).format(format));
                builder.append("\n");
                if (i % itemspersegment == itemspersegment - 1 || i == max - 1)
                    builder.append(end);
            }
            return builder.toString();
        }
    }
}
