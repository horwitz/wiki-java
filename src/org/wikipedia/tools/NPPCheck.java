/**
 *  @(#)NPPCheck.java 0.02 07/02/2026
 *  Copyright (C) 2019 - 2026 MER-C
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

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import javax.security.auth.login.FailedLoginException;
import org.wikipedia.*;

/**
 *  Provides a listing of NPP article patrols and AFC acceptances for a given
 *  user with metadata.
 *  @author MER-C
 *  @version 0.02
 */
public class NPPCheck
{
    private WMFWiki wiki;
    private String reviewer;
    private Mode mode = Mode.PATROLS;

    /**
     *  An enum denoting what type of review to fetch.
     */
    public static enum Mode
    {
        /**
         *  Lists pages patrolled by a particular user.
         */
        PATROLS,

        /**
         *  Lists pages moved from draft space to mainspace by a particular 
         *  user. This includes the majority of [[WP:AFC]] acceptances.
         */
        DRAFTS,

        /**
         *  Lists pages moved from user space to mainspace by a particular 
         *  user. This may include a small number of [[WP:AFC]] acceptances.
         */
        USERSPACE,
        
        /**
         *  Lists unpatrolled pages (requires NPP rights).
         */
        UNPATROLLED,
        
        /**
         *  Lists articles created upon an existing redirect.
         */
        REDIRECTS;

        /**
         *  Parses a string into an instance of this enum.
         *  @param s "patrols", "drafts" or "userspace"
         *  @return the obvious value, or null if unrecognized
         */
        public static Mode fromString(String s)
        {
            return switch (s)
            {
                case "patrols" -> PATROLS;
                case "drafts" -> DRAFTS;
                case "userspace" -> USERSPACE;
                case "unpatrolled" -> UNPATROLLED;
                case "redirects" -> REDIRECTS;
                case null, default -> null;
            };
        }
        
        /**
         *  {@return {@code true} if this mode deals with drafts}
         */
        public boolean requiresDrafts()
        {
            return this == USERSPACE || this == DRAFTS;
        }
        
        /**
         *  {@return {@code true} if this mode deals with reviewed pages}
         */
        public boolean requiresReviews()
        {
            return this == PATROLS || this == DRAFTS || this == USERSPACE;
        }
    }
    
    /**
     *  A record representing the tabular output of this program.
     *  @param draft the name of the draft that was moved into the main namespace
     *  (if applicable)
     *  @param title the page that was reviewed
     *  @param createts the time at which the page was created
     *  @param reviewts the time at which the page was reviewed
     *  @param ageatreview how old the page was when it was reviewed
     *  @param betweenreviews the time since the reviewer's last review
     *  @param size the size of the page
     *  @param author the author of the page
     *  @param regts when the author registered their account
     *  @param authorec the edit count of the author
     *  @param blocked whether the author is blocked
     *  @param ageatcreation the age of the author's account at page creation
     *  @param reviewer the reviewer of the page
     *  @param reviewerec the edit count of the reviewer
     *  @param snippet a snippet from the page contents
     *  @since 0.02
     */
    public record NPPCheckResult(WikitextUtils.WikiLink draft, WikitextUtils.WikiLink title, OffsetDateTime createts, OffsetDateTime reviewts,
        Duration ageatreview, Duration betweenreviews, int size, Users.ShortLinks author, OffsetDateTime regts, int authorec, boolean blocked, 
        Duration ageatcreation, Users.ShortLinks reviewer, int reviewerec, String snippet) {}
    
