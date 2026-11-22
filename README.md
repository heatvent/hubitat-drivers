# Heatvent Hubitat Drivers

Hubitat drivers maintained by [Heatvent](https://github.com/heatvent).

## Install with Hubitat Package Manager (recommended)

1. In **Hubitat Package Manager** → **Package Manager Settings** → **Add a Custom Repository**
2. Enter: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/repository.json`
3. **Install** → search for the package (for example **GE/Jasco Enbrighten Switch & Dimmer (Multi)** or **Relay Garage Door**)

This repository is in the [HubitatCommunity package list](https://github.com/HubitatCommunity/hubitat-packagerepositories), so its packages also appear under **Install → Search by Keywords** without adding a custom repository.

### Manual Import URLs

- Switch: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/GE-Jasco-Enbrighten-Switch-Multi/GE-Jasco-Enbrighten-Switch-Multi.groovy`
- Dimmer: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/GE-Jasco-Enbrighten-Dimmer-Multi/GE-Jasco-Enbrighten-Dimmer-Multi.groovy`

---

## GE/Jasco Enbrighten Switch & Dimmer (Multi)

Two profile-driven drivers (Switch and Dimmer) that auto-detect the model from Z-Wave `productType` + `productId` and **only write configuration parameters that model supports**. Preferences for unsupported features still appear in the UI but are ignored on Save.

Detected identity is stored in device **Data Values**: `profileKey`, `profileName`, `profileGeneration`, `profileSource`, `profileColorLed`, `profileThreeWay`, plus `driverVersion` (and mirrored in driver state). Association group max-node counts from the device are stored as `assocMaxGroup2` / `assocMaxGroup3` when reported.

**Credit:** Based on Jason Bottjen ([Botched1](https://github.com/Botched1/Hubitat)) GE Enbrighten Switch/Dimmer drivers.

### How profile selection works

1. **Auto-detect** (default): fingerprint → profile key `TTTT-IIII` (hex productType-productId).
2. **Device Profile Override** preference: force a known profile if detection is wrong.
3. **Unknown fingerprint**: conservative fallback (core params only; no color LED / 3-way / relay writes).

---

### Supported switches (`GE/Jasco Enbrighten Switch (Multi)`)

| Generation | Brand / model | SKU examples | Profile key | Params written |
|------------|---------------|--------------|-------------|----------------|
| **500S** | Enbrighten / Jasco / UltraPro **ZW4008** (paddle) | 46201, 46562, 39348 | `4952-3135` / `3136` / `3237` | 3, 4, 19 |
| **500S** | Enbrighten **ZW4008DV** (120+277) | 43072 | `4952-3139` | 3, 4, 19 |
| **500S** | Enbrighten / Jasco / UltraPro **ZW4009** (toggle) | 46202, 46563, 39354 | `4952-3137` / `3138` / `3238` | 3, 4, 19 |
| **500S** | Enbrighten **ZW4009DV** (120+277) | 43074 | `4952-3231` | 3, 4, 19 |
| **700S** | Enbrighten / Jasco / UltraPro **ZWA4011** (+ DV) | 58433, 59337, 59381, 59347 | `4952-3239` / `3331` / `3335` / `3333` | 3, 4, 5, 19, 34–36, 39 |
| **700S** | Enbrighten / Jasco / UltraPro **ZWA4012** (+ DV) | 58436, 59338, 59382, 59368 | `4952-3330` / `3332` / `3336` / `3334` | 3, 4, 5, 19, 34–36, 39 |
| **800S** | Enbrighten / Jasco **ZWN4015** | 76591, 76596 | `4952-3435` / `3437` | 3, 4, 5, 19, 34–36, 39 |
| **800S** | Enbrighten / Jasco **ZWN4016** | 76592, 76598 | `4952-3438` / `3530` | 3, 4, 5, 19, 34–36, 39 |
| **800S** | UltraPro **ZWN4017** | 76601 | `4952-3534` | 3, 4, 5, 19, 34–36, 39 |

### Supported dimmers (`GE/Jasco Enbrighten Dimmer (Multi)`)

| Generation | Brand / model | SKU examples | Profile key | Params written |
|------------|---------------|--------------|-------------|----------------|
| **500S** | Enbrighten / Jasco / UltraPro **ZW3010** | 46203, 46564, 39351 | `4944-3235` / `3236` / `3339` | 3, 4, 6, 16, 19, 30–32 |
| **500S** | Enbrighten **ZW3010DV** (120+277) | 43105 | `4944-3330` | 3, 6, 16, 19, **20**, 32 |
| **500S** | Enbrighten / Jasco **ZW3011** | 46204, 46565 | `4944-3237` / `3238` | 3, 4, 6, 16, 19, 30–32 |
| **700S** | Enbrighten / Jasco / UltraPro **ZWA3016** | 58438, 59383, 59350 | `4944-3430` / `3434` / `3433` | 3, 4, 5, 16, 19, 30–32, 34–36 |
| **700S** | Enbrighten / Jasco **ZWA3017** | 59341, 59384 | `4944-3431` / `3435` | 3, 4, 5, 16, 19, 30–32, 34–36 |
| **800S** | Enbrighten / Jasco **ZWN3018** | 76593, 76597 | `4944-3537` / `3539` | 3, 4, 5, 16, 19, 30–32, 34–36 |
| **800S** | Enbrighten / Jasco **ZWN3019** | 76595, 76599 | `4944-3630` / `3631` | 3, 4, 5, 16, 19, 30–32, 34–36 |
| **800S** | UltraPro **ZWN3020** | 76602 | `4944-3538` | 3, 4, 5, 16, 19, 30–32, 34–36 |

> **Note:** Param **6** (dim ramp rate) is **500S-only**. Param **39** (relay / smart-bulb mode) is **switch-only**. Verified with live Config-Get on Banbury hub (500S switch/dimmer, 700S switch/dimmer) plus Z-Wave Alliance / Jasco manuals.

---

### Configuration parameters (what each setting does)

| Param | Preference | Applies to | Values / meaning |
|------:|------------|------------|------------------|
| **3** | LED Indicator Behavior | All | `0` LED on when load off (guidelight, default); `1` LED on when load on; `2` always off; `3` always on |
| **4** | Switch Buttons Direction | All | `0` normal paddle orientation; `1` inverted (installed upside-down) |
| **5** | 3-Way Type | 700S/800S | `0` Add-On Switch (default); `1` Standard 3-Way traveler switch |
| **6** | Dim up/down rate | **500S dimmer only** | `0` fast ramp (default); `1` slow ramp (Z-Wave level changes other than 0/FF) |
| **16** | Switch Mode | Dimmers | `0` dimmer mode (default); `1` on/off switch mode (levels map to full on) |
| **19** | Alternate Exclusion | All | `0` exclude with a single paddle press (default); `1` alternate exclusion sequence / menu (model-dependent) |
| **20** | Minimum Dim Threshold | **ZW3010DV only** | Same meaning as param 30; this SKU uses param **20** instead |
| **30** | Minimum Dim Threshold | Dimmers (except ZW3010DV) | `1`–`99` % lowest level the dimmer will use |
| **31** | Maximum Brightness Threshold | Dimmers | `1`–`99` % highest level the dimmer will use |
| **32** | Default ON % | Dimmers | `0` restore last level (default); `1`–`99` always turn on to that % |
| **34** | LED Light Color | Color LED models | `1` Red … `5` Blue (default) … `8` White |
| **35** | LED Intensity | Color LED models | `1` (lowest) – `7` (highest); default `4` |
| **36** | Guidelight Intensity | Color LED models | Brightness of the indicator when param 3 is guidelight mode; `1`–`7`, default `4` |
| **39** | Relay Control | **700S/800S switches** | `0` paddle controls relay (default); `1` **smart bulb mode** — relay stays off, line power stays on (use with care) |

Param **84** (Z-Wave factory reset) on **700S/800S** is exposed only via the `factoryResetDevice(RESET)` command (not a sticky preference), so it cannot be triggered accidentally by Save.

---

### Commands and scene / association behavior

**Both drivers**

| Command / feature | Behavior |
|-------------------|----------|
| `on` / `off` | Control the load |
| `refresh` | Read switch/level state, supported config params, associations, version |
| `configure` | Associations + button setup; re-resolves profile |
| `setIndicatorBehavior` | Sets param **3** (same options as the preference) |
| `setLedColor` | Sets param **34** on color-LED models; ignored otherwise |
| `setRelayControl` | **Switch only** — sets param **39** (paddle vs smart-bulb mode); warns on smart-bulb |
| `getParameter` / `setParameter` | Read/write any config param (debug); param **84** is blocked — use `factoryResetDevice` |
| `identify` | Indicator CC blink (primarily 700S/800S) |
| `factoryResetDevice` | Writes param **84=1** on 700S/800S after selecting **RESET**; device leaves the network |
| Central Scene buttons | Up/Down: push (1/2), hold (1/2), release (1/2), double (3/4), triple (5/6); also `doubleTapped` 1/2 |
| Association group 1 | Lifeline (hub) |
| Association group 2 | Single-press Basic Set to listed nodes (device-reported max; typically 5) |
| Association group 3 | Double-press Basic Set; includes hub for button reports (device-reported max minus hub) |
| Config reports → prefs | After refresh/`updated`, reported values sync into preferences so the UI matches the device |

**Dimmer only**

| Command / feature | Behavior |
|-------------------|----------|
| `setLevel` | Multilevel set (optional duration) |
| `setDefaultDimmerLevel` | Sets param **32** (default ON %) |

---

### Preferences that are not Z-Wave config params

| Preference | Purpose |
|------------|---------|
| Device Profile Override | Force a profile instead of auto-detect |
| Association Group 2 / 3 Members | Hex node IDs for associations (comma-separated) |
| Enable debug logging | Verbose driver logs (auto-off after 30 min) |
| Enable descriptionText logging | Info-level user-facing event text |

---

### Notes

- Changing preferences and saving runs `updated()`, which writes **only** the active profile’s parameters.
- Smart bulb mode (param **39** = 1) leaves power on with the relay off — intended for smart bulbs; do not enable casually.
- For a narrower 700S-only upstream option, see the Botched1 PR for `GE Enbrighten 700S` drivers.

---

## Zooz ZSE50 Siren & Chime

Import: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/Zooz-ZSE50-Siren-Chime/Zooz-ZSE50-Siren-Chime.groovy`

HPM package name: **Zooz ZSE50 Siren & Chime** (same custom repository URL as above).

Hubitat includes a built-in ZSE50 driver (platform 2.4.3+). This Heatvent driver adds the full Zooz configuration parameter set (1–16), association group 2, LED color helpers, and Chime/`playSound` for Rule Machine.

| Command | Behavior |
|---------|----------|
| `siren` / `on` / `playSound(n)` | Play tone (default = param 4) |
| `strobe` | LED-only mode (param 1 = 4) then start playback |
| `both` | Normal playback with LED |
| `off` / `stop` | Stop tone |
| `setVolume` / `setLedColor` / `setDefaultTone` | Write params 5 / 7 / 4 |
| `refreshTones` | Re-read the tone files from the device (after adding or removing MP3s) |
| `previewTones(seconds, which)` | Play each tone for 1–10 s (default 2), all / custom only / stock only; `preview` shows e.g. `27 of 27: 27 - Door Warning Beep (custom)`. `stop` ends it |

**Tone names.** The driver reads each tone's file name from the device and fills the **Play Sound** picker (`soundEffects`) and `toneList` with readable labels, for example `10 - Buzzer Alarm`. Stock files keep Zooz's names. Custom MP3s are named from their file name and marked custom, for example `garage_door-closing.mp3` becomes `26 - Garage Door Closing (custom)`. After copying files onto the ZSE50, run **refreshTones** (or Save Preferences). Until the device has reported, the stock list 1–25 is shown.

`status` (`playing` / `stopped`) follows playback. Commands also accept numbers sent as text, as Maker API and some apps send them.

---

## Relay Garage Door

An app and driver that combine a relay and a door sensor into **one garage door opener device**. Built for a **Zooz ZEN16 MultiRelay** wired to the opener's button terminals. Install one app instance per door.

**Door sensor**, any one of:

| Sensor type | Example | Notes |
|-------------|---------|-------|
| Tilt sensor on the top panel | Zooz ZSE43 Tilt-Shock XS | Battery shown; vibration can show *closing* for manual closes |
| Wireless contact sensor | Zooz ZSE41, any Z-Wave/Zigbee contact | Mount it so it is **closed when the door is down** |
| Wired contact | Reed switch on a ZEN16 or ZEN17 input | Reports as a contact **or** a switch (on = closed); no battery |

**Fully open sensor (optional):** a second sensor, of any of the types above, where the door stops when fully open. With it, the door shows **open** only when it is fully open, and **closing** as soon as a door moved by a wall button leaves the open position. A ZEN16 has three inputs, so two reed switches (closed and fully open) can be wired straight to it.

The device has `GarageDoorControl`, `ContactSensor`, `Battery` and `Refresh`. When no sensor has a battery, the app uses **Relay Garage Door (No Battery)**, so Home Assistant doesn't show an empty battery sensor.

| Attribute | Values |
|-----------|--------|
| `door` | `open`, `opening`, `closing`, `closed`, `unknown` |
| `contact` | from the closed sensor (inverted if you ask) |
| `battery` | from a sensor, or another battery device you pick |
| `lastOpened` / `lastClosed` | time of the last open / close |
| `lastResult` | `ok`, `failed to open`, `failed to close`, `cancelled` |
| `movedBy` | `hubitat` or `manual` (wall button, remote, keypad) |

### How it behaves

- **Open / Close** press the relay only when the door isn't already there. A `close` while the door is still opening is ignored, because a press would stop it instead. An `open` while it is closing is allowed: a press reverses a closing door on most openers.
- The door shows **opening** or **closing** until it reaches the position (the fully open sensor, or the closed sensor) or the travel time runs out.
- **Doors moved by a wall button or remote** show **opening** as soon as the closed sensor opens, and **closed** when it closes. Without a fully open sensor, a manual close shows only when it ends. For a ZSE43, turn on *Show closing when the open door's sensor detects vibration* to show it sooner (leave it off if bumps set it off).
- If the door doesn't reach the position in time, it shows where it is, sets `lastResult`, and notifies the devices you pick.

### Warning before the door moves

Pick a **buzzer or strobe relay** (switched on while warning), a **chime or siren** (any `Chime` device, e.g. the Zooz ZSE50, which plays a sound number), or both. Choose *Closing* or *Opening and closing*, and how many seconds to warn before the opener is pressed (5 is typical). By default the warning lasts until the door stops. An Open or Close during the warning cancels the move before the door starts.

**Buzzer on the ZEN16:** the relays are dry contacts with no power output. Take power from the ZEN16's own 12 V DC supply, switched by a spare relay:

```
12 V supply (+) ──┬── ZEN16 power (+)
                  └── Relay 2 COM          Relay 2 NO ──── buzzer (+)
12 V supply (−) ──┬── ZEN16 power (−)
                  └─────────────────────────────────── buzzer (−)
```

Use a **12 V DC piezo buzzer** (a pulsing one sounds like a commercial opener) or a **12 V strobe with buzzer**. Both draw well under 100 mA. If your ZEN16 runs on 24 V, use a 24 V buzzer. Leave Relay 2's input set to *Report Input Status Only* if a sensor is wired to Sw2, and pick *Relay 2* as the buzzer relay.

### Setup

1. Install **Relay Garage Door (Zooz ZEN16 + Tilt/Contact Sensor)** from HPM. Or import by URL:
   - App: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Apps/Relay-Garage-Door/Relay-Garage-Door.groovy`
   - Driver: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/Relay-Garage-Door/Relay-Garage-Door.groovy`
   - Driver (no battery): `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/Relay-Garage-Door/Relay-Garage-Door-No-Battery.groovy`
2. **Apps → Add User App → Relay Garage Door**. Name the door, pick the ZEN16 relay's child switch (for example *Relay 1*), choose the sensor type and the sensor, then set the travel time.
3. On the ZEN16, set the relay's **Auto Turn-Off** to 1 second, or its input to *Garage Door Momentary*. The app also turns the relay off after the time you choose.
4. For a reed switch on a ZEN16 input, set that input's *Input Control* to *Report Input Status Only*, then pick the input's child device as the sensor.

### Home Assistant (Maker API)

Add the new device to **Maker API**. The [Hubitat integration](https://github.com/jason0x43/hacs-hubitat) creates one device with:

- a **garage door cover** with open, opening, closing and closed states
- a **contact** binary sensor
- a **battery** sensor (not with the No Battery driver)

The Hubitat-only attributes (`lastOpened`, `movedBy`, …) don't appear as entities. The relay and the sensors don't need to be in Maker API for the door to work; add them only if Home Assistant uses them on their own. The integration names the cover *<device> Door*; set it to **Use device name** in Home Assistant for a plain *Garage Door*.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE). The GE/Jasco drivers build on Jason Bottjen's (Botched1) Apache-2.0 drivers.
