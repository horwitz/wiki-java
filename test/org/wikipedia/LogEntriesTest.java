/**
 *  @(#)LogEntriesTest.java 0.01 29/03/2026
 *  Copyright (C) 2026-2026 MER-C and contributors
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

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Tests for {@link org.wikipedia.LogEntries}.
 *  @author MER-C
 */
public class LogEntriesTest {
    
    private final Wiki enWiki = WMFWikiFarm.instance().sharedSession("en.wikipedia.org");

    @Test
    public void toDataTable() throws IOException
    {
        Wiki.RequestHelper rh = enWiki.new RequestHelper().byUser("Rangwezen");
        List<Wiki.LogEntry> logs = enWiki.getLogEntries(Wiki.ALL_LOGS, null, rh);
        
        String userstr = "[[User:Rangwezen|Rangwezen]] ([[User talk:Rangwezen|talk]] &middot; [[Special:Contributions/Rangwezen|contribs]])";
        String expected = """
            {| class="wikitable sortable"          
            ! Domain !! Timestamp !! User !! Log !! Action !! Target !! Comment !! Details
            |-
            | en.wikipedia.org || 2026-03-28T08:48:43Z || %s || move || move || [[Draft:Baloo Living]] || || {target_title=Baloo Living}
            |-
            | en.wikipedia.org || 2026-02-13T15:45:56Z || %s || create || create || [[Draft:Baloo Living]] || <nowiki>-- Draft creation using the [[WP:Article wizard]] --</nowiki> || 
            |-
            | en.wikipedia.org || 2025-08-29T16:11:49Z || %s || newusers || create || [[User:Rangwezen]] || || 
            |}
            """.formatted(userstr, userstr, userstr);
        assertEquals(expected, LogEntries.toDataTable(logs).format(Writable.Format.WIKITEXT));
    }
}
