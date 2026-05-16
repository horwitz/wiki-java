/**
 *  @(#)UserInfo.java 0.02 11/01/2026
 *  Copyright (C) 2021-2026 MER-C and contributors
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
package org.wikipedia.tools;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import org.wikipedia.*;

/**
 *  Outputs a table of summary user information.
 *  @author MER-C
 *  @version 0.02
 */
public class UserInfo
{
    private static WMFWikiFarm sessions = WMFWikiFarm.instance();
    
    /**
     *  Information returned about users. 
     *  @param user username with links to user page and talk page
     *  @param actions link to logs
     *  @param regts registration timestamp
     *  @param firstedit timestamp of first edit (if the user has edited)
     *  @param lastedit timestamp of last edit (if the user has edited)
     *  @param editcount MediaWiki edit count with link to [[Special:Contibutions]]
     *  @param articles the number of articles created by this user that are still live
     *  @param groups the user groups the user belongs to, formatted
     *  @param blocked whether the user is blocked, with links to [[Special:Block]]
     *  and [[Special:Unblock]] if applicable
     *  @param blockexpiry when the block expires
     *  @param blockts when the user was blocked
     *  @param blockcomment why the user was blocked
     *  @param locked whether the user is locked (yes/no, not true/false)
     *  @see UserInfo#userInfoTable(Wiki, List, Writable.Format)
     *  @since 0.02
     */
    public record UserInfoRecord(String user, String actions, OffsetDateTime regts, OffsetDateTime firstedit,
        OffsetDateTime lastedit, String editcount, int articles, String groups, String blocked, String blockexpiry,
        OffsetDateTime blockts, String blockcomment, String locked) {}
    
    /**
     *  Runs this program.
     *  @param args the command line arguments
     *  @throws Exception if a network error occurs
     */
    public static void main(String[] args) throws Exception
    {
        Map<String, String> parsedargs = new CommandLineParser("org.wikipedia.tools.BlockLockStuff")
            .addVersion("0.02")
            .addSingleArgumentFlag("--wiki", "en.wikipedia.org", "Fetch socks from this wiki")
            .addUserInputOptions("Fetch sock info for")
            .addHelp()
            .parse(args);
        String wikistring = parsedargs.getOrDefault("--wiki", "en.wikipedia.org");
        sessions.setInitializer(wiki -> wiki.setUserAgent(WMFWikiFarm.TOOL_USER_AGENT));
        Wiki wiki = sessions.sharedSession(wikistring);
        List<String> socks = CommandLineParser.parseUserOptions(parsedargs, wiki);

        System.out.println(userInfoTable(wiki, socks, Writable.Format.WIKITEXT).format(Writable.Format.WIKITEXT));
        //lockFinder(socks);
        //staleScreener(wiki, socks);
        
        // TODO: a servlet and offline version overhauling the above.
        // options: show only unblocked, show only accounts < X old (default: 90 days)
        // highlight non-stale
        // pipe to other tools (e.g. ContributionSurveyor)
        // pipe from other tools (e.g. ArticleEditorIntersection)
    }
    
