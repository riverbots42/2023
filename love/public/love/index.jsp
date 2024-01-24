<%
// Main page that you hit when you visit https://love.riverbots.org, with or without a code in the URL.
//
// A request of the form "https://love.riverbots.org/" without a code will redirect to landing.html.
if(request.getQueryString() == null) {
  response.sendRedirect("landing.html");
  return;
}

// If we got here, then some code was added to the URL (e.g. "https://love.riverbots.org/0EM0").
// Grab the year info and redirect to the final page.
%>
<html>
	<head>
		<title>Riverbots Love &trade;</title>
		<link rel="stylesheet" href="love.css" />
		<script src="https://code.jquery.com/jquery-3.6.3.min.js"></script>
		<script src="love.js"></script>
	</head>
	<body>
		<div id="throb"><img src="landing.gif" alt="[Heart]" /></div>
	</body>
	<script>
		
	</script>
</html>
