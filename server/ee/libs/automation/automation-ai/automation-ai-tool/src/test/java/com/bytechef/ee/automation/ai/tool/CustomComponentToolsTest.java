/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.exception.ExecutionException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class CustomComponentToolsTest {

    @Mock
    private CustomComponentFacade customComponentFacade;

    @Test
    void testCreateCustomComponentReturnsConfirmationMessage() {
        CustomComponentTools tools = new CustomComponentTools(customComponentFacade);

        CustomComponent customComponent = new CustomComponent();

        customComponent.setId(7L);

        when(customComponentFacade.createEmptyCustomComponent("myComponent", CustomComponent.Language.JAVASCRIPT))
            .thenReturn(customComponent);

        String result = tools.createCustomComponent("myComponent", CustomComponent.Language.JAVASCRIPT);

        verify(customComponentFacade).createEmptyCustomComponent("myComponent", CustomComponent.Language.JAVASCRIPT);
        assertThat(result).contains("7")
            .contains("myComponent");
    }

    @Test
    void testCreateCustomComponentThrowsExecutionExceptionOnFacadeFailure() {
        CustomComponentTools tools = new CustomComponentTools(customComponentFacade);

        when(customComponentFacade.createEmptyCustomComponent(anyString(), any()))
            .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> tools.createCustomComponent("myComponent", CustomComponent.Language.JAVASCRIPT))
            .isInstanceOf(ExecutionException.class);
    }

    @Test
    void testUpdateCustomComponentSourceReturnsResultingRowDetails() {
        CustomComponentTools tools = new CustomComponentTools(customComponentFacade);

        CustomComponent draftCustomComponent = new CustomComponent();

        draftCustomComponent.setId(12L);
        draftCustomComponent.setComponentVersion(2);
        draftCustomComponent.setStatus(CustomComponent.Status.DRAFT);

        when(customComponentFacade.updateCustomComponentSource(5L, "console.log('hi');"))
            .thenReturn(draftCustomComponent);

        String result = tools.updateCustomComponentSource(5L, "console.log('hi');");

        verify(customComponentFacade).updateCustomComponentSource(5L, "console.log('hi');");
        assertThat(result).contains("12")
            .contains("2")
            .contains("DRAFT");
    }

    @Test
    void testPublishCustomComponentReturnsConfirmationMessage() {
        CustomComponentTools tools = new CustomComponentTools(customComponentFacade);

        CustomComponent publishedCustomComponent = new CustomComponent();

        publishedCustomComponent.setId(12L);
        publishedCustomComponent.setComponentVersion(2);
        publishedCustomComponent.setName("myComponent");

        when(customComponentFacade.publishCustomComponent(12L)).thenReturn(publishedCustomComponent);

        String result = tools.publishCustomComponent(12L);

        verify(customComponentFacade).publishCustomComponent(12L);
        assertThat(result).contains("12")
            .contains("myComponent");
    }

    @Test
    void testPublishCustomComponentThrowsExecutionExceptionOnFacadeFailure() {
        CustomComponentTools tools = new CustomComponentTools(customComponentFacade);

        when(customComponentFacade.publishCustomComponent(12L)).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> tools.publishCustomComponent(12L))
            .isInstanceOf(ExecutionException.class);
    }

    @Test
    void testUpdateCustomComponentSourceThrowsExecutionExceptionOnFacadeFailure() {
        CustomComponentTools tools = new CustomComponentTools(customComponentFacade);

        doThrow(new RuntimeException("boom"))
            .when(customComponentFacade)
            .updateCustomComponentSource(5L, "content");

        assertThatThrownBy(() -> tools.updateCustomComponentSource(5L, "content"))
            .isInstanceOf(ExecutionException.class);
    }

    @Test
    void testDeleteCustomComponentReturnsConfirmationMessage() {
        CustomComponentTools tools = new CustomComponentTools(customComponentFacade);

        String result = tools.deleteCustomComponent(9L);

        verify(customComponentFacade).delete(9L);
        assertThat(result).isNotBlank()
            .contains("9");
    }

    @Test
    void testDeleteCustomComponentThrowsExecutionExceptionOnFacadeFailure() {
        CustomComponentTools tools = new CustomComponentTools(customComponentFacade);

        doThrow(new RuntimeException("boom"))
            .when(customComponentFacade)
            .delete(9L);

        assertThatThrownBy(() -> tools.deleteCustomComponent(9L))
            .isInstanceOf(ExecutionException.class);
    }

    @Test
    void testGetCustomComponentSourceReturnsSourceText() {
        ReadCustomComponentTools tools = new ReadCustomComponentTools(customComponentFacade);

        when(customComponentFacade.getCustomComponentSource(3L)).thenReturn("const x = 1;");

        String result = tools.getCustomComponentSource(3L);

        verify(customComponentFacade).getCustomComponentSource(3L);
        assertThat(result).isEqualTo("const x = 1;");
    }

    @Test
    void testGetCustomComponentSourceThrowsExecutionExceptionOnFacadeFailure() {
        ReadCustomComponentTools tools = new ReadCustomComponentTools(customComponentFacade);

        when(customComponentFacade.getCustomComponentSource(3L)).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> tools.getCustomComponentSource(3L))
            .isInstanceOf(ExecutionException.class);
    }

    @Test
    void testListCustomComponentsReturnsFacadeList() {
        ReadCustomComponentTools tools = new ReadCustomComponentTools(customComponentFacade);

        CustomComponent customComponent = new CustomComponent();

        customComponent.setId(1L);

        when(customComponentFacade.getCustomComponents()).thenReturn(List.of(customComponent));

        List<CustomComponent> result = tools.listCustomComponents();

        verify(customComponentFacade).getCustomComponents();
        assertThat(result).containsExactly(customComponent);
    }

    @Test
    void testListCustomComponentsThrowsExecutionExceptionOnFacadeFailure() {
        ReadCustomComponentTools tools = new ReadCustomComponentTools(customComponentFacade);

        when(customComponentFacade.getCustomComponents()).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(tools::listCustomComponents)
            .isInstanceOf(ExecutionException.class);
    }
}
