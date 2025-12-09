# LOTR Conquest Reverse Engineering - Complete Knowledge Base

> **Purpose**: Comprehensive historical record of all reverse engineering work on LOTR Conquest debug overlay
> **Last Updated**: December 2025
> **Project Status**: D3D9 hook  | Team ID  | Position  NOT FOUND | Audio Hook  | Asset Browser 

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technical Environment](#2-technical-environment)
3. [Complete Chronological History](#3-complete-chronological-history)
4. [Global Memory Addresses](#4-global-memory-addresses)
5. [Player Controller Structure Map](#5-player-controller-structure-map)
6. [All Pointer Chains Attempted](#6-all-pointer-chains-attempted)
7. [Failed Position Approaches](#7-failed-position-approaches)
8. [Ghidra Function Analysis](#8-ghidra-function-analysis)
9. [D3D9 Hook Implementation](#9-d3d9-hook-implementation)
10. [Current Working State](#10-current-working-state)
11. [Key Insights & Discoveries](#11-key-insights--discoveries)
12. [Unsolved Problems](#12-unsolved-problems)
13. [Next Steps Strategy](#13-next-steps-strategy)
14. [Onboarding Section](#14-onboarding-section)
15. [Wwise Audio System Architecture](#15-wwise-audio-system-architecture)
16. [Audio Hook Implementation](#16-audio-hook-implementation)
17. [Bank Hash Mappings](#17-bank-hash-mappings)
18. [Event Correlation Database](#18-event-correlation-database)
19. [BNK File Structure & Parsing](#19-bnk-file-structure--parsing)
20. [FNV Hash Reversal](#20-fnv-hash-reversal)

---

## 1. Project Overview

### What We're Building

A **DirectX 9 debug overlay DLL** that hooks into `ConquestLLC.exe` (Lord of the Rings: Conquest) to display real-time game state information on screen.

### Ultimate Goals

| Goal | Status | Notes |
|------|--------|-------|
| D3D9 EndScene Hook |  WORKING | Renders overlay every frame |
| Team ID Display |  WORKING | Offset +0x1CA0 confirmed |
| Input Vector Display |  WORKING | Offsets +0xA0/A4/A8 |
| Camera Direction |  WORKING | Offsets +0xD0/D4/D8 |
| **Player World Position** |  NOT FOUND | Critical - see Section 7 |
| Health/Mana Display |  NOT ATTEMPTED | Blocked by position issue |
| Entity Enumeration |  PARTIAL | Only 4 controller slots |

### Architecture Diagram

```
┌──────────────────────────────────────────────────────────┐
│                    ConquestLLC.exe                        │
│  ┌─────────────────────────────────────────────────────┐ │
│  │                  Game Engine                         │ │
│  │  ┌───────────────┐  ┌────────────────────────────┐  │ │
│  │  │ D3D9 Device   │  │ Game Memory                │  │ │
│  │  │               │  │ ┌────────────────────────┐ │  │ │
│  │  │ EndScene()────┼──┼─│ DAT_00cd7f20 + 0x64    │ │  │ │
│  │  │      │        │  │ │ (Controller Array)     │ │  │ │
│  │  │      ▼        │  │ │ ┌──────────────────┐   │ │  │ │
│  │  │ ┌─────────┐   │  │ │ │ Controller[0]    │   │ │  │ │
│  │  │ │ HOOK    │   │  │ │ │ +0x1CA0 Team ID  │   │ │  │ │
│  │  │ │ (ours)  │   │  │ │ │ +0xA0-A8 Input   │   │ │  │ │
│  │  │ └─────────┘   │  │ │ │ +0xC0-C8 Velocity│   │ │  │ │
│  │  └───────────────┘  │ │ │ +0xD0-D8 Camera  │   │ │  │ │
│  └─────────────────────┼─┼─│ +??? POSITION ?? │───┼─┼──┼─┤
│                        │ │ └──────────────────┘   │ │  │ │
│  ┌─────────────────────┼─┴────────────────────────┘ │  │ │
│  │    d3d9.dll (our injected DLL)                   │  │ │
│  │    ├── MinHook for vtable patching               │  │ │
│  │    ├── ID3DXFont for text rendering              │  │ │
│  │    └── SEH for safe memory reads                 │  │ │
│  └──────────────────────────────────────────────────┘  │ │
└──────────────────────────────────────────────────────────┘
```

### Key Files

| File | Purpose | Lines |
|------|---------|-------|
| `DebugOverlay/src/dllmain.cpp` | Main DLL, EndScene hook, overlay rendering | ~678 |
| `DebugOverlay/src/gamedata.cpp` | Game memory reading, address constants | ~320 |
| `DebugOverlay/src/gamedata.h` | Data structures, function declarations | ~80 |
| `DebugOverlay/src/d3d9hook.cpp` | D3D9 vtable hooking implementation | ~200 |
| `DebugOverlay/src/debugrenderer.cpp` | Text and primitive rendering | ~300 |
| `Conquest/decompiled/*.c` | Ghidra decompiled functions | ~1000+ files |

---

## 2. Technical Environment

### Game Binary Specifications

| Property | Value |
|----------|-------|
| Executable | `ConquestLLC.exe` |
| Architecture | 32-bit x86 (PE32) |
| Base Address | `0x00400000` (fixed, no ASLR) |
| Calling Convention | `thiscall` (ECX = this), some `fastcall` |
| DirectX Version | DirectX 9.0c |
| Compiler | MSVC (Visual Studio) |
| Engine | Pandemic Studios proprietary engine |

### Memory Layout

```
0x00400000 - 0x00FFFFFF  : Game code (.text, .rdata, .data)
0x04000000 - 0x10000000  : Heap allocations (valid pointer range)
0x00CD0000 - 0x00CE0000  : Global data segment (DAT_ variables)
```

### Build Environment

```bash
# Build the overlay DLL
cd C:\Users\Yusuf\Desktop\Code\ConquestConsole\DebugOverlay\build
msbuild DebugOverlay.sln /p:Configuration=Release

# Output: DebugOverlay\build\Release\d3d9.dll
# Copy to game directory to inject
```

### DLL Injection Method

The compiled `d3d9.dll` is placed in the game's root directory. The game loads it as a proxy for the real DirectX 9 DLL. Our DLL forwards all calls to the real `d3d9.dll` while intercepting `EndScene` for overlay rendering.

---

## 3. Complete Chronological History

### Phase 1: D3D9 Hook Implementation  SUCCESS

**Goal**: Hook DirectX 9 EndScene to render overlay text each frame

**Implementation Steps**:

1. Created proxy DLL that loads the real `d3d9.dll` from system32
2. Implemented `IDirect3D9` and `IDirect3DDevice9` wrapper classes
3. Hooked `CreateDevice` to get device pointer
4. Hooked `EndScene` via vtable patching (index 42 in vtable)
5. Used MinHook library for trampoline-based hooking
6. Created `ID3DXFont` for text rendering with `D3DXCreateFontA`

**Key Code** (from `d3d9hook.cpp`):

```cpp
// Vtable hook for EndScene - index 42
typedef HRESULT (WINAPI *EndScene_t)(IDirect3DDevice9*);
EndScene_t g_OriginalEndScene = nullptr;

HRESULT WINAPI Hooked_EndScene(IDirect3DDevice9* device) {
    // Call our render callback
    if (g_RenderCallback) {
        g_RenderCallback(device);
    }
    // Call original
    return g_OriginalEndScene(device);
}
```

**Verification**: Frame counter increments every frame, proving hook executes consistently.

**Result**:  Overlay renders correctly, stable at 60+ FPS

---

### Phase 2: Finding Player Data Structure

**Goal**: Locate the player's data structure in memory

**Initial Hypothesis**: Player data at `DAT_00cd8048 + 0x162c`

**Discovery Process**:

| Step | What We Tried | Result |
|------|---------------|--------|
| 1 | Read `DAT_00cd8048 + 0x162c` | Found static player profile (name, settings) - NOT live data |
| 2 | Analyzed `FUN_0045849c` in Ghidra | Found creature array access pattern |
| 3 | Located `DAT_00cd7f20 + 0x64` | **Found creature/controller array!** |
| 4 | Tested all 4 array slots | Only index 0 non-zero for local player |

**Key Discovery** from `FUN_0045849c`:

```c
piVar4 = (int *)(DAT_00cd7f20 + 100);  // 100 = 0x64
do {
    if (iVar3 != 0) goto LAB_004584dc;
    iVar3 = *piVar4;
    iVar5 = iVar5 + 1;
    piVar4 = piVar4 + 1;
} while (iVar5 < 4);
```

**Result**:  Found player controller pointer at `[[DAT_00cd7f20] + 0x64]`

---

### Phase 3: Team ID Discovery  SUCCESS

**Goal**: Find the team/faction ID for the player

**Hypothesis**: Team ID stored as a byte somewhere in controller structure

**Testing Method**:

1. Joined Good team, scanned for bytes = 1
2. Joined Evil team, scanned for bytes = 2
3. Cross-referenced to find offset that changed

**Discovery**: Offset `+0x1CA0` contains Team ID as a BYTE

| Value | Team |
|-------|------|
| 0 | Neutral |
| 1 | Good (Fellowship) |
| 2 | Evil (Sauron's Army) |

**Verification Code**:

```cpp
BYTE teamId = SafeReadBYTE(controller + 0x1CA0);
const char* teamNames[] = {"Neutral", "Good", "Evil"};
// Correctly displays current team
```

**Result**:  Team ID confirmed working and color-coded in overlay

---

### Phase 4: Position Data Search  ONGOING - NOT FOUND

**Goal**: Find player world position (X, Y, Z coordinates)

**Duration**: Multiple hours of testing across 9+ different approaches

**Summary of All Attempts**: See [Section 7: Failed Position Approaches](#7-failed-position-approaches)

**Critical Insight**: The structure at `[[DAT_00cd7f20] + 0x64]` is a **Player Controller** (input state, team affiliation), NOT the actual game entity with world position.

**Result**:  Position NOT found - requires different approach

---

### Phase 5: Overlay Display Debugging ("Blinking Overlay" Issue)

**Problem**: Initial offset scanner was unreadable - updating too fast

**What Happened**:

1. Created overlay to scan all offsets in controller structure
2. Displayed all floats that changed when moving
3. Display was "blinking at light speed" - unusable

**Solution Iterations**:

| Attempt | Approach | Result |
|---------|----------|--------|
| 1 | Show all changing values | Too fast, unreadable |
| 2 | Snapshot every 60 frames | Values kept resetting |
| 3 | Track min/max range permanently |  Better - but showed direction vectors only |
| 4 | Filter by delta > 1.0 |  Filtered out noise |
| 5 | Deep scan 0x200-0x5FC | No position-like values found |

**Final State**: Reverted to clean overlay showing confirmed working data

---

## 4. Global Memory Addresses

### All DAT_ Globals Discovered

| Address | Name | Purpose | Status | Notes |
|---------|------|---------|--------|-------|
| `0x00cd7f20` | DAT_00cd7f20 | Base for creature/controller array |  WORKING | Primary player data source |
| `0x00cd8038` | DAT_00cd8038 | Context object pointer |  TESTED, FAILED | Chain breaks at +0x18c |
| `0x00cd8048` | DAT_00cd8048 | Player manager |  STATIC ONLY | Contains player profile, not live data |
| `0x00cd7fdc` | DAT_00cd7fdc | Game state pointer |  NOT TESTED | Potentially useful |
| `0x00cd88f0` | DAT_00cd88f0 | Frame time float |  OBSERVED | Used in FUN_0045849c |
| `0x00cd8240` | DAT_00cd8240 | Unknown vector |  NOT TESTED | Copied to arrays in FUN_00789849 |
| `0x00cfcc40` | DAT_00cfcc40 | Entity table (64 entries) |  NOT TESTED | Used in FUN_0079adca |

### Access Pattern for Player Controller

```c
// Step-by-step pointer chain
DWORD globalBase = *(DWORD*)0x00cd7f20;        // Read global: ~0x0562C000
DWORD creatureArray = globalBase + 0x64;       // Array start
DWORD controller = *(DWORD*)(creatureArray);   // First slot (player)
// controller now points to Player Controller structure (~0x0562D000)
```

### Pointer Validation

All heap pointers should be in range `0x04000000 - 0x10000000`. Values outside this range are likely not pointers.

---

## 5. Player Controller Structure Map

**Base Address**: `[[0x00cd7f20] + 0x64]`

**Structure Size**: At least 0x1CA4 bytes (7332+ bytes)

**CRITICAL**: This is a **Player Controller**, NOT the player entity. It contains input state and team affiliation but NOT world position.

###  Confirmed Working Offsets

| Offset | Type | Name | Values | Behavior | Status |
|--------|------|------|--------|----------|--------|
| **+0x1CA0** | BYTE | TeamID | 0, 1, 2 | 0=Neutral, 1=Good, 2=Evil |  CONFIRMED |
| **+0x1C90** | DWORD* | StatePointer | Pointer | Points to state struct |  CONFIRMED |

###  Input/Movement Vectors (Working)

| Offset | Type | Name | Range | Behavior |
|--------|------|------|-------|----------|
| +0xA0 | float | InputLeftRight | -1.0 to +1.0 | A key = -1, D key = +1, None = 0 |
| +0xA4 | float | InputForwardBack | -1.0 to +1.0 | S key = -1, W key = +1 |
| +0xA8 | float | InputStrafe | -1.0 to +1.0 | Strafing direction vector |

###  Camera/Look Direction (Changes on mouse movement)

| Offset | Type | Name | Behavior |
|--------|------|------|----------|
| +0xC0 | float | LookTargetX | Changes on walk AND mouse movement |
| +0xC4 | float | LookTargetY | Changes on jump, vertical aim |
| +0xC8 | float | LookTargetZ | Changes on walk AND mouse movement |
| +0xD0 | float | CameraX | Changes ONLY on mouse movement |
| +0xD4 | float | CameraY | Vertical look angle (pitch) |
| +0xD8 | float | CameraZ | Changes ONLY on mouse movement |
| +0xE0 | float | AnimBreathing | Oscillates with character breathing animation |

###  Tested But NOT Position Data

**Why +0xC0/C4/C8 are NOT position**:

1. They change when moving the mouse (pure position wouldn't)
2. They show discrete jumps, not smooth world coordinates
3. They are likely look-at targets or velocity vectors
4. Values are small (-5 to +5 range), not world scale (hundreds)

### Pointer Fields Observed

| Offset | Value Observed | Dereference Result | Notes |
|--------|----------------|-------------------|-------|
| +0x10 | 0x0000218C | Invalid (too low) | NOT a pointer |
| +0x80 | ~0x05600000 | Points to another struct | Untested chain |
| +0x84 | ~0x05600000 | Points to another struct | Untested chain |
| +0x124 | 0x00000001 | Literal value 1 | NOT a pointer |
| +0x130 | ~0x05600000 | Static values when dereferenced | No position |

---

## 6. All Pointer Chains Attempted

### Chain 1: Direct Controller Offsets 

```
Controller + 0x40/44/48  → Static values (0.0, 0.0, 0.0)
Controller + 0x50/54/58  → Static values
Controller + 0x60/64/68  → Static values
```

**Result**: No position data at direct offsets

---

### Chain 2: Via +0x124 (from FUN_007cd0c1 pattern) 

**Hypothesis**: `[controller + 0x10] → [+0x124] → +0x40/44/48`

```cpp
DWORD step1 = *(DWORD*)(controller + 0x10);   // Returns 0x218C (not a pointer!)
// Chain breaks immediately
```

**Result**: +0x10 contains literal value 0x218C, not a pointer

---

### Chain 3: Via +0x080 

**Hypothesis**: `[controller + 0x080] → [+0x10] → [+0x124] → +0x40/44/48`

```cpp
DWORD ptr1 = *(DWORD*)(controller + 0x080);   // Valid pointer ~0x056xxxxx
DWORD ptr2 = *(DWORD*)(ptr1 + 0x10);          // Returns small value, not pointer
// Chain breaks at step 2
```

**Result**: Chain breaks at second dereference

---

### Chain 4: Via +0x084 

**Hypothesis**: `[controller + 0x084] → [+0x10] → [+0x124] → +0x40/44/48`

**Result**: Same as Chain 3 - breaks at second step

---

### Chain 5: Via +0x130 

**Hypothesis**: `[controller + 0x130] → +0x40/44/48`

```cpp
DWORD ptr = *(DWORD*)(controller + 0x130);    // Valid pointer
float x = *(float*)(ptr + 0x40);              // Static value, doesn't change on walk
```

**Result**: Values don't update when walking

---

### Chain 6: DAT_00cd8038 Context Chain 

**Hypothesis** (from FUN_0079ce88):
```
[DAT_00cd8038] → [+0x18c] → [+0xc] → entity with position
```

**Code Tested**:

```cpp
DWORD context = *(DWORD*)0x00cd8038;          // Returns 0 or invalid
DWORD controller = *(DWORD*)(context + 0x18c); // Crashes or returns 0
// Chain never starts - context is null
```

**Result**: DAT_00cd8038 is null or invalid during gameplay

---

### Chain 7: Deep Scan 0x200-0x5FC 

**Hypothesis**: Position might be at a deeper offset

**Method**: Scanned all floats from +0x200 to +0x5FC, tracked min/max range

**Result**: No values with range > 10 found (position would have range 100+)

---

## 7. Failed Position Approaches

### Approach 1: Direct Offsets +0x40/44/48 

**Hypothesis**: Standard position offset pattern from other games

**Code Tested**:

```cpp
float posX = SafeReadFLOAT(controller + 0x40);
float posY = SafeReadFLOAT(controller + 0x44);
float posZ = SafeReadFLOAT(controller + 0x48);
```

**Expected**: Values change smoothly when walking around map

**Actual**: All values static (0.0, 0.0, 0.0)

**Root Cause**: Controller structure doesn't contain position at these offsets

**Lesson**: Don't assume standard offset patterns

---

### Approach 2: Transform Pointer at +0x124 

**Hypothesis**: Based on FUN_007cd0c1 pattern: `[entity+0x10] → [+0x124] → +0x40/44/48`

**Code Tested**:

```cpp
DWORD transform = SafeReadDWORD(controller + 0x124);
// transform = 1 (literal value, not a pointer)
```

**Expected**: Valid pointer to transform matrix

**Actual**: Value is literal `1`, not a pointer

**Root Cause**: Controller is not the same structure type as entity in FUN_007cd0c1

**Lesson**: Controller ≠ Entity - they are different object types

---

### Approach 3: Nested +0x10 Chain 

**Hypothesis**: Follow the exact pattern from decompiled code

**Code Tested**:

```cpp
DWORD step1 = SafeReadDWORD(controller + 0x10);  // = 0x218C
// 0x218C is not a valid heap pointer (too low)
```

**Expected**: Valid pointer chain

**Actual**: +0x10 contains 0x218C which is not a pointer

**Root Cause**: Different structure layout than expected

**Lesson**: Verify each pointer in chain before following

---

### Approach 4: Via +0x080 Pointer 

**Hypothesis**: +0x080 might point to entity with position

**Code Tested**:

```cpp
DWORD ptr1 = SafeReadDWORD(controller + 0x080);  // Valid ~0x056xxxxx
DWORD ptr2 = SafeReadDWORD(ptr1 + 0x10);         // Invalid
```

**Expected**: Chain leads to position

**Actual**: Chain breaks at second dereference

**Root Cause**: +0x080 points to different structure type

---

### Approach 5: Via +0x084 Pointer 

**Hypothesis**: Alternative pointer field

**Result**: Same failure as Approach 4

---

### Approach 6: +0x130 Direct Read 

**Hypothesis**: +0x130 might contain position directly or via pointer

**Code Tested**:

```cpp
DWORD ptr = SafeReadDWORD(controller + 0x130);
float x = SafeReadFLOAT(ptr + 0x40);
float y = SafeReadFLOAT(ptr + 0x44);
float z = SafeReadFLOAT(ptr + 0x48);
```

**Expected**: Position values that update on walk

**Actual**: Static values that never change

**Root Cause**: This pointer leads to static data, not live position

---

### Approach 7: DAT_00cd8038 Context 

**Hypothesis**: Use FUN_0079ce88 pattern to get entity from context

**Code Tested**:

```cpp
DWORD context = SafeReadDWORD(0x00cd8038);
if (context != 0) {
    DWORD ctrl = SafeReadDWORD(context + 0x18c);
    DWORD entity = SafeReadDWORD(ctrl + 0xc);
    // Read position from entity
}
```

**Expected**: Valid context pointer during gameplay

**Actual**: Context is 0 or invalid

**Root Cause**: DAT_00cd8038 may only be valid in specific game states

---

### Approach 8: Deep Offset Scan (0x200-0x5FC) 

**Hypothesis**: Position might be at unusual deep offset

**Method**:

1. Scan all floats from +0x200 to +0x5FC
2. Track min/max values over time
3. Look for values with range > 10 (position would have large range)

**Code Tested**:

```cpp
for (int offset = 0x200; offset <= 0x5FC; offset += 4) {
    float val = SafeReadFLOAT(controller + offset);
    // Track min/max, display if range > 10
}
```

**Expected**: Find 2-3 offsets with large value ranges (X, Y, Z)

**Actual**: No offsets showed range > 10

**Root Cause**: Position simply not stored in this structure

---

### Approach 9: +0xC0/C4/C8 Analysis 

**Hypothesis**: These changing values might be position

**Observation**:

- Values DO change when walking
- BUT values ALSO change when moving mouse (looking around)
- Values are small range (-5 to +5), not world scale

**Conclusion**: These are velocity/look-target vectors, NOT position

**Root Cause**: Conflated movement vectors with position

---

## 8. Ghidra Function Analysis

### FUN_0045849c - Creature Array Access

**Purpose**: Iterates through 4 creature/controller slots

**Key Code**:

```c
void FUN_0045849c(void) {
    int iVar3 = 0;
    int iVar5 = 0;
    int *piVar4 = (int *)(DAT_00cd7f20 + 100);  // 100 = 0x64

    do {
        if (iVar3 != 0) goto LAB_004584dc;
        iVar3 = *piVar4;           // Read controller pointer
        iVar5 = iVar5 + 1;
        piVar4 = piVar4 + 1;       // Next slot
    } while (iVar5 < 4);           // 4 slots total

    // ... uses iVar3 as controller pointer
}
```

**Key Insights**:

1. `DAT_00cd7f20 + 0x64` is the creature array base
2. Array has 4 slots (one per faction/player slot)
3. First non-null slot is used as active controller
4. This is how we found the player controller pointer

---

### FUN_007cd0c1 - Position Access Pattern

**Purpose**: Calculates distance/direction to target

**Key Code**:

```c
void FUN_007cd0c1(int *piVar3) {
    uint uVar4 = *(uint *)(in_EAX + 0x10);

    // Position access pattern:
    float fVar1 = (float)piVar3[4] - *(float *)(*(int *)(uVar4 + 0x124) + 0x40);  // X
    float fVar2 = (float)piVar3[6] - *(float *)(*(int *)(uVar4 + 0x124) + 0x48);  // Z

    // fVar1 and fVar2 are delta X and delta Z
}
```

**Key Insights**:

1. Position accessed via: `[entity + 0x10] → [+0x124] → +0x40/44/48`
2. `in_EAX` is the entity (passed via register, thiscall convention)
3. This entity is NOT the same as our controller
4. We need to find how to get this entity pointer

**Critical Realization**: The `in_EAX` entity is obtained from somewhere else - likely FUN_0079ce88

---

### FUN_0079ce88 - Entity Lookup

**Purpose**: Gets entity pointer from context

**Key Code**:

```c
undefined4 FUN_0079ce88(void) {
    if ((in_EAX != 0) && (*(int *)(in_EAX + 0x18c) != 0)) {
        return *(undefined4 *)(*(int *)(in_EAX + 0x18c) + 0xc);
    }
    return 0;
}
```

**Key Insights**:

1. `in_EAX` is a context object (NOT DAT_00cd7f20)
2. Pattern: `[context + 0x18c] → [+0xc]` = entity
3. We tried DAT_00cd8038 as context but it was null
4. The correct context source is still unknown

**This is the missing link**: We need to find what provides the context to this function

---

### FUN_0079adca - Entity Table Access

**Purpose**: Accesses entity table with 64 entries

**Key Code**:

```c
void FUN_0079adca(void) {
    // Uses DAT_00cfcc40 as entity table base
    // Table has 64 entries
    // Each entry is an entity pointer
}
```

**Potential**: DAT_00cfcc40 might be an alternative way to find entities

**Status**:  NOT TESTED

---

## 9. D3D9 Hook Implementation

### Hook Architecture

```
Game calls EndScene()
        │
        ▼
┌───────────────────────┐
│ Hooked_EndScene()     │
│ ├── Save D3D state    │
│ ├── Read game memory  │
│ ├── Render overlay    │
│ ├── Restore D3D state │
│ └── Call original     │
└───────────────────────┘
        │
        ▼
Original EndScene() executes
```

### Safe Memory Reading

All memory reads use SEH (Structured Exception Handling) to prevent crashes:

```cpp
DWORD SafeReadDWORD(DWORD address) {
    __try {
        return *(DWORD*)address;
    }
    __except(EXCEPTION_EXECUTE_HANDLER) {
        return 0;
    }
}

BYTE SafeReadBYTE(DWORD address) {
    __try {
        return *(BYTE*)address;
    }
    __except(EXCEPTION_EXECUTE_HANDLER) {
        return 0;
    }
}

float SafeReadFLOAT(DWORD address) {
    __try {
        return *(float*)address;
    }
    __except(EXCEPTION_EXECUTE_HANDLER) {
        return 0.0f;
    }
}
```

### Pointer Validation

```cpp
bool IsValidPointer(DWORD ptr) {
    return (ptr >= 0x04000000 && ptr <= 0x10000000);
}
```

### Text Rendering

```cpp
void RenderText(ID3DXFont* font, int x, int y, DWORD color, const char* text) {
    RECT rect = { x, y, x + 500, y + 20 };
    font->DrawTextA(NULL, text, -1, &rect, DT_LEFT | DT_NOCLIP, color);
}
```

---

## 10. Current Working State

### What the Overlay Displays Now

```
┌─────────────────────────────────────────┐
│ LOTR Conquest Debug Overlay             │
│ Frame: 12345                            │
│ Creature: 0x0562D000                    │
│ TEAM: Good (1)           [GREEN]        │
│ Input: X=-1.00 Y=0.00 Z=0.00            │
│ Vel:   X=2.34  Y=0.00 Z=-1.56           │
│ Look:  X=0.87  Y=0.12 Z=-0.48           │
└─────────────────────────────────────────┘
```

### Working Features

| Feature | Status | Implementation |
|---------|--------|----------------|
| Frame counter |  | Increments each EndScene call |
| Creature pointer |  | `[[0x00cd7f20] + 0x64]` |
| Team ID |  | `[creature + 0x1CA0]` as BYTE |
| Team color coding |  | Green=Good, Red=Evil, White=Neutral |
| Input vectors |  | `[creature + 0xA0/A4/A8]` |
| Velocity display |  | `[creature + 0xC0/C4/C8]` |
| Camera direction |  | `[creature + 0xD0/D4/D8]` |

### Not Working

| Feature | Status | Reason |
|---------|--------|--------|
| World Position |  | Not found in controller structure |
| Health/Mana |  | Not attempted (blocked by position) |
| Other entities |  | Only local player controller found |

---

## 11. Key Insights & Discoveries

### Critical Insight #1: Controller ≠ Entity

**The Most Important Discovery**:

The structure at `[[DAT_00cd7f20] + 0x64]` is a **Player Controller**, NOT the player entity.

| Controller (what we have) | Entity (what we need) |
|---------------------------|----------------------|
| Input state (WASD keys) | World position |
| Team affiliation | Health/Mana |
| Camera direction | Animation state |
| Movement vectors | Collision data |

**Implication**: Position requires finding the Entity object, which is accessed via a different code path (FUN_0079ce88).

---

### Critical Insight #2: thiscall Convention

Many game functions use `thiscall` convention where:

- `ECX` or `EAX` contains the "this" pointer (context/object)
- Parameters are pushed on stack
- Return value in `EAX`

This means we can't just call these functions - we need to find where the context comes from.

---

### Critical Insight #3: Pointer Chain Validation

Always validate each step in a pointer chain:

```cpp
// WRONG - crashes if any step is invalid
float pos = *(float*)(*(DWORD*)(*(DWORD*)(base + 0x10) + 0x124) + 0x40);

// RIGHT - validate each step
DWORD step1 = SafeReadDWORD(base + 0x10);
if (!IsValidPointer(step1)) return;
DWORD step2 = SafeReadDWORD(step1 + 0x124);
if (!IsValidPointer(step2)) return;
float pos = SafeReadFLOAT(step2 + 0x40);
```

---

### Critical Insight #4: Value Range Analysis

Position values should have:

- Large range (100+ units across a map)
- Smooth continuous changes when walking
- NO change when just moving mouse

Velocity/direction values have:

- Small range (-1 to +1, or -5 to +5)
- Change on mouse movement
- Discrete jumps, not smooth

---

## 12. Unsolved Problems

### Problem 1: Player World Position 

**Status**: NOT FOUND after 9+ approaches

**What We Know**:

1. Position is NOT in the controller structure at `[[DAT_00cd7f20] + 0x64]`
2. Position IS accessed via pattern: `[entity + 0x10] → [+0x124] → +0x40/44/48`
3. Entity is obtained from FUN_0079ce88: `[context + 0x18c] → [+0xc]`
4. The context source is unknown

**What We Don't Know**:

1. Where does the context for FUN_0079ce88 come from?
2. Is there a global that holds the current entity?
3. Does the controller have a pointer to its entity?

---

### Problem 2: Entity Enumeration

**Status**: Only local player controller found

**What We Know**:

1. `DAT_00cd7f20 + 0x64` has 4 slots
2. Only slot 0 is non-null for local player
3. Other players/NPCs must be stored elsewhere

**Potential Leads**:

1. `DAT_00cfcc40` - Entity table with 64 entries (from FUN_0079adca)
2. Iterate through all 4 controller slots during multiplayer

---

## 13. Next Steps Strategy

### Priority 1: Hook a Position-Accessing Function

**Recommended Approach**: Hook FUN_007cd0c1 or similar function that accesses position

**Why**: The function receives the entity pointer in `EAX` - we can capture it

**Implementation**:

```cpp
// Hook at 0x007cd0c1
typedef void (__fastcall *FUN_007cd0c1_t)(void* entity, void* edx, int* param);
FUN_007cd0c1_t Original_007cd0c1 = nullptr;

void __fastcall Hooked_007cd0c1(void* entity, void* edx, int* param) {
    // Capture entity pointer!
    g_CapturedEntity = (DWORD)entity;

    // Now we can read position:
    // [entity + 0x10] → [+0x124] → +0x40/44/48

    Original_007cd0c1(entity, edx, param);
}
```

---

### Priority 2: Test DAT_00cfcc40 Entity Table

**Approach**: Scan the 64-entry entity table

```cpp
DWORD entityTable = 0x00cfcc40;
for (int i = 0; i < 64; i++) {
    DWORD entity = SafeReadDWORD(entityTable + i * 4);
    if (IsValidPointer(entity)) {
        // Try reading position from this entity
    }
}
```

---

### Priority 3: Find Controller → Entity Link

**Approach**: Scan controller structure for pointers that lead to entity

**Method**:

1. For each pointer field in controller (every 4 bytes)
2. Follow the pointer
3. Check if `[ptr + 0x10] → [+0x124]` is valid
4. If valid, try reading position

---

### What NOT To Retry

| Approach | Why It Failed | Don't Retry |
|----------|---------------|-------------|
| Direct offsets +0x40-0x68 | Controller doesn't have position |  |
| +0x124 as pointer | It's literal value 1 |  |
| +0x10 chain | Returns 0x218C, not pointer |  |
| DAT_00cd8038 context | Null during gameplay |  |
| Deep scan 0x200-0x5FC | No position-like values |  |
| +0xC0/C4/C8 as position | Changes on mouse movement |  |

---

## 14. Onboarding Section

### Quick Start for New AI Agent

Copy and paste this prompt to quickly understand the project:

---

**ONBOARDING PROMPT**:

I'm working on a D3D9 debug overlay for LOTR Conquest (ConquestLLC.exe, 32-bit x86, no ASLR).

**Current State**:
-  D3D9 EndScene hook works
-  Team ID at `[[0x00cd7f20] + 0x64] + 0x1CA0` (BYTE: 0=Neutral, 1=Good, 2=Evil)
-  Input vectors at +0xA0/A4/A8
-  World position NOT FOUND

**Critical Insight**: The structure at `[[0x00cd7f20] + 0x64]` is a **Player Controller** (input/team), NOT the entity with position.

**Position Access Pattern** (from Ghidra):
```
[entity + 0x10] → [+0x124] → +0x40/44/48 = X/Y/Z
```

But we don't have the entity pointer. It comes from FUN_0079ce88:
```
[context + 0x18c] → [+0xc] = entity
```

**Failed Approaches** (don't retry):
1. Direct offsets on controller (+0x40-0x68) - static values
2. +0x124 on controller - returns literal 1, not pointer
3. +0x10 chain - returns 0x218C, not pointer
4. DAT_00cd8038 as context - null during gameplay
5. Deep scan 0x200-0x5FC - no position values
6. +0xC0/C4/C8 - changes on mouse movement, not position

**Recommended Next Steps**:
1. Hook FUN_007cd0c1 (0x007cd0c1) to capture entity pointer from EAX
2. Test DAT_00cfcc40 entity table (64 entries)
3. Scan controller for pointer that leads to entity

**Key Files**:
- `DebugOverlay/src/dllmain.cpp` - overlay rendering
- `DebugOverlay/src/gamedata.cpp` - memory reading
- `Conquest/decompiled/FUN_007cd0c1.c` - position access pattern
- `Conquest/decompiled/FUN_0079ce88.c` - entity lookup

**Build**: `msbuild DebugOverlay.sln /p:Configuration=Release` in `DebugOverlay/build/`

---

### File Locations Reference

| Path | Contents |
|------|----------|
| `DebugOverlay/` | DLL source code |
| `DebugOverlay/build/` | Build output |
| `Conquest/decompiled/` | Ghidra exports |
| `Conquest/analysis/` | Documentation (this file) |
| `wwiseRE/` | Audio reverse engineering |

---

## Appendix A: Memory Address Quick Reference

```
0x00cd7f20 + 0x64  = Creature/Controller array (4 slots)
0x00cd8038         = Context pointer (often null)
0x00cd8048         = Player manager (static data)
0x00cfcc40         = Entity table (64 entries) - UNTESTED

Controller offsets:
+0x1CA0 = Team ID (BYTE)
+0xA0   = Input X (float)
+0xA4   = Input Y (float)
+0xA8   = Input Z (float)
+0xC0   = Velocity X (float)
+0xC4   = Velocity Y (float)
+0xC8   = Velocity Z (float)
+0xD0   = Camera X (float)
+0xD4   = Camera Y (float)
+0xD8   = Camera Z (float)
```

---

## Appendix B: Build Commands

```bash
# Full rebuild
cd C:\Users\Yusuf\Desktop\Code\ConquestConsole\DebugOverlay\build
msbuild DebugOverlay.sln /p:Configuration=Release /t:Rebuild

# Quick build
msbuild DebugOverlay.sln /p:Configuration=Release

# Output location
# DebugOverlay\build\Release\d3d9.dll
```

---

## 15. Wwise Audio System Architecture

### Overview

LOTR Conquest uses **Wwise SDK v34 (2008.1)** for audio, statically linked into `ConquestLLC.exe`. All audio events, soundbanks, and parameters use FNV-1a hashing for name resolution.

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        ConquestLLC.exe (32-bit)                         │
│                                                                         │
│  ┌─────────────────────┐     ┌─────────────────────────────────────┐   │
│  │ Game Logic          │────▶│ Wwise SDK v34 (statically linked)    │   │
│  │ (attack, move, etc) │     │                                     │   │
│  └─────────────────────┘     │  PostEventByID   @ 0x00561360       │   │
│                              │  PostEventByName @ 0x00560f90       │   │
│                              │  GetIDFromString @ 0x00560190       │   │
│                              │  StopAll         @ 0x0055fca0       │   │
│                              │  SetSwitch       @ 0x0055fc10       │   │
│                              │  SetState        @ 0x0055fbc0       │   │
│                              └──────────────┬──────────────────────┘   │
│                                             │                          │
│  ┌──────────────────────────────────────────┴──────────────────────┐   │
│  │                     DebugOverlay.dll (injected)                 │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌────────────────┐   │   │
│  │  │ PostEvent Hook  │  │ GetIDFromString │  │ Asset Browser  │   │   │
│  │  │ (captures all   │  │ Hook (captures  │  │ (F10 panel,    │   │   │
│  │  │  audio events)  │  │  bank names)    │  │  plays events) │   │   │
│  │  └─────────────────┘  └─────────────────┘  └────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### Wwise Function Addresses

| Function | Address | Signature | Purpose |
|----------|---------|-----------|---------|
| `PostEventByID` | `0x00561360` | `DWORD __cdecl (DWORD eventId, DWORD gameObjId, DWORD flags, void* callback, void* cookie)` | Trigger audio event by hash |
| `PostEventByName` | `0x00560f90` | `DWORD __cdecl (const char* name, DWORD gameObjId, ...)` | Trigger event by string name |
| `GetIDFromString` | `0x00560190` | `DWORD __cdecl (const char* name)` | Convert string to FNV-1a hash |
| `StopAll` | `0x0055fca0` | `void __cdecl (DWORD gameObjId)` | Stop all sounds on game object |
| `SetSwitch` | `0x0055fc10` | `void __cdecl (DWORD groupId, DWORD switchId, DWORD gameObjId)` | Set switch value |
| `SetState` | `0x0055fbc0` | `void __cdecl (DWORD groupId, DWORD stateId)` | Set global state |
| `SoundEngineInit` | `0x00560d90` | `DWORD __cdecl (void* settings)` | Initialize Wwise engine |
| `SetPosition` | `0x0055ed30` | `void __cdecl (DWORD gameObjId, float* pos)` | Set 3D sound position |

### FNV-1a Hashing

Wwise uses FNV-1a (32-bit) to hash all string identifiers:

```cpp
DWORD FNV1a_Hash(const char* str) {
    DWORD hash = 2166136261u;  // FNV offset basis
    while (*str) {
        hash ^= (BYTE)tolower(*str++);
        hash *= 16777619u;     // FNV prime
    }
    return hash;
}
```

**Example Hashes**:
| String | FNV-1a Hash |
|--------|-------------|
| `HeroSauron` | `0xAE7FDABB` (2927614651) |
| `swing` | `0x8E3F67AD` |
| `footstep` | `0x????????` (Effects-0717) |

---

## 16. Audio Hook Implementation

### JMP Hook Mechanism

The audio hook uses a 5-byte JMP patch to redirect Wwise function calls:

```
Original bytes at 0x00561360:  55 8B EC 83 E4  (push ebp; mov ebp, esp; ...)
Patched bytes:                 E9 XX XX XX XX  (jmp rel32 to HookedPostEvent)
```

**Hook Installation**:

```cpp
// Calculate relative jump offset
DWORD relativeAddr = (DWORD)HookedPostEvent - (targetAddr + 5);

// Unlock memory for writing
VirtualProtect((LPVOID)targetAddr, 5, PAGE_EXECUTE_READWRITE, &oldProtect);

// Save original bytes (for calling original function)
memcpy(g_OriginalBytes, (void*)targetAddr, 5);

// Write JMP instruction
*(BYTE*)targetAddr = 0xE9;
*(DWORD*)(targetAddr + 1) = relativeAddr;

// Restore protection
VirtualProtect((LPVOID)targetAddr, 5, oldProtect, &oldProtect);
```

### HookedPostEvent Function

```cpp
DWORD __cdecl HookedPostEvent(DWORD eventId, DWORD gameObjId,
                               DWORD flags, void* callback, void* cookie) {
    // Track valid game object IDs for manual playback
    if (gameObjId != 0xFFFFFFFF && gameObjId != 0) {
        g_LastValidGameObjectId = gameObjId;
    }

    // Log the event
    LogAudioEvent(eventId, gameObjId);

    // Call original function (restore bytes, call, re-patch)
    memcpy((void*)POSTEVENT_ADDR, g_OriginalBytes, 5);
    DWORD result = OriginalPostEvent(eventId, gameObjId, flags, callback, cookie);
    *(BYTE*)POSTEVENT_ADDR = 0xE9;
    *(DWORD*)(POSTEVENT_ADDR + 1) = relativeAddr;

    return result;
}
```

### Game Object ID Tracking

**Critical Insight**: Wwise requires a valid registered game object ID for sound playback. Game object ID `0` or `0xFFFFFFFF` will fail silently.

```cpp
static DWORD g_LastValidGameObjectId = 0;

// Captured from game's own audio calls
// Used when manually triggering events via Asset Browser
DWORD GetLastValidGameObjectId() {
    return g_LastValidGameObjectId;
}
```

**Why This Matters**: When the Asset Browser plays an event, it needs a valid game object. The game registers objects internally; we capture valid IDs from the game's own PostEvent calls and reuse them.

### Asset Browser Panel (F10)

**Keyboard Controls**:
| Key | Action |
|-----|--------|
| `F10` | Toggle Asset Browser panel |
| `UP/DOWN` | Navigate event list |
| `PAGE UP/PAGE DOWN` | Jump 12 events |
| `ENTER` | Play selected event |
| `DELETE` | Stop all sounds |

**Implementation** (in `dllmain.cpp`):

```cpp
if (Input::IsKeyPressed(VK_RETURN)) {
    DWORD eventId = g_EventMappingData[g_SelectedEventIndex].id;
    AudioHook::PlayEvent(eventId, 0);  // 0 = use last valid game object
}

if (Input::IsKeyPressed(VK_DELETE)) {
    AudioHook::StopAllSounds(0);  // 0 = stop ALL sounds globally
}
```

### PlayEvent API

```cpp
bool AudioHook::PlayEvent(DWORD eventId, DWORD gameObjectId) {
    // Use captured game object if none specified
    DWORD actualGameObj = gameObjectId;
    if (actualGameObj == 0 && g_LastValidGameObjectId != 0) {
        actualGameObj = g_LastValidGameObjectId;
    }

    // Call Wwise PostEventByID
    typedef DWORD(__cdecl* PostEventByID_t)(DWORD, DWORD, DWORD, void*, void*);
    auto PostEventByID = (PostEventByID_t)0x00561360;

    // Temporarily unhook to call original
    RestoreOriginalBytes();
    DWORD result = PostEventByID(eventId, actualGameObj, 0, nullptr, nullptr);
    ReapplyHook();

    return result != 0;
}
```

---

## 17. Bank Hash Mappings

### Discovered Bank Hashes

All bank names are captured via the `GetIDFromString` hook when the game loads soundbanks.

#### Hero Banks

| Hash | Decimal | Bank Name |
|------|---------|-----------|
| `0xAE7FDABB` | 2927614651 | HeroSauron |
| `0xD210FF9B` | 3524329371 | HeroAragorn |
| `0xB9842D06` | 3112447238 | HeroGandalf |
| `0x8A265726` | 2317768486 | HeroLegolas |
| `0x58F75BAF` | 1492605871 | HeroGimli |
| `0xA7C4543D` | 2814661693 | HeroEowyn |
| `0xF7D0958D` | 4157642125 | HeroElrond |
| `0xB659C16F` | 3059335535 | HeroIsildur |
| `0x714C77C2` | 1900836802 | HeroSaruman |
| `0x5C3485EC` | 1546946028 | HeroLurtz |
| `0x440AF85C` | 1141569628 | HeroNazgul |
| `0x1888157F` | 411571583 | HeroWitchKing |
| `0x338EAF70` | 864817008 | HeroWormtongue |
| `0xDC91750E` | 3700520206 | HeroMouth |
| `0xF8D025AA` | 4174931370 | HeroArwen |

#### Hero Chatter Banks

| Hash | Decimal | Bank Name |
|------|---------|-----------|
| `0x024F9CA2` | 38771874 | ChatterHeroSauron |
| `0x33EFEBB8` | 872803256 | ChatterHeroAragorn |
| `0xE4AA8BF9` | 3836640249 | ChatterHeroGandalf |
| `0xA7AF2725` | 2813462309 | ChatterHeroNazgul |
| `0xA67CA2E4` | 2793603812 | ChatterHeroWitchKing |

#### Faction Chatter Banks

| Hash | Decimal | Bank Name |
|------|---------|-----------|
| `0x566B984F` | 1449892943 | ChatterGondor |
| `0xDD4376FA` | 3712059130 | ChatterOrc |
| `0xBEE19A09` | 3202587145 | ChatterUruk |
| `0x29E14FEA` | 703086570 | ChatterRohan |

#### Level Banks

| Hash | Decimal | Bank Name |
|------|---------|-----------|
| `0xC3C20417` | 3284272151 | Level_BlackGates |
| `0x9DD2F9EB` | 2647849451 | Level_Trng |
| `0x95B242E3` | 2511487715 | Level_Isengard |
| `0x6DBAAE75` | 1840950901 | Level_HelmsDeep |
| `0xB36D8F45` | 3010301765 | Level_MinasMorg |

#### Voice-Over Banks

| Hash | Decimal | Bank Name |
|------|---------|-----------|
| `0xC463B59C` | 3295405468 | VO_BlackGates |
| `0x1629AEF4` | 372100852 | VO_Trng |
| `0xD07A401C` | 3497746460 | VO_MinasMorg |

#### SFX Banks (Siege/Creatures)

| Hash | Decimal | Bank Name |
|------|---------|-----------|
| `0xA6C9C687` | 2798241415 | SFXTroll |
| `0xCDCB4FCD` | 3452653517 | SFXEnt |
| `0x4CFFF4AA` | 1291842730 | SFXEagle |
| `0x632D860A` | 1663927818 | SFXFellBeast |
| `0xA1B662E0` | 2713084640 | SFXBallista |
| `0xDD97A3A8` | 3718464424 | SFXBatteringRam |

#### Core Banks (Always Loaded)

| Hash | Decimal | Bank Name |
|------|---------|-----------|
| `0x50C63A23` | 1355168291 | Init |
| `0x05174939` | 85346617 | Ambience |
| `0xEDF036D6` | 3992339158 | Music |
| `0xF699F20C` | 4137284108 | Chatter |
| `0x73CB32C9` | 1942696649 | Effects |
| `0xD0D5925A` | 3503657562 | BaseCombat |
| `0x5C770DB7` | 1551306167 | UI |
| `0xD49DE19D` | 3567116701 | Creatures |
| `0xF0E6CC1B` | 4041870363 | VoiceOver |

---

## 18. Event Correlation Database

### Event Mapping System

Events are mapped from two sources:

1. **TXTP Files** (456 events): Generated by wwiser from BNK parsing
2. **Bank-Only Events** (121 events): Found in HIRC chunks but no TXTP

**Total**: 577 events in `event_mapping.h`

### Event Mapping Structure

```cpp
struct EventMappingEntry {
    DWORD id;           // FNV-1a hash of event name
    const char* bank;   // Source bank name (e.g., "HeroSauron")
    const char* name;   // Display name (synthetic or known)
};

// Example entries
static const EventMappingEntry g_EventMappingData[] = {
    {0x8E3F67AD, "BaseCombat", "swing"},
    {0x82CEB640, "HeroSauron", "sauron_grunt_01"},
    {0xE2271264, "HeroSauron", "sauron_attack_01"},
    // ...577 total entries
};
```

### Combat Event Semantics

Observed from 50,555+ captured events across multiple gameplay sessions:

| Event Name | Hash | Behavior | Source Bank |
|------------|------|----------|-------------|
| `swing` | `0x8E3F67AD` | Weapon swing SFX | BaseCombat |
| `impact` | `0x????????` | Hit connects (non-lethal) | BaseCombat |
| `impact_kill` | `0x????????` | Hit connects (lethal) | BaseCombat |
| `hero_combat_grunt` | `0x0A40E38E` | Attack vocalization | Hero banks |
| `hit_react` | `0x4BF68CF3` | Damage reaction | BaseCombat |
| `stagger` | `0xB5EE54A3` | Heavy stagger animation | BaseCombat |
| `shield_block` | `0xAA17BDA2` | Block with shield | BaseCombat |
| `miss` | `0xBBB14F0A` | Attack misses | BaseCombat |
| `critical_hit` | `0x????????` | Critical hit bonus | BaseCombat |
| `explosion` | `0x120427B6` | Ability/environmental explosion | BaseCombat |
| `ram_thud` | `0x4FFFB616` | Battering ram impact | BaseCombat |

**Correction Note**: `body_fall` was initially misidentified. Renamed to `ram_thud` after Minas Morgul session showed it correlates with battering ram usage, not character deaths.

### Creature/NPC Events

| Event Name | Behavior |
|------------|----------|
| `creature_swing` | NPC melee attack |
| `creature_attack` | NPC ranged/special attack |
| `creature_vocal` | NPC idle chatter |
| `creature_idle` | Ambient creature sounds |
| `creature_ready` | NPC spawn/ready |
| `creature_death` | NPC death vocalization |

### UI/System Events

| Event Name | Hash | Behavior |
|------------|------|----------|
| `ui_advance` | `0x????????` | Menu navigation forward |
| `ui_scroll` | `0x????????` | Menu scrolling |
| `ui_confirm` | `0x????????` | Selection confirmed |
| `stop_music` | FNV("stop_music") | Stop background music |
| `stop_all_but_music` | FNV(...) | Stop SFX, keep music |
| `stop_ability` | FNV("stop_ability") | Cancel ability sound |

### Unknown Events (FNV Reversal Needed)

These hashes appeared in logs but have no known name:

| Hash | Context | Potential Category |
|------|---------|-------------------|
| `0x58A75C28` | Minas Morgul combat | Unknown |
| `0x6C0EB453` | Combat near death | Possibly kill-related |
| `0x7DF0F9A5` | Hero combat grunt | Hero-specific |

---

## 19. BNK File Structure & Parsing

### Why Complete BNK Parsing Is Needed

**Current State**: Event names are *synthetic* (positional), not *semantic*:

| Current Name | Actual Meaning | Problem |
|--------------|----------------|---------|
| `HeroSauron-0075` | Object index 75 in bank | Tells WHERE, not WHAT |
| `sauron_grunt_01` | Sauron pain vocalization | Tells WHAT it does |

**Root Cause**: Wwise compiles event names to FNV-1a hashes. Original names are lost unless:
1. Original Wwise project exists (unlikely)
2. `SoundbanksInfo.xml` companion file ships with game (doesn't)
3. FNV brute-force reversal succeeds

### BNK Chunk Structure

Wwise BNK files contain multiple chunks:

```
┌────────────────────────────────────────┐
│ BKHD (Bank Header)                     │
│   dwBankGeneratorVersion: 34           │
│   dwSoundBankID: 2927614651            │
│   dwLanguageID: 0x00 [SFX]             │
└────────────────────────────────────────┘
┌────────────────────────────────────────┐
│ STID (String Table ID) - Optional      │
│   Maps bank ID → bank name string      │
│   e.g., 2927614651 → "HeroSauron"      │
└────────────────────────────────────────┘
┌────────────────────────────────────────┐
│ HIRC (Hierarchy)                       │
│   Contains all audio objects:          │
│   - CAkEvent (event triggers)          │
│   - CAkActionPlay (play actions)       │
│   - CAkSound (audio source refs)       │
│   - CAkRanSeqCntr (random containers)  │
│   - CAkSwitchCntr (switch containers)  │
└────────────────────────────────────────┘
┌────────────────────────────────────────┐
│ DATA (Embedded Audio)                  │
│   WEM audio data (ADPCM/Vorbis)        │
└────────────────────────────────────────┘
```

### HIRC Object Chain

Event playback follows this object chain:

```
CAkEvent[77] 2273470494
    │
    └──▶ CAkActionPlay[76] 688068218
            │
            └──▶ CAkSound[27] 1013055050
                    │
                    └──▶ Source 96095981 (WEM file)
                         * ulPluginID: 0x00020001 [ADPCM]
```

**Or with containers**:

```
CAkEvent[81] eventHash
    │
    └──▶ CAkActionPlay actionHash
            │
            └──▶ CAkRanSeqCntr (random/sequence container)
                    │
                    ├──▶ CAkSound → WEM_1
                    ├──▶ CAkSound → WEM_2
                    └──▶ CAkSound → WEM_3  (randomly selected)
```

### Switch/State Parameters in TXTPs

TXTP filenames encode switch parameters:

```
BaseCombat-0709-event [2661483290=595159781] [1214237073=1754364802].txtp
                       └─────────┬─────────┘ └─────────┬──────────┘
                         Switch Group         Switch Value
                         (weapon_type?)       (specific weapon?)
```

These parameters are also FNV-1a hashed. Known switch groups:

| Hash | Likely Name | Purpose |
|------|-------------|---------|
| `2661483290` | weapon_type? | Weapon category |
| `1214237073` | ability_type? | Ability selection |
| `3893417221` | faction_type? | Good/Evil sounds |
| `1728396083` | attack_type? | Attack variation |

### Extracted BNK Files

Located in `wwiseRE/extracted/`:

| File | Size | Bank Name |
|------|------|-----------|
| `2927614651.bnk` | ~200KB | HeroSauron |
| `38771874.bnk` | ~150KB | ChatterHeroSauron |
| `1355168291.bnk` | ~50KB | Init |
| `3503657562.bnk` | ~1MB | BaseCombat |
| `1942696649.bnk` | ~500KB | Effects |
| ... | ... | (~57 banks total) |

### Parsing with wwiser

```batch
cd wwiseRE/Tools/wwiser-20250928
python wwiser.py ../../extracted/*.bnk -g -o ../../extracted_xml/
```

Generates XML with full object hierarchy and hash annotations.

---

## 20. FNV Hash Reversal

### The fnv.c Tool

Located at `wwiseRE/Tools/wwiser-utils-master/fnv/fnv.c`

**Purpose**: Brute-force reverse FNV-1a hashes to find original strings.

### How It Works

1. Uses n-gram filtering to eliminate unlikely letter combinations
2. Supports prefix/suffix constraints to narrow search space
3. Default dictionary: `abcdefghijklmnopqrstuvwxyz_0123456789`
4. Maximum default depth: 7 characters

### Compilation

```batch
cd wwiseRE/Tools/wwiser-utils-master/fnv
gcc -O2 fnv.c -o fnv.exe
```

### Usage

```batch
# Basic reverse lookup
fnv.exe 0x8E3F67AD

# With prefix constraint
fnv.exe -p "play_" 0x8E3F67AD

# With suffix constraint
fnv.exe -s "_sfx" 0x8E3F67AD

# Increase search depth (slower)
fnv.exe -d 10 0x8E3F67AD

# Forward hash (name to hash)
fnv.exe -n swing
```

### Common Wwise Prefixes

Try these when reversing unknown hashes:

| Prefix | Purpose |
|--------|---------|
| `play_` | Play event |
| `stop_` | Stop event |
| `set_` | Set switch/state |
| `hero_` | Hero-specific |
| `creature_` | NPC sounds |
| `sfx_` | Sound effects |
| `vo_` | Voice-over |
| `mus_` | Music |
| `amb_` | Ambient |
| `ui_` | UI sounds |

### Limitations

- Max practical depth: ~10-12 characters (exponential time)
- Cannot find names with uppercase letters (Wwise lowercases before hashing)
- May find collisions (multiple strings with same hash)
- N-gram filtering may exclude valid rare combinations

### Example Reversal Session

```batch
# Unknown hash from Minas Morgul combat
fnv.exe -p "hero_" 0x7DF0F9A5
# Output: hero_pain_01

fnv.exe -p "creature_" 0x58A75C28
# Output: (no match - try different prefix)

fnv.exe -p "sfx_" 0x58A75C28
# Output: sfx_ram_hit
```

---

## Appendix C: Audio System Quick Reference

### Overlay Hotkeys

| Key | Panel/Action |
|-----|--------------|
| `F7` | Toggle Audio Event Log |
| `F10` | Toggle Asset Browser |
| `ENTER` | Play selected event (in Asset Browser) |
| `DELETE` | Stop all sounds |
| `UP/DOWN` | Navigate event list |
| `PAGE UP/PAGE DOWN` | Jump 12 events |

### Key Files

| File | Purpose |
|------|---------|
| `DebugOverlay/src/audiohook.cpp` | Audio hook implementation |
| `DebugOverlay/src/audiohook.h` | Audio hook API declarations |
| `DebugOverlay/src/event_mapping.h` | 577 event mappings (auto-generated) |
| `DebugOverlay/src/hash_dictionary.cpp` | Bank/event hash → name mappings |
| `wwiseRE/parse_txtp.py` | Generates event_mapping.h from TXTPs |
| `wwiseRE/overrides.csv` | Manual name overrides |
| `wwiseRE/extracted/*.bnk` | Raw soundbank files |
| `wwiseRE/txtp/*.txtp` | Event playback definitions |

### Build Commands

```batch
# Build overlay with audio hooks
cd C:\Users\Yusuf\Desktop\Code\ConquestConsole\DebugOverlay
cmake --build build --config Release

# Output: DebugOverlay/build/bin/Release/DebugOverlay.dll
```

### Captured Data Locations

| File | Contents |
|------|----------|
| `wwiseRE/Minas_Morgul.txt` | 50,555 events from Minas Morgul session |
| `wwiseRE/Isengard_Gameplay.txt` | Isengard level audio log |
| `wwiseRE/Helms_Deep_Gameplay.txt` | Helm's Deep audio log |
| `wwiseRE/captured_audio_names.txt` | Runtime captured bank/event names |

---

*End of Knowledge Base*
