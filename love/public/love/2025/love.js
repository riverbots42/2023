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
var lines_to_print = ["RiverbotOS 2.1.1 Booting...", 2000, "Connecting to Lovebot&trade; Server...", 2000];

// How long to delay between printing chars onscreen.
var delay_between_chars = 50;

// For Safari only, force user to tap before playing audio.
var wait_for_click = false;

// Tick prints stuff that's in queue to the screen and is run 20 times/sec.
// So don't put too much stuff in here :-)
function tick() {
    if (lines_to_print.length == 0 || wait_for_click) {
        return;
    }
    // shift() has the side effect that line is removed from the array.
    // We *may* add it back if it's not done processing yet.
    line = lines_to_print.shift();
    if( typeof(line) == "function" ) {
        line();
    } else if (typeof(line) == "number") {
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
//
// If the user has already interacted with the page, or if the play button is pressed, go to play_index().
function index_init(code) {
    var isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);
    var audiosrc = "love.ogg";
    var audiotype = "audio/ogg";
    if(isSafari) {
        lines_to_print.push("Installing fix for Apple devices...", 2000);
        audiosrc = "love.mp3";
        audiotype = "audio/mpeg";
    } else {
        lines_to_print.push(2000);
    }
    console.log("Setting src = " + audiosrc + ", type = " + audiotype);
    $("#audiosrc")[0].src = audiosrc;
    $("#audiosrc")[0].type = audiotype;

    if(!navigator.userActivation.hasBeenActive) {
        // when the user clicks the play button, hide the coverbox.
        $("#play").click(function() {
            $.get("poke.jsp");
            play_index(code);
        });
    } else {
        play_index(code);
    }
}

// play_index is run when the play button has been pressed.
//
// A few callbacks:
// - the play button has a click event to hide the coverbox when clicked.
// - there's a 50ms interval that types out characters on the simulated
//   terminal.
// - there's a 500ms blinker for the simulated cursor
function play_index(code) {
    $("#cover_box").hide();

    resize();

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
        $.get("../message.jsp?code=" + code, function(data) {
            lines_to_print.push(function() {
                $("#message").text("");
		play_audio();
	    });
            lines_to_print.push("From:&nbsp;&nbsp;&nbsp;&nbsp;" + data["sender"], 2000);
            lines_to_print.push("To:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + data["recipient"], 2000);
            lines_to_print.push("", 1000);
            data["body"].split("\n").forEach(function(line) {
                lines_to_print.push(line, 1000);
            });
        });
    }, 4000);
}

function play_audio() {
    var isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);
    if( isSafari ) {
        wait_for_click = true;
        $("#message").text("-- Tap to Play --");
        $("#message").on("click", function() {
            $("#player")[0].play();
            $("#message").text("");
            $("#message").off("click");
            wait_for_click = false;
        });
    } else {
        $("#player")[0].play();
    }
}

// Adjust the width/height/position of various elements on the laptop graphic.
// Note that we need to handle BOTH portrait and landscape variants, so we try to maintain at LEAST a square aspect ratio box on
// screen at all times (i.e. the top half of the laptop), allowing more if there's room.
function resize() {
	var screen_top = 196;
	var screen_left = 164;
	var screen_bottom = 1076;
	var screen_right = 1484;
	var width = window.innerWidth;
	var height = window.innerHeight;
	var laptop_width = $("#laptop")[0].naturalWidth;
	var laptop_height = $("#laptop")[0].naturalHeight;
	var adjustment_factor = 1.0;

	// Case 1: We're in landscape
	if(width > height) {
		// We need to fit the l_h x l_h box in such that the height determines the width.
		var adjustment_factor = height / laptop_width;

	// Case 2: We're in portrait
	} else {
		// We need to fit the l_h x l_h box in such that the width determines the height.
		adjustment_factor = width / laptop_width;
	}

	console.log(adjustment_factor);

	screen_top = Math.round(screen_top * adjustment_factor);
	screen_left = Math.round(screen_left * adjustment_factor);
	screen_bottom = Math.round(screen_bottom * adjustment_factor);
	screen_right = Math.round(screen_right * adjustment_factor);
	width = Math.round(laptop_width * adjustment_factor);
	height = Math.round(laptop_height * adjustment_factor);

	var left_margin = Math.round((window.innerWidth - width) / 2);

	$("#laptop").width(width);
	$("#laptop").height(height);
	$("#laptop").css("left", left_margin);
	$("#term_box").css("top", screen_top);
	$("#term_box").css("left", screen_left + left_margin);
	$("#term_box").width(screen_right - screen_left);
	$("#term_box").height(screen_bottom - screen_top);
	$(window).off("resize");
	$(window).on("resize", resize);
}
