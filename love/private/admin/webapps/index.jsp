<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.nio.charset.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang3.*" %>
<%!
// BEGIN SHARED FUNCTION DEFININTIONS

/**
 * Function declarations for the main admin page.  The general set of
 * stuff on the page is:
 *
 * 1. Authentication, to make sure that the user is logged in.
 * 2. Database connection.
 * 3. The main form, which is either an insert form (empty), an
 *    update form (filled), or a delete form (filled but not editable).
 * 4. The list of all the messages we know about.
**/

/**
 * Create one form item, either a normal text field with labels and
 * decorations, or a hidden one that's display-only and not editable.
 *
 * IN: name:        the form variable name to give this field.
 * IN: value:       the current value of this field.  Can be null.
 * IN: length:      the length of the text field (<=0 means it'll be a
 *                  hidden field).
 * IN: label:       the MixedCaseWithSpaces label to put on this field
 *                  that's  human-readable.
 * IN: placeholder: the default value of this field to hint to the human.
 * IN: helptext:    the small text underneath the field to give help to
 *                  the human.
 *
 * OUT: the resulting form HTML content.
**/
public String makeItem(String name, String value, int length, String label, String placeholder, String helptext) {
    StringWriter out = new StringWriter();
    PrintWriter ret = new PrintWriter(out);
    ret.printf("  <div class=\"form-group\">\n");
    ret.printf("    <label for=\"%s\">%s</label>\n", name, label);
    if( length <= 0 ) {
        // This is a hidden field.  Create a hidden entry with a read-only display of the value.
        ret.printf("<input type=\"hidden\" id=\"%s\" name=\"%s\" value=\"%s\" />%s\n", name, name, value, value);
    } else {
        // ThimakeIs is a visible field.  Create the whole shebang--label, helptext, et al.
        if( value == null || value.equals("") ) {
            ret.printf("    <input type=\"text\" class=\"form-control\" id=\"%s\" name=\"%s\" size=\"%d\" aria-describedby=\"%shelp\" placeholder=\"%s\">\n",
                       name, name, length, name, placeholder);
        } else {
            ret.printf("    <input type=\"text\" class=\"form-control\" id=\"%s\" name=\"%s\" size=\"%d\" aria-describedby=\"%shelp\" value=\"%s\" placeholder=\"%s\">\n",
                       name, name, length, name, value, placeholder);
        }
        ret.printf("    <small id=\"%shelp\" class=\"form-text text-muted\">%s</small>\n", name, helptext);
    }
    ret.printf("  </div>\n");
    ret.close();
    return out.toString();
}

/**
 * Create a form.
 *
 * This creates the HTML snippet corresponding to the form, including the
 * passed-in fields, which themselves are HTML snippets.
 *
 * IN: name:       the name of the form.
 * IN: buttontext: what text to put on the submit button (human-readable).
 * IN: fields:     the list of fields to input (presumably from calls to
 *                 makeItem() above).
 *
 * OUT: the resulting form HTML content.
**/
public String form(String name, String buttontext, String fields[]) {
    StringWriter out = new StringWriter();
    PrintWriter ret = new PrintWriter(out);
    ret.printf("<!-- BEGIN form %s -->\n", name);
    ret.printf("<form id=\"form\">\n");
    for( int i=0; i<fields.length; i++ ) {
        ret.print(fields[i]);
    }
    ret.printf("  <button type=\"submit\" class=\"btn btn-primary\">%s</button>\n", buttontext);
    ret.printf("</form>\n");
    ret.printf("<!-- END form %s -->\n", name);
    ret.close();
    return out.toString();
}

/**
 * Create a blank form with empty items.
 *
 * IN: nothing
 *
 * OUT: the resulting form HTML content.
**/
public String blankForm() {
    String items[] = new String[4];
    items[0] = makeItem("sender", null, 50, "Sender", "Joe Sender", "The person who ordered this message.");
    items[1] = makeItem("recipient", null, 50, "Recipient", "Sally Recipient", "The person who should be receiving this message");
    items[2] = makeItem("body", null, 200, "Message", "Happy Valentine's Day", "The message that's being sent.");
    items[3] = makeItem("notes", null, 200, "Notes", "Room/Class Notes", "Any notes for this message delivery (not printed).");
    return form("blank", "Create", items);
}

