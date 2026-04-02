/**
 *  @(#)EventsTest.java 0.01 29/07/2018
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

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for the {@link Events} utility class.
 *  @author MER-C
 */
public class EventsTest
{
    private static Wiki enWiki = Wiki.newSession("en.wikipedia.org");
    
    public EventsTest()
    {
    }
    
    @Test
    public void renderEvent() throws Exception
    {
        List<Wiki.Revision> revisions = enWiki.getRevisions(new long[] { 1165014330L, 1171939631L, 1346059881L });
        Wiki.Revision rev = revisions.get(0);
        Events.Comment cmt = new Events.Comment(rev);
        assertEquals(rev.getParsedComment(), cmt.format(Writable.Format.HTML), "normal comment, html");
        assertEquals("<nowiki>" + rev.getComment() + "</nowiki>", cmt.format(Writable.Format.WIKITEXT), "normal comment, wikitext");
        assertEquals(rev.getComment(), cmt.format(Writable.Format.CSV), "normal comment, csv");
        
        rev = revisions.get(1);
        cmt = new Events.Comment(rev);
        assertEquals(Events.DELETED_EVENT_HTML, cmt.format(Writable.Format.HTML), "deleted comment, html");
        assertEquals(Events.DELETED_EVENT_HTML, cmt.format(Writable.Format.WIKITEXT), "deleted comment, wikitext");
        assertEquals("[DELETED]", cmt.format(Writable.Format.CSV), "deleted comment, csv");
        
        rev = revisions.get(2);
        cmt = new Events.Comment(rev);
        assertEquals("", cmt.format(Writable.Format.HTML), "empty comment, html");
        assertEquals("", cmt.format(Writable.Format.WIKITEXT), "empty comment, wikitext");
        assertEquals("", cmt.format(Writable.Format.CSV), "empty comment, csv");
    }

    @Test
    public void timeBetweenEvents() throws Exception
    {
        assertThrows(IllegalArgumentException.class, 
            () -> Events.timeBetweenEvents(Collections.emptyList()),
            "cannot compute time between a list of no events");        
        // https://en.wikipedia.org/wiki/Wikipedia:Articles_for_deletion/HD_Brows
        List<? extends Wiki.Event> events = enWiki.getPageHistory("Wikipedia:Articles for deletion/HD Brows", null);
        assertThrows(IllegalArgumentException.class,
            () -> Events.timeBetweenEvents(events.subList(0, 0)),
            "cannot compute time between a list of one event");
                
        List<Duration> result = Events.timeBetweenEvents(events);
        assertEquals(events.size() - 1, result.size(), "check quantity");
        // check a couple of entries
        assertEquals(Duration.ofSeconds(4880), result.get(result.size() - 2));
        assertEquals(Duration.ofSeconds(976), result.get(result.size() - 1));
    }
}
