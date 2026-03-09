# Guardrails Cluster Root Element Implementation

## Overview

This document describes the implementation of the Guardrails component as a cluster root element for AI agents in ByteChef. The component provides content validation and safety guardrails that intercept AI agent requests/responses.

## Architecture

### Two-Stage Processing Model

1. **Preflight Stage** (fast, no LLM): PII detection, keyword filtering, custom pattern matching
2. **Input/Output Stage** (optional): Content validation on both input and output

### Operation Modes

- **CLASSIFY**: Block content that violates rules (returns blocked message)
- **SANITIZE**: Mask/redact sensitive content and continue processing

## Files Created

### Core Interface

**`server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/ai/agent/GuardrailsFunction.java`**

```java
@FunctionalInterface
public interface GuardrailsFunction {
    ClusterElementType GUARDRAILS = new ClusterElementType("GUARDRAILS", "guardrails", "Guardrails");

    Advisor apply(Parameters inputParameters, Parameters connectionParameters,
        Parameters extensions, Map<String, ComponentConnection> componentConnections) throws Exception;
}
```

Defines the functional interface for guardrails, following the pattern established by other cluster elements (ModelFunction, ChatMemoryFunction, etc.).

### Component Definition Interface

**`server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/GuardrailsComponentDefinition.java`**

Interface for guardrails components extending `ClusterRootComponentDefinition`.

### Module Structure

```
server/libs/modules/components/ai/agent/guardrails/
├── build.gradle.kts
├── PLAN.md (this file)
├── src/main/java/com/bytechef/component/ai/agent/guardrails/
│   ├── GuardrailsComponentHandler.java
│   ├── advisor/
│   │   ├── GuardrailsAdvisor.java
│   │   └── GuardrailsResult.java
│   ├── cluster/
│   │   ├── KeywordGuardrails.java
│   │   ├── PiiGuardrails.java
│   │   └── CustomPatternGuardrails.java
│   ├── constant/
│   │   └── GuardrailsConstants.java
│   └── util/
│       ├── PiiDetector.java
│       └── KeywordMatcher.java
├── src/main/resources/assets/
│   └── guardrails.svg
└── src/test/java/com/bytechef/component/ai/agent/guardrails/
    └── GuardrailsComponentHandlerTest.java
```

### Build Configuration

**`build.gradle.kts`**

```kotlin
dependencies {
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
}
```

### Constants

**`GuardrailsConstants.java`**

Defines:
- Mode constants: `MODE_CLASSIFY`, `MODE_SANITIZE`
- Property names for configuration
- Default blocked message
- PII type constants (EMAIL, PHONE, SSN, CREDIT_CARD, IP_ADDRESS)

### Advisor Implementation

**`GuardrailsAdvisor.java`**

Core Spring AI Advisor implementing `CallAdvisor` and `StreamAdvisor`:

- Intercepts input before sending to model
- Intercepts output before returning to user
- Runs pattern-based checks (PII, keywords, custom patterns)
- Supports CLASSIFY (block) and SANITIZE (mask) modes
- Has highest precedence (`Ordered.HIGHEST_PRECEDENCE`) to block early

Key methods:
- `adviseCall()`: Synchronous request/response handling
- `adviseStream()`: Streaming response handling
- `validateContent()`: Runs all configured checks
- `sanitizeContent()`: Masks detected violations
- `createBlockedResponse()`: Returns blocked message response

**`GuardrailsResult.java`**

Record for guardrail check results:
```java
public record GuardrailsResult(
    String guardrailName,
    boolean tripwireTriggered,
    double confidenceScore,
    Map<String, Object> info) {

    public static GuardrailsResult passed(String guardrailName);
    public static GuardrailsResult blocked(String guardrailName, double confidenceScore, Map<String, Object> info);
}
```

### Utility Classes

**`PiiDetector.java`**

Utility for detecting and masking personally identifiable information:
- Default patterns for EMAIL, PHONE, SSN, CREDIT_CARD, IP_ADDRESS
- `detect()`: Returns list of PII matches with positions
- `mask()`: Replaces PII with type tokens (e.g., `<EMAIL>`)

**`KeywordMatcher.java`**

Utility for keyword matching:
- Case-insensitive matching
- `match()`: Returns matched keywords
- `mask()`: Replaces keywords with asterisks

### Cluster Elements

**`KeywordGuardrails.java`**

