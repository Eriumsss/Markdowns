# Stronghold Multiplayer Lobby System Analysis

## Executive Summary

The Stronghold mode is a multiplayer lobby system that relies on **EA's online services** and **GameSpy** for matchmaking and authentication. The system is currently non-functional due to:
1. **GameSpy shutdown** (May 31, 2014)
2. **EA online services deprecation**
3. **Authentication server unavailability**

---

## Component Overview

### UI Components

#### Flash UI Files:
```
flash/strongholdlobby.gfx          - Main lobby interface
flash/strongholdsetup.gfx          - Pre-lobby setup screen
flash/strongholdpowersummary.gfx   - Post-game power summary
```

#### UI State Classes:
```
MgUIStrongholdLobby    (0x009eefa4) - Lobby state manager
MgUIStrongholdSetup    (0x009ef15c) - Setup state manager
```

#### ActionScript External Interface Functions:
```
exShowRewardInfo       - Display reward information
exShowBattleResults    - Show post-battle results
exSetResultValues      - Set result values
exSetRewardText        - Set reward text
exSetNextText          - Set next screen text
exSelectRegion         - Select map region
exAddPlayer            - Add player to lobby
exRemovePlayer         - Remove player from lobby
exSetTeam              - Set player team
exChangeMapStatus      - Update map availability
exChangeRegionAdded    - Update region selection
```

#### Button/Action Strings:
```
ChangeTeams            (0x009eefd4) - Team change button
StartBattle            (0x009eefe0) - Start game button
CancelStrongholdGame   (0x009eefec) - Cancel/leave button
```

---

## Network Architecture

### GameManager System

The game uses a **GameManager** system for multiplayer coordination:

#### Key Components:
- **GameManagerHostedGame** - Host-side game management
- **GameManagerListenerCrit** - Critical section for thread safety
- **Peer Mesh Networking** - P2P connections between players
- **Host Migration** - Automatic host transfer on disconnect

#### Network Ports:
```
gameListenPort         - Main game port (likely 11900 based on QUICK_START.md)
LanBroadcastPort       - LAN discovery port
```

#### Network Messages:
```
GM: Game Start Request Received
GM: Received host hello
GM: Received join announcement for player %i
GM: Received leave announcement for player %i
GM: Received host migration finish packet
GM: Sent join mesh announcement for player %i
GM: Broadcasting player %d playgroup change
GM: Sending voip change
```

---

## Authentication & Online Services

### EA/Nu Services (Deprecated)

The game relies on EA's "Nu" authentication system:

#### Authentication Functions (0x009a5xxx range):
```
NuLogin                - Main login function
NuAddAccount           - Create account
NuUpdateAccount        - Update account info
NuGetPersonas          - Retrieve player personas
NuLoginPersona         - Login with persona
NuAddPersona           - Create new persona
NuDisablePersona       - Disable persona
NuUpdatePassword       - Change password
NuGetTos               - Get Terms of Service
NuGetAccount           - Retrieve account data
NuGetAccountByNuid     - Get account by Nu ID
```

#### Platform-Specific Auth:
```
NuPS3Login             - PlayStation 3 login
NuPS3AddAccount        - PS3 account creation
NuXBL360Login          - Xbox 360 login
NuXBL360AddAccount     - Xbox 360 account creation
NuGetAccountByPS3Ticket - PS3 ticket validation
```

#### Entitlement System:
```
NuEntitleUser          - Check user entitlements
NuEntitleGame          - Check game entitlements
NuGetEntitlementCount  - Get entitlement count
NuGetEntitlements      - Retrieve entitlements
NuSearchOwners         - Search content owners
```

### GameSpy Integration

**GameSpy PreAuth** (0x009a5d98):
- Used for GameSpy authentication
- GameSpy services shut down May 31, 2014
- No longer functional

---

## Lobby Flow

### 1. Main Menu → Stronghold Setup
```
User selects "Stronghold" mode
↓
MgUIStrongholdSetup loads
↓
flash/strongholdsetup.gfx displayed
↓
Player configures:
  - Map/Region selection (exSelectRegion, exChangeRegionAdded)
  - Game settings
```

