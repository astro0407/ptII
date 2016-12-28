// JavaScript functions for a Ptolemy II (Nashorn) accessor host.
// Copyright (c) 2016-2016 The Regents of the University of California.
// All rights reserved.
//
// Permission is hereby granted, without written agreement and without
// license or royalty fees, to use, copy, modify, and distribute this
// software and its documentation for any purpose, provided that the above
// copyright notice and the following two paragraphs appear in all copies
// of this software.
//
// IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
// FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
// ENHANCEMENTS, OR MODIFICATIONS.
//

/** JavaScript functions for a Ptolemy II (Nashorn) accessor host.
 *
 *  <p>This file includes basic utility functions assumed by version 1
 *  accessors.</p>
 *
 * @module basicFunctions
 * @author Edward A. Lee, Contributor: Christopher Brooks
 * @version $$Id$$
 * @since Ptolemy II 11.0
 */

// Stop extra messages from jslint.  Note that there should be no
// space between the / and the * and global.
/*globals Java, accessors, actor, commonHost, getAccessorCode, load */
/*jshint globalstrict: true */
/*jslint nomen: true */
"use strict";

// Java classes that define some static functions to call from JS.
var NashornAccessorHostApplication 
        = Java.type('ptolemy.actor.lib.jjs.NashornAccessorHostApplication');
var StringUtilities
        = Java.type('ptolemy.util.StringUtilities');

// Flag that will cause debug output to the console if set to true.
var debug = false;

/** Pop up a dialog with the specified message.
 *  NOTE: This function is not required by the accessor specification, so accessors
 *  should not rely on it being present.
 *  @param message The message
 */
function alert(message) {
    var MessageHandler = Java.type('ptolemy.util.MessageHandler');
    MessageHandler.message(message);
}

/** Clear an interval timer with the specified handle.
 *  @param handle The handle
 *  @see setInterval().
 */
function clearInterval(handle) {
    actor.clearTimeout(handle);
}

/** Clear a timeout with the specified handle.
 *  @param handle The handle
 *  @see setTimeout().
 */
function clearTimeout(handle) {
    actor.clearTimeout(handle);
}

/** Report an error. This implementation delegates to the host actor to
 *  report the error. In this implementation, the host actor has an error output
 *  port. If that port is connected to something, then the error message is sent
 *  to that port. Otherwise, the error is reported via a dialog or on stderr (the
 *  latter if running headless).
 *  This function should be used only for non-fatal errors, where it is OK to
 *  continue executing. For fatal errors, throw an exception.
 *  @param message The message for the exception.
 */
function error(message) {
    // Print a stack trace to the console.
    /*
    console.error(message);
    console.error('------------------------- error stack trace:');
    var e = new Error('dummy');
    var stack = e.stack.replace(/^[^\(]+?[\n$]/gm, '')
            .replace(/^\s+at\s+/gm, '')
            .replace(/^Object.<anonymous>\s*\(/gm, '{anonymous}()@')
            .split('\n');
    console.error(stack);
    console.error('-------------------------');
    */

    actor.error(message);
}

/** Get a resource, which may be a relative file name or a URL, and return the
 *  value of the resource as a string.
 *  Implementations of this function may restrict the locations from which
 *  resources can be retrieved. This implementation restricts relative file
 *  names to be in the same directory where the swarmlet model is located or
 *  in a subdirectory, or if the resource begins with "$CLASSPATH/", to the
 *  classpath of the current Java process.
 *  @param uri A specification for the resource.
 *  @param timeout The timeout in milliseconds.
 */
function getResource(uri, timeout) {
    return actor.getResource(uri, timeout);
}

/** Perform a blocking HTTP request.
 *  @param url The url for the request, method, properties, body, timeout) {
 *  @param method the request method for the url connection.
 *  @param properties An array of properties for the connection.
 *  @param body If non-empty, the body to be written to the output
 *  stream of the connection.
 *  @param timeout If specified, the connect and read timeout in milliseconds.
 *
 *  @deprecated: Use the http module instead, which provides non-blocking requests.
 */
