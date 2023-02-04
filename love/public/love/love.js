//
// Javascript frontend for the Riverbots Lovebot application.
//
// The HTML for this is in index.jsp (itself a Java Server Pages app) and
// landing.html (which is redirected to in the event that a code isn't
// passed in the initial call to the index.
//
// The assumption is that the typical user will either be hitting landing.html
// and manually entering a code or that there will be phone-scanned QR code
// of the form https://love.riverbots.org?123456, which will automatically run
// the app with the connected message (retrieved by Jquery AJAX below).
//

// lines_to_print consists of a number of lines (which get consumed as a queue).
// Strings are printed to the content element; numbers are pauses (in ms).
var lines_to_print = ["RiverbotOS 1.2 Booting...", 2000, "Connecting to Lovebot&trade; Server...", 4000];

// How long to delay between printing chars onscreen.
var delay_between_chars = 50;

// Tick prints stuff that's in queue to the screen and is run 20 times/sec.
// So don't put too much stuff in here :-)
function tick() {
    if (lines_to_print.length == 0) {
        return;
    }
    // shift() has the side effect that line is removed from the array.
    // We *may* add it back if it's not done processing yet.
    line = lines_to_print.shift();
    if (typeof(line) == "number") {
        // We're in the middle of a pause.  Decrement the counter.
        line -= delay_between_chars;
        if (line <= 0) {
            // If the pause is about to end, go ahead and print a line
            // feed to jump to the next line.
            lines_to_print.unshift("\n");
        } else {
            lines_to_print.unshift(line);
        }
    } else {
        // We should be printing a letter.  Go ahead and remove the
        // virtual-cursor span tag, pop in a letter or a <br/> and
        // put the cursor span back in after.
        t = $("#message").html().replace(/<span.*/, "");
        if (line.substring(0, 1) == "&") {
            // This is a special case for HTML entities.  We consume
            // the whole entity all at once.
            sc = line.indexOf(";");
            console.log(t);
            t += line.substring(0, sc + 1);
            console.log(t);
            line = line.substring(sc + 1);
        } else if (line.substring(0, 1) == "\n") {
            // If we got a \n, then replace it with a <br />
            t += "<br />";
            line = line.substring(1);
        } else {
            // Normal letter; just parrot it out.
            t += line.substring(0, 1);
            line = line.substring(1);
        }
        // Pop back in the virtual cursor we took out at the
        // beginning.
        t += "<span id='cursor'>_</span>";
        $("#message").html(t);
        // If there's stuff left on this line, save it back for the
        // next tick().
        if (line != "") {
            lines_to_print.unshift(line);
        }
    }
}

// index_init is run on document ready for the main index page.
// A few callbacks:
// - the play button has a click event to hide the coverbox when clicked.
// - there's a 50ms interval that types out characters on the simulated
//   terminal.
// - there's a 500ms blinker for the simulated cursor
function index_init(code) {
    // when the user clicks the play button, hide the coverbox.
    $("#play").click(function() {
        $("#coverbox").hide();
    });

    // set the cursor off at first.
    cursor = false;

    // Run tick() to add characters to the screen 20/sec as they appear.
    setInterval(() => {
        tick();
    }, 50);

    // Blink the simulated cursor every 500ms.
    setInterval(() => {
        if (cursor) {
            document.getElementById('cursor').style.opacity = 0;
            cursor = false;
        } else {
            document.getElementById('cursor').style.opacity = 1;
            cursor = true;
        }
    }, 500);

    // After 4 secs, try the AJAX call to get the message for this code.
    setTimeout(() => {
        $.get("message.jsp?code=" + code, function(data) {
            $("#player")[0].start();
            lines_to_print.push("", 1000);
            lines_to_print.push("From:&nbsp;&nbsp;&nbsp;&nbsp;" + data["sender"], 2000);
            lines_to_print.push("To:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + data["recipient"], 2000);
            lines_to_print.push("", 1000);
            data["body"].split("\n").forEach(function(line) {
                lines_to_print.push(line, 1000);
            });
        });
    }, 4000);
}

// validate runs a simple checksum validator to make sure no simple
// typing errors have occurred.
function validate(code) {
    if (code == "0EM0") {
        return true;
    }
    count = 0;
    for (i = 0; i < code.length - 1; i++) {
        count += code.charCodeAt(i);
    }
    return (count % 10).toString() == code.substr(5, 1);
}

// landing_init is run on document ready for landing.html (the one where
// users enter their codes).
function landing_init() {
    $("#code").on('input', function(code) {
        sanitize($("#code"));
    });
    $("#codeform").submit(function(event) {
        event.preventDefault();
        sanitize($("#code"));
        code = $("#code")[0].value;
        if (validate(code)) {
            window.location.href = "/?" + code;
        }
    });
}

// sanitize makes sure that codes are entered unambiguously and without
// data entry errors.  Uses the simple parity method to do error detection
// with the last digit being a checksum.
//
// This function uses a base-25 scheme with the chars 0-9A-Z with the following
// remaps:
//
// B is mapped to 8
// D, O, Q are mapped to 0
// F is mapped to E
// I, J, L are mapped to 1
// S, Z are mapped to 2
// V is mapped to U
//
// This prevents handwritten codes from getting confused when entered by a
// human and should reduce data entry errors.
function sanitize(target) {
    inval = target[0].value;
    outval = "";
    for (i = 0; i < 6 && i < inval.length; i++) {
        c = inval.substr(i, 1).toUpperCase();
        switch (c) {
            case 'B':
                c = '8';
                break;;
            case 'D':
            case 'O':
            case 'Q':
                c = '0';
                break;;
            case 'F':
                c = 'E';
                break;;
            case 'I':
            case 'J':
            case 'L':
                c = '1';
                break;;
            case 'S':
            case 'Z':
                c = '2';
                break;;
            case 'V':
                c = 'U';
                break;;
        }
        outval += c;
    }
    target[0].value = outval;
    if (inval.length == 6 || inval == "0EM0") {
        if (!validate(inval)) {
            $("#status").addClass("error");
            $("#status").text("Invalid code.  Please correct it below.");
        } else {
            if ($("#status").hasClass("error")) {
                $("#status").removeClass("error");
            }
            $("#status").text("Enter the code from the sticker you were given.");
        }
    }
}