    /**
     *  Runs this program.
     *  @param args args[0] is the username
     *  @throws IOException if a network error occurs
     */
    public static void main(String[] args) throws IOException
    {
        Map<String, String> parsedargs = new CommandLineParser("org.wikipedia.tools.NPPCheck")
            .synopsis("[options]")
            .description("Tool for reviewing the work of new page patrollers")
            .addBooleanFlag("--unpatrolled", "Output results for unpatrolled new articles (REQUIRES NPP RIGHTS)")
            .addBooleanFlag("--patrols", "Output results from new pages patrol")
            .addBooleanFlag("--userspace", "Output results for moves from user to main")
            .addBooleanFlag("--drafts", "Output results for moves from draft to main")
            .addBooleanFlag("--redirects", "Output results for expanded redirects")
            .addSingleArgumentFlag("--user", "user", "Output results for this user only "
                + "(requires one of --patrols, --userspace or --drafts)")
            .addSingleArgumentFlag("--start", "date", "Include patrols made after this date (ISO format).")
            .addSingleArgumentFlag("--end", "date", "Include patrols made before this date (ISO format).")
            .addVersion("0.02")
            .addHelp()
            .parse(args);
        String user = parsedargs.get("--user");
        Wiki.Interval dt = CommandLineParser.parseInterval(parsedargs, "--start", "--end");
        
        WMFWikiFarm sessions = WMFWikiFarm.instance();
        sessions.setInitializer(wiki -> wiki.setUserAgent(WMFWikiFarm.TOOL_USER_AGENT));
        WMFWiki enWiki = sessions.sharedSession("en.wikipedia.org");
        NPPCheck check = new NPPCheck(enWiki);
        
        // NPPBrowser mode (for bulk fetching)
        if (parsedargs.containsKey("--unpatrolled"))
        {
            Users.of(enWiki).cliLogin();
            check.setMode(Mode.UNPATROLLED);
            check.setReviewer(null);
            
            List<? extends Wiki.Event> le = check.fetchLogs(dt);
            System.out.println(check.outputTable(le).format(Writable.Format.WIKITEXT));
        }
        
        // patrol log
        if (parsedargs.containsKey("--patrols"))
        {
            check.setMode(Mode.PATROLS);
            check.setReviewer(user);
        
            List<? extends Wiki.Event> le = check.fetchLogs(dt);
            System.out.println("==NPP patrols ==");        
            if (le.isEmpty())
                System.out.println("No new pages patrolled.");
            else
                System.out.println(check.outputTable(le).format(Writable.Format.WIKITEXT));
        }
                
        // Pages moved from draft to main
        if (parsedargs.containsKey("--drafts"))
        {
            check.setMode(Mode.DRAFTS);
            check.setReviewer(user);
            
            List<? extends Wiki.Event> le = check.fetchLogs(dt);
            System.out.println("==Pages moved from draft to main ==");
            if (le.isEmpty())
                System.out.println("No pages moved from draft to main.");
            else
                System.out.println(check.outputTable(le).format(Writable.Format.WIKITEXT));
        }

        // Pages moved from user to main
        if (parsedargs.containsKey("--userspace"))
        {
            check.setMode(Mode.USERSPACE);
            check.setReviewer(user);
            
            List<? extends Wiki.Event> le = check.fetchLogs(dt);
            System.out.println("==Pages moved from user to main ==");
            if (le.isEmpty())
                System.out.println("No pages moved from user to main.");
            else
                System.out.println(check.outputTable(le).format(Writable.Format.WIKITEXT));
        }
        
        // Expanded redirects
        if (parsedargs.containsKey("--redirects"))
        {
            check.setMode(Mode.REDIRECTS);
            check.setReviewer(null);
            
            List<? extends Wiki.Event> le = check.fetchLogs(dt);
            System.out.println("==Expanded redirects ==");
            if (le.isEmpty())
                System.out.println("No expanded redirects.");
            else
                System.out.println(check.outputTable(le).format(Writable.Format.WIKITEXT));
        }

/* 
 *  EXPERIMENTAL - insert to get summary statistics for a given mode

        Pattern pattern = Pattern.compile("REDACTED", Pattern.CASE_INSENSITIVE);


            System.out.println("{| class=\"wikitable sortable\"");
            String header = "! Draft !! Title !! Accept timestamp !! Snippet";
            List<String> snippets = check.fetchSnippets(le);

            // stats
            Map<String, Integer> reviewhist = new HashMap<>();
            Map<String, Integer> delhist = new HashMap<>();
            Map<String, Integer> regexhist = new HashMap<>();
            int totaldeleted = 0, totalregex = 0;
            for (int i = 0; i < le.size(); i++)
            {
                String patroller = le.get(i).getUser();
                String snippet = snippets.get(i);

                int count = reviewhist.getOrDefault(patroller, Integer.valueOf(0));
                reviewhist.put(patroller, count + 1);
                if (snippet == null)
                {
                    count = delhist.getOrDefault(patroller, Integer.valueOf(0));
                    delhist.put(patroller, count + 1);
                    totaldeleted++;
                }
                else
                {
                    Matcher matcher = pattern.matcher(snippet);
                    if (matcher.find())
                    {
                        count = regexhist.getOrDefault(patroller, Integer.valueOf(0));
                        regexhist.put(patroller, count + 1);
                        totalregex++;
                    }
                }
            }
            
            System.out.println("{| class=\"wikitable sortable\"");
            System.out.println("! User !! Draft !! Article !! Review timestamp !! Snippet");
            for (int i = 0; i < le.size(); i++)
            {
                System.out.println("|-");
                Wiki.LogEntry log = le.get(i);
                String patroller = log.getUser();
                String snippet = snippets.get(i);
                String article = (String)log.getDetails();
                String draft = log.getTitle();
                OffsetDateTime timestamp = log.getTimestamp();
                System.out.printf("| {{user|%s}} || [[%s]] || [[%s]] || %s || %s %n", 
                    patroller, draft, article, timestamp.toString(), snippet);
            }
            System.out.println("|}");

            System.out.println("{| class=\"wikitable sortable\"");
            System.out.println("! User !! Reviews !! Fraction deleted !! Fraction regex match");
            for (Map.Entry<String, Integer> entry : reviewhist.entrySet())
            {
                String loguser = entry.getKey();
                int reviews = entry.getValue();
                int deleted = delhist.getOrDefault(loguser, Integer.valueOf(0));
                int regex = regexhist.getOrDefault(loguser, Integer.valueOf(0));
                System.out.println("|-");
                System.out.printf("| [[User:%s|%s]] || %d || %2.1f || %2.1f %n", loguser, 
                    loguser, reviews, 100. * deleted/reviews, 100. * regex/reviews);
            }
            System.out.println("|-");
            System.out.printf("| TOTAL || %d || %2.1f || %2.1f %n", le.size(), 100. * totaldeleted/le.size(), 100.*totalregex/le.size());
            System.out.println("|}");
*/
    }

