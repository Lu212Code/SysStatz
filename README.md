# SysStatz

**SysStatz** ist ein leichtgewichtiges plattformübergreifendes Monitoring-Tool zur Anzeige der Systemauslastung von Servern (CPU, RAM, Festplatte, Netzwerk etc.) – optimiert für einfache Integration, Echtzeit-Visualisierung und mobile Nutzung.

---

## 🔍 Features

- 🌐 **Webinterface & API** – Übersichtliche Anzeige aller verbundenen Clients im Browser
- 📈 **Echtzeit-Daten** – CPU, RAM, Festplattenauslastung, Netzwerktraffic und Bootzeit
- 🖥️ **Plattformübergreifend** – Unterstützt Windows, Linux, macOS & Android
- 🔔 **Benachrichtigungen** – Automatische Alerts per E-Mail bei definierter Auslastung
- 💡 **Dark Mode** – Modernes UI-Design mit dunklem Theme
- 🔐 **Client-Identifikation** – Jeder Client besitzt eindeutigen Namen zur Wiedererkennung
- ⚙️ **Einfache Konfiguration** – In wenigen Sekunden einsatzbereit

---

## 🚀 Schnellstart

### 📥 Server starten

```bash
git clone https://github.com/DEIN-NAME/sysstatz.git
cd sysstatz/server
./mvnw spring-boot:run
```

Webinterface aufrufen: [http://localhost:8080](http://localhost:8080)

### 📦 Client starten (Java)

```bash
cd sysstatz/client
java -jar SysStatzClient.jar
```

Konfiguration erfolgt automatisch beim ersten Start (Clientname, Server-IP, Intervall).

---

## 📲 Android-App

Die Android-App zeigt die aktuellen Werte mobil an. Funktionen:

- Anzeige der aktuellen Auslastung
- Automatische Verbindung zur letzten bekannten Serveradresse

📦 **Download APK**: [Releases](https://github.com/Lu212Code/sysstatz/releases)

---

## ⚙️ Konfiguration

### Server

Auf der Weboberfläche unter Einstellungen.

### Client

In `config.properties` (wird automatisch erstellt):

```properties
server=192.168.1.100:12345
clientName=Local
interval=5
```

---

## 📧 Benachrichtigungen (Alerting)

Meldungen können im Webinterface konfiguriert werden:

- Typ: CPU / RAM / Disk
- Grenzwert in %
- Ziel-Email-Adresse

Wenn der Grenzwert überschritten wird, wird eine Mail verschickt (SMTP-Konfiguration erforderlich).

---

## 🧠 Architektur

```text
[Client(s)] → [Webserver (Spring Boot)] → [Webinterface / API]
```

Kommunikation erfolgt über ein leichtgewichtiges Protokoll via TCP.

---

## 📜 Lizenz

MIT License – frei zur Nutzung, Modifikation und Verbreitung.

---

## 🤝 Mitwirken

Pull Requests sind willkommen! Für größere Änderungen bitte zuerst ein Issue eröffnen.
