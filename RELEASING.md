# Releasing

1. Change the version in `build.gradle.kts` to a non-SNAPSHOT version.
1. `git commit -am "Release X.Y.Z"` (where X.Y.Z is the new version)
1. `git tag -a vX.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
1. `./gradlew assemble`, copy zip in `/build/distribution` to desired location. Alternatively, skip this step and download the artifact from the [GitHub releases page](https://github.com/ryanmoelter/splity/releases) after pushing the new tag.
1. Update the `build.gradle` to the next SNAPSHOT version. (Bump the patch version by 1, usually.)
1. `git commit -am "Prepare for development of version P.Q.R"`
1. `git push && git push --tags`
1. Unzip and run your distribution.
