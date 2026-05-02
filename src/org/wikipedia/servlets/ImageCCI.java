/**
 *  @(#)ImageCCI.java 0.05 02/05/2026
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
import java.util.zip.ZipOutputStream;

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
 *  @version 0.05
 */
@WebServlet(name = "ImageCCI", urlPatterns = {"/imagecci.jsp"})
public class ImageCCI extends BaseServlet
{
    /**
     *  Ensures that CAPTCHAs are computed over the defining parameter of the
     *  survey, that is the user or category being surveyed and the wiki these
     *  reside on.
     *  @return {@code List.of("user", "wiki", "category")}
     *  @since 0.04
     */
    @Override
    public List<String> getCaptchaParams()
    {
        return List.of("user", "wiki", "category");
    }
    
    /**
     *  {@inheritDoc}
     *  @since 0.04
     */
    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        // 1. Parse parameters
        // category not enabled because API list=allusers aiuser parameter is not vectorised
        // (it works, it's just too slow)
        request.setAttribute("toolname", "Image contribution surveyor");
        // request.setAttribute("scripts", new String[] { "common.js", "ContributionSurveyor.js" });
        String homewiki = ServletUtils.sanitizeForAttributeOrDefault(request.getParameter("wiki"), "en.wikipedia.org");
        String user = request.getParameter("user");
        String category = null; // request.getParameter("category");
        boolean transferred = (request.getParameter("transferred") != null);
        Wiki.Interval interval = ServletUtils.parseIntervalParams(request);
        boolean comingled = false; // (request.getParameter("comingled") != null);
        
        Wiki wiki = WMFWikiFarm.instance().sharedSession(homewiki);
        wiki.setQueryLimit(3500); // 7 network requests, GAE only allows run time of 15s

        // TODO: consolidate front-end user processing code
        // see also CommandLineParser.parseUserOptions2
        List<String> users = new ArrayList<>();
        if (user != null)
            users.add(user);
        else if (category != null)
        {
            List<String> catmembers = wiki.getCategoryMembers(category, Wiki.USER_NAMESPACE);
            if (catmembers.isEmpty())
                request.setAttribute("error", "Category \"" + HTMLUtils.sanitizeForHTML(category) + "\" contains no users!");
            else
                for (String tempstring : catmembers)
                    users.add(wiki.removeNamespace(tempstring));
        }
    
        // 2. Perform business logic if form submitted (defining parameter: user list)
        List<ContributionSurveyor.Survey> survey = null;
        ContributionSurveyor surveyor = new ContributionSurveyor(wiki);
        if (request.getAttribute("error") == null && !users.isEmpty())
        {
            surveyor.setInterval(interval);
            surveyor.setComingled(comingled);
            surveyor.setFooter("Survey URL: " + ServletUtils.getRequestURL(request));
            surveyor.setSurveyingTransferredFiles(transferred);
            survey = surveyor.runSurvey(users, false, false, true);
            
            if (survey.isEmpty())
            {
                request.setAttribute("error", "No uploads found!");
                survey = null;
            }
        }
        
        // 3. Render output
        String surveyhtml = null;
        if (survey != null)
        {
            String output = request.getParameter("format");
            String fname = user == null ? category : user;
            List<String> sl = surveyor.pages(survey, 50, 20, Writable.Format.WIKITEXT);
            switch (output)
            {
                // TODO: this is common to CCI servlets but could be applicable to other tools?
                case null:
                case "text":
                    response.setContentType("text/plain;charset=UTF-8");
                    response.setHeader("Content-Disposition", "attachment; filename=" 
                        + URLEncoder.encode(fname, StandardCharsets.UTF_8) + ".txt");
                    try (PrintWriter out = response.getWriter())
                    {
                        out.print(String.join("\n", sl));
                    }
                    return;
                case "zip":
                    response.setContentType("application/zip");
                    response.setHeader("Content-Disposition", "attachment; filename=" 
                        + URLEncoder.encode(fname, StandardCharsets.UTF_8) + ".zip");
                    Map<String, byte[]> zip = new LinkedHashMap<>();
                    for (int i = 0; i < survey.size(); i++)
                        zip.put(fname + ".txt" + (i == 0 ? "" : ".%03d".formatted(i)), sl.get(i).getBytes());
                    try (ZipOutputStream zout = new ZipOutputStream(response.getOutputStream()))
                    {
                        ContributionSurveyor.outputZipFile(zout, zip);
                    }
                    return;
                case "html": // plain list of images (to be made default)
                case "gallery": // thumbnails
                case "json":
                default:
                    // TODO: NOT IMPLEMENTED
            }
        }
        
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter())
        {
            ServletUtils.renderHeader(request, response, out);
            out.printf("""
                <p>
                This tool generates a listing of a user's image uploads for use at <a
                href="//en.wikipedia.org/wiki/WP:CCI">Contributor copyright investigations.</a>
                A query limit of 3500 uploads per wiki (after date interval filter) applies.

                <p>
                <form action="./imagecci.jsp" method=GET>
                <table>
                <tr>
                    <!--<td><input type=radio name=mode id="radio_user" checked>-->
                    <td><label for=user>User to survey:</label>
                    <td><input type=text name=user id=user value="%s" required>
                <!--<tr>
                    <td><input type=radio name=mode id="radio_category">
                    <td><label for=radio_category>Fetch users from category:</label>
                    <td><input type=text name=category id=category value="%s" disabled>-->
                <tr>
                    <td>Home wiki:
                    <td><input type=text name="wiki" value="%s" required>
                <tr>
                    <td>Include uploads from:
                    <td>%s
                <tr>
                    <td colspan=2>%s
                <!--<tr>
                    <td colspan=2>Output:
                    <td>%s-->
                <tr>
                    <td>Output format:
                    <td><input type=radio name=format id=format_text value=text checked><label for=format_text>Text</label>
                        <input type=radio name=format id=format_zip value=zip><label for=format_zip>Zip</label>
                </table>
                <br>
                <input type=submit value="Survey user">
                </form>
                """, ServletUtils.sanitizeForAttribute(user), 
                    ServletUtils.sanitizeForAttribute(category),
                    homewiki,
                    ServletUtils.addIntervalInputs(request, null, null),
                    ServletUtils.addCheckbox("transferred", transferred, 
                        "Include transferred files (may be inaccurate depending on username)"),
                    ServletUtils.addCheckbox("comingle", comingled, "comingled (for sockfarms where each user has few edits)"));
            if (surveyhtml != null)
                out.print(surveyhtml);
            ServletUtils.renderFooter(request, out);
        }
    }
}
