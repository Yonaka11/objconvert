# PMX Viewer & Converter

A browser-based viewer and OBJ converter for **MikuMikuDance (.pmx)** model files. Everything runs entirely client-side — no files are ever uploaded to a server.

## Features

### Core
- **Drag & drop** your entire model folder (PMX + texture files) or browse to select it
- Renders the PMX model in 3D using [Three.js](https://threejs.org/) and the official `MMDLoader`
- **Export OBJ + MTL** — generates both the geometry file and a companion material file
- Screenshot capture (PNG)

### UI / Visuals
- Dark glassmorphism sidebar panel with `backdrop-filter` blur
- Two-level **3D grid floor** (major 10-unit lines + minor 1-unit lines) with tick marks for scale reference
- Three-point studio lighting (key, fill, rim) with cast shadows
- **Error pop-up modal** that lists every texture file that failed to load, with actionable fix guidance
- Toast notifications for load success/failure and exports
- FPS counter and camera position overlay

### View Controls
- Toggle: grid, wireframe, axes helper, shadows, auto-rotate
- Sliders: ambient intensity, key light intensity, exponential fog density
- Six background presets (dark, navy, studio, light, purple, forest)
- Camera presets: Front / Side / Top + Reset; orbit with mouse/touch

### Model Info Panel
- Vertex count, polygon count, material count, model height
- Per-texture status list (green = loaded, red = missing)

## Usage

1. Open `index.html` in any modern browser (Chrome, Edge, Firefox, Safari).
2. Drop your **complete model folder** onto the drop zone — the folder must contain the `.pmx` file alongside its `textures/` subfolder.
3. Interact with the model using:
   - **Left-click drag** → orbit
   - **Right-click drag** → pan
   - **Scroll wheel** → zoom
4. Adjust lighting and view options in the sidebar.
5. Click **Export OBJ + MTL** to download the converted files.

## Technical Notes

- PMX uses a left-handed coordinate system; `MMDLoader` handles the conversion automatically.
- Textures are matched by filename (case-insensitive) against everything in the dropped folder tree.
- OBJ export is static mesh only — bones, physics, and morph targets are not preserved.
- The MTL file references texture filenames directly; keep textures in the same directory as the OBJ when importing into other tools.

## License

MIT
