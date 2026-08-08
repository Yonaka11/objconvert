# PMX Studio Lite

A clean, browser-based PMX/MMD, OBJ, and FBX model viewer with OBJ export.

**[Live Demo →](https://yonaka11.github.io/objconvert/)**

---

## What It Does

PMX Studio Lite lets you preview MikuMikuDance (PMX), Wavefront OBJ, and Autodesk FBX models directly in your browser and export the loaded scene as static OBJ/MTL files — no installs, no uploads, no server.

Drop in a ZIP archive, loose folder, or supported model file from your phone or desktop, and the model loads with its textures in a 3D viewport you can orbit, zoom, and screenshot.

---

## Features

- **ZIP upload** — drop a `.zip` archive containing a supported model and related textures; the archive is unpacked entirely in-browser using JSZip
- **PMX, OBJ, and FBX preview** — 3D viewer powered by Three.js loaders, with orbit controls, auto-rotate, and camera presets
- **Texture path handling** — case-insensitive, multi-key filename matching so textures nested in sub-folders resolve correctly across Windows and macOS zip conventions
- **OBJ + MTL export** — exports the loaded static mesh geometry plus a companion material file with `map_Kd` and `map_bump` entries
- **Mobile-friendly upload flow** — separate Browse Folder and Browse ZIP buttons; drag-and-drop also works on desktop
- **Debug log** — expandable in-page log (also mirrored to `console.debug`) showing virtual model URLs, blob redirects, and any texture resolution events
- **All processing happens in the browser** — no file is ever sent to a server

---

## Supported Input

| Format | Status |
|---|---|
| `.zip` archive containing `.pmx`, `.obj`, or `.fbx` + textures | ✅ Supported |
| Loose folder drop (desktop only) | ✅ Supported |
| Single `.pmx`, `.obj`, or `.fbx` file (no external textures) | ✅ Renders untextured or with embedded/default materials |
| `.obj` with companion `.mtl` and textures | ✅ Supported |
| `.fbx` with embedded or sibling textures | ✅ Supported |
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
| [OBJLoader](https://threejs.org/examples/?q=obj) + [MTLLoader](https://threejs.org/examples/?q=mtl) | Parsing Wavefront OBJ geometry and material libraries |
| [FBXLoader](https://threejs.org/examples/?q=fbx) | Parsing Autodesk FBX files |
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

- Rigging, animation, physics, and morph targets are not preserved in the OBJ export — the output is a static mesh only
- The coordinate-system conversion (MMD left-handed → OBJ right-handed) is handled by MMDLoader and may not be perfect for every model
- FBX animation clips can be parsed by the loader, but this viewer currently previews static geometry/materials only
- Very large model files (100 MB+) may be slow or fail in memory-constrained browsers
- Texture formats that browsers do not natively support (`.tga`, some `.bmp` variants) may render as blank

---

## Roadmap

- [ ] Improve OBJ/MTL export reliability (material names, UV mapping edge cases)
- [ ] Add a visible warning when a `.rar` file is detected instead of silently failing
- [ ] Test against a wider range of public-domain PMX, OBJ, and FBX model packs
- [ ] Investigate experimental RAR support (e.g. libarchivejs) if there is enough demand
- [ ] Drag-and-drop support on iOS Safari

---

## Development Note

This project was developed with AI-assisted coding tools and manually tested, debugged, and deployed by me. The core architecture decisions, bug investigations, and deployment were hands-on work; the AI helped write and iterate on the implementation. I'm not claiming to have written every line from scratch.

---

## License

MIT
