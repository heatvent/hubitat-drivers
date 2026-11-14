# Heatvent Hubitat Drivers

Hubitat drivers maintained by [Heatvent](https://github.com/heatvent).

## Install with Hubitat Package Manager (recommended)

1. In **Hubitat Package Manager** → **Package Manager Settings** → **Add a Custom Repository**
2. Enter: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/repository.json`
3. **Install** → search for **GE/Jasco Enbrighten Switch & Dimmer (Multi)**

Once this repository is merged into the [HubitatCommunity packagerepositories](https://github.com/HubitatCommunity/hubitat-packagerepositories) list, it will also appear under Install from a repository without adding a custom repo.

### Manual Import URLs

- Switch: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/GE-Jasco-Enbrighten-Switch-Multi/GE-Jasco-Enbrighten-Switch-Multi.groovy`
- Dimmer: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/GE-Jasco-Enbrighten-Dimmer-Multi/GE-Jasco-Enbrighten-Dimmer-Multi.groovy`

---

## GE/Jasco Enbrighten Switch & Dimmer (Multi)

Two profile-driven drivers (Switch and Dimmer) that auto-detect the model from Z-Wave `productType` + `productId` and **only write configuration parameters that model supports**. Preferences for unsupported features still appear in the UI but are ignored on Save.

Detected identity is stored in device **Data Values**: `profileKey`, `profileName`, `profileGeneration`, `profileSource`, `profileColorLed`, `profileThreeWay` (and mirrored in driver state).

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
| **500S** | Enbrighten / Jasco / UltraPro **ZW4009** (toggle) | 46202, 46563, 39354 | `4952-3137` / `3138` / `3238` | 3, 4, 19 |
| **700S** | Enbrighten / Jasco / UltraPro **ZWA4011** (+ DV) | 58433, 59337, 59381, 59347 | `4952-3239` / `3331` / `3335` / `3333` | 3, 4, 5, 19, 34–36, 39 |
| **700S** | Enbrighten / Jasco / UltraPro **ZWA4012** (+ DV) | 58436, 59338, 59382, 59368 | `4952-3330` / `3332` / `3336` / `3334` | 3, 4, 5, 19, 34–36, 39 |
| **800S** | Enbrighten / Jasco **ZWN4015** | 76591, 76596 | `4952-3435` / `3437` | 3, 4, 5, 19, 34–36, 39 |
| **800S** | Enbrighten / Jasco **ZWN4016** | 76592, 76598 | `4952-3438` / `3530` | 3, 4, 5, 19, 34–36, 39 |
| **800S** | UltraPro **ZWN4017** | 76601 | `4952-3534` | 3, 4, 5, 19, 34–36, 39 |

### Supported dimmers (`GE/Jasco Enbrighten Dimmer (Multi)`)

| Generation | Brand / model | SKU examples | Profile key | Params written |
|------------|---------------|--------------|-------------|----------------|
| **500S** | Enbrighten / Jasco / UltraPro **ZW3010** | 46203, 46564, 39351 | `4944-3235` / `3236` / `3339` | 3, 4, 6, 16, 19, 30–32 |
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
| **30** | Minimum Dim Threshold | Dimmers | `1`–`99` % lowest level the dimmer will use |
| **31** | Maximum Brightness Threshold | Dimmers | `1`–`99` % highest level the dimmer will use |
| **32** | Default ON % | Dimmers | `0` restore last level (default); `1`–`99` always turn on to that % |
| **34** | LED Light Color | Color LED models | `1` Red … `5` Blue (default) … `8` White |
| **35** | LED Intensity | Color LED models | `1` (lowest) – `7` (highest); default `4` |
| **36** | Guidelight Intensity | Color LED models | Brightness of the indicator when param 3 is guidelight mode; `1`–`7`, default `4` |
| **39** | Relay Control | **700S/800S switches** | `0` paddle controls relay (default); `1` **smart bulb mode** — relay stays off, line power stays on (use with care) |

Param **84** (Z-Wave factory reset) exists on some 700S/800S manuals but is **not** exposed by these drivers.

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
| Central Scene buttons | Up/Down: push (1/2), hold (1/2), release (1/2), double (3/4), triple (5/6); also `doubleTapped` 1/2 |
| Association group 1 | Lifeline (hub) |
| Association group 2 | Single-press Basic Set to listed nodes (max 5) |
| Association group 3 | Double-press Basic Set; includes hub for button reports (max 4 extra) |

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
