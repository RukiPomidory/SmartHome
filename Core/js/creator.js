'use strict';

var Device = require('./device');

class Creator {
	
	constructor(){
		
	}
	
	create(){
		return new Device();
	}
	
}

module.exports = Creator;