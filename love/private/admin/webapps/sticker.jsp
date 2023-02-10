<%@ page language="java" contentType="image/png" trimDirectiveWhitespaces="true" %>
<%@ page import="java.awt.*" %>
<%@ page import="java.awt.image.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.nio.charset.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page import="javax.imageio.*" %>
<%@ page import="com.google.zxing.*" %>
<%@ page import="com.google.zxing.common.*" %>
<%!
// BEGIN SHARED FUNCTION DEFININTIONS

String post(BufferedImage image) {
    String url = "http://web_printer_1:8080/";
    String charset = "UTF-8";
    // Just generate some unique random value.
    String boundary = Long.toHexString(System.currentTimeMillis());
    // Line separator required by multipart/form-data.
    String CRLF = "\r\n";

    URLConnection connection;
    try {
        connection = new URL(url).openConnection();
    } catch(Exception e) {
        return "{ \"status\": \"Error: " + e.toString() + "\"}";
    }

    connection.setDoOutput(true);
    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

    try (
        OutputStream output = connection.getOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
) {
        // Send binary file.
        writer.append("--" + boundary).append(CRLF);
        writer.append("Content-Disposition: form-data; name=\"lbl\"; filename=\"label.png\"").append(CRLF);
        writer.append("Content-Type: image/png").append(CRLF);
        writer.append("Content-Transfer-Encoding: binary").append(CRLF);
        writer.append(CRLF).flush();
        ImageIO.write(image, "png", output);
        output.flush(); // Important before continuing with writer!
        writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

        // End of multipart/form-data.
        writer.append("--" + boundary + "--").append(CRLF).flush();
    } catch(IOException e) {
        return "{ \"status\": \"Error: " + e.toString() + "\"}";
    }
    StringBuilder ret = new StringBuilder();
    try {
        HttpURLConnection res = (HttpURLConnection) connection;
        BufferedReader br = new BufferedReader(new InputStreamReader(res.getInputStream()));
        int responseCode = ((HttpURLConnection) connection).getResponseCode();
        String output;
        while((output = br.readLine()) != null) {
            ret.append(output);
        }
    } catch(IOException e) {
        return "{ \"status\": \"Error: " + e.toString() + "\"}";
    }
    return ret.toString();
}

// END SHARED FUNCTION DEFINITIONS.
%>
<%
// BEGIN PAGE CONTENT.

// Various locations in the graphic.
final int RECIPIENT_X = 160;
final int RECIPIENT_Y = 639;
final int CODE_X = 205;
final int CODE_Y = 768;
final int QR_X = 380;
final int QR_Y = 360;
final int QR_WIDTH = 250;
final int QR_HEIGHT = 250;

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
Connection conn = DriverManager.getConnection("jdbc:mysql://10.68.0.1:3306/love", "love", props.getProperty("password"));

String toPrinter = request.getParameter("toprinter");
String code = request.getParameter("code");
if( code == null || code.equals("") ) {
    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    return;
}
// Fetch the recipient info from the database.
PreparedStatement stmt = conn.prepareStatement("SELECT recipient FROM message WHERE code=?");
stmt.setString(1, code);
ResultSet rs = stmt.executeQuery();
rs.next();
String recipient = rs.getString(1);
rs.close();

File infile = new File(request.getRealPath("sticker.png"));
BufferedImage image = ImageIO.read(infile);
Graphics2D gc = (Graphics2D) image.getGraphics();
gc.setColor(Color.BLACK);
File fontfile = new File(request.getRealPath("sticker.ttf"));
gc.setFont(Font.createFont(Font.TRUETYPE_FONT, fontfile).deriveFont(Font.BOLD, (float)40.0));
gc.drawString(recipient, RECIPIENT_X, RECIPIENT_Y);
gc.drawString(code, CODE_X, CODE_Y);
String URL = "https://love.riverbots.org?" + code;
BitMatrix qr = new MultiFormatWriter().encode(URL, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT);
for(int row=0; row<qr.getHeight(); row++) {
    for(int col=0; col<qr.getHeight(); col++) {
        if(qr.get(col, row)) {
            gc.fillRect(QR_X+col, QR_Y+row, 1, 1);
        }
    }
}
if( toPrinter != null && toPrinter.equals("yes") ) {
    FileOutputStream zpl = new FileOutputStream("/tmp/dump.png");
    ImageIO.write(image, "png", zpl);
    zpl.close();

    response.setContentType("application/json");
    String ret = post(image);
    response.getOutputStream().write(ret.getBytes());
    return;
}
ImageIO.write(image, "png", response.getOutputStream());

// END PAGE CONTENT
%>
