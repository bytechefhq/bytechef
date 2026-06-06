import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useSampleOutputCopilot} from './useSampleOutputCopilot';

describe('useSampleOutputCopilot', () => {
    beforeEach(() => {
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
    });
});
