/**
 *  @(#)ContributionSurveyorServlet.java 0.04 02/05/2026
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
import java.time.*;
import java.util.*;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.wikipedia.*;
import org.wikipedia.tools.ContributionSurveyor;

/**
 *  A text contribution surveyor servlet. This is a web front end for
 *  {@link org.wikipedia.tools.ContributionSurveyor}.
 *  @see <a href="https://wikipediatools.appspot.com/contributionsurveyor.jsp">Official instance</a>
 *  @author MER-C
 *  @version 0.04
 */
@WebServlet(name = "ContributionSurveyor", urlPatterns = {"/contributionsurveyor.jsp"})
public class ContributionSurveyorServlet extends BaseServlet
{
    /**
     *  Ensures that CAPTCHAs are computed over the defining parameter of the
     *  survey, that is the user or category being surveyed and the wiki these
     *  reside on.
     *  @return {@code List.of("user", "wiki", "category")}
     *  @since 0.03
     */
    @Override
    public List<String> getCaptchaParams()
    {
        return List.of("user", "wiki", "category");
    }
    
    /**
     *  {@inheritDoc}
     *  @since 0.03
     */
    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        // 1. Parse parameters
        request.setAttribute("toolname", "Contribution surveyor");
        request.setAttribute("scripts", new String[] { "common.js", "ContributionSurveyor.js" });

        String mode = Objects.requireNonNullElse(request.getParameter("mode"), "user");
        String un = request.getParameter("user");
        String category = request.getParameter("category");
        boolean nominor = (request.getParameter("nominor") != null);
        boolean noreverts = (request.getParameter("noreverts") != null);
        boolean nodrafts = (request.getParameter("nodrafts") != null);
        boolean newonly = (request.getParameter("newonly") != null);
        boolean comingle = (request.getParameter("comingle") != null);
        String blockedafterstr = request.getParameter("blockedafter");
        Wiki.Interval interval = ServletUtils.parseIntervalParams(request);
        String homewiki = ServletUtils.sanitizeForAttributeOrDefault(request.getParameter("wiki"), "en.wikipedia.org");
        String bytefloor = ServletUtils.sanitizeForAttributeOrDefault(request.getParameter("bytefloor"), "150");
    
        Wiki wiki = WMFWikiFarm.instance().sharedSession(homewiki);
        wiki.setQueryLimit(10000); // 20 network requests, GAE only allows run time of 15s

