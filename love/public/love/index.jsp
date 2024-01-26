<%@include file="common/message.jsp" %>
<%
// The main page, where we look to see if anything's in the query string (i.e. after the "?..." in the URL) to see if we
// jump straight to a specific page.
//
// Note that this is now somewhat modular and the docs for it are in README.md as far as adding 2025/6/7/8... goes.

// A request of the form "https://love.riverbots.org/" without a code will redirect to landing.html.
if(request.getQueryString() == null) {
	session.setAttribute("poked", "yes");
	response.sendRedirect("landing.html");
	return;
}

// If we got here, then some code was added to the URL (e.g. "https://love.riverbots.org/0EM0").
// Grab the year info and redirect to the final page.

TreeMap<String, String> message = GetMessage(request.getQueryString(), request.getRealPath("META-INF/love.properties"));

if(message.containsKey(ERROR)) {
	session.setAttribute("poked", "yes");
%>
<html>
	<head>
		<title>Error Retrieving Message</title>
	</head>
	<body>
		<p>I got error &quot;<%=message.get(ERROR).toString()%>&quot; when fetching your lovebot.  Please go <a href="/">here</a> and try again.</p>
	<body>
</html>
<%
} else if(message.containsKey(YEAR)) {
	response.sendRedirect(message.get(YEAR).toString() + "/?" + request.getQueryString());
} else {
	session.setAttribute("poked", "yes");
%>
<html>
	<head>
		<title>Error Retrieving Message</title>
	</head>
	<body>
		<p>I couldn't find your lovebot.  Please go <a href="/">here</a> and try again.</p>
	<body>
</html>
<%
}
%>
