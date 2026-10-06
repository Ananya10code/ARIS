# ARIS — Autonomous Reliability & Intelligence System

[![Java](https://img.shields.io/badge/Java-24-blue.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.12-yellow.svg)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110+-teal.svg)](https://fastapi.tiangolo.com/)
[![scikit-learn](https://img.shields.io/badge/scikit--learn-Isolation%20Forest-orange.svg)](https://scikit-learn.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](LICENSE)

**ARIS** is an autonomous site reliability engineering (SRE) and observability platform designed to monitor applications, detect abnormal behavior, diagnose incidents, and provide developer-approved remediation suggestions.

It continuously collects multi-dimensional telemetry, trains unsupervised machine learning models (**Isolation Forest**) on baseline behavior, identifies abnormal patterns, extracts stack traces directly from runtime application logs, pinpoints the root-cause function and line number, and generates proposed Before $\rightarrow$ After code patches for developer review and approval. Live monitoring can then verify whether the affected endpoint has recovered.

---

## ⚡ Closed-Loop Reliability Architecture

$$\mathbf{Historical\ Telemetry} \longrightarrow \mathbf{Feature\ Engineering} \longrightarrow \mathbf{Isolation\ Forest} \longrightarrow \mathbf{Anomaly\ Score} \longrightarrow \mathbf{Incident} \longrightarrow \mathbf{AI\ Root\ Cause} \longrightarrow \mathbf{Fix\ Recommendation}$$

```text
[Live Application Telemetry]
  ├── Latency (mean, peak, latest ms)
  ├── Request Rate (throughput in req/s)
  ├── Error Rate (HTTP 4xx / 5xx ratio)
  ├── Host Utilization (CPU % & Memory %)
  ├── Network Timeouts (HTTP 0 socket drops)
  └── Quota Violations (HTTP 429 Rate-Limits)
               │
               ▼
[Feature Engineering & Isolation Forest]
  ├── Builds the telemetry feature vector used by the trained model
  ├── Scores current behavior against learned baseline patterns
  └── Produces an anomaly score and supporting deviation evidence
               │
               ▼
[Incident Detection]
  ├── Emits unique Incident ID (INC-xxx) bound to Project ID
  ├── Records anomaly score and application-level confidence display
  └── Assigns severity rating (CRITICAL, HIGH, MEDIUM)
               │
               ▼
[AI Root Cause & Code Localization]
  ├── Tails project runtime log (logs/aris.log)
  ├── Parses unhandled exception stack trace
  └── Identifies failing class, method, source file, and line number
               │
               ▼
[Fix Recommendation & Code Patching]
  ├── Generates step-by-step SRE remediation checklist
  ├── Produces a proposed Before → After code diff for supported cases
  ├── Developer reviews and approves the proposed patch
  └── Live prober verifies health normalization → Incident RESOLVED
```

---

## 🚀 Key Features

* **Dedicated Authentication & Multi-Tenant User Isolation**:
  * **Sign In Page**: Clean, modern interface supporting login via User ID or Email ID and Password, with a 1-click **Quick Admin** auto-fill button.
  * **Candidate Registration**: Dedicated onboarding form requiring Full Name, Email ID, and Password.
  * **Strict Project Isolation**: Enforced at the Spring Security JWT filter and database layer. New candidates only see their own projects and cannot view or access Admin projects. Direct unauthorized access attempts return `HTTP 403 Forbidden`.
* **Dynamic Multi-Project Architecture (Zero Hardcoding)**: Supports arbitrary target projects and microservices created at runtime. Every project receives an auto-generated API Key (`aris_live_...`), separate telemetry streams, and dedicated code diagnostics.
* **Strict Project Ownership & JWT Security**: Projects belong strictly to the authenticated user's ID resolved from the Spring Security JWT context. Cross-user access to projects, metrics, files, and patches is prohibited.
* **Multi-Dimensional Anomaly Detection**: Uses an Isolation Forest model over application and host telemetry features, including latency, request rate, error rate, CPU, memory, timeouts, and HTTP 429 rate-limit behavior.
* **Interactive Live Prober**: Includes an on-demand `[▶ Test Now]` console that sends probe requests to endpoints and prints response snippets, round-trip latency, and status codes in real time.
* **Integrated Source Explorer & Code Editor**: In-browser file explorer allowing developers to inspect source code and jump directly to the exact line number of captured stack traces.
* **Recovery Verification**: Live monitoring/probing verifies when an affected endpoint returns to a healthy response after a patch is applied or a simulated fault is cleared, automatically transitioning incidents from `ACTIVE` to `RESOLVED`.

---

## 🛠️ Technology Stack

| Layer | Technology | Description |
| :--- | :--- | :--- |
| **Backend & APM** | **Java 24** / **Spring Boot 4.1.1** | High-performance APM platform, probe scheduler, JWT security, and file patcher. |
| **Database** | **H2 Database (In-Memory)** | Zero-dependency standalone database running in PostgreSQL compatibility mode. |
| **ML Engine** | **Python 3.12** / **scikit-learn** | Unsupervised Isolation Forest model for multidimensional anomaly scoring. |
| **AI Web Service** | **FastAPI** / **Uvicorn** | Asynchronous REST microservice exposing anomaly analysis, history, and patch synthesis. |
| **Frontend UI** | **Vanilla HTML5 / ES6+ JavaScript** | Zero-framework, high-speed SPA with dedicated Login, Registration, and Multi-Tenant Dashboard. |
| **Visualization** | **Chart.js v4.4.1** | Dynamic multi-series time-series charts rendering real-time API latency trends. |

---

## 📁 Repository Structure

```text
ARIS/
├── ai-engine/                               # Python ML & Anomaly Detection Microservice
│   ├── models/                              # Serialized model weights (.joblib)
│   │   ├── api.joblib                       # Trained API anomaly model & baselines
│   │   └── host.joblib                      # Trained Host CPU/RAM model
│   ├── engine.py                            # Core analysis engine, log parser, patch generator
│   ├── main.py                              # FastAPI REST service (/api/analysis, /api/history)
│   ├── train.py                             # Isolation Forest training script & feature engineer
│   └── requirements.txt                     # Python dependencies (fastapi, scikit-learn, joblib)
├── src/main/java/com/aris/                  # Spring Boot Backend Source Code
│   ├── auth/                                # JWT Authentication & User management
│   │   ├── AuthService.java                 # Login & Registration with case-insensitive ID support
│   │   ├── User.java                        # User entity (ID, name, email, password, role)
│   │   ├── Role.java                        # Role definitions (ADMIN, DEVELOPER)
│   │   ├── repository/UserRepository.java   # User persistence and lookup methods
│   │   └── controller/AuthController.java   # POST /api/auth/login, POST /api/auth/register
│   ├── config/                              # Configuration & Seeders
│   │   └── DemoSeeder.java                  # Seeds Admin (rakshit) & E-Commerce / Demo projects
│   ├── ecom/                                # Target E-Commerce Microservices
│   │   ├── OrderController.java             # /api/orders (Order processing section)
│   │   ├── PaymentController.java           # /api/payment (Payment gateway with fault injection)
│   │   └── ProductController.java           # /api/products (Product catalog section)
│   ├── incident/                            # Incident tracking & lifecycle
│   │   ├── Incident.java                    # Incident entity (ACTIVE / RESOLVED)
│   │   └── controller/IncidentController.java
│   ├── monitor/                             # Monitor configurations and intervals
│   ├── probe/                               # Telemetry & System Endpoints
│   │   ├── DashboardController.java         # /api/dashboard (Monitors, charts, host stats)
│   │   ├── DemoController.java              # /api/demo (Latency spike & flaky tests)
│   │   ├── ProbeScheduler.java              # Background HTTP probe worker
│   │   └── WorkspaceController.java         # /api/workspace (Overview, files, patching)
│   ├── project/                             # Project ownership & metadata
│   │   ├── Project.java                     # Project entity (owner, apiKey, sourcePath, logPath)
│   │   ├── ProjectService.java              # Scoped lookups and ownership validation
│   │   └── controller/ProjectController.java# GET /api/projects, POST /api/projects
│   └── security/                            # Spring Security & Authorization
│       ├── ArisUserDetailsService.java      # UserDetailsService with case-insensitive resolution
│       ├── JwtAuthenticationFilter.java     # Validates Authorization: Bearer tokens
│       ├── JwtService.java                  # Token generation and claim extraction
│       ├── SecurityConfig.java              # Filter chain and public route rules
│       └── SecurityUtils.java               # Resolves authenticated user from JWT context
├── src/main/resources/
│   ├── application.yml                      # Application settings & logging output configuration
│   └── static/
│       └── index.html                       # Frontend SPA (Login, Registration, Dashboard & Dock)
├── logs/
│   └── aris.log                             # Runtime application log (scanned for stack traces)
├── ARCHITECTURE.md                          # Comprehensive technical design specification
├── pom.xml                                  # Maven dependencies & build configuration
└── README.md                                # Project documentation
```

---

## 🚦 Getting Started

### Prerequisites

* **Java JDK 24** (or OpenJDK 21+)
* **Python 3.10+** (with virtual environment support)
* **Maven 3.9+** (or use the included `./mvnw.cmd` wrapper)

---

### Step 1: Start the Spring Boot Backend

Open a terminal in the project root:

```bash
# On Windows PowerShell
$env:JAVA_HOME = "C:\Users\DeLL\.jdks\openjdk-24.0.2+12-54"
.\mvnw.cmd spring-boot:run

# On Linux / macOS
export JAVA_HOME=/path/to/jdk-24
./mvnw spring-boot:run
```

* The backend initializes on port `8080`.
* The in-memory H2 database auto-seeds the admin user and reference projects.
* Application logs stream to `logs/aris.log`.

---

### Step 2: Start the FastAPI AI Engine

Open a second terminal in `ai-engine/`:

```bash
cd ai-engine

# Activate Python virtual environment and run Uvicorn
python -m uvicorn main:app --host 0.0.0.0 --port 8000
```

* The AI Engine starts on port `8000`.
* If models are missing, it automatically invokes `train.py` to fit the Isolation Forest models on baseline telemetry.

---

### Step 3: Open the Platform

Navigate to **[http://localhost:8080/](http://localhost:8080/)** in any modern web browser.

#### 🔑 Access Credentials:

| Role | User ID / Email | Password | Assigned Projects | Isolation Scope |
| :--- | :--- | :--- | :--- | :--- |
| **Admin** | `rakshit` | `admin123` | **E-Commerce Backend**, **Demo Project** | Full access to Admin projects. |
| **Candidate** | *(Your registered email)* | *(Your password)* | Only projects created by candidate | Completely isolated from Admin. |

> [!TIP]
> On the sign-in screen, click the **🔑 Quick Admin** pill to automatically fill `rakshit` / `admin123` and sign in with one click.

---

## 🧪 Interactive Fault Simulation & Self-Healing Walkthrough

To experience the complete autonomous reliability cycle:

1. **Access the Overview**: Sign in as `rakshit` with `admin123` and select the **E-Commerce Backend** project card.
2. **Observe Baseline Operation**:
   * Navigate to **Tab 1 (Health & Telemetry)**: Endpoints `/api/products`, `/api/orders`, and `/api/payment` display `UP (200 OK)` with low latency (~2ms) and 100% Health.
3. **Trigger Fault Simulation**:
   * Click **`⚡ Simulate 500 Fault`** in the top navigation bar (or execute `POST http://localhost:8080/api/demo/payment/toggle-fault`).
   * `/api/payment` begins throwing an unhandled `java.lang.IllegalStateException: Payment provider gateway error: connection pool exhausted to bank switch`.
4. **Inspect Automated Anomaly Detection**:
   * **Tab 3 (AI Anomalies)**: Isolation Forest generates an anomaly score (> 0.70) and flags the multi-dimensional deviation: `error_rate=0.5 (+6.5 std from baseline)`.
5. **Inspect Root Cause & Code Localization**:
   * **Tab 4 (Root Cause & AI)**: The AI engine parses `logs/aris.log` and isolates:
     * **Class**: `PaymentController`
     * **Function**: `processPayment`
     * **File**: `com/aris/ecom/PaymentController.java`
     * **Line**: `20`
   * Click **`[Open in Editor]`** to jump directly to the code viewer with line 20 highlighted in red.
6. **Review Suggested Patch**:
   * **Tab 5 (AI Fix & Incident Timeline)**: Inspect the synthesized Before $\rightarrow$ After diff block implementing circuit breaker fallback routing.
7. **Approve Patch & Verify Recovery**:
   * Click **`[✓ Approve & Apply Patch]`** or click **`⚡ Clear Fault`**.
   * Switch to **Tab 2 (API Testing / Monitoring)** and click **`[▶ Test Now]`** on `/api/payment`.
   * The probe console returns `HTTP 200 OK`.
   * In **Tab 5**, the incident status automatically transitions from **`ACTIVE`** $\rightarrow$ **`RESOLVED`**, and the project health score restores to 100%.

---

## 🛡️ Candidate Onboarding & Project Isolation

1. Click **Sign Out** from the top-right user menu.
2. On the authentication page, select the **New Registration** tab.
3. Fill in:
   * **Your Full Name**: e.g., `Candidate Alice`
   * **Email ID**: e.g., `alice@candidate.dev`
   * **Password**: e.g., `password123`
4. Click **Register & Enter Dashboard**.
5. Notice that:
   * Your dashboard shows **0 Projects** with an isolated empty-state card.
   * Admin projects (`E-Commerce Backend` and `Demo Project`) are **not visible**.
   * Click **+ Onboard Your First Project** to create and monitor your own microservices in complete privacy.

---

## 🤖 Machine Learning & AI Clarifications

### Anomaly Detection
ARIS uses **Isolation Forest**, an unsupervised anomaly-detection algorithm. It evaluates current telemetry against learned behavior and produces an anomaly score. The anomaly score is **not model accuracy**.

Any confidence value displayed by the dashboard should be interpreted as an application-level confidence/strength indicator from the anomaly analysis, **not as a calibrated probability or accuracy percentage**.

### Root Cause Analysis
Isolation Forest identifies **unusual behavior**; it does not determine an exact source-code line by itself. ARIS uses runtime logs and available exception stack traces to locate the relevant class, method, file, and line. Endpoint/controller mappings provide a fallback when stack-trace information is unavailable.

### Recommendations and Patches
ARIS provides remediation recommendations using incident context and supported remediation logic. Supported cases produce a proposed **Before → After** code diff. Patches require **developer review and approval** before application and should not be described as guaranteed autonomous or guaranteed-correct code generation.

### Evaluation Metrics
Accuracy, precision, recall, and F1 values from synthetic or simulated evaluation data describe that evaluation experiment only. They should not be presented as production model accuracy unless evaluated against representative real ARIS telemetry with appropriate ground-truth labels.

### Recovery
After a patch is approved/applied or a simulated fault is cleared, ARIS uses live probing and telemetry to check whether the affected endpoint has recovered. Successful health checks move an incident from `ACTIVE` to `RESOLVED`.

---

## 🔌 API Reference Summary

### Authentication Endpoints
* `POST /api/auth/register` — Register a new candidate developer account (`name`, `email`, `password`).
* `POST /api/auth/login` — Authenticate with User ID / Email and Password to receive a signed JWT token.

### Project & Workspace Management
* `GET /api/projects` — Retrieve all projects owned by the authenticated user.
* `POST /api/projects` — Create a new project (automatically bound to the caller's JWT user ID).
* `GET /api/projects/{id}` — Fetch project details (verifies user ownership; returns `403 Forbidden` on mismatch).
* `GET /api/workspace/overview` — System-wide telemetry aggregation scoped strictly to the caller's owned projects.
* `POST /api/workspace/projects/{id}/endpoints` — Dynamically register a new API endpoint probe.
* `POST /api/workspace/monitors/{id}/test` — Trigger an immediate on-demand manual probe.
* `POST /api/workspace/projects/{id}/patch` — Apply a developer-approved code diff to a project source file.

### AI Engine Endpoints
* `GET http://localhost:8000/api/analysis?project_id={id}` — Run Isolation Forest inference, stack trace analysis, and patch synthesis.
* `GET http://localhost:8000/api/history?project_id={id}` — Fetch historical incident timeline and recovery status.

---

## 📄 License

This project is licensed under the Apache 2.0 License.