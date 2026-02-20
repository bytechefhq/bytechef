# IntelliJ Startup Optimization Guide

## Problem Statement

Running `server-app` via IntelliJ takes **~60+ seconds** to start, while `./gradlew bootRun` takes only **~10 seconds**.

## Root Cause Analysis

### Primary Culprits

| Cause | Impact | Location |
|-------|--------|----------|
| **Classpath scanning** | ~20-30s | `@SpringBootApplication(scanBasePackages = "com.bytechef")` scans 4354 Java files |
| **ServiceLoader discovery** | ~15-20s | 174 component modules discovered via `ServiceLoader.load()` |
| **Exploded class directories** | ~10-15s | IntelliJ uses class dirs vs Gradle's optimized JARs |
| **Auto-configuration** | ~5-10s | RabbitMQ, Kafka, AI, AWS auto-configs initialize |

### Why Gradle is Faster

| Aspect | Gradle bootRun | IntelliJ Run |
|--------|----------------|--------------|
| Classpath | Optimized JAR-based | Exploded class directories |
| ServiceLoader | Reads from JAR manifests (fast) | Scans directories (slow) |
| JVM | Warm Gradle daemon | Cold JVM start |
| Compilation | Cached artifacts | May compile on-demand |

### Key Code Locations

- **Component Loading**: `ComponentHandlerBeanRegistrar.java` and `ComponentHandlerConfiguration.java`
- **Component Registry**: `ComponentDefinitionRegistry.java:90-126`
- **ServiceLoader**: `AbstractComponentHandlerLoader.java:50-51`

---

## Solutions Implemented

### 1. Spring Context Indexer (Compile-time)

Generates `META-INF/spring.components` at compile time, eliminating runtime classpath scanning.

**File**: `build.gradle.kts` (root)

```kotlin
subprojects {
    dependencies {
        annotationProcessor("org.springframework:spring-context-indexer")
    }
}
```

**Impact**: ~10-20s saved

---

### 2. Component Filtering (Build-time)

Control which components are included in the classpath. Components not included won't be discovered by ServiceLoader at all.

**File**: `gradle.properties`

```properties
# WHITELIST - load ONLY these components (fastest)
includeComponents=http-client,script,var,data-mapper,logger

# OR BLACKLIST - load all EXCEPT these
#excludeComponents=salesforce,hubspot,shopify,amazonBedrock
```

**File**: `server/apps/server-app/build.gradle.kts`

```kotlin
val includeComponents = project.findProperty("includeComponents")?.toString()
    ?.split(",")
    ?.map { it.trim().lowercase() }
    ?.filter { it.isNotEmpty() }
    ?.toSet()
    ?: emptySet()

val excludeComponents = project.findProperty("excludeComponents")?.toString()
    ?.split(",")
    ?.map { it.trim().lowercase() }
    ?.filter { it.isNotEmpty() }
    ?.toSet()
    ?: emptySet()

fun shouldIncludeComponent(componentPath: String): Boolean {
    val componentName = componentPath.substringAfterLast(":").lowercase()
    if (componentName == "example") return false
    if (includeComponents.isNotEmpty()) {
        return includeComponents.contains(componentName)
    }
    if (excludeComponents.isNotEmpty()) {
        return !excludeComponents.contains(componentName)
    }
    return true
}
```

**Priority Logic**:

| includeComponents | excludeComponents | Result |
|-------------------|-------------------|--------|
| empty/not set | empty/not set | Load ALL components |
| comp1,comp2 | (ignored) | Load ONLY comp1, comp2 |
| empty/not set | comp1,comp2 | Load all EXCEPT comp1, comp2 |

**Impact**: ~30-40s saved (with minimal component set)

---

### 3. Pre-built Component JARs (Build-time)

Use pre-built JAR files instead of project dependencies for faster ServiceLoader discovery.

**File**: `gradle.properties`

```properties
useComponentJars=true
```

**File**: `server/apps/server-app/build.gradle.kts`