    /**
     *  Gets information about a list of users, fused from {@link Wiki.User} and
     *  their contributions, and presents it in one table.
     *  @param wiki the wiki to get information from
     *  @param usernames a list of usernames, non-existing allowed
     *  @param fmt {@link Writable.Format#WIKITEXT} or {@link Writable.Format#WIKITEXT}
     *  @return the user information
     *  @see UserInfoRecord
     *  @throws IOException if a network error occurs
     *  @since 0.02
     */
    public static DataTable<UserInfoRecord> userInfoTable(Wiki wiki, List<String> usernames, Writable.Format fmt) throws IOException
    {
        List<Wiki.User> users = wiki.getUsers(usernames);
        // somewhat slow, there could be tens of thousands of edits
        List<List<Wiki.Revision>> contribs = wiki.contribs(usernames, null, null);
        List<String> boring_groups = List.of("*", "user", "autoconfirmed");
        Wiki.LogEntry earliestblock = null;
        List<WMFWikiFarm.QuickGlobalUserInfo> qgui = sessions.getQuickGlobalUserInfo(usernames);
        
        List<String> headers = List.of("User", "Actions", "Reg. date", "First edit", "Last edit", 
            "Edit count", "Articles", "Groups", "Blocked?", "B. Expiry", "B. Timestamp", "B. Reason", "Locked?");
        List<UserInfoRecord> rows = new ArrayList<>();
        
        for (int i = 0; i < usernames.size(); i++)
        {
            Wiki.User user = users.get(i);
            if (user == null)
            {
                rows.add(new UserInfoRecord(usernames.get(i), "", null, null,
                    null, "0", 0, "unregistered", "", "", null, null, null));
                continue;
            }
            
            Wiki.LogEntry blockinfo = user.getBlockDetails();
            Map<String, String> blockdetails = null;
            OffsetDateTime blockts = null;
            if (blockinfo != null)
            {
                blockdetails = blockinfo.getDetails();
                blockts = blockinfo.getTimestamp();
                if (earliestblock == null || earliestblock.getTimestamp().isAfter(blockts))
                    earliestblock = blockinfo;
            }
            
            List<Wiki.Revision> usercontribs = contribs.get(i);
            usercontribs.sort((rev1, rev2) -> rev1.getTimestamp().isBefore(rev2.getTimestamp()) ? -1 : 1);
            int articles = 0;
            for (Wiki.Revision rev : usercontribs)
                if (rev.isNew() && wiki.namespace(rev.getTitle()) == Wiki.MAIN_NAMESPACE)
                    articles++;
            
            String un = user.getUsername();
            String unenc = URLEncoder.encode(un, StandardCharsets.UTF_8);
            
            String usercell = new WikitextUtils.WikiLink(wiki, "User:" + un, un).format(fmt) + " (" +
                new WikitextUtils.WikiLink(wiki, "User talk:" + un, "talk").format(fmt) + ")";
            String actioncell = new WikitextUtils.ExternalLink(wiki.getIndexPhpUrl() + "?title=Special:Log&user=" + unenc, "logs").format(fmt); // TODO: add more summary links/actions
            OffsetDateTime firstedit = null, lastedit = null;
            if (!usercontribs.isEmpty())
            {
                firstedit = usercontribs.get(0).getTimestamp();
                lastedit = usercontribs.get(usercontribs.size() - 1).getTimestamp();
            }
            String editcount = new WikitextUtils.WikiLink(wiki, "Special:Contributions/" + un, "" + user.countEdits()).format(fmt);

            List<String> groups = user.getGroups();
            String groupcell;
            groups.removeAll(boring_groups);
            if (groups.isEmpty())
                groupcell = "none (" + new WikitextUtils.WikiLink(wiki, "Special:Userrights/" + un, "add").format(fmt) + ")";
            else
                groupcell = new WikitextUtils.WikiLink(wiki, "Special:Userrights/" + un, String.join(", ", groups)).format(fmt);
            String blocked, bexpiry = null, bcomment = null;
            if (blockinfo == null)
                blocked = "No (" + new WikitextUtils.WikiLink(wiki, "Special:Block/" + un, "block").format(fmt) + ")";
            else
            {
                blocked = new WikitextUtils.WikiLink(wiki, "Special:Block/" + un, "Yes").format(fmt) + " (" + 
                    new WikitextUtils.WikiLink(wiki, "Special:Unblock/" + un, "unblock").format(fmt) + ")";
                bexpiry = blockdetails.get("expiry") == null ? "indefinite" : blockdetails.get("expiry");
                // force wikitext because parsedcomment is absent, see warning at Wiki.User.getBlockDetails()
                bcomment = new Events.Comment(blockinfo).format(Writable.Format.WIKITEXT);
            }
            
            WMFWikiFarm.QuickGlobalUserInfo qi = qgui.get(i);
            String locked = qi == null ? null : (qi.locked() ? "Yes" : "No");
            rows.add(new UserInfoRecord(usercell, actioncell, user.getRegistrationDate(), firstedit, lastedit, 
                editcount, articles, groupcell, blocked, bexpiry, blockts, bcomment, locked));
            
            // TODO: add lock timestamp and lock reason - not possible currently due to:
            // 1.The API call behind WMFWiki.getQuickGlobalUserInfo doesn't return when the lock occurred
            // 2. Wiki.getLogEntries("globalauth", null, null) doesn't return the details because it
            //    is not a native Wiki log type
            // Any will result in this bug being fixed.
            // Also the way formatting is handled leaves something to be desired
        }
        return DataTable.create(rows, headers);
        // once lock information is available, then this should be the minimum of block and lock timestamps
        //System.out.println("Earliest current block: " + earliestblock.getTitle() + 
        //    " at " + earliestblock.getTimestamp().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
    
    public static void lockFinder(List<String> socks) throws Exception
    {
        System.out.println("Not locked:");
        System.out.println("*{{MultiLock");
        List<WMFWikiFarm.QuickGlobalUserInfo> qgui = sessions.getQuickGlobalUserInfo(socks);
        for (WMFWikiFarm.QuickGlobalUserInfo info : qgui)
            if (info != null && info.locked())
                System.out.print("|" + info.username());
        System.out.println("}}\n\n");
    }
    
    public static void staleScreener(Wiki wiki, List<String> socks) throws Exception
    {
        // determine whether accounts are stale
        List<String> notstale = new ArrayList<>();
        List<String> stale = new ArrayList<>();
        List<List<Wiki.Revision>> contribs = wiki.contribs(socks, null, null);
        OffsetDateTime staledate = OffsetDateTime.now().minusDays(91);

        for (int i = 0; i < socks.size(); i++)
        {
            String sock = socks.get(i);
            String sock2 = wiki.removeNamespace(sock);
            Wiki.RequestHelper rh = wiki.new RequestHelper().byUser(sock);
            List<Wiki.LogEntry> socklogs = wiki.getLogEntries(Wiki.ALL_LOGS, null, rh);
            
            List<Wiki.Revision> sockcontribs = contribs.get(i);
            OffsetDateTime lastlog = socklogs.get(0).getTimestamp();
            OffsetDateTime lastactive = lastlog;
            if (!sockcontribs.isEmpty())
            {
                OffsetDateTime lastedit = sockcontribs.get(0).getTimestamp();
                if (lastedit.isAfter(lastlog))
                    lastactive = lastedit;
            }
            if (lastactive.isAfter(staledate))
                notstale.add("*{{checkuser|" + sock2 + "}}");
            else
                stale.add("*{{checkuser|" + sock2 + "}}");
        }
        System.out.println(";Not stale:");
        for (String s : notstale)
            System.out.println(s);
        System.out.println(";Probably stale:");
        for (String s : stale)
            System.out.println(s);
    }
}
