/**
 *  @(#)UserInfoServlet.java 0.01 11/03/2026
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
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.wikipedia.servlets;

import java.io.*;
import java.util.*;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.wikipedia.*;
import org.wikipedia.tools.UserInfo;

/**
 *  Servlet front-end for {@link org.wikipedia.tools.UserInfo}.
 *  @author MER-C
 *  @version 0.01
 *  @see org.wikipedia.tools.UserInfo
 */
@WebServlet(name = "UserInfo", urlPatterns = {"/userinfo.jsp"})
public class UserInfoServlet extends BaseServlet
{
    /**
     *  {@inheritDoc}
     */
    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        // 1. Parse parameters
        request.setAttribute("toolname", "User Information");
        request.setAttribute("scripts", new String[] { "common.js", "UserInfo.js" });

        String wikistr = ServletUtils.sanitizeForAttributeOrDefault(request.getParameter("wiki"), "en.wikipedia.org");
        String mode = request.getParameter("mode");
        String users = request.getParameter("users");
        String category = ServletUtils.sanitizeForAttribute(request.getParameter("category"));

        // 2. Perform business logic if submitted
        Wiki wiki = WMFWikiFarm.instance().sharedSession(wikistr);
        wiki.setQueryLimit(10000); // 20 network requests, GAE only allows run time of 15s
        List<String> usernames = Collections.EMPTY_LIST;
        DataTable dt = null;
        if (mode != null)
        {
            if (mode.equals("users"))
                usernames = Arrays.asList(users.split("\r\n"));
            else if (mode.equals("category"))
                usernames = wiki.getCategoryMembers(category, Wiki.USER_NAMESPACE);
        
            if (usernames.isEmpty())
                request.setAttribute("error", "No users found!");
            else
            {
                try
                {
                    List<String> usernames2 = new ArrayList<>();
                    for (String t : usernames)
                        usernames2.add(wiki.removeNamespace(t.trim(), Wiki.USER_NAMESPACE));
                    dt = UserInfo.userInfoTable(wiki, usernames2, Writable.Format.HTML);
                }
                catch (IllegalArgumentException ex)
                {
                    request.setAttribute("error", "Invalid username(s) or category name!");
                }
            }
        }
        
        // 3. Render output
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter())
        {
            ServletUtils.renderHeader(request, response, out);
            List<String> modes = ServletUtils.generateRadioButtons("mode", List.of("users", "category"), request);
            out.printf("""
                <p>
                This tool displays user information from a variety of sources in
                a convenient table. A query limit of 10000 edits applies.
                       
                <p>
                <form action="./userinfo.jsp" method=POST>
                <table>
                <tr>
                    <td>%s
                    <td><label for=users>Users (one per line):</label>
                    <td><textarea name=users id=users rows=10 %s>%s</textarea>
                <tr>
                    <td>%s
                    <td><label for=category>Fetch users from category:</label>
                    <td><input type=text name=category id=category value="%s" %s> (not recursive)
                <tr>
                    <td colspan=2>Wiki:
                    <td><input type=text name="wiki" value="%s" required>
                </table>
                <input type=submit value="Get user info">
                </form>
                """, modes.get(0), "users".equals(mode) || mode == null ? "required" : "disabled", 
                HTMLUtils.sanitizeForHTML(users), 
                modes.get(1), category,
                "category".equals(mode) ? "required" : "disabled", wikistr);
            if (dt != null)
            {
                out.print("<hr>");
                // TODO: add links to contribution survey etc here
                dt.setTableClass("wikitable");
                out.print(dt.format(Writable.Format.HTML));
            }
            ServletUtils.renderFooter(request, out);
        }
    }
}
