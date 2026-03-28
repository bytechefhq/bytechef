package com.agui.json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JacksonModule;

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
}
