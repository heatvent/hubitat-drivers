"""Original starship-style bay alert: a repeating electronic warble, loop-seamless (~6 s).

Each 0.75 s cycle: a bright tone that sweeps up with a fast warble, a short echo tail,
then a gap. Synthesized from scratch (not sampled or transcribed from any recording).
"""
import array
import math
import sys

import lameenc

RATE = 44100
CYCLE = 0.75
CYCLES = 8                      # 6.0 s; whole cycles so the chime's loop is seamless
N = int(RATE * CYCLE * CYCLES)


def pulse(t, length):
    """One alert sweep starting at t=0; returns sample value."""
    if t >= length:
        return 0.0
    k = t / length
    ease = 0.5 - 0.5 * math.cos(math.pi * min(1.0, k * 1.1))  # smooth whoop curve
    freq = 280 + 520 * ease                             # wide low-to-high sweep
    freq *= 1.0 + 0.012 * math.sin(2 * math.pi * 7 * t)  # gentle vibrato only
    return freq


def build():
    out = [0.0] * N
    length = 0.58
    for c in range(CYCLES):
        s0 = int(RATE * c * CYCLE)
        ph = sub = 0.0
        for i in range(int(RATE * length)):
            t = i / RATE
            f = pulse(t, length)
            ph = (ph + f / RATE) % 1.0
            sub = (sub + f / 2 / RATE) % 1.0
            # full body: fundamental, sub-octave and a stack of harmonics (brassy klaxon)
            x = (math.sin(2 * math.pi * ph) + 0.55 * math.sin(2 * math.pi * sub)
                 + 0.5 * math.sin(4 * math.pi * ph) + 0.33 * math.sin(6 * math.pi * ph)
                 + 0.2 * math.sin(8 * math.pi * ph) + 0.12 * math.sin(10 * math.pi * ph))
            env = min(1.0, t / 0.02) * min(1.0, (length - t) / 0.08)
            v = 0.5 * env * x
            out[s0 + i] += v
            # short echo (bay acoustics), kept inside the cycle
            for delay, gain in ((0.08, 0.08),):          # barely-there room reflection
                j = s0 + i + int(RATE * delay)
                if j < s0 + int(RATE * CYCLE):
                    out[j % N] += gain * v
    # faint low room tone, constant so the loop point is invisible
    for i in range(N):
        t = i / RATE
        out[i] += 0.04 * math.sin(2 * math.pi * 60 * t) + 0.02 * math.sin(2 * math.pi * 120 * t)
    peak = max(abs(v) for v in out) or 1.0
    return array.array("h", (int(32767 * 0.89 * v / peak) for v in out))


enc = lameenc.Encoder()
enc.set_bit_rate(128)
enc.set_in_sample_rate(RATE)
enc.set_channels(1)
enc.set_quality(2)
path = f"{sys.argv[1]}/scifi-bay_alert.mp3"
open(path, "wb").write(enc.encode(build().tobytes()) + enc.flush())
print(path)
