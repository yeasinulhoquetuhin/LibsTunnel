#!/usr/bin/env python3
"""Repack an APK and optionally strip native debug symbols before re-signing.

Output is unsigned. Run zipalign, then apksigner, before distributing it.
"""

import argparse
from pathlib import Path
import struct
import subprocess
import tempfile
import zipfile


def allocated_sections(data):
    if data[:6] != b"\x7fELF\x02\x01":
        raise ValueError("Expected a little-endian ELF64 library")
    header = struct.unpack_from("<16sHHIQQQIHHHHHH", data)
    offset, entry_size, count, names_index = header[6], header[11], header[12], header[13]
    entries = [
        struct.unpack_from("<IIQQQQIIQQ", data, offset + index * entry_size)
        for index in range(count)
    ]
    names_entry = entries[names_index]
    names = data[names_entry[4]:names_entry[4] + names_entry[5]]
    sections = {}
    for entry in entries:
        if entry[2] & 2 and entry[1] != 8:  # SHF_ALLOC, excluding SHT_NOBITS
            name_end = names.index(b"\0", entry[0])
            name = names[entry[0]:name_end].decode()
            sections[name] = (entry[3], data[entry[4]:entry[4] + entry[5]])
    return sections


def is_signature(name):
    upper = name.upper()
    return upper.startswith("META-INF/") and (
        upper == "META-INF/MANIFEST.MF" or
        upper.endswith((".SF", ".RSA", ".DSA", ".EC"))
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--strip-tool", help="Compatible llvm-strip or NDK llvm-strip executable")
    args = parser.parse_args()
    if args.input.resolve() == args.output.resolve():
        parser.error("Input and output must be different files")
    if not args.output.parent.is_dir():
        parser.error("Output directory must exist")

    with tempfile.TemporaryDirectory(prefix="apk-compact-", dir=args.output.parent) as temp:
        temp = Path(temp)
        packed = temp / "unsigned.apk"
        with zipfile.ZipFile(args.input) as source, zipfile.ZipFile(packed, "w", compresslevel=9) as target:
            for entry in source.infolist():
                if is_signature(entry.filename):
                    continue
                data = source.read(entry)
                if args.strip_tool and entry.filename.startswith("lib/") and entry.filename.endswith(".so"):
                    library = temp / "native.so"
                    stripped = temp / "stripped.so"
                    library.write_bytes(data)
                    subprocess.run([
                        args.strip_tool, "--strip-unneeded", "-o", str(stripped), str(library)
                    ], check=True)
                    stripped_data = stripped.read_bytes()
                    if allocated_sections(data) != allocated_sections(stripped_data):
                        raise ValueError(f"Runtime ELF sections changed: {entry.filename}")
                    print(f"{entry.filename}: {len(data):,} -> {len(stripped_data):,} bytes; runtime sections preserved")
                    data = stripped_data
                clean = zipfile.ZipInfo(entry.filename, entry.date_time)
                clean.compress_type = entry.compress_type
                clean.external_attr = entry.external_attr
                clean.create_system = entry.create_system
                target.writestr(clean, data, compresslevel=9)
        with zipfile.ZipFile(packed) as archive:
            bad = archive.testzip()
            if bad:
                raise ValueError(f"ZIP integrity check failed: {bad}")
        packed.replace(args.output)
    print(f"Unsigned APK: {args.output} ({args.output.stat().st_size:,} bytes)")


if __name__ == "__main__":
    main()
