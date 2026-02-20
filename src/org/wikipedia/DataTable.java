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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;

/**
 *  A table of data. Note: this class is not intended to be a data frame or to
 *  be used for data analysis.
 *  @param <T> a Java record class that represents the rows of this table
 *  @author MER-C
 *  @version 0.02
 */
public class DataTable<T extends Record> implements Writable
{
    private final List<T> data;
    private List<String> headers, skipcols, cssclasses;
    private String tableclass;
    private BiFunction<T, Integer, String> rowclass;
    
    // TODO: remove all instances of manually constructed HTML/wikitable exports
    // Sub-tables (for pagination)
    // Make HTML output sortable
    
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
     *  @see #setHeaders(List)
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
     *  @see #getHeaders() 
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
     *  Returns a read-only view of skipped columns on export.
     *  @return the skipped columns
     *  @see #setSkippedColumns(List)
     *  @since 0.02
     */
    public List<String> getSkippedCols()
    {
        if (skipcols == null)
            return null;
        return Collections.unmodifiableList(skipcols);
    }
    
    /**
     *  Sets columns to be skipped when exporting the table. Column names must
     *  match the record parameter name; incorrect column names are ignored. Use
     *  {@code null} to not skip any columns.
     * 
     *  @param cols columns to skip
     *  @throws IllegalArgumentException if the number of skipped columns is
     *  greater than or equal to the number of record components (columns)
     *  @see #getSkippedCols() 
     *  @since 0.02
     */
    public void setSkippedColumns(List<String> cols)
    {
        if (cols != null && !data.isEmpty())
        {
            int ncols = data.get(0).getClass().getRecordComponents().length;
            int nskipcols = cols.size();
            if (nskipcols >= ncols)
                throw new IllegalArgumentException("The number of skipped columns (" + nskipcols + 
                    ") must be less than the total number of columns (" + ncols + ").");
        }
        this.skipcols = cols;
    }
    
    /**
     *  Returns a read-only view of the names of CSS classes to be used when 
     *  exporting the table as HTML.
     *  @return the CSS class names
     *  @see #setColumnClasses(List)
     *  @since 0.02
     */
    public List<String> getColumnClasses()
    {
        if (cssclasses == null)
            return null;
        return Collections.unmodifiableList(cssclasses);
    }
    
    /**
     *  Sets CSS class names to be used when exporting the table as HTML. Use 
     *  {@code null} to not use any CSS classes or {@code null} as an individual
     *  value to leave a column without a class.
     * 
     *  @param colclasses a list of CSS class names
     *  @throws IllegalArgumentException if the number of mappings is not equal 
     *  to the number of record components (columns)
     *  @see #getColumnClasses() 
     *  @since 0.02
     */
    public void setColumnClasses(List<String> colclasses)
    {
        if (colclasses != null && !data.isEmpty())
        {
            int ncols = data.get(0).getClass().getRecordComponents().length;
            int nmappings = colclasses.size();
            if (nmappings != ncols)
                throw new IllegalArgumentException("The number of CSS classes (" + nmappings + 
                    ") must be equal to the total number of columns (" + ncols + ").");
        }
        this.cssclasses = colclasses;
    }
    
    /**
     *  Gets the CSS class to be applied to the table as a whole or {@code null}
     *  if unstyled.
     *  @return (see above)
     *  @see #setTableClass(String) 
     *  @since 0.02
     */
    public String getTableClass()
    {
        return tableclass;
    }
    
    /**
     *  Sets a CSS class to be applied to the the table as a whole. Use {@code 
     *  null} to not use one.
     *  @param cls a CSS class to be applied to the table as a whole
     *  @see #getTableClass()
     *  @since 0.02
     */
    public void setTableClass(String cls)
    {
        this.tableclass = cls;
    }
    
    /**
     *  Gets the two-parameter function that computes the CSS class to be 
     *  applied to each row of the data table.
     *  @return (see above)
     *  @see #setRowClasses(BiFunction) 
     *  @since 0.02
     */
    public BiFunction<T, Integer, String> getRowClasses()
    {
        return rowclass;
    }
    
    /**
     *  Sets the two-parameter function that computes the CSS class to be 
     *  applied to each row of the data table. The parameters of the function
     *  are the record to be rendered and the row number. Use {@code null} to 
     *  not use one.
     *  @param clsfn the two-parameter function to use to compute row classes
     *  @see #getRowClasses()
     *  @since 0.02
     */
    public void setRowClasses(BiFunction<T, Integer, String> clsfn)
    {
        this.rowclass = clsfn;
    }
    