/**
 * Create a prefilled form for updating an existing record.
 *
 * IN: conn: The Connection to use to fetch the record.
 * IN: code: The code id for this particular message to edit.
 *
 * OUT: the resulting form HTML content.
**/
public String editForm(Connection conn, String code) {
    String csrbn[] = doSelect(conn, code);
    if(csrbn == null) {
        return "<p>Error: Couldn't get record for code " + code + "</p>\n";
    }
    String items[] = new String[5];
    items[0] = makeItem("code", csrbn[0], 0, "Code", null, null);
    items[1] = makeItem("sender", csrbn[1], 50, "Sender", "Joe Sender", "The person who ordered this message.");
    items[2] = makeItem("recipient", csrbn[2], 50, "Recipient", "Sally Recipient", "The person who should be receiving this message");
    items[3] = makeItem("body", csrbn[3], 200, "Message", "Happy Valentine's Day", "The message that's being sent.");
    items[4] = makeItem("notes", csrbn[4], 200, "Notes", "Delivery notes", "Room/Class Delivery notes (not printed).");
    return form("update", "Update", items);
}

/**
 * Create a form to confirm that we want to delete the record before we
 * actually do it.  The form will not have any editable fields and the
 * submit button will say 'Delete.'
 *
 * IN: conn: The Connection to use to fetch the record.
 * IN: code: The code id for this particular message on which to confirm
 *           deletion.
 *
 * OUT: the resulting form HTML content.
**/
public String confirmForm(Connection conn, String code) {
    String csrbn[] = doSelect(conn, code);
    if(csrbn == null) {
        return "<p>Error: Couldn't get record for code " + code + "</p>\n";
    }
    String items[] = new String[6];
    items[0] = makeItem("code", csrbn[0], 0, "Code", null, null);
    items[1] = makeItem("sender", csrbn[1], 0, "Sender", null, null);
    items[2] = makeItem("recipient", csrbn[2], 0, "Recipient", null, null);
    items[3] = makeItem("body", csrbn[3], 0, "Message", null, null);
    items[4] = makeItem("body", csrbn[4], 0, "Notes", null, null);
    items[5] = "<input type=\"hidden\" name=\"delete\" value=\"confirm\">";
    return form("delete confirm", "Delete", items);
}

