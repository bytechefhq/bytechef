---
title: Google Cloud
description: Learn how to deploy ByteChef on Google Cloud Platform
---

# Google Cloud

ByteChef runs on Google Cloud as the container image `docker.bytechef.io/bytechef/bytechef:latest` on port `8080`. **Cloud Run** is the simplest managed target; the same image also runs on Google Kubernetes Engine (GKE) using the [Helm chart](/platform/use-bytechef/self-hosted/installation/kubernetes) or on a **Compute Engine** VM with Docker (see [Compute Engine](#compute-engine) below). Pair it with **Cloud SQL for PostgreSQL**.

## Prerequisites

- A Google Cloud project and the `gcloud` CLI.
- A **Cloud SQL for PostgreSQL 15+** instance with a `bytechef` database. Connect to it from Cloud Run via the [Cloud SQL connector / Auth Proxy](https://cloud.google.com/sql/docs/postgres/connect-run).
- Secret Manager for the sensitive values.

## 1. Provision the database

Create a Cloud SQL for PostgreSQL instance and a `bytechef` database. Note the connection name, user, and password, and enable the Cloud SQL connection on the Cloud Run service so the database is reachable.

## 2. Deploy to Cloud Run

```bash
gcloud run deploy bytechef \
  --image=docker.bytechef.io/bytechef/bytechef:latest \
  --port=8080 \
  --allow-unauthenticated \
  --add-cloudsql-instances=<connection-name> \
  --set-env-vars=BYTECHEF_DATASOURCE_URL="jdbc:postgresql:///bytechef?cloudSqlInstance=<connection-name>&socketFactory=com.google.cloud.sql.postgres.SocketFactory",BYTECHEF_DATASOURCE_USERNAME="<db-user>",BYTECHEF_ENCRYPTION_PROVIDER=property,BYTECHEF_PUBLIC_URL="https://bytechef.example.com" \
  --set-secrets=BYTECHEF_DATASOURCE_PASSWORD=db-password:latest,BYTECHEF_ENCRYPTION_PROPERTY_KEY=encryption-key:latest,BYTECHEF_SECURITY_REMEMBER_ME_KEY=remember-me-key:latest
```

Use **stable** values for the encryption and remember-me keys in Secret Manager so stored credentials and sessions survive revision restarts.

## 3. Tune the service

- **Health checks:** point the Cloud Run startup / liveness probe at `GET /actuator/health/readiness` and `/actuator/health/liveness` on port `8080`.
- **Public URL:** set `BYTECHEF_PUBLIC_URL` to the Cloud Run service URL (or your mapped custom domain) so webhook and OAuth2 redirect URLs resolve.
- **Startup time:** ByteChef runs schema migrations on first start; give the startup probe a generous timeout so the first revision becomes ready.
- **Concurrency and instances:** because migrations run at startup, deploy a single revision for the first release and any upgrade. If you allow more than one instance, apply the multi-instance settings in [Running multiple instances](/platform/use-bytechef/self-hosted/configuration#running-multiple-instances) — a shared message broker and cache, and a shared encryption key.

## 4. Access the instance

Open the Cloud Run service URL and click **Create account** to register the first user.

<!-- TODO screenshot: the ByteChef first-login screen reached at the Cloud Run service URL, with the "Create account" link visible -->

## Compute Engine

Cloud Run is the managed path. If you want a plain VM you control — with local Docker, a
containerized database, and no Cloud SQL connector — deploy to Compute Engine instead.

### Machine sizing

ByteChef runs a JVM alongside the workflow engine, so memory is the binding constraint.

| Use case | Machine type | RAM |
|---|---|---|
| Testing / development | `e2-medium` | 4 GB |
| Production | `e2-standard-2` | 8 GB |
| Heavy workloads | `e2-standard-4` | 16 GB |

Do not use `e2-micro`, `e2-small`, or `f1-micro`. They have insufficient memory and the container
will be killed under load.

### 1. Create the instance

From the [Compute Engine console](https://console.cloud.google.com/compute/instances), choose
**Create Instance** and configure:

- **Name:** `bytechef-server`
- **Region:** whichever is closest to your users
- **Machine type:** `e2-medium` or larger, per the table above
- **Boot disk:** Ubuntu 22.04 LTS, 30 GB
- **Firewall:** allow HTTP and HTTPS traffic

### 2. Open the ByteChef port

Cloud Console's HTTP/HTTPS checkboxes open `80` and `443` only, so port `8080` needs its own rule
under **VPC Network → Firewall**:

- **Name:** `allow-bytechef`
- **Targets:** all instances in the network (or a target tag on this VM)
- **Source IP ranges:** restrict to your own ranges where you can; `0.0.0.0/0` exposes the instance
  to the internet
- **Protocols and ports:** `tcp:8080`

If you terminate TLS on the instance (below), you do not need to open `8080` publicly at all.

### 3. Connect and install Docker

```bash
gcloud compute ssh bytechef-server --zone=<zone>
```

```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
```

Log out and back in so the group membership takes effect.

### 4. Deploy

```bash
curl -O https://raw.githubusercontent.com/bytechefhq/bytechef/master/docker-compose.yml
docker compose up -d
```

That Compose file brings up ByteChef and PostgreSQL together, but ships with fixed default
credentials and a fixed remember-me key — use it for evaluation only. For anything durable, point
the container at a Cloud SQL instance and supply your own secrets, as in the
[`docker run` command in the AWS EC2 guide](/platform/use-bytechef/self-hosted/installation/aws-ec2#5-run-bytechef).

Then browse to `http://<external-ip>:8080` and click **Create account** to register the first user.

### Production notes

- **Reserve a static IP.** Under **VPC Network → IP Addresses**, reserve an external static address
  and attach it to the instance, so the address survives a stop/start cycle. Without one, stopping
  the instance changes its IP and breaks any webhook URL pointing at it.
- **Terminate TLS.** Put nginx in front and set `BYTECHEF_PUBLIC_URL` to the HTTPS URL so webhook and
  OAuth2 redirect URLs are correct:

  ```bash
  sudo apt install nginx certbot python3-certbot-nginx -y
  sudo certbot --nginx -d your-domain.com
  ```

- **Back up PostgreSQL.** See [Upgrades and backups](/platform/use-bytechef/self-hosted/management/upgrades).

## Storage note (Cloud Run)

Cloud Run instances have ephemeral local storage, and file storage defaults to **`filesystem`** — so set `BYTECHEF_FILE_STORAGE_PROVIDER=jdbc` explicitly to keep files in the database, or store them in object storage of your own. Leaving the default in place writes files to a disk that disappears with the instance.

