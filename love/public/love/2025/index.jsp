<%
// Main page that you hit when you visit https://love.riverbots.org, with a code in the URL (2025 version).
//
// A request of the form "https://love.riverbots.org/2025" without a code will redirect to landing.html.
if(request.getQueryString() == null) {
  response.sendRedirect("../index.jsp");
  return;
}

// Has the user interacted with the page yet?  If they have, no need to show the play box.
String isPoked = (session.getAttribute("poked") == null) ? "false" : "true";

// If we got here, then some code was added to the URL (e.g. "https://love.riverbots.org/0EM025").
// Load a simple container page and run the index_init function in love.js when the page is loaded.
%>
<html>
	<head>
		<title>Riverbots Love &trade;</title>
		<link rel="stylesheet" href="love.css" />
		<script src="https://code.jquery.com/jquery-3.6.3.min.js"></script>
		<script src="love.js"></script>
	</head>
	<body style="margin: 0px; padding: 0px;">
		<div id="laptop_box">
			<img id="laptop" src="love.png" alt="[Laptop]" />
		</div>
		<div id="term_box">
			<p id="message" />
		</div>
		<audio id="player" controls>
			<source id="audiosrc" src="" type="" />
		</audio>
		<div id="cover_box"><img id="play" src="play.gif" /></div>
	</body>
	<script>
		$(document).ready(index_init('<%=request.getQueryString()%>', <%=isPoked%>));
	</script>
</html>