function httpRequest(url, method, properties, body, timeout) {
    var theURL = new(Java.type('java.net.URL'))(url),
        protocol = theURL.getProtocol().toLowerCase(),
        connection,
        key,
        writer;

    if (debug) {
        console.log("httpRequest(" + (function (obj) {
            var result = [],
                p;
            for (p in obj) {
                if (obj.hasOwnProperty(p)) {
                    result.push(JSON.stringify(obj[p]));
                }
            }
            return result;
        })(arguments) + ")");
    }
    if (actor.isRestricted &&
        !(protocol.equals("http") ||
            protocol.equals("https"))) {
        throw "Actor is restricted. Only HTTP(S) requests will be honored by httpRequest().";
    }
    connection = theURL.openConnection();

    for (key in properties) {
        if (properties.hasOwnProperty(key)) {
            connection.setRequestProperty(key, properties[key]);
        }
    }

    connection.setRequestMethod(method);

    // If a timeout has been specified, set it.
    if (timeout && timeout >= 0) {
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
    }

    // Send body if applicable.
    if (body && !body.equals('')) {
        connection.setDoOutput(true);
        writer = new(Java.type('java.io.OutputStreamWriter'))(connection
            .getOutputStream());
        writer.write(body);
        writer.flush();
        writer.close();
    }

    // Wait for response.
    return NashornAccessorHostApplication.readFromInputStream(
        connection.getInputStream()
    );
}

// Print a message to the console.
// print is built in to Nashorn, but is not required by the accessor specification,
// so accessors should not rely on it being present.

/** Synchronously read a URL.
 *  @param url The url.
 *  @param timeout The timeout in milliseconds
 *
 *  @deprecated: Use the http module, which provides non-blocking functions.
 */
function readURL(url, timeout) {
    if (!timeout) {
        timeout = 3000;
    }
    if (debug) {
        console.log("readURL('" + url + "')");
    }
    var theURL = new(Java.type('java.net.URL'))(url),
        request,
        response;
    if (actor.isRestricted &&
        !theURL.getProtocol().toLowerCase().equals("http") &&
        !theURL.getProtocol().toLowerCase().equals("https")) {
        throw "Actor is restricted. Only HTTP and HTTPS requests will be honored by readURL().";
    }
    request = new(Java.type('org.ptolemy.ptango.lib.HttpRequest'))();
    request.setUrl(theURL);
    request.setTimeout(timeout); // In milliseconds.
    response = request.execute();
    if (!response.isSuccessful()) {
        throw "Failed to read URL: " + url + "\nResponse code: " +
            response.getResponseCode() + "\nResponse message: " +
            response.getResponseMessage();
    }
    return response.getBody();
}

var __moduleFile = Java.type('ptolemy.util.FileUtilities').nameToFile(
    '$CLASSPATH/ptolemy/actor/lib/jjs/',
    null
);

/** A string giving the full path to the root directory for installed modules. */
var _moduleRoot = __moduleFile.getAbsolutePath();

// Check to see if _moduleFile is a Jar URL like
if (_moduleRoot.indexOf("!/") !== -1) {
    _moduleRoot = "jar:" + __moduleFile.toString();
}

var __accessorFile = Java.type('ptolemy.util.FileUtilities').nameToFile(
    '$CLASSPATH/org/terraswarm/accessor/accessors/web/',
    null
);

var _accessorRoot = __accessorFile.getAbsolutePath();

// Check to see if _accessorRoot is a Jar URL like
if (_accessorRoot.indexOf("!/") !== -1) {
    _accessorRoot = "jar:" + __accessorFile.toString();
}

/** An array that gives the search path for modules to be required. */
var _modulePath = [_moduleRoot + '/',
		   _moduleRoot + '/modules/',
		   _moduleRoot + '/node/',
		   _moduleRoot + '/node_modules/',
		   _accessorRoot + '/hosts/',
		   _accessorRoot + '/'];

