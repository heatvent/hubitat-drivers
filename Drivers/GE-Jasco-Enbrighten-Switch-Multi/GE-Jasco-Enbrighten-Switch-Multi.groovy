/**
 *  IMPORT URL: https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/GE-Jasco-Enbrighten-Switch-Multi/GE-Jasco-Enbrighten-Switch-Multi.groovy
 *
 *  GE/Jasco Enbrighten Switch (Multi)
 *
 *  One driver for GE Enbrighten / Jasco / UltraPro Z-Wave Plus in-wall switches (500S, 700S and 800S series).
 *  The exact model is detected from the Z-Wave fingerprint (productType + productId) and a per-model
 *  profile (DEVICE_PROFILES) decides which configuration parameters are written to the device.
 *
 *  CREDIT:
 *    Based on the "GE Enbrighten Z-Wave Plus Switch" driver by Jason Bottjen (Botched1)
 *    https://github.com/Botched1/Hubitat/tree/master/Drivers/GE%20Enbrighten%20Switch
 *    All button / Central Scene / association / S2 behavior comes from that work.
 *    Developed with Cursor.
 *
 *  Author:    Heatvent
 *  Namespace: Heatvent
 *
 *  ---------------------------------------------------------------------------------------------------
 *  1.0.1 (2026-11-14) - Persist detected profile to device Data Values (profileKey, profileName,
 *                       profileGeneration, profileSource, profileColorLed, profileThreeWay) so the
 *                       model ID is visible on the device page without overwriting user Notes.
 *                     - Author credit normalized to Heatvent
 *  1.0.0 (2026-11-14) - Initial multi-model release (Heatvent)
 *                       - Profile system keyed by "<productType>-<productId>" (hex, lowercase, no 0x)
 *                       - Auto-detect with optional forceProfile override
 *                       - Only parameters listed in the profile are written to the device
 *                       - Configuration CC V4 for 700S/800S profiles, V2 for older 500S profiles
 *                       - Added setLedColor command (color LED profiles only)
 *                       - Includes the Heatvent maintenance fixes (2026-11-12) from the ZWA4011/ZWA3016 patched drivers:
 *                           F2 typo Guidelight Intensity title
 *                           F3 ConfigurationReport v4 handler
 *                           F4 VersionReport logDebug -> logEnable
 *                           F5 held-down descriptionText said Up -> Down
 *                           F6 updated() txtEnable -> logDesc
 *                           F7 null LED color/intensity/guidelight defaults 5/4/4 (not 0)
 *
 *  Original Botched1 history (abridged):
 *  1.0.0 (07/16/2019) - Initial Version
 *  1.2.0 (02/07/2020) - Added pushed, held, and released capability. Buttons 1/2=Up/Down, 3/4=Double Up/Down, 5/6=Triple Up/Down
 *  1.3.0 (05/17/2020) - Added associations and inverted paddle options
 *  2.0.0f (08/13/2020) - Added S2 capability for Hubitat 2.2.3 and newer
 *  2.2.0 (08/29/2020) - Added number of button config to configure
 *  2.3.0 (02/13/2021) - Added Alternate Exclusion mode to preferences.
 *  2.3.1 (03/08/2022) - Added setIndicatorBehavior command.
*/


import groovy.transform.Field

// Base command class versions (700S / 800S). 500S profiles are downgraded in ccVersions().
@Field static Map commandClassVersions = 
    [
         0x25: 2 //1    //Switch Binary
        ,0x70: 4 //2    //configuration
        ,0x72: 2    //Manufacturer Specific
        ,0x85: 2    //association
	    ,0x86: 3    //Version
]

