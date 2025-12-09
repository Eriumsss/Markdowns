












 ----------------------------------------------------------------------------------------------------------------------------------------------------------------                                                               
                                                                {
                                                                "type": "logic_gamestart",
                                                                "layer": TemplateLayerGUID,
                                                                "fields": {
                                                                "GUID": UniqueGUID,
                                                                "ParentGUID": ConstructGUID,
                                                                "GameModeMask": GameModeBitmask,
                                                                "Name": "CQ_GameStart",
                                                                etc...}
                                                                outputs:→ →
                                                                }         ↓
                                                                          ↓
                                                                          ↓
                                                                          ↓
                                                                          ↓
                                                                  "type": "Output",
                                                                  "layer": TemplateLayerGUID,
                                                                  "fields": {
                                                                  "GUID": UniqueGUID,
                                                                  "ParentGUID": ConstructGUID,
                                                                  "GameModeMask": GameModeBitmask,
                        EventNameList ← ← ← ← ← ← ← ← ← ← ← ← ← ← "Name": "EventName",
                         "OnTrigger",                              etc...}
                         "OnMetaGameStart",                                 ↓
                         "OnDamaged",                                       ↓
                         "OnTimeHit",                             "Output": "EventName",
                         "OnCase1",                               "Target": "TargetEventGUID",
                         "OnCase2",                               "Input":  "InputEventName", → → → → → → → → → → → → → → → → → InputNameList
                         "OnCase3",                               "Delay": 0.0,                                                 "Start",
                         "OnCase4",                               "Sticky": true/false,                                         "PickRandom",
                         "OnCase5",                               "Parameter": "ParameterString"                                "Activate",
                         "OnEnter",                                                                                             "Kill",
                         "OnExit",                                                                                              "Destroy",
                         "OnDeath",                                                                                             "ForceGrantRewardToTeam",
                         "OnFadeInComplete",                                                                                    "Deactivate",
                         "OnFadeOutStart",                                                                                      "trigger",
                         "OnDepleted",                                                                                          "StartWarning",
                         "OnCapture",                                                                                           "StopWarning",
                         "OnDecapture",                                                                                         "AddPoints",
                         "OnBlueCapture",                                                                                       "create",
                         "OnRedCapture",                                                                                        "PostAnimationEvent",     
                         "OnStartCapture",                                                                                      "DisplayOn",
                         "OnStartDefend",                                                                                       "DisplayDecreasing",
                         "OnComplete",                                                                                          "DisplayOff",
                         "OnMaxHit",                                                                                            "DisableCapture",
                         "OnMemberKilledByHostilePlayer",                                                                       "SetTeam",                      
                         "OnKilledByHostilePlayer",                                                                             "SetAllowBlueCapture",                
                         "OnActivate",                                                                                          "COMPLETE", 
                         "OnIncrement",                                                                                         "EnableEvents",
                         "OnMaxHit",                                                                                            "SetAllowRedCapture", 
                         "OnMemberDeath",                                                                                       "Increment",
                         "OnEqualTo",                                                                                           "Fire",
                         "OnPointChange",                                                                                       "DisableEvents",
                         "OnVictory",                                                                                           "SetIsDamageAllowed",
                         "OnMemberKilledByHostileTeam",                                                                         "StartFiring",                    
                         "OnMemberSuicide",                                                                                     "StartEffectOnCamera",        
                         "OnTrickleTeam1",                                                                                      "DeactivateLinkPath",       
                         "OnTrickleTeam2",                                                                                      "SetPathLinkActiveBothWays",       
                         "On50Percent",                                                                                         "Team2Won",    
                         "On50PointsToGo"                                                                                       "Pause",
                                                                                                                                "Team1Won",
                                                                                                                                "SetInputAndCompare",
                                                                                                                                "compare",
                                                                                                                                "AttachObject",
                                                                                                                                "PostAnimationEvent",
                                                                                                                                "Neutral",
                                                                                                                                "AddTrickle",
                                                                                                                                "UnlockTeam1Hero",
                                                                                                                                "UnlockTeam2Hero",
                                                                                                                                "stop",
                                                                                                                                "PickRandom",