    /**
     *  Binds an instance of this tool to a given wiki.
     *  @param wiki the wiki to bind to
     */
    public NPPCheck(WMFWiki wiki)
    {
        this.wiki = wiki;
    }
    
    /**
     *  Restricts results to only this reviewer. Set to null or empty for all 
     *  reviewers.
     *  @param reviewer (see above)
     */
    public void setReviewer(String reviewer)
    {
        if (reviewer == null || reviewer.isEmpty())
        {
            this.reviewer = null;
            wiki.setQueryLimit(250);
        }
        else
        {
            this.reviewer = reviewer;
            wiki.setQueryLimit(Integer.MAX_VALUE);
        }
    }
    
    /**
     *  {@return the user for which we are examining actions by, or {@code null} 
     *  if we are looking at all users}
     */
    public String getUser()
    {
        return reviewer;
    }
    
    /**
     *  Sets the type of new article to fetch.
     *  @param mode (see above)
     */
    public void setMode(Mode mode)
    {
        this.mode = mode;
    }

    /**
     *  Fetches events corresponding to new articles depending on mode. If
     *  mode is {@link Mode#UNPATROLLED}, the result is a list of {@link Wiki.Revision}
     *  output from {@link Wiki#newPages(RequestHelper)}. You must possess NPP
     *  rights. Otherwise the result is a list of {@link Wiki#MOVE_LOG} or 
     *  {@link Wiki#PATROL_LOG} {@link Wiki.LogEntry}.
     * 
     *  @param interval fetch events only between these timestamps
     *  @return (see above)
     *  @throws IOException if a network error occurs
     */
    public List<? extends Wiki.Event> fetchLogs(Wiki.Interval interval) throws IOException
    {
        Wiki.RequestHelper rh = wiki.new RequestHelper().withinInterval(interval);
        if (reviewer != null)
            rh = rh.byUser(reviewer);
        List<Wiki.LogEntry> le = Collections.emptyList();
        switch (mode)
        {
            case PATROLS:
                rh = rh.inNamespaces(Wiki.MAIN_NAMESPACE);
                return wiki.getLogEntries("patrol", "patrol", rh);
            case DRAFTS:
                rh = rh.inNamespaces(118);
                le = wiki.getLogEntries(Wiki.MOVE_LOG, "move", rh);
                break;
            case USERSPACE:
                rh = rh.inNamespaces(Wiki.USER_NAMESPACE);
                le = wiki.getLogEntries(Wiki.MOVE_LOG, "move", rh);
                break;
            case UNPATROLLED:
                rh = rh.inNamespaces(Wiki.MAIN_NAMESPACE)
                    .filterBy(Map.of("patrolled", Boolean.FALSE, "redirect", Boolean.FALSE));
                return wiki.newPages(rh);
            case REDIRECTS:
                return wiki.getAbuseLogEntries(new int[] { 342 }, rh);
                
        }
        return ArrayUtils.transformIf(le, ArrayList::new, Function.identity(), 
            log -> wiki.namespace(log.getDetails().get("target_title")) == Wiki.MAIN_NAMESPACE);
    }

