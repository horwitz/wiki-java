/**
 *  @(#)ContributionSurveyorUnitTest.java 0.04 25/01/2018
 *  Copyright (C) 2011-20xx MER-C
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
package org.wikipedia.tools;

import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.wikipedia.*;

/**
 *  Unit tests for {@link ContributionSurveyor}.
 *  @author MER-C
 */
public class ContributionSurveyorTest 
{
    private final Wiki enWiki;
    private final ContributionSurveyor surveyor;
    
    /**
     *  Constructs a tool object and wiki connection for every test so that 
     *  tests are independent.
     */
    public ContributionSurveyorTest()
    {
        enWiki = Wiki.newSession("en.wikipedia.org");
        enWiki.setMaxLag(-1);
        surveyor = new ContributionSurveyor(enWiki);
    }
    
    @Test
    public void makeContributionSurveyor()
    {
        Map<String, String> args = new HashMap<>();
        args.put("--editsafter", "1970-01-01T00:00:03Z");
        args.put("--editsbefore", "2020-01-01T00:00:04Z");
        args.put("--newonly", "x");
        args.put("--minsize", "159");
        args.put("--includeminor", "x");
        args.put("--includereverts", "x");
        args.put("--comingle", "x");
        
        ContributionSurveyor cs = ContributionSurveyor.makeContributionSurveyor(enWiki, args);
        assertEquals(enWiki.getDomain(), cs.getWiki().getDomain());
        assertEquals(Wiki.Interval.parse("1970-01-01T00:00:03Z", "2020-01-01T00:00:04Z"), cs.getInterval());
        assertTrue(cs.newOnly());
        assertEquals(159, cs.getMinimumSizeDiff());
        assertFalse(cs.isIgnoringMinorEdits());
        assertFalse(cs.isIgnoringReverts());
        assertTrue(cs.isComingled());
    }

    @Test
    public void getWiki()
    {
        assertEquals("en.wikipedia.org", surveyor.getWiki().getDomain());
    }
    
