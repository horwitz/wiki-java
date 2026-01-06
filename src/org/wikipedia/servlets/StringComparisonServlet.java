/**
 *  @(#)StringComparisonServlet.java 0.01 04/01/2026
 *  Copyright (C) 2026 MER-C
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
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.List;

import org.wikipedia.*;
import org.wikipedia.tools.StringSimilarityFinder;

/**
 *  A text passage comparison servlet. This is a web front end for
 *  {@link org.wikipedia.tools.StringSimilarityFinder}.
 *  @see <a href="https://wikipediatools.appspot.com/compare.jsp">Official instance</a>
 *  @author MER-C
 *  @version 0.01
 */
@WebServlet(name = "StringComparisonServlet", urlPatterns = {"/compare.jsp"})
public class StringComparisonServlet extends BaseServlet
{
    private static final String DEFAULT_MATCH_LENGTH = "6";
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        // 1. Parse parameters
        String wiki1 = request.getParameter("wiki1");
        String wiki2 = request.getParameter("wiki2");
        String page1 = request.getParameter("page1");
        String page2 = request.getParameter("page2");
        String text1 = request.getParameter("text1");
        String text2 = request.getParameter("text2");
        String minlength = request.getParameter("minlength");
        
        if (minlength == null || minlength.isEmpty())
            minlength = DEFAULT_MATCH_LENGTH;

        // 2. Perform business logic if submitted
        String comparisonResultHtml = null;
        if (wiki1 != null && page1 != null)
        {
            WMFWiki w = WMFWikiFarm.instance().sharedSession(wiki1);
            text1 = w.getPlainText(List.of(page1)).get(0);
        }
        if (wiki2 != null && page2 != null)
        {
            WMFWiki w = WMFWikiFarm.instance().sharedSession(wiki2);
            text2 = w.getPlainText(List.of(page2)).get(0);
        }
        if (text1 != null && text2 != null)
        {
            try
            {
                int minMatch = Integer.parseInt(minlength);

                StringSimilarityFinder finder = new StringSimilarityFinder();
                finder.setMinimumMatchLength(minMatch);

                List<StringSimilarityFinder.Match> matches = finder.findConsecutiveWordMatches(text1, text2);
                comparisonResultHtml = finder.generateHtmlHighlight(text1, text2, matches);
            } 
            catch (NumberFormatException e)
            {
                request.setAttribute("error", "Invalid number format for Minimum Match Length.");
            } 
            catch (Exception e)
            {
                request.setAttribute("error", "An error occurred during comparison: " + e.getMessage());
                e.printStackTrace();
            }
        }
        else if (text1 != null)
            request.setAttribute("error", "Page " + HTMLUtils.sanitizeForHTML(page1) + " does not exist.");
        else if (text2 != null)
            request.setAttribute("error", "Page " + HTMLUtils.sanitizeForHTML(page2) + " does not exist.");

        // 3. Render output        
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter())
        {
            request.setAttribute("toolname", "Passage Comparison");
            request.setAttribute("scripts", new String[] { "common.js", "StringComparison.js" });
            ServletUtils.renderHeader(request, response, out);
            
            out.printf("""
                <p>
                This tool compares two passages for groups of common words.

                <form method="post" action="compare.jsp">
                <h4>Text 1</h4>
                <table>
                <tr>
                    <td><input type=radio name=mode1 id="radio_wikipage1" checked>
                    <td><label for=page1>Wiki page:</label>
                    <td><input type=text name=wiki1 id=wiki1 value="%s" required>/wiki/
                        <input type=text name=page1 id=page1 value="%s" required>
                <tr>
                    <td><input type=radio name=mode1 id="radio_text1">
                    <td><label for=text1>Enter text:</label>
                    <td>
                <textarea name=text1 id=text1 placeholder="Enter first text here..." rows=5 required disabled>
                %s
                </textarea>
                </table>
                <h4>Text 2</h4>
                <table>
                <tr>        
                    <td><input type=radio name=mode2 id="radio_wikipage2" checked>
                    <td><label for=radio_wikipage2>Wiki page:</label>
                    <td><input type=text name=wiki2 id=wiki2 value="%s" required>/wiki/
                        <input type=text name=page2 id=page2 value="%s" required>
                <tr>
                    <td><input type=radio name=mode2 id="radio_text2">
                    <td><label for=text2>Enter text:</label>
                    <td>
                <textarea name=text2 id=text2 placeholder="Enter second text here..." rows=5 required disabled>
                %s
                </textarea>
                </table>
                <h4>Settings</h4>
                <label for="minlength">Minimum words for match:</label>
                <input type=number id="minlength" name=minlength min=2 value=%s>
                <br>
                <br>
                <input type=submit value="Compare texts">
                </form>
                """, 
                ServletUtils.sanitizeForAttributeOrDefault(wiki1, "en.wikipedia.org"), 
                ServletUtils.sanitizeForAttribute(page1), 
                HTMLUtils.sanitizeForHTML(text1), 
                ServletUtils.sanitizeForAttributeOrDefault(wiki2, "en.wikipedia.org"),
                ServletUtils.sanitizeForAttribute(page2), 
                HTMLUtils.sanitizeForHTML(text2), 
                ServletUtils.sanitizeForAttribute(minlength));

            // results section
            if (comparisonResultHtml != null)
            {
                out.println("""
                    <hr>
                    %s
                    """.formatted(comparisonResultHtml));
            }
            ServletUtils.renderFooter(request, out);
        }
    }
}