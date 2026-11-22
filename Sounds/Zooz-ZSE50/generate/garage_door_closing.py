"""Garage door closing warning: pulsing two-tone beeps, about 6 seconds."""
import array
import math
import sys

import lameenc

RATE = 44100
out = f"{sys.argv[1]}/garage-door_closing.mp3"
samples = array.array("h")


def tone(freq, secs, vol=0.6):
    n = int(RATE * secs)
    fade = int(RATE * 0.01)
    for i in range(n):
        env = min(1.0, i / fade, (n - i) / fade)
        # square-ish wave (fundamental + 3rd harmonic) cuts through garage noise
        x = math.sin(2 * math.pi * freq * i / RATE) + 0.3 * math.sin(2 * math.pi * 3 * freq * i / RATE)
        samples.append(int(32767 * vol * env * x / 1.3))


def silence(secs):
    samples.extend([0] * int(RATE * secs))


for _ in range(12):  # 12 x 0.5 s = 6 s
    tone(2400, 0.15)
    tone(1800, 0.15)
    silence(0.2)

enc = lameenc.Encoder()
enc.set_bit_rate(128)
enc.set_in_sample_rate(RATE)
enc.set_channels(1)
enc.set_quality(2)
data = enc.encode(samples.tobytes()) + enc.flush()
open(out, "wb").write(data)
print(out, len(data), "bytes", round(len(samples) / RATE, 1), "s")
