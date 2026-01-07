/**
 *  @(#)BaseServlet.java 0.01 02/01/2026
 *  Copyright (C) 2026 - 2026 MER-C
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.

 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.wikipedia.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.*;

/**
 *  Common, stateful servlet code.
 *  @author MER-C
 *  @version 0.01
 */
public abstract class BaseServlet extends HttpServlet
{
    /**
     *  Responds to GET requests. Before it does so, it sets common headers,
     *  checks if the request is allowed, and shows a CAPTCHA if there are any
     *  parameters.
     *  @param request the servlet request
     *  @param response the servlet response
     *  @throws ServletException if the request could not be handled
     *  @throws IOException if a network error occurs
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        ServletUtils.setHeaders(request, response);
        if (ServletFirewall.isAllowed(request, response))
            if (ServletUtils.showCaptcha(request, response, getCaptchaParams(), 4))
                processRequest(request, response);
    }
    
    /**
     *  Responds to POST requests. Before it does so, it sets common headers and
     *  checks if the request is allowed.
     *  @param request the servlet request
     *  @param response the servlet response
     *  @throws ServletException if the request could not be handled
     *  @throws IOException if a network error occurs
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        ServletUtils.setHeaders(request, response);
        if (ServletFirewall.isAllowed(request, response))
            processRequest(request, response);
    }
    
    /**
     *  Returns the HTTP parameters used to calculate a CAPTCHA challenge string
     *  when a CAPTCHA is shown.
     *  @return (see above) 
     *  @see ServletUtils#showCaptcha(HttpServletRequest, HttpServletResponse, List, int)
     */
    public List<String> getCaptchaParams()
    {
        return Collections.emptyList();
    }
    
    /**
     *  Responds to GET and POST requests.
     *  @param request the servlet request
     *  @param response the servlet response
     *  @throws ServletException if the request could not be handled
     *  @throws IOException if a network error occurs
     */
    public abstract void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
}
