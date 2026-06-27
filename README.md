# PMX Studio

A browser-based PMX viewer and OBJ converter prototype for MikuMikuDance models.

## Features

- Dark glassmorphism control panel for uploading, inspecting, and exporting models.
- Local-only PMX loading with Three.js `MMDLoader`; files are not uploaded to a server.
- Drag-and-drop support for PMX files, full model folders, or ZIP archives.
- Visual 3D floor grid with 1-unit spacing, 5-unit major guides, and axis labels for scale reference.
- Texture diagnostics with an error pop-up when referenced texture files fail to load.
- Model statistics for vertices, triangles, materials, and detected texture maps.
- Viewer tools for auto-framing, wireframe mode, axis/grid toggles, auto-rotate, and screenshots.
- OBJ export package with a best-effort MTL file and conversion audit report.

## Usage

Open `index.html` in a modern browser, or serve the folder with any static web server.

Recommended local server:

```bash
python3 -m http.server 8080
```

Then open:

```text
http://localhost:8080
```

Drop one of the following onto the app:

1. A full PMX model folder containing the `.pmx` file and texture folders.
2. A `.zip` archive preserving the original model folder structure.
3. A single `.pmx` file, if you only need geometry preview/export.

## Important limitations

- OBJ is a static mesh format. PMX bones, morphs, IK, toon settings, and physics are not preserved.
- Texture paths are resolved from the files you provide. If a PMX references missing images or sphere maps, the app shows a texture error dialog.
- The generated MTL is best effort because PMX material settings do not map perfectly to Wavefront MTL.
- Always review the original model license/readme before porting, remixing, publishing, or selling converted files.