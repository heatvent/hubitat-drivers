/**
 *  IMPORT URL: https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/Relay-Garage-Door/Relay-Garage-Door-No-Battery.groovy
 *
 *  Relay Garage Door (No Battery) (device)
 *
 *  The garage door device created by the Relay Garage Door app. It combines a relay
 *  (e.g. Zooz ZEN16 MultiRelay) and a tilt, wireless or wired door sensor into one device
 *  with Garage Door Control and Contact Sensor. The app picks this driver when no sensor
 *  has a battery (e.g. a wired reed switch), so Home Assistant shows no empty battery sensor.
 *
 *  Exposed through Maker API, the Home Assistant Hubitat integration shows it as a
 *  garage-door cover (open / opening / closing / closed) and a contact binary sensor
 *  on a single device.
 *
 *  This driver has no logic of its own: the app sends its events and runs its commands.
 *  Install the Relay Garage Door app and let it create this device.
 *
 *  Author:    Heatvent
 *  Namespace: Heatvent
 *
 *  ---------------------------------------------------------------------------------------------------
 *  1.2.0 (2026-11-22) - Version bump with the app (low battery reminder).
 *  1.1.0 (2026-11-22) - Initial release (for wired sensors without a battery).
 */

import groovy.transform.Field

@Field static final String DRIVER_VERSION = "1.2.0"

metadata {
    definition(
        name: "Relay Garage Door (No Battery)",
        namespace: "Heatvent",
        author: "Heatvent",
        importUrl: "https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/Relay-Garage-Door/Relay-Garage-Door-No-Battery.groovy"
    ) {
        capability "Actuator"
        capability "Sensor"
        capability "GarageDoorControl"
        capability "ContactSensor"
        capability "Refresh"

        // Hubitat-only details (the Home Assistant integration ignores custom attributes).
        attribute "lastOpened", "string"
        attribute "lastClosed", "string"
        attribute "lastResult", "string"
        attribute "movedBy", "enum", ["hubitat", "manual"]
    }

    preferences {
        input name: "txtEnable", type: "bool", title: "Enable description text logging", defaultValue: true
    }
}

void installed() {
    device.updateDataValue("driverVersion", DRIVER_VERSION)
}

void updated() {
    device.updateDataValue("driverVersion", DRIVER_VERSION)
}

void open() {
    if (parent == null) {
        log.warn "${device.displayName}: no parent app; install the Relay Garage Door app to create this device"
        return
    }
    parent.componentOpen(device)
}

void close() {
    if (parent == null) {
        log.warn "${device.displayName}: no parent app; install the Relay Garage Door app to create this device"
        return
    }
    parent.componentClose(device)
}

void refresh() {
    parent?.componentRefresh(device)
}

/** Events from the parent app: a list of sendEvent maps. */
void parse(List<Map> events) {
    events.each { Map evt ->
        if (evt.descriptionText == null && evt.name in ["door", "contact"]) {
            String unit = evt.unit ?: ""
            evt.descriptionText = "${device.displayName} ${evt.name} is ${evt.value}${unit}"
        }
        if (txtEnable && evt.descriptionText && device.currentValue(evt.name as String)?.toString() != evt.value?.toString()) {
            log.info evt.descriptionText
        }
        sendEvent(evt)
    }
}

void parse(String description) {
    log.warn "${device.displayName}: parse(String) is not used by this driver"
}