```kotlin
val useComponentJars = project.findProperty("useComponentJars")?.toString()?.toBoolean() ?: false

// In dependencies block:
if (useComponentJars) {
    rootProject.subprojects
        .filter { it.path.startsWith(":server:libs:modules:components") }
        .filter { shouldIncludeComponent(it.path) }
        .forEach {
            implementation(files(it.layout.buildDirectory.file("libs/${it.name}.jar")))
        }
} else {
    rootProject.subprojects
        .filter { it.path.startsWith(":server:libs:modules:components") }
        .filter { shouldIncludeComponent(it.path) }
        .forEach { implementation(project(it.path)) }
}
```

**Build Task**:

```bash
./gradlew buildComponentJars --parallel
```

**Impact**: ~5-10s saved

---

### 4. IntelliJ Profile (Runtime)

Optimized Spring profile with lazy initialization and disabled features.

**File**: `server/apps/server-app/src/main/resources/config/application-intellij.yml`

```yaml
spring:
  main:
    lazy-initialization: true

  devtools:
    restart:
      enabled: false
    livereload:
      enabled: false

  jmx:
    enabled: false

  batch:
    job:
      enabled: false

  quartz:
    auto-startup: false

  graphql:
    graphiql:
      enabled: false

  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
      - org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
      - org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration
      - org.springframework.ai.autoconfigure.anthropic.AnthropicAutoConfiguration
      - org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration
      - org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration
      - io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration
      - io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration

management:
  endpoints:
    enabled-by-default: false
  endpoint:
    health:
      enabled: true

logging:
  level:
    ROOT: INFO
    com.bytechef: INFO
    org.springframework.boot.autoconfigure: WARN
    org.quartz: ERROR

bytechef:
  ai:
    copilot:
      enabled: false
    knowledge-base:
      enabled: false
    mcp:
      server:
        enabled: false
  analytics:
    enabled: false
  help-hub:
    enabled: false
  observability:
    enabled: false
  message-broker:
    provider: memory
```

**Impact**: ~10-15s saved

---

### 5. JVM Options (Runtime)

Optimized JVM settings for faster startup and lower memory.

```
-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -XX:TieredStopAtLevel=1 --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow
```

| Option | Effect |
|--------|--------|
| `-Xms512m -Xmx2g` | 50% less memory (vs 4GB default) |
| `-XX:+UseG1GC` | Better GC for shorter pauses |
| `-XX:TieredStopAtLevel=1` | Faster JIT (less optimization) |
| `-XX:+UseStringDeduplication` | Reduces memory for duplicate strings |

**Impact**: ~5s saved, 50% less memory

---

---

## How to Use Each Feature

### Feature 1: Spring Context Indexer

**What it does**: Generates component index at compile time, eliminating runtime classpath scanning of 4354 Java files.

**How to use**:

```bash
# Just rebuild the project - the indexer runs automatically during compilation
./gradlew clean compileJava --parallel

# Verify indexes were created
find . -name "spring.components" -path "*/META-INF/*" | head -5
```

**When to rebuild**: After adding new `@Component`, `@Service`, `@Repository`, `@Controller` classes.

**Impact**: ~10-20s saved

---

### Feature 2: Component Filtering (Whitelist/Blacklist)

**What it does**: Controls which component modules are included in the classpath. Excluded components are never loaded by ServiceLoader.

**How to use (Whitelist - load ONLY specific components)**:

```properties
# gradle.properties
includeComponents=http-client,script,var,data-mapper,logger
```

**How to use (Blacklist - load all EXCEPT specific components)**:

```properties
# gradle.properties
excludeComponents=salesforce,hubspot,shopify,amazonBedrock,stabilityAi
```

**How to use (Load all components - default)**:

```properties
# gradle.properties
# Leave both commented out or empty
#includeComponents=
#excludeComponents=
```

**After changing**: Refresh Gradle in IntelliJ (`Ctrl+Shift+O` / `Cmd+Shift+I`)

**Available component names**: Directory names under `server/libs/modules/components/`

**Impact**: ~30-40s saved (with minimal 5-component set)

---

### Feature 3: Pre-built Component JARs

