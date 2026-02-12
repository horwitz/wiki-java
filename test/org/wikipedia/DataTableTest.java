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
import java.util.function.BiFunction;
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
    
    private final List<String> headers2, headers3, skipcols;
    private List<TwoColRecord> list1, list2;
    private List<MultiColRecord> list3;
    
    /**
     *  Construct wiki objects for each test so that tests are independent.
     */
    public DataTableTest()
    {
        headers2 = List.of("Column1", "Column2");
        headers3 = List.of("Test1", "Test2", "Test3");
        skipcols = List.of("col2", "col4");
        
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
        assertThrows(IllegalArgumentException.class, () -> DataTable.create(list1, headers3), "Too many headers");
    }
    
    @Test
    public void headers() 
    {
        DataTable dt = DataTable.create(list1, headers2);
        assertEquals(headers2, dt.getHeaders(), "From constructor");
        assertThrows(IllegalArgumentException.class, () -> DataTable.create(list1, headers3), "Too many headers");
        
        List<String> newheaders2 = List.of("Test1", "Test2");
        dt.setHeaders(newheaders2);
        assertEquals(newheaders2, dt.getHeaders(), "Get/set");
        
        dt.setHeaders(null);
        assertNull(dt.getHeaders(), "Null accepted");
    }
    
    @Test
    public void skipCols()
    {
        DataTable dt = DataTable.create(list3, headers3);
        assertNull(dt.getSkippedCols(), "Default");
        assertThrows(IllegalArgumentException.class, () -> dt.setSkippedColumns(headers3), "Too many skipped columns");
        
        dt.setSkippedColumns(skipcols);
        assertEquals(skipcols, dt.getSkippedCols(), "Get/set");
        
        dt.setSkippedColumns(null);
        assertNull(dt.getSkippedCols(), "Null accepted");
    }
    
    @Test
    public void columnClasses()
    {
        DataTable dt = DataTable.create(list1, headers2);
        assertNull(dt.getColumnClasses(), "Default");
        assertThrows(IllegalArgumentException.class, () -> dt.setColumnClasses(headers3), "Too many CSS classes");
        
        dt.setColumnClasses(headers2);
        assertEquals(headers2, dt.getColumnClasses(), "Get/set");
        
        dt.setColumnClasses(null);
        assertNull(dt.getColumnClasses(), "Null accepted");
    }
    
    @Test
    public void tableClass()
    {
        DataTable dt = DataTable.create(list1, headers2);
        assertNull(dt.getTableClass(), "Default");
        
        dt.setTableClass("testclass");
        assertEquals("testclass", dt.getTableClass(), "Get/set");
        
        dt.setTableClass(null);
        assertNull(dt.getTableClass(), "Null accepted");
    }
    
    @Test
    public void rowClasses()
    {
        DataTable<TwoColRecord> dt = DataTable.create(list1, headers2);
        assertNull(dt.getRowClasses(), "Default");
        
        BiFunction<TwoColRecord, Integer, String> bf = (rec, rn) -> "Test";
        dt.setRowClasses(bf);
        assertEquals(bf, dt.getRowClasses(), "Get/set");
        
        dt.setRowClasses(null);
        assertNull(dt.getRowClasses(), "Null accepted");
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
        assertEquals(expected, dt.format(Writable.Format.CSV), "Simple");
        
        dt = DataTable.create(list1, null);
        expected = """
            Value1,10
            Value2,20
            """;
        assertEquals(expected, dt.format(Writable.Format.CSV), "Null header");
        
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
        assertEquals(expected, dt.format(Writable.Format.CSV), "Challenging");
        
        dt = DataTable.create(list3, headers3);
        expected = """
            Test1,Test2,Test3
            A,2020-01-01T00:00:00Z,5
            B,2025-06-21T00:05:05Z,2
            """;
        assertEquals(expected, dt.format(Writable.Format.CSV), "Three column");
        
        dt.setSkippedColumns(skipcols);
        expected = """
            Test1,Test3
            A,5
            B,2
            """;
        assertEquals(expected, dt.format(Writable.Format.CSV), "Three column with skipped columns");
    }

    @Test
    public void formatAsWikitext() 
    {
        DataTable<TwoColRecord> dt = DataTable.create(list1, headers2);
        String expected = """
            {| class="wikitable sortable"
            ! Column1 !! Column2
            |-
            | Value1 || 10
            |-
            | Value2 || 20
            |}
            """;
        assertEquals(expected, dt.format(Writable.Format.WIKITEXT), "Simple");
        
        dt = DataTable.create(list1, null);
        expected = """
            {| class="wikitable sortable"
            |-
            | Value1 || 10
            |-
            | Value2 || 20
            |}
            """;
        assertEquals(expected, dt.format(Writable.Format.WIKITEXT), "Null header");
        
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
        assertEquals(expected, dt.format(Writable.Format.WIKITEXT), "Challenging");
        
        DataTable<MultiColRecord> dt2 = DataTable.create(list3, headers3);
        expected = """
            {| class="wikitable sortable"
            ! Test1 !! Test2 !! Test3
            |-
            | A || 2020-01-01T00:00:00Z || 5
            |-
            | B || 2025-06-21T00:05:05Z || 2
            |}
            """;
        assertEquals(expected, dt2.format(Writable.Format.WIKITEXT), "Three column");
        
        dt2.setSkippedColumns(skipcols);
        expected = """
            {| class="wikitable sortable"
            ! Test1 !! Test3
            |-
            | A || 5
            |-
            | B || 2
            |}
            """;
        assertEquals(expected, dt2.format(Writable.Format.WIKITEXT), "Three column with skipped columns");
        
        dt = DataTable.create(list1, headers2);
        dt.setTableClass("class1");
        expected = """
            {| class="wikitable sortable class1"
            ! Column1 !! Column2
            |-
            | Value1 || 10
            |-
            | Value2 || 20
            |}
            """;
        assertEquals(expected, dt.format(Writable.Format.WIKITEXT), "Styled");
    }
    
    @Test
    public void formatAsHTML()
    {
        DataTable<TwoColRecord> dt = DataTable.create(list1, headers2);
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
        assertEquals(expected, dt.format(Writable.Format.HTML), "Simple");
        
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
            <td>Has "Quotes" inside
            <tr>
            <td><script>alert('PWNED');</script>
            <td>Simple
            </tbody>
            </table>
            """;
        assertEquals(expected, dt.format(Writable.Format.HTML), "Challenging");
        
        DataTable<MultiColRecord> dt2 = DataTable.create(list3, headers3);
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
        assertEquals(expected, dt2.format(Writable.Format.HTML), "Three column");
        
        dt2.setSkippedColumns(skipcols);
        expected = """
            <table>
            <thead>
            <tr>
            <th>Test1
            <th>Test3
            </thead>
            <tbody>
            <tr>
            <td>A
            <td>5
            <tr>
            <td>B
            <td>2
            </tbody>
            </table>
            """;
        assertEquals(expected, dt2.format(Writable.Format.HTML), "Three column with skipped columns");
        
        List<String> classes = new ArrayList<>();
        classes.add(null);
        classes.add("column2");
        dt = DataTable.create(list1, headers2);
        dt.setColumnClasses(classes);
        dt.setTableClass("class1");
        dt.setRowClasses((rec, rn) -> rec.key() + "_" + rn);
        expected = """
            <table class="class1">
            <colgroup>
            <col />
            <col class="column2" />
            </colgroup>
            <thead>
            <tr>
            <th>Column1
            <th>Column2
            </thead>
            <tbody>
            <tr class="Value1_0">
            <td>Value1
            <td>10
            <tr class="Value2_1">
            <td>Value2
            <td>20
            </tbody>
            </table>
            """;
        assertEquals(expected, dt.format(Writable.Format.HTML), "Styled");
        
        dt2 = DataTable.create(list3, headers3);
        classes = new ArrayList<>();
        classes.add(null);
        classes.add("skipped");
        classes.add("column2");
        dt2.setSkippedColumns(skipcols);
        dt2.setColumnClasses(classes);
        expected = """
            <table>
            <colgroup>
            <col />
            <col class="column2" />
            </colgroup>
            <thead>
            <tr>
            <th>Test1
            <th>Test3
            </thead>
            <tbody>
            <tr>
            <td>A
            <td>5
            <tr>
            <td>B
            <td>2
            </tbody>
            </table>
            """;
        assertEquals(expected, dt2.format(Writable.Format.HTML), "Styled three column with skipped columns");
    }
}
