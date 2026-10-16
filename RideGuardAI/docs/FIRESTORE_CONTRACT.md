# Firestore contract between the vest (Raspberry Pi) and the app

The Raspberry Pi talks to Firebase with the **Firebase Admin SDK for Python**
(`pip install firebase-admin`) using a service-account key. The app reads and
writes the same documents. All timestamps are **milliseconds since 1970** (numbers).

## Collections

### `users/{authUid}`
| field | example | notes |
|---|---|---|
| role | `"rider"` or `"company"` | checked at login |
| riderId | `"<authUid>"` | riders only |
| companyId | `"SWIFT01"` | the company code |
| displayName | `"Ali Hassan"` | |
| fcmToken | `"..."` | written by the app, used for push notifications |

### `riders/{riderId}`
`fullName, phone, email, companyId, companyName, riderCode, age, bloodType, allergies,
conditions, medication, medicalNotes, contactName, contactRelationship, contactPhone,
consent (bool), deviceId, status ("active" | "inactive" | "invited" | "removed")`

### `devices/{deviceId}` (e.g. `VEST-001`) — written by the Pi every ~30 s
| field | type | notes |
|---|---|---|
| riderId, companyId | string | |
| online | bool | true while running |
| lastSeen | number | ms; the app shows "disconnected" after 2 minutes |
| battery | number | 0–100 |
| mpuOk, frontCamOk, rearCamOk, networkOk, gnssOk | bool | hardware checks |
| state | string | one of the states below |
| rideActive | bool | set by the app's Start/End ride button |

### `incidents/{incidentId}`
| field | written by | notes |
|---|---|---|
| riderId, deviceId, companyId, riderName, riderPhone, contactName, contactPhone, healthSnapshot | Pi (copy from `riders`) or app (manual SOS) | |
| incidentNumber | Pi | e.g. `INC-4F2A9C` |
| type | Pi / app | `"auto"` or `"manual_sos"` |
| source | Pi / app | `"vest"` or `"app"` |
| status | both | see states |
| createdAt | Pi | ms |
| countdownEndsAt | Pi | ms; **required** so the app can show the countdown |
| latitude, longitude | Pi | numbers |
| locationStatus | Pi | `"pending"`, `"live"`, `"last_known"` |
| locationUpdatedAt | Pi | ms |
| locationRequestAt | app | rider pressed "Update my location" → Pi sends a fresh fix |
| maxAcceleration (g), maxRotation (°/s), tiltDeg, aiScore (0–1) | Pi | sensor summary |
| smsStatus | Pi | `"not_sent"`, `"sending"`, `"sent"`, `"failed"` |
| companyNotified | Pi / Cloud Function | bool |
| frontVideoStatus, rearVideoStatus | Pi | `"recording"`, `"pending"`, `"uploading"`, `"uploaded"`, `"failed"`, `"not_uploaded"` |
| frontVideoProgress, rearVideoProgress | Pi | 0–100 |
| frontVideoUrl, rearVideoUrl | Pi | Storage path (`videos/<id>/front.mp4`), `gs://` URL or https URL |
| acknowledged, acknowledgedBy, closedAt, finalResult | company app | |
| falseAlarmReported | rider app | Pi should send a follow-up "rider is safe" SMS |
| deleteRequested | rider app | |
| viewLog | company app | list of "name – time" entries |
| cancelledFrom | app | `"app"` when cancelled in the app |

## States
`ready → possible_accident → countdown → cancelled`
`countdown → emergency_confirmed → alert_sent → video_uploading → emergency_active → emergency_closed`

## What the Pi must do

1. **Possible accident:** create an incident with `status: "countdown"` and
   `countdownEndsAt = now + 20000`. The rider's app opens the countdown page automatically.
2. **Listen** to that incident document. If `status` becomes `"cancelled"` (app button),
   stop the buzzer and vibration and go back to `ready`. If the physical cancel button
   is pressed, set `status: "cancelled"` and `cancelledFrom: "vest"` yourself.
3. **Countdown ends without cancel:** set `status: "emergency_confirmed"`, get GNSS,
   write the location, **send the SMS first**, then set `smsStatus: "sent"` and
   `status: "alert_sent"`.
4. **Upload videos** to Storage (`videos/<incidentId>/front.mp4` and `rear.mp4`),
   updating progress, then set `status: "emergency_active"`.
5. **Manual SOS from the app:** listen for incidents where `deviceId == <this vest>`,
   `source == "app"` and `smsStatus == "not_sent"`. Treat them like step 3.
6. **Follow-ups:** watch `locationRequestAt` (send a new fix) and `falseAlarmReported`
   (send the "rider is safe" SMS).

## Minimal Python example (Raspberry Pi)

```python
import time, firebase_admin
from firebase_admin import credentials, firestore

firebase_admin.initialize_app(credentials.Certificate("serviceAccount.json"))
db = firestore.client()
DEVICE_ID = "VEST-001"

def now_ms(): return int(time.time() * 1000)

def heartbeat(status):
    db.collection("devices").document(DEVICE_ID).set({
        "online": True, "lastSeen": now_ms(), "battery": status["battery"],
        "mpuOk": status["mpu"], "frontCamOk": status["front"], "rearCamOk": status["rear"],
        "networkOk": status["net"], "gnssOk": status["gnss"],
    }, merge=True)

def start_countdown(rider, peaks):
    ref = db.collection("incidents").document()
    ref.set({
        "riderId": rider["id"], "deviceId": DEVICE_ID, "companyId": rider["companyId"],
        "riderName": rider["fullName"], "riderPhone": rider["phone"],
        "contactName": rider["contactName"], "contactPhone": rider["contactPhone"],
        "healthSnapshot": f"Blood type: {rider.get('bloodType','')}",
        "incidentNumber": "INC-" + ref.id[:6].upper(),
        "type": "auto", "source": "vest", "status": "countdown",
        "createdAt": now_ms(), "countdownEndsAt": now_ms() + 20000,
        "maxAcceleration": peaks["acc"], "maxRotation": peaks["rot"],
        "tiltDeg": peaks["tilt"], "aiScore": peaks["ai"],
        "smsStatus": "not_sent", "frontVideoStatus": "recording", "rearVideoStatus": "recording",
    })
    return ref
```