### 2. Stronghold Setup → Lobby
```
Player clicks "Continue" or "Host Game"
↓
Authentication check (NuLogin, GameSpyPreAuth)
↓
If hosting:
  - GameManagerHostedGame created
  - Listen on gameListenPort
  - Broadcast on LAN (if enabled)
  - Register with EA servers (FAILED - servers down)
↓
If joining:
  - Query lobby list (LOBBY-NUM-GAMES, LOBBY-MAX-GAMES)
  - Connect to host
  - Send join request
↓
MgUIStrongholdLobby loads
↓
flash/strongholdlobby.gfx displayed
```

### 3. Lobby State
```
Players in lobby can:
  - Change teams (ChangeTeams button → exSetTeam)
  - Add/remove players (exAddPlayer, exRemovePlayer)
  - Select regions (exSelectRegion)
  - View rewards (exShowRewardInfo)
  - Chat (exScrollChatText, exAddChatText)
↓
Host clicks "StartBattle"
↓
GM: Game Start Request Received
↓
Peer mesh connections established
↓
Game launches
```

### 4. Post-Game
```
Game ends
↓
flash/strongholdpowersummary.gfx loads
↓
Display results:
  - exShowBattleResults
  - exSetResultValues
  - exSetRewardText
↓
Return to lobby or main menu
```

---

## LAN Mode

### LAN Discovery System

The game supports **LAN multiplayer** without online services:

#### LAN Broadcast:
```
GMLAN: Cannot bind to broadcast port %i
GMLAN: Cannot open broadcast socket!
GMLAN: Dropping oldest message due to max simultaneous messages
```

#### LAN Lobby:
```
NUM-LOBBIES            - Number of active lobbies
LOBBY-NUM-GAMES        - Games in lobby
LOBBY-MAX-GAMES        - Max games per lobby
NUM-GAMES              - Total games
```

#### LAN Packet Handling:
```
Request from %s id = %i
Received unknown LAN packet type %i from 0x%x
```

**LAN mode may still be functional** as it doesn't require external servers.

---

## Critical Dependencies

### External Services (BROKEN):
1. **EA Nu Authentication Servers** - Offline
2. **GameSpy Matchmaking** - Shut down 2014
3. **EA Entitlement Servers** - Likely offline
4. **EA Presence/Ranking Services** - Likely offline

### Internal Systems (FUNCTIONAL):
1. **GameManager** - Local game coordination
2. **Peer Mesh Networking** - P2P connections
3. **LAN Broadcast** - Local network discovery
4. **Flash UI** - Interface rendering

---

## Restoration Strategy

### Option 1: LAN-Only Mode (Easiest)
**Bypass online authentication, enable LAN-only multiplayer**

Steps:
1. Patch authentication checks to always succeed
2. Force LAN mode enabled
3. Remove EA/GameSpy service calls
4. Test LAN lobby functionality

### Option 2: Custom Server (Medium)
**Implement replacement authentication/matchmaking server**

Requirements:
1. Reverse engineer Nu authentication protocol
2. Create mock authentication server
3. Implement lobby listing service
4. Redirect game to custom server (DNS/hosts file)

### Option 3: Direct Connect (Hard)
**Add IP-based direct connection**

Requirements:
1. Add UI for IP entry
2. Bypass lobby system
3. Direct peer connection
4. Modify Flash UI or create new screen

---

## Next Steps

### Immediate Actions:
1. **Find authentication check functions** - Search for NuLogin calls
2. **Locate LAN mode toggle** - Find where LAN is enabled/disabled
3. **Test LAN functionality** - Try LAN multiplayer on local network
4. **Identify server connection code** - Find where EA servers are contacted

### Analysis Tasks:
1. Search disassembly for "NuLogin" references
2. Find GameManager initialization code
3. Locate LAN broadcast implementation
4. Identify authentication bypass points
5. Map out lobby state transitions

### Testing:
1. Try LAN mode with 2 PCs on same network
2. Monitor network traffic (Wireshark)
3. Check for hardcoded server addresses
4. Test with firewall blocking EA domains

