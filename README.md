# EduLink Ecosystem: Advanced Multi-Platform LMS

EduLink is a state-of-the-art Learning Management System (LMS) designed to bridge the gap between web-based learning and desktop efficiency. Built with a robust **Symfony 7** backend and a high-performance **JavaFX** desktop client, the platform leverages **Generative AI** and **Biometric Security** to provide a truly integrated educational experience.

---

## 🏗️ Project Architecture
EduLink operates as a synchronized ecosystem:
-   **Web Platform**: The core management hub for administration, AI content generation, and social interaction.
-   **Desktop Application**: A productivity-focused client for fast note-taking, offline-first interactions, and real-time synchronization.
-   **Shared Infrastructure**: Both platforms communicate via a unified MySQL/MariaDB database, ensuring that progress, notes, and XP are consistent everywhere.

---

## 🚀 Key Modules & Features

### 🔐 Security & Identity
*   **Face ID Authentication**: Password-less biometric login using `face-api.js` and TensorFlow models.
*   **Unified Account Management**: Single-sign-on (SSO) experience across Web and Java platforms.
- **Role-Based Permissions**: Granular access control for Students, Tutors, and Administrators.

### 📝 Integrated Journal & Notebook System (Synchronized)
*   **Cross-Platform Sync**: Create a note on the Java Desktop app and instantly see it on the Symfony Web Dashboard.
*   **AI Sentiment Analysis**: Automated analysis of journal entries to track student well-being and engagement.
*   **Unified "All Notes" View**: A simplified, stream-based view of all personal entries regardless of their parent notebook.
*   **Smart Categorization**: Shared category system for organizing notes across devices.

### 🏆 Challenge Hub & Gamification
*   **AI Mission Generation**: Administrators can use **Groq/Gemini AI** to automatically generate practical missions based on challenge goals.
*   **XP Economy**: Students earn XP points for completing challenges, tracked via a "Wallet" system.
*   **Admin Dashboard**: Real-time monitoring of challenge participation, mission validation, and reward distribution.
*   **Proof Submission**: Multi-step validation where students submit proofs (text/files) for administrative review.

### 🤖 AI Study Companion
*   **PDF Summarizer**: Instant bullet-point summaries of long academic documents.
*   **Quiz Generator**: Transform lecture notes into interactive MCQs.
*   **Video Scripting**: AI-assisted conversion of complex text into structured video scripts.

### 📊 Assistance & Statistics Dashboard
*   **Top Tutors Analytics**: Real-time leaderboard of the most active and highly-rated tutors.
*   **Resolution Tracking**: Visual breakdown of help request statuses and category distribution.
*   **Financial Ledger**: Full transparency for point transactions, refunds, and grants.

---

## 🛠️ Technology Stack

### Backend (Web)
- **Framework**: Symfony 7.4 / PHP 8.2+
- **ORM**: Doctrine (MySQL/MariaDB)
- **Security**: Symfony Security Bundle with Biometric hooks

### Frontend (Web)
- **Engine**: Twig + Vanilla CSS (Modern design tokens)
- **Interactivity**: Vanilla JavaScript (ES6+)
- **Icons**: Lucide & Remix Icons

### Desktop Client
- **Platform**: Java 17+ / JavaFX
- **Data Access**: JDBC with shared connection pooling
- **UI Architecture**: FXML + MVC Pattern

### AI Integration
- **LLMs**: Google Gemini API, Groq AI
- **Computer Vision**: Face-API.js / TensorFlow.js

---

## 📦 Installation & Sync Setup

### 1. Database Setup
Ensure both projects point to the same database:
```env
# Symfony (.env)
DATABASE_URL="mysql://root:@127.0.0.1:3306/edulinkpi"

# Java (MyConnection.java)
private String url = "jdbc:mysql://localhost:3306/edulinkpi";
```

### 2. Web Setup
```bash
composer install
php bin/console doctrine:migrations:migrate
# Download AI Models
python download_models_v2.py
# Start Server
symfony server:start
```

### 3. Desktop Setup
1. Open the `piweb-desktop` project in IntelliJ IDEA.
2. Ensure the **JavaFX SDK** is configured in project structure.
3. Run `MainApp.java` (Ensure your Maven dependencies are loaded).

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
**EduLink** — *Learning without boundaries.*
