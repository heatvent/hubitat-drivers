"""Original sci-fi "hangar bay door" warning tones (opening / closing), ~7 s, loop-friendly.

Layers: pneumatic hiss (filtered noise) at the start, a heavy servo hum that glides up
(opening) or down (closing) with a mechanical wobble, a pulsing two-tone alert on top,
and a soft clunk at the end. Written from scratch with sine/saw/noise synthesis.
"""
import array
import math
import random
import sys

import lameenc

RATE = 44100
DUR = 7.0
N = int(RATE * DUR)


def build(direction: str) -> array.array:
    rnd = random.Random(7)
    out = [0.0] * N
    up = direction == "opening"

    # 1) Pneumatic hiss: band-limited noise, sharp attack, 0.9 s decay (pressure release)
    lp = hp = 0.0
    for i in range(int(RATE * 1.2)):
        t = i / RATE
        env = min(1.0, t / 0.02) * math.exp(-t / 0.35)
        x = rnd.uniform(-1, 1)
        lp += 0.35 * (x - lp)          # low-pass
        hp = lp - (hp * 0.0 + lp * 0.15)  # crude high emphasis
        out[i] += 0.45 * env * hp

    # 2) Servo / hydraulic hum: detuned saws + sub sine, gliding pitch, wobble, low-passed
    f0, f1 = (48.0, 72.0) if up else (72.0, 48.0)
    ph1 = ph2 = ph3 = 0.0
    filt = 0.0
    for i in range(N):
        t = i / RATE
        k = min(1.0, max(0.0, (t - 0.3) / (DUR - 1.3)))
        glide = f0 + (f1 - f0) * (k * k * (3 - 2 * k))        # smoothstep glide
        wobble = 1.0 + 0.015 * math.sin(2 * math.pi * 5.5 * t)  # gear chatter
        f = glide * wobble
        ph1 = (ph1 + f / RATE) % 1.0
        ph2 = (ph2 + f * 1.007 / RATE) % 1.0
        ph3 = (ph3 + f * 0.5 / RATE) % 1.0
        saw = (2 * ph1 - 1) + (2 * ph2 - 1)
        sub = math.sin(2 * math.pi * ph3)
        filt += 0.08 * (saw - filt)                            # warm low-pass
        env = min(1.0, max(0.0, (t - 0.15) / 0.6)) * min(1.0, (DUR - t) / 0.4)
        out[i] += env * (0.22 * filt + 0.25 * sub)

    # 3) Alert: two-tone "bee-boo" pulses every 1.0 s, with a slight sweep for a sci-fi feel
    hi, lo = (1320.0, 990.0) if up else (990.0, 1320.0)
    for n in range(int(DUR)):
        start = 0.4 + n * 1.0
        for j, (freq, length) in enumerate([(hi, 0.16), (lo, 0.22)]):
            s0 = int(RATE * (start + j * 0.19))
            m = int(RATE * length)
            ph = 0.0
            for i in range(m):
                idx = s0 + i
                if idx >= N:
                    break
                t = i / RATE
                sweep = freq * (1.0 + (0.04 if up else -0.04) * t / length)
                ph = (ph + sweep / RATE) % 1.0
                tri = 4 * abs(ph - 0.5) - 1                    # soft triangle
                env = min(1.0, t / 0.008) * min(1.0, (length - t) / 0.03)
                out[idx] += 0.28 * env * (tri + 0.3 * math.sin(4 * math.pi * ph))

    # 4) End clunk: door seating into its frame (low thump + short noise)
    s0 = int(RATE * (DUR - 0.45))
    for i in range(int(RATE * 0.35)):
        idx = s0 + i
        if idx >= N:
            break
        t = i / RATE
        env = math.exp(-t / 0.06)
        out[idx] += 0.6 * env * math.sin(2 * math.pi * (70 - 40 * t) * t) + 0.08 * env * rnd.uniform(-1, 1)

    peak = max(abs(v) for v in out) or 1.0
    gain = 0.89 / peak
    return array.array("h", (int(32767 * v * gain) for v in out))


def save(samples: array.array, path: str) -> None:
    enc = lameenc.Encoder()
    enc.set_bit_rate(128)
    enc.set_in_sample_rate(RATE)
    enc.set_channels(1)
    enc.set_quality(2)
    open(path, "wb").write(enc.encode(samples.tobytes()) + enc.flush())


folder = sys.argv[1]
for d in ("opening", "closing"):
    path = f"{folder}/scifi-bay-door_{d}.mp3"
    save(build(d), path)
    print(path)
