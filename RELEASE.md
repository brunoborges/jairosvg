# Releasing JairoSVG

Releases are started manually by a maintainer using the
[Release workflow](.github/workflows/release.yml). It updates the version, creates
a Git tag, builds and publishes the artifacts, and prepares the next development
version. The publishing configuration is in
[.jreleaser/jreleaser.yml](.jreleaser/jreleaser.yml).

## Before starting

1. Make sure the intended changes are merged into `main` and CI is passing.
   The workflow builds with Java 25; locally, use `./mvnw clean verify`.
2. Choose an unused numeric `MAJOR.MINOR.PATCH` version, without leading zeroes
   in its components (except for zero itself), a `v` prefix, or a suffix such
   as `-SNAPSHOT`. Check that the corresponding `vMAJOR.MINOR.PATCH` tag and
   GitHub release do not already exist and that the version is not already on
   [Maven Central](https://central.sonatype.com/artifact/io.brunoborges/jairosvg).
   The workflow does not validate the version or protect an existing tag.
3. Ensure the maintainer can run GitHub Actions, and that the workflow's
   `GITHUB_TOKEN` can write repository contents, issues, and Actions. Branch and
   tag protection must permit the bot's pushes, including a force push to `main`.
4. Configure these repository secrets:
   - `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD`: Maven Central
     publishing credentials.
   - `GPG_PUBLIC_KEY`, `GPG_SECRET_KEY`, and `GPG_PASSPHRASE`: the signing key
     pair and its passphrase.
5. Run only one release at a time, and avoid updates to `main` until it finishes.

> **Warning:** The workflow force-creates the release tag and runs
> `git push origin main --tags --force` **before** building or publishing. This
> can overwrite concurrent changes to `main` and existing tags, including tags
> other than the release tag. A failed build can therefore leave a release
> commit and tag behind.

## Start the release

In GitHub, open **Actions → Release → Run workflow**, select `main`, and
optionally enter the release version. If the input is empty, the workflow uses
the version in `pom.xml` with a trailing `-SNAPSHOT` removed. Check this value
before running: if the POM already contains a released version, the default
would select it again.

Alternatively, from the repository checkout, run:

```bash
gh workflow run release.yml --ref main
```

To override the POM version, append `-f version=1.2.3` using the intended
version. Do not manually create the tag or bump the version before dispatching
the workflow; it does both.

Monitor the **Release** run in Actions. With the GitHub CLI, use
`gh run list --workflow release.yml` to find the run, then
`gh run watch <run-id> --exit-status`.

## What the workflow does

1. **Prepare the release.** The
   [version-bump script](.github/workflows/scripts/bump-version.sh) sets the
   release version in `pom.xml`, versioned README examples, `jbang-catalog.json`,
   the current-version row in `comparison/README.md`, selected site pages, and
   the comparison JBang scripts. Maven's `versions:set` also updates the POM.
   The workflow commits any changes as `Release v<version>`, force-creates the
   `v<version>` tag, and force-pushes `main` and all local tags.
2. **Build and stage.** It runs
   `./mvnw clean deploy -Prelease,cli --no-transfer-progress`. This runs the
   Maven build and tests, attaches source and Javadoc JARs, builds the shaded
   CLI JAR, and deploys to the local `target/staging-deploy` directory. The CLI
   JAR is removed from Maven Central staging but retained for GitHub Releases.
   The staged files and release JARs are uploaded as workflow artifacts.
3. **Publish.** A second job checks out the tag and runs JReleaser's
   `full-release`. JReleaser signs and publishes the staged library artifacts
   to Maven Central and creates a GitHub release with the library and CLI JARs,
   signatures, and checksums. The release notes use the Conventional Commits
   changelog configuration. JReleaser expects the existing tag and is
   configured not to overwrite an existing GitHub release.
4. **Prepare the next development version.** After publishing succeeds, the
   workflow increments the patch component, updates `pom.xml` and the
   comparison scripts to the next `-SNAPSHOT`, and pushes a
   `Bump version to <version>-SNAPSHOT` commit to `main`. For example,
   `1.2.3` becomes `1.2.4-SNAPSHOT`. User-facing examples remain on the
   released version.
5. **Update milestones and documentation.** It searches the first 100 open
   milestones, attempts to close one named for the released version, and
   creates the next patch milestone if it finds no matching open milestone.
   This step is best-effort and does not fail the release if it encounters an
   error. Finally, it dispatches the separate
   [documentation deployment workflow](.github/workflows/site.yml), which
   rebuilds the site from `main`.

The workflow's pushes use `GITHUB_TOKEN`, so they do not normally start other
push-triggered workflows. The site workflow is dispatched explicitly, and its
completion must be checked separately.

## After the run

- Check the GitHub release, its notes, and the library and CLI assets. Confirm
  the released library version is available on Maven Central; availability may
  not be immediate.
- Check that `main` contains the next patch `-SNAPSHOT`, that the milestone
  updates succeeded, and that the separate **Deploy Documentation** run
  completed successfully.

## If something fails

Inspect the failed Actions step and, when available, the `staged-artifacts`,
`release-jars`, and `jreleaser-release` workflow artifacts. The latter contains
JReleaser's trace log and output properties.

Do not blindly rerun a failed release: the workflow may already have pushed
the commit and tag, created the GitHub release, or published to Maven Central.
Check all of these before deciding how to recover. Published Maven Central
versions cannot be replaced, and rerunning can force-move tags or overwrite
newer `main` commits.

If publication succeeded but the snapshot bump failed, use the version-bump
script with the next version **without** the `-SNAPSHOT` suffix. For example,
after releasing `1.2.3`:

```bash
./.github/workflows/scripts/bump-version.sh snapshot 1.2.4
```

Review and commit the changes through the normal repository process. Reconcile
the milestones if necessary. After any version changes have reached `main`,
dispatch a missing site deployment with `gh workflow run site.yml --ref main`.