    /**
     *  For each log entry, fetch a snippet of text from the article in its
     *  current state.
     *  @param events a bunch of events
     *  @return a list of snippets for those logs, with null indicating a deleted
     *  page
     *  @throws IOException if a network error occurs
     */
    public List<String> fetchSnippets(List<? extends Wiki.Event> events) throws IOException
    {
        List<String> pages = ArrayUtils.transform(events, ArrayList::new, event -> switch(event)
        {
            case Wiki.LogEntry log when log.getType().equals(Wiki.MOVE_LOG) -> log.getDetails().get("target_title");
            default -> event.getTitle();
        });
        
        // account for pages subsequently moved in namespace
        wiki.setResolveRedirects(true);
        List<String> pagetext = wiki.getLedeAsPlainText(pages);
        wiki.setResolveRedirects(false);
        return pagetext;
    }

    /**
     *  For each log entry, fetches metadata for the accepted/patrolled/unpatrolled
     *  article.
     *  @param events a list of events to examine
     *  @return the page metadata, augmented with key "logentry" representing 
     *  the input log entry.
     *  @throws IOException if a network error occurs
     */
    public List<Map<String, Object>> fetchMetadata(List<? extends Wiki.Event> events) throws IOException
    {
        // TODO: filter out pages that were redirects when patrolled
        
        List<String> pages = ArrayUtils.transform(events, ArrayList::new, event -> switch(event)
        {
            case Wiki.LogEntry log when log.getType().equals(Wiki.MOVE_LOG) -> log.getDetails().get("target_title");
            default -> event.getTitle();
        });
        
        // account for pages subsequently moved in namespace
        wiki.setResolveRedirects(true);
        List<Map<String, Object>> pageinfo = wiki.getPageInfo(pages);
        wiki.setResolveRedirects(false);
        
        for (int i = 0; i < pageinfo.size(); i++)
            pageinfo.get(i).put("logentry", events.get(i));
        return pageinfo;
    }

    /**
     *  Augments page metadata with information about the creator and first 
     *  revision. This is a slow query, so don't push too many items through it 
     *  in online mode.
     *  @param metadata the output from Wiki.getPageInfo
     *  @return the metadata, augmented with keys "creator" (Wiki.User) and 
     *  "firstrevision" (Wiki.Revision)
     *  @throws IOException if a network error occurs
     */
    public List<Map<String, Object>> fetchCreatorMetadata(List<Map<String, Object>> metadata) throws IOException
    {
        List<String> users = new ArrayList<>();
        for (int i = 0; i < metadata.size(); i++)
        {
            Map<String, Object> info = metadata.get(i);
            String title = (String)info.get("pagename");
            if (wiki.namespace(title) == Wiki.MAIN_NAMESPACE && (Boolean)info.get("exists"))
            {
                Wiki.Revision first = wiki.getFirstRevision(title);
                info.put("firstrevision", first);
                if (first != null && !first.getUser().contains(">")) // ContentTranslation ([[Bucket crusher]])
                    users.add(first.getUser());
                else
                    users.add("Example"); // dummy value
            }
            else
                users.add("Example");
        }

        // fetch info of creators
        List<Wiki.User> userinfo = wiki.getUsers(users);
        for (int i = 0; i < metadata.size(); i++)
            metadata.get(i).put("creator", userinfo.get(i));
        return metadata;        
    }
    
    /**
     *  Fetches data of reviewers if this is a run that isn't for a single
     *  user. Otherwise returns the empty list.
     *  @param events the events to fetch data for
     *  @return (see above)
     *  @throws IOException if a network error occurs
     */
    public List<Wiki.User> fetchReviewerData(List<? extends Wiki.Event> events) throws IOException
    {
        if (reviewer != null)
            return Collections.emptyList();
        List<String> usernames = ArrayUtils.transform(events, ArrayList::new, event -> event.getUser());
        return wiki.getUsers(usernames);
    }
    
