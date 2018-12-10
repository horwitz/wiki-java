# Wiki.java

[ ![Codeship Status for MER-C/wiki-java](https://codeship.com/projects/46dd6420-bb60-0132-1d73-5ea58638974e/status?branch=master)](https://codeship.com/projects/72144)
[![Build Status](https://travis-ci.org/MER-C/wiki-java.svg?branch=master)](https://travis-ci.org/MER-C/wiki-java?branch=master)
[![codecov.io](http://codecov.io/github/MER-C/wiki-java/coverage.svg?branch=master)](http://codecov.io/github/MER-C/wiki-java?branch=master)


A Java wiki bot framework that is only one file -- [org/wikipedia/Wiki.java](src/org/wikipedia/Wiki.java).
Some functionality provided by MediaWiki extensions deployed on Wikimedia sites
is available in [org/wikipedia/WMFWiki.java](src/org/wikipedia/WMFWiki.java). 
This project also contains the source code to the tools hosted at 
https://wikipediatools.appspot.com and other Wikipedia-related bits and pieces. 
Requires JDK >= 1.8. For those using Java 9, only the java.base and java.logging
modules are required.

Latest stable version: [0.35](https://github.com/MER-C/wiki-java/releases/tag/0.35) -- 
MediaWiki versions 1.31+

## Dependencies

| Class/Package            | Java          | MediaWiki extensions |
| ------------------------ |-------------- | -------------------- |
| org.wikipedia.Wiki       | None          | None                 |
| org.wikipedia.WMFWiki    | None          | As indicated. Works on WMF sites. |
| org.wikipedia.*Utils     | None          | None                 |
| org.wikipedia.tools.*    | None          | See below.           |
| org.wikipedia.servlets.* | javax.servlet | See below.           |

Note: Some tools and servlets are hardcoded to work on WMF sites only, and in
some cases for just the English Wikipedia. (Some tools solve en.wp specific
problems). They should all work on WMF sites. If you would like tool coverage 
for your wiki, please file a bug report.

## Bug reports

Bug reports may be filed in the Issue tracker or at [my talk page](https://en.wikipedia.org/wiki/User_talk:MER-C). 
Please read [this essay on filing bug reports effectively](http://www.chiark.greenend.org.uk/~sgtatham/bugs.html)
if you are not already familiar with its contents. Bugs regarding bot things
should have a short test case that demonstrates the problem. Before reporting 
character encoding problems, please display the output in a JOptionPane to 
isolate your development environment.

## Licenses

* The package [org.wikipedia.servlets](src/org/wikipedia/servlets) is licensed 
  under [AGPLv3+](COPYING.AGPL)
* Everything else: [GPLv3+](COPYING.GPL)

## Documentation

* [Javadoc](https://wikipediatools.appspot.com/doc/index.html)
* [Deploying these tools on your own server](https://wikipediatools.appspot.com/doc/org/wikipedia/servlets/package-summary.html)
* [Extended documentation](https://github.com/MER-C/wiki-java/wiki/Extended-documentation),
  including an example program
* See [the page on Wikipedia](https://en.wikipedia.org/wiki/User:MER-C/Wiki.java)
  for some old revision history.
