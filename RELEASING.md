# Releasing

I've set up GitHub actions to automatically release this library when we add a "vX.Y.Z" tag. To release automatically:

1. Change the version in `build.gradle.kts` to a non-SNAPSHOT version.
2. `git commit -am "Release X.Y.Z"` (where X.Y.Z is the new version)
3. `git tag -a vX.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
4. Update the `build.gradle` to the next SNAPSHOT version. (Bump the patch version by 1, usually.)
5. `git commit -am "Prepare for development of P.Q.R"`
6. `git push && git push --tags`
7. Download the artifact from the [GitHub releases page](https://github.com/ryanmoelter/splity/releases) after pushing the new tag.
8. Unzip and run your distribution.

Or, if not using github's releases do this between steps 3 and 4:

1. `./gradlew assemble`
2. Copy zip in `/build/distribution` to desired location.  
