package com.agui.json;

import com.agui.core.event.RunErrorEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObjectMapperFactory")
class ObjectMapperFactoryTest {

    @Test
    void shouldCreateModule() {
        JacksonModule module = ObjectMapperFactory.createModule();

        assertThat(module).isNotNull();
        assertThat(module.getModuleName()).isEqualTo("AgUiModule");
    }

    @Test
    void shouldHavePrivateConstructor() {
        assertThat(ObjectMapperFactory.class.getDeclaredConstructors()).hasSize(1);
        assertThat(ObjectMapperFactory.class.getDeclaredConstructors()[0].getModifiers() & 0x00000002).isEqualTo(2);
    }

    /**
     * Pinned regression: the AG-UI TypeScript {@code RunErrorEventSchema} requires the field to be
     * named {@code message}. If a future refactor accidentally drops the {@link
     * com.agui.json.mixins.RunErrorEventMixin} wiring or removes its {@code @JsonProperty("message")},
     * every agent run failure will surface in the browser as a Zod parse error
     * ({@code path: ["message"], message: "Required"}) instead of the actual error text — and the user
     * never sees the original failure. This test fixes the on-the-wire field name as a contract.
     */
    @Test
    void shouldSerializeRunErrorEventWithMessageFieldNotErrorField() {
        JsonMapper mapper = JsonMapper.builder()
            .addModule(ObjectMapperFactory.createModule())
            .build();

        RunErrorEvent event = new RunErrorEvent();
        event.setError("agent crashed");

        String json = mapper.writeValueAsString(event);

        assertThat(json)
            .as("RUN_ERROR must serialize as {\"message\": ...} for AG-UI Zod compatibility")
            .contains("\"message\":\"agent crashed\"")
            .doesNotContain("\"error\":\"agent crashed\"");
    }

    /**
     * Pinned regression for back-compat: any caller still emitting the legacy {@code "error"} key on
     * the wire (older Java producers, archived event payloads) must still deserialize cleanly into
     * {@link RunErrorEvent#getError()} via the {@code @JsonAlias("error")} on the mixin. Without
     * this, the rename above would be a hard wire break for replay tooling.
     */
    @Test
    void shouldDeserializeLegacyErrorFieldViaJsonAlias() {
        JsonMapper mapper = JsonMapper.builder()
            .addModule(ObjectMapperFactory.createModule())
            .build();

        String legacyJson = "{\"type\":\"RUN_ERROR\",\"error\":\"old format\",\"timestamp\":1}";

        RunErrorEvent event = mapper.readValue(legacyJson, RunErrorEvent.class);

        assertThat(event.getError()).isEqualTo("old format");
    }
}
