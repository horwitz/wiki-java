/**
 *  @(#)ImageCCI.java 0.04 02/01/2026
 *  Copyright (C) 2011 - 2026 MER-C
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

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.wikipedia.*;
import org.wikipedia.tools.ContributionSurveyor;

/**
 *  An image contribution surveyor servlet. This is a web front end for
 *  {@link org.wikipedia.tools.ContributionSurveyor}.
 *  @see <a href="https://wikipediatools.appspot.com/imagecci.jsp">Official instance</a>
 *  @author MER-C
 *  @version 0.04
 */
@WebServlet(name = "ImageCCI", urlPatterns = {"/imagecci.jsp"})
public class ImageCCI extends BaseServlet
{
    /**
     *  Ensures that CAPTCHAs are computed over the defining parameter of the
     *  survey, that is the user being surveyed.
     *  @return {@code List.of("user")}
     *  @since 0.04
     */
    @Override
    public List<String> getCaptchaParams()
    {
        return List.of("user");
    }
    
    /**
     *  {@inheritDoc}
     *  @since 0.04
     */
    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        // 1. Parse parameters
        request.setAttribute("toolname", "Image contribution surveyor");
        String homewiki = ServletUtils.sanitizeForAttributeOrDefault(request.getParameter("wiki"), "en.wikipedia.org");
        String user = request.getParameter("user");
        boolean transferred = (request.getParameter("transferred") != null);
        Wiki.Interval interval = ServletUtils.parseIntervalParams(request);
    
        // 2. Perform business logic if form submitted (defining parameter: user)
        List<String> survey = null;
        if (user != null && request.getAttribute("error") == null)
        {
            WMFWiki wiki = WMFWikiFarm.instance().sharedSession(homewiki);
            wiki.setMaxLag(-1);
            ContributionSurveyor surveyor = new ContributionSurveyor(wiki);
            surveyor.setInterval(interval);
            surveyor.setFooter("Survey URL: " + ServletUtils.getRequestURL(request));
            surveyor.setSurveyingTransferredFiles(transferred);
            survey = surveyor.outputContributionSurvey(List.of(user), false, false, true);
        }
        
        // 3. Render output
        String surveyhtml = null;
        if (survey != null)
        {
            String output = "text";
            if (survey.isEmpty())
                request.setAttribute("error", "ERROR: User " + HTMLUtils.sanitizeForHTML(user) + " does not exist!");
            else if (output.equals("text"))
            {
                response.setContentType("text/plain;charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=" 
                    + URLEncoder.encode(user, StandardCharsets.UTF_8) + ".txt");
                try (PrintWriter out = response.getWriter())
                {
                    out.print(String.join("\n", survey));
                }
                return;
            }
            else if (output.equals("zip"))
            {
                response.setContentType("application/zip");
                response.setHeader("Content-Disposition", "attachment; filename=" 
                    + URLEncoder.encode(user, StandardCharsets.UTF_8) + ".zip");
                Map<String, byte[]> zip = new LinkedHashMap<>();
                for (int i = 0; i < survey.size(); i++)
                    zip.put(user + (i == 0 ? "" : ".txt.%03d".formatted(i)), survey.get(i).getBytes());
                try (ZipOutputStream zout = new ZipOutputStream(response.getOutputStream()))
                {
                    ContributionSurveyor.outputZipFile(zout, zip);
                }
                return;
            }
            else if (output.equals("html")) // plain list of images (to be made default)
            {
                // TODO: NOT IMPLEMENTED
            }
            else if (output.equals("gallery")) // thumbnails
            {
                // TODO: NOT IMPLEMENTED
            }
            else if (output.equals("json"))
            {
                // TODO: NOT IMPLEMENTED
            }
        }

        try (PrintWriter out = response.getWriter())
        {
            ServletUtils.renderHeader(request, response, out);
            // TODO: feature parity with the text contribution surveyor
            out.printf("""
                <p>
                This tool generates a listing of a user's image uploads for use at <a
                href="//en.wikipedia.org/wiki/WP:CCI">Contributor copyright investigations.</a>

                <p>
                <form action="./imagecci.jsp" method=GET>
                <table>
                <tr>
                    <td>User to survey:
                    <td><input type=text name=user value="%s" required>
                <tr>
                    <td>Home wiki:
                    <td><input type=text name="wiki" value="%s" required>
                <tr>
                    <td>Include uploads from:
                    <td>%s
                </table>
                <input type=checkbox name=transferred value="%s">Include transferred files 
                    (may be inaccurate depending on username)
                <br>
                <input type=submit value="Survey user">
                </form>
                """, ServletUtils.sanitizeForAttribute(user), homewiki,
                    ServletUtils.addIntervalInputs(request, null, null),
                    transferred ? " checked" : "");
            if (surveyhtml != null)
                out.print(surveyhtml);
            ServletUtils.renderFooter(request, out);
        }
    }
}