---

## Key Addresses

### UI Strings & References:
```
0x009eefa4 - MgUIStrongholdLobby string
0x009657b8 - MgUIStrongholdLobby XREF (function that uses it)
0x009eefb8 - flash/strongholdlobby.gfx string
0x0090a960 - flash/strongholdlobby.gfx XREF (loader function)
0x009eefd4 - ChangeTeams string
0x0090b1f0 - ChangeTeams XREF
0x009eefe0 - StartBattle string
0x0090b225 - StartBattle XREF #1
0x0090b25c - StartBattle XREF #2
0x009eefec - CancelStrongholdGame string
0x0090b3b0 - CancelStrongholdGame XREF
0x009ef004 - exShowRewardInfo string
0x009ef15c - MgUIStrongholdSetup string
0x009ef170 - flash/strongholdsetup.gfx string
0x009ed2fc - flash/strongholdpowersummary.gfx string
```

### Authentication Functions:
```
0x009a5d98 - GameSpyPreAuth string
0x005af6b3 - GameSpyPreAuth initialization (PATCH TARGET)
0x009a5ea4 - NuLogin string
0x005af578 - NuLogin initialization (PATCH TARGET)
0x005af620 - NuLoginPersona initialization
0x005af58d - NuAddAccount initialization
0x005af5a2 - NuAddPersona initialization
0x005af5b7 - NuDisablePersona initialization
0x005af5e1 - NuGetTos initialization
0x005af689 - NuGetPersonas initialization
0x005af69e - NuUpdateAccount initialization
```

### GameManager Functions:
```
0x009a5b88 - GameManager string
0x005ad7d0 - GameManager initialization
0x005cf25f - GameManager listening on port (function)
0x005cf280 - GameManager network handler
0x005d427e - GameManagerHostedGame::EndGame()
0x005d42a1 - GameManagerHostedGame::EndGame() (ignored path)
0x005e4800 - GameManagerHostedGame::StartGame()
0x005e4824 - GameManagerHostedGame::StartGame() (alternate)
0x005e5907 - GameManager Protocol Version check
0x005df02a - GameManagerListenerCrit (critical section)
```

### Network Strings:
```
0x009a5a34 - gameListenPort %d
0x009a5a1c - LanBroadcastPort %d
0x009aa0ac - NUM-LOBBIES
0x009aa720 - LOBBY-NUM-GAMES
0x009aa730 - LOBBY-MAX-GAMES
0x009a9168 - "GM: GameManager listening on port %d"
```

---

## Practical Restoration Steps

### Phase 1: Analysis (Current)
 **Completed:**
- Identified UI components and Flash files
- Located authentication functions
- Found GameManager system
- Mapped network architecture
- Documented key addresses

### Phase 2: Function Analysis (Next)
**Tasks:**
1. **Examine authentication initialization** (0x005af578 - 0x005af717)
   - Open in Ghidra at address 0x005af578
   - Analyze the function that sets up all Nu/GameSpy services
   - Identify where these services are called during lobby join

2. **Analyze MgUIStrongholdLobby** (0x009657b8)
   - Find the function that references this string
   - Understand lobby initialization flow
   - Identify authentication checks

3. **Study GameManager initialization** (0x005ad7d0)
   - Understand how GameManager starts
   - Find LAN mode toggle
   - Locate port configuration

4. **Trace StartBattle flow** (0x0090b225, 0x0090b25c)
   - Follow the code path from button click to game start
   - Identify authentication gates
   - Find peer connection establishment

### Phase 3: Patching Strategy
**Option A: NOP Authentication (Simplest)**
```
At 0x005af6b3 (GameSpyPreAuth init):
  Replace: MOV dword ptr [.data:...], .rdata:s_GameSpyPreAuth
  With:    NOP instructions (0x90)

At 0x005af578 (NuLogin init):
  Replace: MOV dword ptr [.data:...], .rdata:s_NuLogin
  With:    NOP instructions (0x90)
```

