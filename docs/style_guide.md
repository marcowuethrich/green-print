# GreenPrint — Design System

A calm, modern design system for plant care.

GreenPrint is a tool for people who want their digital things to feel as alive and well-tended as the plants they're growing. The visual language is soft, natural, and confident — built around generous whitespace, gentle motion, and a single typeface set with care.

This document is the source of truth for visual decisions. Every token here is consumed by the Compose theme in `composeApp/src/commonMain/kotlin/ch/wuethrich/greenprint/ui/theme/`.

---

## Contents

1. [Principles](#principles)
2. [Palette — "Spring Garden"](#palette--spring-garden)
3. [Typography](#typography)
4. [Spacing & Layout](#spacing--layout)
5. [Shapes](#shapes)
6. [Elevation](#elevation)
7. [Motion](#motion)
8. [Components](#components)
9. [Iconography](#iconography)
10. [Accessibility](#accessibility)
11. [Voice & Tone](#voice--tone)
12. [Implementation Checklist](#implementation-checklist)

---

## Principles

1. **Calm over loud.** Whitespace is a feature, not a gap to fill.
2. **Soft, not sharp.** Living things have rounded edges. Our geometry follows.
3. **Natural palette, modern tuning.** Greens that look like real foliage, not marketing decks.
4. **One typeface, well-set.** Inter, with hierarchy from weight and size — not from family-mixing.
5. **Motion that responds, not performs.** Subtle springs and smooth fades. No parlor tricks.
6. **Accessible by default.** AA contrast, generous tap targets, motion can be muted.

---

## Palette — "Spring Garden"

Calm sage greens against a warm off-white, with one warm coral accent reserved for moments of delight (a streak, a successful repot, a milestone).

```
Foundation
  background      #F7F9F5   sage-tinted off-white — the canvas
  surface         #FFFFFF   cards, sheets, primary surfaces
  surface-dim     #EDF1E8   slightly recessed surface (input fields, etc.)

Brand
  primary         #4F9461   fresh sage — the brand color
  primary-dark    #3D7A4D   pressed/active state
  primary-light   #DDE9D8   soft tinted backgrounds, primary container

Accent (use sparingly)
  accent          #F2A47A   warm coral — for celebrations and delight
  accent-light    #FBE4D5   tinted accent backgrounds

Text
  text            #1F2D1F   deep forest near-black — primary text
  text-muted      #5A6B5A   warm gray-green — secondary text
  text-subtle     #95A095   tertiary text, disabled

Lines
  outline         #D3DCD0   subtle borders and dividers

System
  error           #D14B4B
  success         #4F9461   shares the brand green — calm "good"
  warning         #E8B25E   soft gold
```

### Use rules

- **Primary** is for the main CTA, brand moments, and active states. Don't tile it across whole surfaces.
- **Accent (coral)** is reserved for *positive* delight — never for warnings or destructive actions. One coral element per screen at most.
- **Text** is `#1F2D1F`, not pure black. Pure black against our warm background looks harsh.
- **Borders** are quiet. If you find yourself thickening borders to add definition, reach for elevation instead.

---

## Typography

One family: **Inter**. Hierarchy is built from size and weight. The variable file is bundled at `composeApp/src/commonMain/composeResources/font/inter_variable.ttf`.

| Role          | Size    | Weight        | Tracking | Where it goes                  |
|---------------|---------|---------------|----------|--------------------------------|
| Display       | 36sp    | Bold (700)    | -0.5sp   | Page heroes (rare)             |
| Headline      | 28sp    | SemiBold (600)| -0.4sp   | Empty-state titles, big greets |
| Title Large   | 22sp    | SemiBold (600)| -0.3sp   | Section heads                  |
| Title         | 18sp    | SemiBold (600)| -0.2sp   | Card titles                    |
| Body Large    | 16sp    | Regular (400) | 0        | Long-form copy                 |
| Body          | 14sp    | Regular (400) | 0        | Secondary copy                 |
| Label         | 13sp    | Medium (500)  | 0.1sp    | Buttons, chips                 |
| Caption       | 12sp    | Medium (500)  | 0.2sp    | Metadata, timestamps           |

### Setting type

- Line-height is generous — body copy lands at `1.55–1.65×` the size.
- Headlines use slightly **negative** tracking (`-0.2 to -0.5sp`) for a tighter, more confident set.
- Body text uses `0` tracking. Don't loosen body type "for legibility"; it backfires.
- Avoid all-caps in UI text. Caps belong in the occasional small label (≤12sp) and not in headlines.

---

## Spacing & Layout

Base unit: **4dp**. Generous gaps over dense layouts.

```
space-1   4dp     hairlines, micro-gaps
space-2   8dp     tight grouping (icon + label)
space-3   12dp    related controls
space-4   16dp    default content padding
space-5   24dp    card padding, section gaps
space-6   32dp    larger sections
space-7   48dp    page-level breathing room
space-8   64dp    hero spacing
```

- Card content padding: **20–24dp** all sides.
- Stack rhythm inside cards: **12–16dp** between elements.
- Page-level horizontal padding on phone: **24dp**.
- Maximum content width on tablet/desktop: **640dp** for prose, **1120dp** for grids.

---

## Shapes

Geometry is friendly. Corners are always rounded — the only question is how much.

| Token       | Radius  | Where it goes                              |
|-------------|---------|--------------------------------------------|
| extra-small | 8dp     | Chips, tags, small badges                  |
| small       | 12dp    | Small buttons, compact inputs              |
| medium      | 16dp    | Inputs, dropdowns, alerts                  |
| large       | 24dp    | Cards, sheets, modals                      |
| extra-large | 32dp    | Hero cards, large surfaces                 |
| pill        | 999dp   | Pill buttons, status indicators            |

Avoid sharp corners. Avoid radii between these tokens — pick one.

---

## Elevation

Soft, layered shadows via Material's elevation tokens. **No manual offsets, no opaque blocks, no stacking shadows for drama.**

| Level | Where it goes                                     |
|-------|---------------------------------------------------|
| 0dp   | Background, default surface                       |
| 1dp   | Cards at rest                                     |
| 3dp   | Cards on hover/focus                              |
| 6dp   | Modals, bottom sheets, menus                      |
| 12dp  | Floating action buttons                           |

In Compose, prefer `CardDefaults.cardElevation(defaultElevation = 1.dp, hoveredElevation = 3.dp)` over hand-rolled shadows.

---

## Motion

Motion should feel like the UI is *responding*, not *performing*. Calm, fast, and predictable.

### Easing

Use Material's "emphasized" easing as the default:

```
standard     cubic-bezier(0.2, 0.0, 0.0, 1.0)   most transitions
decelerate   cubic-bezier(0.0, 0.0, 0.0, 1.0)   things entering view
accelerate   cubic-bezier(0.3, 0.0, 1.0, 1.0)   things leaving view
spring       Spring(stiffness=Medium, damping=NoBouncy)   press feedback
```

### Durations

- **Instant**: 100ms — micro-feedback (toggle, checkmark)
- **Quick**: 180ms — small UI tweaks (color shift, opacity)
- **Standard**: 250ms — most transitions
- **Slow**: 400ms — page-level reveals, modal entrances

### Patterns

- **Press feedback** — scale to 0.98 with a quick spring back. Subtle. Never bouncy or comedic.
- **Page entrance** — fade + 16dp slide-up over 250ms.
- **Stagger** — when revealing a list, offset children by 60ms each. Calm cascade.
- **No infinite loops** outside of loaders. No marquees. No glitch effects. No ambient motion.

### Reduced motion

When `prefers-reduced-motion` is set, replace every transition with an instant fade. Stagger becomes a single fade. Press feedback becomes a 50ms color flash. Test by toggling the OS setting.

---

## Components

### Cards

The hero component. Plants, rooms, spaces — they all live in cards.

- **Background**: `surface` (#FFFFFF)
- **Radius**: `large` (24dp)
- **Elevation**: 1dp resting, 3dp on hover/press
- **Padding**: 20dp
- **Border**: none (rely on elevation). If you must add one, use `outline` at 1dp for very dim contexts.
- **Press feedback**: subtle scale to 0.98 + elevation rise

### Buttons

| Variant     | Style                                                                 |
|-------------|-----------------------------------------------------------------------|
| Primary     | Filled `primary`, white text, 16dp radius, 56dp tall                  |
| Secondary   | Outlined 1.5dp `primary` border, primary text, transparent fill       |
| Tertiary    | No background or border, primary text, used inline                    |
| Pill        | Filled or outlined, 999dp radius, used for filters/categories         |
| Destructive | Outlined `error`, error text — no filled-red destructive buttons      |

All buttons:
- Press feedback: subtle scale 0.98 (handled by `Modifier.softPress`)
- Min height 48dp, recommended 56dp for primary actions
- Label uses `label` typography (13sp Medium)

### Inputs

- Background: `surface-dim` (#EDF1E8)
- Radius: `medium` (16dp)
- Border: 1dp `outline` resting, 2dp `primary` on focus
- Internal padding: 16dp horizontal, 14dp vertical
- Label sits above the field in `label` typography, `text-muted`
- Helper text below in `caption`, `text-muted`
- Error state: 2dp `error` border + error helper text

### Tags & Chips

- Radius: `extra-small` (8dp) or `pill` (999dp) — pick one per surface
- Background: `primary-light` for active, `surface-dim` for resting
- Text: `label` typography, `text-muted` resting / `primary-dark` active

### Modals & Sheets

- Radius: `large` (24dp), or top-only 24dp for bottom sheets
- Elevation: 6dp
- Backdrop: `text` (#1F2D1F) at 40% opacity
- Entrance: fade + 16dp slide-up, 250ms

### Empty states

Empty states aren't errors — they're invitations. Always include:
- A friendly illustration or icon (in `primary-light` circle)
- A short, encouraging title
- A one-sentence subtitle
- One clear primary action

---

## Iconography

- **Library**: [Lucide](https://lucide.dev) — clean, consistent stroke icons
- **Stroke weight**: 2dp
- **Caps and joins**: rounded
- **Default size**: 24dp; large displays use 48–64dp
- **Color**: inherit from surrounding text (`text` or `primary` for active)

Avoid filled icons. Avoid mixing icon styles (Lucide stroke + Material filled — pick one per screen).

---

## Accessibility

This is non-negotiable, not a polish pass.

- ✅ **AA contrast minimum** for all text/background pairs (4.5:1 body, 3:1 large)
- ✅ **Tap targets ≥ 48dp** on touch surfaces, 44dp absolute minimum
- ✅ **Focus states are visible** — 2dp `primary` outline at 2dp offset
- ✅ **Motion respects `prefers-reduced-motion`** — instant fades, no springs
- ✅ **Color is never the only signal** — error states have icons or copy too
- ✅ **Screen reader labels** on every icon-only button

---

## Voice & Tone

Warm, encouraging, never preachy. The app helps; it doesn't lecture. Plants don't make you feel guilty for being late, and neither should we.

| Don't say                  | Say                                            |
|----------------------------|------------------------------------------------|
| Submit                     | Save                                           |
| Are you sure?              | Delete this room?                              |
| Error                      | Couldn't save — let's try again                |
| No data                    | No plants yet. Add your first one.             |
| Loading…                   | Just a sec…                                    |
| Settings                   | Settings *(plain is fine — don't overdo it)*   |

Keep copy short. Use sentence case. Don't add exclamation points to make text feel friendly — friendliness comes from the words themselves.

---

## Implementation Checklist

Before merging anything that touches UI:

- [ ] All colors come from `MaterialTheme.colorScheme.*` or `DesignSystem.*` tokens. Zero ad-hoc `Color(0xFF…)` literals in components.
- [ ] All shapes come from `MaterialTheme.shapes.*`. No inline `RoundedCornerShape(N.dp)` outside the theme.
- [ ] All spacing comes from `DesignSystem.Space*` tokens.
- [ ] Cards use Material elevation (1–3dp), not manual shadows.
- [ ] Hover, focus, and press states defined for every interactive element.
- [ ] Press feedback uses `Modifier.softPress` (or none — never jelly/glitch).
- [ ] Stagger entrances use `StaggerFadeUp`, capped at 60ms steps.
- [ ] `prefers-reduced-motion` tested by toggling OS setting.
- [ ] Screen-reader label exists for any icon-only control.
- [ ] Copy reviewed against the [Voice & Tone](#voice--tone) table.
