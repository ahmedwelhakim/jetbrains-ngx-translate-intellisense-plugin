# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

JetBrains plugin (Kotlin, JVM 21) providing IntelliSense for `ngx-translate` / `@jsverse/transloco` translation keys in Angular/Nx projects. Built from the IntelliJ Platform Plugin Template; targets WebStorm (`platformType=WS`, `pluginSinceBuild=252`) with bundled `JavaScript` and `AngularJS` plugins. Version, platform, and dependency settings live in `gradle.properties`; library versions in `gradle/libs.versions.toml`.

## Commands

```bash
./gradlew buildPlugin                 # build distributable zip (build/distributions)
./gradlew runIde                      # launch sandbox IDE with the plugin
./gradlew check                       # tests + Kover coverage report (what CI runs)
./gradlew test --tests "com.github.ahmedwelhakim.ngxtranslateintellisense.TranslationFileNamingTest"
./gradlew test --tests "*TranslationFileNamingTest.comparesKeysOnlyWithinTheSameSection"
./gradlew verifyPlugin                # IntelliJ Plugin Verifier against recommended IDEs
```

## Release notes / versioning

- Plugin description shown on the Marketplace is extracted from `README.md` between `<!-- Plugin description -->` markers — the build fails if they're missing.
- Change notes come from `CHANGELOG.md` (Gradle Changelog plugin): the entry matching `pluginVersion`, else `[Unreleased]`. Add user-facing changes under `## [Unreleased]`; `publishPlugin` runs `patchChangelog`.
- A pre-release suffix in `pluginVersion` (e.g. `2.1.0-beta.1`) publishes to that Marketplace channel.

## Architecture

All extensions are registered in `src/main/resources/META-INF/plugin.xml` (TypeScript + Angular2/Angular2Html languages for code features; JSON for annotators). Package root: `com.github.ahmedwelhakim.ngxtranslateintellisense`.

**Data flow:**
1. `services/NgxTranslateConfigurationStateService` — project-level persisted settings (workspace file). On init and on any VFS change to a `.json` file it auto-discovers i18n directories by scanning module content roots (skipping `NgxTranslateConstants.EXCLUDED_DIRS_IN_PATH`) for directories containing locale files. `saveSettings` restarts the daemon and refreshes fold regions in open editors.
2. `psi/NgxTranslatePsiUtils` — reads JSON files in the configured `i18nPaths`, flattening nested objects into dot-separated keys (`a.b.c`). Keys are unioned across all locale files; values come only from files whose locale matches `state.lang` (exact or `lang-XX`). Also does key-consistency validation across locales, and full-key ↔ `JsonProperty` lookups for navigation.
3. `services/NgxTranslateTranslationCache` — `CachedValue` snapshot of all keys/values, invalidated by the translation JSON files and the config state. **Feature code should read keys/values through this cache, not parse JSON directly.**

**Translation file identity:** `NgxTranslateConstants.LOCALE_PATTERN` matches `en.json`, `en-US.json`, `settings.en.json`, `settings.en-US.json`. `NgxTranslateUtils.translationFileParts` returns `(sectionPrefix, locale)`; missing/extra-key checks only compare files with the same section prefix within a directory.

**Feature gate:** every extension early-returns unless `NgxTranslateUtils.isSupportedProject(project)` — root `package.json` must include `@ngx-translate/core` or `@jsverse/transloco` plus Angular/Nx markers. Keep this check in new extension points.

**Key detection:** there is no call-site analysis (e.g. of `translate` pipe or `instant()`); any `JSLiteralExpression` whose string value is a known key in the cache is treated as a translation key (references, inlays, folding).

**Feature packages:**
- `reference/` — `PsiReferenceContributor` on `JSLiteralExpression` (TS + Angular2) → navigates to the JSON property.
- `completion/` — keys offered only on repeated basic completion (`invocationCount > 1`) or smart completion when `addToSmartCompletion` is on.
- `inlay/`, `folding/` — show translation values inline / fold keys into values; `NgxTranslateFoldingAutoCollapseService` re-collapses folds when the caret leaves the line.
- `navigation/NgxTranslateJsonKeyGotoDeclarationHandler` — reverse navigation: JSON key → usages in TS/HTML.
- `inspection/` — JSON annotators: extra keys (error), unused keys, duplicate values (warnings); each toggleable in settings.
- `ui/` — right-side tool window (tree of translations + missing/extra key warnings with clickable links).
- `settings/` — project configurable under Tools.

User-facing strings go in `src/main/resources/messages/NgxTranslateIntellisenseBundle.properties` via `NgxTranslateIntellisenseBundle`.

## Tests

`src/test/kotlin` uses JUnit 4 with `TestFrameworkType.Platform`. Pure-logic tests (e.g. `NgxTranslatePsiUtils.validateTranslationKeySets`, marked `internal` for testability) don't need a fixture.