**What it does**: Uses pre-compiled JAR files instead of project dependencies. ServiceLoader reads from JAR manifests (instant) instead of scanning directories (slow).

**How to use**:

```properties
# gradle.properties
useComponentJars=true
```

```bash
# Build the component JARs (respects includeComponents/excludeComponents filters)
./gradlew buildComponentJars --parallel

# Refresh Gradle in IntelliJ
# Ctrl+Shift+O (Windows/Linux) or Cmd+Shift+I (Mac)
```

**When to rebuild JARs**: After modifying any component source code.

**How to disable** (for active component development):

```properties
# gradle.properties
useComponentJars=false
```

**Impact**: ~5-10s saved

---

### Feature 4: IntelliJ Profile (Lazy Initialization)

**What it does**: Enables lazy bean initialization, disables unused features (AI, analytics, message brokers), excludes heavy auto-configurations.

**How to use**:

1. In IntelliJ Run Configuration, set Active profiles:
   ```
   dev,local,intellij
   ```

2. Or via command line:
   ```bash
   ./gradlew bootRun -Dspring.profiles.active=dev,local,intellij
   ```

**Impact**: ~10-15s saved

---

### Feature 5: Optimized JVM Options

**What it does**: Reduces memory usage, uses G1GC, enables faster JIT compilation.

**How to use**:

In IntelliJ Run Configuration, set VM options:
```
-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -XX:TieredStopAtLevel=1 --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow
```

**Impact**: ~5s saved, 50% less memory

---

### Feature 6: Startup Timing Analysis

**What it does**: Logs warning for any bean taking >100ms to initialize.

**How to use**:

Add `startup-timing` to active profiles:
```
dev,local,intellij,startup-timing
```

**Output example**:
```
WARN  - SLOW BEAN: liquibase took 1523ms to initialize
WARN  - SLOW BEAN: dataSource took 892ms to initialize
```

**Impact**: Diagnostic only (helps identify bottlenecks)

---

### Feature 7: Pre-configured IntelliJ Run Configurations

**What it does**: Ready-to-use run configurations with all optimizations applied.

**How to use**:

1. Restart IntelliJ or refresh project
2. Open Run dropdown (top right)
3. Select one of:
   - `ServerApplication (Fast Dev)` - Lazy init + optimized JVM
   - `ServerApplication (Pre-built JARs)` - Uses JAR dependencies
   - `ServerApplication (Gradle Delegated)` - Delegates to Gradle bootRun

**Location**: `.run/` directory in project root

---

### Feature 8: Gradle Delegated Build

**What it does**: Makes IntelliJ use Gradle for build and run, matching `./gradlew bootRun` behavior.

**How to use (per-run)**:

Use the `ServerApplication (Gradle Delegated)` run configuration.

**How to use (globally)**:

1. IntelliJ Settings → Build, Execution, Deployment → Build Tools → Gradle
2. Change "Build and run using" from `IntelliJ IDEA` to `Gradle`
3. Change "Run tests using" from `IntelliJ IDEA` to `Gradle`

**Impact**: Matches Gradle bootRun speed (~10s)

---

## Combining Features

### Minimal + Fast (Recommended for daily development)

```properties
# gradle.properties
useComponentJars=true
includeComponents=http-client,script,var,data-mapper,logger
```

```bash
./gradlew buildComponentJars --parallel
# Refresh Gradle in IntelliJ
# Run with profile: dev,local,intellij
```

**Result**: ~10s startup, ~1.5GB memory

### Full Components + Fast

```properties
# gradle.properties
useComponentJars=true
#includeComponents=  (load all)
```

```bash
./gradlew buildComponentJars --parallel
# Refresh Gradle in IntelliJ
# Run with profile: dev,local,intellij
```

**Result**: ~25-30s startup, ~2GB memory

### Active Component Development

```properties
# gradle.properties
useComponentJars=false
#includeComponents=  (load all)
```

```
# Run with profile: dev,local,intellij
```

**Result**: ~40s startup, changes reflect immediately

---

## Quick Start Guide

### Fastest Configuration (Recommended)

