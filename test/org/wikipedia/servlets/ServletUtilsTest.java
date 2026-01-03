/**
 *  @(#)ServletUtilsTest.java 0.02 20/04/2025
 *  Copyright (C) 2018-2025 MER-C and contributors
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
package org.wikipedia.servlets;

import java.util.Map;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit tests for {@link org.wikipedia.servlets.ServletUtils}.
 *  @author MER-C
 */
public class ServletUtilsTest
{
    @Test
    public void sanitizeForAttribute()
    {
        assertEquals("", ServletUtils.sanitizeForAttribute(null));
        assertEquals("default", ServletUtils.sanitizeForAttributeOrDefault(null, "default"));
        assertThrows(NullPointerException.class, 
            () -> ServletUtils.sanitizeForAttributeOrDefault(null, null));
        
        Map<String, String> tests = Map.of(
            "simple", "simple",
            "val\"ue", "val&quot;ue", // Tests quote replacement
            "multiple\"quotes\"", "multiple&quot;quotes&quot;",
            "'single-quotes'", "'single-quotes'"); // Single quotes usually safe in double-quoted attrs
        for (var entry : tests.entrySet())
            assertEquals(entry.getValue(), ServletUtils.sanitizeForAttribute(entry.getKey()));
    }
    
    @Test
    public void sanitizeForAttributeOrDefault()
    {
        assertEquals("fallback", ServletUtils.sanitizeForAttributeOrDefault(null, "fallback"));
        assertThrows(NullPointerException.class, () -> 
            ServletUtils.sanitizeForAttributeOrDefault(null, null));
            
        Map<String, String> tests = Map.of(
            "simple", "simple",
            "val\"ue", "val&quot;ue", // Tests quote replacement
            "multiple\"quotes\"", "multiple&quot;quotes&quot;",
            "'single-quotes'", "'single-quotes'"); // Single quotes usually safe in double-quoted attrs
        for (var entry : tests.entrySet())
            assertEquals(entry.getValue(), ServletUtils.sanitizeForAttributeOrDefault(entry.getKey(), "default"));
    }
    
    @Test
    public void generatePagination()
    {
        // failure states
        String urlbase = "https://example.com/test.jsp?x=1";
        assertThrows(IllegalArgumentException.class, () -> ServletUtils.generatePagination(urlbase, -1, 10, 100));
        assertThrows(IllegalArgumentException.class, () -> ServletUtils.generatePagination(urlbase, 0, 0, 100));
        assertThrows(IllegalArgumentException.class, () -> ServletUtils.generatePagination(urlbase, 0, 0, 0));
        
        // test start from zero
        assertEquals("<p>Previous 50 | <a href=\"" + urlbase + "&offset=50\">Next 50</a>", 
            ServletUtils.generatePagination(urlbase, 0, 50, 149));
        // test intermediate
        assertEquals("<p><a href=\"" + urlbase + "&offset=1\">Previous 50</a> | " 
            + "<a href=\"" + urlbase + "&offset=101\">Next 50</a>", 
            ServletUtils.generatePagination(urlbase, 51, 50, 149));
        // test final
        assertEquals("<p><a href=\"" + urlbase + "&offset=50\">Previous 50</a> | Next 50",
            ServletUtils.generatePagination(urlbase, 100, 50, 149));
    }
    
    @Test
    public void addCheckboxInput()
    {
        String html = ServletUtils.addCheckbox("myParam", true, "My Label");
        assertEquals("<input type=checkbox name=myParam id=\"myParam\" value=1 checked><label for=\"myParam\">My Label</label>", html);

        html = ServletUtils.addCheckbox("myParam", false, "My Label");
        assertEquals("<input type=checkbox name=myParam id=\"myParam\" value=1><label for=\"myParam\">My Label</label>", html);

    }
}
