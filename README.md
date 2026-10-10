# Distributed Task Scheduler

Enterprise-grade, highly available **Distributed Task Scheduler** built with **Java 21**, **Spring Boot 3.x**, **MySQL 8.0 (InnoDB)**, **Flyway**, **gRPC**, **Protocol Buffers**, and **Java 21 Virtual Threads**.

---

## 🌟 Architectural Features

- **Spring Boot 3.2+ & Java 21 Virtual Threads**: Handles thousands of concurrent task dispatches with lightweight virtual threads (`spring.threads.virtual.enabled=true`).
- **MySQL 8.0 Primary Storage**: Uses MySQL InnoDB as the single source of truth for jobs, workers, executions, events, and audit logs.
- **Atomic Job Claiming**: Implements MySQL InnoDB pessimistic row-level locking (`SELECT FOR UPDATE`) to prevent duplicate task dispatching across clustered nodes.
- **gRPC & Protocol Buffers**: High-performance, low-latency binary RPC communication between the Coordinator and Worker nodes.
- **Flyway Database Migrations**: Versioned database migrations (`V1__init_schema.sql`, `V2__seed_data.sql`).
- **Worker Heartbeat Monitoring & Failover**: Automatic detection of dead/stale worker nodes (missed heartbeats > threshold) with automatic task reassignment and retry mechanisms.
- **Spring Security & JWT**: Role-Based Access Control (`ROLE_ADMIN`, `ROLE_OPERATOR`) with stateless JWT authentication for REST APIs.
- **Control Center Web Dashboard**: Real-time dark-mode UI embedded directly in the Coordinator application.

---

## 🏗 System Architecture Diagram

```mermaid
graph TD
    Client["💻 Client / Dashboard / REST API"] -->|HTTP / JWT| Coordinator["⚡ Coordinator Node (Spring Boot 3.2)"]
    
    subgraph Coordinator Engine
        Coordinator --> REST["REST Controllers"]
        Coordinator --> SchedulerEngine["⏰ Scheduler Engine (Virtual Threads)"]
        Coordinator --> Dispatcher["🚀 Job Dispatcher (InnoDB Locks)"]
        Coordinator --> HeartbeatMonitor["💔 Heartbeat & Recovery Monitor"]
        Coordinator --> gRPCServer["📡 gRPC Server (Port 9090)"]
    end
    
    gRPCServer <-->|gRPC / Protobuf| Worker1["🖥️ Worker Node 1 (Port 9091)"]
    gRPCServer <-->|gRPC / Protobuf| Worker2["🖥️ Worker Node 2 (Port 9092)"]

    Coordinator -->|JDBC / JPA / Flyway| MySQL[("🛢️ MySQL 8.0 Database")]

    subgraph Task Runners
        Worker1 --> Task1["Shell Commands"]
        Worker1 --> Task2["HTTP Requests"]
        Worker1 --> Task3["Computation / Math"]
        Worker1 --> Task4["Synthetic Workloads"]
    end
```

---

## 🛢️ Database Schema Diagram

```mermaid
erDiagram
    users {
        bigint id PK
        string username UK
        string password_hash
        string email UK
        string role
        boolean enabled
    }

    scheduled_jobs {
        bigint id PK
        string job_key UK
        string name
        string job_type
        json payload
        string cron_expression
        string schedule_type
        int max_retries
        int timeout_seconds
        string status
    }

    worker_nodes {
        bigint id PK
        string worker_id UK
        string hostname
        int grpc_port
        string status
        int capacity
        int active_tasks
        datetime last_heartbeat_at
    }

    job_executions {
        bigint id PK
        string execution_id UK
        bigint job_id FK
        string worker_id
        string status
        int retry_count
        datetime scheduled_time
        datetime start_time
        datetime end_time
        mediumtext stdout_log
        mediumtext stderr_log
    }

    execution_events {
        bigint id PK
        string execution_id
        string event_type
        string worker_id
        text message
    }

    audit_logs {
        bigint id PK
        string username
        string action
        string entity_type
        string entity_id
    }

    scheduled_jobs ||--o{ job_executions : "generates"
    job_executions ||--o{ execution_events : "emits"
    worker_nodes ||--o{ job_executions : "executes"
```

---

