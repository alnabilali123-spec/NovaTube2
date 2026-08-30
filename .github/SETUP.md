# Quick Setup — Build APK on GitHub

This is the **fastest** way to get a NovaTube APK.

## 1. Push the repo to GitHub

```bash
cd NovaTube
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/NovaTube.git
git push -u origin main
```

## 2. Wait for the first build

GitHub Actions will automatically:
1. Detect the `.github/workflows/build.yml` file
2. Set up JDK 17 + Android SDK
3. Download Gradle + Maven dependencies + yt-dlp native binaries
4. Build the debug APK
5. Upload it as an artifact

Go to the **Actions** tab in your repository to watch it run. First build takes 8-12 minutes.

## 3. Download the APK

When the workflow finishes:
1. Click the completed run
2. Scroll to the bottom → **Artifacts**
3. Click `NovaTube-debug` to download a ZIP containing `app-debug.apk`

## 4. Install on your phone

Enable **Install from unknown sources** in your phone's settings, then transfer the APK and tap to install.

OR via ADB:
```bash
adb install app-debug.apk
```

## That's it!

For more details (signing release builds, manual triggers, etc.) see [SIGNING.md](SIGNING.md).
