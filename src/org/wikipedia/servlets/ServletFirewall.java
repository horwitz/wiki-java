/**
 *  @(#)ServletFirewall.java 0.01 26/03/2026
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
package org.wikipedia.servlets;

import java.io.IOException;

import jakarta.servlet.http.*;

/**
 *  Logic for filtering requests to tools.
 *  @author MER-C
 *  @version 0.01
 */
public class ServletFirewall
{
    /**
     *  {@return {@code true} if this request is allowed to proceed to business
     *  logic} Implementations are expected to set the HTTP status and 
     *  appropriate headers/content if not allowed. This is a placeholder that
     *  always returns {@code true}.
     *  @param request the request to check
     *  @param response the response to set the status of
     *  @throws IOException if a network error occurs
     */
    public static boolean isAllowed(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        return true;
    }
}