    @Test
    public void contributionSurvey() throws Exception
    {
        // https://en.wikipedia.org/wiki/Special:Contributions/HilStev               - no edits
        // https://en.wikipedia.org/wiki/Special:Contributions/OfficialPankajPatidar - no mainspace edits
        // https://en.wikipedia.org/wiki/Special:Contributions/Rt11642               - mainspace edits all revisiondeleted
        List<String> users = List.of("HilStev", "OfficialPankajPatidar", "Rt11642");
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty(), "User with no edits");
        assertTrue(results.get(users.get(1)).isEmpty(), "Check namespace filter");
        assertTrue(results.get(users.get(2)).isEmpty(), "Check revision deletion");
    }
    
    @Test
    public void imageContributionSurvey() throws Exception
    {
        // https://meta.wikimedia.org/wiki/Special:CentralAuth/Helga_The_Great_Kyiv - one deleted upload
        // https://meta.wikimedia.org/wiki/Special:CentralAuth/Lozouhg - one image, PD text (so probably not going away)
        // note search for transferred uploads is not stable enough for testing
        List<String> users = List.of("Helga The Great Kyiv", "Lozouhg");
        List<ContributionSurveyor.ImageContributions> results = surveyor.imageContributionSurvey(users);
        assertTrue(results.get(0).local().isEmpty(), "User with no uploads (local)");
        assertTrue(results.get(0).mediarepo().isEmpty(), "User with no uploads (commons)");
        assertTrue(results.get(1).local().isEmpty(), "User with only commons uploads");
        assertEquals(List.of("File:Infinum logo.jpg"), results.get(1).mediarepo());
    }
    
    @Test
    public void renderTextSurveyLine() throws Exception
    {
        Wiki.RequestHelper rh = enWiki.new RequestHelper().limitedTo(2)
            .withinInterval(new Wiki.Interval(null, OffsetDateTime.parse("2026-04-07T00:25:07Z")));
        List<Wiki.Revision> revisions = enWiki.contribs("RickyCourtney", rh);
        ContributionSurveyor.TextSurveyLine tsl = new ContributionSurveyor.TextSurveyLine(revisions);
        assertEquals("[[Artemis II]] (2 edits): [[Special:Diff/1347482853|(+927)]][[Special:Diff/1347481236|(+421)]]", 
            tsl.format(Writable.Format.WIKITEXT), "wikitext, two edits");
        assertEquals("<a href=\"https://en.wikipedia.org/wiki/Artemis_II\">Artemis II</a> (2 edits): "
            + "<a href=\"https://en.wikipedia.org/wiki/Special%3ADiff%2F1347482853\">(+927)</a>"
            + "<a href=\"https://en.wikipedia.org/wiki/Special%3ADiff%2F1347481236\">(+421)</a>", tsl.format(Writable.Format.HTML), "html, two edits");
        
        rh = rh.limitedTo(1).withinInterval(new Wiki.Interval(null, OffsetDateTime.parse("2026-04-11T16:35:04Z")));
        revisions = enWiki.contribs("Esculenta", rh);
        tsl = new ContributionSurveyor.TextSurveyLine(revisions);
        assertEquals("'''N''' [[Synarthonia psoromica]] (1 edit): [[Special:Diff/1348256309|(+4492)]]", 
            tsl.format(Writable.Format.WIKITEXT), "wikitext, one edit, new");
        assertEquals("<b>N</b> <a href=\"https://en.wikipedia.org/wiki/Synarthonia_psoromica\">Synarthonia psoromica</a> (1 edit): "
            + "<a href=\"https://en.wikipedia.org/wiki/Special%3ADiff%2F1348256309\">(+4492)</a>", tsl.format(Writable.Format.HTML), "html, one edit new");
    }
    
    @Test
    public void outputContributionSurvey() throws Exception
    {
        // same use case as above: all three users have no surveyable edits
        List<String> users = List.of("HilStev", "OfficialPankajPatidar", "Rt11642");
        List<String> results = surveyor.outputContributionSurvey(users, true, false, false, Wiki.MAIN_NAMESPACE);
        assertTrue(results.isEmpty());
    }
    
    @Test
    public void setInterval() throws Exception
    {        
        Wiki.Interval interval = Wiki.Interval.parse("2017-12-07T00:00:00Z", "2018-01-23T00:00:00Z");
        surveyor.setInterval(interval);
        assertEquals(interval, surveyor.getInterval(), "verify get/set");
        
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&contribs=user&target=Jimbo+Wales&namespace=0&start=2017-12-01&end=2018-01-24
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&contribs=user&target=Jimbo+Wales&namespace=0&start=2017-12-07&end=2018-01-17
        List<String> users = List.of("Jimbo Wales");
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty(), "Check interval functionality (text)");
        
        // images
        users = List.of("Lozouhg");
        var results2 = surveyor.imageContributionSurvey(users);
        assertTrue(results2.get(0).mediarepo().isEmpty(), "Check interval functionality (images)");
    }
    
    @Test
    public void setIgnoreMinorEdits() throws Exception
    {
        assertTrue(surveyor.isIgnoringMinorEdits(), "minor edits are ignored by default"); 

        // https://en.wikipedia.org/wiki/Special:Contributions/Jjdevine2
        List<String> users = List.of("Jjdevine2");
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty());
        
        surveyor.setIgnoringMinorEdits(false);
        assertFalse(surveyor.isIgnoringMinorEdits(), "verify get/set"); 
        
        // check functionality
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals(2, results.get(users.get(0)).size(), "User with nearly only minor edits");
    }
    
    @Test
    public void setIgnoringReverts() throws Exception
    {
        assertTrue(surveyor.isIgnoringReverts(), "reverts are ignored by default"); 
        
        // https://en.wikipedia.org/w/index.php?title=Special:Contributions&dir=prev&offset=20191109040135&target=Dl2000
        List<String> users = List.of("Dl2000");
        surveyor.setIgnoringMinorEdits(false);
        surveyor.setInterval(Wiki.Interval.parse("2019-11-09T16:00:00Z", "2019-11-09T16:21:00Z"));
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty(), "rollbacks with tag mw-rollback");
        
        surveyor.setIgnoringReverts(false);
        assertFalse(surveyor.isIgnoringReverts(), "verify get/set");
        
        // check functionality
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals(1, results.get(users.get(0)).size());
        
        // reverts with tag mw-manual-revert
        // https://en.wikipedia.org/w/index.php?title=Special:Contributions&offset=20200808093000&target=SouthAfricanCitizen
        users = List.of("SouthAfricanCitizen");
        surveyor.setMinimumSizeDiff(0);
        surveyor.setInterval(Wiki.Interval.parse("2020-08-08T09:00:00Z", "2020-08-08T09:30:00Z"));
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals(1, results.get(users.get(0)).size());
        surveyor.setIgnoringReverts(true);
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty());
    }
    
    @Test
    public void setMinimumSizeDiff() throws Exception
    {
        assertEquals(150, surveyor.getMinimumSizeDiff(), "default is an addition of at least 150 bytes");
        
        // https://en.wikipedia.org/wiki/Special:Contributions/Cyprumande
        List<String> users = List.of("Cyprumande");
        surveyor.setInterval(Wiki.Interval.parse("2019-01-01T00:00:00Z", null));
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertTrue(results.get(users.get(0)).isEmpty());
        
        surveyor.setMinimumSizeDiff(0);
        assertEquals(0, surveyor.getMinimumSizeDiff(), "verify get/set");
        
        // check functionality
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals(1, results.get(users.get(0)).size());
    }
    
    @Test
    public void setNewOnly() throws Exception
    {
        assertFalse(surveyor.newOnly(), "default is false");
        
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&target=GarciaB&start=2005-03-14&end=2005-03-15
        List<String> users = List.of("GarciaB");
        surveyor.setInterval(Wiki.Interval.parse("2005-03-14T00:00:00Z", "2005-03-15T00:00:00Z"));
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        Map<String, List<Wiki.Revision>> results2 = results.get(users.get(0));
        assertEquals(2, results2.size());
        assertTrue(results2.keySet().containsAll(List.of("Akan people", "Lists of volcanoes")));
        
        surveyor.setNewOnly(true);
        assertTrue(surveyor.newOnly(), "verify get/set");
        
        // check functionality
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        results2 = results.get(users.get(0));
        assertEquals(1, results2.size());
        assertTrue(results2.containsKey("Akan people"));
    }
    
    @Test
    public void setComingled() throws Exception
    {
        assertFalse(surveyor.isComingled(), "default is false");
        
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&target=Dhouston45&start=2022-07-06&end=2022-07-07
        // https://en.wikipedia.org/w/index.php?title=Special%3AContributions&target=Dhouston17&start=2022-07-06&end=2022-07-07
        List<String> users = List.of("Dhouston17", "Dhouston45");
        surveyor.setInterval(Wiki.Interval.parse("2022-07-06T00:00:00Z", "2022-07-07T00:00:00Z"));
        var results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals(2, results.size());
        assertTrue(results.keySet().containsAll(users));
        Map<String, List<Wiki.Revision>> results2 = results.get(users.get(0));
        assertEquals(1, results2.size());
        assertTrue(results2.containsKey("NHL on ESPN2"));
        results2 = results.get(users.get(1));
        assertTrue(results2.containsKey("NHL on ESPN"));
        assertTrue(results2.containsKey("NHL on ESPN2"));
        
        surveyor.setComingled(true);
        assertTrue(surveyor.isComingled(), "verify get/set");
        
        // check functionality
        results = surveyor.contributionSurvey(users, Wiki.MAIN_NAMESPACE);
        assertEquals(1, results.size());
        results2 = results.get("");
        assertEquals(2, results2.size());
        assertTrue(results2.containsKey("NHL on ESPN"));
        assertTrue(results2.containsKey("NHL on ESPN2"));
    }
    
    @Test
    public void setSurveyingTransferredFiles() throws Exception
    {
        assertFalse(surveyor.isSurveyingTransferredFiles(), "default is false");
        
        // https://en.wikipedia.org/wiki/Special:CentralAuth/Pochta
        // no uploads but pochta (Почта) is Russian for post, which means this
        // is one of the cases where transferred files do not work well
        String un = "Pochta";
        List<String> users = List.of(un);
        assertTrue(surveyor.imageContributionSurvey(users).get(0).transferred().isEmpty());
        
        surveyor.setSurveyingTransferredFiles(true);
        assertTrue(surveyor.isSurveyingTransferredFiles(), "verify get/set");
        
        assertFalse(surveyor.imageContributionSurvey(users).get(0).transferred().isEmpty());
    }
    
    @Test
    public void setFooter()
    {
        String f = "Test123";
        surveyor.setFooter(f);
        assertTrue(surveyor.generateFooter(Writable.Format.HTML).endsWith(f));
        assertTrue(surveyor.generateFooter(Writable.Format.WIKITEXT).endsWith(f));
    }
}
