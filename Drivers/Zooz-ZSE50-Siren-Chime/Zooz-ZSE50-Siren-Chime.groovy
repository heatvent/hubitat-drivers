/**
 *  IMPORT URL: https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/Zooz-ZSE50-Siren-Chime/Zooz-ZSE50-Siren-Chime.groovy
 *
 *  Zooz ZSE50 Siren & Chime
 *
 *  Full-parameter community driver for the Zooz ZSE50 (Sound Switch + Configuration).
 *  Hubitat ships a built-in ZSE50 driver (2.4.3+); this driver exposes all Zooz config
 *  parameters (1–16), Sound Switch playback helpers, LED color/brightness, and Alarm/Chime UX.
 *
 *  Author:    Heatvent
 *  Namespace: Heatvent
 *
 *  ---------------------------------------------------------------------------------------------------
 *  2.3.0 (2026-11-28) - playSound takes an optional volume (1–100) for that one sound; the chime's own
 *                       volume (setVolume) stays as it is. Uses Sound Switch v2 Tone Play Set.
 *  2.2.1 (2026-11-22) - Fix: power source was reversed (unplugged showed mains, plugged in showed battery).
 *                       "Power applied" also counts as mains. Declares the PowerSource capability.
 *  2.2.0 (2026-11-22) - previewTones: plays every tone (or only custom / stock ones) for a few seconds
 *                       each, with a "preview" attribute showing which one is playing. Stop, or
 *                       playing another sound, ends the preview.
 *  2.1.1 (2026-11-22) - Tone labels no longer show a length: the ZSE50 reports lengths inaccurately
 *                       (a 6 s custom file as 1 s, stock lengths change between scans).
 *  2.1.0 (2026-11-22) - Readable tone list: reads each tone's file name and length from the device
 *                       (Sound Switch Tone Info) and fills soundEffects / toneList as "10 - Buzzer
 *                       Alarm (4s)"; custom MP3s get a name from their file name. refreshTones command.
 *                       Chime "status" attribute (playing / stopped). ping() for Health Check.
 *                       Commands accept numbers sent as text (Maker API, Rule Machine).
 *  2.0.0 (2026-11-14) - Rewrite: correct Zooz param map (1–16), S2 via zwaveSecureEncap,
 *                       fingerprints, Chime/Alarm/strobe, playSound, setVolume, LED color,
 *                       association group 2, Config V4, driverVersion Data Value.
 *  1.6.0              - Prior matt.custom / Copilot local driver (incorrect param 1–3 mapping).
 */

import groovy.transform.Field

@Field static final String DRIVER_VERSION = "2.3.0"

// Stock tone names by number, used until the device reports its files.
@Field static final Map TONE_NAMES = [
     1: "Door Open Alert"
    ,2: "Door Closed Alert"
    ,3: "Window Open Alert"
    ,4: "Window Closed Alert"
    ,5: "Motion Detected"
    ,6: "Motion Cleared"
    ,7: "Leak Detected"
    ,8: "Leak Cleared"
    ,9: "Ambulance"
    ,10: "Buzzer Alarm"
    ,11: "Cluster Chimes"
    ,12: "Doorbell 1"
    ,13: "Doorbell 2"
    ,14: "Doorbell 3"
    ,15: "Doorbell 4"
    ,16: "Doorbell 5"
    ,17: "Doorbell 6"
    ,18: "Fire Alarm"
    ,19: "Jingle Bells"
    ,20: "Microwave"
    ,21: "Old Doorbell"
    ,22: "Siren 1"
    ,23: "Siren 2"
    ,24: "Siren 3"
    ,25: "Tone"
]

