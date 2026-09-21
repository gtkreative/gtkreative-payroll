import os
import struct
import zlib

src = "GTKreativePayroll_MVP_v0.1.zip"
out = "build-source"
data = open(src, "rb").read()
sig = bytes([0x50, 0x4b, 0x03, 0x04])
pos = 0
count = 0

while True:
    pos = data.find(sig, pos)
    if pos < 0:
        break
    if pos + 30 > len(data):
        break
    _, ver, flag, method, mt, md, crc, csize, usize, nlen, elen = struct.unpack_from("<IHHHHHIIIHH", data, pos)
    name = data[pos+30:pos+30+nlen].decode("utf-8")
    extra_end = pos + 30 + nlen + elen
    payload_end = extra_end + csize
    if payload_end > len(data):
        raise SystemExit(f"Truncated entry: {name}")
    target = os.path.join(out, name)
    if name.endswith("/"):
        os.makedirs(target, exist_ok=True)
    else:
        os.makedirs(os.path.dirname(target), exist_ok=True)
        payload = data[extra_end:payload_end]
        if method == 0:
            raw = payload
        elif method == 8:
            try:
                raw = zlib.decompress(payload, -15)
            except zlib.error:
                print(f"Skipping corrupt compressed entry: {name}")
                pos = payload_end
                continue
        else:
            raise SystemExit(f"Unsupported ZIP method {method}: {name}")
        open(target, "wb").write(raw)
    count += 1
    pos = payload_end

if count < 10:
    raise SystemExit(f"Archive extraction found only {count} entries")
print(f"Extracted {count} entries")
