/**
 *  IMPORT URL: https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Apps/Relay-Garage-Door/Relay-Garage-Door.groovy
 *
 *  Relay Garage Door
 *
 *  Turns a relay (e.g. Zooz ZEN16 MultiRelay) and a tilt or contact sensor (e.g. Zooz
 *  ZSE43 Tilt-Shock XS) into one garage door device:
 *    - door: open / opening / closing / closed / unknown, also for doors moved by a
 *      wall button or remote
 *    - contact and battery mirrored from the sensor
 *    - open / close pulse the relay, and do nothing when the door is already there
 *    - a notification when the door does not reach the requested position
 *
 *  Install one instance per door. Each instance creates a "Relay Garage Door" device.
 *
 *  Author:    Heatvent
 *  Namespace: Heatvent
 *
 *  ---------------------------------------------------------------------------------------------------
 *  1.0.0 (2026-11-22) - Initial release.
 */

import groovy.transform.Field

@Field static final String APP_VERSION = "1.0.0"
@Field static final String CHILD_DRIVER = "Relay Garage Door"
@Field static final String CHILD_NAMESPACE = "Heatvent"

definition(
    name: "Relay Garage Door",
    namespace: "Heatvent",
    author: "Heatvent",
    description: "Combine a relay (Zooz ZEN16) and a tilt sensor into one garage door opener device.",
    category: "Convenience",
    singleInstance: false,
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Apps/Relay-Garage-Door/Relay-Garage-Door.groovy",
    documentationLink: "https://github.com/heatvent/hubitat-drivers#relay-garage-door"
)

preferences {
    page(name: "mainPage")
}

Map mainPage() {
    dynamicPage(name: "mainPage", title: "Relay Garage Door ${APP_VERSION}", install: true, uninstall: true) {
        section("Door") {
            input "doorName", "text", title: "Garage door name", required: true, defaultValue: "Garage Door",
                description: "Name of the garage door device this app creates"
        }
        section("Devices") {
            input "relaySwitch", "capability.switch", title: "Relay that presses the opener button", required: true,
                description: "For a ZEN16, pick the relay's child switch (e.g. Relay 1)"
            input "contactSensor", "capability.contactSensor", title: "Tilt or contact sensor on the door", required: true,
                submitOnChange: true
            input "invertContact", "bool", title: "Sensor reports open when the door is closed", defaultValue: false
            input "batterySensor", "capability.battery", title: "Battery to show (optional, defaults to the door sensor)",
                required: false
        }
        section("Timing") {
            input "autoOffSeconds", "enum", title: "Turn the relay off after", defaultValue: "1",
                options: ["0": "Never (relay turns itself off)", "1": "1 second", "2": "2 seconds", "3": "3 seconds", "5": "5 seconds"],
                description: "A ZEN16 relay with Auto Turn-Off (or Garage Door input mode) turns itself off; 1 second is a safe extra"
            input "travelSeconds", "number", title: "Seconds the door takes to open or close", defaultValue: 15, range: "5..120",
                required: true
            input "warnSeconds", "number", title: "Seconds to wait before pressing (for a warning light or chime)", defaultValue: 0,
                range: "0..30", required: false
        }
        section("Manual operation") {
            paragraph "A door moved by a wall button or remote shows <b>opening</b> as soon as the sensor opens and " +
                "<b>closed</b> when it closes. A tilt sensor only changes at the end of a close, so the door shows " +
                "<b>open</b> while it closes, unless vibration can be used."
            if (contactSensor?.hasAttribute("shock") || contactSensor?.hasAttribute("acceleration")) {
                input "useVibration", "bool", title: "Show closing when the open door's sensor detects vibration",
                    defaultValue: false,
                    description: "Uses the sensor's shock/acceleration reports. Leave off if bumps or wind set it off."
            }
        }
        section("Notifications") {
            input "notifyDevices", "capability.notification", title: "Notify when the door doesn't open or close",
                multiple: true, required: false
        }
        section("Logging") {
            input "logEnable", "bool", title: "Enable debug logging (turns off after 30 minutes)", defaultValue: false
        }
    }
}

// ----------------------------------------------------------------------------------------------------
// Lifecycle
// ----------------------------------------------------------------------------------------------------

void installed() {
    initialize()
}

void updated() {
    unsubscribe()
    unschedule()
    initialize()
}

