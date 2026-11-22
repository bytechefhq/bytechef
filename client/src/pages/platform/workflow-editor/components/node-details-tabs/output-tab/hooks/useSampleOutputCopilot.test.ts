import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useSampleOutputCopilot} from './useSampleOutputCopilot';

const hoisted = vi.hoisted(() => ({
    toastError: vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: {error: hoisted.toastError},
}));

describe('useSampleOutputCopilot', () => {
    beforeEach(() => {
        hoisted.toastError.mockClear();

        useCopilotPostTurnRegistry.setState({callbacks: {}});
        useCopilotStateContributorRegistry.setState({contributors: []});
        useCopilotStore.setState({context: undefined, messages: []});
        useCopilotToolResultHandlerRegistry.setState({handlers: {}});
    });

    it('sets sample-output context on open and restores on close', () => {
        const restoreSpy = vi.spyOn(useCopilotStore.getState(), 'restoreConversationState');
        const saveSpy = vi.spyOn(useCopilotStore.getState(), 'saveConversationState');

        const {result} = renderHook(() =>
            useSampleOutputCopilot({
                onApply: vi.fn(),
                sampleOutputRef: {current: {a: 1}},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() => result.current.handleCopilotOpen());

        expect(saveSpy).toHaveBeenCalled();
        expect(useCopilotStore.getState().context).toMatchObject({
            mode: MODE.ASK,
            parameters: {workflowId: 'w1', workflowNodeName: 'node1'},
            source: Source.SAMPLE_OUTPUT,
        });
        expect(result.current.copilotPanelOpen).toBe(true);

        act(() => result.current.handleCopilotClose());

        expect(restoreSpy).toHaveBeenCalled();
        expect(result.current.copilotPanelOpen).toBe(false);
    });

    it('restores the saved conversation when unmounted with the panel still open', () => {
        const {conversationId, setContext} = useCopilotStore.getState();

        setContext({mode: MODE.BUILD, parameters: {workflowId: 'editor-workflow'}, source: Source.WORKFLOW_EDITOR});

        const {result, unmount} = renderHook(() =>
            useSampleOutputCopilot({
                onApply: vi.fn(),
                sampleOutputRef: {current: undefined},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() => result.current.handleCopilotOpen());

        expect(useCopilotStore.getState().context).toMatchObject({source: Source.SAMPLE_OUTPUT});

        unmount();

        expect(useCopilotStore.getState().conversationId).toBe(conversationId);
        expect(useCopilotStore.getState().context).toMatchObject({
            mode: MODE.BUILD,
            source: Source.WORKFLOW_EDITOR,
        });
    });

    it('does not restore twice when closed before unmounting', () => {
        const {result, unmount} = renderHook(() =>
            useSampleOutputCopilot({
                onApply: vi.fn(),
                sampleOutputRef: {current: undefined},
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

    it('does not report an apply from a failed turn on the next turn', () => {
        renderHook(() =>
            useSampleOutputCopilot({
                onApply: vi.fn(),
                sampleOutputRef: {current: undefined},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() => {
            useCopilotStore.setState({messages: [{content: 'first', role: 'user'}]});

            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor('updateSampleOutput', JSON.stringify({sampleOutput: {a: 1}}));
        });

        act(() => {
            useCopilotStore.setState({
                messages: [
                    {content: 'first', role: 'user'},
                    {content: 'second', role: 'user'},
                    {content: 'Nothing to change.', role: 'assistant'},
                ],
            });

            useCopilotPostTurnRegistry.getState().runFor(Source.SAMPLE_OUTPUT);
        });

        expect(useCopilotStore.getState().messages.at(-1)).toMatchObject({content: 'Nothing to change.'});
    });

    it('applies the value from an updateSampleOutput tool result', () => {
        const onApply = vi.fn();

        renderHook(() =>
            useSampleOutputCopilot({
                onApply,
                sampleOutputRef: {current: undefined},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() =>
            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor('updateSampleOutput', JSON.stringify({sampleOutput: {a: 1}}))
        );

        expect(onApply).toHaveBeenCalledWith(JSON.stringify({a: 1}, null, 4));
        expect(hoisted.toastError).not.toHaveBeenCalled();
    });

    it('surfaces a failed updateSampleOutput tool result', () => {
        const onApply = vi.fn();

        renderHook(() =>
            useSampleOutputCopilot({
                onApply,
                sampleOutputRef: {current: undefined},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() =>
            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor('updateSampleOutput', JSON.stringify({error: 'Workflow node not found'}))
        );

        expect(onApply).not.toHaveBeenCalled();
        expect(hoisted.toastError).toHaveBeenCalledWith('Workflow node not found');
    });

    it('omits an undefined workflowId instead of clobbering another contributor', () => {
        useCopilotStateContributorRegistry.getState().register(() => ({workflowId: 'editor-workflow'}));

        renderHook(() =>
            useSampleOutputCopilot({
                onApply: vi.fn(),
                sampleOutputRef: {current: {a: 1}},
            })
        );

        expect(useCopilotStateContributorRegistry.getState().contribute()).toMatchObject({
            currentSampleOutput: {a: 1},
            workflowId: 'editor-workflow',
        });
    });
});
