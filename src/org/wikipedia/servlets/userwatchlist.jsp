<!--
    @(#)userwatchlist.jsp 0.02 31/08/2017
    Copyright (C) 2015 - 20xx MER-C

    This is free software: you are free to change and redistribute it under the
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html>
    for details. There is NO WARRANTY, to the extent permitted by law.
-->
<%@ include file="security.jspf" %>
<%
    if (!ServletUtils.showCaptcha(request, response, List.of("page"), difficulty))
        throw new SkipPageException();
    request.setAttribute("toolname", "User watchlist");

    String inputpage = request.getParameter("page");
    String inputpage_url = "";
    String inputpage_attribute = "";
    if (inputpage != null)
    {
        inputpage_url = ServletUtils.sanitizeForURL(inputpage);
        inputpage_attribute = ServletUtils.sanitizeForAttribute(inputpage);
    }

    String temp = request.getParameter("offset");
    int skip = (temp == null) ? 0 : Integer.parseInt(temp);
    skip = Math.max(skip, 0);
    boolean newonly = (request.getParameter("newonly") != null);
    
    Wiki.Interval interval = ServletUtils.parseIntervalParams(request);
    
    Wiki enWiki = sessions.sharedSession("en.wikipedia.org");
    enWiki.setQueryLimit(20000); // 40 network requests
    Users userUtils = Users.of(enWiki);
    Revisions revisionUtils = Revisions.of(enWiki);
    Pages pageUtils = Pages.of(enWiki);
    
    ServletUtils.renderHeader(request, response, out);
%>

<p>
This tool retrieves contributions of a list of users. There is a limit of 50
users per request, though the list may be of indefinite length.

<p>
Syntax: one user per line, reason after # . Example:

<pre>
Example user # Copyright violations
// This is a comment
Someone # Spam
</pre>

<form action="./userwatchlist.jsp" method=GET>
<table>
<tr>
    <td>Input page or category:
    <td>
        <input type=text size=50 name=page required value="<%= inputpage_attribute %>">
        <%
        if (inputpage != null)
        {
            out.print("(" + pageUtils.generatePageLink(inputpage, "visit") + " &middot; ");
        %>
        <a href="<%= enWiki.getIndexPhpUrl() + "?action=edit&title=" + inputpage_url %>">edit</a>)
        <%
        }
        %>

<tr><td>Show changes from:
    <td><%= ServletUtils.addIntervalInputs(request, LocalDate.now(ZoneOffset.UTC).minusDays(30), null) %>
<tr><td>Show:
    <td><input type=checkbox name=newonly id="newonly" value=1<%= newonly ? " checked" : 
        "" %>>
        <label for="newonly">New pages only</label>
<tr><td>Skip:
    <td><input type=number size=50 name=skip value="<%= skip %>">
</table>
<input type=submit value="Submit">
</form>

<%
    if (inputpage == null || request.getAttribute("error") != null)
    {
        ServletUtils.renderFooter(request, out);
        throw new SkipPageException();
    }

    Map<String, String> input = new LinkedHashMap<>();
    if (enWiki.namespace(inputpage) == Wiki.CATEGORY_NAMESPACE)
    {
        List<String> catmembers = enWiki.getCategoryMembers(inputpage);
        for (String member : catmembers)
            input.put(enWiki.removeNamespace(member), "");
    }
    else if (inputpage.matches("^User:.+/.+\\.(cs|j)s$"))
    {
        String us = inputpage.substring(5, inputpage.indexOf('/'));
        Wiki.User us2 = enWiki.getUsers(List.of(us)).get(0);
        if (us2 == null || !us2.isA("sysop"))
        {
            request.setAttribute("error", "TESTING WOOP WOOP WOOP!");
            ServletUtils.renderFooter(request, out);
            throw new SkipPageException();
        }
        String text = enWiki.getPageText(List.of(inputpage)).get(0);
        if (text == null) // unreachable?
        {
            request.setAttribute("error", "ERROR: page &quot;" + HTMLUtils.sanitizeForHTML(inputpage) + "&quot; does not exist!");
            ServletUtils.renderFooter(request, out);
            throw new SkipPageException();
        }
        // parse input
        String[] lines = text.split("\n");

        for (String user : lines)
        {
            // remove comments, parse reasons
            user = user.trim();
            if (user.contains("//"))
                user = user.substring(0, user.indexOf("//")).trim();
            int boundary = user.indexOf("#");
            String reason = "";
            if (boundary >= 0)
            {
                reason = user.substring(boundary + 1).trim();
                user = user.substring(0, boundary).trim();
            }
            if (user.isEmpty())
                continue;
            input.put(user, reason);
        }
    }
    else
    {
        request.setAttribute("error", "TESTING WOOP WOOP WOOP!");
        ServletUtils.renderFooter(request, out);
        throw new SkipPageException();
    }

    if (input.isEmpty())
    {
        request.setAttribute("error", "ERROR: no users found!");
        ServletUtils.renderFooter(request, out);
        throw new SkipPageException();
    }

    // top pagination
    String requesturl = ServletUtils.getRequestURL(request);
    out.println("<hr>");
    out.println(ServletUtils.generatePagination(requesturl, skip, 50, input.size()));

    // fetch contributions
    Wiki.RequestHelper rh = enWiki.new RequestHelper().withinInterval(interval);
    if (newonly)
        rh = rh.filterBy(Map.of("new", Boolean.TRUE));
    List<String> users = new ArrayList<>(input.keySet());
    List<String> userstofetch = users.subList(skip, Math.min(skip + 50, users.size()));
    List<List<Wiki.Revision>> contribs = enWiki.contribs(userstofetch, null, rh);

    for (int i = 0; i < userstofetch.size(); i++)
    {
        String user = userstofetch.get(i);
        String reason = HTMLUtils.sanitizeForHTML(input.get(user));
        // user links
        %>
<h3><%= user %></h3>
<p>
<ul>
    <li>
        <%
        out.println(userUtils.generateHTMLSummaryLinks(user));
        if (!reason.isEmpty())
            out.println("<li><i>" + reason + "</i>");
        out.println("</ul>");

        // write contribs
        List<Wiki.Revision> usercontribs = contribs.get(i);
        if (usercontribs.isEmpty())
            out.println("<p>No contributions within interval or user does not exist.");
        else
            out.println(revisionUtils.toDataTable(usercontribs, "html").format(Writable.Format.HTML));
    }

    // end pagination
    out.println(ServletUtils.generatePagination(requesturl, skip, 50, input.size()));   
    ServletUtils.renderFooter(request, out);
%>