/**
 * Generate a random 5-digit base25 number with a 6th check digit.  This code
 * is specifically picked to be easy for humans to not mess up when
 * transcribing by hand (so "0" is the same as "D" or "O" or "Q", e.g.).
 *
 * OUT: the 6-char randomized code.
**/
public String generateCode() {
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

/**
 * Create an entry in the audit table.
 *
 * IN: conn:      The Connection to use.
 * IN: username:  The user that made the change.
 * IN: action:    The action (insert/update/delete) the user performed.
 * IN: code:      The id code for the particular message.
 * IN: sender:    The sender of the message after the change.
 * IN: recipient: The receipient of the message after the change.
 * IN: body:      The message send after the change.
 * IN: notes:     Notes to the entry (not printed).
 *
 * OUT: nothing
 *
 * Side effect: Could throw a SQLException.
**/
public void audit(Connection conn, String username, String action, String code, String sender, String recipient, String body, String notes) throws SQLException {
    PreparedStatement stmt = conn.prepareStatement( "INSERT INTO audit (user, action, code, sender, recipient, body, notes) VALUES (?, ?, ?, ?, ?, ?, ?)" );
    stmt.setString(1, username);
    stmt.setString(2, action);
    stmt.setString(3, code);
    stmt.setString(4, sender);
    stmt.setString(5, recipient);
    stmt.setString(6, body);
    stmt.setString(7, notes);
    stmt.executeUpdate();
}

/**
 * Select the code/sender/recipient/body of a message based on a passed-in code.
 *
 * IN: response: the Response from JSP to use if we have to puke.
 * IN: conn:     the database Connection to use.
 * IN: code:     the code to look up.
 *
 * OUT: An array of strings returning the list [code, sender, recipient, body].
 *
 * Side effect: Could throw SQLException.
**/
public String[] doSelect(Connection conn, String code) {
    PreparedStatement stmt;
    ResultSet rs;
    String ret[] = new String[5];
    try {
        stmt = conn.prepareStatement("SELECT code, sender, recipient, body, notes from message where code=?");
        stmt.setString(1, code);
        rs = stmt.executeQuery();
        rs.next();
        ret[0] = rs.getString(1);
        ret[1] = rs.getString(2);
        ret[2] = rs.getString(3);
        ret[3] = rs.getString(4);
        ret[4] = rs.getString(5);
        rs.close();
    } catch(SQLException e) {
        return null;
    }
    return ret;
}

/**
 * Insert a message into the database.  Requires us to generate a code first.
 *
 * IN: response:  The Response object from the JSP (so we can send redirects).
 * IN: conn:      The Connection to use.
 * IN: username:  The user who made the insert.
 * IN: sender:    The sender of this message.
 * IN: recipient: The recipient of this message.
 * IN: body:      The body of this message.
 *
 * OUT: nothing
 *
 * Side Effects: redirects the page to edit the message just inserted, adds an
 *               entry to the audit table.  Could throw SQLException.
**/
public void doInsert(HttpServletResponse response, Connection conn, String username, String sender, String recipient, String body, String notes) throws IOException {
    String code = null;
    int attempts_remaining = 10;
    // Since we *could* potentially get duplicate codes, we retry a bunch of
    // times, just in case.
    while(attempts_remaining > 0 && code == null) {
        try {
            PreparedStatement stmt = conn.prepareStatement( "INSERT INTO message (code, sender, recipient, body, notes) VALUES (?, ?, ?, ?, ?)" );
            code = generateCode();
            stmt.setString(1, code);
            stmt.setString(2, sender);
            stmt.setString(3, recipient);
            stmt.setString(4, body);
            stmt.setString(5, notes);
            stmt.executeUpdate();
        } catch(SQLException e) {
            attempts_remaining--;
            code = null;
        }
    }
    if(attempts_remaining > 0) {
        // We ran OK, so add an audit entry and redirect to the item we just
        // created.
        try {
            audit(conn, username, "insert", code, sender, recipient, body, notes);
        } catch(SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't write to audit table.");
            return;
        }
        response.sendRedirect("/?code=" + code);
        return;
    }
    // If we got here, almost-for-sure there is a legit DB problem.
    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ran out of attempts to insert a record!");
}

/**
 * Update an existing record in the DB.
 *
 * IN: response:  The Response object from the JSP (so we can send redirects).
 * IN: conn:      The Connection to use.
 * IN: username:  The user who made the insert.
 * IN: code:      The code for the message to be updated.
 * IN: sender:    The sender of this message.
 * IN: recipient: The recipient of this message.
 * IN: body:      The body of this message.
 * IN: notes:     The notes assigned to this message.
 *
 * OUT: nothing
 *
 * Side Effects: redirects the page to he message just inserted, adds an
 *               entry to the audit table.  Could throw SQLException.
**/
public void doUpdate(HttpServletResponse response, Connection conn, String username, String code, String sender, String recipient, String body, String notes) throws SQLException, IOException {
    PreparedStatement stmt = conn.prepareStatement("UPDATE message SET sender=?, recipient=?, body=?, notes=? where code=?");
    stmt.setString(1, sender);
    stmt.setString(2, recipient);
    stmt.setString(3, body);
    stmt.setString(4, notes);
    stmt.setString(5, code);
    stmt.executeUpdate();
    try {
        audit(conn, username, "update", code, sender, recipient, body, notes);
    } catch(SQLException e) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't write to audit table.");
        return;
    }
    response.sendRedirect("/");
    return;
}

/**
 * Delete an existing record in the DB.
 *
 * IN: response:  The Response object from the JSP (so we can send redirects).
 * IN: conn:      The Connection to use.
 * IN: username:  The user who made the insert.
 * IN: code:      The code for the message to be updated.
 *
 * OUT: nothing
 *
 * Side Effects: redirects the page to he message just inserted, adds an
 *               entry to the audit table.  Could throw SQLException.
**/
public void doDelete(HttpServletResponse response, Connection conn, String username, String code) throws SQLException, IOException {
    PreparedStatement stmt = conn.prepareStatement("DELETE FROM message WHERE code=?");
    stmt.setString(1, code);
    stmt.executeUpdate();
    try {
        audit(conn, username, "delete", code, "", "", "", "");
    } catch(SQLException e) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't write to audit table.");
        return;
    }
    response.sendRedirect("/");
    return;
}

// END SHARED FUNCTION DEFINITIONS.
%>
<%
// BEGIN PAGE CONTENT.

// Start by loading the password that we're expecting to get from the user
// (and with which we'll be connecting to the database).
Properties props = new Properties();
props.load(new FileInputStream(request.getRealPath("META-INF/love.properties")));

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

// OK, now to figure out which state this page is in.  Options are:
//
// 1. Normal load -- just spit out a blank form and show a table of all the
//    messages in the database.
// 2. Edit -- we got a code and no delete, so spit out a filled form and
//    put in a hidden code field.
// 3. Insert -- we got sender/recipient/body, but no code, so generate a
//    code and insert into the database.
// 4. Update -- we got sender/recipient/body AND code, so update the entry
//    in the database.
// 5. Delete Confirm -- we got code and a delete=ask, so display the entry
//    and ask to make sure the user wants to delete this entry.
// 6. Delete -- we got code and a delete=really, so delete the entry.

