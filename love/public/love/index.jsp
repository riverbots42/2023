<%
if(request.getQueryString() == null) {
  response.sendRedirect("landing.html");
}
%>
<html>
	<head>
		<title>Riverbots Love &trade;</title>
		<link rel="stylesheet" href="love.css" />
		<script src="https://code.jquery.com/jquery-3.6.3.min.js"></script>
		<script src="https://cdn.jsdelivr.net/combine/npm/tone@14.7.58,npm/@magenta/music@1.23.1/es6/core.js,npm/focus-visible@5,npm/html-midi-player@1.5.0"></script>
		<script src="love.js"></script>
	</head>
	<body style="margin: 0px; padding: 0px;">
		<div id="outerbox">
			<div class="termbox" id="content">
				<p id="message" />
			</div>
			<div class="termbox" id="fuzzer" />
				<midi-player id="player" src="love.mid" sound-font />
			</div>
			<div id="coverbox"><img id="play" src="play.png" /></div>
		</div>
	</body>
	<script>
		$(document).ready(index_init('<%=request.getQueryString()%>'));
	</script>
</html>
