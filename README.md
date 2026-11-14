# Heatvent Hubitat Drivers

Hubitat drivers maintained by [Heatvent](https://github.com/heatvent).

## Install with Hubitat Package Manager (recommended)

1. In **Hubitat Package Manager** → **Package Manager Settings** → **Add a Custom Repository**
2. Enter: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/repository.json`
3. **Install** → search for **GE/Jasco Enbrighten Switch & Dimmer (Multi)**

Once this repository is merged into the [HubitatCommunity packagerepositories](https://github.com/HubitatCommunity/hubitat-packagerepositories) list, it will also appear under Install from a repository without adding a custom repo.

## GE/Jasco Enbrighten Switch & Dimmer (Multi)

Profile-driven drivers that auto-detect model (productType + productId) and only write the configuration parameters that model supports. Covers classic 500S Enbrighten/ZW4008-class devices plus 700S/800S color-LED models (ZWA4011, ZWA3016, ZWN*, etc.).

Detected profile is stored in device **Data Values** (`profileKey`, `profileName`, `profileGeneration`, and related fields).

**Credit:** Based on Jason Bottjen ([Botched1](https://github.com/Botched1/Hubitat)) GE Enbrighten Switch/Dimmer drivers. Developed with Cursor.

### Manual Import URLs

- Switch: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/GE-Jasco-Enbrighten-Switch-Multi/GE-Jasco-Enbrighten-Switch-Multi.groovy`
- Dimmer: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/GE-Jasco-Enbrighten-Dimmer-Multi/GE-Jasco-Enbrighten-Dimmer-Multi.groovy`

### Notes

- Unsupported preferences for a given model are ignored (never written).
- Optional **Device Profile Override** if auto-detect is wrong.
- For a narrower 700S-only option contributed upstream, see the Botched1 PR for `GE Enbrighten 700S` drivers.