// Stock file names (lower case) -> readable name. Custom files are named from their file name.
@Field static final Map STOCK_FILES = [
    "alert1_door-open.mp3": "Door Open Alert", "alert2_door-closed.mp3": "Door Closed Alert",
    "alert3_window-open.mp3": "Window Open Alert", "alert4_window-closed.mp3": "Window Closed Alert",
    "alert5_motion-detected.mp3": "Motion Detected", "alert6_motion-cleared.mp3": "Motion Cleared",
    "alert7_leak-detected.mp3": "Leak Detected", "alert8_leak-cleared.mp3": "Leak Cleared",
    "ambulance.mp3": "Ambulance", "buzzer-alarm.mp3": "Buzzer Alarm", "cluster-chimes.mp3": "Cluster Chimes",
    "doorbell1.mp3": "Doorbell 1", "doorbell2.mp3": "Doorbell 2", "doorbell3.mp3": "Doorbell 3",
    "doorbell4.mp3": "Doorbell 4", "doorbell5.mp3": "Doorbell 5", "doorbell6.mp3": "Doorbell 6",
    "fire-alarm.mp3": "Fire Alarm", "jingle-bells.mp3": "Jingle Bells", "microwave.mp3": "Microwave",
    "old-doorbell.mp3": "Old Doorbell", "siren1.mp3": "Siren 1", "siren2.mp3": "Siren 2",
    "siren3.mp3": "Siren 3", "tone.mp3": "Tone"
]

@Field static final Map LED_COLORS = [
    "Red": 0, "Yellow": 42, "Green": 85, "Indigo": 127, "Blue": 170, "Purple": 212, "White": 255
]

@Field static final Map commandClassVersions = [
     0x20: 2  // Basic
    ,0x25: 2  // Switch Binary
    ,0x55: 2  // Transport Service
    ,0x59: 3  // Association Group Info
    ,0x5A: 1  // Device Reset Locally
    ,0x5B: 3  // Central Scene
    ,0x5E: 2  // Z-Wave Plus Info
    ,0x6C: 1  // Supervision
    ,0x70: 4  // Configuration
    ,0x71: 8  // Notification
    ,0x72: 2  // Manufacturer Specific
    ,0x73: 1  // Powerlevel
    ,0x79: 1  // Sound Switch
    ,0x7A: 5  // Firmware Update MD
    ,0x80: 1  // Battery
    ,0x85: 2  // Association
    ,0x86: 3  // Version
    ,0x87: 3  // Indicator
    ,0x8E: 3  // Multi Channel Association
    ,0x9F: 1  // Security 2
]

