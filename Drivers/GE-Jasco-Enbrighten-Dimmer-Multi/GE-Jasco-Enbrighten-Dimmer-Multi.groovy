/**
 *  IMPORT URL: https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/GE-Jasco-Enbrighten-Dimmer-Multi/GE-Jasco-Enbrighten-Dimmer-Multi.groovy
 *
 *  GE/Jasco Enbrighten Dimmer (Multi)
 *
 *  One driver for GE Enbrighten / Jasco / UltraPro Z-Wave Plus in-wall dimmers (500S, 700S and 800S series).
 *  The exact model is detected from the Z-Wave fingerprint (productType + productId) and a per-model
 *  profile (DEVICE_PROFILES) decides which configuration parameters are written to the device.
 *
 *  CREDIT:
 *    Based on the "GE Enbrighten Z-Wave Plus Dimmer" driver by Jason Bottjen (Botched1)
 *    https://github.com/Botched1/Hubitat/tree/master/Drivers/GE%20Enbrighten%20Dimmer
 *    All button / Central Scene / association / S2 behavior comes from that work.
 *    Developed with Cursor.
 *
 *  Author:    Heatvent
 *  Namespace: Heatvent
 *
 *  ---------------------------------------------------------------------------------------------------
 *  1.0.6 (2026-11-21) - importUrl in the driver definition so Hubitat's Import button can
 *                       refresh it.
 *  1.0.5 (2026-11-21) - Handle Basic Report v1/v2 at debug level (was logged as an unhandled warning)
 *  1.0.4 (2026-11-16) - Hover tooltips on non-obvious preferences (3-way, exclusion,
 *                       associations, profile override, switch mode, min dim, guidelight).
 *  1.0.3 (2026-11-14) - ConfigReport syncs preferences; association maxNodes from reports;
 *                       driverVersion Data Value; ZW3010DV fingerprint (param 20 min dim);
 *                       getParameter/setParameter, identify (Indicator CC);
 *                       factoryResetDevice command for param 84 on 700S/800S (UI confirm).
 *  1.0.2 (2026-11-14) - Live Config-Get verification: add params 30/31 to 500S dimmers; drop
 *                       param 6 (ramp) from 700S/800S (unsupported); clarify param 5 labels;
 *                       LED intensity prefs limited to documented 1-7 range.
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
 *                       - Includes the Heatvent maintenance fixes (2026-11-12) from the ZWA3016 patched driver:
 *                           F2 typo Guidelight Intensity title
 *                           F4 VersionReport logDebug -> logEnable
 *                           F5 held-down descriptionText said Up -> Down
 *                           F6 updated() txtEnable -> logDesc
 *                           F7 null LED color/intensity/guidelight defaults 5/4/4 (not 0)
 *                           F8 secure() on()/off() basicSet
 *                           F9 secure() min/max dim params 30/31
 *                           F10 association V4 -> V2 (matches commandClassVersions)
 *
 *  Original Botched1 history (abridged):
 *  1.0.0 (07/16/2019) - Initial Version
 *  1.4.0 (02/07/2020) - Added pushed, held, and released capability. Buttons 1/2=Up/Down, 3/4=Double Up/Down, 5/6=Triple Up/Down
 *  1.5.0 (05/17/2020) - Added associations and inverted paddle options
 *  2.0.0b (08/07/2020) - Added S2 capability for Hubitat 2.2.3 and newer
 *  2.2.0  (08/29/2020) - Added number of button config to configure
 *  2.3.0  (12/15/2020) - Added state for defaultDimmerLevel
 *  2.4.0  (02/13/2021) - Added Alternate Exclusion mode to preferences.
 *  2.5.0  (03/10/2021) - Fixed redundant ON events when changing dimmer level
 *  2.5.1  (03/08/2022) - Added setIndicatorBehavior command.
 *  2.6.0  (03/18/2023) - Change on/off behavior to match physical switch on/off behavior. Thanks to user michicago on the Hubitat forum.
 *  2.7.0  (03/27/2023) - Fixed setLevel duration conversion, thanks to user jpt1081 on hubitat forum for the idea/example code
*/


import groovy.transform.Field

@Field static final String DRIVER_VERSION = "1.0.6"

