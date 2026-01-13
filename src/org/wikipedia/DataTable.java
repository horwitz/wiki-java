/**
 *  @(#)DataTable.java 0.02 13/01/2026
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

import java.lang.reflect.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 *  A table of data. Note: this class is not intended to be a data frame or to
 *  be used for data analysis.
 *  @param <T> a Java record class that represents the rows of this table
 *  @author MER-C
 *  @version 0.02
 */
public class DataTable<T extends Record>
{
    private final List<T> data;
    private List<String> headers;
    
    /**
     *  Creates a new data table.
     *  @param <T> a Java record class that represents the rows of this table
     *  @param data the input data, must not be null
     *  @param headers the column headers used for export, {@code null} means 
     *  the table has no headers
     *  @return the constructed table
     *  @throws IllegalArgumentException if the number of headers does not equal
     *  the number of record components (columns)
     */
    public static <T extends Record> DataTable<T> create(List<T> data, List<String> headers)
    {
        DataTable table = new DataTable(data);
        table.setHeaders(headers);
        return table;
    }
    
    private DataTable(List<T> data)
    {
        this.data = Objects.requireNonNull(data, "Data list cannot be null");
    }
    
    /**
     *  Returns a read-only view of the table's headers.
     *  @return the list of headers
     */
    public List<String> getHeaders()
    {
        if (headers == null)
            return null;
        return Collections.unmodifiableList(headers);
    }
    
    /**
     *  Sets the headers for this table. A {@code null} header list results in 
     *  the table having no headers.
     *  @param headers the headers for this table
     *  @throws IllegalArgumentException if the number of headers does not equal
     *  the number of record components (columns)
     */
    public void setHeaders(List<String> headers)
    {
        if (headers != null && !data.isEmpty())
        {
            int ncols = data.get(0).getClass().getRecordComponents().length;
            int nhdrs = headers.size();
            if (ncols != nhdrs)
                throw new IllegalArgumentException("The number of headers (" + nhdrs + 
                    ") must equal the number of columns (" + ncols + ").");
        }
        this.headers = headers;
    }
    
    /**
     *  Exports the table to CSV. 
     *  @return the table in CSV format
     */
    public String formatAsCSV()
    {
        StringBuilder sb = new StringBuilder();
        if (headers != null)
            writeCsvLine(sb, headers.toArray());
        for (T record : data)
            writeCsvLine(sb, extractValues(record));
        return sb.toString();
    }
    
    private void writeCsvLine(StringBuilder sb, Object[] values)
    {
        for (int i = 0; i < values.length; i++)
        {
            if (i > 0)
                sb.append(",");

            Object val = values[i];
            if (val == null)
                continue; // equivalent to empty string

            String text = render(val);
            
            // RFC 4180: Quote if text contains comma, double-quote, or newline
            boolean searchQuotes = text.contains("\"");
            boolean searchDelimiter = text.contains(",");
            boolean searchNewLines = text.contains("\n") || text.contains("\r");

            if (searchQuotes || searchDelimiter || searchNewLines)
            {
                sb.append("\"");
                // Escape existing double quotes by doubling them (" -> "")
                sb.append(text.replace("\"", "\"\""));
                sb.append("\"");
            }
            else
                sb.append(text);
        }
        sb.append("\n");
    }
    
    /**
     *  Exports the table to wikitext.
     *  @return the table in wikitext format
     */
    public String formatAsWikitext()
    {
        StringBuilder sb = new StringBuilder("{| class=\"wikitable sortable\"\n");
        if (headers != null)
        {
            sb.append("! ").append(headers.get(0));
            for (int i = 1; i < headers.size(); i++)
                sb.append(" !! ").append(headers.get(i));
            sb.append("\n");
        }
        for (T record : data)
        {
            sb.append("|-\n");
            Object[] values = extractValues(record);
            for (int i = 0; i < values.length; i++)
            {
                String sval = render(values[i]);
                if (i == 0)
                    sb.append("| ").append(sval);
                else
                    sb.append(" || ").append(sval);
            }
            sb.append("\n");
        }
        sb.append("|}\n");
        return sb.toString();
    }
    
    /**
     *  Exports the table to HTML without styling.
     *  @return the table in HTML format
     *  @since 0.02
     */
    public String formatAsHTML()
    {
        StringBuilder sb = new StringBuilder("<table>\n");
        if (headers != null)
        {
            sb.append("<thead>\n<tr>\n");
            for (String header : headers)
                sb.append("<th>").append(HTMLUtils.sanitizeForHTML(header)).append("\n");
            sb.append("</thead>\n");
        }
        sb.append("<tbody>\n");

        for (T record : data)
        {
            sb.append("<tr>\n");
            Object[] values = extractValues(record);
            for (Object value : values)
            {
                sb.append("<td>");
                sb.append(HTMLUtils.sanitizeForHTML(render(value))).append("\n");
            }
        }
        sb.append("</tbody>\n</table>\n");
        return sb.toString();
    }
    
    // helper to transform an object to a string
    private String render(Object value)
    {
        return switch (value)
        {
            case null -> "";
            case OffsetDateTime dt -> dt.format(DateTimeFormatter.ISO_DATE_TIME);
            default -> value.toString();
        };
    }
    
    // helper to extract component values from a Record using reflection
    private Object[] extractValues(T record)
    {
        RecordComponent[] components = record.getClass().getRecordComponents();
        Object[] values = new Object[components.length];
        try
        {
            for (int i = 0; i < components.length; i++)
                values[i] = components[i].getAccessor().invoke(record);
        } 
        catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException("Failed to extract record values", e);
        }
        return values;
    }
}