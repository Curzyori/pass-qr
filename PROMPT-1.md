# PROMPT-1.md: Scaffolding Phase

**Goal:** Create a modern Android mobile app project for PassQR-15.
**Role:** Senior Android Engineer (Kotlin/Compose).

**Tasks:**
1. Scaffold a new Android Kotlin project in `/home/curzy/workspace/50-Projects-Challenges/PassQR-15` using Gradle (Kotlin DSL).
2. Configure `build.gradle.kts` with:
   - Jetpack Compose (Material 3).
   - CameraX library.
   - ZXing Android Embedded.
   - Jetpack Security (EncryptedSharedPreferences).
3. Implement `Single Activity` architecture with a `ComposeView` as content.
4. Implement the root Navigation Controller with a `DashboardScreen`.
5. Implement the top-level UI shell (Segmented Control for "Generator" and "Scanner").
6. Apply styling based on DESIGN.md:
   - Use `colors` from DESIGN.md (define them in `ui/theme/Color.kt`).
   - Use Typography scales matching Tiempos/StyreneB aesthetic.
7. Verify successful build (`./gradlew assembleDebug`).

**Constraints:**
- No online databases.
- Strictly follow the DESIGN.md aesthetics.
- Keep implementation modular for future phases.
