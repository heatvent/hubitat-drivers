/**
 *  IMPORT URL: https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Apps/Relay-Garage-Door/Relay-Garage-Door.groovy
 *
 *  Relay Garage Door
 *
 *  Turns a relay (e.g. Zooz ZEN16 MultiRelay) and a door sensor into one garage door device.
 *  The door sensor can be a tilt sensor (e.g. Zooz ZSE43), a wireless contact sensor, or a
 *  wired contact (e.g. a reed switch on a ZEN16/ZEN17 input).
 *    - door: open / opening / closing / closed / unknown, also for doors moved by a
 *      wall button or remote
 *    - optional second sensor at the fully-open position for exact open/closing states
 *    - contact and battery mirrored from the sensor
 *    - open / close pulse the relay, and do nothing when the door is already there
 *    - optional warning (buzzer on a relay, or a chime) before and while the door moves
 *    - a notification when the door does not reach the requested position
 *
 *  Install one instance per door. Each instance creates a "Relay Garage Door" device
 *  ("Relay Garage Door (No Battery)" when no sensor has a battery).
 *
 *  Author:    Heatvent
 *  Namespace: Heatvent
 *
 *  ---------------------------------------------------------------------------------------------------
 *  1.2.1 (2026-11-22) - Low battery reminder can also repeat every 5, 15 or 30 minutes.
 *  1.2.0 (2026-11-22) - Low battery reminder: a notification when the sensor's battery drops below a
 *                       level, then a short chime (or buzzer chirp) every few hours within set hours.
 *                       It never sounds while the door moves; a door warning always takes over.
 *  1.1.0 (2026-11-22) - Sensor type (tilt / wireless contact / wired contact; a wired input can report
 *                       as a contact or a switch), optional fully-open sensor, warning buzzer or chime
 *                       before and during movement, no-battery device for wired sensors.
 *  1.0.0 (2026-11-22) - Initial release.
 */

import groovy.transform.Field

@Field static final String APP_VERSION = "1.2.1"
@Field static final String CHILD_DRIVER = "Relay Garage Door"
@Field static final String CHILD_DRIVER_NO_BATTERY = "Relay Garage Door (No Battery)"
@Field static final String CHILD_NAMESPACE = "Heatvent"

@Field static final Map SENSOR_TYPES = [
    tilt    : "Tilt sensor on the top panel",
    wireless: "Wireless contact sensor (closed when the door is down)",
    wired   : "Wired contact (e.g. reed switch on a ZEN16 input)"
]