## ⚙️ Environment Variables

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `DB_URL` | MySQL JDBC URL | Local development URL |
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | Required; supply outside source control |
| `PORT` | Coordinator REST port (Render supplies this) | `8080` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated browser origins allowed to call the coordinator | Vercel production URL and local development URLs |
| `GRPC_PORT` | Coordinator gRPC server port | `9090` |
| `COORDINATOR_HOST` | Coordinator hostname used by a worker | `localhost` |
| `COORDINATOR_GRPC_PORT` | Coordinator gRPC port used by a worker | `9090` |
| `WORKER_ID` | Unique worker identifier | `worker-node-1` |
| `WORKER_HOSTNAME` | Worker hostname registered with coordinator | `localhost` |
| `WORKER_PORT` | Worker port registered with coordinator | `9091` |
| `JWT_SECRET` | Base64-encoded JWT signing key (at least 32 bytes) | Required |
| `SCHEDULER_HEARTBEAT_TIMEOUT_SECONDS` | Stale heartbeat threshold | `15` |

---

## 🚀 Setup & Execution Guide

### Prerequisites
1. **Java 21 JDK** installed and configured.
2. **MySQL Server 8.0** running locally or via Docker.
3. **Maven 3.8+**

### 1. Database Initialization
Ensure MySQL 8.0 is running and create the target database:
```sql
CREATE DATABASE IF NOT EXISTS distributed_scheduler;
```

### 2. Build the Maven Modules
The Maven aggregator and all three modules are under `backend`. Run these commands from the repository root in PowerShell:
```powershell
mvn -f .\backend\pom.xml -pl coordinator -am clean package
mvn -f .\backend\pom.xml -pl worker -am clean package
```

### 3. Run Coordinator Application
Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` in the process environment first. Then run:
```powershell
java -jar .\backend\coordinator\target\coordinator-1.0.0-SNAPSHOT.jar
```

### 4. Run Worker Nodes
The worker uses a gRPC client to register with the coordinator and send heartbeats:
```powershell
$env:COORDINATOR_HOST = "localhost"
$env:COORDINATOR_GRPC_PORT = "9090"
$env:WORKER_ID = "worker-node-1"
java -jar .\backend\worker\target\worker-1.0.0-SNAPSHOT.jar
```

> **Dispatch limitation:** the coordinator calls the worker's `DispatchTask` gRPC RPC, but the worker module currently has no gRPC server implementation for that RPC. Registration and heartbeat support do not by themselves make task dispatch operational.

### 5. Access Dashboard
When the frontend is running locally, open:
```
http://localhost:8080/
```
Default Credentials:
- **Username**: `admin`
- **Password**: `Admin@123`

---

## 🐳 Docker Compose Deployment

Copy `.env.example` to `.env`, set unique local values for `DB_PASSWORD` and `JWT_SECRET`, then start the services:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

The coordinator and worker Dockerfiles expect the repository root as their build context so they can copy `backend/pom.xml` and the sibling Maven modules. When creating either service directly on Render, leave **Root Directory** blank, set the Dockerfile path to `backend/coordinator/Dockerfile` or `backend/worker/Dockerfile`, and use `.` (the repository root) as the Docker build context.

---

## 🧪 Verification & Failover Demonstration

### 1. Job Creation & Execution
1. Open the dashboard at `http://localhost:8080/`.
2. Navigate to **Scheduled Jobs** and click **+ Create New Job**.
3. Create a **SHELL** or **COMPUTATION** job with cron `*/15 * * * * *`.
4. Observe the job automatically being claimed by the Coordinator engine, dispatched over gRPC to `worker-node-1`, and recorded in the MySQL `job_executions` table.

### 2. Demonstrating Worker Timeout & Task Failover Recovery
1. Start a long-running synthetic or shell task on `worker-node-1`.
2. Abruptly kill the worker process (`worker-node-1`).
3. After **15 seconds** (heartbeat timeout threshold), the Coordinator's `HeartbeatMonitorService`:
   - Detects `worker-node-1` as **DEAD**.
   - Identifies unfinished active tasks assigned to `worker-node-1`.
   - Automatically reassigns the tasks to status **PENDING**, increments `retry_count`, and dispatches them to **Worker Node 2**.
   - Emits an execution event `REASSIGNED` and logs an audit record in MySQL.
