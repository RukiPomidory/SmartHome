'use strict'

const BluetoothSerialPort = require("bluetooth-serial-port");
const readline = require("readline");

var MAC = "A8:1B:6A:75:9E:17"

serial = new (BluetoothSerialPort).BluetoothSerialPort();
serial.findSerialPortChannel(MAC, (channel) => {
	console.log(`${this.name} on ${channel} channel`)
	let start = time();
	serial.connect(MAC, channel, () => {
		let duration = (time() - start).toFixed(3);
		console.log(`connected to ${this.name} [${duration} s]`);
	});
}, () => {
	console.log(`can't find ${this.name}`);
});
		
		