"""Plain door-closing warning: back-up-alarm style piezo beep.

~1040 Hz, 0.5 s on / 0.5 s off, 6 beeps (6.0 s, whole cycles so a looping chime stays
in rhythm). Piezo timbre: band-limited square wave with a little resonance, hard edges
softened by 3 ms ramps so it doesn't click.
"""
import array
import math
import sys

import lameenc

RATE = 44100
FREQ = 1040.0
ON, OFF, BEEPS = 0.5, 0.5, 6
N = int(RATE * (ON + OFF) * BEEPS)


def build():
    out = [0.0] * N
    for b in range(BEEPS):
        s0 = int(RATE * b * (ON + OFF))
        for i in range(int(RATE * ON)):
            t = i / RATE
            ph = 2 * math.pi * FREQ * t
            # odd harmonics up to ~11 kHz = band-limited square (piezo-like)
            x = sum(math.sin(h * ph) / h for h in (1, 3, 5, 7, 9))
            x += 0.15 * math.sin(2 * ph)          # slight asymmetry of a real piezo
            ramp = 0.003
            env = min(1.0, t / ramp, (ON - t) / ramp)
            out[s0 + i] = env * x
    peak = max(abs(v) for v in out) or 1.0
    return array.array("h", (int(32767 * 0.89 * v / peak) for v in out))


enc = lameenc.Encoder()
enc.set_bit_rate(128)
enc.set_in_sample_rate(RATE)
enc.set_channels(1)
enc.set_quality(2)
path = f"{sys.argv[1]}/door-warning_beep.mp3"
open(path, "wb").write(enc.encode(build().tobytes()) + enc.flush())
print(path)
