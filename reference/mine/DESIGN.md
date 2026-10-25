---
name: Warm Editorial Culinary System
colors:
  surface: '#fff8f6'
  surface-dim: '#e1d8d6'
  surface-bright: '#fff8f6'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#fbf2f0'
  surface-container: '#f5ecea'
  surface-container-high: '#efe6e4'
  surface-container-highest: '#e9e1df'
  on-surface: '#1e1b1a'
  on-surface-variant: '#594137'
  inverse-surface: '#342f2e'
  inverse-on-surface: '#f8efed'
  outline: '#8d7165'
  outline-variant: '#e1bfb2'
  surface-tint: '#a33e00'
  primary: '#9f3d00'
  on-primary: '#ffffff'
  primary-container: '#c74e00'
  on-primary-container: '#fffbff'
  inverse-primary: '#ffb596'
  secondary: '#006c4a'
  on-secondary: '#ffffff'
  secondary-container: '#82f5c1'
  on-secondary-container: '#00714e'
  tertiary: '#8d4b00'
  on-tertiary: '#ffffff'
  tertiary-container: '#b15f00'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbcd'
  primary-fixed-dim: '#ffb596'
  on-primary-fixed: '#360f00'
  on-primary-fixed-variant: '#7c2e00'
  secondary-fixed: '#85f8c4'
  secondary-fixed-dim: '#68dba9'
  on-secondary-fixed: '#002114'
  on-secondary-fixed-variant: '#005137'
  tertiary-fixed: '#ffdcc3'
  tertiary-fixed-dim: '#ffb77d'
  on-tertiary-fixed: '#2f1500'
  on-tertiary-fixed-variant: '#6e3900'
  background: '#fff8f6'
  on-background: '#1e1b1a'
  surface-variant: '#e9e1df'
typography:
  display:
    fontFamily: Newsreader
    fontSize: 44px
    fontWeight: '500'
    lineHeight: 52px
  display-mobile:
    fontFamily: Newsreader
    fontSize: 34px
    fontWeight: '500'
    lineHeight: 40px
  headline-lg:
    fontFamily: Newsreader
    fontSize: 32px
    fontWeight: '500'
    lineHeight: 38px
  headline-lg-mobile:
    fontFamily: Newsreader
    fontSize: 26px
    fontWeight: '500'
    lineHeight: 32px
  headline-md:
    fontFamily: Newsreader
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Newsreader
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Manrope
    fontSize: 17px
    fontWeight: '400'
    lineHeight: 26px
  body-md:
    fontFamily: Manrope
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 22px
  body-sm:
    fontFamily: Manrope
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-lg:
    fontFamily: Manrope
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: Manrope
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
  label-sm:
    fontFamily: Manrope
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-mobile: 0.75rem
  margin: 1.5rem
  margin-mobile: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style

This design system expresses a warm, editorial culinary identity reminiscent of modern independent food magazines and Michelin-level recipe volumes. It bridges timeless literary typography with clean, contemporary functional surfaces, prioritizing clarity, visual breathing room, and artisanal tactility.

### Personality & Tone
- **Warm & Epicurean:** Centered on rich caramel tones, unbleached linen paper tones, and organic freshness to evoke tactile appetite and culinary reverence.
- **Editorial & Precise:** High-contrast serif headlines provide literary poise, balanced by structured, neutral geometric sans for effortless scanning in active cooking environments.
- **Quiet Luxury:** Restrained visual noise, generous negative space, delicate border work, and muted ambient light replace heavy gradients and artificial embellishments.

### Design Principles
- **Atmospheric Whitespace:** Screens maintain unhurried, airy spacing to minimize cognitive overload while prepping or cooking.
- **Sensory Depth:** Subtle tonal elevations and soft warm cast shadows mimic premium printed paper stock stacked on a sunlit wooden kitchen table.
- **Utilitarian Elegance:** Clear interactive states and oversized actionable tap targets ensure effortless navigation even with wet or flour-dusted hands.

## Colors

The palette balances warm organic neutrality with targeted chromatic highlights:

- **Primary (`#E85D04`):** Rich sun-drenched caramel orange. Used for primary call-to-actions, cooking progress indicators, active step tags, and focused interactive states.
- **Secondary (`#059669`):** Fresh basil herb green. Reserved for organic dietary badges, fresh ingredient indicators, healthy substitutions, and positive confirmation states.
- **Tertiary (`#D97706`):** Warm roasted amber. Deployed for review ratings, preparation difficulty meters, and culinary curation marks.
- **Neutral (`#292524`):** Deep charcoal stone. Provides soft yet resolute typographic contrast without the harshness of pure black.

### Surface Tones & Backgrounds
- **Canvas Base:** `#FAFAF7` (Warm cream linen base, soft on the eyes in warm lighting).
- **Surface Layer:** `#FFFFFF` (Pure card surface for ingredients and step cards).
- **Subtle Recessed Surface:** `#F3F2EE` (Pantry gray for disabled segments, metadata containers, and recipe prep notes).
- **Subdued Text:** `#78716C` (Warm stone gray for secondary metadata, cooking prep times, and author captions).
- **Structural Border:** `#E7E5E4` (Subtle boundary lines to ground elevated cards against canvas base).

