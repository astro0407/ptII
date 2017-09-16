/** Accessor for the SoundServer swarmlet. This accessor sends
 *  messages to the SoundServer via a web socket given the IP address of a server
 *  running the SoundServer swarmlet.
 *
 *  @accessor SoundActuator
 *  @author Edward A. Lee (eal@eecs.berkeley.edu)
 *  @input control A JSON object FIXME.
 *  @output data A JSON object FIXME.
 *  @output schema Upon initialization, this output port produces a JSON object that
 *   is a schema for the JSON expected on the control input.
 *  @version $$Id$$
 */
// Stop extra messages from jslint.  Note that there should be no
// space between the / and the * and global.
/*globals console, error, exports, require */
/*jshint globalstrict: true*/
"use strict";

exports.setup = function() {
	this.implement('ControllableSensor');
	var WebSocketClient = this.instantiate('WebSocketClient', 'net/WebSocketClient');
	// FIXME: This should be a parameter?
	WebSocketClient.input('server', {
	    'value': '128.32.47.81'
	});
	WebSocketClient.input('port', {
	    'value': '8080'
	});
    this.connect('control', WebSocketClient, 'toSend');
    this.connect(WebSocketClient, 'received', 'data');
};

exports.initialize = function() {
	// At initialize, send the schema;
	this.send('schema', schema);
	// Also send null data.
	this.send('data', null);
};

var schema = {
  "type": "object",
  "properties": {
    "sound": {
      	"type": "string",
      	"title": "Name of the sound to produce",
      	"description": "The name of the sound to produce"
    }
  }
};