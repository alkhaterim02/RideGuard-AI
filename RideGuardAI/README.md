# RideGuard AI – Android app (Java)

Native Android app for the RideGuard AI accident-detection vest.
Java, Android Studio project, Firebase backend, minimum Android 7.0 (API 24).

The app has **two modes**, chosen automatically when it is built:

| Mode | When | What it does |
|---|---|---|
| **Demo mode** | `app/google-services.json` is missing | Sample riders, vests and incidents. Includes a "Simulate accident" button that runs the full countdown → SMS → video upload flow. No hardware or Firebase needed. |
| **Real mode** | `app/google-services.json` is present | Uses Firebase Auth, Firestore, Storage and Cloud Messaging, and talks to the real vest. |

---

## 1. Build the APK without installing Android Studio (GitHub)

1. Create a free account at **github.com** and click **New repository** (any name, e.g. `rideguard-app`).
2. On the new repository page click **uploading an existing file**, then drag in **all the files
   and folders** from this project (unzipped). Click **Commit changes**.
3. Make sure the file `.github/workflows/build-apk.yml` was uploaded. Some computers hide
   folders whose names start with a dot. If it is missing: click **Add file → Create new file**,
   type the name `.github/workflows/build-apk.yml`, paste in the contents of that file, and commit.
4. Open the **Actions** tab. The build starts by itself (takes about 5–8 minutes).
5. When it shows a green tick, open the run and download **RideGuardAI-apk** under *Artifacts*.
   Unzip it to get `app-debug.apk`.
6. Copy the APK to your Android phone, open it, and allow "Install unknown apps" when asked.

If the build shows a red cross, open the failed step, copy the error, and ask for a fix.

## 2. Demo accounts

| Role | Login | Password |
|---|---|---|
| Rider | `rider@demo.com` (or phone `0500000001`, or rider ID `R-1001`) | `demo123` |
| Rider 2 | `rider2@demo.com` | `demo123` |
| Company | `company@demo.com` | `demo123` |

Try this: log in as the rider → **Simulate accident (demo)** → watch the countdown →
press **CANCEL ALERT**, or let it reach 0 to see the emergency page, SMS status and
video uploads. Then log out, log in as the company, and open the emergency, map and videos.
Demo data resets every time the app is closed.

## 3. Switch to real mode (Firebase)

1. Go to **console.firebase.google.com** → *Add project*.
2. *Add app → Android*, package name **`com.rideguard.ai`**. Download `google-services.json`.
3. Either put the file in the `app/` folder, **or** (better for a public repo) add it as a GitHub
   secret: repository *Settings → Secrets and variables → Actions → New repository secret*,
   name `GOOGLE_SERVICES_JSON`, value = the whole file contents. Re-run the build.
4. In Firebase enable: **Authentication → Email/Password**, **Firestore Database**,
   **Storage**, **Cloud Messaging**.
5. Firestore → *Rules*: paste `firestore.rules` from this project and publish.
6. Create a company account: *Authentication → Add user* (email + password), copy its UID,
   then in Firestore create document `users/<UID>` with fields
   `role: "company"`, `companyId: "SWIFT01"`, `displayName: "Swift Admin"`.
7. Riders register in the app using that company code (`SWIFT01`).
8. Give the Raspberry Pi a service-account key and follow `docs/FIRESTORE_CONTRACT.md`.

In real mode riders log in with their **email** (phone / rider-ID login works in demo mode only).
Push notifications when the app is closed need a small Cloud Function that sends an FCM
message (data keys: `title`, `body`, `incidentId`) to the company users' `fcmToken`s
when an incident becomes `alert_sent`.

## 4. Screens (matches the project document)

| Document section | Screen / file |
|---|---|
| 13.1 Welcome & role selection | `WelcomeActivity` |
| 13.2 / 14.1 Login | `LoginActivity` |
| 13.3 Registration | `RegisterActivity` |
| 13.4 Rider home (vest status, Start Ride, SOS) | `RiderHomeActivity` |
| 13.5 Health profile | `HealthProfileActivity` |
| 13.6 Countdown | `CountdownActivity` |
| 13.7 Emergency confirmed | `EmergencyActivity` |
| 13.8 / 14.6 History | `HistoryActivity` |
| 13.9 / 14.4 Details, map, front & rear video | `IncidentDetailActivity`, `VideoPlayerActivity` |
| 14.2 / 14.3 Company dashboard & active emergencies | `CompanyDashboardActivity` |
| 14.5 Rider management | `RiderManagementActivity` |

HCI choices (section 15): buttons are at least 64–72 dp tall (glove friendly), the cancel
button is 140 dp, every status uses text + symbol + colour (never colour alone), SOS needs a
2-second hold, and closing an emergency asks for confirmation.

## 5. Project structure

```
app/src/main/java/com/rideguard/ai/
  model/      Rider, Incident, DeviceStatus, IncidentStatus, UserSession
  data/       DataRepository (interface), DemoRepository, FirebaseRepository, SessionManager
  ui/         all screens + list adapters
  util/       UiUtils, FormUtils, NotificationHelper, Extras
  messaging/  RideGuardMessagingService (push notifications)
firestore.rules              Firestore security rules
docs/FIRESTORE_CONTRACT.md   data format the Raspberry Pi must follow
.github/workflows/           cloud build that produces the APK
```

## 6. Opening in Android Studio later

*File → Open* → select this folder. If Android Studio asks to create a Gradle wrapper or
sync, accept. Use Gradle 8.7 and JDK 17.