/** An array that gives the search path for modules to be required relative to the classpath. */
var _moduleClasspath = ['$CLASSPATH/ptolemy/actor/lib/jjs/modules/',
			   '$CLASSPATH/ptolemy/actor/lib/jjs/node/',
			   '$CLASSPATH/ptolemy/actor/lib/jjs/node_modules/',
			   '$CLASSPATH/org/terraswarm/accessor/accessors/web/hosts/',
			   '$CLASSPATH/org/terraswarm/accessor/accessors/web/'];

/** A string giving the full path to the root directory for installed accessors. */
var _accessorRoot = Java.type('ptolemy.util.FileUtilities').nameToFile(
    '$CLASSPATH/org/terraswarm/accessor/accessors/web/',
    null
).getAbsolutePath();

/** A string giving the full path to the root directory for test accessors. */
var _testAccessors = Java.type('ptolemy.util.FileUtilities').nameToFile(
    '$CLASSPATH/org/terraswarm/accessor/test/auto/accessors/',
    null
).getAbsolutePath();

/** An array that gives the search path for accessors to be extended. */
var _accessorPath = [_accessorRoot + '/', _testAccessors + '/'].concat(_modulePath);

/** An array that gives the search path for accessors to be extended. */
var _accessorClasspath = ['$CLASSPATH/org/terraswarm/accessor/test/auto/accessors/'].concat(_moduleClasspath);

/**
 * Require the named module. This function imports modules formatted
 * according to the CommonJS standard.
 *
 * <p>If the name begins with './' or '/', then it is assumed to
 * specify a file or directory on the local disk. If it is a file, the
 * '.js' suffix may be optionally omitted. If it is a directory, then
 * this function will look for a package.json file in that directory
 * and load the file specified by the 'main' property the JSON object
 * defined in that file. If there is no package.json file, then it
 * will load an 'index.js' file, if there is one.</p>
 *
 * <p>If the name does not begin with './' or '/', then it is assumed
 * to specify a module installed in this accessor host.</p>
 *
 * <p>In both cases, this function returns an object that includes as
 * properties any properties that have been added to the 'exports'
 * property. For example, to export a function, the module JavaScript
 * file could define the function as follows:</p>
 *
 * <pre>
 *   exports.myFunction = function() {...};
 * </pre>
 *
 * <p>Alternatively, the module JavaScript file can explicitly define
 * the exports object as follows:<p>
 *
 * <pre>
 *   var myFunction = function() {...};
 *   module.exports = {
 *       myFunction : myFunction
 *   };
 * </pre>
 *
 * <p>This implementation uses the requires() function implemented by Walter Higgins,
 * found here: <a href="https://github.com/walterhiggins/commonjs-modules-javax-script.">https://github.com/walterhiggins/commonjs-modules-javax-script</a>.</p>
 *
 * @see http://nodejs.org/api/modules.html#modules_the_module_object
 * @see also: http://wiki.commonjs.org/wiki/Modules
 */
var require = load(_moduleRoot + '/external/require.js')(
    // Invoke the function returned by 'load' immediately with the following arguments.
    //    - a root directory in which to look for modules.
    //    - an array of paths in which to look for modules.
    //    - an optional hook object that includes two callback functions for notification.
    _moduleRoot,
    _modulePath
);

// If we are using Nashorn outside of Cape Code, then actor will be undefined or null.
if (typeof actor === 'undefined') {
    var actor = require('external/setTimeout-nashorn.js');
} else if (actor === null) {
    // FIXME: What we have a RestrictedJavaScriptInterface?
    actor = require('external/setTimeout-nashorn.js');
}

/**
 * Require the named accessor. This is a version of require() that looks
 * in a different place for accessors.
 * @see #require()
 */
var requireAccessor = load(_moduleRoot + '/external/require.js')(
    // Invoke the function returned by 'load' immediately with the following arguments.
    //    - a root directory in which to look for accessors.
    //    - an array of paths in which to look for accessors.
    //    - an optional hook object that includes two callback functions for notification.
    _accessorRoot,
    _accessorPath
);