// Preference hover tips (Hubitat has no native tooltip on inputs — CSS + title HTML).
@Field static final String PREF_TIP_CSS = "<style>.tTip{display:inline-block;border-bottom:1px dotted currentColor;cursor:help}.tTip .tTipText{display:none;position:absolute;z-index:20;max-width:22rem;margin-top:.35rem;padding:.5rem .65rem;border-radius:6px;background:#222;color:#fff;font-weight:400;font-size:12px;line-height:1.35;white-space:normal}.tTip:hover .tTipText{display:block}</style>"
@Field static final String TIP_FORCE_PROFILE = "<div class='tTip'>Device Profile Override<span class='tTipText'>Leave Auto-detect unless the wrong model was detected. Override forces which parameter set is written; it does not change the physical device.</span></div>"
@Field static final String TIP_THREE_WAY = "<div class='tTip'>(5) 3-Way Type [700S/800S only]<span class='tTipText'>Add-On Switch: use with a Jasco/GE add-on (companion) paddle that talks to this smart dimmer. Standard 3-Way: use when the other switch is a plain mechanical 3-way. Wrong setting breaks multi-location control. Ignored on 500S / non-3-way models.</span></div>"
@Field static final String TIP_SWITCH_MODE = "<div class='tTip'>(16) Switch Mode<span class='tTipText'>Dimmer mode: full level control. On/Off switch mode: device behaves like a binary switch (no dimming) — useful for non-dimmable loads on a dimmer SKU.</span></div>"
@Field static final String TIP_ALT_EXCLUSION = "<div class='tTip'>(19) Alternate Exclusion<span class='tTipText'>How the device enters Z-Wave exclusion (remove) mode from the paddle. Leave Single paddle press unless the hub cannot exclude the device the normal way.</span></div>"
@Field static final String TIP_MIN_DIM = "<div class='tTip'>(30/20) Minimum Dim Threshold<span class='tTipText'>Lowest level the dimmer will go (1–99%). Raise this if LEDs flicker or drop out near the bottom. ZW3010DV uses param 20; other models use param 30 — the driver picks the right one from the profile.</span></div>"
@Field static final String TIP_GUIDELIGHT = "<div class='tTip'>(36) Guidelight Intensity [color LED models only]<span class='tTipText'>Brightness of the status LED when it is acting as a nightlight / locator (typically when the load is OFF, depending on LED Indicator Behavior). Separate from (35) LED Intensity.</span></div>"
@Field static final String TIP_ASSOC2 = "<div class='tTip'>Association Group 2 Members<span class='tTipText'>Direct Z-Wave control targets (hex node IDs, space or comma separated). Group 2 is typically mirrored with this dimmer. Max members is device-reported (often 5). Do not add the hub.</span></div>"
@Field static final String TIP_ASSOC3 = "<div class='tTip'>Association Group 3 Members<span class='tTipText'>Extra association targets (hex node IDs). Hub is already associated separately; leave room under the device-reported max. Advanced — leave blank unless you need device-to-device links.</span></div>"

// Base command class versions (700S / 800S). 500S profiles are downgraded in ccVersions().
@Field static Map commandClassVersions = 
    [
         0x26: 4 //3    //switchMultiLevel    //DIMMER
        ,0x70: 4 //2    //configuration
        ,0x72: 2    //Manufacturer Specific
        ,0x85: 2    //association
	    ,0x86: 3    //Version
        ,0x87: 3    //Indicator (identify on 700S/800S)
]

