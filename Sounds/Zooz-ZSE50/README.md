# Custom sounds for the Zooz ZSE50

Door warning sounds for the [Zooz ZSE50 Siren & Chime driver](../../README.md#zooz-zse50-siren--chime), made for the [Relay Garage Door](../../README.md#relay-garage-door) closing warning. They are original sounds, synthesized from scratch by the scripts in [`generate/`](generate/), and are covered by this repository's license.

| File | Sound | Length | Shows in Hubitat as |
|------|-------|--------|---------------------|
| [`door-warning_beep.mp3`](door-warning_beep.mp3) | Back-up-alarm style beep, about 1 kHz, half a second on and off. Like a city garage exit or a reversing truck. **Recommended.** | 6 s | `Door Warning Beep (custom)` |
| [`garage-door_closing.mp3`](garage-door_closing.mp3) | Pulsing two-tone beeps (2.4 / 1.8 kHz), like a commercial opener | 6 s | `Garage Door Closing (custom)` |
| [`scifi-bay-door_opening.mp3`](scifi-bay-door_opening.mp3) | Sci-fi hangar door: air hiss, rising motor hum, two-tone alert, end clunk | 7 s | `Scifi Bay Door Opening (custom)` |
| [`scifi-bay-door_closing.mp3`](scifi-bay-door_closing.mp3) | The same with a falling hum, for closing | 7 s | `Scifi Bay Door Closing (custom)` |
| [`scifi-bay_alert.mp3`](scifi-bay_alert.mp3) | Starship-style klaxon whoop | 6 s | `Scifi Bay Alert (custom)` |

Every sound is made of whole cycles, so it loops without a jump when the ZSE50's Playback Mode is *Loop until cancelled* (parameter 1 = 3). The Relay Garage Door app stops it when the door finishes moving.

## Putting them on the ZSE50

1. Connect the ZSE50 to a computer with a USB-C data cable. It shows up as a removable drive.
2. Copy the MP3s you want onto it, next to the stock files. Keep the file names: the driver builds the readable name from them.
3. Eject the drive, unplug the cable, and put the chime back on its power adapter.
4. In Hubitat, run **Refresh Tones** on the chime. The new sounds appear after the stock ones (26, 27, …).
5. Optionally, run **Preview Tones** with *Custom only* to hear each one for 2 seconds.

Set the sound number as *Sound number to play* in the Relay Garage Door app, or use it with `playSound`.

## Regenerating or tweaking them

The scripts need Python 3 and [`lameenc`](https://pypi.org/project/lameenc/) to encode MP3:

```
python -m pip install lameenc
python generate/door_warning_beep.py .
python generate/garage_door_closing.py .
python generate/scifi_bay_door.py .
python generate/scifi_bay_alert.py .
```

Each script writes its file(s) into the folder you give it. Pitch, beep length and loudness are set by the constants at the top of each script.