// Profiles keyed by "<productType>-<productId>" (4 hex digits each, lowercase, no 0x prefix).
// params : configuration parameter numbers this model supports (only these are written / read)
// relayControl : reserved for models that expose a relay-control parameter (param 39); none of the
//                models below are known to, so no parameter 39 traffic is generated.
@Field static Map DEVICE_PROFILES = [
    // ---- 500S classic (no color LED, no 3-way add-on parameter) ----
    "4952-3135": [name: "Enbrighten ZW4008 / 46201",   generation: "500S", colorLed: false, threeWay: false, relayControl: false, params: [3,4,19]]
   ,"4952-3136": [name: "Jasco ZW4008 / 46562",        generation: "500S", colorLed: false, threeWay: false, relayControl: false, params: [3,4,19]]
   ,"4952-3237": [name: "UltraPro ZW4008 / 39348",     generation: "500S", colorLed: false, threeWay: false, relayControl: false, params: [3,4,19]]
   ,"4952-3137": [name: "Enbrighten ZW4009 / 46202",   generation: "500S", colorLed: false, threeWay: false, relayControl: false, params: [3,4,19]]
   ,"4952-3138": [name: "Jasco ZW4009 / 46563",        generation: "500S", colorLed: false, threeWay: false, relayControl: false, params: [3,4,19]]
   ,"4952-3238": [name: "UltraPro ZW4009 / 39354",     generation: "500S", colorLed: false, threeWay: false, relayControl: false, params: [3,4,19]]
    // ---- 700S color LED ----
   ,"4952-3239": [name: "Enbrighten ZWA4011 / 58433",    generation: "700S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,19,34,35,36]]
   ,"4952-3331": [name: "Enbrighten ZWA4011DV / 59337",  generation: "700S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,19,34,35,36]]
   ,"4952-3335": [name: "Jasco ZWA4011 / 59381",         generation: "700S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,19,34,35,36]]
   ,"4952-3333": [name: "UltraPro ZWA4011 / 59347",      generation: "700S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,19,34,35,36]]
   ,"4952-3330": [name: "Enbrighten ZWA4012 / 58436",    generation: "700S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,19,34,35,36]]
   ,"4952-3332": [name: "Enbrighten ZWA4012DV / 59338",  generation: "700S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,19,34,35,36]]
   ,"4952-3336": [name: "Jasco ZWA4012 / 59382",         generation: "700S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,19,34,35,36]]
   ,"4952-3334": [name: "UltraPro ZWA4012 / 59368",      generation: "700S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,19,34,35,36]]
    // ---- 800S color LED ----
   ,"4952-3435": [name: "Enbrighten ZWN4015 / 76591",    generation: "800S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,19,34,35,36]]
   ,"4952-3437": [name: "Jasco ZWN4015 / 76596",         generation: "800S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,19,34,35,36]]
   ,"4952-3438": [name: "Enbrighten ZWN4016 / 76592",    generation: "800S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,19,34,35,36]]
   ,"4952-3530": [name: "Jasco ZWN4016 / 76598",         generation: "800S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,19,34,35,36]]
   ,"4952-3534": [name: "UltraPro ZWN4017 / 76601",      generation: "800S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,19,34,35,36]]
]

// Used when the model is not recognised: core parameters only, no color LED writes.
@Field static Map FALLBACK_PROFILE = [name: "Unknown model (conservative)", generation: "unknown", colorLed: false, threeWay: false, relayControl: false, params: [3,4,19]]

// Parameter number -> preference name and default (written in this order)
@Field static List PARAM_PREFS = [
     [num: 3,  setting: "paramLED",                dflt: 0]
    ,[num: 4,  setting: "paramInverted",           dflt: 0]
    ,[num: 5,  setting: "paramThreeWayType",       dflt: 0]
    ,[num: 19, setting: "paramAlternateExclusion", dflt: 0]
    ,[num: 34, setting: "paramLEDColor",           dflt: 5]   // F7 Blue
    ,[num: 35, setting: "paramIntensity",          dflt: 4]   // F7
    ,[num: 36, setting: "paramGuidelightIntensity",dflt: 4]   // F7
]

@Field static Map LED_COLORS = ["Red": 1, "Orange": 2, "Yellow": 3, "Green": 4, "Blue": 5, "Pink": 6, "Purple": 7, "White": 8]

metadata {
	definition (name: "GE/Jasco Enbrighten Switch (Multi)", namespace: "Heatvent", author: "Heatvent") {
		capability "Actuator"
        capability "DoubleTapableButton"
        capability "HoldableButton"
		capability "PushableButton"
        capability "ReleasableButton"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Light"
        
	    command "setIndicatorBehavior", [[name:"LED Indicator Behavior", type: "ENUM", description: "", constraints: ["LED ON When Switch OFF", "LED ON When Switch ON", "LED Always OFF", "LED Always ON", "0", "1", "2", "3"] ] ]
	    command "setLedColor", [[name:"LED Color (color LED models only)", type: "ENUM", description: "Param 34. Ignored on models without a color LED.", constraints: ["Red", "Orange", "Yellow", "Green", "Blue", "Pink", "Purple", "White"] ] ]
        
        // 500S classic
        fingerprint mfr:"0063", prod:"4952", deviceId:"3135", inClusters:"0x5E,0x25,0x85,0x59,0x86,0x72,0x55,0x5A,0x73,0x5B,0x9F,0x6C,0x70,0x2C,0x2B,0x22,0x7A", controllerType: "ZWV" // Enbrighten ZW4008
        fingerprint mfr:"0063", prod:"4952", deviceId:"3136" // Jasco ZW4008
        fingerprint mfr:"0063", prod:"4952", deviceId:"3237", inClusters:"0x5E,0x25,0x85,0x59,0x86,0x72,0x55,0x5A,0x73,0x5B,0x9F,0x6C,0x70,0x2C,0x2B,0x22,0x7A", controllerType: "ZWV" // UltraPro ZW4008
        fingerprint mfr:"0063", prod:"4952", deviceId:"3137" // Enbrighten ZW4009
        fingerprint mfr:"0063", prod:"4952", deviceId:"3138" // Jasco ZW4009
        fingerprint mfr:"0063", prod:"4952", deviceId:"3238" // UltraPro ZW4009
        // 700S color
        fingerprint mfr:"0063", prod:"4952", deviceId:"3239", inClusters:"0x5E,0x22,0x85,0x8E,0x59,0x87,0x25,0x2B,0x2C,0x55,0x86,0x72,0x5A,0x73,0x70,0x9F,0x6C,0x5B,0x7A", controllerType: "ZWV" // Enbrighten ZWA4011
        fingerprint mfr:"0063", prod:"4952", deviceId:"3331" // Enbrighten ZWA4011DV
        fingerprint mfr:"0063", prod:"4952", deviceId:"3335" // Jasco ZWA4011
        fingerprint mfr:"0063", prod:"4952", deviceId:"3333" // UltraPro ZWA4011
        fingerprint mfr:"0063", prod:"4952", deviceId:"3330" // Enbrighten ZWA4012
        fingerprint mfr:"0063", prod:"4952", deviceId:"3332" // Enbrighten ZWA4012DV
        fingerprint mfr:"0063", prod:"4952", deviceId:"3336" // Jasco ZWA4012
        fingerprint mfr:"0063", prod:"4952", deviceId:"3334" // UltraPro ZWA4012
        // 800S color
        fingerprint mfr:"0063", prod:"4952", deviceId:"3435" // Enbrighten ZWN4015
        fingerprint mfr:"0063", prod:"4952", deviceId:"3437" // Jasco ZWN4015
        fingerprint mfr:"0063", prod:"4952", deviceId:"3438" // Enbrighten ZWN4016
        fingerprint mfr:"0063", prod:"4952", deviceId:"3530" // Jasco ZWN4016
        fingerprint mfr:"0063", prod:"4952", deviceId:"3534" // UltraPro ZWN4017
	}

 preferences {
        input name: "profileNote", type: "paragraph", element: "paragraph", title: "Model-specific settings", description: "All settings are listed for every model. Settings your switch does not support (for example color LED options on non-color models, or the 3-way add-on option on 500S models) are ignored and never written to the device. The detected model is stored in device Data Values (profileKey, profileName, profileGeneration) and in state."
        input name: "forceProfile", type: "enum", title: "Device Profile Override", multiple: false, options: ["auto" : "Auto-detect (default)", "4952-3135" : "4952-3135 Enbrighten ZW4008 / 46201 [500S]", "4952-3136" : "4952-3136 Jasco ZW4008 / 46562 [500S]", "4952-3237" : "4952-3237 UltraPro ZW4008 / 39348 [500S]", "4952-3137" : "4952-3137 Enbrighten ZW4009 / 46202 [500S]", "4952-3138" : "4952-3138 Jasco ZW4009 / 46563 [500S]", "4952-3238" : "4952-3238 UltraPro ZW4009 / 39354 [500S]", "4952-3239" : "4952-3239 Enbrighten ZWA4011 / 58433 [700S]", "4952-3331" : "4952-3331 Enbrighten ZWA4011DV / 59337 [700S]", "4952-3335" : "4952-3335 Jasco ZWA4011 / 59381 [700S]", "4952-3333" : "4952-3333 UltraPro ZWA4011 / 59347 [700S]", "4952-3330" : "4952-3330 Enbrighten ZWA4012 / 58436 [700S]", "4952-3332" : "4952-3332 Enbrighten ZWA4012DV / 59338 [700S]", "4952-3336" : "4952-3336 Jasco ZWA4012 / 59382 [700S]", "4952-3334" : "4952-3334 UltraPro ZWA4012 / 59368 [700S]", "4952-3435" : "4952-3435 Enbrighten ZWN4015 / 76591 [800S]", "4952-3437" : "4952-3437 Jasco ZWN4015 / 76596 [800S]", "4952-3438" : "4952-3438 Enbrighten ZWN4016 / 76592 [800S]", "4952-3530" : "4952-3530 Jasco ZWN4016 / 76598 [800S]", "4952-3534" : "4952-3534 UltraPro ZWN4017 / 76601 [800S]"], required: false, displayDuringSetup: true, defaultValue: "auto"
        input name: "paramLED", type: "enum", title: "(3) LED Indicator Behavior", multiple: false, options: ["0" : "LED ON When Switch OFF (default)", "1" : "LED ON When Switch ON", "2" : "LED Always OFF", "3" : "LED Always ON"], required: false, displayDuringSetup: true, defaultValue: "0"
        input name: "paramInverted", type: "enum", title: "(4) Switch Buttons Direction", multiple: false, options: ["0" : "Normal (default)", "1" : "Inverted"], required: false, displayDuringSetup: true, defaultValue: "0"
		input name: "paramThreeWayType", type: "enum", title: "(5) 3-Way Add-On Switch [700S/800S only]", multiple: false, options: ["0" : "Off (default)", "1" : "On"], required: false, displayDuringSetup: true, defaultValue: "0"
        input name: "paramAlternateExclusion", type: "enum", title: "(19) Enable alternate exclusion (2-ON, 2-OFF)", multiple: false, options: ["0" : "OFF (default)", "1" : "ON"], required: false, displayDuringSetup: true, defaultValue: "0"
        input name: "paramLEDColor", type: "enum", title: "(34) LED Light Color [color LED models only]", multiple: false, options: ["1" : "Red", "2" : "Orange", "3" : "Yellow", "4" : "Green", "5" : "Blue (default)", "6" : "Pink", "7" : "Purple", "8" : "White"], required: false, displayDuringSetup: true, defaultValue: "5"
        input name: "paramIntensity", type: "enum", title: "(35) LED Intensity [color LED models only]", multiple: false, options: ["0" : "0 - Off","1" : "1", "2" : "2", "3" : "3", "4" : "4 (default)", "5" : "5", "6" : "6", "7" : "7"], required: false, displayDuringSetup: true, defaultValue: "4"
        // FIX 2026-11-12 F2: typo Intesity -> Intensity
        input name: "paramGuidelightIntensity", type: "enum", title: "(36) Guidelight Intensity [color LED models only]", multiple: false, options: ["0" : "0 - Off","1" : "1", "2" : "2", "3" : "3", "4" : "4 (default)", "5" : "5", "6" : "6", "7" : "7"], required: false, displayDuringSetup: true, defaultValue: "4"
        input name: "requestedGroup2", type: "text", title: "Association Group 2 Members (Max of 5):", required: false
        input name: "requestedGroup3", type: "text", title: "Association Group 3 Members (Max of 4):", required: false
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
	    input name: "logDesc", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Profile handling
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Normalise a productType / productId value to 4 lowercase hex digits (no 0x).
// Accepts "0x4952", "4952" (hex as shown in fingerprints) or decimal "18770" (as stored in Hubitat data values).
private String normalizeHex4(value) {
    if (value == null) return null
    String s = value.toString().trim().toLowerCase()
    if (!s) return null
    if (s.startsWith("0x")) {
        s = s.substring(2)
        return s.matches("[0-9a-f]{1,4}") ? s.padLeft(4, "0") : null
    }
    if (s.matches("[0-9a-f]{4}")) return s
    if (s.matches("\\d+")) return Integer.toHexString(Integer.parseInt(s)).padLeft(4, "0")
    return null
}

private String detectProfileKey() {
    String prod = null
    String did = null
    try {
        def zi = getZwaveInfo()
        if (zi) {
            prod = normalizeHex4(zi.prod)
            did = normalizeHex4(zi.deviceId)
        }
    } catch (Exception e) {
        if (logEnable) log.debug "getZwaveInfo() not available: ${e.message}"
    }
    if (!prod || !did) {
        prod = normalizeHex4(device.getDataValue("deviceType"))
        did = normalizeHex4(device.getDataValue("deviceId"))
    }
    if (!prod || !did) return null
    return "${prod}-${did}".toString()
}

private void storeProfile(String key, Map profile, String source = "auto") {
    if (state.profileKey != key || state.profileSource != source) {
        if (DEVICE_PROFILES.containsKey(key)) {
            log.info "${device.displayName} profile: ${key} - ${profile.name} (${profile.generation}) [${source}]"
        } else {
            log.warn "${device.displayName} unknown model fingerprint (${key}). Using conservative profile (core parameters only). Set 'Device Profile Override' in preferences if your model is known."
        }
    }
    state.profileKey = key
    state.profileName = profile.name
    state.profileGeneration = profile.generation
    state.profileSource = source
    // Data Values survive driver changes and show on the device page (better than Notes for ID info)
    updateDataValue("profileKey", key ?: "")
    updateDataValue("profileName", profile?.name?.toString() ?: "")
    updateDataValue("profileGeneration", profile?.generation?.toString() ?: "")
    updateDataValue("profileSource", source ?: "")
    updateDataValue("profileColorLed", profile?.colorLed ? "true" : "false")
    updateDataValue("profileThreeWay", profile?.threeWay ? "true" : "false")
}

// Detect (or force) the profile and store it in state + Data Values
Map resolveProfile() {
    String forced = settings?.forceProfile
    if (forced && forced != "auto" && DEVICE_PROFILES.containsKey(forced)) {
        storeProfile(forced, DEVICE_PROFILES[forced], "forced")
        return DEVICE_PROFILES[forced]
    }
    String key = detectProfileKey()
    if (key && DEVICE_PROFILES.containsKey(key)) {
        storeProfile(key, DEVICE_PROFILES[key], "auto")
        return DEVICE_PROFILES[key]
    }
    storeProfile(key ?: "unknown", FALLBACK_PROFILE, key ? "fallback" : "unknown")
    return FALLBACK_PROFILE
}

// Cheap lookup for hot paths (parse, on/off); falls back to resolveProfile() when nothing is cached
Map currentProfile() {
    String forced = settings?.forceProfile
    if (forced && forced != "auto" && DEVICE_PROFILES.containsKey(forced)) return DEVICE_PROFILES[forced]
    String key = state.profileKey
    if (key && DEVICE_PROFILES.containsKey(key)) return DEVICE_PROFILES[key]
    return resolveProfile()
}

private boolean supportsParam(Map profile, int num) {
    return profile?.params?.contains(num) ? true : false
}

// Configuration CC V4 for 700S/800S, V2 for older profiles
private boolean useConfigV4(Map profile) {
    return (profile?.generation == "700S" || profile?.generation == "800S")
}

private Map ccVersions() {
    Map versions = [:] + commandClassVersions
    if (!useConfigV4(currentProfile())) {
        versions[0x70] = 2
        versions[0x25] = 1
    }
    return versions
}

private String cfgSet(Map profile, int param, int value) {
    if (useConfigV4(profile)) {
        return secure(zwave.configurationV4.configurationSet(scaledConfigurationValue: value, parameterNumber: param, size: 1).format())
    }
    return secure(zwave.configurationV2.configurationSet(scaledConfigurationValue: value, parameterNumber: param, size: 1).format())
}

private String cfgGet(Map profile, int param) {
    if (useConfigV4(profile)) {
        return secure(zwave.configurationV4.configurationGet(parameterNumber: param).format())
    }
    return secure(zwave.configurationV2.configurationGet(parameterNumber: param).format())
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Parse
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void parse(String description){
    if (logEnable) log.debug "parse description: ${description}"
    hubitat.zwave.Command cmd = zwave.parse(description, ccVersions())
    if (cmd) {
        zwaveEvent(cmd)
    }
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Z-Wave Messages
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
String secure(String cmd){
    return zwaveSecureEncap(cmd)
}

String secure(hubitat.zwave.Command cmd){
    return zwaveSecureEncap(cmd)
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd){
    hubitat.zwave.Command encapCmd = cmd.encapsulatedCommand(ccVersions())
    
    if (encapCmd) {
        zwaveEvent(encapCmd)
    }
    sendHubCommand(new hubitat.device.HubAction(secure(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0)), hubitat.device.Protocol.ZWAVE))
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    if (logEnable) log.debug "---BASIC REPORT V1--- ${device.displayName} sent ${cmd}"
	if (logEnable) log.debug "This report does nothing in this driver, and shouldn't have been called..."
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
    if (logEnable) log.debug "---BASIC SET V1--- ${device.displayName} sent ${cmd}"
    if (logEnable) log.debug "This report does nothing in this driver, and can be ignored..."
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
	if (logEnable) log.debug "---ASSOCIATION REPORT V2--- ${device.displayName} sent groupingIdentifier: ${cmd.groupingIdentifier} maxNodesSupported: ${cmd.maxNodesSupported} nodeId: ${cmd.nodeId} reportsToFollow: ${cmd.reportsToFollow}"
    if (cmd.groupingIdentifier == 3) {
    	if (cmd.nodeId.contains(zwaveHubNodeId)) {
        	sendEvent(name: "numberOfButtons", value: 6, displayed: false)
        }
        else {
        	sendEvent(name: "numberOfButtons", value: 0, displayed: false)
			delayBetween([
                secure(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format())
			    ,secure(zwave.associationV2.associationGet(groupingIdentifier: 3).format())
                ] ,250)
        }
    }
}

def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
	if (logEnable) log.debug "---CONFIGURATION REPORT V2--- ${device.displayName} sent ${cmd}"
}

// FIX 2026-11-12 F3: 700S/800S profiles use Configuration CC v4; handle v4 reports
def zwaveEvent(hubitat.zwave.commands.configurationv4.ConfigurationReport cmd) {
	if (logEnable) log.debug "---CONFIGURATION REPORT V4--- ${device.displayName} sent ${cmd}"
}

private void handleSwitchBinaryReport(cmd) {
	def desc, newValue, curValue, newType
	
	// check state.bin variable to see if event is digital or physical
	if (state.bin)
	{
		newType = "digital"
	}
	else
	{
		newType = "physical"
	}
	
	// Reset state.bin variable
	state.bin = 0
	
	curValue = device.currentValue("switch")

	if (cmd.value) { // == 255) {
		desc = "$device.displayName was turned on [$newType]"
		newValue = "on"
	} else {
		desc = "$device.displayName was turned off [$newType]"
		newValue = "off"
	}

	if (curValue != newValue) {
		if (logDesc) log.info "$device.displayName is " + (cmd.value ? "on" : "off")
		sendEvent([name: "switch", value: cmd.value ? "on" : "off", descriptionText: "$desc", type: "$newType", isStateChange: true])
	}
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    if (logEnable) log.debug "---BINARY SWITCH REPORT V1--- ${device.displayName} sent ${cmd}"
    handleSwitchBinaryReport(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv2.SwitchBinaryReport cmd) {
    if (logEnable) log.debug "---BINARY SWITCH REPORT V2--- ${device.displayName} sent ${cmd}"
    handleSwitchBinaryReport(cmd)
}

// Lifted from https://github.com/RMoRobert/Hubitat/blob/master/drivers/iBlinds-v3.groovy
def zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
   if (logEnable) log.debug "VersionReport: ${cmd}"  // FIX 2026-11-12 F4: logDebug -> logEnable
   device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion.toString().padLeft(2,'0')}")
   device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
   device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

def zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd) {
	if (logEnable) log.debug "Hail command received..."
	if (logEnable) log.debug "This does nothing in this driver, and shouldn't have been called..."
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) {
    if (logEnable) log.debug "Device Specific Report: ${cmd}"
    switch (cmd.deviceIdType) {
        case 1:
            // serial number
            def serialNumber=""
            if (cmd.deviceIdDataFormat==1) {
                cmd.deviceIdData.each { serialNumber += hubitat.helper.HexUtils.integerToHexString(it & 0xff,1).padLeft(2, '0')}
            } else {
                cmd.deviceIdData.each { serialNumber += (char) it }
            }
            device.updateDataValue("serialNumber", serialNumber)
            break
    }
}

def zwaveEvent(hubitat.zwave.commands.centralscenev1.CentralSceneNotification cmd) {
	if (logEnable) log.debug "CentralSceneNotification V1 Called."
    
    def result = []
    
    // Single Tap Up
    if ((cmd.keyAttributes == 0) && (cmd.sceneNumber == 1)) {
		if (logEnable) log.debug "Physical ON Triggered"
		if (logDesc) log.info "$device.displayName turned on [physical]"
        result << sendEvent([name: "pushed", value: 1, descriptionText: "$device.displayName had Up Pushed (button 1) [physical]", type: "physical", isStateChange: true])
    }
    // Single Tap Down
    if ((cmd.keyAttributes == 0) && (cmd.sceneNumber == 2)) {
		if (logEnable) log.debug "Physical OFF Triggered"
		if (logDesc) log.info "$device.displayName turned off [physical]"
        result << sendEvent([name: "pushed", value: 2, descriptionText: "$device.displayName had Down Pushed (button 2) [physical]", type: "physical", isStateChange: true])
    }
    // Released Up
    if ((cmd.keyAttributes == 1) && (cmd.sceneNumber == 1)) {
		if (logEnable) log.debug "Up Released Triggered"
        if (logDesc) log.info "$device.displayName had Up Released (button 1) [physical]"
        result << sendEvent([name: "released", value: 1, descriptionText: "$device.displayName had Up Released (button 1) [physical]", type: "physical", isStateChange: true])
    }
    // Released Down
    if ((cmd.keyAttributes == 1) && (cmd.sceneNumber == 2)) {
		if (logEnable) log.debug "Down Released Triggered"
        if (logDesc) log.info "$device.displayName had Down Released (button 2) [physical]"
        result << sendEvent([name: "released", value: 2, descriptionText: "$device.displayName had Down Released (button 2) [physical]", type: "physical", isStateChange: true])
    }
    // Held Up
    if ((cmd.keyAttributes == 2) && (cmd.sceneNumber == 1)) {
		if (logEnable) log.debug "Up Held Triggered"
		if (logDesc) log.info "$device.displayName had Up Held (button 1) [physical]"
		result << sendEvent([name: "held", value: 1, descriptionText: "$device.displayName had Up held (button 1) [physical]", type: "physical", isStateChange: true])
    }
    // Held Down
    if ((cmd.keyAttributes == 2) && (cmd.sceneNumber == 2)) {
		if (logEnable) log.debug "Down Held Triggered"
		if (logDesc) log.info "$device.displayName had Down Held (button 2) [physical]"
		result << sendEvent([name: "held", value: 2, descriptionText: "$device.displayName had Down held (button 2) [physical]", type: "physical", isStateChange: true])  // FIX 2026-11-12 F5
    }        
    // Double Tap Up
    if ((cmd.keyAttributes == 3) && (cmd.sceneNumber == 1)) {
		if (logEnable) log.debug "Double Tap Up Triggered"
		if (logDesc) log.info "$device.displayName had Doubletap up (button 3) [physical]"
		result << sendEvent([name: "pushed", value: 3, descriptionText: "$device.displayName had Doubletap up (button 3) [physical]", type: "physical", isStateChange: true])
        result << sendEvent([name: "doubleTapped", value: 1, descriptionText: "$device.displayName had Doubletap up (doubleTapped 1) [physical]", type: "physical", isStateChange: true])
    }
    // Double Tap Down
    if ((cmd.keyAttributes == 3) && (cmd.sceneNumber == 2)) {
		if (logEnable) log.debug "Double Tap Down Triggered"
		if (logDesc) log.info "$device.displayName had Doubletap down (button 4) [physical]"
		result << sendEvent([name: "pushed", value: 4, descriptionText: "$device.displayName had Doubletap down (button 4) [physical]", type: "physical", isStateChange: true])
        result << sendEvent([name: "doubleTapped", value: 2, descriptionText: "$device.displayName had Doubletap down (doubleTapped 2) [physical]", type: "physical", isStateChange: true])
    }
    // Triple Tap Up
    if ((cmd.keyAttributes == 4) && (cmd.sceneNumber == 1)) {
		if (logEnable) log.debug "Triple Tap Up Triggered"
		if (logDesc) log.info "$device.displayName had Tripletap up (button 5) [physical]"
		result << sendEvent([name: "pushed", value: 5, descriptionText: "$device.displayName had Tripletap up (button 5) [physical]", type: "physical", isStateChange: true])
    }
    // Triple Tap Down
    if ((cmd.keyAttributes == 4) && (cmd.sceneNumber == 2)) {
		if (logEnable) log.debug "Triple Tap Down Triggered"
		if (logDesc) log.info "$device.displayName had Tripletap down (button 6) [physical]"
		result << sendEvent([name: "pushed", value: 6, descriptionText: "$device.displayName had Tripletap down (button 6) [physical]", type: "physical", isStateChange: true])
    }

    return result
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    log.warn "${device.displayName} received unhandled command: ${cmd}"
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Driver Commands / Functions
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def on() {
	if (logEnable) log.debug "Turn device ON"
	state.bin = -1

    return delayBetween([
		secure(zwave.switchBinaryV1.switchBinarySet(switchValue: 0xFF).format())
	],250)
}

def off() {
	if (logEnable) log.debug "Turn device OFF"
	state.bin = -1

    return delayBetween([
		secure(zwave.switchBinaryV1.switchBinarySet(switchValue: 0x00).format())
	],250)
}

def refresh() {
	log.info "refresh() is called"
	Map profile = resolveProfile()
	def cmds = []

	cmds << secure(zwave.switchBinaryV1.switchBinaryGet().format())
	PARAM_PREFS.each { row ->
		if (supportsParam(profile, row.num)) {
			cmds << cfgGet(profile, row.num)
		}
	}
	cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 2).format())
	cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 3).format())
	cmds << secure(zwave.versionV3.versionGet().format())

	return delayBetween(cmds, 250)
}

def installed() {
	configure()
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${logDesc == true}"  // FIX 2026-11-12 F6: txtEnable -> logDesc
    if (logEnable) runIn(1800,logsOff)

    if (state.lastUpdated && now() <= state.lastUpdated + 3000) return
    state.lastUpdated = now()

	Map profile = resolveProfile()
	def cmds = []

	// Associations    
    cmds << secure(zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId).format())

	def nodes = []
	nodes = parseAssocGroupList(settings.requestedGroup2, 2)
    cmds << secure(zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: []).format())
    cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes).format())
    cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 2).format())
    state.currentGroup2 = settings.requestedGroup2
	
   	nodes = parseAssocGroupList(settings.requestedGroup3, 3)
    cmds << secure(zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: []).format())
    cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes).format())
    cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 3).format())
    state.currentGroup3 = settings.requestedGroup3

	// Configuration parameters: ONLY those listed in the profile are written.
	// 3 LED behavior, 4 direction, 5 3-way add-on, 19 alternate exclusion, 34 LED color, 35 LED intensity, 36 guidelight intensity
	PARAM_PREFS.each { row ->
		if (supportsParam(profile, row.num)) {
			def raw = settings[row.setting]
			int val = (raw == null || raw.toString() == "") ? row.dflt : raw.toString().toInteger()
			cmds << cfgSet(profile, row.num, val)
			cmds << cfgGet(profile, row.num)
		} else if (logEnable) {
			log.debug "Parameter ${row.num} (${row.setting}) is not supported by ${profile.name}; skipped"
		}
	}
    
    delayBetween(cmds, 250)
}