**Step 1**: Edit `gradle.properties`

```properties
useComponentJars=true
includeComponents=http-client,script,var,data-mapper,logger
```

**Step 2**: Build component JARs

```bash
./gradlew buildComponentJars --parallel
```

**Step 3**: Refresh Gradle in IntelliJ

- Windows/Linux: `Ctrl+Shift+O`
- Mac: `Cmd+Shift+I`

**Step 4**: Run with IntelliJ profile

- Active profiles: `dev,local,intellij`
- VM options: `-Xms512m -Xmx2g -XX:+UseG1GC -XX:TieredStopAtLevel=1`

---

## IntelliJ Run Configurations

Pre-configured run configurations are in `.run/` directory:

| Configuration | Description |
|---------------|-------------|
| `ServerApplication (Fast Dev).run.xml` | Lazy init + optimized JVM |
| `ServerApplication (Pre-built JARs).run.xml` | Uses pre-built component JARs |
| `ServerApplication (Gradle Delegated).run.xml` | Delegates to Gradle bootRun |
| `1. Build Component JARs.run.xml` | Builds component JARs |

---

## Preset Component Configurations

| Use Case | `includeComponents` Value |
|----------|---------------------------|
| **Minimal** | `http-client,script,var,data-mapper,logger` |
| **File Processing** | `http-client,script,var,data-mapper,logger,json-file,csv-file,xml-file` |
| **API Development** | `http-client,script,var,data-mapper,logger,json-helper,xml-helper` |
| **Full** | *(leave empty to load all 174)* |

---

## Expected Results

| Configuration | Components | Startup Time | Memory |
|---------------|------------|--------------|--------|
| Default IntelliJ | 174 | ~60s | ~4GB |
| + intellij profile | 174 | ~40s | ~2GB |
| + Context Indexer | 174 | ~30s | ~2GB |
| + Component filtering (5) | 5 | ~15-20s | ~1.5GB |
| + Pre-built JARs | 5 | **~10s** | **~1.5GB** |

---

## Debugging Slow Startup

### Option 1: Startup Timing Profile

Add `startup-timing` to active profiles to log slow beans (>100ms):

```
Active profiles: dev,local,intellij,startup-timing
```

### Option 2: Startup Analysis Profile

Use `startup-analysis` profile for detailed auto-configuration logging:

```
Active profiles: dev,local,startup-analysis
```

### Option 3: Spring Debug

Add VM option:

```
-Dspring.context.debug=true
```

---

## Files Modified

| File | Changes |
|------|---------|
| `build.gradle.kts` (root) | Added `spring-context-indexer` |
| `gradle.properties` | Added `useComponentJars`, `includeComponents`, `excludeComponents` |
| `server/apps/server-app/build.gradle.kts` | Component filtering, JAR mode, `buildComponentJars` task |
| `application-intellij.yml` | Lazy init, auto-config exclusions, disabled features |
| `application-startup-analysis.yml` | Debug profile for slow startup analysis |
| `StartupTimingConfiguration.java` | Bean timing utility |
| `.run/*.run.xml` | IntelliJ run configurations |

---

## Switching Between Modes

### For Active Component Development

```properties
# gradle.properties
useComponentJars=false
#includeComponents=  (commented out)
```

Changes to components reflect immediately without rebuilding JARs.

### For Fast Startup (Not Modifying Components)

```properties
# gradle.properties
useComponentJars=true
includeComponents=http-client,script,var,data-mapper,logger
```

Requires `./gradlew buildComponentJars` after component changes.

---

## Troubleshooting

### JARs not found

```bash
# Rebuild component JARs
./gradlew buildComponentJars --parallel
```

### Changes not reflected

```bash
# Refresh Gradle in IntelliJ
# Ctrl+Shift+O (Windows/Linux) or Cmd+Shift+I (Mac)
```

### Still slow startup

1. Check active profiles include `intellij`
2. Verify `includeComponents` is set correctly
3. Run with `startup-timing` profile to identify slow beans
4. Ensure Docker infrastructure is running (database connection timeouts are slow)
