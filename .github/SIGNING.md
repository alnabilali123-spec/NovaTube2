# GitHub Actions Build for NovaTube

This project includes GitHub Actions workflows to build the APK automatically on every push, pull request, or manually.

## Workflows

### `.github/workflows/build.yml` (Main)
- **Triggers**: Push to `main`/`master`/`develop`, PRs, tags `v*`, and manual dispatch
- **Jobs**:
  - `build`: Builds the requested variant (debug/release) and uploads the APK as an artifact
  - `build-all`: On `main`/`master` or `v*` tags, builds both debug and release APKs together

### `.github/workflows/quick-build.yml`
- **Trigger**: Manual only (`workflow_dispatch`)
- **Job**: Single-step debug build for fast iteration

## How to build the APK

### Option 1: Automatic (push)
Just push code to the `main` branch — the APK is built and uploaded automatically.

### Option 2: Manual from GitHub UI
1. Open the **Actions** tab in your repository
2. Select **Build NovaTube APK**
3. Click **Run workflow**
4. Choose `debug` or `release`
5. Click **Run workflow**
6. Wait ~5-10 minutes for the build
7. Scroll down to **Artifacts** and download `NovaTube-debug` (or `NovaTube-release`)

### Option 3: Tag a release
```bash
git tag v1.0.0
git push origin v1.0.0
```
The workflow will build both debug and release APKs and upload them as `NovaTube-all`.

## Downloading the APK from artifacts

After a successful run:
1. Click on the completed workflow run
2. Scroll to the **Artifacts** section at the bottom
3. Download `NovaTube-debug.zip` (or `NovaTube-release.zip`)
4. Unzip to get `app-debug.apk`
5. Install on your device:
   ```bash
   adb install app-debug.apk
   ```

## Release signing (optional)

By default, release builds are **unsigned** (cannot be installed on most devices as updates to a signed install).

To enable signed release builds, add these secrets in **Settings → Secrets and variables → Actions**:

| Secret | Description |
| --- | --- |
| `KEYSTORE_BASE64` | The `release.keystore` file encoded as base64 |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (e.g. `upload`) |
| `KEY_PASSWORD` | Key password |

### Generate a keystore
```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### Encode for GitHub Secrets
```bash
base64 -w 0 release.keystore > keystore.b64
# Paste contents of keystore.b64 into KEYSTORE_BASE64
```

### Add signing config in `app/build.gradle.kts`
Once you have the secrets, add the `signingConfigs` block to `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            if (System.getenv("KEYSTORE_BASE64") != null) {
                storeFile = file("novatube.keystore")
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = if (System.getenv("KEYSTORE_BASE64") != null)
                signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
}
```

The `build.yml` workflow decodes the keystore automatically and the build picks it up.

## Build time

Typical first build: **8-12 minutes** (downloads Gradle distribution, Android SDK, all Maven dependencies, yt-dlp + FFmpeg native binaries).

Subsequent builds with cache: **3-5 minutes**.

## SDK versions

The workflow uses `android-actions/setup-android@v3` which installs:
- `platforms;android-34`
- `build-tools;34.0.0`
- `platform-tools`

You can pin different versions in `.github/workflows/build.yml` if needed.

## Troubleshooting

- **"SDK location not found"** — the `setup-android` action handles this; if it fails, ensure the action version is `v3+`
- **"Could not resolve com.github.yausername.youtubedl-android"** — JitPack is configured in `settings.gradle.kts`; check that the dependency is reachable from GitHub Actions
- **"Out of memory"** — `GRADLE_OPTS=-Xmx4g` is set; for very large builds, increase to `-Xmx6g`
- **Build timeout** — the default is 60 minutes; if your build needs more, raise `timeout-minutes`
