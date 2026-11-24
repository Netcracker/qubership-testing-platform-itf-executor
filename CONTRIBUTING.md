# Contribution Guide

We'd love to accept patches and contributions to this project.
Please, follow these guidelines to make the contribution process easy and effective for everyone involved.

## Contributor License Agreement

You must sign the [Contributor License Agreement](https://github.com/Netcracker/qubership-workflow-hub/blob/main/CLA/cla.md) in order to contribute.

## Code of Conduct

Please make sure to read and follow the [Code of Conduct](CODE-OF-CONDUCT.md).

## Branches and releases

Production still runs releases from before the Spring Boot upgrade, so this repository maintains two release lines
side by side.

| Branch | Spring Boot | Release tags | Use it for |
| --- | --- | --- | --- |
| `main` | 3.5.x | `v5.x` | Development and releases of the upgraded line |
| `old-main-before-springboot-upgrade` | The version used before the upgrade | `v4.4.x` | Fixes for the versions that run in production |

Follow these rules:

1. Choose the base branch by the version the defect is reported against. For a defect in a `v4.4.x` release, create your
   branch from `old-main-before-springboot-upgrade` and open the pull request against that branch. For a defect in
   a `v5.x` release, or for a new feature, use `main`. If you cannot tell which version is affected, ask before you
   start.
2. Never merge `old-main-before-springboot-upgrade` into `main`, and never merge `main` into it. When a fix is needed
   in both lines, open two pull requests: one per branch. Port the second one by cherry-picking the first, then adapt
   it to the Spring Boot differences.
3. Release from the branch that the fix is on. Start the `Maven release` workflow (`maven-release-v2.yaml`) with
   `Use workflow from` set to that branch. A release from `old-main-before-springboot-upgrade` continues the `v4.4.x`
   tags, and a release from `main` continues the `v5.x` tags.
4. Keep dependencies inside one line. On `old-main-before-springboot-upgrade`, depend on the pre-upgrade releases of
   the other ITF services and libraries, and do not bump to a release that needs Spring Boot 3.5.
5. Make a change to this section, to `AGENTS.md`, or to `README.md` on both branches, so that a branch cut from either
   one carries the same instructions.

The same layout is used in [qubership-testing-platform-diameter-transport-library](https://github.com/Netcracker/qubership-testing-platform-diameter-transport-library).