metadata {
    definition(
        name: "Zooz ZSE50 Siren & Chime",
        namespace: "Heatvent",
        author: "Heatvent",
        importUrl: "https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/Zooz-ZSE50-Siren-Chime/Zooz-ZSE50-Siren-Chime.groovy"
    ) {
        capability "Actuator"
        capability "Alarm"
        capability "Battery"
        capability "Chime"
        capability "Configuration"
        capability "HealthCheck"
        capability "PowerSource"
        capability "Refresh"
        capability "Switch"

        attribute "soundName", "string"
        attribute "toneList", "string"
        attribute "volume", "number"
        attribute "ledColor", "string"
        attribute "toneCount", "number"
        attribute "preview", "string"

        command "playSound", [
            [name: "Tone Number*", type: "NUMBER", description: "Tone file number 1–50", range: "1..50"],
            [name: "Volume %", type: "NUMBER", description: "Optional, 1–100, for this sound only (blank = the chime's volume)", range: "1..100"]
        ]
        command "setVolume", [[name: "Volume %*", type: "NUMBER", description: "0=mute, 1–100", range: "0..100"]]
        command "setLedColor", [[name: "LED Color", type: "ENUM", constraints: ["Red", "Yellow", "Green", "Indigo", "Blue", "Purple", "White"]]]
        command "setDefaultTone", [[name: "Default Tone*", type: "NUMBER", description: "Param 4 tone number 1–50", range: "1..50"]]
        command "stop"
        command "testTone", [[name: "Tone Number", type: "NUMBER", description: "Alias for playSound", range: "1..50"]]
        command "refreshTones"
        command "previewTones", [
            [name: "Seconds per tone", type: "NUMBER", description: "1–10 (default 2)", range: "1..10"],
            [name: "Which tones", type: "ENUM", constraints: ["All", "Custom only", "Stock only"]]
        ]

        fingerprint mfr: "027A", prod: "0004", deviceId: "0369", inClusters: "0x5E,0x55,0x98,0x9F,0x6C,0x22,0x85,0x8E,0x59,0x87,0x72,0x5A,0x73,0x80,0x71,0x70,0x79,0x25,0x86,0x7A", controllerType: "ZWV"
        fingerprint mfr: "027A", prod: "0004", deviceId: "0369", controllerType: "ZWV"
    }

    preferences {
        input name: "infoEnable", type: "bool", title: "Enable descriptionText / info logging", defaultValue: true
        input name: "debugEnable", type: "bool", title: "Enable debug logging", defaultValue: false

        input name: "param1", type: "enum", title: "(1) Playback Mode", options: [
            "0": "Play once",
            "1": "Loop for duration (param 2)",
            "2": "Loop N times (param 3)",
            "3": "Loop until cancelled (default)",
            "4": "No sound — LED only"
        ], defaultValue: "3"

        input name: "param2", type: "number", title: "(2) Playback Duration (seconds, mode 1)", range: "1..900", defaultValue: 180
        input name: "param3", type: "number", title: "(3) Playback Loop Count (mode 2)", range: "1..99", defaultValue: 1
        input name: "param4", type: "number", title: "(4) Default Playback Tone (1–50)", range: "1..50", defaultValue: 1
        input name: "param5", type: "number", title: "(5) Playback Volume (0–100%)", range: "0..100", defaultValue: 100

        input name: "param6", type: "enum", title: "(6) LED Indicator Mode", options: [
            "0": "LED always off",
            "1": "Strobe single color (param 7) (default)",
            "2": "Strobe red and blue",
            "3": "Pulse single color (param 7)",
            "4": "Solid on single color (param 7)"
        ], defaultValue: "1"

        input name: "param7", type: "enum", title: "(7) LED Indicator Color", options: [
            "0": "Red (default)",
            "42": "Yellow",
            "85": "Green",
            "127": "Indigo",
            "170": "Blue",
            "212": "Purple",
            "255": "White"
        ], defaultValue: "0"

        input name: "param8", type: "number", title: "(8) Low Battery Threshold (%)", range: "10..50", defaultValue: 20

        input name: "param9", type: "enum", title: "(9) LED on Backup Battery", options: [
            "0": "LED off (save battery)",
            "1": "Regular LED mode (default)",
            "2": "Pulse white (full) / red (low)"
        ], defaultValue: "1"

        input name: "param10", type: "enum", title: "(10) Physical Tone Buttons", options: [
            "0": "Disabled",
            "1": "Enabled (default)"
        ], defaultValue: "1"

        input name: "param11", type: "enum", title: "(11) Physical Volume Buttons", options: [
            "0": "Disabled",
            "1": "Enabled (default)"
        ], defaultValue: "1"

        input name: "param12", type: "enum", title: "(12) Group 2 Basic Set Behavior", options: [
            "0": "Do not send Basic Set",
            "1": "ON on start, OFF on stop (default)",
            "2": "OFF on start, ON on stop",
            "3": "ON on start only",
            "4": "OFF on stop only",
            "5": "OFF on start only",
            "6": "ON on stop only"
        ], defaultValue: "1"

        input name: "param13", type: "number", title: "(13) System Message Volume (0–100%)", range: "0..100", defaultValue: 50
        input name: "param14", type: "number", title: "(14) LED Brightness (0–10)", range: "0..10", defaultValue: 5
        input name: "param15", type: "number", title: "(15) Battery Report Interval (hours)", range: "1..84", defaultValue: 12
        input name: "param16", type: "number", title: "(16) Battery Report Threshold % (0=off)", range: "0..20", defaultValue: 0

        input name: "assocGroup2", type: "text", title: "Association Group 2 Members (hex node IDs, max 5)", required: false
    }
}

void installed() {
    logInfo "Installed ${DRIVER_VERSION}"
    updateDataValue("driverVersion", DRIVER_VERSION)
    publishTones()
    sendEvent(name: "checkInterval", value: 12 * 60 * 60, displayed: false, data: [protocol: "zwave"])
    initialize()
}

void updated() {
    logInfo "Updated ${DRIVER_VERSION}"
    updateDataValue("driverVersion", DRIVER_VERSION)
    publishTones()
    if (debugEnable) {
        logDebug "Debug logging on; auto-off in 30 minutes"
        runIn(1800, "logsOff")
    }
    initialize()
}

