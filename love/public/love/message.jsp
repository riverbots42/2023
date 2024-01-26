<%
// Simple page to load the message from the database and return it as a JSON blob.
%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="org.json.simple.*" %>
<%@include file="common/message.jsp" %>
<%
	TreeMap<String, String> message = GetMessage(request.getParameter("code"), request.getRealPath("META-INF/love.properties"));
	JSONObject o = new JSONObject();
	Iterator<String> i = message.keySet().iterator();
	while(i.hasNext()) {
		String key = i.next();
		String val = message.get(key);
		o.put(key, val);
	}
	out.print(o.toJSONString());
%>
