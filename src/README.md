# Distributed Chat System me Rooms dhe Message Queue

## 1. Përshkrimi i projektit
Ky projekt implementon një **Distributed Chat System** në Java duke përdorur arkitekturë **client-server**, **multithreading**, **sockets**, **blocking queue** dhe **JavaFX** për pjesën grafike të klientit.

Përdoruesit mund të:
- regjistrohen me username unik,
- krijojnë rooms të reja,
- bashkohen në rooms ekzistuese,
- largohen nga room-i aktual,
- dërgojnë mesazhe te anëtarët e room-it.

Serveri menaxhon klientët dhe room-et, ndërsa secili room ka thread-in e vet dhe një `BlockingQueue` për menaxhimin e mesazheve.

---

## 2. Struktura e projektit

```
src/
├── client/
│   ├── ChatClient.java
│   └── ServerListener.java
├── common/
│   ├── Command.java
│   ├── CommandType.java
│   └── Message.java
├── gui/
│   └── ChatApp.java
└── server/
    ├── ChatRoom.java
    ├── ChatServer.java
    ├── ClientHandler.java
    └── ServerMain.java
```

---

## 3. Arkitektura e sistemit

Sistemi ndahet në 4 pjesë kryesore:

### 3.1 Client Side
- **ChatApp**: ndërfaqja grafike me JavaFX.
- **ChatClient**: menaxhon lidhjen me serverin me `Socket`, `ObjectInputStream` dhe `ObjectOutputStream`.
- **ServerListener**: callback interface që njofton GUI-në kur vijnë komanda nga serveri ose kur shkëputet lidhja.

### 3.2 Server Side
- **ChatServer**: hap `ServerSocket`, pranon klientë të rinj dhe menaxhon listën e klientëve dhe room-eve.
- **ClientHandler**: krijohet një instancë për çdo klient. Lexon komandat e ardhura dhe kryen veprimet përkatëse.
- **ChatRoom**: përfaqëson një room chat-i. Ka thread-in e vet dhe një `LinkedBlockingQueue<Message>` për radhitjen e mesazheve.

### 3.3 Shared Common Package
- **Command**: objekti kryesor që përdoret për komunikim ndërmjet klientit dhe serverit.
- **CommandType**: enum me tipat e komandave (`REGISTER`, `CREATE_ROOM`, `JOIN_ROOM`, `LEAVE_ROOM`, `SEND_MESSAGE`, etj.).
- **Message**: përfaqëson mesazhin e chat-it me dërgues, përmbajtje, timestamp dhe room.

### 3.4 Modeli i komunikimit
1. Klienti lidhet me serverin.
2. Klienti dërgon komandë `REGISTER`.
3. Serveri e regjistron klientin ose kthen gabim nëse username ekziston.
4. Klienti mund të krijojë ose të bashkohet në një room.
5. Mesazhet dërgohen te serveri si `Command` me tip `SEND_MESSAGE`.
6. `ClientHandler` i shton mesazhet në `BlockingQueue` të `ChatRoom`.
7. Thread-i i `ChatRoom` i merr mesazhet nga queue dhe i shpërndan te të gjithë anëtarët e room-it.

---

## 4. Udhëzime për ekzekutim

### 4.1 Kërkesat
- Java JDK 17 ose më e re
- JavaFX SDK (vetëm për klientin me GUI)
- IDE si IntelliJ / Eclipse / VS Code, ose terminal

### 4.2 Kompilimi i serverit dhe klasave bazë
Ekzekutimi bëhet me butonin Run në IDE.

Serveri starton në portin **5000**.

### 4.4 Kompilimi i klientit GUI
Për shkak se `ChatApp.java` përdor JavaFX, duhet të kesh JavaFX SDK të konfiguruar. 
Mund të shikoni këtë video në Youtube për ta konfiguruar në Eclipse: https://youtu.be/SY1yXAnyFqo?si=jPYUoijLzYtrEwN1

### 4.5 Ekzekutimi i klientit GUI
Ekzekutimi bëhet me butonin Run në IDE.
---

## 5. Funksionalitetet e implementuara
- Regjistrim i klientit me username unik
- Krijimi i room-eve
- Bashkimi në room-e ekzistuese
- Largimi nga room-i
- Dërgimi i mesazheve brenda room-it
- Broadcast i mesazheve për të gjithë anëtarët e room-it
- Mesazhe sistemore për join/leave
- Listim i room-eve të disponueshme
- GUI bazike me JavaFX

---

## 6. Koncepte të përdorura

### Client/Server Programming
Komunikimi realizohet përmes `Socket` në anën e klientit dhe `ServerSocket` në anën e serverit.

### Multithreading
- Serveri krijon një thread të veçantë për çdo klient (`ClientHandler`).
- Çdo room ekzekutohet në thread-in e vet (`ChatRoom`).
- Klienti ka një thread listener për të dëgjuar komandat që vijnë nga serveri.

### Synchronization
- `ReentrantLock` përdoret për mbrojtjen e listës së klientëve dhe room-eve në server.
- `ReentrantLock` përdoret edhe për listën e anëtarëve të room-it.

### Blocking Queue
- `LinkedBlockingQueue<Message>` përdoret për të ruajtur mesazhet që dërgohen në room.
- Kjo e bën trajtimin e mesazheve më të sigurt dhe më të organizuar mes thread-eve.

### Network Communication
- Objektet `Command` dhe `Message` dërgohen si objekte serializable përmes stream-eve.

---

## 7. Problemet e hasura

### 7.1 Package mismatch
Nëse fajllat Java nuk vendosen në folderat që përputhen me `package` deklarimet (`client`, `server`, `common`, `gui`), del gabimi:
- `The declared package ... does not match the expected package ...`

Kjo u zgjidh duke i organizuar fajllat sipas strukturës së saktë të paketave.

### 7.2 JavaFX runtime components are missing
Nëse klienti GUI ekzekutohet pa JavaFX SDK të konfiguruar, del gabimi:
- `JavaFX runtime components are missing, and are required to run this application`

Zgjidhja është të konfigurohet `--module-path` dhe `--add-modules` me JavaFX SDK.

### 7.3 Sinkronizimi i room-eve dhe klientëve
Për shkak se shumë klientë dhe room-e mund të ndryshohen paralelisht, është dashur të përdoren `ReentrantLock` për shmangien e race conditions.

### 7.4 Kufizime aktuale të implementimit
- Lista e room-eve nuk rifreskohet automatikisht te të gjithë klientët sa herë krijohet një room i ri.
- `currentRoom` në GUI përditësohet menjëherë kur përdoruesi klikon join, para se serveri të konfirmojë suksesin.
- Nuk është implementuar login me fjalëkalim, histori mesazhesh, apo private chat.
- Thread-et e room-eve nuk kanë mekanizëm shutdown të dedikuar kur një room mbetet bosh.

---

## 8. Diagrami i sistemit
Diagrami i sistemit ndodhet te fajlli:

- `system_diagram.png`

Ai paraqet marrëdhëniet ndërmjet `ChatApp`, `ChatClient`, `ChatServer`, `ClientHandler`, `ChatRoom`, dhe klasave të paketës `common`.

---

## 9. Përfundim
Ky projekt demonstron qartë përdorimin e koncepteve kryesore të sistemeve të distribuara në Java: komunikim në rrjet, ekzekutim paralel me threads, sinkronizim, radhë mesazhesh dhe menaxhim të room-eve. Arkitektura është e ndarë mirë në client, server, common dhe GUI, gjë që e bën projektin të kuptueshëm dhe të zgjerueshëm për funksionalitete të tjera në të ardhmen.
