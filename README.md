# PMX Studio Lite

A clean, browser-based PMX/MMD model viewer and OBJ converter.

**[Live Demo →](https://yonaka11.github.io/objconvert/)**

---

## What It Does

PMX Studio Lite lets you preview MikuMikuDance (PMX) models directly in your browser and export them as static OBJ/MTL files — no installs, no uploads, no server.

Drop in a ZIP archive or a loose folder from your phone or desktop, and the model loads with its textures in a 3D viewport you can orbit, zoom, and screenshot.

---

## Features

- **ZIP upload** — drop a `.zip` archive downloaded from BowlRoll, DeviantArt, or any MMD model site; the archive is unpacked entirely in-browser using JSZip
- **PMX preview** — 3D viewer powered by Three.js and the official MMDLoader, with orbit controls, auto-rotate, and camera presets
- **Texture path handling** — case-insensitive, multi-key filename matching so textures nested in sub-folders resolve correctly across Windows and macOS zip conventions
- **OBJ + MTL export** — exports the static mesh geometry plus a companion material file with `map_Kd` and `map_bump` entries
- **Mobile-friendly upload flow** — separate Browse Folder and Browse ZIP buttons; drag-and-drop also works on desktop
- **Debug log** — expandable in-page log (also mirrored to `console.debug`) showing the virtual PMX URL, blob redirect, and any texture resolution events
- **All processing happens in the browser** — no file is ever sent to a server

---

## Supported Input

| Format | Status |
|---|---|
| `.zip` archive containing `.pmx` + textures | ✅ Supported |
| Loose folder drop (desktop only) | ✅ Supported |
| Single `.pmx` file (no textures) | ✅ Renders untextured |
| `.rar` archive | ❌ Not supported |
| `.7z` archive | ❌ Not supported |
| Encrypted or password-protected archives | ❌ Not supported |
| Multi-part archives (`.zip.001`, etc.) | ❌ Not supported |

If your model comes in a `.rar` file, extract it locally first and re-zip it, or use a tool like [7-Zip](https://www.7-zip.org/) to convert it.

---

## Tech Stack

| Library | Purpose |
|---|---|
| [Three.js](https://threejs.org/) | 3D rendering, scene, camera, lighting |
| [MMDLoader](https://threejs.org/examples/?q=mmd) | Parsing PMX binary format |
| [OBJExporter](https://threejs.org/examples/?q=obj) | Exporting geometry to Wavefront OBJ |
| [JSZip](https://stuk.github.io/jszip/) | In-browser ZIP extraction |
| [GitHub Pages](https://pages.github.com/) | Static hosting |
| Vanilla JavaScript | Application logic, UI, file routing |

No build step. No framework. Single HTML file.

---

## Privacy

Files are processed locally in your browser and are **not uploaded to any server**. Nothing leaves your device.

---

## Limitations

- PMX bones, physics, and morph targets are not preserved in the OBJ export — the output is a static mesh only
- The coordinate-system conversion (MMD left-handed → OBJ right-handed) is handled by MMDLoader and may not be perfect for every model
- Very large model files (100 MB+) may be slow or fail in memory-constrained browsers
- Texture formats that browsers do not natively support (`.tga`, some `.bmp` variants) may render as blank

---

## Roadmap

- [ ] Improve OBJ/MTL export reliability (material names, UV mapping edge cases)
- [ ] Add a visible warning when a `.rar` file is detected instead of silently failing
- [ ] Test against a wider range of public-domain MMD model packs
- [ ] Investigate experimental RAR support (e.g. libarchivejs) if there is enough demand
- [ ] Drag-and-drop support on iOS Safari

---

## Development Note

This project was developed with AI-assisted coding tools and manually tested, debugged, and deployed by me. The core architecture decisions, bug investigations, and deployment were hands-on work; the AI helped write and iterate on the implementation. I'm not claiming to have written every line from scratch.

---

## License

MIT
