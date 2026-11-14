# Heatvent Hubitat Drivers

Hubitat drivers maintained by [Heatvent](https://github.com/heatvent).

## GE/Jasco Enbrighten Switch & Dimmer (Multi)

Profile-driven drivers that auto-detect model (productType + productId) and only write the configuration parameters that model supports. Covers classic 500S Enbrighten/ZW4008-class devices plus 700S/800S color-LED models (ZWA4011, ZWA3016, ZWN*, etc.).

**Credit:** Based on Jason Bottjen ([Botched1](https://github.com/Botched1/Hubitat)) GE Enbrighten Switch/Dimmer drivers. Developed with Cursor.

### Import URLs

- Switch: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/GE-Jasco-Enbrighten-Switch-Multi/GE-Jasco-Enbrighten-Switch-Multi.groovy`
- Dimmer: `https://raw.githubusercontent.com/heatvent/hubitat-drivers/main/Drivers/GE-Jasco-Enbrighten-Dimmer-Multi/GE-Jasco-Enbrighten-Dimmer-Multi.groovy`

### Notes

- Unsupported preferences for a given model are ignored (never written).
- Optional **Device Profile Override** if auto-detect is wrong.
- For a narrower 700S-only option contributed upstream, see the Botched1 PR for `GE Enbrighten 700S` drivers.