+------------------------------------------------------------------------------------------------------------------------------------------------------------



                                                                {
                                                                "type": "logic_timer",
                                                                "layer": TemplateLayerGUID,
                                                                "fields": {
                                                                "GUID": UniqueGUID,
                                                                "ParentGUID": ConstructGUID,
                                                                "GameModeMask": GameModeBitmask,
                                                                "Name": "CQ_Timer_GameStart",
                                                                "TimeHit": 180.0,
                                                                "Looping": false,
                                                                "CreateOnLoad": true,
                                                                "Texture": "fed_timer.tga",
                                                                "Type": "Billboard",
                                                                etc...}
                                                                outputs:→ →
                                                                }         ↓
                                                                          ↓
                                                                          ↓ (OnTimeHit triggers when timer reaches TimeHit value)
                                                                          ↓
                                                                  "type": "Output",
                                                                  "layer": TemplateLayerGUID,
                                                                  "fields": {
                                                                  "GUID": UniqueGUID,
                                                                  "ParentGUID": ConstructGUID,
                                                                  "GameModeMask": GameModeBitmask,
                                                                  "Name": "OnTimeHit",
                                                                  etc...}
                                                                          ↓
                                                                          ↓
                                                                "Output": "OnTimeHit",
                                                                "Target": "TargetEventGUID",
                                                                "Input":  "DisplayOff",
                                                                "Delay": 0.0,
                                                                "Sticky": true,
                                                                "Parameter": ""
                                                                }


                                                        Timer Inputs (triggered by other objects):

                                                        CQ_GameStart → → → → → → → → → → → → → → → → → → → → CQ_Timer_GameStart
                                                        (GUID: 7055406)                                         (GUID: 1100050000)
                                                                ↓                                                       ↑
                                                                ↓                                                       ↑
                                                        Output: "OnTrigger"                                             ↑
                                                        Target: 1100050000                                              ↑
                                                        Input:  "DisplayOn"          ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
                                                        Delay:  0.0
                                                        Sticky: true
                                                        (GUID: 1100050010)
                                                                ↓
                                                                ↓
                                                        Output: "OnTrigger"
                                                        Target: 1100050000
                                                        Input:  "DisplayDecreasing"  ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
                                                        Delay:  0.0                                                     ↑
                                                        Sticky: true                                                    ↑
                                                        (GUID: 1100050011)                                              ↑
                                                                ↓                                                       ↑
                                                                ↓                                                       ↑
                                                        Output: "OnTrigger"                                             ↑
                                                        Target: 1100050000                                              ↑
                                                        Input:  "Start"              ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
                                                        Delay:  0.0
                                                        Sticky: true
                                                        (GUID: 1100050012)


                                                        Timer Outputs (when OnTimeHit fires):

                                                        CQ_Timer_GameStart → → → → → → → → → → → → → → → → → → → → Self
                                                        (GUID: 1100050000)                                              ↑
                                                                ↓                                                       ↑
                                                                ↓                                                       ↑
                                                        Output: "OnTimeHit"                                             ↑
                                                        Target: 1100050000                                              ↑
                                                        Input:  "DisplayOff"         ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
                                                        Delay:  0.0
                                                        Sticky: true
                                                        (GUID: 1100050001)
                                                                ↓
                                                                ↓
                                                        Output: "OnTimeHit"
                                                        Target: 7055406              ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
                                                        Input:  "Activate"                                              ↑
                                                        Delay:  0.0                                                     ↑
                                                        Sticky: true                                                    ↑
                                                        (GUID: 1100050002)                                    CQ_GameStart
                                                                ↓                                         (GUID: 7055406)
                                                                ↓
                                                        Output: "OnTimeHit"
                                                        Target: 1100050000           ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
                                                        Input:  "Kill"                                                  ↑
                                                        Delay:  1.0                                                     ↑
                                                        Sticky: true                                                    ↑
                                                        (GUID: 1100050003)                                              ↑
                                                                                                                      Self

+------------------------------------------------------------------------------------------------------------------------------------------------------------




 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 -----------------------------------------------------------------------------------------------------------------------------------------------------------
                                           (Animation_Tables)
                                                    ↓
                                                    ↓
                                Example: ANM_UNIT_GDR_CAP_Warrior.json
                                                    ↓
                                                    ↓
                                              (sub_blocks1)
                                                    ↓
                                                    ↓
                                                    ↓
                                         (AT_UNIT_BASE_Warrior.lua)  → → → → →
                                                    ↓                         ↓
                                                    ↓                         ↓
                                                    ↓                AnimTableNames = {
                                                    ↓                "AT_WarriorCommon",
                                                    ↓                "AT_WarriorGood",
                                                    ↓                "AT_WarriorEvil",
                                                    ↓                "AT_WarriorCaptain"
                                                    ↓                 }
                                                    ↓                AT_WarriorCaptain = {
                                                    ↓                StandIdle = "RH10_mace_loc_Idle",
                                                    ↓                etc...
                                                    ↓                }
                                                    ↓
                                                    ↓
                                                    ↓
                                         ANM_UNIT_GDR_CAP_Warrior.lua → → → → inherit("AT_UNIT_GDR_CAP_Warrior")
                                                    ↓                                           ↓
                                                    ↓                                           ↓
                                                    ↓                                           ↓
                                            (pak_vals_a.json)                                   ↓
                                                    ↓                                           ↓
                                                    ↓                                    (AT_UNIT_GDR_CAP_Warrior.lua)    
                                                    ↓                                           ↓
                                         {                                                import("AT_CommonHumanoid")
                                         "unk_0": 7,                                      import("AT_UNIT_BASE_Warrior")
                                         "gamemodemask": 2,                               AnimTableName = "AnimTable_UnitWarriorCaptainClass_GOOD"
                                         "key": "AT_UNIT_GDR_CAP_Warrior.lua",            AnimTable = {
                                         "unk_3": 0,                                      AT_CommonHumanoidGood,
                                         "unk_4": 0,                                      AT_CommonHumanoidRH6,
                                         "unk_5": 4245281217,                             AT_CommonHumanoid,
                                         "unk_6": 29970285                                AT_WarriorCommon,
                                         },                                               AT_WarriorCaptain
                                                   ↓                                      }
                                                   ↓                                     AnimTableUsed = {
                                                   ↓                                     "AT_CommonHumanoid",
                                            (pak_strings.json)                           "AT_CommonHumanoidRH6",
                                                   ↓                                     "AT_CommonHumanoidGood",
                                                   ↓                                     "AT_WarriorCommon",
                                                   ↓                                     "AT_WarriorCaptain"
                                      "ANM_UNIT_GDR_CAP_Warrior.lua",                    }
                                      "AT_UNIT_GDR_CAP_Warrior.lua",
                                                   ↓
                                                   ↓
                                                   ↓
                                              (index.json)
                                                   ↓
                                                   ↓
                                                   ↓
                                                   ↓
                                    "ANM_UNIT_GDR_CAP_Warrior.lua",
                                    "AT_UNIT_GDR_CAP_Warrior.lua", 





"BC_UNIT_GDR_CAP_Warrior.lua",
"SM_UNIT_GDR_CAP_Warrior.lua",
"ATK_SM_UNIT_GDR_CAP_Warrior.lua",
"ATK_INFO_UNIT_GDR_CAP_Warrior.lua",