**Option B: Force Success Return**
- Find where authentication functions are called
- Patch to skip call and set success return value (EAX = 1)

**Option C: Redirect to Stub**
- Create stub functions that always return success
- Redirect authentication calls to stubs

### Phase 4: Testing
1. **Hex edit Debug.exe** with chosen patches
2. **Launch game** and navigate to Stronghold mode
3. **Monitor behavior**:
   - Does lobby screen load?
   - Can you see LAN games?
   - Can you host a game?
4. **Test with 2 PCs** on same LAN
5. **Use Wireshark** to monitor network traffic

### Phase 5: Refinement
- Fix any crashes from patching
- Enable LAN broadcast if disabled
- Test full game flow: lobby → game → results

---

##  CRITICAL DISCOVERY: Stronghold Mode Selection Logic

### **Function FUN_00726825** - Game Mode Selector

This function determines which game mode to load based on a value at `[ESI + 0x3f0]`:

```asm
00726825  PUSH  ESI
00726826  MOV   ESI, dword ptr [DAT_00cd7fdc]     ; Load game state pointer
0072682c  MOV   EAX, dword ptr [ESI + 0x3f0]      ; Load mode selector value
00726832  TEST  EAX, EAX                          ; Check if 0
00726834  JZ    LAB_00726858                      ; Jump if 0
00726836  CMP   EAX, 0x1                          ; Check if 1
00726839  JZ    LAB_00726850                      ; Jump if 1 (InstantAction)
0072683b  ADD   EAX, -0x2                         ; Subtract 2
0072683e  CMP   EAX, 0x1                          ; Check if result <= 1
00726841  JA    LAB_00726887                      ; Jump if > 1 (out of range)
00726843  CALL  FUN_0074673d                      ; Prepare for Stronghold
00726848  PUSH  ECX
00726849  MOV   ECX, s_Game_Stronghold_009ea43c   ; Load "Game_Stronghold" string
0072684e  JMP   LAB_0072687a                      ; Continue to load Stronghold

LAB_00726850:  ; InstantAction path
00726850  PUSH  ECX
00726851  MOV   ECX, s_Game_InstantAction_009ea428  ; Load "Game_InstantAction"
00726856  JMP   LAB_0072687a
```

### **Mode Selection Logic:**
```
Value at [ESI + 0x3f0]:
  0 = Unknown/Default
  1 = Instant Action
  2 = Stronghold (value - 2 = 0, passes check)
  3 = Stronghold (value - 2 = 1, passes check)
  4+ = Out of range (blocked)
```

### **The Problem:**
The game mode selector value at `[ESI + 0x3f0]` is **never being set to 2 or 3** because:
1. Authentication fails (EA servers down)
2. Entitlement check fails (no online verification)
3. UI disables the Stronghold option before user can select it

### **The Solution:**
We need to find where the UI checks if Stronghold should be enabled and patch it to always allow selection.

---

## Immediate Action Items

### **PRIORITY 1: Find UI Availability Check**

Search for functions that check if Stronghold mode is available. Look for:

1. **Entitlement checks** - Functions that verify DLC/content ownership
2. **Online status checks** - Functions that check EA server connection
3. **Menu builder** - Function that creates the mode selection UI

**Key addresses to investigate:**
- `0x00726825` - Mode selector (we found this!)
- `0x0074673d` - Called before loading Stronghold (line 742)
- `DAT_00cd7fdc` - Game state pointer
- `[ESI + 0x3f0]` - Mode selection value storage

### **PRIORITY 2: Patch Options**

**Option A: Force Mode Value (Simplest)**
- Find where `[ESI + 0x3f0]` is set
- Patch to always allow value 2 or 3

**Option B: Bypass Range Check**
- At `0x00726841`: Change `JA LAB_00726887` to `JMP LAB_00726843`
- This forces Stronghold path regardless of value

**Option C: Enable UI Option**
- Find menu builder function
- Patch to always show Stronghold option
- Let user select it normally

### For You (User):
1. **Search for function 0x0074673d** in Ghidra
   - This is called right before Stronghold loads
   - May contain authentication/entitlement checks

