# Instructions for AI agents

## Two release lines: read this before you change code

This repository has two maintained branches, and both receive fixes and releases.

- `main`: Spring Boot 3.5.x, release tags `v5.x`.
- `old-main-before-springboot-upgrade`: the Spring Boot version used before the upgrade, release tags `v4.4.x`.
  Production still runs this line.

Rules:

1. Find out which version the task concerns before you create a branch. A defect in a `v4.4.x` release is fixed on
   a branch cut from `old-main-before-springboot-upgrade`, and its pull request targets that branch. A defect in
   a `v5.x` release, and a new feature, go to `main`. If the task does not say, ask.
2. Never merge or rebase one of the two branches into the other. If both lines need the fix, make two pull requests
   and port the second by cherry-picking, then adapt it to the Spring Boot differences.
3. Do not use Spring Boot 3.5 APIs, dependencies, or dependency versions on `old-main-before-springboot-upgrade`, and
   do not move old-line code to `main` unchanged.
4. Releases come from the branch of the fix. Never start the release workflow from the other branch.
5. Edits to this file and to the "Branches and releases" section of `CONTRIBUTING.md` go to both branches.

Details are in [CONTRIBUTING.md](CONTRIBUTING.md#branches-and-releases). The same layout is used in [qubership-testing-platform-diameter-transport-library](https://github.com/Netcracker/qubership-testing-platform-diameter-transport-library).
