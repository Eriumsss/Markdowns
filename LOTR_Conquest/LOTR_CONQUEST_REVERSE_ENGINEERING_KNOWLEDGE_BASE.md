# LOTR Conquest Reverse Engineering Knowledge Base

> **Purpose**: Complete knowledge base for AI agents working on the LOTR Conquest debug overlay project.
> **Last Updated**: December 2025
> **Status**: Team ID working, Position data NOT YET FOUND

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technical Environment](#2-technical-environment)
3. [Complete Timeline of Work](#3-complete-timeline-of-work)
4. [Global Memory Addresses](#4-global-memory-addresses)
5. [Player Controller Structure Map](#5-player-controller-structure-map)
6. [Failed Position Approaches](#6-failed-position-approaches)
7. [Key Ghidra Functions](#7-key-ghidra-functions)
8. [Current Working State](#8-current-working-state)
9. [Outstanding Problems](#9-outstanding-problems)
10. [Next Steps & Strategy](#10-next-steps--strategy)
11. [Onboarding Prompt for Future Agents](#11-onboarding-prompt-for-future-agents)

---

## 1. Project Overview

### What We're Building
A **DirectX 9 debug overlay DLL** that hooks into `ConquestLLC.exe` (Lord of the Rings: Conquest) to display real-time game state information on screen.

### Ultimate Goals
-  Display **Team ID** (Good/Evil/Neutral)
-  Display **Player World Position** (X, Y, Z coordinates) - NOT YET WORKING
-  Display **Health/Mana** - NOT YET ATTEMPTED
-  Display **Other Entity Data** - NOT YET ATTEMPTED

### Architecture
```
ConquestLLC.exe
    └── d3d9.dll (our injected DLL)
            ├── Hooks IDirect3DDevice9::EndScene()
            ├── Reads game memory via direct pointer access
            └── Renders overlay text using ID3DXFont
```

### Key Files
| File | Purpose |
|------|---------|
| `DebugOverlay/src/dllmain.cpp` | Main DLL entry, EndScene hook, overlay rendering |
| `DebugOverlay/src/gamedata.cpp` | Game memory reading functions, address constants |
| `DebugOverlay/src/gamedata.h` | Data structures and function declarations |
| `Conquest/decompiled/*.c` | Ghidra decompiled functions |

---

## 2. Technical Environment

### Game Binary
- **Executable**: `ConquestLLC.exe`
- **ASLR**: **DISABLED** - Base address is always `0x00400000`
- **Architecture**: 32-bit x86
- **Calling Convention**: Primarily `thiscall` (ECX = this pointer)
- **DirectX Version**: DirectX 9

### Build Environment
```bash
cd C:\Users\Yusuf\Desktop\Code\ConquestConsole\DebugOverlay\build
msbuild DebugOverlay.sln /p:Configuration=Release
```

### DLL Injection
The compiled `d3d9.dll` is placed in the game directory. The game loads it as a proxy for the real `d3d9.dll`.

---

## 3. Complete Timeline of Work

### Phase 1: D3D9 Hook Implementation  SUCCESS

**Goal**: Hook EndScene to render overlay text

**Implementation**:
1. Created proxy DLL that forwards to real `d3d9.dll`
2. Hooked `IDirect3DDevice9::EndScene` using vtable patching
3. Used trampoline pattern: save original bytes, JMP to hook, restore and call original
4. Created `ID3DXFont` for text rendering

**Result**:  Frame counter updates every frame, overlay renders correctly

### Phase 2: Finding Player Data Structure

**Initial Hypothesis**: Player data at `DAT_00cd8048 + 0x162c`

**Discovery Process**:
1. Analyzed `DAT_00cd8048 + 0x162c` - Found static player profile (name, etc.) - NOT live data
2. Found `DAT_00cd7f20 + 0x64` in `FUN_0045849c` - Creature array with 4 slots
3. Confirmed only index 0 is non-zero for local player

**Result**:  Found player controller pointer at `[[DAT_00cd7f20] + 0x64]`

### Phase 3: Team ID Discovery  SUCCESS

**Hypothesis**: Team ID stored somewhere in controller structure

**Testing Method**: Scanned controller structure for bytes that change when switching teams

**Discovery**: Offset `+0x1CA0` contains Team ID as a BYTE
- Value 0 = Neutral
- Value 1 = Good (Fellowship)
- Value 2 = Evil (Sauron's Army)

**Result**:  Team ID confirmed working and color-coded in overlay

### Phase 4: Position Data Search  ONGOING - NOT FOUND

See [Section 6: Failed Position Approaches](#6-failed-position-approaches) for complete details.

---

## 4. Global Memory Addresses

### Confirmed Globals

| Address | Name | Purpose | Status |
|---------|------|---------|--------|
| `0x00cd7f20` | DAT_00cd7f20 | Base for creature/controller array |  WORKING |
| `0x00cd8038` | DAT_00cd8038 | Context object pointer |  TESTED, chain breaks |
| `0x00cd8048` | DAT_00cd8048 | Player manager |  Static profile only |
| `0x00cd7fdc` | DAT_00cd7fdc | Game state pointer |  NOT TESTED |

### Access Pattern for Player Controller
```c
// Get player controller pointer
DWORD base = *(DWORD*)0x00cd7f20;           // Read global
DWORD controller = *(DWORD*)(base + 0x64);   // First creature slot
// controller now points to Player Controller structure
```

---

## 5. Player Controller Structure Map

**Base Address**: `[[0x00cd7f20] + 0x64]`

**CRITICAL UNDERSTANDING**: This is a **Player Controller** object, NOT the actual game entity with world position. It contains input state and team affiliation but position is stored elsewhere.

###  Confirmed Working Offsets

| Offset | Type | Name | Values/Range | Notes |
|--------|------|------|--------------|-------|
| **+0x1CA0** | BYTE | Team ID | 0, 1, 2 | 0=Neutral, 1=Good, 2=Evil |

###  Input/Movement Vectors (Working)

| Offset | Type | Name | Range | Behavior |
|--------|------|------|-------|----------|
| +0xA0 | float | InputLeftRight | -1.0 to +1.0 | Left=-1, Right=+1, None=0 |
| +0xA4 | float | InputForwardBack | -1.0 to +1.0 | Back=-1, Forward=+1 |
| +0xA8 | float | InputStrafe | -1.0 to +1.0 | Strafe direction |

###  Camera/Look Direction (Changes on mouse - NOT pure position)

| Offset | Type | Name | Notes |
|--------|------|------|-------|
| +0xC0 | float | LookTargetX | Changes on walk AND mouse movement |
| +0xC4 | float | LookTargetY | Changes on jump |
| +0xC8 | float | LookTargetZ | Changes on walk AND mouse movement |
| +0xD0 | float | CameraX | Changes on mouse movement only |
| +0xD4 | float | CameraY | Vertical look angle |
| +0xD8 | float | CameraZ | Changes on mouse movement only |
| +0xE0 | float | AnimBreathing | Oscillates with breathing animation |

###  NOT Position Data

The +0xC0/C4/C8 values were initially thought to be position but:

- They change when moving the mouse (pure position wouldn't)
- They show discrete jumps, not smooth world coordinates
- They are likely look-at targets or velocity vectors

### Pointer Fields (Observed but chains failed)

| Offset | Type | Observed Value | Notes |
|--------|------|----------------|-------|
| +0x010 | DWORD | 0x0000218C | NOT a valid heap pointer |
| +0x080 | DWORD* | 0x0562D030 | Valid pointer, chain to position failed |
| +0x084 | DWORD* | 0x0564A140 | Valid pointer, chain to position failed |
| +0x124 | DWORD | 0x00000001 | Value is 1, NOT a pointer |
| +0x130 | DWORD* | 0x0562D138 | Valid pointer, tested - no position |
| +0x13C | DWORD* | 0x0562D148 | Valid pointer, shows direction vectors only |

---

## 6. Failed Position Approaches

###  Attempt 1: Direct Offsets on Controller

**Hypothesis**: Position stored directly in controller structure at common offsets

**Code Tested**:
```c
float x = *(float*)(controller + 0x40);
float y = *(float*)(controller + 0x44);
float z = *(float*)(controller + 0x48);
// Also tried: 0x50-0x58, 0x60-0x68, 0x70-0x78, etc.
```

**Result**:  All values static or garbage

**Lesson**: Controller structure doesn't contain position directly

---

###  Attempt 2: Transform Pointer at +0x124

**Hypothesis**: Based on `FUN_007cd0c1` which uses `*(int*)(obj + 0x124) + 0x40/0x48`

**Code Tested**:
```c
DWORD transform = *(DWORD*)(controller + 0x124);  // Expected pointer
float x = *(float*)(transform + 0x40);
float z = *(float*)(transform + 0x48);
```

**Result**:  `controller + 0x124` = 1 (not a pointer)

**Lesson**: The +0x124 pattern applies to a DIFFERENT object type, not our controller

---

###  Attempt 3: Nested Chain via +0x10

**Hypothesis**: Controller has inner object at +0x10 that has the transform

**Code Tested**:
```c
DWORD inner = *(DWORD*)(controller + 0x10);      // Expected: object pointer
DWORD transform = *(DWORD*)(inner + 0x124);      // Expected: transform pointer
float x = *(float*)(transform + 0x40);
```

**Result**:  `controller + 0x10` = 0x218C (not a valid heap pointer)

**Lesson**: +0x10 is not a pointer in this structure

---

###  Attempt 4: Via +0x130 Pointer

**Hypothesis**: Pointer at +0x130 leads to entity with position

**Code Tested**:
```c
DWORD ptr130 = *(DWORD*)(controller + 0x130);    // Valid pointer: 0x0562D138
float x = *(float*)(ptr130 + 0x30);              // Various offsets tried
float y = *(float*)(ptr130 + 0x34);
float z = *(float*)(ptr130 + 0x38);
```

**Result**:  Values static, don't change on movement

---

###  Attempt 5: Via +0x080 -> +0x10 -> +0x124 Chain

**Hypothesis**: Deep pointer chain following FUN_007cd0c1 pattern through +0x080

**Code Tested**:
```c
DWORD ptr080 = *(DWORD*)(controller + 0x080);
DWORD inner = *(DWORD*)(ptr080 + 0x10);
DWORD transform = *(DWORD*)(inner + 0x124);
float x = *(float*)(transform + 0x40);
```

**Result**:  Chain produces valid pointers but position doesn't update

---

###  Attempt 6: Via +0x084 Chain

**Hypothesis**: Similar to +0x080 but using adjacent pointer

**Code Tested**:
```c
DWORD ptr084 = *(DWORD*)(controller + 0x084);
DWORD inner = *(DWORD*)(ptr084 + 0x10);
DWORD transform = *(DWORD*)(inner + 0x124);
float x = *(float*)(transform + 0x40);
```

**Result**:  Same as +0x080 - chain exists but no position data

---

###  Attempt 7: +0x13C Direct Read

**Hypothesis**: +0x13C pointer leads directly to position floats

**Code Tested**:
```c
DWORD ptr13C = *(DWORD*)(controller + 0x13C);
float x = *(float*)(ptr13C + 0x40);
float y = *(float*)(ptr13C + 0x44);
float z = *(float*)(ptr13C + 0x48);
```

**Result**:  Values DO change, but only show -1.0/0.0/+1.0 - these are direction vectors, not world position

---

###  Attempt 8: DAT_00cd8038 Context Chain

**Hypothesis**: Based on `FUN_0079ce88` pattern for entity lookup

**Code Tested**:
```c
DWORD context = *(DWORD*)0x00cd8038;
DWORD ctrl = *(DWORD*)(context + 0x18c);
DWORD entity = *(DWORD*)(ctrl + 0xc);
DWORD inner = *(DWORD*)(entity + 0x10);
DWORD transform = *(DWORD*)(inner + 0x124);
float x = *(float*)(transform + 0x40);
```

**Result**:  Chain breaks - pointers don't lead to valid position

---

###  Attempt 9: Deep Scan (0x200-0x5FC)

**Hypothesis**: Position might be at unusual deep offset

**Method**: Scanned all floats from +0x200 to +0x5FC, tracked which changed on movement

**Result**:  No offsets in this range showed position-like values with large deltas

---

### Key Insight from All Failures

**The structure at `[[DAT_00cd7f20] + 0x64]` is definitively a Player Controller, NOT the player entity.**

It contains:
-  Team affiliation
-  Input state
-  Camera/look direction
-  NO world position

Position must be accessed via:
1. A different global pointer entirely
2. Following a different chain from this controller
3. Hooking a function that accesses position

---

## 7. Key Ghidra Functions

### FUN_0045849c - Creature Array Access

**Address**: `0x0045849c`

**Purpose**: Iterates through creature array (4 slots)

**Key Pattern**:
```c
piVar4 = (int *)(DAT_00cd7f20 + 100);  // 100 = 0x64
// Loops 4 times through creature slots
```

**What We Learned**: This gave us the creature array location

---

### FUN_007cd0c1 - Position Access Pattern

**Address**: `0x007cd0c1`

**Critical Code**:
```c
uVar4 = *(uint *)(in_EAX + 0x10);
fVar1 = (float)piVar3[4] - *(float *)(*(int *)(uVar4 + 0x124) + 0x40);  // X
fVar2 = (float)piVar3[6] - *(float *)(*(int *)(uVar4 + 0x124) + 0x48);  // Z
```

**Pattern Decoded**:
```
in_EAX = some entity object
[in_EAX + 0x10] = inner object
[[in_EAX + 0x10] + 0x124] = transform pointer
[transform + 0x40] = X position
[transform + 0x48] = Z position
```

**Why It Failed For Us**: `in_EAX` is NOT our controller. It's a different object type obtained from `FUN_0079ce88()`.

---

### FUN_0079ce88 - Entity Lookup

**Address**: `0x0079ce88`

**Key Pattern**:
```c
if ((in_EAX != 0) && (*(int *)(in_EAX + 0x18c) != 0)) {
    return *(undefined4 *)(*(int *)(in_EAX + 0x18c) + 0xc);
}
```

**Pattern Decoded**:
```
in_EAX = context object
[in_EAX + 0x18c] = some controller
[[in_EAX + 0x18c] + 0xc] = actual entity
```

**Problem**: We don't know what `in_EAX` (context) is or how to get it globally

---

### Key Relationship

```
FUN_0079ce88 returns → entity
FUN_007cd0c1 uses → [entity + 0x10] → [+0x124] → position

We have: controller at [[DAT_00cd7f20] + 0x64]
We need: entity that FUN_0079ce88 returns
```

The controller and entity are DIFFERENT objects!


---

## 8. Current Working State

### Overlay Displays (Confirmed Working)

1. **Frame Counter**: Updates every frame (proves hook is working)
2. **Creature Pointer**: Shows hex address of controller
3. **Team ID**: Color-coded display
   - Cyan = Neutral (0)
   - Green = Good/Fellowship (1)
   - Red = Evil/Sauron (2)
4. **Input Vectors** (+A0/A4/A8): Shows movement input
5. **Velocity/Look** (+C0/C4/C8): Shows look target
6. **Camera Direction** (+D0/D4/D8): Shows camera angles

### Current Code State

The overlay code in `dllmain.cpp` renders all confirmed working data. It does NOT attempt to display position (since we haven't found it).

---

## 9. Outstanding Problems

### Primary Problem: World Position Not Found

**Status**:  UNSOLVED

**What We Know**:
- Position is NOT in the controller structure at `[[DAT_00cd7f20] + 0x64]`
- Position access pattern exists in `FUN_007cd0c1`: `[[entity + 0x10] + 0x124] + 0x40/0x48`
- The entity object is obtained via `FUN_0079ce88` which requires an unknown context
- Controller and Entity are DIFFERENT object types

**What We Don't Know**:
- How to get the entity object globally
- What global pointer leads to the entity (not controller)
- Whether there's a simpler path to position

---

## 10. Next Steps & Strategy

### Recommended Approaches (In Priority Order)

#### 1. Find the Entity Global
Search Ghidra for other globals that might contain the entity pointer:
- Look for functions that call `FUN_0079ce88` and trace where they get the context
- Search for other `DAT_` globals that contain heap pointers
- Look for "player" or "local" related function names

#### 2. Hook FUN_007cd0c1 Directly
Instead of reading memory, hook the function that accesses position:
- Hook at `0x007cd0c1`
- Capture `in_EAX` when the function is called
- This gives us the entity pointer directly

#### 3. Search for Camera Position
Game cameras often store world position:
- Look for camera-related globals
- Search strings for "Camera", "Cam", "View"
- Camera position might be easier to find than player position

#### 4. Memory Scanning Approach
Use a memory scanner (Cheat Engine style):
- Search for float values that match known position
- Move player to known coordinates
- Find addresses that contain those values

### What NOT To Try Again

 **Do not retry these - they are confirmed failures:**
- Direct offsets on controller (+0x40, +0x50, etc.)
- `[controller + 0x124]` - returns 1, not a pointer
- `[controller + 0x10]` - returns 0x218C, not a heap pointer
- `DAT_00cd8038` context chain - pointers don't lead to position
- Deep scanning controller 0x200-0x5FC - no position values

---

## 11. Onboarding Prompt for Future Agents

Copy and paste this prompt to quickly onboard a new AI agent:

---

**ONBOARDING PROMPT START**

You are continuing reverse engineering work on LOTR Conquest (ConquestLLC.exe) debug overlay.

**Project Status:**
- D3D9 EndScene hook:  WORKING
- Team ID display:  WORKING (offset +0x1CA0)
- Player Position:  NOT FOUND

**Critical Knowledge:**

1. The structure at `[[DAT_00cd7f20] + 0x64]` is a **Player Controller**, NOT the player entity
2. Team ID works at `controller + 0x1CA0` (byte: 0/1/2)
3. Position uses pattern `[[entity + 0x10] + 0x124] + 0x40/48` but we don't have the entity pointer
4. `FUN_0079ce88` returns the entity, but needs an unknown context object
5. All direct offset attempts on controller have failed

**DO NOT TRY:**
- `[controller + 0x124]` - returns 1
- `[controller + 0x10]` - returns 0x218C
- `DAT_00cd8038` chain - breaks

**RECOMMENDED NEXT STEPS:**
1. Find a different global that points to the entity (not controller)
2. Hook `FUN_007cd0c1` to capture the entity pointer
3. Search for camera position globals
4. Read `Conquest/analysis/LOTR_CONQUEST_REVERSE_ENGINEERING_KNOWLEDGE_BASE.md` for full details

**Build Command:**
```bash
cd C:\Users\Yusuf\Desktop\Code\ConquestConsole\DebugOverlay\build
msbuild DebugOverlay.sln /p:Configuration=Release
```

**ONBOARDING PROMPT END**

---

## Appendix: File Locations

| Path | Description |
|------|-------------|
| `DebugOverlay/src/dllmain.cpp` | Main overlay code |
| `DebugOverlay/src/gamedata.cpp` | Memory reading functions |
| `Conquest/decompiled/*.c` | Ghidra decompiled functions |
| `Conquest/analysis/*.md` | Analysis documentation |

---

*End of Knowledge Base*
