import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {usePropertyJsonSchemaBuilderCopilot} from './usePropertyJsonSchemaBuilderCopilot';

const hoisted = vi.hoisted(() => ({
    toastError: vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: {error: hoisted.toastError},
}));

describe('usePropertyJsonSchemaBuilderCopilot', () => {
    beforeEach(() => {
        hoisted.toastError.mockClear();

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

    it('keeps the original conversation when opened twice before closing', () => {
        useCopilotStore.setState({conversationStack: []});

        const {conversationId, setContext} = useCopilotStore.getState();

        setContext({mode: MODE.BUILD, parameters: {workflowId: 'editor-workflow'}, source: Source.WORKFLOW_EDITOR});

        const {result} = renderHook(() =>
            usePropertyJsonSchemaBuilderCopilot({
                onSchemaApply: vi.fn(),
                propertyPath: 'output',
                schemaRef: {current: undefined},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() => result.current.handleCopilotOpen());
        act(() => result.current.handleCopilotOpen());

        expect(useCopilotStore.getState().conversationStack).toHaveLength(1);

        act(() => result.current.handleCopilotClose());

        expect(useCopilotStore.getState().conversationStack).toHaveLength(0);
        expect(useCopilotStore.getState().conversationId).toBe(conversationId);
        expect(useCopilotStore.getState().context).toMatchObject({source: Source.WORKFLOW_EDITOR});
    });

    it('restores the saved conversation when unmounted with the panel still open', () => {
        useCopilotStore.setState({conversationStack: []});

        const {conversationId, setContext} = useCopilotStore.getState();

        setContext({mode: MODE.BUILD, parameters: {workflowId: 'editor-workflow'}, source: Source.WORKFLOW_EDITOR});

        const {result, unmount} = renderHook(() =>
            usePropertyJsonSchemaBuilderCopilot({
                onSchemaApply: vi.fn(),
                propertyPath: 'output',
                schemaRef: {current: undefined},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() => result.current.handleCopilotOpen());

        expect(useCopilotStore.getState().context).toMatchObject({source: Source.JSON_SCHEMA_BUILDER});

        unmount();

        expect(useCopilotStore.getState().conversationId).toBe(conversationId);
        expect(useCopilotStore.getState().conversationStack).toHaveLength(0);
        expect(useCopilotStore.getState().context).toMatchObject({
            mode: MODE.BUILD,
            source: Source.WORKFLOW_EDITOR,
        });
    });

    it('does not restore twice when closed before unmounting', () => {
        useCopilotStore.setState({conversationStack: []});

        const {result, unmount} = renderHook(() =>
            usePropertyJsonSchemaBuilderCopilot({
                onSchemaApply: vi.fn(),
                propertyPath: 'output',
                schemaRef: {current: undefined},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() => result.current.handleCopilotOpen());
        act(() => result.current.handleCopilotClose());

        const restoreSpy = vi.spyOn(useCopilotStore.getState(), 'restoreConversationState');

        unmount();

        expect(restoreSpy).toHaveBeenCalledWith(null);
        expect(useCopilotStore.getState().conversationStack).toHaveLength(0);
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
        expect(hoisted.toastError).not.toHaveBeenCalled();
    });

    it.each([['"a string"'], ['[{"type":"object"}]'], ['42'], ['null']])(
        'does not apply a non-object schema %s',
        (schemaJson) => {
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
                useCopilotToolResultHandlerRegistry.getState().runFor('updateJsonSchema', `{"schema":${schemaJson}}`)
            );

            expect(onSchemaApply).not.toHaveBeenCalled();
            expect(hoisted.toastError).toHaveBeenCalledWith('The assistant could not update the schema.');
        }
    );

    it('surfaces a failed updateJsonSchema tool result', () => {
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
                .runFor('updateJsonSchema', JSON.stringify({error: 'The property no longer exists'}))
        );

        expect(onSchemaApply).not.toHaveBeenCalled();
        expect(hoisted.toastError).toHaveBeenCalledWith('The property no longer exists');
    });

    it('does not report an apply from a failed turn on the next turn', () => {
        renderHook(() =>
            usePropertyJsonSchemaBuilderCopilot({
                onSchemaApply: vi.fn(),
                propertyPath: 'output',
                schemaRef: {current: undefined},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() => {
            useCopilotStore.setState({messages: [{content: 'first', role: 'user'}]});

            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor('updateJsonSchema', JSON.stringify({schema: {type: 'object'}}));
        });

        act(() => {
            useCopilotStore.setState({
                messages: [
                    {content: 'first', role: 'user'},
                    {content: 'second', role: 'user'},
                    {content: 'Nothing to change.', role: 'assistant'},
                ],
            });

            useCopilotPostTurnRegistry.getState().runFor(Source.JSON_SCHEMA_BUILDER);
        });

        expect(useCopilotStore.getState().messages.at(-1)).toMatchObject({content: 'Nothing to change.'});
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