## Typography

The type scale orchestrates a deliberate dialog between high-culture culinary prose and crisp operational accuracy:

- **Headlines (Newsreader):** Used for recipe titles, editorial essays, chapter headings, and storytelling introductions. Uses optical sizing and subtle italic accents for chef notes and culinary origins.
- **Functional Body & Labels (Manrope):** Geometric clarity ensuring instant recognition for ingredient measurements, timer readouts, cooking equipment callouts, and step actions.
- **Hierarchy Rules:** Recipe titles command high visual weight using `headline-lg` with tightened tracking. Measurement units, times, and step enumerations utilize `label-md` or `label-lg` with generous tabular numeric clarity.

## Layout & Spacing

Designed primarily for mobile culinary consumption with smooth tablet responsive capabilities:

- **Mobile Grid:** Single-column layout anchored with an outer screen margin of `1.25rem` (20px) and contextual inner gutters of `0.75rem` (12px).
- **Rhythm & Padding:** Sections rely heavily on vertical pacing (`space-xl` between recipe chapters, `space-lg` between ingredient sub-categories, `space-sm` between ingredients and unit counters).
- **Touch Ergonomics:** High-priority culinary execution touch points (start timer, check ingredient, next cooking step) preserve min-heights of 48px to 56px to ensure effortless interaction during kitchen tasks.
- **Edge Restraint:** Full-bleed recipe imagery transitions into bounded, bordered card structures maintaining unified lateral padding.

## Elevation & Depth

Visual hierarchy uses warm ambient surface diffusion combined with delicate structural outlines rather than heavy drop shadows:

- **Level 0 (Flat Canvas):** `#FAFAF7` canvas ground. Zero elevation.
- **Level 1 (Card Rest):** Background `#FFFFFF`, bordered with 1px solid `#E7E5E4`, cast with a diffuse warm shadow: `box-shadow: 0 2px 8px -2px rgba(41, 37, 36, 0.04), 0 1px 4px -1px rgba(41, 37, 36, 0.02)`.
- **Level 2 (Active Step & Floating Control):** Background `#FFFFFF`, border `#E7E5E4`, cast with soft elevated warmth: `box-shadow: 0 8px 24px -4px rgba(41, 37, 36, 0.08), 0 2px 6px -1px rgba(41, 37, 36, 0.04)`.
- **Level 3 (Modal Bottom Sheets):** Cast against a `#292524` backdrop with 24% opacity: `box-shadow: 0 16px 40px -8px rgba(41, 37, 36, 0.16)`.

## Shapes

The shape system employs roundedness level 2 to deliver inviting, organic warmth without sliding into playful distortion:

- **Micro Shapes (`rounded` - 0.5rem):** Ingredient checkbox squares, measurement unit badges, small counter steppers.
- **Containers (`rounded-lg` - 1rem):** Recipe instruction cards, modal overlays, image preview windows, ingredient group containers.
- **Featured Elements (`rounded-xl` - 1.5rem):** Hero recipe image cards, persistent bottom cooking drawers.
- **Pill Caps (`rounded-full`):** Filter tags, culinary tags, timer buttons, primary action buttons, serving size controls.

## Components

### Buttons
- **Primary Action (Start Cooking / Order Basket):** Full-bleed or pill container, `#E85D04` background with crisp `#FFFFFF` text (`label-lg`), subtle warm glow on press, height 52px.
- **Secondary (Save Recipe / Share):** Warm stone outline (`1px solid #E7E5E4`), `#FFFFFF` background, `#292524` text, gentle scale-down feedback on click.
- **Ghost Action (Unit Switcher / Servings):** No border, `#F3F2EE` hover surface, `#78716C` resting color shifting to `#292524` on active.

### Chips & Badges
- **Editorial Flavor Chips:** `#FAFAF7` background with 1px border `#E7E5E4`, typography `label-md` in `#78716C`. Active chips turn into solid `#E85D04` fill with white text.
- **Ingredient Health / Dietary Tags:** Tinted `#059669` light wash (opacity 10%) with dark green `#059669` typography (`label-sm`).

### Ingredient Checklist
- **List Item:** Height 48px, horizontal layout separating ingredient name and precise quantity.
- **Checkboxes:** Custom rounded 6px square with border `#D6D3D1`. When checked, smooth fill transition to `#059669` with white checkmark, while ingredient text transitions to `#A8A29E` with delicate strikethrough.

### Recipe Step Cards
- **Card Container:** `#FFFFFF` fill with 1px border `#E7E5E4` and Level 1 elevation.
- **Header:** Step number highlighted in caramel orange (`Newsreader`, italic headline-md).
- **Body:** Clear Manrope body-md, with interactive culinary terms (e.g., "simmer", "sear") hyperlinked with dotted orange underlines triggering temperature/time hints.

### Inputs & Quantity Steppers
- **Search & Filter:** Pill shape, `#FFFFFF` background, 1px border `#E7E5E4`, warm charcoal text, subtle orange ring on focus.
- **Serving Stepper:** Pill container in `#F3F2EE`, enclosing minus/plus triggers with centered bold numeric counter.