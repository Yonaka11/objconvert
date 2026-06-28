# PMX Studio

A browser-based PMX viewer and OBJ converter for **MikuMikuDance (.pmx)** model files.
Everything runs client-side in the browser; files are not uploaded to a server.

## Features

### Core

- Drag and drop a PMX model folder containing the `.pmx` file and texture folders.
- Render PMX models locally with Three.js and the official `MMDLoader`.
- Export a static OBJ mesh with companion material data.
- Capture viewport screenshots.

### UI and visuals

- Dark glassmorphism control panel for uploading, inspecting, and exporting models.
- Visual 3D floor grid with scale guides so users can understand model size.
- Studio lighting, orbit controls, and optional axes/grid/wireframe/auto-rotate tools.
- Texture diagnostics with an error pop-up when referenced texture files fail to load.
- Model statistics for vertices, polygons/triangles, materials, textures, and scale.

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
2. A single `.pmx` file, if you only need geometry preview/export.

Interact with the model using:

- Left-click drag: orbit
- Right-click drag: pan
- Scroll wheel: zoom

## Important limitations

- OBJ is a static mesh format. PMX bones, morphs, IK, toon settings, and physics are not preserved.
- Texture paths are resolved from the files you provide. If a PMX references missing images or sphere maps, the app shows a texture error dialog.
- The generated MTL is best effort because PMX material settings do not map perfectly to Wavefront MTL.
- Always review the original model license/readme before porting, remixing, publishing, or selling converted files.

## License

MIT
