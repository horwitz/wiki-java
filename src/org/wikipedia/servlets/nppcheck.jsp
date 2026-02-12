<!--
    @(#)nppcheck.jsp 0.02 07/02/2026
    Copyright (C) 2019 - 2026 MER-C
  
    This is free software: you are free to change and redistribute it under the 
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
    for details. There is NO WARRANTY, to the extent permitted by law.
-->
<%@ include file="security.jspf" %>
<%
    if (!ServletUtils.showCaptcha(request, response, List.of("username"), difficulty))
        throw new SkipPageException();

    request.setAttribute("toolname", "NPP/AFC checker");
    String username = ServletUtils.sanitizeForAttribute(request.getParameter("username"));
    NPPCheck.Mode mode = NPPCheck.Mode.fromString(request.getParameter("mode"));
    String offsetparam = Objects.requireNonNullElse(request.getParameter("offset"), "0");
    Wiki.Interval interval = ServletUtils.parseIntervalParams(request);
    
    ServletUtils.renderHeader(request, response, out);
%>

<p>
This tool retrieves recent new page patrols and moves from draft/user space to 
main space for a given user (or for all users) and page metadata. A query limit of 7500 log entries applies.

<form action="./nppcheck.jsp" method=GET>
<table>
<tr>
    <td>Username (leave blank for all):
    <td><input type=text name=username value="<%= username %>">
<tr>
    <td>Fetch:
    <td><input type=radio name=mode id="patrols" value="patrols" <%= mode == NPPCheck.Mode.PATROLS ? " checked" : "" %>>
        <label for="patrols">New page patrols</label>
        <input type=radio name=mode id="drafts" value="drafts" <%= mode == NPPCheck.Mode.DRAFTS ? " checked" : "" %>>
        <label for="drafts">Moves from Draft to Main</label>
        <input type=radio name=mode id="userspace" value="userspace" <%= mode == NPPCheck.Mode.USERSPACE ? " checked" : "" %>>
        <label for="userspace">Moves from User to Main</label>
        <input type=radio name=mode id="redirects" value="redirects" <%= mode == NPPCheck.Mode.REDIRECTS ? " checked" : "" %>>
        <label for="redirects">Redirects converted to articles</label>
<tr>
    <td>Show patrols from:
    <td><%= ServletUtils.addIntervalInputs(request, null, null) %>
</table>
<input type=hidden name=offset value="<%= offsetparam %>">
<input type=submit value="Search">
</form>

<%
    if (mode == null || request.getAttribute("error") != null)
    {
        ServletUtils.renderFooter(request, out);
        throw new SkipPageException();
    }
    out.println("<hr>");

    WMFWiki enWiki = sessions.sharedSession("en.wikipedia.org");
    Pages pageutils = Pages.of(enWiki);
    enWiki.setQueryLimit(7500);
    NPPCheck check = new NPPCheck(enWiki);
    check.setReviewer(username);
    check.setMode(mode);
    List<? extends Wiki.Event> logs = check.fetchLogs(interval);
    
    if (logs.isEmpty())
    {
        out.println("<p>No results found!");
        ServletUtils.renderFooter(request, out);
        throw new SkipPageException();
    }

    // limit to 50 articles per page
    int offset = Integer.parseInt(offsetparam);
    List<? extends Wiki.Event> logsub = logs.subList(offset, Math.min(logs.size(), offset + 51));
    String requesturl = ServletUtils.getRequestURL(request);
    out.println(ServletUtils.generatePagination(requesturl, offset, 50, logs.size()));
    out.println(check.outputTable(logsub).format(Writable.Format.HTML));
    out.println(ServletUtils.generatePagination(requesturl, offset, 50, logs.size()));
    ServletUtils.renderFooter(request, out);
%>