definition(
    name: "Relay Garage Door",
    namespace: "Heatvent",
    author: "Heatvent",
    description: "Combine a relay (Zooz ZEN16) and a tilt, wireless or wired door sensor into one garage door opener device.",
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
            input "relaySwitch", "capability.switch", title: "Relay that presses the opener button", required: true,
                description: "For a ZEN16, pick the relay's child switch (e.g. Relay 1)"
        }
        section("Door closed sensor") {
            input "sensorType", "enum", title: "Sensor type", options: SENSOR_TYPES, defaultValue: "tilt",
                required: true, submitOnChange: true
            if (isWired()) {
                input "contactSensor", "capability.*", title: "Wired sensor (contact or switch)", required: true,
                    submitOnChange: true,
                    description: "A ZEN16 input shows up as a child contact sensor or switch; pick that child"
                paragraph "A wired input that reports as a switch counts as <b>closed</b> when it is <b>on</b> " +
                    "(circuit closed by the magnet). On a ZEN16, set the input's <i>Input Control</i> to " +
                    "<i>Report Input Status Only</i> so it doesn't also switch its relay."
                input "invertContact", "bool", title: "Sensor reports open/off when the door is closed", defaultValue: false
            } else {
                input "contactSensor", "capability.contactSensor",
                    title: sensorType == "wireless" ? "Contact sensor at the bottom of the door" : "Tilt sensor on the door",
                    required: true, submitOnChange: true
                input "invertContact", "bool", title: "Sensor reports open when the door is closed", defaultValue: false
            }
        }
        section("Door fully open sensor (optional)") {
            paragraph "A second sensor where the door stops when fully open (e.g. a reed switch on the track). " +
                "With it, the door shows <b>open</b> when it really is, and <b>closing</b> as soon as a door moved " +
                "by a wall button or remote leaves the open position."
            input "openSensor", "capability.*", title: "Fully open sensor (contact or switch)", required: false,
                submitOnChange: true
            if (openSensor) {
                input "invertOpen", "bool",
                    title: "Sensor reports open/off when the door is fully open (instead of closed/on)", defaultValue: false
            }
        }
        section("Battery") {
            input "batterySensor", "capability.battery", title: "Battery to show (optional, defaults to a door sensor's battery)",
                required: false, submitOnChange: true
            if (doorDevice() != null && wantsBattery() != hasBatteryDriver()) {
                paragraph "<b>Note:</b> the door device was created ${hasBatteryDriver() ? 'with' : 'without'} a battery. " +
                    "To change that, remove this app and add it again (Home Assistant will see a new device)."
            }
        }
        section("Timing") {
            input "autoOffSeconds", "enum", title: "Turn the relay off after", defaultValue: "1",
                options: ["0": "Never (relay turns itself off)", "1": "1 second", "2": "2 seconds", "3": "3 seconds", "5": "5 seconds"],
                description: "A ZEN16 relay with Auto Turn-Off (or Garage Door input mode) turns itself off; 1 second is a safe extra"
            input "travelSeconds", "number", title: "Seconds the door takes to open or close", defaultValue: 15, range: "5..120",
                required: true
        }
        section("Warning before the door moves") {
            paragraph "Sound a buzzer or chime before Hubitat moves the door and while it moves. A 12 V buzzer can be " +
                "wired through a spare ZEN16 relay (e.g. Relay 2) from the ZEN16's own 12 V supply."
            input "warningSwitch", "capability.switch", title: "Buzzer or strobe relay (on while warning)", required: false
            input "warningChime", "capability.chime", title: "Chime or siren to play a sound", required: false,
                submitOnChange: true
            if (warningChime) {
                input "chimeSound", "number", title: "Sound number to play", defaultValue: 1, required: false
            }
            input "warnOn", "enum", title: "Warn when", defaultValue: "close",
                options: ["close": "Closing", "both": "Opening and closing"]
            input "warnSeconds", "number", title: "Seconds to warn before pressing the opener", defaultValue: 0,
                range: "0..30", required: false, description: "5 seconds is typical when a warning device is set"
            input "warnWhileMoving", "bool", title: "Keep warning until the door stops", defaultValue: true
        }
        if (batterySensor || contactSensor?.hasAttribute("battery") || openSensor?.hasAttribute("battery")) {
            section("Low battery reminder") {
                paragraph "When the sensor's battery drops below the level, the notification devices get a message. " +
                    "Then the chime plays a short sound (its LED flashes with it), or the buzzer relay chirps, on the " +
                    "schedule below until the battery is replaced. A short chirp that differs from the door warning works best."
                input "lowBatteryLevel", "number", title: "Battery level (%) that counts as low (0 = off)",
                    defaultValue: 20, range: "0..90", required: false
                if (warningChime) {
                    input "lowBatterySound", "number", title: "Chime sound number for the reminder", required: false,
                        description: "Pick a different sound from the door warning"
                }
                input "lowBatteryHours", "enum", title: "Remind every", defaultValue: "4",
                    options: ["5m": "5 minutes", "15m": "15 minutes", "30m": "30 minutes", "1": "1 hour", "2": "2 hours", "4": "4 hours", "8": "8 hours", "12": "12 hours", "24": "24 hours"]
                input "lowBatteryFrom", "time", title: "Only beep after (optional)", required: false
                input "lowBatteryTo", "time", title: "Only beep before (optional)", required: false
            }
        }
        section("Manual operation") {
            paragraph "A door moved by a wall button or remote shows <b>opening</b> as soon as the closed sensor opens and " +
                "<b>closed</b> when it closes. Without a fully open sensor, it shows <b>open</b> while it closes, " +
                "unless a tilt sensor's vibration can be used."
            if (contactSensor?.hasAttribute("shock") || contactSensor?.hasAttribute("acceleration")) {
                input "useVibration", "bool", title: "Show closing when the open door's sensor detects vibration",
                    defaultValue: false,
                    description: "Uses the sensor's shock/acceleration reports. Leave off if bumps or wind set it off."
            }
        }
        section("Notifications") {
            input "notifyDevices", "capability.notification", title: "Notify when the door doesn't open or close, or the battery is low",
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
    stopWarning()
    initialize()
}

