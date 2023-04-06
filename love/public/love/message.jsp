<%
// Simple page to load the message from the database and return it as a JSON blob.
%/
<%@ page language="java" contentType="application/json; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.json.simple.*" %>
<%
	// Grab the credentials to connect to the database as a Properties object.
	Properties props = new Properties();
	props.load(new FileInputStream(request.getRealPath("META-INF/love.properties")));
	Thread.sleep(1000);
	// Grab the code from the query string (e.g. https://love.riverbots.org/message.jsp?code=0EM0)
	String code = request.getParameter("code");
	// Connect to the database.
	Class.forName("com.mysql.jdbc.Driver");
	Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/love", "love", props.getProperty("password"));
	// Construct the SQL string to get the message sender/recipient/body
	PreparedStatement stmt = con.prepareStatement("select sender, recipient, body from message where code=?");
	stmt.setString(1, code);
	ResultSet result = stmt.executeQuery();
	result.next();
	// OK, if we got here and didn't error out, then we have one valid message from the database.
	// Construct (and spit out) the JSON blob.
	JSONObject o = new JSONObject();
	o.put("sender", result.getString(1));
	o.put("recipient", result.getString(2));
	o.put("body", result.getString(3));
	out.print(o.toJSONString());
%>
