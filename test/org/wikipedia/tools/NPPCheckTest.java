/**
 *  @(#)NPPCheckTest.java 0.01 20/06/2019
 *  Copyright (C) 2019 - 20xx MER-C
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
package org.wikipedia.tools;

import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import org.wikipedia.*;

/**
 *  Unit tests for {@link NPPCheck}.
 *  @author MER-C
 */
public class NPPCheckTest
{
    private final NPPCheck check;
    private final WMFWiki enWiki;
    
    public NPPCheckTest()
    {
        enWiki = WMFWiki.newSession("en.wikipedia.org");
        check = new NPPCheck(enWiki);
    }
    
    @Test
    public void setUser()
    {
        check.setReviewer("Blah");        
        check.setReviewer(null);
        assertNull(check.getUser());
        check.setReviewer("Blah");
        assertEquals("Blah", check.getUser());
        // empty string = no user (for servlets)
        check.setReviewer("");
        assertNull(check.getUser());
    }

    @Test
    public void outputTable() throws Exception
    {
        // Zero row output, tests correct headers only
        List<String> expected = List.of("Draft", "Title", "Create timestamp", "Review timestamp", "Age at review", "Time between reviews",
            "Size", "Author", "Author registration timestamp", "Author edit count", "Author blocked", "Author age at creation", "Reviewer", 
            "Reviewer edit count", "Snippet");
        check.setMode(NPPCheck.Mode.DRAFTS);
        check.setReviewer(null);
        assertEquals(expected, check.outputTable(Collections.EMPTY_LIST).getHeaders(), "Generic header");
        expected = List.of("betweenreviews");
        assertEquals(expected, check.outputTable(Collections.EMPTY_LIST).getSkippedCols(), "Patrolled content in draft namespace for all reviewers");
        check.setMode(NPPCheck.Mode.USERSPACE);
        assertEquals(expected, check.outputTable(Collections.EMPTY_LIST).getSkippedCols(), "Patrolled content in user namespace for all reviewers");
        
        expected = List.of("reviewer", "reviewerec");
        check.setMode(NPPCheck.Mode.DRAFTS);
        check.setReviewer("MER-C");
        assertEquals(expected, check.outputTable(Collections.EMPTY_LIST).getSkippedCols(), "Patrolled content in draft namespace for a given reviewer");
        check.setMode(NPPCheck.Mode.USERSPACE);
        assertEquals(expected, check.outputTable(Collections.EMPTY_LIST).getSkippedCols(), "Patrolled content in user namespace for a given reviewer");
        
        expected = List.of("draft", "betweenreviews");
        check.setMode(NPPCheck.Mode.PATROLS);
        check.setReviewer(null);
        assertEquals(expected, check.outputTable(Collections.EMPTY_LIST).getSkippedCols(), "Patrolled content in main namespace for all reviewers");
        
        expected = List.of("draft", "reviewer", "reviewerec");
        check.setReviewer("MER-C");
        assertEquals(expected, check.outputTable(Collections.EMPTY_LIST).getSkippedCols(), "Patrolled content in main namespace for a given reviewer");
        
        expected = List.of("draft", "reviewts", "ageatreview", "betweenreviews", "reviewer", "reviewerec");
        check.setMode(NPPCheck.Mode.UNPATROLLED);
        check.setReviewer(null);
        assertEquals(expected, check.outputTable(Collections.EMPTY_LIST).getSkippedCols(), "Unpatrolled content (no users)");
        check.setMode(NPPCheck.Mode.REDIRECTS);
        assertEquals(expected, check.outputTable(Collections.EMPTY_LIST).getSkippedCols(), "Unpatrolled redirects (no users)");
        
        check.setMode(NPPCheck.Mode.UNPATROLLED);
        check.setReviewer("MER-C");
        assertEquals(expected, check.outputTable(Collections.EMPTY_LIST).getSkippedCols(), "Reviewer parameter doesn't matter for unpatrolled content");
        check.setMode(NPPCheck.Mode.REDIRECTS);
        assertEquals(expected, check.outputTable(Collections.EMPTY_LIST).getSkippedCols(), "Reviewer parameter doesn't matter for unpatrolled content");
        
        // TODO: expand tests to include one row output, timestamp restrictions
        // and multiple row output
    }
}
