/**
 *  @(#)DataTableTest.java 0.02 13/01/2026
 *  Copyright (C) 2021-2026 MER-C
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

import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Tests for DataTable.
 *  @author MER-C
 *  @version 0.02
 */
public class DataTableTest
{
    private record TwoColRecord(String key, Object value) {}
    private record MultiColRecord(String col1, OffsetDateTime col2, int col3) {}
    
    private final List<String> headers2, headers3;
    private List<TwoColRecord> list1, list2;
    private List<MultiColRecord> list3;
    
    /**
     *  Construct wiki objects for each test so that tests are independent.
     */
    public DataTableTest()
    {
        headers2 = List.of("Column1", "Column2");
        headers3 = List.of("Test1", "Test2", "Test3");
        
        list1 = List.of(
            new TwoColRecord("Value1", 10),
            new TwoColRecord("Value2", 20));
        list2 = List.of(
            new TwoColRecord("Simple key", "Simple value"),
            new TwoColRecord(null, "Has \"Quotes\" inside"),
            new TwoColRecord("<script>alert('PWNED');</script>", "Simple"),
            new TwoColRecord("Carriage\r\nReturn", "With, Comma"),
            new TwoColRecord("Multi\nLine", "Easy"));
        list3 = List.of(
            new MultiColRecord("A", OffsetDateTime.parse("2020-01-01T00:00:00Z"), 5),
            new MultiColRecord("B", OffsetDateTime.parse("2025-06-21T00:05:05Z"), 2));
    }
    
    @Test
    public void create()
    {
        assertThrows(IllegalArgumentException.class, () -> DataTable.create(list1, headers3));
    }
    
    @Test
    public void headers() 
    {
        DataTable dt = DataTable.create(list1, headers2);
        assertEquals(headers2, dt.getHeaders(), "From constructor");
        assertThrows(IllegalArgumentException.class, () -> DataTable.create(list1, headers3));
        
        List<String> newheaders2 = List.of("Test1", "Test2");
        dt.setHeaders(newheaders2);
        assertEquals(newheaders2, dt.getHeaders(), "Get/set");
        
        dt.setHeaders(null);
        assertNull(dt.getHeaders(), "Null accepted");
    }

    @Test
    public void formatAsCSV() 
    {
        DataTable dt = DataTable.create(list1, headers2);
        String expected = """
            Column1,Column2
            Value1,10
            Value2,20
            """;
        assertEquals(expected, dt.formatAsCSV(), "Simple");
        
        dt = DataTable.create(list1, null);
        expected = """
            Value1,10
            Value2,20
            """;
        assertEquals(expected, dt.formatAsCSV(), "Null header");
        
        dt = DataTable.create(list2, headers2);
        expected = """
            Column1,Column2
            Simple key,Simple value
            ,"Has ""Quotes"" inside"
            <script>alert('PWNED');</script>,Simple
            "Carriage\r
            Return","With, Comma"
            "Multi
            Line",Easy
            """;
        assertEquals(expected, dt.formatAsCSV(), "Challenging");
        
        dt = DataTable.create(list3, headers3);
        expected = """
            Test1,Test2,Test3
            A,2020-01-01T00:00:00Z,5
            B,2025-06-21T00:05:05Z,2
            """;
        assertEquals(expected, dt.formatAsCSV(), "Three column");
    }

    @Test
    public void formatAsWikitext() 
    {
        DataTable dt = DataTable.create(list1, headers2);
        String expected = """
            {| class="wikitable sortable"
            ! Column1 !! Column2
            |-
            | Value1 || 10
            |-
            | Value2 || 20
            |}
            """;
        assertEquals(expected, dt.formatAsWikitext(), "Simple");
        
        dt = DataTable.create(list1, null);
        expected = """
            {| class="wikitable sortable"
            |-
            | Value1 || 10
            |-
            | Value2 || 20
            |}
            """;
        assertEquals(expected, dt.formatAsWikitext(), "Null header");
        
        dt = DataTable.create(list2.subList(0, 2), headers2);
        expected = """
            {| class="wikitable sortable"
            ! Column1 !! Column2
            |-
            | Simple key || Simple value
            |-
            |  || Has "Quotes" inside
            |}
            """;
        assertEquals(expected, dt.formatAsWikitext(), "Challenging");
        
        dt = DataTable.create(list3, headers3);
        expected = """
            {| class="wikitable sortable"
            ! Test1 !! Test2 !! Test3
            |-
            | A || 2020-01-01T00:00:00Z || 5
            |-
            | B || 2025-06-21T00:05:05Z || 2
            |}
            """;
        assertEquals(expected, dt.formatAsWikitext(), "Three column");
    }
    
    @Test
    public void formatAsHTML()
    {
        DataTable dt = DataTable.create(list1, headers2);
        String expected = """
            <table>
            <thead>
            <tr>
            <th>Column1
            <th>Column2
            </thead>
            <tbody>
            <tr>
            <td>Value1
            <td>10
            <tr>
            <td>Value2
            <td>20
            </tbody>
            </table>
            """;
        assertEquals(expected, dt.formatAsHTML(), "Simple");
        
        dt = DataTable.create(list2.subList(0, 3), headers2);
        expected = """
            <table>
            <thead>
            <tr>
            <th>Column1
            <th>Column2
            </thead>
            <tbody>
            <tr>
            <td>Simple key
            <td>Simple value
            <tr>
            <td>
            <td>Has &quot;Quotes&quot; inside
            <tr>
            <td>&lt;script&gt;alert(&#x27;PWNED&#x27;);&lt;&#x2F;script&gt;
            <td>Simple
            </tbody>
            </table>
            """;
        assertEquals(expected, dt.formatAsHTML(), "Challenging");
        
        dt = DataTable.create(list3, headers3);
        expected = """
            <table>
            <thead>
            <tr>
            <th>Test1
            <th>Test2
            <th>Test3
            </thead>
            <tbody>
            <tr>
            <td>A
            <td>2020-01-01T00:00:00Z
            <td>5
            <tr>
            <td>B
            <td>2025-06-21T00:05:05Z
            <td>2
            </tbody>
            </table>
            """;
        assertEquals(expected, dt.formatAsHTML(), "Three column");
    }
}