// Profiles keyed by "<productType>-<productId>" (4 hex digits each, lowercase, no 0x prefix).
// params : configuration parameter numbers this model supports (only these are written / read)
// Dimmer models do not use param 39 (relay control); that is switch-only.
@Field static Map DEVICE_PROFILES = [
    // ---- 500S classic dimmers (no color LED, no 3-way add-on parameter) ----
    // Live Config-Get on ZW3010 confirmed 4, 6, 30, 31 (Alliance pages were incomplete / inconsistent).
    "4944-3235": [name: "Enbrighten ZW3010 / 46203",   generation: "500S", colorLed: false, threeWay: false, relayControl: false, params: [3,4,6,16,19,30,31,32]]
   ,"4944-3236": [name: "Jasco ZW3010 / 46564",        generation: "500S", colorLed: false, threeWay: false, relayControl: false, params: [3,4,6,16,19,30,31,32]]
   ,"4944-3339": [name: "UltraPro ZW3010 / 39351",     generation: "500S", colorLed: false, threeWay: false, relayControl: false, params: [3,4,6,16,19,30,31,32]]
   ,"4944-3237": [name: "Enbrighten ZW3011 / 46204",   generation: "500S", colorLed: false, threeWay: false, relayControl: false, params: [3,4,6,16,19,30,31,32]]
   ,"4944-3238": [name: "Jasco ZW3011 / 46565",        generation: "500S", colorLed: false, threeWay: false, relayControl: false, params: [3,4,6,16,19,30,31,32]]
   ,"4944-3330": [name: "Enbrighten ZW3010DV / 43105", generation: "500S", colorLed: false, threeWay: false, relayControl: false, params: [3,6,16,19,20,32]]
    // ---- 700S color LED (no param 6 — Config-Get returned empty; manuals omit it) ----
   ,"4944-3430": [name: "Enbrighten ZWA3016 / 58438",  generation: "700S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,16,19,30,31,32,34,35,36]]
   ,"4944-3431": [name: "Enbrighten ZWA3017 / 59341",  generation: "700S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,16,19,30,31,32,34,35,36]]
   ,"4944-3434": [name: "Jasco ZWA3016 / 59383",       generation: "700S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,16,19,30,31,32,34,35,36]]
   ,"4944-3435": [name: "Jasco ZWA3017 / 59384",       generation: "700S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,16,19,30,31,32,34,35,36]]
   ,"4944-3433": [name: "UltraPro ZWA3016 / 59350",    generation: "700S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,16,19,30,31,32,34,35,36]]
    // ---- 800S color LED (manuals match 700S set; no param 6) ----
   ,"4944-3537": [name: "Enbrighten ZWN3018 / 76593",  generation: "800S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,16,19,30,31,32,34,35,36]]
   ,"4944-3630": [name: "Enbrighten ZWN3019 / 76595",  generation: "800S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,16,19,30,31,32,34,35,36]]
   ,"4944-3539": [name: "Jasco ZWN3018 / 76597",       generation: "800S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,16,19,30,31,32,34,35,36]]
   ,"4944-3631": [name: "Jasco ZWN3019 / 76599",       generation: "800S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,16,19,30,31,32,34,35,36]]
   ,"4944-3538": [name: "UltraPro ZWN3020 / 76602",    generation: "800S", colorLed: true, threeWay: true, relayControl: false, params: [3,4,5,16,19,30,31,32,34,35,36]]
]

// Used when the model is not recognised: core parameters only, no color LED writes.
@Field static Map FALLBACK_PROFILE = [name: "Unknown model (conservative)", generation: "unknown", colorLed: false, threeWay: false, relayControl: false, params: [3,4,6,16,19,30,31,32]]

// Parameter number -> preference name and default (written in this order)
@Field static List PARAM_PREFS = [
     [num: 3,  setting: "paramLED",                dflt: 0]
    ,[num: 4,  setting: "paramInverted",           dflt: 0]
    ,[num: 5,  setting: "paramThreeWayType",       dflt: 0]
    ,[num: 6,  setting: "paramUpDownRate",         dflt: 0]    //DIMMER
    ,[num: 16, setting: "paramSwitchMode",         dflt: 0]    //DIMMER
    ,[num: 19, setting: "paramAlternateExclusion", dflt: 0]
    ,[num: 20, setting: "paramMinDim",             dflt: 1]    //DIMMER ZW3010DV uses 20 instead of 30
    ,[num: 30, setting: "paramMinDim",             dflt: 1]    //DIMMER
    ,[num: 31, setting: "paramMaxDim",             dflt: 99]   //DIMMER
    ,[num: 32, setting: "paramDefaultDim",         dflt: 0]    //DIMMER
    ,[num: 34, setting: "paramLEDColor",           dflt: 5]    // F7 Blue
    ,[num: 35, setting: "paramIntensity",          dflt: 4]    // F7
    ,[num: 36, setting: "paramGuidelightIntensity",dflt: 4]    // F7
]

@Field static Map LED_COLORS = ["Red": 1, "Orange": 2, "Yellow": 3, "Green": 4, "Blue": 5, "Pink": 6, "Purple": 7, "White": 8]

metadata {
	definition (name: "GE/Jasco Enbrighten Dimmer (Multi)", namespace: "Heatvent", author: "Heatvent", importUrl: "https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/GE-Jasco-Enbrighten-Dimmer-Multi/GE-Jasco-Enbrighten-Dimmer-Multi.groovy") {
		capability "Actuator"
        capability "DoubleTapableButton"
        capability "HoldableButton"
		capability "PushableButton"
        capability "ReleasableButton"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
		capability "Light"

		command "setDefaultDimmerLevel", [[name:"Default ON %",type:"NUMBER", description:"Default Dimmer Level Used when Turning ON. (0=Last Dimmer Value)", range: "0..99"]]
		command "setIndicatorBehavior", [[name:"LED Indicator Behavior", type: "ENUM", description: "", constraints: ["LED ON When Switch OFF", "LED ON When Switch ON", "LED Always OFF", "LED Always ON", "0", "1", "2", "3"] ] ]
		command "setLedColor", [[name:"LED Color (color LED models only)", type: "ENUM", description: "Param 34. Ignored on models without a color LED.", constraints: ["Red", "Orange", "Yellow", "Green", "Blue", "Pink", "Purple", "White"] ] ]
		command "getParameter", [[name:"Parameter Number*", type: "NUMBER", description: "Configuration parameter to read", range: "1..255"]]
		command "setParameter", [[name:"Parameter Number*", type: "NUMBER", description: "Configuration parameter to write (not 84 — use factoryResetDevice)", range: "1..255"], [name:"Value*", type: "NUMBER", description: "Value to write", range: "0..255"]]
		command "identify", [[name:"Identify", type: "ENUM", description: "Blink the indicator via Indicator CC (700S/800S)", constraints: ["Blink"] ] ]
		command "factoryResetDevice", [[name:"Confirm factory reset (param 84)", type: "ENUM", description: "Writes param 84=1 on 700S/800S. Device leaves the Z-Wave network.", constraints: ["RESET"] ] ]
        
		// 500S classic
		fingerprint mfr:"0063", prod:"4944", deviceId:"3235", inClusters:"0x5E,0x26,0x85,0x59,0x86,0x72,0x55,0x5A,0x73,0x5B,0x9F,0x6C,0x70,0x22,0x7A", controllerType: "ZWV" // Enbrighten ZW3010
		fingerprint mfr:"0063", prod:"4944", deviceId:"3236" // Jasco ZW3010
		fingerprint mfr:"0063", prod:"4944", deviceId:"3339", inClusters:"0x5E,0x26,0x85,0x59,0x86,0x72,0x55,0x5A,0x73,0x5B,0x9F,0x6C,0x70,0x22,0x7A", controllerType: "ZWV" // UltraPro ZW3010
		fingerprint mfr:"0063", prod:"4944", deviceId:"3237" // Enbrighten ZW3011
		fingerprint mfr:"0063", prod:"4944", deviceId:"3238" // Jasco ZW3011
		fingerprint mfr:"0063", prod:"4944", deviceId:"3330" // Enbrighten ZW3010DV / 43105
		// 700S color
		fingerprint mfr:"0063", prod:"4944", deviceId:"3430", inClusters:"0x5E,0x22,0x85,0x8E,0x59,0x26,0x87,0x55,0x86,0x72,0x5A,0x73,0x70,0x9F,0x6C,0x5B,0x7A", controllerType: "ZWV" // Enbrighten ZWA3016
		fingerprint mfr:"0063", prod:"4944", deviceId:"3431" // Enbrighten ZWA3017
		fingerprint mfr:"0063", prod:"4944", deviceId:"3434" // Jasco ZWA3016
		fingerprint mfr:"0063", prod:"4944", deviceId:"3435" // Jasco ZWA3017
		fingerprint mfr:"0063", prod:"4944", deviceId:"3433" // UltraPro ZWA3016
		// 800S color
		fingerprint mfr:"0063", prod:"4944", deviceId:"3537" // Enbrighten ZWN3018
		fingerprint mfr:"0063", prod:"4944", deviceId:"3630" // Enbrighten ZWN3019
		fingerprint mfr:"0063", prod:"4944", deviceId:"3539" // Jasco ZWN3018
		fingerprint mfr:"0063", prod:"4944", deviceId:"3631" // Jasco ZWN3019
		fingerprint mfr:"0063", prod:"4944", deviceId:"3538" // UltraPro ZWN3020
}

 preferences {
        input name: "profileNote", type: "paragraph", element: "paragraph", title: "Model-specific settings", description: PREF_TIP_CSS + "All settings are listed for every model. Settings your dimmer does not support (for example color LED options on non-color models, or ramp rate on 700S/800S) are ignored and never written to the device. The detected model is stored in device Data Values (profileKey, profileName, profileGeneration) and in state. Dotted labels below show extra help on hover."
        input name: "forceProfile", type: "enum", title: TIP_FORCE_PROFILE, multiple: false, options: ["auto" : "Auto-detect (default)", "4944-3235" : "4944-3235 Enbrighten ZW3010 / 46203 [500S]", "4944-3236" : "4944-3236 Jasco ZW3010 / 46564 [500S]", "4944-3339" : "4944-3339 UltraPro ZW3010 / 39351 [500S]", "4944-3237" : "4944-3237 Enbrighten ZW3011 / 46204 [500S]", "4944-3238" : "4944-3238 Jasco ZW3011 / 46565 [500S]", "4944-3330" : "4944-3330 Enbrighten ZW3010DV / 43105 [500S]", "4944-3430" : "4944-3430 Enbrighten ZWA3016 / 58438 [700S]", "4944-3431" : "4944-3431 Enbrighten ZWA3017 / 59341 [700S]", "4944-3434" : "4944-3434 Jasco ZWA3016 / 59383 [700S]", "4944-3435" : "4944-3435 Jasco ZWA3017 / 59384 [700S]", "4944-3433" : "4944-3433 UltraPro ZWA3016 / 59350 [700S]", "4944-3537" : "4944-3537 Enbrighten ZWN3018 / 76593 [800S]", "4944-3630" : "4944-3630 Enbrighten ZWN3019 / 76595 [800S]", "4944-3539" : "4944-3539 Jasco ZWN3018 / 76597 [800S]", "4944-3631" : "4944-3631 Jasco ZWN3019 / 76599 [800S]", "4944-3538" : "4944-3538 UltraPro ZWN3020 / 76602 [800S]"], required: false, displayDuringSetup: true, defaultValue: "auto"
        input name: "paramLED", type: "enum", title: "(3) LED Indicator Behavior", multiple: false, options: ["0" : "LED ON When Switch OFF (default)", "1" : "LED ON When Switch ON", "2" : "LED Always OFF", "3" : "LED Always ON"], required: false, displayDuringSetup: true, defaultValue: "0"
        input name: "paramInverted", type: "enum", title: "(4) Switch Buttons Direction", multiple: false, options: ["0" : "Normal (default)", "1" : "Inverted"], required: false, displayDuringSetup: true, defaultValue: "0"
		input name: "paramThreeWayType", type: "enum", title: TIP_THREE_WAY, multiple: false, options: ["0" : "Add-On Switch (default)", "1" : "Standard 3-Way Switch"], required: false, displayDuringSetup: true, defaultValue: "0"
        input name: "paramUpDownRate", type: "enum", title: "(6) Dim up/down rate [500S only]", multiple: false, options: ["0" : "Fast (default)", "1" : "Slow"], required: false, displayDuringSetup: true, defaultValue: "0" //DIMMER
        input name: "paramSwitchMode", type: "enum", title: TIP_SWITCH_MODE, multiple: false, options: ["0" : "Dimmer mode (default)", "1" : "On/Off switch mode"], required: false, displayDuringSetup: true, defaultValue: "0" //DIMMER
		input name: "paramAlternateExclusion", type: "enum", title: TIP_ALT_EXCLUSION, multiple: false, options: ["0" : "Single paddle press (default)", "1" : "Alternate exclusion sequence / menu"], required: false, displayDuringSetup: true, defaultValue: "0"
		input name: "paramMinDim", type: "number", title: TIP_MIN_DIM, multiple: false, range: "1..99", required: false, displayDuringSetup: true, defaultValue: "1" //DIMMER
        input name: "paramMaxDim", type: "number", title: "(31) Maximum Brightness Threshold (1-99%)", multiple: false, range: "1..99", required: false, displayDuringSetup: true, defaultValue: "99" //DIMMER
        input name: "paramDefaultDim", type: "number", title: "(32) Default ON % (0 = last level)", multiple: false, range: "0..99", required: false, displayDuringSetup: true, defaultValue: "0"
        input name: "paramLEDColor", type: "enum", title: "(34) LED Light Color [color LED models only]", multiple: false, options: ["1" : "Red", "2" : "Orange", "3" : "Yellow", "4" : "Green", "5" : "Blue (default)", "6" : "Pink", "7" : "Purple", "8" : "White"], required: false, displayDuringSetup: true, defaultValue: "5"
        input name: "paramIntensity", type: "enum", title: "(35) LED Intensity [color LED models only]", multiple: false, options: ["1" : "1 (lowest)", "2" : "2", "3" : "3", "4" : "4 (default)", "5" : "5", "6" : "6", "7" : "7 (highest)"], required: false, displayDuringSetup: true, defaultValue: "4"
        // FIX 2026-11-12 F2: typo Intesity -> Intensity
        input name: "paramGuidelightIntensity", type: "enum", title: TIP_GUIDELIGHT, multiple: false, options: ["1" : "1 (lowest)", "2" : "2", "3" : "3", "4" : "4 (default)", "5" : "5", "6" : "6", "7" : "7 (highest)"], required: false, displayDuringSetup: true, defaultValue: "4"
        input name: "requestedGroup2", type: "text", title: TIP_ASSOC2, required: false
        input name: "requestedGroup3", type: "text", title: TIP_ASSOC3, required: false
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
	    input name: "logDesc", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Profile handling
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Normalise a productType / productId value to 4 lowercase hex digits (no 0x).
// Accepts "0x4944", "4944" (hex as shown in fingerprints) or decimal "18756" (as stored in Hubitat data values).
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
    updateDataValue("driverVersion", DRIVER_VERSION)
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
        versions[0x26] = 3
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

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
    if (logEnable) log.debug "---BASIC SET V1--- ${device.displayName} sent ${cmd}"
	if (logEnable) log.debug "This does nothing in this driver, and can be ignored..."
}

def zwaveEvent(hubitat.zwave.commands.basicv2.BasicSet cmd) {
    if (logEnable) log.debug "---BASIC SET V2--- ${device.displayName} sent ${cmd}"
	if (logEnable) log.debug "This does nothing in this driver, and can be ignored..."
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
	if (logEnable) log.debug "---ASSOCIATION REPORT V2--- ${device.displayName} sent groupingIdentifier: ${cmd.groupingIdentifier} maxNodesSupported: ${cmd.maxNodesSupported} nodeId: ${cmd.nodeId} reportsToFollow: ${cmd.reportsToFollow}"
	if (cmd.maxNodesSupported != null) {
		Integer maxN = cmd.maxNodesSupported as Integer
		state["assocMaxGroup${cmd.groupingIdentifier}"] = maxN
		updateDataValue("assocMaxGroup${cmd.groupingIdentifier}", maxN.toString())
	}
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
	handleConfigurationReport(cmd)
}

// FIX 2026-11-12 F3: 700S/800S profiles use Configuration CC v4; handle v4 reports
def zwaveEvent(hubitat.zwave.commands.configurationv4.ConfigurationReport cmd) {
	handleConfigurationReport(cmd)
}

private void handleConfigurationReport(cmd) {
	Integer param = cmd.parameterNumber as Integer
	Integer value = cmd.scaledConfigurationValue as Integer
	if (logEnable) log.debug "---CONFIGURATION REPORT--- ${device.displayName} param ${param} = ${value}"
	def row = PARAM_PREFS.find { it.num == param }
	if (!row) return
	String val = value.toString()
	// Number-style prefs (dimmer thresholds / default level); everything else is enum
	if (param in [20, 30, 31, 32]) {
		device.updateSetting(row.setting, [type: "number", value: val])
	} else {
		device.updateSetting(row.setting, [type: "enum", value: val])
	}
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {                                 //DIMMER
    if (logEnable) log.debug "---BINARY SWITCH REPORT V1--- ${device.displayName} sent ${cmd}"
	if (logEnable) log.debug "This report does nothing in this driver, and shouldn't have been called..."    
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv2.SwitchBinaryReport cmd) {                                 //DIMMER
    if (logEnable) log.debug "---BINARY SWITCH REPORT V2--- ${device.displayName} sent ${cmd}"
	if (logEnable) log.debug "This report does nothing in this driver, and shouldn't have been called..."    
}

def zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
   if (logEnable) log.debug "VersionReport: ${cmd}"  // FIX 2026-11-12 F4: logDebug -> logEnable
   device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion.toString().padLeft(2,'0')}")
   device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
   device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

def zwaveEvent(hubitat.zwave.commands.versionv3.VersionZWaveSoftwareReport cmd) {
	if (logEnable) log.debug "---SOFTWARE REPORT V3--- ${device.displayName} sent ${cmd}"
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

private void handleSwitchMultilevelReport(cmd) {                                                                //DIMMER
	def newType
	
	// check state.bin variable to see if event is digital or physical
	if (state.bin == -1)
	{
		newType = "digital"
	}
	else
	{
		newType = "physical"
	}
	
	// Reset state.bin variable
	state.bin = 0
	
	if (cmd.value) {
		// Set switch status
        if (device.currentValue("switch") == "off") {
			if (logDesc) log.info "$device.displayName was turned on [$newType]"
			sendEvent(name: "switch", value: "on", descriptionText: "$device.displayName was turned on [$newType]", type: "$newType")
		}

		// Send level event
		sendEvent(name: "level", value: cmd.value, unit: "%", descriptionText: "$device.displayName is " + cmd.value + "%", type: "$newType")

		// Update state.level
		state.level = cmd.value

		// info logging
		if (logDesc) log.info "$device.displayName is " + cmd.value + "%"

	} else {
		sendEvent(name: "switch", value: "off", descriptionText: "$device.displayName was turned off [$newType]", type: "$newType")
		if (logDesc) log.info "$device.displayName was turned off [$newType]"
	}
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {                        //DIMMER
	if (logEnable) log.debug "---SwitchMultilevelReport V3---  ${device.displayName} sent ${cmd}"
	handleSwitchMultilevelReport(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv4.SwitchMultilevelReport cmd) {                        //DIMMER
	if (logEnable) log.debug "---SwitchMultilevelReport V4---  ${device.displayName} sent ${cmd}"
	handleSwitchMultilevelReport(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelSet cmd) {                    //DIMMER
	if (logEnable) log.debug "SwitchMultilevelSet V3 Called."
	if (logEnable) log.debug "This does nothing in this driver, and shouldn't have been called..."
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv4.SwitchMultilevelSet cmd) {                    //DIMMER
	if (logEnable) log.debug "SwitchMultilevelSet V4 Called."
	if (logEnable) log.debug "This does nothing in this driver, and shouldn't have been called..."
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

def zwaveEvent(hubitat.zwave.commands.indicatorv3.IndicatorReport cmd) {
	if (logEnable) log.debug "IndicatorReport: ${cmd}"
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    // FIX 2026-11-21 F11: Basic Report is sent alongside the switch/level report; the state is
    // already handled there, so log at debug instead of falling through to the unhandled warning.
    if (logEnable) log.debug "---BASIC REPORT V1--- ${device.displayName} sent ${cmd}"
}

def zwaveEvent(hubitat.zwave.commands.basicv2.BasicReport cmd) {
    // FIX 2026-11-21 F11: Basic Report is sent alongside the switch/level report; the state is
    // already handled there, so log at debug instead of falling through to the unhandled warning.
    if (logEnable) log.debug "---BASIC REPORT V2--- ${device.displayName} sent ${cmd}"
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    log.warn "${device.displayName} received unhandled command: ${cmd}"
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Driver Commands / Functions
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def on() {
    if (logEnable) log.debug "Turn device ON"
    // FIX 2026-11-12 F8: secure encapsulation for S2 devices
    secure(zwave.basicV1.basicSet(value: 0xFF).format())
}

def off() {
    if (logEnable) log.debug "Turn device OFF"
    // FIX 2026-11-12 F8: secure encapsulation for S2 devices
    secure(zwave.basicV1.basicSet(value: 0x00).format())
}

def setLevel(value) {
	if (logEnable) log.debug "setLevel($value)"
	setLevel(value, 0)
}

def setLevel(value, duration) {
	if (logEnable) log.debug "setLevel($value, $duration)"
	def result = []
	state.bin = -1
	
	def getStatusDelay
    
    	// Clip to 7620s max on duration to stay within zwave parameter max value of 254
    	duration = Math.min(duration.toInteger(), 7620)
    
	//Convert seconds to minutes when duration is above 120s
	if (duration > 120) {
		duration = Math.round(duration / 60) + 127
		getStatusDelay = ((duration - 127) * 60 * 1000 + 1000).toInteger()	    
	} else {
		getStatusDelay = (duration * 1000 + 1000).toInteger()	
	}
    
	// Clip value to min/max of zwave spec
	value = Math.max(Math.min(value.toInteger(), 99), 0)
	
	if (value) {state.level = value}

	if (logEnable) log.debug "setLevel(value, duration) >> value: $value, duration: $duration"

	return delayBetween([
		secure(zwave.switchMultilevelV3.switchMultilevelSet(value: value, dimmingDuration: duration))
        ] , 250)
}

def setDefaultDimmerLevel(value) {
	Map profile = currentProfile()
	if (!supportsParam(profile, 32)) {
		log.warn "setDefaultDimmerLevel ignored: parameter 32 is not supported by ${profile.name}"
		return
	}
	if (logEnable) log.debug "Setting default dimmer level: ${value}"
	
	value = Math.max(Math.min(value.toInteger(), 99), 0)
	state.defaultDimmerLevel = value
	sendEvent([name:"defaultDimmerLevel", descriptionText: "$device.displayName defaultDimmerLevel set to $value%", value: value, displayed:true])
	
	return delayBetween([
        cfgSet(profile, 32, value)
        ,cfgGet(profile, 32)
        ], 250)
}

def refresh() {
	log.info "refresh() is called"
	Map profile = resolveProfile()
	def cmds = []

	cmds << secure(zwave.switchMultilevelV3.switchMultilevelGet())                            //DIMMER
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
	updateDataValue("driverVersion", DRIVER_VERSION)
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
    cmds << secure(zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: []).format())  // FIX 2026-11-12 F10
    cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes).format())
    cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 2).format())
    state.currentGroup2 = settings.requestedGroup2
	
   	nodes = parseAssocGroupList(settings.requestedGroup3, 3)
    cmds << secure(zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: []).format())
    cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes).format())
    cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 3).format())
    state.currentGroup3 = settings.requestedGroup3

	// Configuration parameters: ONLY those listed in the profile are written.
	// 3 LED behavior, 4 direction, 5 3-way add-on, 6 ramp rate, 16 switch mode, 19 alternate exclusion,
	// 30/31 min/max dim (F9 secure), 32 default ON %, 34 LED color, 35 LED intensity, 36 guidelight intensity
	PARAM_PREFS.each { row ->
		if (supportsParam(profile, row.num)) {
			def raw = settings[row.setting]
			int val = (raw == null || raw.toString() == "") ? row.dflt : new BigDecimal(raw.toString()).intValue()
			cmds << cfgSet(profile, row.num, val)
			cmds << cfgGet(profile, row.num)
		} else if (logEnable) {
			log.debug "Parameter ${row.num} (${row.setting}) is not supported by ${profile.name}; skipped"
		}
	}

    delayBetween(cmds, 500)
}

def configure() {
	log.info "configure triggered"
	resolveProfile()
	state.bin = -1
	if (state.level == "") {state.level = 99} //DIMMER
	def cmds = []

	// configure buttons
	sendEvent(name: "numberOfButtons", value: 6, displayed: false)
	
	// Associations    
	cmds << secure(zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId).format())

	def nodes = []
	nodes = parseAssocGroupList(settings.requestedGroup2, 2)
	cmds << secure(zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: []).format())  // FIX 2026-11-12 F10
	cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes).format())
	cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 2).format())
	state.currentGroup2 = settings.requestedGroup2
	
   	nodes = parseAssocGroupList(settings.requestedGroup3, 3)
	cmds << secure(zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: []).format())
	cmds << secure(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes).format())
	cmds << secure(zwave.associationV2.associationGet(groupingIdentifier: 3).format())
	state.currentGroup3 = settings.requestedGroup3

	delayBetween(cmds, 500)
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
        Integer reported = state["assocMaxGroup${group}"] as Integer
        // Group 2: all slots are user nodes. Group 3: hub occupies one slot for button reports.
        def max
        if (group == 2) {
            max = reported ?: 5
        } else {
            Integer supported = reported ?: 5
            max = Math.max(0, supported - 1)
        }
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


