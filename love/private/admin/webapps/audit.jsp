<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.nio.charset.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang3.*" %>
<%
// BEGIN PAGE CONTENT.

// Start by loading the password that we're expecting to get from the user
// (and with which we'll be connecting to the database).
Properties props = new Properties();
props.load(new FileInputStream(request.getRealPath("/etc/love.properties")));

// Now make sure the user has authorization to load this page.  If there's
// no auth, send an HTTP 401 and require a password.
final String authorization = request.getHeader("Authorization");
String username = "";
String password = "";
if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
    // Authorization: Basic base64credentials
    String base64Credentials = authorization.substring("Basic".length()).trim();
    byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
    String credentials = new String(credDecoded, StandardCharsets.UTF_8);
    // credentials = username:password
    final String[] values = credentials.split(":", 2);
    username = values[0];
    password = values[1];
}
if(password == "" || !props.getProperty("password").equals(password)) {
    response.setHeader("WWW-Authenticate", "basic");
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    return;
}

// OK, so for the last bit of setup, we grab a database connection.  Note that
// we don't get here if authentication is wrong...
Class.forName("com.mysql.jdbc.Driver");
Connection conn=DriverManager.getConnection("jdbc:mysql://10.68.0.1:3306/love", "love", props.getProperty("password"));
%>
<html>
    <head>
        <title>Riverbots Lovebot&trade; Administration UI: Audit Log</title>
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/css/bootstrap.min.css" integrity="sha384-HSMxcRTRxnN+Bdg0JdbxYKrThecOKuH5zCYotlSAcp1+c8xmyTe9GYg1l9a69psu" crossorigin="anonymous">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/css/bootstrap-theme.min.css" integrity="sha384-6pzBo3FDv/PJ8r2KRkGHifhEocL+1X2rVCTTkUfGk7/0pbek5mMa1upzvWbrUbOZ" crossorigin="anonymous">
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/js/bootstrap.min.js" integrity="sha384-aJ21OjlMXNL5UyIl/XNwTMqvzeRMZH2w8c5cRVpzpU8Y5bApTppSuUkhZXN0VxHd" crossorigin="anonymous"></script>
    </head>
    <body>
        <h1>Audit Log</h1>
        <!-- START List of All Messages -->
        <table class="table">
            <tr>
                <th scope="col">Timestamp</th>
                <th scope="col">User</th>
                <th scope="col">Action</th>
                <th scope="col">Code</th>
                <th scope="col">Sender</th>
                <th scope="col">Recipient</th>
                <th scope="col">Message</th>
                <th scope="col">Paid</th>
            </tr>
<%
PreparedStatement stmt = conn.prepareStatement("SELECT ts, user, action, code, sender, recipient, body, paid FROM audit ORDER BY ts DESC");
ResultSet result = stmt.executeQuery();
while(result.next()) {
    String ts = StringEscapeUtils.escapeXml(result.getString(1));
    String user = StringEscapeUtils.escapeXml(result.getString(2));
    String action = StringEscapeUtils.escapeXml(result.getString(3));
    String code = StringEscapeUtils.escapeXml(result.getString(4));
    String sender = StringEscapeUtils.escapeXml(result.getString(5));
    String recipient = StringEscapeUtils.escapeXml(result.getString(6));
    String body = StringEscapeUtils.escapeXml(result.getString(7));
    String paid = StringEscapeUtils.escapeXml(result.getString(8));
%>
            <tr>
                <td><%=ts%></td>
                <td><%=user%></td>
                <td><%=action%></td>
                <td><%=code%></td>
                <td><%=sender%></td>
                <td><%=recipient%></td>
                <td><%=body%></td>
                <td><%=paid%></td>
            </tr>
<%
}
result.close();
%>
        </table>
        <!-- END List of All Messages -->
    </body>
</html>
<%
// END PAGE CONTENT
%>
