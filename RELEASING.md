Releasing
========

1. Change the version in `build.gradle` to a non-SNAPSHOT version.
1. `git commit -am "Prepare for release X.Y"` (where X.Y is the new version)
1. `git tag -a X.Y -m "Version X.Y"` (where X.Y is the new version)
1. `./gradlew assemble`, copy zip in `/build/distribution` to desired location
1. Update the `build.gradle` to the next SNAPSHOT version.
1. `git commit -am "Prepare for version 0.3"`
1. `git push && git push --tags`
1. Unzip and run your distribution.