void initialize() {
    sendEvent(name: "checkInterval", value: 12 * 60 * 60, displayed: false, data: [protocol: "zwave"])
    configure()
}

void configure() {
    logInfo "configure()"
    List cmds = []
    cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId))
    cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 1))

    List g2 = parseAssocGroupList(settings.assocGroup2, 5)
    cmds << secure(zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: []))
    if (g2) cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: g2))
    cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 2))

    cmds += configWriteCmds()
    cmds << secure(zwave.batteryV1.batteryGet())
    cmds << secure(zwave.versionV3.versionGet())
    cmds << secure(zwave.manufacturerSpecificV2.manufacturerSpecificGet())
    cmds << secure(zwave.soundSwitchV1.tonesNumberGet())
    sendCmds(cmds)
}

void refresh() {
    logInfo "refresh()"
    sendCmds([
        secure(zwave.batteryV1.batteryGet()),
        secure(zwave.switchBinaryV2.switchBinaryGet()),
        secure(zwave.soundSwitchV1.tonePlayGet()),
        secure(zwave.versionV3.versionGet()),
        // Is "AC mains disconnected" active? The answer is that event (on battery) or idle (on mains).
        secure(zwave.notificationV8.notificationGet(notificationType: 0x08, event: 0x02))
    ] + configGetCmds())
}

void ping() {
    logDebug "ping()"
    sendCmds([secure(zwave.switchBinaryV2.switchBinaryGet())])
}

/** Reads the number of tones, then each tone's file name and length. */
void refreshTones() {
    logInfo "refreshTones()"
    sendCmds([secure(zwave.soundSwitchV1.tonesNumberGet())])
}

void on() { siren() }

void off() { stop() }

void siren() {
    cancelPreview()
    playSound(prefInt("param4", 1))
}

void strobe() {
    // Param 1 = 4 is LED-only (no sound). stop() restores the configured playback mode.
    logInfo "strobe() - LED-only mode (param 1=4)"
    state.strobeActive = true
    List cmds = []
    cmds << cfgSet(1, 4, 1)
    cmds << secure(zwave.soundSwitchV1.tonePlaySet(toneIdentifier: (settings.param4 as Integer) ?: 1))
    sendCmds(cmds)
    sendEvent(name: "alarm", value: "strobe")
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "soundName", value: "Strobe (LED only)")
    sendEvent(name: "status", value: "playing")
}

void both() {
    logInfo "both() — siren + LED (normal playback mode)"
    // Ensure not stuck in LED-only mode
    Integer mode = (settings.param1 != null) ? settings.param1.toString().toInteger() : 3
    if (mode == 4) mode = 3
    List cmds = []
    cmds << cfgSet(1, mode, 1)
    cmds << secure(zwave.soundSwitchV1.tonePlaySet(toneIdentifier: (settings.param4 as Integer) ?: 1))
    sendCmds(cmds)
    sendEvent(name: "alarm", value: "both")
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "soundName", value: toneName(prefInt("param4", 1)))
    sendEvent(name: "status", value: "playing")
}

/** Plays each tone for a few seconds, one after another. */
void previewTones(seconds = null, which = null) {
    Integer secs = clampInt(toInt(seconds) ?: 2, 1, 10)
    String pick = (which ?: "All").toString()
    Map tones = knownTones()
    List nums = tones.keySet().sort().findAll { Integer t ->
        String file = tones[t]?.file
        boolean custom = file && !STOCK_FILES.containsKey(file.toLowerCase())
        pick == "Custom only" ? custom : (pick == "Stock only" ? !custom : true)
    }
    if (!nums) {
        logWarn "previewTones: no ${pick.toLowerCase()} tones to play"
        return
    }
    logInfo "previewTones: ${nums.size()} tone(s), ${secs}s each"
    state.preview = [list: nums, idx: 0, secs: secs]
    previewNext()
}

void previewNext() {
    Map p = state.preview
    if (!p) return
    List nums = p.list
    Integer idx = p.idx as Integer
    if (idx >= nums.size()) {
        logInfo "previewTones: done"
        stop()
        return
    }
    Integer t = nums[idx] as Integer
    playTone(t)
    sendEvent(name: "preview", value: "${idx + 1} of ${nums.size()}: ${toneLabel(t)}")
    p.idx = idx + 1
    state.preview = p
    runIn(p.secs as Integer, "previewNext")
}

