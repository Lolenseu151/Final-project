# Desktop Export Guide

This project already uses Gradle's `application` plugin and the LWJGL3 backend, so you can produce a zipped desktop build and a Windows `.exe` without touching the source code. Follow the steps below from the repository root (`C:\New folder\Final-project`).

## 1. Prerequisites
- JDK 17 or newer **with** the `jpackage` tool on your `PATH` (it ships with the standard Oracle, Microsoft, Temurin, and Azul distributions).
- The project assets (already checked in) plus at least one square PNG that you like as the game icon. You can reuse `assets/Start/1.png` or drop your own file under `assets/icons/`.
- (Optional) [ImageMagick](https://imagemagick.org) or any PNG→ICO converter so you can hand `jpackage` a proper `.ico` file.

## 2. Build the zipped JVM distribution (Gradle `distZip`)
```powershell
# From the repo root
./gradlew.bat clean distZip
```
Output: `build/distributions/Final-project.zip`

Contents:
- `bin/Final-project.bat` and `bin/Final-project` launch scripts.
- `lib/Final-project.jar` plus all dependency jars.
- `assets/` directory (because the run task sets the working directory to the project root).

Distribute this zip directly if you are fine with a script-based launcher.</n
## 3. Create an "install" folder for packaging (Gradle `installDist`)
```powershell
./gradlew.bat installDist
```
Output folder: `build/install/Final-project/`

Structure:
```
build/install/Final-project/
  ├─ bin/Final-project(.bat)
  └─ lib/Final-project.jar + dependencies
```
This folder is what `jpackage` will consume in the next step.

## 4. Prepare a Windows icon
1. Pick or drop a square PNG (256×256 recommended). Example: `assets/Start/1.png`.
2. Convert it to `.ico`. With ImageMagick installed:
   ```powershell
   New-Item -ItemType Directory -Force -Path build\icons | Out-Null
   magick.exe assets\Start\1.png -resize 256x256,128x128,64x64,48x48,32x32,16x16 build\icons\papertrail.ico
   ```
   (If you use an online converter, just save the resulting `papertrail.ico` to `build/icons/`.)

## 5. Generate a Windows `.exe` with `jpackage`
```powershell
$AppName = "PapertrailGDX"
$InstallDir = "build/install/Final-project"
$IconPath = "build/icons/papertrail.ico"  # update if you placed it elsewhere

jpackage `
  --type exe `
  --name $AppName `
  --app-version 1.0.0 `
  --input "$InstallDir/lib" `
  --main-jar Final-project.jar `
  --main-class com.mygdx.game.desktop.DesktopLauncher `
  --icon $IconPath `
  --win-dir-chooser `
  --win-menu `
  --resource-dir $InstallDir/bin `
  --dest build/jpackage`
```
What the flags do:
- `--input` points to the jars produced by `installDist`.
- `--resource-dir` copies the `bin` scripts so the packaged app can still locate `assets/` relative to the working directory.
- `--main-class` uses the standard launcher (which now sets the window icon via LWJGL).
- `--dest build/jpackage` writes `PapertrailGDX-1.0.0.exe` (an installer) and an exploded `PapertrailGDX/` app image.

If you prefer a portable folder with `PapertrailGDX.exe` inside, change `--type exe` to `--type app-image` and zip the resulting `build/jpackage/PapertrailGDX` directory.

## 6. Final zip with the `.exe`
```powershell
Compress-Archive -Force `
  -Path build/jpackage/PapertrailGDX/* `
  -DestinationPath build/Final-project-win64.zip
```
Ship `Final-project-win64.zip` or the installer produced by `jpackage`.

## 7. Smoke test checklist
1. Run the generated `.exe` once; the window should display the custom icon (provided via `DesktopLauncher`).
2. Confirm that the game still finds the `assets/` folder. If not, copy `assets/` next to the `.exe` or update the launcher script to run from the project root.
3. Scan the zip on Windows Defender / SmartScreen before distributing it.

## Troubleshooting
- **`jpackage` not found**: ensure you are using a full JDK (not a JRE) and that `%JAVA_HOME%\bin` is first on your PATH.
- **Black square icon**: verify your `.ico` contains multiple resolutions; the ImageMagick command above writes all common sizes.
- **Missing assets on customer machines**: include the `assets/` directory next to the `.exe` or embed it via [Packr](https://github.com/libgdx/packr). The current Gradle run configuration already expects to be launched from the project root with `assets/` alongside the executable.
