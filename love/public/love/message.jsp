<%@ page language="java" contentType="application/json; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.json.simple.*" %>
<%
	Properties props = new Properties();
	props.load(new FileInputStream(request.getRealPath("META-INF/love.properties")));
	Thread.sleep(1000);
	String code = request.getParameter("code");
	Class.forName("com.mysql.jdbc.Driver");
	Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/love", "love", props.getProperty("password"));
	PreparedStatement stmt = con.prepareStatement("select sender, recipient, body from message where code=?");
	stmt.setString(1, code);
	ResultSet result = stmt.executeQuery();
	result.next();
	JSONObject o = new JSONObject();
	o.put("sender", result.getString(1));
	o.put("recipient", result.getString(2));
	o.put("body", result.getString(3));
	out.print(o.toJSONString());
%>