private void cancelPreview() {
    if (!state.preview) return
    state.remove("preview")
    unschedule("previewNext")
    sendEvent(name: "preview", value: "off")
}

void stop() {
    logInfo "stop()"
    cancelPreview()
    List cmds = [secure(zwave.soundSwitchV1.tonePlaySet(toneIdentifier: 0))]
    // If strobe left the device in LED-only mode, restore the configured playback mode
    if (state.strobeActive) {
        Integer mode = prefInt("param1", 3)
        if (mode == 4) mode = 3
        cmds << cfgSet(1, mode, 1)
        state.strobeActive = false
    }
    sendCmds(cmds)
    sendStopped()
}

private void sendStopped() {
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "alarm", value: "off")
    sendEvent(name: "soundName", value: "None")
    sendEvent(name: "status", value: "stopped")
}

void playSound(toneNumber, volume = null) {
    cancelPreview()
    Integer v = toInt(volume)
    playTone(clampInt(toInt(toneNumber), 1, 50), v ? clampInt(v, 1, 100) : null)
}

private void playTone(Integer t, Integer vol = null) {
    if (state.toneInfo && !knownTones().containsKey(t)) logWarn "playSound(${t}): the device has no tone ${t}; trying anyway"
    String name = toneName(t)
    logInfo "playSound(${t}${vol ? ", ${vol}%" : ""}) -> ${name}"
    sendEvent(name: "soundName", value: name)
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "alarm", value: "siren")
    sendEvent(name: "status", value: "playing")
    sendCmds([
        // Sound Switch v2 Tone Play Set (0x79 0x08) carries a volume for this play only.
        vol ? secure("7908" + hex2(t) + hex2(vol)) : secure(zwave.soundSwitchV1.tonePlaySet(toneIdentifier: t)),
        secure(zwave.switchBinaryV2.switchBinaryGet())
    ])
}

void testTone(toneNumber) { playSound(toneNumber) }

void setVolume(level) {
    Integer v = clampInt(toInt(level), 0, 100)
    logInfo "setVolume(${v})"
    device.updateSetting("param5", [type: "number", value: v])
    sendEvent(name: "volume", value: v, unit: "%")
    sendCmds([cfgSet(5, v, 1), cfgGet(5)])
}

void setDefaultTone(toneNumber) {
    Integer t = clampInt(toInt(toneNumber), 1, 50)
    logInfo "setDefaultTone(${t})"
    device.updateSetting("param4", [type: "number", value: t])
    sendCmds([cfgSet(4, t, 1), cfgGet(4)])
}

void setLedColor(String colorName) {
    Integer val = LED_COLORS[colorName]
    if (val == null) {
        logWarn "setLedColor: unknown color '${colorName}'"
        return
    }
    logInfo "setLedColor(${colorName} -> ${val})"
    device.updateSetting("param7", [type: "enum", value: val.toString()])
    sendEvent(name: "ledColor", value: colorName)
    sendCmds([cfgSet(7, val, 1), cfgGet(7)])
}

void parse(String description) {
    if (debugEnable) logDebug "parse: ${description}"
    if (description?.startsWith("Err")) {
        logWarn "Z-Wave error: ${description}"
        return
    }
    hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)
    if (cmd) {
        zwaveEvent(cmd)
    } else {
        logWarn "Unable to parse: ${description}"
    }
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd) {
    hubitat.zwave.Command encap = cmd.encapsulatedCommand(commandClassVersions)
    if (encap) zwaveEvent(encap)
    sendHubCommand(new hubitat.device.HubAction(
        secure(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0)),
        hubitat.device.Protocol.ZWAVE
    ))
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) { handleOnOff(cmd.value as Integer) }
void zwaveEvent(hubitat.zwave.commands.basicv2.BasicReport cmd) { handleOnOff(cmd.value as Integer) }
void zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) { handleOnOff(cmd.value as Integer) }
void zwaveEvent(hubitat.zwave.commands.switchbinaryv2.SwitchBinaryReport cmd) { handleOnOff(cmd.value as Integer) }

