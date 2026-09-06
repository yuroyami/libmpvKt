# Releasing

Every step is a command the owner runs; nothing publishes on its own.

1. **Pins.** If an upstream version changes, edit `buildscripts/include/depinfo.sh`, run `buildscripts/download-deps.sh --refresh`, then `scripts/check-gate.sh tier2`, which builds arm64 and runs the device tests.
2. **Version and notes.** Set `VERSION` in `gradle.properties`. Add a `## [x.y.z] - YYYY-MM-DD` section to `CHANGELOG.md` naming what changed and the versions inside. Commit: `Release x.y.z`. Push.
3. **Natives.** Run the "Release natives" workflow with tag `vx.y.z`. It creates the tag at the current commit if it does not exist, builds the four ABIs, and attaches the zips, checksums, `SOURCES.txt` and the mpv and FFmpeg source tarballs to the release. Check the release page: eight library assets, three source assets.
4. **Publish.** Run the "Publish release" workflow with `version` = `x.y.z`. It refuses a version that does not match `gradle.properties`, has no changelog section, or already exists in the repository. It downloads the release's zips, verifies them, assembles the AAR, writes it into a checkout of `yuroyami/maven` and pushes. GitHub Pages serves the push within a few minutes.
5. **Check.** `https://yuroyami.github.io/maven/io/github/yuroyami/libmpvkt-android/x.y.z/libmpvkt-android-x.y.z.aar` answers 200. The "pages build and deployment" run under `yuroyami/maven` shows when it went live.
6. **Docs.** Run the "Docs" workflow to deploy the site for the new version.
7. **Consumers.** Only now bump the version in consuming projects. A consumer pinned to a version the repository does not serve yet has red CI until it does.

A published version directory is never rewritten. A wrong version is followed by a corrected one.

## Pruning

The site may not exceed 1 GB and one version is about 45 MB, so the repository holds about twenty versions. When it nears the cap, delete the oldest version directories (the artifact and its `-android` variant for that version) in one commit named `Prune x.y.z`, and say so in the changelog of the next release. The native zips of every version stay on its GitHub release page for ever, and `.github/scripts/fetch-natives.sh vx.y.z` followed by `./gradlew publishToMavenLocal` rebuilds any pruned version on any machine.
