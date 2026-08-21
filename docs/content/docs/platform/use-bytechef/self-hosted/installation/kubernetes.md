---
title: Kubernetes
description: Deploy ByteChef on Kubernetes
---

# Kubernetes

ByteChef ships a Helm chart for deploying the single-node application on a Kubernetes cluster. The chart lives in the repository at [`kubernetes/helm/bytechef`](https://github.com/bytechefhq/bytechef/tree/master/kubernetes/helm/bytechef).

## Prerequisites

- A Kubernetes cluster and `kubectl` configured to reach it.
- [Helm 3](https://helm.sh/).
- A reachable **PostgreSQL 15+** database. The chart does not deploy PostgreSQL — use a managed database or an in-cluster instance you manage separately.

## 1. Get the chart

Clone the repository (the chart is not yet published to a Helm registry):

```bash
git clone https://github.com/bytechefhq/bytechef.git
cd bytechef/kubernetes/helm/bytechef
```

## 2. Configure values

The chart is driven by `values.yaml`. The settings you almost always need to change are the image tag, the database connection, and the encryption and remember-me secrets. Configuration is split into two blocks under `env`:

- `env.normal` — non-sensitive environment variables rendered into a ConfigMap.
- `env.secret` — sensitive environment variables rendered into a Secret.

A minimal override file (`my-values.yaml`) looks like this:

```yaml
image:
  repository: docker.bytechef.io/bytechef/bytechef
  tag: latest

env:
  normal:
    BYTECHEF_ENCRYPTION_PROVIDER: property
    BYTECHEF_PUBLIC_URL: "https://bytechef.example.com"
    SERVER_PORT: 8080

  secret:
    BYTECHEF_DATASOURCE_URL: "jdbc:postgresql://postgres.example.com:5432/bytechef"
    BYTECHEF_DATASOURCE_USERNAME: "bytechef"
    BYTECHEF_DATASOURCE_PASSWORD: "<db-password>"
    BYTECHEF_ENCRYPTION_PROPERTY_KEY: "<stable-encryption-key>"
    BYTECHEF_SECURITY_REMEMBER_ME_KEY: "<stable-remember-me-key>"
```

Use `BYTECHEF_ENCRYPTION_PROVIDER: property` with a fixed `BYTECHEF_ENCRYPTION_PROPERTY_KEY` on Kubernetes. The default `filesystem` provider writes a key to the pod's local disk, which does not survive pod restarts and is not shared across replicas — either would leave stored connection credentials undecryptable. See [Encryption of stored credentials](/platform/use-bytechef/self-hosted/configuration#the-key) for the full reasoning.

## 3. Install

```bash
helm install bytechef . -f my-values.yaml --namespace bytechef --create-namespace
```

After the release installs, Helm prints (via the chart's notes) the command to reach the application based on your `service.type`. With the default `ClusterIP`, port-forward to it:

```bash
kubectl --namespace bytechef port-forward svc/bytechef 8080:8080
```

Then open [http://127.0.0.1:8080](http://127.0.0.1:8080) and click **Create account** to register the first user.

<!-- TODO screenshot: kubectl get pods -n bytechef output showing the bytechef pod Running, or the first-login screen reached through the port-forward -->

## Exposing the instance

The chart includes optional resources for production exposure:

| Concern | Values key | Notes |
|---|---|---|
| Service type | `service.type` | Defaults to `ClusterIP`; set to `NodePort` or `LoadBalancer` to expose it directly. `service.port` defaults to `8080`. |
| Ingress | `ingress.enabled` | Disabled by default. Set `ingress.enabled: true`, then configure `ingress.className`, `ingress.hosts`, and `ingress.tls`. Set `BYTECHEF_PUBLIC_URL` to the external URL. |
| Autoscaling | `autoscaling.enabled` | Disabled by default. When enabled it creates a HorizontalPodAutoscaler between `minReplicas` and `maxReplicas` on `targetCPUUtilizationPercentage`. |
| Replicas | `replicaCount` | Static replica count when autoscaling is off (default `1`). |

When running more than one replica, align the multi-instance settings described in [Running multiple instances](/platform/use-bytechef/self-hosted/configuration#running-multiple-instances) — shared encryption key, a shared message broker and cache, and coordinated schema migrations.

## Health probes

The chart wires the Kubernetes probes to ByteChef's Actuator health endpoints out of the box:

- **Startup / liveness:** `GET /actuator/health/liveness` on port `8080`.
- **Readiness:** `GET /actuator/health/readiness` on port `8080`.

These are the same probes documented under [Observability](/platform/use-bytechef/self-hosted/management/observability).

## Upgrading

Update the `image.tag` in your values file and run:

```bash
helm upgrade bytechef . -f my-values.yaml --namespace bytechef
```

Schema migrations run automatically on startup. Back up the database first, and see [Upgrades and backups](/platform/use-bytechef/self-hosted/management/upgrades) for upgrade and backup guidance.