private void handleOnOff(Integer value) {
    boolean on = (value != null && value != 0)
    if (on) {
        sendEvent(name: "switch", value: "on")
    } else {
        sendStopped()
    }
}

void zwaveEvent(hubitat.zwave.commands.soundswitchv1.TonePlayReport cmd) {
    if (debugEnable) logDebug "TonePlayReport: ${cmd}"
    if ((cmd.toneIdentifier as Integer) == 0) {
        sendStopped()
    } else {
        Integer t = cmd.toneIdentifier as Integer
        sendEvent(name: "switch", value: "on")
        sendEvent(name: "soundName", value: toneName(t))
        sendEvent(name: "status", value: "playing")
        if (device.currentValue("alarm") == "off" || !device.currentValue("alarm")) {
            sendEvent(name: "alarm", value: "siren")
        }
    }
}

void zwaveEvent(hubitat.zwave.commands.soundswitchv1.TonesNumberReport cmd) {
    if (debugEnable) logDebug "TonesNumberReport: ${cmd}"
    Integer count = clampInt(cmd.supportedTones as Integer, 0, 50)
    updateDataValue("tonesSupported", "${count}")
    if (count > 0) {
        state.pendingToneInfo = [:]
        sendCmds((1..count).collect { secure(zwave.soundSwitchV1.toneInfoGet(toneIdentifier: it)) }, 400)
        runIn(count + 10, "publishTones")   // in case the last report never arrives
    }
}

void zwaveEvent(hubitat.zwave.commands.soundswitchv1.ToneInfoReport cmd) {
    // Decoded from the raw frame: tone id, duration (2 bytes, seconds), name length, name bytes.
    List<Short> p = cmd.payload
    if (!p || p.size() < 4) return
    Integer t = p[0] as Integer
    Integer secs = ((p[1] as Integer) << 8) + (p[2] as Integer)
    Integer len = Math.min(p[3] as Integer, p.size() - 4)
    String file = len > 0 ? new String(p[4..<(4 + len)].collect { (byte) (it & 0xFF) } as byte[], "UTF-8").trim() : ""
    if (debugEnable) logDebug "ToneInfoReport ${t}: '${file}' ${secs}s"
    if (t == null || t < 1) return
    Map info = state.pendingToneInfo ?: [:]
    // Unused slots report "[empty Track]" with no length.
    boolean empty = !file || secs == 0 || file.toLowerCase().contains("empty track")
    if (!empty) info["${t}"] = [file: file, secs: secs]
    state.pendingToneInfo = info
    runIn(3, "publishTones")   // after the last report of a batch
}

/** Publishes soundEffects (the Play Sound picker) and toneList from what the device reported. */
void publishTones() {
    if (state.pendingToneInfo) {
        state.toneInfo = state.pendingToneInfo
        state.remove("pendingToneInfo")
    }
    Map tones = knownTones()
    List nums = tones.keySet().sort()
    Map effects = [:]
    nums.each { Integer t -> effects["${t}"] = toneLabel(t) }
    sendEvent(name: "soundEffects", value: groovy.json.JsonOutput.toJson(effects))
    sendEvent(name: "toneList", value: nums.collect { toneLabel(it) }.join(", "))
    sendEvent(name: "toneCount", value: nums.size())
}

void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) { handleConfigReport(cmd) }
void zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) { handleConfigReport(cmd) }
void zwaveEvent(hubitat.zwave.commands.configurationv4.ConfigurationReport cmd) { handleConfigReport(cmd) }

private void handleConfigReport(cmd) {
    Integer num = cmd.parameterNumber as Integer
    Integer val = cmd.scaledConfigurationValue as Integer
    if (debugEnable) logDebug "ConfigReport param ${num} = ${val}"
    String setting = "param${num}"
    if (num in [1, 6, 7, 9, 10, 11, 12]) {
        device.updateSetting(setting, [type: "enum", value: val.toString()])
    } else if (num in [2, 3, 4, 5, 8, 13, 14, 15, 16]) {
        device.updateSetting(setting, [type: "number", value: val])
    }
    if (num == 5) sendEvent(name: "volume", value: val, unit: "%")
    if (num == 7) {
        String name = LED_COLORS.find { k, v -> v == val }?.key ?: "Custom(${val})"
        sendEvent(name: "ledColor", value: name)
    }
}

void zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
    Integer val = (cmd.batteryLevel == 0xFF) ? 1 : (cmd.batteryLevel as Integer)
    sendEvent(name: "battery", value: val, unit: "%")
}

void zwaveEvent(hubitat.zwave.commands.notificationv8.NotificationReport cmd) {
    if (debugEnable) logDebug "Notification: type=${cmd.notificationType} event=${cmd.event}"
    // Power Management (0x08): 0x01 power applied, 0x02 AC mains disconnected (on backup battery),
    // 0x03 AC mains re-connected. Confirmed on the ZSE50: unplugging sends 0x02, plugging in sends 0x03.
    if ((cmd.notificationType as Integer) == 0x08) {
        switch (cmd.event as Integer) {
            case 0x00:
                // Idle reply to the refresh query: "mains disconnected" is not active.
                List params = cmd.eventParameter ?: []
                if (params && (params[0] as Integer) == 0x02) sendPowerSource("mains")
                break
            case 0x01:
            case 0x03:
                sendPowerSource("mains")
                break
            case 0x02:
                sendPowerSource("battery")
                break
        }
    }
}

private void sendPowerSource(String value) {
    if (device.currentValue("powerSource") != value) logInfo "power source: ${value}"
    sendEvent(name: "powerSource", value: value)
}

void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
    if (debugEnable) logDebug "AssociationReport group ${cmd.groupingIdentifier}: ${cmd.nodeId} (max ${cmd.maxNodesSupported})"
    if (cmd.maxNodesSupported != null) {
        updateDataValue("assocMaxGroup${cmd.groupingIdentifier}", "${cmd.maxNodesSupported}")
    }
}

void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
    if (debugEnable) logDebug "VersionReport: ${cmd}"
    updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion.toString().padLeft(2, '0')}")
    updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    if (cmd.hardwareVersion != null) updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    updateDataValue("manufacturer", String.format("0x%04X", cmd.manufacturerId))
    updateDataValue("deviceType", String.format("0x%04X", cmd.productTypeId))
    updateDataValue("deviceId", String.format("0x%04X", cmd.productId))
}

void zwaveEvent(hubitat.zwave.commands.indicatorv3.IndicatorReport cmd) {
    if (debugEnable) logDebug "IndicatorReport: ${cmd}"
}

void zwaveEvent(hubitat.zwave.commands.centralscenev1.CentralSceneNotification cmd) {
    if (debugEnable) logDebug "CentralScene: ${cmd}"
}

void zwaveEvent(hubitat.zwave.Command cmd) {
    if (debugEnable) logDebug "Unhandled: ${cmd}"
}

private List configWriteCmds() {
    List cmds = []
    cmds << cfgSet(1, prefInt("param1", 3), 1)
    cmds << cfgSet(2, prefInt("param2", 180), 2)
    cmds << cfgSet(3, prefInt("param3", 1), 1)
    cmds << cfgSet(4, prefInt("param4", 1), 1)
    cmds << cfgSet(5, prefInt("param5", 100), 1)
    cmds << cfgSet(6, prefInt("param6", 1), 1)
    cmds << cfgSet(7, prefInt("param7", 0), 1)
    cmds << cfgSet(8, prefInt("param8", 20), 1)
    cmds << cfgSet(9, prefInt("param9", 1), 1)
    cmds << cfgSet(10, prefInt("param10", 1), 1)
    cmds << cfgSet(11, prefInt("param11", 1), 1)
    cmds << cfgSet(12, prefInt("param12", 1), 1)
    cmds << cfgSet(13, prefInt("param13", 50), 1)
    cmds << cfgSet(14, prefInt("param14", 5), 1)
    cmds << cfgSet(15, prefInt("param15", 12), 1)
    cmds << cfgSet(16, prefInt("param16", 0), 1)
    cmds += configGetCmds()
    return cmds
}

private List configGetCmds() {
    (1..16).collect { cfgGet(it) }
}

private String cfgSet(int param, int value, int size) {
    return secure(zwave.configurationV4.configurationSet(
        parameterNumber: param,
        size: size,
        scaledConfigurationValue: value
    ))
}