- Blocks/masks content containing sensitive words
- Properties: `sensitiveWords` (list), `mode`, `validateInput`, `validateOutput`, `blockedMessage`

**`PiiGuardrails.java`**

- Detects and protects PII
- Properties: `piiTypes` (EMAIL, PHONE, SSN, CREDIT_CARD, IP_ADDRESS), `mode`, `validateInput`, `validateOutput`, `blockedMessage`

**`CustomPatternGuardrails.java`**

- Custom regex pattern matching
- Properties: `patterns` (list of regex strings), `mode`, `validateInput`, `validateOutput`, `blockedMessage`

### Component Handler

**`GuardrailsComponentHandler.java`**

```java
@Component(GUARDRAILS_COMPONENT + "_v1_ComponentHandler")
public class GuardrailsComponentHandler implements ComponentHandler {
    public GuardrailsComponentHandler() {
        this.componentDefinition = new GuardrailsComponentDefinitionImpl(
            component(GUARDRAILS_COMPONENT)
                .title("Guardrails")
                .description("Content validation and safety guardrails for AI agents")
                .icon("path:assets/guardrails.svg")
                .clusterElements(
                    new KeywordGuardrails().clusterElementDefinition,
                    new PiiGuardrails().clusterElementDefinition,
                    new CustomPatternGuardrails().clusterElementDefinition));
    }
}
```

## Files Modified

### AiAgentComponentDefinition.java

Added GUARDRAILS to cluster element types:

```java
import static com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction.GUARDRAILS;

default List<ClusterElementType> getClusterElementTypes() {
    return List.of(MODEL, CHAT_MEMORY, RAG, GUARDRAILS, SUB_AGENTS, TOOLS);
}
```

### AbstractAiAgentChatAction.java

Added guardrails advisor integration in `getAdvisors()` method:

```java
// Added as first advisor to block early (highest precedence)
clusterElementMap.fetchClusterElement(GUARDRAILS)
    .map(clusterElement -> getGuardrailsAdvisor(connectionParameters, clusterElement))
    .ifPresent(advisors::add);
```

Added `getGuardrailsAdvisor()` method to retrieve and apply the guardrails function.

### settings.gradle.kts

Added module registration:

```kotlin
include("server:libs:modules:components:ai:agent:guardrails")
```

## Technical Decisions

### Spring AI Version Compatibility

The project uses Spring AI 2.0.0-M1. Initial implementation attempted to use `CallAroundAdvisor` and `StreamAroundAdvisor` which don't exist in this version. Fixed by using:
- `CallAdvisor` with `adviseCall(ChatClientRequest, CallAdvisorChain)`
- `StreamAdvisor` with `adviseStream(ChatClientRequest, StreamAdvisorChain)`

### Sanitize Mode for Input

Input sanitization (modifying the request before sending) was simplified because `ChatClientRequest` doesn't expose methods to rebuild requests with modified content. In SANITIZE mode for input:
- Violations are detected but request continues unchanged
- Output sanitization works fully (response content is masked)

For full input sanitization, CLASSIFY mode should be used (blocks the request entirely).

### Advisor Precedence

Guardrails advisor uses `Ordered.HIGHEST_PRECEDENCE` to ensure it runs before other advisors, blocking invalid content as early as possible.

## Testing

### Unit Test

**`GuardrailsComponentHandlerTest.java`**

Verifies:
- Component definition is not null
- Component name is "guardrails"
- Component title is "Guardrails"
- Three cluster elements are registered

## Verification Commands

```bash
# Compile the module
./gradlew :server:libs:modules:components:ai:agent:guardrails:compileJava

# Run tests
./gradlew :server:libs:modules:components:ai:agent:guardrails:test

# Format code
./gradlew spotlessApply

# Full check
./gradlew check
```

## Usage Example

When configuring an AI Agent, users can add guardrails as a cluster element:

1. **Keyword Guardrails**: Block messages containing sensitive words like "password", "secret", etc.
2. **PII Guardrails**: Detect and mask/block personal information in conversations
3. **Custom Pattern Guardrails**: Apply custom regex patterns for domain-specific validation

## Future Enhancements

Potential additions:
- LLM-based content moderation (requires MODEL dependency)
- Rate limiting guardrails
- Topic restriction guardrails
- Custom validation function support
- Logging and audit trail for blocked content