    /**
     *  Returns the output of this tool in a format-independent table.
     *  @param le the events to analyse
     *  @return a DataTable representing the output of this tool
     *  @throws IOException if a network error occurs
     *  @see NPPCheck.NPPCheckResult
     */
    public DataTable<NPPCheckResult> outputTable(List<? extends Wiki.Event> le) throws IOException
    {
        List<Map<String, Object>> pageinfo = fetchMetadata(le);
        pageinfo = fetchCreatorMetadata(pageinfo);
        List<Duration> dt_patrol = le.size() > 1 ? Events.timeBetweenEvents(le) : new ArrayList<>();
        dt_patrol.add(Duration.ofSeconds(-1));
        List<Wiki.User> reviewerinfo = fetchReviewerData(le);
        List<String> snippets = fetchSnippets(le);
                
        List<NPPCheckResult> out = new ArrayList<>();
        for (int i = 0; i < pageinfo.size(); i++)
        {
            Map<String, Object> info = pageinfo.get(i);
            Wiki.Event entry = (Wiki.Event)info.get("logentry");
            Wiki.Event first = (Wiki.Event)info.get("firstrevision");
            
            OffsetDateTime patroldate = entry.getTimestamp();
            OffsetDateTime createdate = null;
            OffsetDateTime registrationdate = null;
            Duration dt_article = Duration.ofDays(-999999);
            Duration dt_account = Duration.ofDays(-999999);
            Duration dt_review = null;
            
            // author metadata (may be IP address, may be account so old its
            // creation date is null)
            String authorname = "null";
            Wiki.User creator = (Wiki.User)info.get("creator"); 
            int editcount = -1;
            Users.ShortLinks usl = null;
            int reviewerec = -1;
            boolean blocked = false;
            
            if (first != null)
            {
                authorname = first.getUser();
                createdate = first.getTimestamp();
                dt_article = Duration.between(createdate, patroldate);
                if (creator != null)
                {
                    editcount = creator.countEdits();
                    registrationdate = creator.getRegistrationDate();
                    if (registrationdate != null)
                        dt_account = Duration.between(registrationdate, createdate);
                    blocked = creator.getBlockDetails() != null;
                }
            }
            
            if (mode.requiresReviews())
            {
                if (reviewer == null)
                {
                    usl = new Users.ShortLinks(wiki, reviewerinfo.get(i).getUsername());
                    reviewerec = reviewerinfo.get(i).countEdits();
                }
                else
                    dt_review = dt_patrol.get(i);
            }
            
            out.add(new NPPCheckResult(
                mode.requiresDrafts() ? new WikitextUtils.WikiLink(wiki, le.get(i).getTitle(), null) : null, // draft title
                new WikitextUtils.WikiLink(wiki, (String)info.get("pagename"), null), // article title
                createdate,
                patroldate,
                dt_article,
                dt_review,
                (Integer)info.getOrDefault("size", -1),
                new Users.ShortLinks(wiki, authorname), registrationdate, editcount, blocked, dt_account, // author info
                usl, reviewerec, // reviewer info
                snippets.get(i)));
        }
        
        List<String> headers = List.of("Draft", "Title", "Create timestamp", "Review timestamp",
            "Age at review", "Time between reviews", "Size", "Author", "Author registration timestamp", 
            "Author edit count", "Author blocked", "Author age at creation", "Reviewer", "Reviewer edit count", 
            "Snippet");
        List<String> colclasses = new ArrayList<>();
        colclasses.addAll(List.of("title", "title", "date", "date"));
        colclasses.addAll(List.of("duration", "duration", "revsize", "user", "date"));
        colclasses.addAll(List.of("editcount", "boolean", "duration", "user", "editcount"));
        colclasses.add(null);
        DataTable<NPPCheckResult> table = DataTable.create(out, headers);
                
        List<String> skiphdrs = new ArrayList();
        if (!mode.requiresDrafts())
            skiphdrs.add("draft");
        if (mode.requiresReviews())
        {
            if (reviewer == null)
                skiphdrs.add("betweenreviews");
            else
                skiphdrs.addAll(List.of("reviewer", "reviewerec"));
        }
        else
            skiphdrs.addAll(List.of("reviewts", "ageatreview", "betweenreviews", "reviewer", "reviewerec"));
        table.setSkippedColumns(skiphdrs);
        table.setTableClass("wikitable");
        table.setRowClasses((i, s) -> "revision");
        table.setColumnClasses(colclasses);
        return table;
    }
}
