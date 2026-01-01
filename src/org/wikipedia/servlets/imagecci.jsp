<!--
    @(#)imagecci.jsp 0.03 07/02/2018
    Copyright (C) 2011 - 2022 MER-C

    This is free software: you are free to change and redistribute it under the 
    Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
    for details. There is NO WARRANTY, to the extent permitted by law.
-->
<%@ include file="security.jspf" %>
<%@ include file="datevalidate.jspf" %>
<%
    if (!ServletUtils.showCaptcha(request, response, List.of("user"), difficulty))
        throw new SkipPageException();
        
    request.setAttribute("toolname", "Image contribution surveyor");
    String homewiki = ServletUtils.sanitizeForAttributeOrDefault(request.getParameter("wiki"), "en.wikipedia.org");
    String user = request.getParameter("user");
    String output = "text";
    if (user != null)
        request.setAttribute("contenttype", output); 
    boolean transferred = (request.getParameter("transferred") != null);

    ServletUtils.renderHeader(request, response, out);
    
    List<String> survey = null;
    if (user != null)
    {
        WMFWiki wiki = sessions.sharedSession(homewiki);
        ContributionSurveyor surveyor = new ContributionSurveyor(wiki);
        surveyor.setInterval(interval);
        surveyor.setFooter("Survey URL: " + ServletUtils.getRequestURL(request));
        surveyor.setSurveyingTransferredFiles(transferred);
        survey = surveyor.outputContributionSurvey(List.of(user), false, false, true);

        if (output.equals("text"))
        {
            response.setHeader("Content-Disposition", "attachment; filename=" 
                + URLEncoder.encode(user, StandardCharsets.UTF_8) + ".txt");
            out.print(String.join("\n", survey));
            return;
        }
        else if (output.equals("zip"))
        {
            // ERROR: out already called, so cannot use getOutputStream().
            // therefore servlet rewrite required
            response.setHeader("Content-Disposition", "attachment; filename=" 
                + URLEncoder.encode(user, StandardCharsets.UTF_8) + ".zip");
            Map<String, byte[]> zip = new LinkedHashMap<>();
            for (int i = 0; i < survey.size(); i++)
                zip.put(user + (i == 0 ? "" : ".%03d".formatted(i)), survey.get(i).getBytes());
            try (ZipOutputStream zout = new ZipOutputStream(response.getOutputStream()))
            {
                ContributionSurveyor.outputZipFile(zout, zip);
                return;
            }
        }
    }
%>

<p>
This tool generates a listing of a user's image uploads for use at <a
href="//en.wikipedia.org/wiki/WP:CCI">Contributor copyright investigations.</a>

<p>
<form action="./imagecci.jsp" method=GET>
<table>
<tr>
    <td>User to survey:
    <td><input type=text name=user value="<%= ServletUtils.sanitizeForAttribute(user) %>" required>
<tr>
    <td>Home wiki:
    <td><input type=text name="wiki" value="<%= homewiki %>" required>
<tr>
    <td>Include uploads from:
    <td><%= ServletUtils.addIntervalInputs(request, null, null) %>
</table>
<input type=checkbox name=transferred value="<%= transferred ? " checked" : "" %>">Include transferred files 
    (may be inaccurate depending on username)
<br>
<input type=submit value="Survey user">
</form>

<%
    if (user != null && survey.isEmpty()) // currently unreachable?
        request.setAttribute("error", "ERROR: User " + HTMLUtils.sanitizeForHTML(user) + " does not exist!");
    ServletUtils.renderFooter(request, out);
%>