void uninstalled() {
    getChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

void initialize() {
    app.updateLabel(doorName ?: "Relay Garage Door")
    state.appVersion = APP_VERSION
    state.remove("pending")
    state.remove("pressed")
    state.remove("manual")

    def door = doorDevice()
    if (door == null) {
        String driver = wantsBattery() ? CHILD_DRIVER : CHILD_DRIVER_NO_BATTERY
        door = addChildDevice(CHILD_NAMESPACE, driver, "${app.id}-door",
            [label: doorName ?: "Garage Door", isComponent: false])
        log.info "Created garage door device ${door.displayName} (${driver})"
    } else if (doorName && door.label != doorName && state.lastDoorName != doorName) {
        door.setLabel(doorName)
    }
    state.lastDoorName = doorName

    subscribeSensor(contactSensor, "closedSensorHandler")
    if (openSensor) subscribeSensor(openSensor, "openSensorHandler")
    def battery = batteryDevice()
    if (battery?.hasAttribute("battery")) {
        subscribe(battery, "battery", "batteryHandler")
    }
    if (useVibration && contactSensor) {
        if (contactSensor.hasAttribute("shock")) subscribe(contactSensor, "shock.detected", "vibrationHandler")
        if (contactSensor.hasAttribute("acceleration")) subscribe(contactSensor, "acceleration.active", "vibrationHandler")
    }
    if (logEnable) runIn(1800, "logsOff")

    syncDoor()
    checkBattery()
    // updated() cleared the schedule; keep reminding if the battery is still low.
    if (state.lowBattery) runIn(reminderSeconds(), "lowBatteryBeep")
}

private void subscribeSensor(dev, String handler) {
    if (dev == null) return
    if (dev.hasAttribute("contact")) {
        subscribe(dev, "contact", handler)
    } else if (dev.hasAttribute("switch")) {
        subscribe(dev, "switch", handler)
    } else {
        log.warn "${app.label}: ${dev.displayName} reports neither contact nor switch"
    }
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
    [contactSensor, openSensor].each { if (it?.hasCommand("refresh")) it.refresh() }
    if (state.pending == null && state.manual == null) syncDoor()
    sendBattery()
}

/** target is "open" or "closed". */
private void operate(String target) {
    String moving = target == "open" ? "opening" : "closing"
    String door = currentDoor()

    if (door == moving) {
        logDebug "Already ${moving}"
        return
    }
    if (state.pending != null && !state.pressed) {
        // Still warning and the opener hasn't been pressed: the door never moved, so just cancel.
        log.info "${app.label}: ${state.pending == 'open' ? 'open' : 'close'} cancelled before the door moved"
        unschedule("pressButton")
        state.remove("pending")
        unschedule("checkResult")
        stopWarning()
        setDoor(restingState())
        sendResult("cancelled")
        return
    }
    if (state.pending == null && door != "opening" && door != "closing" && atPosition(target)) {
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
    state.remove("pressed")
    state.remove("manual")
    unschedule("finishManual")
    setDoor(moving, "hubitat")

    Integer warn = 0
    if (shouldWarn(target)) {
        startWarning()
        warn = (warnSeconds ?: 0) as Integer
    }
    if (warn > 0) {
        runIn(warn, "pressButton")
    } else {
        pressButton()
    }
    runIn(warn + travel(), "checkResult")
}

void pressButton() {
    if (state.pending == null) return
    logDebug "Pressing ${relaySwitch.displayName}"
    state.pressed = true
    relaySwitch.on()
    Integer off = (autoOffSeconds ?: "0") as Integer
    if (off > 0) runIn(off, "releaseButton")
    if (!warnWhileMoving) stopWarning()
}

void releaseButton() {
    if (relaySwitch.currentValue("switch") != "off") relaySwitch.off()
}

void checkResult() {
    String target = state.pending
    if (target == null) return
    finishPending(atPosition(target) ? target : null)
}

/** Ends a Hubitat-commanded move. reached is the target, or null when it was not reached. */
private void finishPending(String reached) {
    String target = state.pending
    state.remove("pending")
    state.remove("pressed")
    unschedule("checkResult")
    stopWarning()
    if (target == null) return

    if (reached != null) {
        setDoor(reached)
        sendResult("ok")
        return
    }
    String verb = target == "open" ? "open" : "close"
    String actual = restingState()
    boolean partly = actual == "open" && openSensor != null && !openSensorActive()
    String msg = "${app.label} did not ${verb} (door is ${partly ? 'partly open' : actual})"
    log.warn msg
    setDoor(actual)
    sendResult("failed to ${verb}")
    notifyDevices?.each { it.deviceNotification(msg) }
}

// ----------------------------------------------------------------------------------------------------
// Sensor events
// ----------------------------------------------------------------------------------------------------

void closedSensorHandler(evt) {
    String sensed = closedSensorState()
    if (sensed == "unknown") return
    sendToDoor([[name: "contact", value: sensed]])
    logDebug "Closed sensor ${evt.value} -> ${sensed} (pending: ${state.pending}, manual: ${state.manual})"

    if (state.pending != null) {
        // The closed sensor closes at the end of travel: the door is down.
        if (state.pending == "closed" && sensed == "closed") finishPending("closed")
        // Opening: the sensor opens as the door starts moving; keep "opening" until it is open.
        return
    }

    // Wall button, remote, or keypad.
    if (sensed == "open") {
        if (currentDoor() in ["open", "opening"]) return
        state.manual = "open"
        setDoor("opening", "manual")
        runIn(travel(), "finishManual")
    } else {
        boolean moved = state.manual != null || currentDoor() != "closed"
        state.remove("manual")
        unschedule("finishManual")
        setDoor("closed", moved ? "manual" : null)
    }
}

void openSensorHandler(evt) {
    Boolean fullyOpen = openSensorActive()
    if (fullyOpen == null) return
    logDebug "Open sensor ${evt.value} -> fully open: ${fullyOpen} (pending: ${state.pending}, manual: ${state.manual})"

    if (state.pending != null) {
        if (state.pending == "open" && fullyOpen) finishPending("open")
        return
    }
    if (fullyOpen) {
        state.remove("manual")
        unschedule("finishManual")
        setDoor("open", currentDoor() == "open" ? null : "manual")
    } else if (currentDoor() == "open" && closedSensorState() == "open") {
        // Left the open position without a Hubitat command: someone is closing it.
        state.manual = "closed"
        setDoor("closing", "manual")
        runIn(travel(), "finishManual")
    }
}

void vibrationHandler(evt) {
    if (state.pending != null || state.manual != null) return
    if (currentDoor() != "open" || closedSensorState() != "open") return
    if (openSensor && openSensorActive()) return   // still parked fully open
    logDebug "Vibration while open: treating as a manual close"
    state.manual = "closed"
    setDoor("closing", "manual")
    runIn(travel(), "finishManual")
}

void finishManual() {
    state.remove("manual")
    if (state.pending == null) setDoor(restingState())
}

void batteryHandler(evt) {
    sendBattery()
    checkBattery()
}

// ----------------------------------------------------------------------------------------------------
// Low battery reminder
// ----------------------------------------------------------------------------------------------------

private void checkBattery() {
    Integer threshold = (lowBatteryLevel ?: 0) as Integer
    def level = batteryDevice()?.currentValue("battery")
    if (threshold <= 0 || level == null) {
        clearLowBattery()
        return
    }
    Integer pct = level as Integer
    if (pct < threshold) {
        if (!state.lowBattery) {
            state.lowBattery = true
            String msg = "${app.label}: sensor battery is low (${pct}%)"
            log.warn msg
            notifyDevices?.each { it.deviceNotification(msg) }
            lowBatteryBeep()
        }
    } else if (pct >= threshold + 5) {
        // A little above the threshold so a reading that hovers doesn't flip it on and off.
        clearLowBattery()
    }
}

private void clearLowBattery() {
    if (!state.lowBattery) return
    state.remove("lowBattery")
    unschedule("lowBatteryBeep")
    log.info "${app.label}: sensor battery OK; low battery reminder off"
}

void lowBatteryBeep() {
    if (!state.lowBattery) return
    runIn(reminderSeconds(), "lowBatteryBeep")

    if (lowBatteryFrom && lowBatteryTo &&
            !timeOfDayIsBetween(toDateTime(lowBatteryFrom), toDateTime(lowBatteryTo), new Date(), location.timeZone)) {
        logDebug "Low battery beep skipped: outside the beep hours"
        return
    }
    if (state.pending != null || state.manual != null || state.warning) {
        // Never sound while the door moves; try again in a few minutes.
        runIn(300, "lowBatteryBeep")
        return
    }
    if (warningChime && lowBatterySound) {
        if (warningChime.currentValue("status") == "playing") {
            runIn(300, "lowBatteryBeep")   // something else is using the chime
            return
        }
        logDebug "Low battery beep on ${warningChime.displayName}"
        state.batteryBeep = true
        warningChime.playSound((lowBatterySound as Integer))
        runIn(3, "endLowBatteryBeep")
    } else if (warningSwitch) {
        logDebug "Low battery chirp on ${warningSwitch.displayName}"
        state.batteryBeep = true
        warningSwitch.on()
        runInMillis(400, "endLowBatteryBeep")
    }
}

/** "5m" / "15m" / "30m" are minutes; plain numbers are hours. */
private Integer reminderSeconds() {
    String v = (lowBatteryHours ?: "4").toString()
    return v.endsWith("m") ? (v[0..-2] as Integer) * 60 : (v as Integer) * 3600
}

void endLowBatteryBeep() {
    if (!state.batteryBeep) return
    state.remove("batteryBeep")
    if (state.warning) return   // a door warning took over the chime or buzzer
    if (warningChime && lowBatterySound) {
        if (warningChime.hasCommand("stop")) warningChime.stop()
    } else {
        warningSwitch?.off()
    }
}

// ----------------------------------------------------------------------------------------------------
// Warning device
// ----------------------------------------------------------------------------------------------------

private boolean shouldWarn(String target) {
    if (!warningSwitch && !warningChime) return false
    return target == "closed" || warnOn == "both"
}

private void startWarning() {
    state.remove("batteryBeep")
    state.warning = true
    warningSwitch?.on()
    if (warningChime) warningChime.playSound(((chimeSound ?: 1) as Integer))
}

private void stopWarning() {
    if (!state.warning) return
    state.remove("warning")
    warningSwitch?.off()
    if (warningChime?.hasCommand("stop")) warningChime.stop()
}

// ----------------------------------------------------------------------------------------------------
// Helpers
// ----------------------------------------------------------------------------------------------------

private def doorDevice() {
    getChildDevice("${app.id}-door")
}

private boolean isWired() {
    return sensorType == "wired"
}

private def batteryDevice() {
    if (batterySensor) return batterySensor
    if (contactSensor?.hasAttribute("battery")) return contactSensor
    if (openSensor?.hasAttribute("battery")) return openSensor
    return null
}

private boolean wantsBattery() {
    return batteryDevice() != null
}

private boolean hasBatteryDriver() {
    return doorDevice()?.hasAttribute("battery") ?: false
}

private Integer travel() {
    Integer secs = (travelSeconds ?: 15) as Integer
    return Math.max(5, secs)
}

/** "closed" when the contact is made (contact closed, or switch on), "open", or null. */
private String rawState(dev) {
    if (dev == null) return null
    if (dev.hasAttribute("contact")) {
        String c = dev.currentValue("contact")
        return c in ["open", "closed"] ? c : null
    }
    if (dev.hasAttribute("switch")) {
        String s = dev.currentValue("switch")
        return s == "on" ? "closed" : (s == "off" ? "open" : null)
    }
    return null
}

/** Closed sensor: "closed" when the door is down, "open" otherwise, or "unknown". */
private String closedSensorState() {
    String raw = rawState(contactSensor)
    if (raw == null) return "unknown"
    if (invertContact) return raw == "open" ? "closed" : "open"
    return raw
}

/** Fully open sensor: true when the door is fully open, false when not, null when unknown or not set. */
private Boolean openSensorActive() {
    String raw = rawState(openSensor)
    if (raw == null) return null
    boolean made = raw == "closed"
    return invertOpen ? !made : made
}

private boolean atPosition(String target) {
    String closed = closedSensorState()
    if (target == "closed") return closed == "closed"
    if (openSensor) {
        Boolean fullyOpen = openSensorActive()
        if (fullyOpen != null) return fullyOpen
    }
    return closed == "open"
}

/** Where the door is when nothing is moving it. A door stopped part way shows open. */
private String restingState() {
    String closed = closedSensorState()
    if (closed == "closed") return "closed"
    if (closed == "open") return "open"
    Boolean fullyOpen = openSensorActive()
    return fullyOpen ? "open" : "unknown"
}

private String currentDoor() {
    return doorDevice()?.currentValue("door") ?: "unknown"
}

private void syncDoor() {
    String sensed = closedSensorState()
    if (sensed != "unknown") sendToDoor([[name: "contact", value: sensed]])
    setDoor(restingState())
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
    if (!hasBatteryDriver()) return
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
