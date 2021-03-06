/*
 * This script is used to select query methods
 */

var numberOfQueries;       // number of queries
var smartNumOfQueries;     // smart number of queries
var file_name;             // file name of user uploaded text file

$(document).ready(function(){
    $('[data-toggle="qtip"]').tooltip();   
});

/**
 * Enable/disable input box for number of queries
 */
function enableQueryInput (args) {
    if (document.getElementById("randomQuery").checked || document.getElementById("smartQuery").checked) {
        document.getElementById("queryInput").disabled = false;
    } else {
        document.getElementById("queryInput").disabled = true;
    }
    // if user selected smart query, set grid size to 5 km
    if (document.getElementById("smartQuery").checked) {
        $('#gridSizeDisp').html("5 km");
        adjustGridSize("5 km");
    }
}

/**
 * Adjust value of query numbers so that it is multiple of 10
 * Min: 10, Max: 500
 */
function adjustValue(args) {
    var id = "queryInput";
    if (!isNumeric(document.getElementById(id).value)) {
        document.getElementById(id).value = 100;
        return;
    }
    var val = document.getElementById(id).value;
    var newVal = Math.round(val / 10) * 10;
    if (newVal < 10) {
        newVal = 10;
    }
    if (newVal > 500) {
        newVal = 500;
    }
    document.getElementById(id).value = newVal;
}
