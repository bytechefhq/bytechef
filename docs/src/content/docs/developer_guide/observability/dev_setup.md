---
title: "Local Observability Setup"
---
This document lists necessary steps to enable observability features in Bytechef server and start up supporting observability tools in development mode. With the provided configuration, the user gains insight into Metrics, Logs, and Traces.

### Metrics

By including the following dependencies we have enabled metrics in the Bytchef server.
```kotlin
implementation("org.springframework.boot:spring-boot-actuator")
implementation("io.micrometer:micrometer-registry-prometheus")
```
Spring Boot Actuator, using Micrometer, generates a predefined set of metrics. The Micrometer Prometheus registry has a task of formatting these metrics into a Prometheus readable format. Such metrics are available on `/actuator/prometheus` endpoint.
For gathering and processing of metrics data we are using Prometheus, which development configuration can be found in [prometheus-dev.yml](https://github.com/bytechefhq/bytechef/tree/master/server/ee/docker/prometheus/prometheus-dev.yml). 

### Logs
We use Loki to aggregate and store logs. Logs are sent to Loki by Loki4jAppender which is configured in the [logback-spring.xml](https://github.com/bytechefhq/bytechef/tree/master/server/libs/config/logback-config/src/main/resources/logback-spring.xml)

Loki's URL is defined via  `bytechef.observability.loki.appender.http.url` Spring property. Additionally, we can control log level which will be sent to Loki via `bytechef.observability.loki.appender.level`.
```yaml
bytechef:
  observability:
    loki:
      appender:
        level: "ALL"
        http:
          url: http://localhost:3100/loki/api/v1/push
```

In logback-spring.xml it is also defined how Loki indexes logs.
```xml
 <label>
    <pattern>app=${applicationName},host=${HOSTNAME},traceID=%X{traceId:-NONE},level=%level</pattern>
</label>
```
This means you can efficiently filter logs by applicationName, host, level or traceID.

Loki development configuration is defined in a [loki-dev.yml](https://github.com/bytechefhq/bytechef/tree/master/server/ee/docker/loki/loki-dev.yml) file where, among other things, filesystem is defined as a log storage.

### Traces

Traces allow us to follow a request as it traverses Bytechef microservices. Tracing in Bytechef is enabled by including the following dependencies.
```kotlin
implementation("io.micrometer:micrometer-tracing-bridge-otel")
implementation("io.opentelemetry:opentelemetry-exporter-otlp")
```
`micrometer-tracing-bridge-otel` bridges the Micrometer Observation API to OpenTelemetry format while `io.opentelemetry:opentelemetry-exporter-otlp` reports traces to a collector that can accept OTLP.

We use Tempo as a tracing backend, which distributor and ingester support Open Telemetry. Tempo's development configuration is located in [tempo-dev.yml](https://github.com/bytechefhq/bytechef/tree/master/server/ee/docker/tempo/tempo-dev.yml) 

### Grafana

We use Grafana to visualize all the data from the observability backends. Grafana is available locally at http://localhost:3000/explore.

### Bytechef observability local setup

1. Start up the Grafana observability stack
   - in the terminal navigate to `server/ee/docker`.
   - run `docker-compose -f monitoring.yml up`

2. Enable observability features in Bytechef
   - Start Bytechef with updated Spring property `bytechef.observability.enabled` set to `true`.
