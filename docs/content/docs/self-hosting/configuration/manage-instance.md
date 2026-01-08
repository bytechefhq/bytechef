---
description: "ByteChef Manage Instance. Manage and maintain your ByteChef instance configuration, monitoring, and operations."
title: Manage Instance
---

<<<<<<< HEAD
In this section, we will explore the self-hosted ByteChef instance management credentials, for configuring and monitoring your self-hosted ByteChef deployment instance.

## Deployment Information

The deployment type shows whether you're running on Docker, Kubernetes, or another platform. This helps you identify which commands and configuration methods apply to your setup.

The version number indicates which ByteChef release you have installed - it's important when checking for updates or troubleshooting issues.

Database connectivity status shows whether ByteChef can reach your PostgreSQL database. A good connection with databases is needed for all operations.

## Observability

ByteChef provides comprehensive observability features for monitoring your instance through metrics, logs, and distributed traces. With the proper configuration, you gain insight into system performance, application behavior, and request flows across components.

### Metrics

By including the following dependencies we have enabled metrics in the ByteChef server:

```
implementation("org.springframework.boot:spring-boot-actuator")
implementation("io.micrometer:micrometer-registry-prometheus")
```

Spring Boot Actuator, using Micrometer, generates a predefined set of metrics. The Micrometer Prometheus registry formats these metrics into a Prometheus readable format. Such metrics are available on the `/actuator/prometheus` endpoint. For gathering and processing metrics data, we use Prometheus; its development configuration can be found in [prometheus-dev.yml](https://github.com/bytechefhq/bytechef/tree/master/server/ee/docker/prometheus/prometheus-dev.yml).


### Logs

We use Loki to aggregate and store logs. Logs are sent to Loki by `Loki4jAppender`, which is configured in [logback-spring.xml](https://github.com/bytechefhq/bytechef/blob/master/server/libs/config/logback-config/src/main/resources/logback-spring.xml).

Loki’s URL is defined via the Spring property `bytechef.observability.loki.appender.http.url`. Additionally, log level sent to Loki is controlled via `bytechef.observability.loki.appender.level`, for example:

```
bytechef:
  observability:
    loki:
      appender:
        level: "ALL"
        http:
          url: http://localhost:3100/loki/api/v1/push
```

In `logback-spring.xml`, Loki indexing labels are defined, letting you efficiently filter logs by `applicationName`, `host`, `traceID`, and `level`. Loki’s development configuration is defined in [loki-dev.yml](https://github.com/bytechefhq/bytechef/blob/master/server/ee/docker/loki/loki-dev.yml).

### Traces

Traces allow us to follow a request as it traverses ByteChef components. Tracing is enabled by including:

```
implementation("io.micrometer:micrometer-tracing-bridge-otel")
implementation("io.opentelemetry:opentelemetry-exporter-otlp")
```

`micrometer-tracing-bridge-otel` bridges the Micrometer Observation API to OpenTelemetry format, while `opentelemetry-exporter-otlp` reports traces to a collector that accepts OTLP. Tempo is used as the tracing backend, with its development configuration in [tempo-dev.yml](https://github.com/bytechefhq/bytechef/blob/master/server/ee/docker/tempo/tempo-dev.yml).


### Grafana

We use Grafana to visualize data from the observability backends. Grafana is available locally at [http://localhost:3000/explore](http://localhost:3000/explore).

#### Start up Grafana

1. Navigate to `server/ee/docker`
2. Run:
```
docker-compose -f monitoring.yml up
```
3. Enable observability features in ByteChef by starting the server with the Spring property `bytechef.observability.enabled` set to `true`. 
### Health Check Endpoints

Monitor instance health through these Spring Boot Actuator endpoints:

**Liveness:**

```
http://localhost:8080/actuator/health/liveness
```
Running this command helps indicate if the application is running

**Readiness:**

```
http://localhost:8080/actuator/health/readiness
```

This command helps to check if the application is ready to accept traffic

**General Health:**

```
http://localhost:8080/actuator/health
```

Running this command helps check the overall health status

These endpoints are used by Kubernetes liveness and readiness probes to manage pod lifecycle.

## Deployment Management

In this section we will dive into how to manage your ByteChef deployment configuration instances.

### Docker Deployment

If running ByteChef with Docker or Docker Compose:

**View Current Configuration:**

```
docker inspect bytechef
```

**View Environment Variables:**

```
docker exec bytechef env | grep BYTECHEF
```

**Restart Instance:**

```
# Docker Compose
docker-compose restart bytechef

# Docker
docker restart bytechef
```

**View Logs:**

```
# Docker Compose
docker-compose logs -f bytechef

# Docker
docker logs -f bytechef
```

### Kubernetes Deployment

If running ByteChef on Kubernetes:

**View Current Configuration:**

```
kubectl get deployment bytechef -o yaml
```

**View Environment Variables:**

```
# From ConfigMap
kubectl get configmap bytechef-envs -o yaml

# From Secret
kubectl get secret bytechef-secrets -o yaml
```

**Restart Instance:**

```
kubectl rollout restart deployment/bytechef
```

**View Logs:**

```
kubectl logs -f deployment/bytechef
```

**Check Pod Status:**

```
kubectl get pods -l app=bytechef
```

## Instance Actions

### Restart Instance

Some configuration changes require restarting ByteChef.

**When to Restart:**

* Database connection changes
* Encryption provider changes
* Feature flag modifications
* Network URL changes
* Observability settings

**Docker:**

```
docker restart bytechef
```

**Kubernetes:**

```
kubectl rollout restart deployment/bytechef
```

### View Logs

Access real-time logs from your ByteChef instance.

**Docker:**

```
# Follow logs
docker logs -f bytechef

# Last 100 lines
docker logs --tail 100 bytechef

# Logs since timestamp
docker logs --since 2024-01-27T10:00:00 bytechef
```

**Kubernetes:**

```
# Follow logs
kubectl logs -f deployment/bytechef

# Last 100 lines
kubectl logs --tail=100 deployment/bytechef

# Previous instance logs (after crash)
kubectl logs --previous deployment/bytechef
```

## Troubleshooting

### Database Connection Failed

Check database connectivity:

```
# Docker
docker exec bytechef nc -zv postgres 5432

# Kubernetes
kubectl exec deployment/bytechef -- nc -zv postgres 5432
```

### Email Not Sending

Verify mail configuration:

```
docker exec bytechef env | grep BYTECHEF_MAIL
```

### OAuth Callbacks Failing

Verify OAuth callback URL:

```
echo $BYTECHEF_OAUTH_CALLBACK_URL
```

Ensure the URL is:

* Publicly accessible
* Whitelisted in OAuth provider settings
* Using HTTPS (if required by provider)

### Instance Not Starting

Check logs for errors:

```
# Docker
docker logs bytechef

# Kubernetes
kubectl logs deployment/bytechef
```
=======
Documentation for managing your ByteChef instance is coming soon. Please check back later!
>>>>>>> 002576c3f5 (docs: Update Docker deployment documentation and clean up files)
