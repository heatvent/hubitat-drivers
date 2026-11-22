"""Low battery reminder: one short high chirp, like a smoke alarm's low-battery chirp.

A 0.12 s chirp at ~3.2 kHz followed by silence (2.0 s total), so a chime that loops until
stopped chirps about once every two seconds until the app stops it.
"""
import array
import math
import sys

import lameenc

RATE = 44100
FREQ = 3200.0
CHIRP = 0.12
TOTAL = 2.0

n = int(RATE * TOTAL)
samples = array.array("h", [0] * n)
m = int(RATE * CHIRP)
for i in range(m):
    t = i / RATE
    env = min(1.0, t / 0.004, (CHIRP - t) / 0.02)
    x = math.sin(2 * math.pi * FREQ * t) + 0.25 * math.sin(2 * math.pi * 3 * FREQ * t)
    samples[i] = int(32767 * 0.8 * env * x / 1.25)

enc = lameenc.Encoder()
enc.set_bit_rate(128)
enc.set_in_sample_rate(RATE)
enc.set_channels(1)
enc.set_quality(2)
path = f"{sys.argv[1]}/low-battery_chirp.mp3"
open(path, "wb").write(enc.encode(samples.tobytes()) + enc.flush())
print(path)
