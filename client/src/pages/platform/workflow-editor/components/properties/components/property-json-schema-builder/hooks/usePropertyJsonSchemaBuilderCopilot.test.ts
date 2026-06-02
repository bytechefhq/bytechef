import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {usePropertyJsonSchemaBuilderCopilot} from './usePropertyJsonSchemaBuilderCopilot';

describe('usePropertyJsonSchemaBuilderCopilot', () => {
    beforeEach(() => {
        useCopilotPostTurnRegistry.setState({callbacks: {}});
        useCopilotStateContributorRegistry.setState({contributors: []});
        useCopilotStore.setState({context: undefined, messages: []});
        useCopilotToolResultHandlerRegistry.setState({handlers: {}});
    });

    it('sets json-schema context on open and restores on close', () => {
        const saveSpy = vi.spyOn(useCopilotStore.getState(), 'saveConversationState');
        const restoreSpy = vi.spyOn(useCopilotStore.getState(), 'restoreConversationState');

        const {result} = renderHook(() =>
            usePropertyJsonSchemaBuilderCopilot({
                onSchemaApply: vi.fn(),
                propertyPath: 'output',
                schemaRef: {current: {type: 'object'}},
                title: 'Response Schema',
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() => result.current.handleCopilotOpen());

        expect(saveSpy).toHaveBeenCalled();
        expect(useCopilotStore.getState().context).toMatchObject({
            mode: MODE.ASK,
            parameters: {propertyPath: 'output'},
            source: Source.JSON_SCHEMA_BUILDER,
        });
        expect(result.current.copilotPanelOpen).toBe(true);

        act(() => result.current.handleCopilotClose());

        expect(restoreSpy).toHaveBeenCalled();
        expect(result.current.copilotPanelOpen).toBe(false);
    });

    it('applies the schema from an updateJsonSchema tool result', () => {
        const onSchemaApply = vi.fn();

        renderHook(() =>
            usePropertyJsonSchemaBuilderCopilot({
                onSchemaApply,
                propertyPath: 'output',
                schemaRef: {current: undefined},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() =>
            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor('updateJsonSchema', JSON.stringify({schema: {type: 'object'}}))
        );

        expect(onSchemaApply).toHaveBeenCalledWith({type: 'object'});
    });

    it('applies the schema only once across a full turn', () => {
        const onSchemaApply = vi.fn();

        renderHook(() =>
            usePropertyJsonSchemaBuilderCopilot({
                onSchemaApply,
                propertyPath: 'output',
                schemaRef: {current: undefined},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() => {
            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor('updateJsonSchema', JSON.stringify({schema: {type: 'object'}}));
        });

        act(() => {
            useCopilotPostTurnRegistry.getState().runFor(Source.JSON_SCHEMA_BUILDER);
        });

        expect(onSchemaApply).toHaveBeenCalledTimes(1);
    });

    it('unregisters the tool-result handler, post-turn callback, and state contributor on cleanup', () => {
        const onSchemaApply = vi.fn();
        const appendSpy = vi.spyOn(useCopilotStore.getState(), 'appendToLastAssistantMessage');

        const {unmount} = renderHook(() =>
            usePropertyJsonSchemaBuilderCopilot({
                onSchemaApply,
                propertyPath: 'output',
                schemaRef: {current: {type: 'object'}},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() => {
            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor('updateJsonSchema', JSON.stringify({schema: {type: 'object'}}));
        });

        expect(onSchemaApply).toHaveBeenCalledTimes(1);

        unmount();

        onSchemaApply.mockClear();

        act(() => {
            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor('updateJsonSchema', JSON.stringify({schema: {type: 'array'}}));
        });

        expect(onSchemaApply).not.toHaveBeenCalled();

        act(() => {
            useCopilotPostTurnRegistry.getState().runFor(Source.JSON_SCHEMA_BUILDER);
        });

        expect(appendSpy).not.toHaveBeenCalled();

        expect(useCopilotStateContributorRegistry.getState().contribute()).not.toHaveProperty('currentJsonSchema');
    });

    it('reads the live schemaRef value when contributing state', () => {
        const schemaRef: {current: SchemaRecordType | undefined} = {current: {type: 'object'}};

        renderHook(() =>
            usePropertyJsonSchemaBuilderCopilot({
                onSchemaApply: vi.fn(),
                propertyPath: 'output',
                schemaRef,
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        schemaRef.current = {type: 'array'};

        expect(useCopilotStateContributorRegistry.getState().contribute()).toMatchObject({
            currentJsonSchema: {type: 'array'},
        });
    });
});