def configure() {
    log.info "configure triggered"
	resolveProfile()
	state.bin = -1
	def cmds = []
	
	// Setup buttons
	sendEvent(name: "numberOfButtons", value: 6, displayed: false)
	
	// Associations    
	cmds << secure(zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId).format())

	def nodes = []
	nodes = parseAssocGroupList(settings.requestedGroup2, 2)
	cmds << secure(zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: []).format())
	cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes).format())
	cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 2).format())
	state.currentGroup2 = settings.requestedGroup2
	
   	nodes = parseAssocGroupList(settings.requestedGroup3, 3)
	cmds << secure(zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: []).format())
	cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes).format())
	cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 3).format())
	state.currentGroup3 = settings.requestedGroup3
	
	delayBetween(cmds, 250)
}

def setIndicatorBehavior(param) {
    log.info "setting indicator behavior to ${param}"
	Map profile = currentProfile()
	def paramValue = 0
    switch (param) {
        case "LED ON When Switch OFF":
            paramValue = 0
            break
        case "LED ON When Switch ON":
            paramValue = 1
            break
        case "LED Always OFF":
            paramValue = 2
            break
        case "LED Always ON":
            paramValue = 3
            break
        case "0":
        case "1":
        case "2":
        case "3":
            paramValue = param.toInteger()
            break
    }
    device.updateSetting("paramLED", [type: "enum", value: paramValue.toString()])
	delayBetween([
        cfgSet(profile, 3, paramValue),
        cfgGet(profile, 3)
    ], 250)
}