private String cfgGet(int param) {
    return secure(zwave.configurationV4.configurationGet(parameterNumber: param))
}

private int prefInt(String name, int dflt) {
    def raw = settings[name]
    if (raw == null || raw.toString() == "") return dflt
    return raw.toString().toInteger()
}

private Integer clampInt(Integer val, Integer min, Integer max) {
    if (val == null) return min
    if (val < min) return min
    if (val > max) return max
    return val
}

/** Tone number -> [file, secs] as reported by the device, or the stock list before any report. */
private Map knownTones() {
    Map info = state.toneInfo
    Map tones = [:]
    if (info) {
        info.each { k, v -> tones[k as Integer] = v }
    } else {
        TONE_NAMES.each { k, v -> tones[k as Integer] = [file: null, secs: null] }
    }
    return tones
}

/** Readable name: the stock name for a stock file, otherwise one made from the file name. */
private String toneName(Integer t) {
    Map tone = knownTones()[t]
    String file = tone?.file
    if (!file) return TONE_NAMES[t] ?: "Tone ${t}"
    String stock = STOCK_FILES[file.toLowerCase()]
    return stock ?: humanize(file)
}

/** Picker label: "10 - Buzzer Alarm", or "26 - My Sound (custom)". Lengths are left out: the device reports them inaccurately. */
private String toneLabel(Integer t) {
    Map tone = knownTones()[t]
    String file = tone?.file
    List extra = []
    if (file && !STOCK_FILES.containsKey(file.toLowerCase())) extra << "custom"
    return "${t} - ${toneName(t)}" + (extra ? " (${extra.join(', ')})" : "")
}

/** "alert9_Garage-door_closing.mp3" -> "Garage Door Closing". */
private String humanize(String file) {
    String base = file.replaceAll(/(?i)\.(mp3|wav)$/, "")
    base = base.replaceAll(/(?i)^alert\d+_/, "")
    base = base.replaceAll(/[_\-]+/, " ").replaceAll(/\s+/, " ").trim()
    if (!base) return file
    return base.split(" ").collect { String w -> w[0].toUpperCase() + w.substring(1) }.join(" ")
}

/** Accepts numbers sent as numbers or text ("10", "10 - Buzzer Alarm"). */
private Integer toInt(val) {
    if (val == null) return null
    if (val instanceof Number) return (val as Number).intValue()
    def m = (val.toString().trim() =~ /^(\d+)/)
    return m.find() ? m.group(1).toInteger() : null
}

private List parseAssocGroupList(list, int max) {
    List nodes = []
    if (!list) return nodes
    int count = 0
    list.toString().split(",").each { node ->
        node = node.trim()
        if (!node) return
        if (count >= max) {
            logWarn "Association Group 2: discarding extra member ${node} (max ${max})"
            return
        }
        if (node.matches("\\p{XDigit}+")) {
            int nodeId = Integer.parseInt(node, 16)
            if (nodeId == zwaveHubNodeId) {
                logWarn "Association Group 2: hub node not allowed"
            } else if (nodeId > 0 && nodeId < 256) {
                nodes << nodeId
                count++
            } else {
                logWarn "Association Group 2: invalid member ${node}"
            }
        } else {
            logWarn "Association Group 2: invalid member ${node}"
        }
    }
    return nodes
}

private void sendCmds(List cmds, int delay = 300) {
    if (!cmds) return
    sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds, delay), hubitat.device.Protocol.ZWAVE))
}

private String secure(hubitat.zwave.Command cmd) {
    return zwaveSecureEncap(cmd)
}

private String secure(String cmd) {
    return zwaveSecureEncap(cmd)
}

private static String hex2(Integer n) {
    return String.format("%02X", n & 0xFF)
}

void logsOff() {
    logWarn "debug logging disabled"
    device.updateSetting("debugEnable", [value: "false", type: "bool"])
}

private void logDebug(String msg) { if (debugEnable) log.debug "[ZSE50] ${msg}" }
private void logInfo(String msg)  { if (infoEnable)  log.info  "[ZSE50] ${msg}" }
private void logWarn(String msg)  { log.warn "[ZSE50] ${msg}" }
