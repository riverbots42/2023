//
// Javascript frontend for the Riverbots Lovebot application.
//
// The HTML for this is in landing.html for the use case where a user doesn't
// submit a complete URL.
//

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

// validate runs a simple checksum validator to make sure no simple
// typing errors have occurred.
function validate(code) {
    if (code.substr(0,4) == "0EM0") {
        return true;
    }
    count = 0;
    for (i = 0; i < code.length - 1; i++) {
        count += code.charCodeAt(i);
    }
    return (count % 10).toString() == code.substr(5, 1);
}
