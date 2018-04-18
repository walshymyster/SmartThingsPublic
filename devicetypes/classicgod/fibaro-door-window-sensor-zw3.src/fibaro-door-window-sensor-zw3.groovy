/**
 *  Fibaro Door/Window Sensor zw3
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Fibaro Door/Window Sensor zw3", namespace: "ClassicGOD", author: "Artur Draga") {	
		capability "Contact Sensor"
		capability "Tamper Alert"
		capability "Temperature Measurement"
		capability "Configuration"
		capability "Battery"
		capability "Sensor"
	
		attribute "temperatureAlarm", "string"
		command "clearTamper"
	
		fingerprint mfr: "010F", prod: "0700", zwv: "3.42"
		fingerprint mfr: "010F", prod: "0700", zwv: "3.52"
	
		//fingerprint mfr: "010F", prod: "0702"
		//fingerprint deviceId: "0x0701", inClusters:"0x5E,0x59,0x22,0x80,0x56,0x7A,0x73,0x98,0x31,0x85,0x70,0x5A,0x72,0x8E,0x71,0x86,0x84"
		//fingerprint deviceId: "0x0701", inClusters:"0x5E,0x59,0x22,0x80,0x56,0x7A,0x73,0x31,0x85,0x70,0x5A,0x72,0x8E,0x71,0x86,0x84"
	}

	tiles (scale: 2) {
		multiAttributeTile(name:"FGDW", type:"lighting", width:6, height:4) {
			tileAttribute("device.contact", key:"PRIMARY_CONTROL") {
				attributeState("open", label:"open", icon:"https://raw.githubusercontent.com/ClassicGOD/SmartThingsPublic/master/devicetypes/classicgod/fibaro-door-window-sensor-2.src/images/doors_open.png", backgroundColor:"#e86d13")
				attributeState("closed", label:"closed", icon:"https://raw.githubusercontent.com/ClassicGOD/SmartThingsPublic/master/devicetypes/classicgod/fibaro-door-window-sensor-2.src/images/doors_close.png", backgroundColor:"#00a0dc")
			}
			tileAttribute("device.multiStatus", key:"SECONDARY_CONTROL") {
				attributeState("multiStatus", label:'${currentValue}')
			}
		}
		
		standardTile("tamper", "device.tamper", inactiveLabel: false, width: 2, height: 2, decoration: "flat") {
			state "clear", label:'', icon: "https://raw.githubusercontent.com/ClassicGOD/SmartThingsPublic/master/devicetypes/classicgod/fibaro-door-window-sensor-2.src/images/tamper_detector0.png"
			state "detected", label:'', action: "clearTamper", icon: "https://raw.githubusercontent.com/ClassicGOD/SmartThingsPublic/master/devicetypes/classicgod/fibaro-door-window-sensor-2.src/images/tamper_detector100.png"
		}
		
		valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
			state "temperature", label:'${currentValue}°',
			backgroundColors:[
				[value: 31, color: "#153591"],
				[value: 44, color: "#1e9cbb"],
				[value: 59, color: "#90d2a7"],
				[value: 74, color: "#44b621"],
				[value: 84, color: "#f1d801"],
				[value: 95, color: "#d04e00"],
				[value: 96, color: "#bc2323"]
			]
		}
		
		valueTile("battery", "device.battery", inactiveLabel: false, width: 2, height: 2, decoration: "flat") {
			state "battery", label:'${currentValue}%\n battery', unit:"%"
		}	
		
		main "FGDW"
		details(["FGDW","tamper","temperature","battery","temperatureAlarm"])
	}
		
	preferences {
		
 		input (
			title: "Fibaro Door/Window Sensor 2",
			description: "Tap to view the manual.",
			image: "http://manuals.fibaro.com/wp-content/uploads/2017/05/dws2.jpg",
			url: "http://manuals.fibaro.com/content/manuals/en/FGDW-002/FGDW-002-EN-T-v1.0.pdf",
			type: "href",
			element: "href"
		)
					
		input ( name: "logging", title: "Logging", type: "boolean", required: false )
	}
}

def clearTamper() { sendEvent(name: "tamper", value: "clear") }

def updated() {
	if ( state.lastUpdated && (now() - state.lastUpdated) < 500 ) return
	logging("${device.displayName} - Executing updated()","info")
	configure()
	state.lastUpdated = now()
}

def configure() {
	logging("Executing configure()","info")
	def cmds = []
	cmds << response(encap(zwave.wakeUpV2.wakeUpIntervalSet(seconds: 4000  as Integer, nodeid: zwaveHubNodeId)))
	cmds << response(encap(zwave.batteryV1.batteryGet()))
	cmds << response(encap(zwave.associationV2.associationRemove(groupingIdentifier: 1)))
	cmds << response(encap(zwave.associationV2.associationRemove(groupingIdentifier: 2)))
	cmds << response(encap(zwave.associationV2.associationRemove(groupingIdentifier: 3)))
	cmds << response(encap(zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: [zwaveHubNodeId])))
	cmds << response(encap(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: [zwaveHubNodeId])))
	cmds << response(encap(zwave.associationV2.associationGet(groupingIdentifier: 2)))
	cmds << response(encap(zwave.associationV2.associationGet(groupingIdentifier: 3))) 
	sendHubCommand(cmds,3000)
}

private multiStatusEvent(String statusValue, boolean force = false, boolean display = false) {
	if (!device.currentValue("multiStatus")?.contains("Sync") || device.currentValue("multiStatus") == "Sync OK." || force) {
		sendEvent(name: "multiStatus", value: statusValue, descriptionText: statusValue, displayed: display)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	log.debug "${device.displayName} - $cmd"
	def cmds = []
	if (cmd.groupingIdentifier == 3) {
		if (cmd.nodeId != [zwaveHubNodeId]) {
			log.debug "${device.displayName} - incorrect Association for Group 3! nodeId: ${cmd.nodeId} will be changed to [${zwaveHubNodeId}]"
			cmds << zwave.associationV2.associationRemove(groupingIdentifier: 1)
			cmds << zwave.associationV2.associationRemove(groupingIdentifier: 3)
			cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: [zwaveHubNodeId])
		} else {
			logging("${device.displayName} - Association for Group 3 correct.","info")
		}
	} else if (cmd.groupingIdentifier == 2) {
		if (cmd.nodeId != [zwaveHubNodeId]) {
			log.debug "${device.displayName} - incorrect Association for Group 2! nodeId: ${cmd.nodeId} will be changed to [${zwaveHubNodeId}]"
			cmds << zwave.associationV2.associationRemove(groupingIdentifier: 2)
			cmds << zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: [zwaveHubNodeId])
		} else {
			logging("${device.displayName} - Association for Group 2 correct.","info")
		}
	}
	if (cmds) { [response(encapSequence(cmds, 1000))] }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	def lastTime = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
	logging("${device.displayName} - BasicSet received, value: ${cmd.value}", "info")
	sendEvent(name: "contact", value: (cmd.value == 255)? "open":"closed"); 
	if (cmd.value == 255) { multiStatusEvent("Contact Open - $lastTime") }
}

def zwaveEvent(physicalgraph.zwave.commands.sensoralarmv1.SensorAlarmReport cmd) {
	def lastTime = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
	logging("${device.displayName} - SensorAlarmReport received, sensorType: ${cmd.sensorType}, sensorState: ${cmd.sensorState}", "info")
	if ( cmd.sensorType == 0 && cmd.sensorState == 255) {
		sendEvent(name: "tamper", value: "detected"); 
		multiStatusEvent("Tamper - $lastTime")
	}
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, ep) {
	logging("${device.displayName} - SensorMultilevelReport received, sensorType: ${cmd.sensorType}, scaledSensorValue: ${cmd.scaledSensorValue}", "info")
	switch (cmd.sensorType) {
		case 1:
			def cmdScale = cmd.scale == 1 ? "F" : "C"
			sendEvent(name: "temperature", unit: getTemperatureScale(), value: convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale, cmd.precision), displayed: true)
			break
		default: 
			logging("${device.displayName} - Unknown sensorType: ${cmd.sensorType}","warn")
			break
	}
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	logging("${device.displayName} - BatteryReport received, value: ${cmd.batteryLevel}", "info")
	sendEvent(name: "battery", value: cmd.batteryLevel.toString(), unit: "%", displayed: true)
}

def parse(String description) {
	def result = []
	logging("${device.displayName} - Parsing: ${description}")
	if (description.startsWith("Err 106")) {
		result = createEvent(
			descriptionText: "Failed to complete the network security key exchange. If you are unable to receive data from it, you must remove it from your network and add it again.",
			eventType: "ALERT",
			name: "secureInclusion",
			value: "failed",
			displayed: true,
		)
	} else if (description == "updated") {
		return null
	} else {
		def cmd = zwave.parse(description, cmdVersions()) 
		if (cmd) {
			logging("${device.displayName} - Parsed: ${cmd}")
			zwaveEvent(cmd)
		}
	}
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand(cmdVersions()) 
	if (encapsulatedCommand) {
		logging("${device.displayName} - Parsed SecurityMessageEncapsulation into: ${encapsulatedCommand}")
		zwaveEvent(encapsulatedCommand)
	} else {
		log.warn "Unable to extract secure cmd from $cmd"
	}
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	def version = cmdVersions()[cmd.commandClass as Integer]
	def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
	def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
	if (encapsulatedCommand) {
		logging("${device.displayName} - Parsed Crc16Encap into: ${encapsulatedCommand}")
		zwaveEvent(encapsulatedCommand)
	} else {
		log.warn "Could not extract crc16 command from $cmd"
	}
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand(cmdVersions())
	if (encapsulatedCommand) {
		logging("${device.displayName} - Parsed MultiChannelCmdEncap ${encapsulatedCommand}")
		zwaveEvent(encapsulatedCommand, cmd.sourceEndPoint as Integer)
	} else {
		log.warn "Unable to extract MultiChannel command from $cmd"
	}
}

private logging(text, type = "debug") {
	if (settings.logging == "true") {
		log."$type" text
	}
}

private secEncap(physicalgraph.zwave.Command cmd) {
	logging("${device.displayName} - encapsulating command using Secure Encapsulation, command: $cmd","info")
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(physicalgraph.zwave.Command cmd) {
	logging("${device.displayName} - encapsulating command using CRC16 Encapsulation, command: $cmd","info")
	zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format() 
}

private encap(physicalgraph.zwave.Command cmd) {
	if (zwaveInfo.zw.contains("s")) { 
		secEncap(cmd)
	} else if (zwaveInfo.cc.contains("56")){ 
		crcEncap(cmd)
	} else {
		logging("${device.displayName} - no encapsulation supported for command: $cmd","info")
		cmd.format()
	}
}

private encapSequence(cmds, Integer delay=250) {
	delayBetween(cmds.collect{ encap(it) }, delay)
}

private List intToParam(Long value, Integer size = 1) {
	def result = []
	size.times { 
		result = result.plus(0, (value & 0xFF) as Short)
		value = (value >> 8)
	}
	return result
}

private Map cmdVersions() {
	[0x5E: 2, 0x59: 1, 0x22: 1, 0x80: 1, 0x56: 1, 0x7A: 3, 0x73: 1, 0x98: 1, 0x31: 5, 0x85: 2, 0x70: 2, 0x5A: 1, 0x72: 2, 0x8E: 2, 0x71: 2, 0x86: 1, 0x84: 2]
}
