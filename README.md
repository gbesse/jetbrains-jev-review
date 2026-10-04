# Jev Exact Selection Review for JetBrains

Select code, choose **Review Selection with Jev**, and receive highlights anchored only to exact lines already in the selection. Jev answers declared finite questions and selects line IDs; it never writes a review paragraph or edits code.

The TypeSafe API key is requested on first use and stored in JetBrains PasswordSafe. If the document changes while inference is running, every result is discarded as stale.

## Build

This project follows the current IntelliJ Platform 2.x toolchain and targets IntelliJ IDEA 2026.2.3:

```bash
gradle test buildPlugin
```

Java 21 and Gradle 9 are required. The packaged plugin is produced under `build/distributions`. This machine only has a legacy Java runtime, so the definitive build is performed by the included GitHub Actions workflow rather than claimed locally.

## Boundaries

The default checks cover ambiguous intent and behavioral risk. They are probabilistic prompts, not a compiler, linter or security scanner. Source selections are sent to TypeSafe only after the user invokes the action. Keep selections minimal and follow your organization's code-processing policy.

Independent community integration. MIT licensed.
