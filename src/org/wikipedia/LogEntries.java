/**
 *  @(#)LogEntries.java 0.02 29/03/2026
 *  Copyright (C) 2024-2026 MER-C and contributors
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

package org.wikipedia;

import java.time.OffsetDateTime;
import java.util.*;

/**
 *  Utility class for {@link Wiki.LogEntry}. For utility methods and data specific 
 *  to {@link Wiki.Revision}s, see {@link Revisions}.
 *  @author MER-C
 *  @version 0.02
 */
public class LogEntries
{
    /**
     *  A representation of a {@link Wiki.LogEntry} for output in tabular format.
     *  @param domain the domain of the wiki this log occurred in
     *  @param timestamp when the relevant action occurred
     *  @param user a formatted rendering of the user performing the action
     *  @param log the log type (e.g. {@link Wiki#DELETION_LOG})
     *  @param action the action that was performed
     *  @param title the title of the relevant page
     *  @param comment the log comment
     *  @param details any extra log information, see {@link Wiki.LogEntry#getDetails()}
     *  @see DataTable
     *  @see Wiki.LogEntry
     *  @since 0.02
     */
    public record LogRecord(String domain, OffsetDateTime timestamp, Users.ShortLinks user, String log, String action, 
        WikitextUtils.WikiLink title, Events.Comment comment, String details) { }
    
    /**
     *  Turns a list of revisions into a format-independent table. 
     *  @param logs a bunch of log entries
     *  @return those log entries formatted as a DataTable
     */
    public static DataTable<LogRecord> toDataTable(Iterable<Wiki.LogEntry> logs)
    {
        List<LogRecord> rows = new ArrayList<>();
        for (Wiki.LogEntry log : logs)
        {
            String user = log.getUser();
            Wiki wiki = log.getWiki();
            Map details = log.getDetails();
            rows.add(new LogRecord(
                wiki.getDomain(),
                log.getTimestamp(),
                new Users.ShortLinks(wiki, user == null || user.equals(Wiki.Event.USER_DELETED) ? null : user), 
                log.getType(),
                log.getAction(),
                new WikitextUtils.WikiLink(wiki, log.getTitle(), null),
                new Events.Comment(log),
                (details == null || details.isEmpty()) ? "" : details.toString()));
        }
        DataTable dt = DataTable.create(rows, null);
        dt.setHeaders(List.of("Domain", "Timestamp", "User", "Log", "Action", "Target", "Comment", "Details"));
        dt.setColumnClasses(List.of("", "date", "user", "", "", "title", "comment", ""));
        return dt;
    }
}