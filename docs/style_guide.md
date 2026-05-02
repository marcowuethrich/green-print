# Frontend Style Guide — GreenPrint

This document defines the high-end visual and interaction standards for GreenPrint, designed to provide a modern, fresh, and professional user experience.

## 1. Design Philosophy
GreenPrint's design is **nature-centric, airy, and tactile**. It prioritizes soft edges, subtle depth (via gradients and fills rather than heavy shadows), and purposeful animations to create a "living" interface.

## 2. Visual Identity

### Color Palette (Emerald & Sage)
A sophisticated, modern palette that moves away from generic greens toward a more premium aesthetic.

| Role | Color (Hex) | Usage |
| :--- | :--- | :--- |
| **Emerald Dark** | `#064E3B` | Primary text and high-contrast elements. |
| **Emerald Primary** | `#10B981` | Brand identity and primary actions. |
| **Emerald Light** | `#D1FAE5` | Tonal backgrounds and accent areas. |
| **Sage Secondary** | `#6B7280` | Metadata and secondary text. |
| **Soft White** | `#F9FAFB` | Main background (part of gradient). |
| **Pure White** | `#FFFFFF` | Surface areas (Cards, Dialogs). |

### Backgrounds
- Use a vertical gradient: `linear-gradient(to bottom, #ECFDF5, #F9FAFB)`.
- This creates a soft, organic feel that distinguishes the app from flat-color competitors.

## 3. Typography
Focus on readability and strong hierarchy using Material 3 scales with custom weights.

- **Headline Small**: Bold, `24.sp`, negative letter spacing. Used for primary greetings.
- **Title Large**: Semi-Bold, `20.sp`. Used for page titles (e.g., Space name).
- **Body Large**: Regular, `16.sp`. Used for descriptive text.
- **Label Large**: Medium, `14.sp`, Bold. Used for button labels to ensure actionability.

## 4. Component Standards

### Cards & Surfaces
- **Extra Rounded**: Use `24.dp` (Large) or `32.dp` (Extra Large) for primary containers.
- **Borders**: Prefer a subtle `1.dp` border (`outlineVariant`) over heavy shadows.
- **Elevation**: Use `0.dp` or very low elevation for a modern, "flat-depth" look.

### Interactive Elements
- **Primary Buttons**: Extra large rounded corners, high-contrast text, height of `56.dp` for a "tappable" feel.
- **Inputs**: `OutlinedTextField` with `16.dp` corners.

### Animation & Motion
- **Entrance**: All screens should use a coordinated `FadeIn + SlideInVertically` entrance animation.
- **Interactive**: Use `animateContentSize()` for expanding/collapsing elements.
- **State Changes**: Transitions between UI states (Loading -> Content) should be smooth, never jarring.

## 5. Iconography
- **Material Symbols**: Use a consistent weight and style.
- **Leaf Branding**: The `ic_leaf` vector is the primary brand mark, used for empty states and branding rows.
- **Visual Balance**: Always pair icons with descriptive text or labels to maintain accessibility.
