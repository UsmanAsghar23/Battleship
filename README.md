# ⚓ Battleship Multiplayer Game (Java/JavaFX)

A visually engaging, multiplayer **Battleship** game built in **Java** with a modern **JavaFX GUI**. Players can compete online or against AI, place ships, launch attacks, and chat — all in a polished, animated interface.

---

## 📸 Screenshots

### Login Screen
<img width="751" alt="Screenshot 2025-06-10 at 7 17 41 PM" src="https://github.com/user-attachments/assets/269169c2-e6d8-4527-8b98-30c0cd48767d" />

### Game Mode Selection
<img width="747" alt="Screenshot 2025-06-10 at 7 17 50 PM" src="https://github.com/user-attachments/assets/629a297a-a722-4229-a151-d7fa29d59cfa" />

### Multiplayer Gameplay
<img width="794" alt="Screenshot 2025-06-10 at 7 16 53 PM" src="https://github.com/user-attachments/assets/9d394cac-3b75-469c-b8ef-a10cfdc4ba78" />

---

## 🎮 Features

- 🌐 **Online Multiplayer** – Connect and play against real users
- 🤖 **Play vs AI** – Practice against a basic AI
- 🧩 **JavaFX Interface** – Clean, responsive design with animated visuals
- 🔵 **Interactive Boards** – Click to attack, color-coded hits/misses
- 💬 **Live Chat** – Message your opponent during the game
- 🧠 **Turn-Based System** – Automatic turn tracking and move validation

---

## 🧪 Technologies Used

- **Java 17+**
- **JavaFX** for GUI (FXML, CSS, and controllers)
- **Socket Programming** (TCP)
- **Multithreading** for client-server concurrency
- **Object Serialization** for messaging

---

## 📁 Project Files

```bash
.
├── Client.java          # Handles client logic and server communication
├── Server.java          # Game server handling player sessions
├── GuiClient.java       # JavaFX-based game UI for the client
├── GuiServer.java       # JavaFX-based UI for hosting the server
├── board.java           # Core game board logic (ship layout, hit tracking)
├── Message.java         # Serializable class for network communication
├── README.md