void uninstalled() {
    getChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

void initialize() {
    app.updateLabel(doorName ?: "Relay Garage Door")
    state.appVersion = APP_VERSION
    state.remove("pending")
    state.remove("manual")

    def door = doorDevice()
    if (door == null) {
        door = addChildDevice(CHILD_NAMESPACE, CHILD_DRIVER, "${app.id}-door",
            [label: doorName ?: "Garage Door", isComponent: false])
        log.info "Created garage door device ${door.displayName}"
    } else if (doorName && door.label != doorName && state.lastDoorName != doorName) {
        door.setLabel(doorName)
    }
    state.lastDoorName = doorName

    subscribe(contactSensor, "contact", "contactHandler")
    def battery = batteryDevice()
    if (battery?.hasAttribute("battery")) {
        subscribe(battery, "battery", "batteryHandler")
    }
    if (useVibration) {
        if (contactSensor.hasAttribute("shock")) subscribe(contactSensor, "shock.detected", "vibrationHandler")
        if (contactSensor.hasAttribute("acceleration")) subscribe(contactSensor, "acceleration.active", "vibrationHandler")
    }
    if (logEnable) runIn(1800, "logsOff")

    syncDoor()
}

void logsOff() {
    app.updateSetting("logEnable", [value: "false", type: "bool"])
    log.info "${app.label}: debug logging turned off"
}

// ----------------------------------------------------------------------------------------------------
// Commands from the door device
// ----------------------------------------------------------------------------------------------------

void componentOpen(cd) {
    operate("open")
}

void componentClose(cd) {
    operate("closed")
}

void componentRefresh(cd) {
    if (contactSensor.hasCommand("refresh")) contactSensor.refresh()
    if (state.pending == null && state.manual == null) syncDoor()
    sendBattery()
}

/** target is "open" or "closed". */
private void operate(String target) {
    String moving = target == "open" ? "opening" : "closing"
    String door = currentDoor()
    String sensed = sensorState()

    if (door == moving) {
        logDebug "Already ${moving}"
        return
    }
    if (state.pending == null && door != "opening" && door != "closing" && sensed == target) {
        logDebug "Already ${target}"
        setDoor(target)
        return
    }
    if (door == "opening" && target == "closed") {
        // A press while opening stops most openers instead of closing them.
        log.warn "${app.label}: the door is still opening; close it once it stops"
        return
    }

    // Idle, or closing and asked to open (a press reverses a closing door).
    state.pending = target
    state.remove("manual")
    unschedule("finishManual")
    setDoor(moving, "hubitat")

    Integer warn = (warnSeconds ?: 0) as Integer
    if (warn > 0) {
        runIn(warn, "pressButton")
    } else {
        pressButton()
    }
    runIn(warn + travel(), "checkResult")
}

void pressButton() {
    logDebug "Pressing ${relaySwitch.displayName}"
    relaySwitch.on()
    Integer off = (autoOffSeconds ?: "0") as Integer
    if (off > 0) runIn(off, "releaseButton")
}

void releaseButton() {
    if (relaySwitch.currentValue("switch") != "off") relaySwitch.off()
}

void checkResult() {
    String target = state.pending
    state.remove("pending")
    if (target == null) return

    String sensed = sensorState()
    if (sensed == target) {
        setDoor(target)
        sendResult("ok")
        return
    }
    String verb = target == "open" ? "open" : "close"
    String msg = "${app.label} did not ${verb} (sensor reports ${sensed})"
    log.warn msg
    setDoor(sensed)
    sendResult("failed to ${verb}")
    notifyDevices?.each { it.deviceNotification(msg) }
}

// ----------------------------------------------------------------------------------------------------
// Sensor events
// ----------------------------------------------------------------------------------------------------

void contactHandler(evt) {
    String sensed = sensorState()
    if (sensed == "unknown") return
    sendToDoor([[name: "contact", value: sensed]])
    logDebug "Sensor ${evt.value} -> ${sensed} (pending: ${state.pending}, manual: ${state.manual})"

    if (state.pending != null) {
        if (state.pending == "closed" && sensed == "closed") {
            // A tilt sensor closes at the end of travel: the door is down.
            unschedule("checkResult")
            state.remove("pending")
            setDoor("closed")
            sendResult("ok")
        }
        // Opening: the sensor opens as the door starts moving; keep "opening" until travel time ends.
        return
    }

    // Wall button, remote, or keypad.
    if (sensed == "open") {
        if (currentDoor() in ["open", "opening"]) return
        state.manual = "open"
        setDoor("opening", "manual")
        runIn(travel(), "finishManual")
    } else {
        state.remove("manual")
        unschedule("finishManual")
        setDoor("closed", state.manual == null && currentDoor() == "closed" ? null : "manual")
    }
}

void vibrationHandler(evt) {
    if (state.pending != null || state.manual != null) return
    if (currentDoor() != "open" || sensorState() != "open") return
    logDebug "Vibration while open: treating as a manual close"
    state.manual = "closed"
    setDoor("closing", "manual")
    runIn(travel(), "finishManual")
}

void finishManual() {
    state.remove("manual")
    if (state.pending == null) setDoor(sensorState())
}

void batteryHandler(evt) {
    sendBattery()
}

// ----------------------------------------------------------------------------------------------------
// Helpers
// ----------------------------------------------------------------------------------------------------

private def doorDevice() {
    getChildDevice("${app.id}-door")
}

private def batteryDevice() {
    batterySensor ?: (contactSensor?.hasAttribute("battery") ? contactSensor : null)
}

private Integer travel() {
    Integer secs = (travelSeconds ?: 15) as Integer
    return Math.max(5, secs)
}

private String sensorState() {
    String raw = contactSensor?.currentValue("contact")
    if (raw != "open" && raw != "closed") return "unknown"
    if (invertContact) return raw == "open" ? "closed" : "open"
    return raw
}

private String currentDoor() {
    return doorDevice()?.currentValue("door") ?: "unknown"
}

private void syncDoor() {
    String sensed = sensorState()
    if (sensed != "unknown") sendToDoor([[name: "contact", value: sensed]])
    setDoor(sensed)
    sendBattery()
}

private void setDoor(String value, String movedBy = null) {
    List<Map> events = [[name: "door", value: value]]
    String stamp = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    String previous = currentDoor()
    if (value == "open" && previous != "open") events << [name: "lastOpened", value: stamp]
    if (value == "closed" && previous != "closed") events << [name: "lastClosed", value: stamp]
    if (movedBy != null) events << [name: "movedBy", value: movedBy]
    sendToDoor(events)
}

private void sendResult(String result) {
    sendToDoor([[name: "lastResult", value: result]])
}

private void sendBattery() {
    def battery = batteryDevice()
    def level = battery?.currentValue("battery")
    if (level != null) sendToDoor([[name: "battery", value: level as Integer, unit: "%"]])
}

private void sendToDoor(List<Map> events) {
    doorDevice()?.parse(events)
}

private void logDebug(String msg) {
    if (logEnable) log.debug "${app.label}: ${msg}"
}
