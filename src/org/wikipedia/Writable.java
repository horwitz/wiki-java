/**
 *  @(#)Writable.java 0.02 06/04/2026
 *  Copyright (C) 2026-2026 MER-C
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

/**
 *  Represents things that can be written out to at least wikitext and HTML for
 *  presentation to users. Other text output formats are occasionally supported.
 *  @author MER-C
 *  @version 0.02
 */
public interface Writable
{
    /**
     *  Supported output formats.
     */
    public enum Format
    {
        /**
         *  Renders an object as HTML.
         */
        HTML, 
        
        /**
         *  Renders an object as wikitext. 
         */
        WIKITEXT, 
        
        /**
         *  Renders an object as CSV. This is optional and is intended for
         *  tabular data.
         */
        CSV;              
    }
    
    /**
     *  The trivial {@link Writable} that returns the given string independent 
     *  of format.
     *  @param str the string to write
     *  @since 0.02
     */
    public record Identity(String str) implements Writable
    {
        /**
         *  {@return the string supplied on construction}
         *  @param format disregarded
         */
        @Override
        public String format(Writable.Format format)
        {
            return str;
        }
    }
    
    /**
     *  Formats this object in the given format for presenting to the user.
     *  @param format the output format 
     *  @return this object, rendered
     *  @throws UnsupportedOperationException if this output format is not
     *  supported
     */
    public String format(Format format);
}
