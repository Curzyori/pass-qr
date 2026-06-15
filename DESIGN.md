# PassQR-15 DESIGN.md

**Aesthetic Goal:** Industrial Brutalist with Humanist Warmth (Based on Claude Design Spec).
**Palette:**
- Primary: #cc785c (Coral/Rust)
- Canvas: #faf9f5 (Tinted Cream)
- Surface: #181715 (Deep Navy/Black)
- Ink: #141413 (Deep Ink)

**UI Components:**
- Dashboard: Single screen with top segmented control (Scanner/Generator).
- Cards: Elevated surfaces (#efe9de) with rounded-lg (12px).
- Typography: StyreneB/Inter for UI, Copernicus/Tiempos for headers.
- Input Fields: Tinted canvas with primary coral active borders.
- Buttons: Primary Coral with on-primary white text.

**Navigation:**
- Top segmented control (Active: Coral background, Inactive: Transparent).
- Smooth transition between Camera preview (Scanner) and Input form (Generator).

**Privacy & Security:**
- Password Field: Secure by default (password mask). Show/Hide toggle icon.
- History: Stored locally in EncryptedSharedPreferences.