String code = request.getParameter("code");
String sender = request.getParameter("sender");
String recipient = request.getParameter("recipient");
String body = request.getParameter("body");
String notes = request.getParameter("notes");
String delete = request.getParameter("delete");

String form = null;
String title = null;

if( code == null && sender == null && recipient == null && body == null && delete == null ) {
    // Case 1: Display a blank form.
    form = blankForm();
    title = "Create Message";
} else if( code != null && !code.equals("") && sender == null && recipient == null && body == null && notes == null && delete == null ) {
    // Case 2: Display a form with the bits to edit.
    form = editForm(conn, code);
    title = "Edit Message";
} else if( code == null && sender != null && !sender.equals("") && recipient != null && !recipient.equals("") && body != null && !body.equals("") && notes != null && delete == null ) {
    // Case 3: Run the insert and redirect.
    doInsert(response, conn, username, sender, recipient, body, notes);
    return;
} else if( code != null && !code.equals("") && sender != null && !sender.equals("") && recipient != null && !recipient.equals("") && body != null && !body.equals("") && notes != null && delete == null ) {
    // Case 4: Run the update and redirect.
    doUpdate(response, conn, username, code, sender, recipient, body, notes);
    return;
} else if( code != null && !code.equals("") && delete != null && delete.equals("ask") ) {
    // Case 5: Display the confirmation form (to confirm that we really want to delete).
    form = confirmForm(conn, code);
    title = "Confirm Delete";
} else if( code != null && !code.equals("") && delete != null && delete.equals("confirm") ) {
    // Case 6: We got confirmation to delete the entry, so run the delete and redirect.
    doDelete(response, conn, username, code);
    return;
} else {
    // Throw a 400 because we don't know what this is.
    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid set of parameters passed.");
    return;
}
%>
<html>
    <head>
        <title>Riverbots Lovebot&trade; Administration UI: <%=title%></title>
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/css/bootstrap.min.css" integrity="sha384-HSMxcRTRxnN+Bdg0JdbxYKrThecOKuH5zCYotlSAcp1+c8xmyTe9GYg1l9a69psu" crossorigin="anonymous">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/css/bootstrap-theme.min.css" integrity="sha384-6pzBo3FDv/PJ8r2KRkGHifhEocL+1X2rVCTTkUfGk7/0pbek5mMa1upzvWbrUbOZ" crossorigin="anonymous">
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/js/bootstrap.min.js" integrity="sha384-aJ21OjlMXNL5UyIl/XNwTMqvzeRMZH2w8c5cRVpzpU8Y5bApTppSuUkhZXN0VxHd" crossorigin="anonymous"></script>
    </head>
    <body>
        <h1><%=title%></h1>
<%=form%>
        <!-- START List of All Messages -->
        <h1>All Messages</h1>
        <table class="table">
            <tr>
                <th scope="col">Code</th>
                <th scope="col">Sender</th>
                <th scope="col">Recipient</th>
                <th scope="col">Message</th>
                <th scope="col">Notes</th>
                <th scope="col">Actions</th>
            </tr>
<%
PreparedStatement stmt;
if( code != null && !code.equals("") ) {
    stmt = conn.prepareStatement("SELECT code, sender, recipient, body, notes FROM message WHERE code != ? ORDER BY code");
    stmt.setString(1, code);
} else {
    stmt = conn.prepareStatement("SELECT code, sender, recipient, body, notes FROM message ORDER BY code");
}
ResultSet result = stmt.executeQuery();
while(result.next()) {
    code = StringEscapeUtils.escapeXml(result.getString(1));
    sender = StringEscapeUtils.escapeXml(result.getString(2));
    recipient = StringEscapeUtils.escapeXml(result.getString(3));
    body = StringEscapeUtils.escapeXml(result.getString(4));
    notes = StringEscapeUtils.escapeXml(result.getString(5));
%>
            <tr>
                <td scope="row"><a href="https://love.riverbots.org?<%=code%>" target="_blank"><%=code%></a></td>
                <td><%=sender%></td>
                <td><%=recipient%></td>
                <td><%=body%></td>
                <td><%=notes%></td>
                <td>
                    <a href="index.jsp?code=<%=code%>&action=edit"><img src="edit.png" alt="[Edit]" /></a>
                    <a href="index.jsp?code=<%=code%>&delete=ask"><img src="delete.png" alt="[Delete]" /></a>
                    <a href="sticker.jsp?code=<%=code%>"><img src="print.png" alt="[Show Sticker]" /></a>
                    <a href="sticker.jsp?code=<%=code%>&toprinter=yes" target="_blank"><img src="print2.png" alt="[Print]" /></a>
                </td>
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
