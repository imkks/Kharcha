# Release builds

Every push to `main` runs the CI checks, then creates a signed Android App Bundle
and APK. Download both from the **Artifacts** section of the successful GitHub
Actions run. Upload `app-release.aab` to Play Console; use `app-release.apk` for
direct device installation and testing.

## Configure the upload key

Create a dedicated upload key (or use the existing upload key registered with
Play App Signing):

```sh
keytool -genkeypair -v -keystore kharcha-upload.jks -keyalg RSA -keysize 4096 \
  -validity 10000 -alias kharcha-upload
```

Add these repository **Actions secrets** in GitHub. Base64-encode the JKS file
locally and paste the single-line result into the first secret. On macOS, use
`base64 -i kharcha-upload.jks | tr -d '\n'`.

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 contents of the upload keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias (for the example above, `kharcha-upload`) |
| `ANDROID_KEY_PASSWORD` | Key password |

Keep a secure backup of the keystore and passwords. Do not commit them to the
repository. The workflow keeps the decoded keystore in the runner's temporary
directory and removes it when the job finishes.

The workflow uses the GitHub Actions run number as `versionCode` and sets
`versionName` to `1.0.<run number>`, so each main-branch release gets a higher
version code. It does not publish to Play Console automatically.
