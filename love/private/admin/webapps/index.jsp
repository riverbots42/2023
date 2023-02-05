<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.nio.charset.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang3.*" %>
<%!
// Figure out whether to print an input box with stuff in it, what
// size, etc etc.
public String inputfield(String fieldname, String content, String action) {
    int fieldlength=40;
    if(fieldname.equals("code")) {
        if(!content.equals("")) {
            return "<input type=\"hidden\" name=\"code\" value=\"" + content + "\" />" + content;
        } else {
            return "";
        }
    } else if(fieldname.equals("message")) {
        fieldlength = 128;
    }
    if( action != null && action.equals("delete") ) {
        return content;
    } else {
        return "<input type=\"text\" name=\"" + fieldname + "\" size=\"" + fieldlength + "\" value=\"" + content + "\"/>";
    }
}
%>
<%!
public String generatecode() {
    Random rand = new Random();
    String chars = "0123456789ACEGHKMNPRTUWXY";
    String ret = "";
    int check = 0;
    for( int i=0; i<5; i++ ) {
        char c = chars.charAt(rand.nextInt(chars.length()));
        ret += c;
        check += (int) c;
    }
    ret += (char)((char)(check % 10) + '0');
    return ret;
}
%>
<%
Properties props = new Properties();
props.load(new FileInputStream(request.getRealPath("META-INF/love.properties")));
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
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setHeader("WWW-Authenticate", "basic");
    return;
}
String targetcode = request.getParameter("code");
if( targetcode == null ) {
    targetcode = "";
}
String action = request.getParameter("action");
Class.forName("com.mysql.jdbc.Driver");
Connection con=DriverManager.getConnection("jdbc:mysql://10.68.0.1:3306/love", "love", props.getProperty("password"));
String targetsender = "";
String targetrecipient = "";
String targetmessage = "";
if( targetcode.equals("") ) {
    String sender = request.getParameter("sender");
    String recipient = request.getParameter("recipient");
    String message = request.getParameter("message");
    if( sender != null && !sender.equals("") && recipient != null && !recipient.equals("") && message != null && !message.equals("") ) {
        // We have all the info we need to INSERT.
        String code = generatecode();
        PreparedStatement stmt = con.prepareStatement("insert into message (code, sender, recipient, body) values (?, ?, ?, ?)");
        stmt.setString(1, code);
        stmt.setString(2, sender);
        stmt.setString(3, recipient);
        stmt.setString(4, message);
        stmt.execute();
        response.sendRedirect("index.jsp");
        return;
    }
} else {
    if( action != null && action.equals("really") ) {
        PreparedStatement stmt = con.prepareStatement("delete from message where code=?");
        stmt.setString(1, targetcode);
        stmt.execute();
        response.sendRedirect("index.jsp");
        return;
    }
    String sender = request.getParameter("sender");
    String recipient = request.getParameter("recipient");
    String message = request.getParameter("message");
    if( sender != null && !sender.equals("") && recipient != null && !recipient.equals("") && message != null && !message.equals("") ) {
        // We have all the info we need to UPDATE.
        PreparedStatement stmt = con.prepareStatement("update message set sender=?, recipient=?, body=? where code=?");
        stmt.setString(1, sender);
        stmt.setString(2, recipient);
        stmt.setString(3, message);
        stmt.setString(4, targetcode);
        stmt.execute();
        response.sendRedirect("index.jsp");
        return;
    } else {
        PreparedStatement stmt = con.prepareStatement("select sender, recipient, body from message where code=?");
        stmt.setString(1, targetcode);
        ResultSet result = stmt.executeQuery();
        result.next();
        targetsender = result.getString(1);
        targetrecipient = result.getString(2);
        targetmessage = result.getString(3);
    }
}
PreparedStatement stmt = con.prepareStatement("select code, sender, recipient, body from message order by code");
ResultSet result = stmt.executeQuery();
%>
<html>
    <head>
        <title>Riverbots Lovebot&trade; Administration UI</title>
        <link rel="stylesheet" href="love.css" />
    </head>
    <body>
        <h1>Edit Message</h1>
        <!-- START Edit One Message -->
        <table>
            <form method="post" action="index.jsp">
<% if(!targetcode.equals("")) { %>
                <tr>
                    <th>Code</th>
                    <td><%=inputfield("code", targetcode, action)%></td>
                </tr>
<% } // (ends if(!targetcode.equals(""))) %>
                <tr>
                    <th>Sender</th>
                    <td><%=inputfield("sender", targetsender, action)%></td>
                </tr>
                <tr>
                    <th>Recipient</th>
                    <td><%=inputfield("recipient", targetrecipient, action)%></td>
                </tr>
                <tr>
                    <th>Message</th>
                    <td><%=inputfield("message", targetmessage, action)%></td>
                </tr>
                <tr>
<% if(action != null && action.equals("delete")) { %>
                    <td colspan="2"><input type="hidden" name="action" value="really" />Really Delete? <input type="submit" value="Yes" /> <button onclick="window.location.href=index.jsp";>No</button></td>
<% } else { %>
                    <td colspan="2"><input type="submit" value="Save" /></td>
<% } %>
                </tr>
            </form>
        </table>
        <!-- END Edit One Message -->
        <!-- START List of All Messages -->
        <table>
            <tr>
                <th>Code</th>
                <th>Sender</th>
                <th>Recipient</th>
                <th>Message</th>
                <th>Actions</th>
            </tr>
<%
while(result.next()) {
    String code = StringEscapeUtils.escapeXml(result.getString(1));
    if( !code.equals(targetcode) ) {
        String sender = StringEscapeUtils.escapeXml(result.getString(2));
        String recipient = StringEscapeUtils.escapeXml(result.getString(3));
        String message = StringEscapeUtils.escapeXml(result.getString(4));
%>
            <tr>
                <td><%=code%></td>
                <td><%=sender%></td>
                <td><%=recipient%></td>
                <td><%=message%></td>
                <td><a href="index.jsp?code=<%=code%>&action=edit"><img src="edit.png" alt="[Edit]" /></a><a href="index.jsp?code=<%=code%>&action=delete"><img src="delete.png" alt="[Delete]" /></a></td>
            </tr>
<%
    }
}
%>
        </table>
        <!-- END List of All Messages -->
    </body>
</html>