        // TODO: consolidate front-end user processing code
        // see also CommandLineParser.parseUserOptions2
        List<String> users = new ArrayList<>();
        OffsetDateTime blockedafter = (blockedafterstr == null || blockedafterstr.isEmpty()) ? null : 
            LocalDate.parse(blockedafterstr).atTime(OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC));
        if (un != null)
            users.add(un);
        else if (category != null)
        {
            List<String> catmembers = wiki.getCategoryMembers(category, Wiki.USER_NAMESPACE);
            if (catmembers.isEmpty())
                request.setAttribute("error", "Category \"" + HTMLUtils.sanitizeForHTML(category) + "\" contains no users!");
            else
                for (String tempstring : catmembers)
                    users.add(wiki.removeNamespace(tempstring));
        }
        // filter for users blocked after __ (for persistent sockfarms)
        if (blockedafter != null)
        {
            List<Wiki.User> userobjs = wiki.getUsers(users);
            users.clear();
            for (Wiki.User user : userobjs)
            {
                Wiki.LogEntry block = user.getBlockDetails();
                if (block == null || block.getTimestamp().isAfter(blockedafter))
                    users.add(user.getUsername());
            }
        }

        // 2. Perform business logic if form submitted (defining parameter: user list)
        List<ContributionSurveyor.Survey> survey = null;
        ContributionSurveyor surveyor = new ContributionSurveyor(wiki);
        if (request.getAttribute("error") == null && !users.isEmpty())
        {
            surveyor.setIgnoringMinorEdits(nominor);
            surveyor.setIgnoringReverts(noreverts);
            surveyor.setNewOnly(newonly);
            surveyor.setComingled(comingle);
            surveyor.setInterval(interval);
            surveyor.setMinimumSizeDiff(Integer.parseInt(bytefloor));
            surveyor.setFooter("Survey URL: " + ServletUtils.getRequestURL(request));

            // ns 118 = draft namespace on en.wikipedia
            int[] ns = nodrafts ? new int[] { Wiki.MAIN_NAMESPACE } : new int[] { Wiki.MAIN_NAMESPACE, Wiki.USER_NAMESPACE, 118 };
            survey = surveyor.runSurvey(users, true, false, false, ns);
            
            if (survey.isEmpty())
            {
                request.setAttribute("error", "No edits found!");
                survey = null;
            }
        }
        
        // 3. Render output
        String surveyhtml = null;
        if (survey != null)
        {
            String output = request.getParameter("format");
            String fname = un == null ? category : un;
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
                    for (int i = 0; i < sl.size(); i++)
                        zip.put(fname + ".txt" + (i == 0 ? "" : ".%03d".formatted(i)), sl.get(i).getBytes());
                    try (ZipOutputStream zout = new ZipOutputStream(response.getOutputStream()))
                    {
                        ContributionSurveyor.outputZipFile(zout, zip);
                    }
                    return;
                case "html": // plain list of edits in HTML
                case "json":
                default:
                    // TODO: NOT IMPLEMENTED
            }
        }
        
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter())
        {
            ServletUtils.renderHeader(request, response, out);
            List<String> modes   = ServletUtils.generateRadioButtons("mode", List.of("user", "category"), request);
            List<String> formats = ServletUtils.generateRadioButtons("format", List.of("text", "zip"), request);
            out.printf("""
                <p>
                This tool generates a listing of a user's edits for use at <a
                href="//en.wikipedia.org/wiki/WP:CCI">Contributor copyright investigations</a>
                and other venues. It isolates and ranks major edits by size. A query limit of
                10000 edits (after namespace, date interval, new page, and minor edit filters) applies.

                <p>
                <form action="./contributionsurveyor.jsp" method=GET>
                <table>
                <tr>
                    <td>%s
                    <td><label for=user>User to survey:</label>
                    <td><input type=text name=user id=user value="%s" %s>
                <tr>
                    <td rowspan=2>%s
                    <td><label for=category>Fetch users from category:</label>
                    <td><input type=text name=category id=category value="%s" %s> (not recursive)
                <tr>
                    <td>Blocked after:
                    <td><input type=date name=blockedafter id=blockedafter value="%s" %s>
                <tr>
                    <td colspan=2>Home wiki:
                    <td><input type=text name="wiki" value="%s" required>
                <tr>
                    <td colspan=2>Exclude:
                    <td>%s
                        %s
                        %s
                        %s
                <tr>
                    <td colspan=2>Show changes from:
                    <td>%s
                <tr>
                    <td colspan=2>Show changes that added at least:
                    <td><input type=number name=bytefloor value="%s"> bytes
                <tr>
                    <td colspan=2>Output:
                    <td>%s
                <tr>
                    <td colspan=2>Output format:
                    <td>%s<label for=radio_format_text>Text</label>
                        %s<label for=radio_format_zip>Zip</label>
                </table>
                <input type=submit value="Survey user">
                </form>
                """, modes.get(0), ServletUtils.sanitizeForAttribute(un), 
                "user".equals(mode) ? "required" : "disabled", 
                modes.get(1), 
                ServletUtils.sanitizeForAttribute(category), 
                "category".equals(mode) ? "required" : "disabled", 
                ServletUtils.sanitizeForAttribute(blockedafterstr), 
                "category".equals(mode) ? "required" : "disabled", homewiki,
                ServletUtils.addCheckbox("nominor", survey == null || nominor, "minor edits"),
                ServletUtils.addCheckbox("noreverts", survey == null || noreverts, "reverts"),
                ServletUtils.addCheckbox("nodrafts", survey == null || nodrafts, "userspace and draft (ns 118) edits"),
                ServletUtils.addCheckbox("newonly", newonly, "all except new pages"),
                ServletUtils.addIntervalInputs(request, null, null), bytefloor,
                ServletUtils.addCheckbox("comingle", comingle, "comingled (for sockfarms where each user has few edits)"),
                formats.get(0), formats.get(1));
            if (surveyhtml != null)
                out.print(surveyhtml);
            ServletUtils.renderFooter(request, out);
        }
    }
}
