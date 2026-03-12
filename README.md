# 🍛 KhanaBook: Built for the Heart of the Shop

Hey there! Welcome to the engine room of **KhanaBook**! 👋

Running a food stall, cafe, or a bustling restaurant is hard enough. KhanaBook is here to make the "business side" of things feel effortless. It is a companion for the people who keep our neighborhoods fed, whether it's the local biryani corner or a breakfast joint.

This repository contains both the **KhanaBook Lite (Android App)** and the **KhanaBook Server**.

---

## 🔌 Why "Offline-First"? Because Life Happens.

Most billing apps break the moment the Wi-Fi drops. We built this differently. We know how it goes—the kitchen is heating up, orders are flying in, and suddenly... the internet goes out. In a busy shop, that shouldn't be a crisis. 

We built KhanaBook to be **offline-first**. It works whether you're in a basement cafe or a street-side stall.
- **⚡ Works Everywhere:** Create bills instantly without worrying about a signal.
- **🔄 Smart Syncing:** The moment you're back online, the app quietly syncs everything (bills, menu changes, logs) to the server. Your data is safely recorded without you ever having to hit "refresh."

---

## ✨ Features That Actually Matter

We focused on the things that actually matter when you're in the thick of it:

- **🥘 Your Menu, Your Way:** Organize your dishes into simple categories. We handle variants and pricing in a few taps. Changing a price or adding a new seasonal dish takes seconds.
- **🧾 Simple, Honest Billing:** GST, variants, and payments are handled in a flow that feels natural. Send professional-looking invoices directly to your customer's WhatsApp, or print them on the spot with a Bluetooth thermal printer.

- **🔒 Your Data is Yours:** We use **Secure Multi-Tenancy** on the server and local **SQLCipher** (AES-256 encryption) on the app. Every restaurant’s data is locked away securely.

---

## 📱 Android App (KhanaBook Lite)

A mobile Point of Sale (POS) app that doesn't need the internet to work and keeps your data exactly where it belongs: with you.

### 🛠️ What's Under the Hood?
- **Kotlin & Jetpack Compose:** A modern, smooth UI.
- **Local-First Database:** Room with SQLCipher for local encrypted storage.
- **Reliable Sync:** Android's WorkManager and `MasterSyncWorker` handle background syncing.

### 🚀 Getting Started
1. **Requirements:** You'll need **Android Studio (Ladybug or newer)** and **JDK 17**.
2. **Setup Secrets:** Create a `local.properties` file in the root folder and add your Meta/WhatsApp credentials:
   ```properties
   # Meta Cloud API for WhatsApp receipts
   META_ACCESS_TOKEN=your_token_here
   WHATSAPP_PHONE_NUMBER_ID=your_id_here
   WHATSAPP_OTP_TEMPLATE_NAME=verification_otp
   ```
3. **Build & Run:** Open the project in Android Studio, sync Gradle, and run on a real device (best for testing Bluetooth printing).

---

## 🖥️ Backend Server

The engine that receives synced offline data and handles multi-tenant cloud storage.

### 🛠️ The Craftsmanship (Tech Stack)
We chose our tools because they are reliable—just like the equipment in a professional kitchen:
- **Java & Spring Boot:** For a foundation as solid as a cast-iron skillet.
- **PostgreSQL:** To keep your records safe and organized for years to come.
- **JWT Security:** Because your trust—and your customers' trust—is everything.

---

## 📜 License & Privacy

**KhanaBook Server:** MIT License. See the `server/LICENSE` file for more details.
**KhanaBook Lite (Android):** Internal/private project. All rights reserved.

Happy Billing! ☕🥘