def getParameter(paramNum) {
	Map profile = currentProfile()
	int p = paramNum as Integer
	log.info "getParameter(${p})"
	delayBetween([cfgGet(profile, p)], 250)
}

def setParameter(paramNum, value) {
	Map profile = currentProfile()
	int p = paramNum as Integer
	int v = value as Integer
	if (p == 84) {
		log.warn "setParameter: use factoryResetDevice(RESET) for param 84"
		return
	}
	log.info "setParameter(${p}, ${v})"
	delayBetween([
		cfgSet(profile, p, v),
		cfgGet(profile, p)
	], 250)
}

def identify(unused = null) {
	Map profile = currentProfile()
	if (!(profile.generation in ["700S", "800S"])) {
		log.warn "identify: Indicator CC is primarily supported on 700S/800S (${profile.name})"
	}
	log.info "${device.displayName}: identify (Indicator blink)"
	List indicators = [
		[indicatorId: 0x50, propertyId: 0x03, value: 0x08],
		[indicatorId: 0x50, propertyId: 0x04, value: 0x03],
		[indicatorId: 0x50, propertyId: 0x05, value: 0x06]
	]
	delayBetween([
		secure(zwave.indicatorV3.indicatorSet(indicatorCount: 3, value: 0, indicatorValues: indicators).format())
	], 250)
}

def factoryResetDevice(confirm) {
	if (confirm?.toString() != "RESET") {
		log.warn "factoryResetDevice aborted: select RESET to confirm"
		return
	}
	Map profile = currentProfile()
	if (!(profile.generation in ["700S", "800S"])) {
		log.warn "factoryResetDevice: param 84 is not available on ${profile.name} (${profile.generation})"
		return
	}
	log.warn "${device.displayName}: sending Z-Wave factory reset (param 84=1). Device will leave the network and need re-inclusion."
	delayBetween([
		cfgSet(profile, 84, 1)
	], 250)
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}
