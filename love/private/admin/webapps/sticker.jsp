<%@ page language="java" contentType="image/png" trimDirectiveWhitespaces="true" %>
<%@ page import="java.awt.*" %>
<%@ page import="java.awt.geom.*" %>
<%@ page import="java.awt.image.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.nio.charset.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.regex.*" %>
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

void drawOutlinedString(Graphics2D gc, String str, int x, int y) {
    gc.setColor(Color.WHITE);
    for( int xt=x-3; xt<=x+3; xt++ ) {
        for( int yt=y-3; yt<=y+3; yt++) {
            gc.drawString(str, xt, yt);
        }
    }
    gc.setColor(Color.BLACK);
    gc.drawString(str, x, y);
}

// END SHARED FUNCTION DEFINITIONS.
%>
<%
// BEGIN PAGE CONTENT.

// Start by loading the password that we're expecting to get from the user
// (and with which we'll be connecting to the database).
Properties props = new Properties();
props.load(new FileInputStream(request.getRealPath("WEB-INF/love.properties")));

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

// Figure out which sticker we're printing on multi-sticker runs.
int sticker = -1;
try {
    sticker = Integer.parseInt(request.getParameter("sticker"));
} catch(Exception e) {
    sticker = 1;
}

// Fetch the recipient info from the database.
PreparedStatement stmt = conn.prepareStatement("SELECT sender, recipient, body FROM message WHERE code=?");
stmt.setString(1, code);
ResultSet rs = stmt.executeQuery();
rs.next();
TreeMap<String,String> vars = new TreeMap<String,String>();
vars.put("url", "https://love.riverbots.org?" + code);
vars.put("sender", rs.getString(1));
vars.put("recipient", rs.getString(2));
vars.put("body", rs.getString(3));
rs.close();

int sticker_width = Integer.parseInt(props.getProperty("sticker_width"));
int sticker_height = Integer.parseInt(props.getProperty("sticker_height"));
int sticker_margin_left = Integer.parseInt(props.getProperty("sticker_margin_left", "0"));
int sticker_margin_right = Integer.parseInt(props.getProperty("sticker_margin_right", "0"));
int sticker_margin_top = Integer.parseInt(props.getProperty("sticker_margin_top", "0"));
int sticker_margin_bottom = Integer.parseInt(props.getProperty("sticker_margin_bottom", "0"));

int qr_x = Integer.parseInt(props.getProperty("qr_x"));
int qr_y = Integer.parseInt(props.getProperty("qr_y"));
int qr_width = Integer.parseInt(props.getProperty("qr_width"));
int qr_height = Integer.parseInt(props.getProperty("qr_height"));

BufferedImage image = new BufferedImage(
    sticker_width + sticker_margin_left + sticker_margin_right,
    sticker_height + sticker_margin_top + sticker_margin_bottom,
    BufferedImage.TYPE_INT_RGB
);
Graphics2D gc = (Graphics2D) image.getGraphics();
gc.setBackground(Color.WHITE);
gc.setColor(Color.WHITE);
gc.fillRect(0, 0, image.getWidth(), image.getHeight());
gc.setColor(Color.BLACK);
File fontfile = new File(request.getRealPath("sticker.ttf"));
Font basefont = Font.createFont(Font.TRUETYPE_FONT, fontfile);
if( sticker == 1 ) {
    BitMatrix qr = new MultiFormatWriter().encode(vars.get("url"), BarcodeFormat.QR_CODE, qr_width, qr_height);
    for(int row=0; row<qr.getHeight(); row++) {
        for(int col=0; col<qr.getHeight(); col++) {
            if(qr.get(col, row)) {
                gc.fillRect(qr_x+col+sticker_margin_left, qr_y+row+sticker_margin_top, 1, 1);
            }
        }
    }
}

Pattern reFields = Pattern.compile("^sticker(\\d+)_field(\\d+)_x$");
for( String key : props.stringPropertyNames() ) {
    Matcher m = reFields.matcher(key);
    if( m.matches() ) {
        int stickerno = Integer.parseInt(m.group(1));
        if( sticker == stickerno ) {
            int fieldno = Integer.parseInt(m.group(2));
            int x = Integer.parseInt(props.getProperty(key, "0"));
            int y = Integer.parseInt(props.getProperty(String.format("sticker%d_field%d_y", stickerno, fieldno), "0"));
            String fontsize = props.getProperty(String.format("sticker%d_field%d_size", stickerno, fieldno), "");
            if( fontsize.equals("") ) {
                fontsize = "20";
            }
            Font font = basefont.deriveFont(Font.BOLD, (float)Integer.parseInt(fontsize));
            gc.setFont(font);
            String text = props.getProperty(String.format("sticker%d_field%d_text", stickerno, fieldno), "");
            if( text != "" ) {
                Rectangle2D r2 = font.getStringBounds(text, gc.getFontRenderContext());
                drawOutlinedString(gc, text, x+sticker_margin_left, y+sticker_margin_top+((int)r2.getHeight()));
            }
            String var = props.getProperty(String.format("sticker%d_field%d_var", stickerno, fieldno), "");
            if( var != "" ) {
                Rectangle2D r2 = font.getStringBounds(vars.get(var), gc.getFontRenderContext());
                drawOutlinedString(gc, vars.get(var), x+sticker_margin_left, y+sticker_margin_top+((int)r2.getHeight()));
            }
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
