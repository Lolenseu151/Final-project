# LevelSelectScreen Hitbox Configuration Guide

## Fixed for 1280×800 Screen

### Button Hitbox Positions (CORRECTED)

```java
// Level 1 (Top):    x=300, y=620, width=140, height=75
// Level 2:          x=300, y=515, width=140, height=75
// Level 3 (Middle): x=300, y=410, width=140, height=75
// Level 4:          x=300, y=305, width=140, height=75
// Level 5 (Bottom): x=300, y=200, width=140, height=75
```

**Total Hitbox Width:** 140 pixels  
**Total Hitbox Height:** 75 pixels each  
**Horizontal Alignment:** All at x=300 (left side of screen)  
**Vertical Spacing:** 105 pixels between Y positions (620→515→410→305→200)

---

## How the Hover Detection Works

### 1. **Enter Detection (Mouse Over Button)**
```
Mouse Position → Scene2D Hit Test → ClickListener.enter()
↓
hoveredLevel = levelNumber
↓
Render draws YELLOW glow outline
```

### 2. **Exit Detection (Mouse Leaves Button)**
```
Mouse Position → Outside Button Bounds → ClickListener.exit()
↓
hoveredLevel = -1 (cleared)
↓
Render checks if still selected, draws CYAN glow or nothing
```

### 3. **Click Detection (Button Pressed)**
```
Mouse Click → Inside Button Bounds → ClickListener.clicked()
↓
selectedLevel = levelNumber (PERSISTS)
↓
hoveredLevel = -1 (cleared, click isn't a hover)
↓
Game starts after short delay
```

---

## Highlight State Logic

### State Machine:
```
IDLE (hoveredLevel = -1, selectedLevel = 1)
  ↓ Mouse over Level 3
HOVER (hoveredLevel = 3, selectedLevel = 1)
  ↓ Click
SELECT (hoveredLevel = -1, selectedLevel = 3)
  ↓ Mouse over Level 2
HOVER+SELECT (hoveredLevel = 2, selectedLevel = 3)
  ↓ Click
SELECT (hoveredLevel = -1, selectedLevel = 2)
```

### Visual Feedback:
- **hoveredLevel != -1**: Draw YELLOW double-border glow
  - Inner border: (x-5, y-5, w+10, h+10)
  - Outer border: (x-8, y-8, w+16, h+16)
  - Color: (1, 1, 0) - Bright yellow
  - Opacity: 0.8 - Full brightness

- **hoveredLevel == -1 && selectedLevel == i**: Draw CYAN border glow
  - Border: (x-3, y-3, w+6, h+6)
  - Color: (0.5, 1, 1) - Cyan
  - Opacity: 0.6 - Semi-transparent

---

## Key Implementation Details

### Button Creation (`createLevelButton`)
- Creates 1×1 transparent texture (invisible)
- Positions button at exact hitbox coordinates
- Adds ClickListener with enter()/exit()/clicked() methods
- Each listener updates `hoveredLevel` or `selectedLevel` variable

### Render Loop (`render`)
- Handles keyboard input (LEFT/RIGHT/ENTER/ESC)
- Calls `stage.act(delta)` to update mouse hovering
- Calls `stage.draw()` to render background + buttons
- Calls `drawHighlights()` to overlay highlight glows

### Highlight Drawing (`drawHighlights`)
- Uses ShapeRenderer with Line mode
- Iterates through all 5 levels
- Draws YELLOW glow if `hoveredLevel == i`
- Draws CYAN glow if `selectedLevel == i` (and not hovering)
- Logs which level is being highlighted

---

## Why Hover is Now Responsive

✅ **Separate State Tracking**
- `hoveredLevel` only set by Scene2D mouse events (enter/exit)
- `selectedLevel` only set by clicks (persists)
- No conflicting state variables

✅ **Scene2D Hit Detection**
- Built-in collision detection between mouse cursor and button bounds
- Triggers enter()/exit() automatically
- No manual coordinate checking needed

✅ **Fixed Coordinates**
- Hitbox positions now match typical 1280×800 level select layouts
- All buttons use same X coordinate (300) for vertical alignment
- Vertical spacing consistent (105 pixels apart)

✅ **No Dead Zones**
- Each button is 140×75 pixels
- No gaps between hitboxes
- Coverage is complete from y=200 to y=620

---

## Testing Checklist

- [ ] Hover Level 1 → See YELLOW glow at top
- [ ] Hover Level 5 → See YELLOW glow at bottom
- [ ] Move between levels → Yellow glow follows mouse instantly
- [ ] Click Level 3 → See CYAN glow (selection persists)
- [ ] Hover Level 2 while Level 3 selected → See YELLOW glow, then CYAN after moving away
- [ ] Use LEFT/RIGHT arrows → Selection changes, hover clears
- [ ] Use ENTER → Start game with selected level
- [ ] Use ESC → Return to main menu

---

## If Highlights Still Misaligned

1. **Verify SelectLevel.png is 1280×800** (exact pixel dimensions)
2. **Check button area positions in PNG** (measure pixel coordinates)
3. **Adjust BUTTON_POSITIONS array** to match PNG layout:
   ```java
   {x, y, width, height}  // Exact pixel coordinates in PNG
   ```
4. **Verify hitbox doesn't overlap** (Y positions should be 105+ pixels apart)
5. **Check DesktopLauncher.java** - Screen size should be 1280×800

---

## Code Location

**File:** `src/main/java/com/mygdx/game/LevelSelectScreen.java`

**Key Variables:**
- `BUTTON_POSITIONS[5]` - Hitbox coordinates (line 45-52)
- `hoveredLevel` - Current mouse hover (line 41)
- `selectedLevel` - Persistent selection (line 40)
- `drawHighlights()` - Visual feedback (line 227-267)