////////////////////
// Pull in the util and console modules.
var util = require('util');
var console = require('console');

// Locally defined modules.
var commonHost = require('commonHost.js');

// This Cape Code host allows trusted accessors, which means that any
// accessor whose class name begins with 'trusted/' can invoke the
// function getTopLevelAccessors().
commonHost.allowTrustedAccessors(true);

/**
 * Set a timeout to call the specified function after the specified
 * time and repeatedly at multiples of that time.
 *
 * <p> Return a handle to use in clearInterval(). If there are
 * additional arguments beyond the first two, then those arguments
 * will be passed to the function when it is invoked. This
 * implementation uses fireAt() of the director in charge of the host
 * JavaScript actor in Ptolemy II. Hence, actors that use this should
 * be used with a director that respects fireAt(), such as DE.  If the
 * director has synchronizeToRealTime set to true, then it will
 * approximate real-time behavior reasonably closely. Otherwise, the
 * timeout will only be simulated. Either way, the timing is much more
 * precise and well-defined than usual for JavaScript environments. If
 * two actors specify the same timeout time in, say, their
 * initialize() function, then they will be invoked at the same model
 * time, and their outputs will be simultaneous. Any downstream actor
 * will see them simultaneously.</p>
 *
 * <p>Note with this implementation, it is not necessary to
 * call clearInterval() in the actor's wrapup() function.
 * Nevertheless, it is a good idea to do that in an accessor
 * since other accessor hosts may not work the same way.
 *
 * @param func The callback function.
 * @param milliseconds The interval in milliseconds.
 */
function setInterval(func, milliseconds) {
    var callback = func,
        // If there are arguments to the callback, create a new function.
        // Get an array of arguments excluding the first two.
        tail = Array.prototype.slice.call(arguments, 2),
        id;
    if (tail.length !== 0) {
        callback = function () {
            func.apply(this, tail);
        };
    }
    id = actor.setInterval(callback, milliseconds);
    return id;
}

/**
 * Set a timeout to call the specified function after the specified time.
 * Return a handle to use in clearTimeout(). If there are
 * additional arguments beyond the first two, then those arguments
 * will be passed to the function when it is invoked. This
 * implementation uses fireAt() of the director in charge of the host
 * JavaScript actor in Ptolemy II. Hence, actors that use this should
 * be used with a director that respects fireAt(), such as DE.  If the
 * director has synchronizeToRealTime set to true, then it will
 * approximate real-time behavior reasonably closely. Otherwise, the
 * timeout will only be simulated. Either way, the timing is much more
 * precise and well-defined than usual for JavaScript environments. If
 * two actors specify the same timeout time in, say, their
 * initialize() function, then they will be invoked at the same model
 * time, and their outputs will be simultaneous. Any downstream actor
 * will see them simultaneously.
 *
 * Note with this implementation, it is not necessary to
 * call clearTimeout() in the actor's wrapup() function.
 * @param func The callback function.
 * @param milliseconds The interval in milliseconds.
 */
function setTimeout(func, milliseconds) {
    var callback = func,
        // If there are arguments to the callback, create a new function.
        // Get an array of arguments excluding the first two.
        tail = Array.prototype.slice.call(arguments, 2),
        id;
    if (tail.length !== 0) {
        callback = function () {
            func.apply(this, tail);
        };
    }

    id = actor.setTimeout(callback, milliseconds);
    return id;
}

var accessors = [];

/** Return the source code for an accessor from its fully qualified name.
 *  This will throw an exception if there is no such accessor on the accessor
 *  search path.
 *  @param name Fully qualified accessor name, e.g. 'net/REST'.
 */
