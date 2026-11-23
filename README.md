# SafeWard — Android Application

**SafeWard** is the official Android client for the **Smart Camera-Free Healthcare & Security Monitoring System** (Zarqa University — Faculty of Information Technology, 2026/2027).

The app connects to a secure **MQTT over TLS** backend receiving real-time **Wi-Fi CSI** motion/fall events and medical sensor readings (`MAX30102` heart rate / SpO₂ and body temperature) from ESP32 nodes, providing instant emergency alerts, live patient monitoring, and a **Security Audit Log** without using cameras.

---

## Key Features

- **Role-Based Access & 2FA:** Secure login with Two-Factor Authentication (`2FA`) and tailored dashboards for **Doctor**, **Nurse**, **Family**, and **Security**.
- **Live Ward Dashboard:** Real-time room status (`Normal`, `Movement`, `Fall Detected`, `Offline`) updated via MQTT.
- **Emergency Fall Alarms:** High-priority background notifications and full-screen red alert banners with immediate response actions.
- **Patient Profile & Historical Charts:** Live and historical trends for Heart Rate, SpO₂, Body Temperature, and Resting Breathing Rate, plus `PDF/CSV` report export.
- **Device & Room Management:** Add/edit rooms, pair `ESP32` nodes, and monitor sensor health status.
- **Security Audit Log:** Live feed of blocked `Replay`, `Spoofing`, and `Cross-Layer` anomaly attempts detected by the backend.

---

## Tech Stack

- **Language:** Kotlin / Java (Android Studio)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Networking:** MQTT over TLS (`Paho` / `HiveMQ` client) & HTTPS REST API
- **Data Contract:** Shared `mock_data.json` (Week 1) → Live MQTT Telemetry (Week 2+)

---

## Project Structure

```text
SafeWard-Android/
├── app/
│   ├── src/main/java/com/safeward/
│   │   ├── ui/
│   │   │   ├── auth/          # Login, Role Selection & 2FA (Ahmad)
│   │   │   ├── dashboard/     # Ward Dashboard & Fall Alarm (Mohammad)
│   │   │   ├── patient/       # Patient Profile, Charts, Devices & Export (Qasem)
│   │   │   └── security/      # Security Audit Log (Zeina & Yousef)
│   │   ├── network/           # MQTT/TLS Client & Session Security (Yousef)
│   │   └── model/             # Data Models matching mock_data.json
│   └── src/main/res/
│       └── values/colors.xml  # Unified SafeWard Color System
├── docs/                      # Android Team Plan & UI Mockups
└── mock_data.json             # Week 1 Shared Data Contract
```

---

## Android Team (5 Members)

| Name | Major | Responsibility |
| :--- | :--- | :--- |
| **Zeina Ayman Aziz Nasrallah** | Cybersecurity | Team Leader, Data Contract (`mock_data.json`) & Security Audit Log |
| **Yousef Abdulrahman** | Cybersecurity | MQTT/TLS Client, 2FA Backend Integration & Session Security |
| **Ahmad Al-Najjar** | Cybersecurity | Login, Role-Based Access Control & 2FA UI |
| **Mohammad Jalboush** | Computer Science | Ward Dashboard & Emergency Fall Alert Notifications |
| **Qasem Shqair** | Computer Science | Patient Profile, Charts, Device Management & History Export |

---

## Getting Started (Week 1)

1. Clone this repository and open it in **Android Studio**.
2. Build your assigned UI package under `ui/` using the shared `mock_data.json` file.
3. Apply the unified theme colors from `res/values/colors.xml`.
4. Push your feature branch before the first milestone review (**Saturday, Nov 28, 2026**).