    /**
     *  Exports the table to the given format. Outputs are <em>NOT</em> sanitised 
     *  because they may contain additional formatting.
     *  @return the table in that format
     *  @since 0.02
     */
    public String format(Writable.Format format)
    {
        return switch (format)
        {
            case HTML -> formatAsHTML();
            case WIKITEXT -> formatAsWikitext();
            case CSV ->
            {
                StringBuilder sb = new StringBuilder();
                if (headers != null)
                    writeCsvLine(sb, getOutputHeaders());
                for (T record : data)
                    writeCsvLine(sb, extractValues(record));
                yield sb.toString();
            }
        };
    }
    
    private void writeCsvLine(StringBuilder sb, List<?> values)
    {
        for (int i = 0; i < values.size(); i++)
        {
            if (i > 0)
                sb.append(",");

            Object val = values.get(i);
            if (val == null)
                continue; // equivalent to empty string

            String text = render(val, Writable.Format.WIKITEXT);
            
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

    private String formatAsWikitext()
    {
        StringBuilder sb = new StringBuilder("{| class=\"wikitable sortable");
        if (tableclass != null)
            sb.append(" ").append(tableclass);
        sb.append("\"\n");
        if (headers != null)
        {
            List<String> hdrs = getOutputHeaders();
            sb.append("! ").append(hdrs.get(0));
            for (int i = 1; i < hdrs.size(); i++)
                sb.append(" !! ").append(hdrs.get(i));
            sb.append("\n");
        }
        for (T record : data)
        {
            sb.append("|-\n");
            List<Object> values = extractValues(record);
            for (int i = 0; i < values.size(); i++)
            {
                String sval = render(values.get(i), Writable.Format.WIKITEXT);
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
    
    private String formatAsHTML()
    {
        StringBuilder sb = new StringBuilder();
        if (tableclass == null)
            sb.append("<table>\n");
        else
            sb.append("<table class=\"").append(tableclass).append("\">\n");
        if (cssclasses != null)
        {
            sb.append("<colgroup>\n");
            RecordComponent[] rc = data.get(0).getClass().getRecordComponents();
            for (int i = 0; i < cssclasses.size(); i++)
            {
                if (skipcols == null || !skipcols.contains(rc[i].getName()))
                {
                    if (cssclasses.get(i) == null)
                        sb.append("<col />\n");
                    else
                        sb.append("<col class=\"").append(cssclasses.get(i).replace("\"", "&quot;")).append("\" />\n");
                }
            }
            sb.append("</colgroup>\n");
        }
        if (headers != null)
        {
            sb.append("<thead>\n<tr>\n");
            for (String header : getOutputHeaders())
                sb.append("<th>").append(header).append("\n");
            sb.append("</thead>\n");
        }
        sb.append("<tbody>\n");
        for (int i = 0; i < data.size(); i++)
        {
            T record = data.get(i);
            if (rowclass == null)
                sb.append("<tr>\n");
            else
                sb.append("<tr class=\"").append(rowclass.apply(record, i)).append("\">\n");
            for (Object value : extractValues(record))
            {
                sb.append("<td>");
                sb.append(render(value, Writable.Format.HTML)).append("\n");
            }
        }
        sb.append("</tbody>\n</table>\n");
        return sb.toString();
    }
    
    // helper to transform an object to a string
    private String render(Object value, Writable.Format format)
    {
        return switch (value)
        {
            case null -> "";
            case OffsetDateTime dt -> dt.format(DateTimeFormatter.ISO_DATE_TIME);
            case Duration dt when format.equals(Writable.Format.HTML)-> MathsAndStats.formatDuration(dt);
            case Duration dt when format.equals(Writable.Format.WIKITEXT) -> "data-sort-value=" + dt.getSeconds() +
                " | " + MathsAndStats.formatDuration(dt);
            case Writable w -> w.format(format);
            case Wiki w -> w.getDomain();
            default -> value.toString();
        };
    }
    
    // helper to get headers for output
    private List<String> getOutputHeaders()
    {
        if (skipcols == null)
            return headers;
        RecordComponent[] rc = data.get(0).getClass().getRecordComponents();
        List<String> ret = new ArrayList<>();
        for (int i = 0; i < rc.length; i++)
            if (!skipcols.contains(rc[i].getName()))
                ret.add(headers.get(i));
        return ret;
    }
    
    // helper to extract component values from a Record using reflection
    private List<Object> extractValues(T record)
    {
        RecordComponent[] components = record.getClass().getRecordComponents();
        List<Object> values = new ArrayList<>();
        try
        {
            for (RecordComponent component : components)
                if (skipcols == null || !skipcols.contains(component.getName()))
                    values.add(component.getAccessor().invoke(record));
        } 
        catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException("Failed to extract record values", e);
        }
        return values;
    }
}
