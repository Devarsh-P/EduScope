<div align="center">

# 🎓 EduScope

**Crawl. Analyse. Discover.**

A full-stack web application that scrapes online education platforms, analyses course data, and delivers intelligent search, recommendations, and analytics — all in one place.

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white)](https://www.jenkins.io/)

</div>

---

## 📁 Project Structure

```
EduScope/
├── 📂 backend/
│   ├── src/main/java/com/project/edu/
│   │   ├── config/          # CORS configuration
│   │   ├── controller/      # REST API endpoints
│   │   ├── model/           # Data models
│   │   ├── service/         # Business logic, crawler, analysis
│   │   └── util/            # Trie, text utilities
│   ├── data/                # CSV data files (courses, search history, logs)
│   └── pom.xml
├── 📂 frontend/
│   ├── src/
│   │   ├── pages/           # FinalHomePage
│   │   ├── api.js           # API calls to backend
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── vite.config.js
│   └── package.json
├── 🐳 Dockerfile
├── 🐳 .dockerignore
└── ⚙️  Jenkinsfile
```

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version |
|---|---|
| Java | 17+ |
| Maven | 3.x |
| Node.js + npm | 18+ |
| Docker | any recent version |

---

### ▶️ Option 1 — Run Locally (without Docker)

**Start the backend**
```bash
cd backend
mvn spring-boot:run
```
> Runs on `http://localhost:8081`

**Start the frontend** (in a new terminal)
```bash
cd frontend
npm install
npm run dev
```
> Runs on `http://localhost:5173`

---

### 🐳 Option 2 — Run with Docker

**Build the image**
```bash
docker build -t eduscope .
```

**Run the container**
```bash
docker run -p 8081:8081 -p 5173:5173 eduscope
```

Then open **`http://localhost:5173`** in your browser.

> The image is built on `eclipse-temurin:17-jdk-alpine` for a minimal footprint. Both services run inside the same container.

---

## 🌐 API Reference

All endpoints are prefixed with `/api` and served on port `8081`.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Health check |
| `GET` | `/courses` | Get all cached courses |
| `GET` | `/crawl` | Re-crawl platforms and refresh data |
| `POST` | `/search` | Search cached courses by keyword |
| `POST` | `/crawl-search` | Re-crawl then search by keyword |
| `GET` | `/autocomplete?prefix=` | Word completion suggestions |
| `GET` | `/spellcheck?word=` | Spell check suggestions |
| `GET` | `/analytics` | Course analytics data |
| `POST` | `/recommend` | Course recommendations |
| `GET` | `/validate?url=&priceText=` | URL and price regex validation |
| `GET` | `/index-preview` | Inverted index preview |
| `GET` | `/frequency?courseId=&word=` | Word frequency for a course |
| `GET` | `/crawl-summary` | Summary of last crawl |

---

## ⚙️ CI/CD Pipeline

EduScope uses a **Jenkins pipeline** that automatically builds and pushes a new Docker image to Docker Hub on every commit.

```
 Git Commit
     │
     ▼
 Jenkins detects change (polls every 5 min)
     │
     ▼
┌─────────────┐    ┌──────────────┐    ┌─────────────────────┐    ┌──────────┐
│  Checkout   │ →  │ Build Image  │ →  │  Push to Docker Hub │ →  │ Cleanup  │
└─────────────┘    └──────────────┘    └─────────────────────┘    └──────────┘
```

Each build produces two tags:
- `:latest` — always points to the most recent build
- `:<build_number>` — e.g. `:42`, allowing rollback to any previous version

### 🔧 Jenkins Setup

**1. Store your Docker Hub credentials in Jenkins**

Go to `Jenkins → Manage Jenkins → Credentials → Add Credentials`

| Field | Value |
|---|---|
| Kind | Username with password |
| Username | your Docker Hub username |
| Password | your Docker Hub password or access token |
| ID | `dockerhub-credentials` |

**2. Edit the two variables at the top of `Jenkinsfile`**

```groovy
DOCKER_HUB_USER = 'your-dockerhub-username'   // ← your Docker Hub username
IMAGE_NAME      = 'eduscope'                   // ← your desired image name
```

**3. Create a Pipeline job in Jenkins**

- New Item → **Pipeline**
- Definition → `Pipeline script from SCM`
- SCM → `Git` → paste your repository URL
- Script Path → `Jenkinsfile`
- Save → click **Build Now** once to activate the polling trigger

> After the first manual run, Jenkins handles everything automatically — no action needed on new commits.

---

## 🔒 Security Note

Docker Hub credentials are stored exclusively inside Jenkins and are **never hardcoded** in any file in this repository. The `Jenkinsfile` only references the credential by its Jenkins ID, which is safe to commit publicly.

---

<div align="center">

Made with ☕ and Java

</div>