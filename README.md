# SysStatz

**SysStatz** ist ein leichtgewichtiges plattformübergreifendes Monitoring-Tool zur Anzeige der Systemauslastung von Servern (CPU, RAM, Festplatte, Netzwerk etc.) – optimiert für einfache Integration, Echtzeit-Visualisierung und mobile Nutzung.

**Website:** sysstatz.de

---

## 🔍 Features

- 🌐 **Webinterface & API** – Übersichtliche Anzeige aller verbundenen Clients im Browser
- 📈 **Echtzeit-Daten** – CPU, RAM, Festplattenauslastung, Netzwerktraffic, Bootzeit, Uptime, Swap, Auslastung pro Kern und CPU-Temperatur
- 🖥️ **Plattformübergreifend** – Unterstützt Windows und Linux (Teilweise auch MacOS)
- 🔔 **Benachrichtigungen** – Automatische Alerts per E-Mail und Weboberfläche bei definierter Auslastung
- 💡 **Verschiedene Themes** – Mehrere wählbare Website Themes.
- 🔐 **Client-Identifikation** – Jeder Jeder Client besitzt einen eindeutigen Namen zur Wiedererkennung.
- ⚙️ **Einfache Konfiguration** – In wenigen Sekunden einsatzbereit
- 👨‍💻 **Daten Analyse** - Verschiedene Analyse Möglichkeiten wie Langzeitanalyse oder AI-Analyse
- 🪪 **User Management** - Unbegrenzt viele Benutzer möglich.
- 🔒 **Sicherheit** - Jeder Server hat einen eigenen Keystore und die Server - Client verbindung ist verschlüsselt. Clients benötigen außerdem einen Key um sich zu verbinden.

---

## 🚀 Schnellstart

### 📥 Server installieren (Java 17+ benötigt)

```bash
bash <(curl -s http://sysstatz.de/install/install_sysstatz.sh)
```

Webinterface aufrufen: [http://localhost:8080](http://localhost:8080)

### 📦 Client installieren (Java 17+ benötigt)

```bash
bash <(curl -s http://sysstatz.de/install/install_sysstatz_client.sh)
```

Konfiguration erfolgt automatisch beim ersten Start (Clientname, Server-IP, Intervall).

---

## ⚙️ Konfiguration

### Server

Auf der Weboberfläche unter Einstellung und im config.txt.

---

## 📧 Benachrichtigungen (Alerting)

Meldungen können im Webinterface konfiguriert werden:

- Typ: CPU / RAM / Disk
- Grenzwert in %
- Ziel-Email-Adresse

Wenn der Grenzwert überschritten wird, wird eine Mail verschickt und aif der Weboberfläche eine Meldung angezeigt.

---

## 📜 Lizenz

MIT License – frei zur Nutzung, Modifikation und Verbreitung.

---

## 🤝 Mitwirken

Pull Requests sind willkommen! Für größere Änderungen bitte zuerst ein Issue eröffnen.


## Shutdown & Reboot erlauben

Damit das Programm den Rechner herunterfahren oder neu starten darf, müssen die Befehle ohne Passwort über `sudo` ausführbar sein.

### Linux
1. Öffne die sudoers-Datei:
   ```bash
   sudo visudo
Füge am Ende eine Zeile ein (ersetze <username> durch deinen Benutzernamen):

text
Code kopieren
<username> ALL=(ALL) NOPASSWD: /usr/bin/systemctl poweroff, /usr/bin/systemctl reboot, /sbin/shutdown, /sbin/reboot
macOS
Öffne die sudoers-Datei:

bash
Code kopieren
sudo visudo
Ergänze folgende Zeile (wieder <username> anpassen):

text
Code kopieren
<username> ALL=(ALL) NOPASSWD: /sbin/shutdown, /usr/sbin/shutdown, /sbin/reboot, /usr/sbin/reboot
⚠️ Hinweis
Änderungen an der sudoers-Datei sollten mit Vorsicht vorgenommen werden.

Nur die exakt benötigten Befehle freigeben.

Teste die Einrichtung z. B. mit:

bash
Code kopieren
sudo -n shutdown -h now
→ Wenn keine Passwortabfrage kommt, ist alles korrekt eingerichtet.