function getAccessorCode(name) {
    var code,
        i,
        location;
    // Append a '.js' to the name, if needed.
    if (name.indexOf('.js') !== name.length - 3) {
        name += '.js';
    }

    // Handle absolute pathnames.
    if (name[0] === '/' || name[0] === '\\') {
        code = NashornAccessorHostApplication.getFileAsString(name);
        return code;
    }

    // _accessorPath is defined in basicFunctions.js.
    for (i = 0; i < _accessorPath.length; i++) {
        location = _accessorPath[i].concat(name);
        try {
            code = NashornAccessorHostApplication.getFileAsString(location);
            break;
        } catch (err) {
            continue;
        }
    }
    if (!code) {
	for (i = 0; i < _accessorClasspath.length; i++) {
		location = _accessorClasspath[i].concat(name);
		try {
			code = NashornAccessorHostApplication.getFileFromClasspathAsString(location);
			break;
		} catch (err) {
			continue;
		}
	}
    }
    if (!code) {
        throw ('Accessor ' + name + ' not found on path: ' + _accessorPath + ' or relative path: ' + _accessorClasspath);
    }
    return code;
}

/** Instantiate and return an accessor.
 *  This will throw an exception if there is no such accessor class on the accessor
 *  search path.
 *  @param accessorName The name to give to the instance.
 *  @param accessorClass Fully qualified accessor class name, e.g. 'net/REST'.
 */
function instantiate(accessorName, accessorClass) {
    // FIXME: The bindings should be a bindings object where require == a requireLocal
    // function that searches first for local modules.
    var bindings = {
        'require': require,
    };
    var instance = new commonHost.instantiateAccessor(
        accessorName, accessorClass, getAccessorCode, bindings);
    console.log('Instantiated accessor ' + accessorName + ' with class ' + accessorClass);

    accessors.push(instance);
    return instance;
}

/** Instantiate and initialize the accessors named by the
 *  accessorNames argument
 *
 * See invoke() for how this method is used.
 *
 * @param accessorNames An array of accessor names in a format suitable
 * for getAccessorCode(name).
 */
function instantiateAndInitialize(accessorNames) {
    var length = accessorNames.length;
    var index;
    for (index = 0; index < length; ++index) {
        // The name of the accessor is basename of the accessorClass.
        var accessorClass = accessorNames[index];

        // For example, if the accessorClass is
        // test/TestComposite, then the accessorName will be
        // TestComposite.

        var startIndex = (accessorClass.indexOf('\\') >= 0 ? accessorClass.lastIndexOf('\\') : accessorClass.lastIndexOf('/'));
        var accessorName = accessorClass.substring(startIndex);
        if (accessorName.indexOf('\\') === 0 || accessorName.indexOf('/') === 0) {
            accessorName = accessorName.substring(1);
        }
        // If the same accessorClass appears more than once in the
        // list of arguments, then use different names.
        // To replicate: node nodeHostInvoke.js test/TestComposite test/TestComposite
        if (index > 0) {
            accessorName += "_" + (index - 1);
        }
        var accessor = instantiate(accessorName, accessorClass);
        // Push the top level accessor so that we can call wrapup later.
        accessors.push(accessor);
        accessor.initialize();
    }
    return accessors;
}

/** Evaluate command-line arguments by first converting the arguments
 *  from a Java array to a JavaScript array, and then invoking main()
 *  in commonHost.js.
 *  @param argv Command-line arguments.
 */
function processCommandLineArguments(argv) {
	return commonHost.processCommandLineArguments(
	        // Command-line arguments.
	        Java.from(argv),
	        // Function to read a file and return a string.
	        NashornAccessorHostApplication.getFileAsString,
	        // Function to instantiate accessors.
	        instantiate,
	        // Function to call upon termination.
	        function() {
	            // FIXME: Should first call wrapup() on all accessors.
	            StringUtilities.exit(0);
	        }
	);
}

///////////////////////////////////////////////////////////////////////
// Make commonHost functions visible when this file is evaluated directly.

var Accessor = commonHost.Accessor;
var getTopLevelAccessors = commonHost.getTopLevelAccessors;
var stopAllAccessors = commonHost.stopAllAccessors;
var uniqueName = commonHost.uniqueName;

// FIXME: Handle exit calls like how we do in nodeHost?
