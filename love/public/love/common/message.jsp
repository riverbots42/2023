<%@ page import="java.io.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<%!
// Functions to handle getting a message from the database.

// The literal string "sender"
final String SENDER = "sender";
// The literal string "recipient"
final String RECIPIENT = "recipient";
// The literal string "body"
final String BODY = "body";
// The literal string "year"
final String YEAR = "year";
// The literal string "error"
final String ERROR = "error";

// Fetch a message from the database.
//
// IN: code: the code passed in by the user.
// IN: propsfile: absolute path to the properties file in use.
// OUT: a TreeMap of string,string that has the various result fields as strings or "error": "message"
public TreeMap<String, String> GetMessage(String code, String propsfile) {
	TreeMap<String, String> ret = new TreeMap<String, String>();
	try {
		// Grab the credentials to connect to the database as a Properties object.
		Properties props = new Properties();
		props.load(new FileInputStream(propsfile));
		// Connect to the database.
		Class.forName("org.mariadb.jdbc.Driver");
		Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/love", "love", props.getProperty("password"));
		// Construct the SQL string to get the message sender/recipient/body
		PreparedStatement stmt = con.prepareStatement("select sender, recipient, body, year from message where code=?");
		stmt.setString(1, code);
		ResultSet result = stmt.executeQuery();
		result.next();
		// OK, if we got here and didn't error out, then we have one valid message from the database.
		// Construct (and spit out) the JSON blob.
		ret.put(SENDER, result.getString(1));
		ret.put(RECIPIENT, result.getString(2));
		ret.put(BODY, result.getString(3));
		ret.put(YEAR, result.getString(4));
	} catch(Exception E) {
		ret.put(ERROR, E.toString());
	}
	return ret;
}
%>
