# Troubleshooting
- [Purpose](#purpose)
- [Logs](#logs)
  - [Locations](#locations)
  - [Reading Them](#reading-them)
  - [Common issues](#common-issues)
- [Submit an issue](#submit-an-issue)

## Purpose
This document describes basic troubleshooting steps for mxisd.

## Logs
### Locations
mxisd logs to `STDOUT` (Standard Output) and `STDERR` (Standard Error) only, which gets redirected
to log file(s) depending on your system.

If you use the [Debian package](install/debian.md), this goes to `syslog`.  
If you use the [Docker image](install/docker.md), this goes to the container logs.  

For any other platform, please refer to your package maintainer.

### Reading them
Before reporting an issue, it is important to produce clean and complete logs so they can be understood.

It is usually useless to try to troubleshoot an issue based on a single log line. Any action or API request
in mxisd would trigger more than one log lines, and those would be considered necessary context to
understand what happened.

You may also find things called *stacktraces*. Those are important to pin-point bugs and the likes and should
always be included in any report. They also tend to be very specific about the issue at hand.

Example of a stacktrace:
```
Exception in thread "main" java.lang.NullPointerException
        at com.example.myproject.Book.getTitle(Book.java:16)
        at com.example.myproject.Author.getBookTitles(Author.java:25)
        at com.example.myproject.Bootstrap.main(Bootstrap.java:14)
```

### Common issues
#### Internal Server Error
`Contact your administrator with reference Transaction #123456789`

This is a generic message produced in case of an unknown error. The transaction reference allows to easily find
the location in the logs to look for an error.

**IMPORTANT:** That line alone does not tell you anything about the error. You'll need the log lines before and after,
usually including a stacktrace, to know what happened. Please take the time to read the surround output to get
context about the issue at hand.

## Submit an issue
In case the logs do not allow you to understand the issue at hand, please submit clean and complete logs
as explained [here](#reading-them) in a new issue on the repository, or [get in touch](../README.md#contact).