def setLedColor(colorName) {
	Map profile = currentProfile()
	if (!profile.colorLed || !supportsParam(profile, 34)) {
		log.warn "setLedColor ignored: ${profile.name} has no color LED"
		return
	}
	Integer colorValue = LED_COLORS[colorName?.toString()]
	if (colorValue == null && colorName?.toString()?.isInteger()) {
		colorValue = colorName.toString().toInteger()
	}
	if (colorValue == null || colorValue < 1 || colorValue > 8) {
		log.warn "setLedColor: invalid color '${colorName}'"
		return
	}
	log.info "setting LED color to ${colorName} (${colorValue})"
	device.updateSetting("paramLEDColor", [type: "enum", value: colorValue.toString()])
	delayBetween([
		cfgSet(profile, 34, colorValue),
		cfgGet(profile, 34)
	], 250)
}

private parseAssocGroupList(list, group) {
    def nodes = group == 2 ? [] : [zwaveHubNodeId]
    if (list) {
        def nodeList = list.split(',')
        def max = group == 2 ? 5 : 4
        def count = 0

        nodeList.each { node ->
            node = node.trim()
            if ( count >= max) {
                log.warn "Association Group ${group}: Too many members. Greater than ${max}! This one was discarded: ${node}"
            }
            else if (node.matches("\\p{XDigit}+")) {
                def nodeId = Integer.parseInt(node,16)
                if (nodeId == zwaveHubNodeId) {
                	log.warn "Association Group ${group}: Adding the hub ID as an association is not allowed."
                }
                else if ( (nodeId > 0) & (nodeId < 256) ) {
                    nodes << nodeId
                    count++
                }
                else {
                    log.warn "Association Group ${group}: Invalid member: ${node}"
                }
            }
            else {
                log.warn "Association Group ${group}: Invalid member: ${node}"
            }
        }
    }
	if (logEnable) log.debug "Nodes is $nodes"
    return nodes
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}