2. **Find references to 0x00cd7fdc**
   - This is the game state pointer
   - Find where `[ESI + 0x3f0]` is written

3. **Search for "entitlement" or "available"**
   - Look for UI availability checks

### For Me (AI):
1. **Create hex patch for Option B** (bypass range check)
2. **Analyze function 0x0074673d** once you share it
3. **Document complete patching strategy**

---

##  CRITICAL DISCOVERY: NuLogin Call Mechanism Found!

### **LAB_005bb1e0 - NuLogin Caller Function**

This function loads and calls NuLogin with authentication parameters:

```asm
LAB_005bb1e0:                                    XREF[1]: 009a7480
005bb1e0  PUSH  EBX
005bb1e1  PUSH  ESI
005bb1e2  MOV   ESI, dword ptr [ESP + 0xc]
005bb1e6  MOV   EBX, ECX
005bb1e8  PUSH  EDI
005bb1e9  MOV   EDI, dword ptr [DAT_00a32760]      ; Load NuLogin pointer!
005bb1ef  MOV   ECX, ESI
005bb1f1  CALL  FUN_005ade50                       ; Prepare auth parameters
005bb1f6  PUSH  EDI                                ; Push NuLogin pointer
005bb1f7  PUSH  DAT_009a63b4                       ; Push parameter
005bb1fc  MOV   ECX, ESI
005bb1fe  MOV   dword ptr [ESI + 0x18], 0x61636374 ; "acct"
005bb205  CALL  FUN_005b37f0                       ; Call NuLogin via pointer!
```

**Key Finding:** NuLogin is called at **0x005bb205** via function pointer!

### **Function Pointer Table - PTR_FUN_009a7438**

All EA/GameSpy services are stored in a function pointer table:

```
009a7438: FUN_005bb100  - Service handler 0
009a743c: LAB_005bb130  - Service handler 1
...
009a7480: LAB_005bb1e0  - NuLogin handler! ⚠️
009a7484: FUN_005bb730  - Service handler 19
...
```

**Entry 18 (0x009a7480)** points to **LAB_005bb1e0** - the NuLogin caller!

### **Service Initialization - FUN_005bc64c**

```asm
005bc63f  MOV  dword ptr [ESI], PTR_FUN_009a7438   ; Load function table
005bc64c  CALL FUN_005af560                        ; Initialize services
```

This loads the function pointer table and calls the auth init we already patched!

---

## Patching Strategy - UPDATED

### **Current Patches Applied:**
1.  Auth init skip at 0x005af568
2.  Mode selector bypass at 0x00726841 (EB 00)
3.  Connection checks bypass at 0x00727b24-0x00727b4b (40 NOPs)

### **Why Login Still Appears:**

The game is calling **LAB_005bb1e0** (NuLogin handler) which:
1. Loads NuLogin pointer from DAT_00a32760
2. Calls it at 0x005bb205
3. When it fails (EA servers down), shows login dialog

### **Additional Patch Needed:**

**Option A: NOP the NuLogin call**
- **Address:** 0x005bb205
- **Current:** `E8 E6 85 FF FF` (CALL FUN_005b37f0)
- **Change to:** `90 90 90 90 90` (5 NOPs)
- **Effect:** Skip NuLogin call entirely

**Option B: Force success return**
- **Address:** 0x005bb20a (right after call)
- **Insert:** `B8 01 00 00 00` (MOV EAX, 1)
- **Effect:** Fake successful login

**Option C: Redirect function pointer**
- **Address:** 0x00a32760 (NuLogin pointer)
- **Change to:** Point to stub that returns success
- **Effect:** NuLogin always succeeds

---

## Conclusion

The Stronghold lobby system is **architecturally sound** but **functionally broken** due to external service dependencies. The most viable restoration path is **LAN-only mode** by patching authentication checks. The GameManager and peer networking systems appear intact and may work once authentication is bypassed.

**Critical Finding**: NuLogin is called at **0x005bb205** - this is the final blocking point. Patch this call to bypass the login requirement!

