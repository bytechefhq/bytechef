import useComponentRuleCopilot from '@/ee/pages/settings/automation/ai/rules/hooks/useComponentRuleCopilot';
import {Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

describe('useComponentRuleCopilot', () => {
    beforeEach(() => {
        useCopilotStore.setState({context: {mode: 'ASK', parameters: {}, source: Source.WORKFLOW_EDITOR}} as never);
    });

    it('opens the panel scoped to the component and tool', () => {
        const {result} = renderHook(() =>
            useComponentRuleCopilot({
                componentName: 'slack',
                currentCondition: '',
                onConditionGenerated: vi.fn(),
                toolName: 'sendMessage',
            })
        );

        act(() => {
            result.current.openCopilot();
        });

        const {context} = useCopilotStore.getState();

        expect(context.source).toBe(Source.COMPONENT_RULE);
        expect(context.parameters).toEqual({componentName: 'slack', toolName: 'sendMessage'});
    });

    it('restores the prior copilot conversation when the panel closes', () => {
        const restoreSpy = vi.spyOn(useCopilotStore.getState(), 'restoreConversationState');
        const saveSpy = vi.spyOn(useCopilotStore.getState(), 'saveConversationState');

        const {result} = renderHook(() =>
            useComponentRuleCopilot({
                componentName: 'slack',
                currentCondition: '',
                onConditionGenerated: vi.fn(),
                toolName: 'sendMessage',
            })
        );

        act(() => {
            result.current.openCopilot();
        });

        const savedToken = saveSpy.mock.results[0]?.value;

        expect(savedToken).toBeTruthy();

        act(() => {
            result.current.handleCopilotClose();
        });

        expect(restoreSpy).toHaveBeenCalledWith(savedToken);
    });

    it('applies a valid proposed condition', () => {
        const onConditionGenerated = vi.fn();

        renderHook(() =>
            useComponentRuleCopilot({
                componentName: 'slack',
                currentCondition: '',
                onConditionGenerated,
                toolName: 'sendMessage',
            })
        );

        act(() => {
            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor(
                    'proposeComponentRuleCondition',
                    JSON.stringify({condition: "contains(inputParameters['channel'], 'C05')", valid: true})
                );
        });

        expect(onConditionGenerated).toHaveBeenCalledWith("contains(inputParameters['channel'], 'C05')");
    });

    it('ignores a proposal the server marked invalid', () => {
        const onConditionGenerated = vi.fn();

        renderHook(() =>
            useComponentRuleCopilot({
                componentName: 'slack',
                currentCondition: '',
                onConditionGenerated,
            })
        );

        act(() => {
            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor(
                    'proposeComponentRuleCondition',
                    JSON.stringify({condition: 'bad(', error: 'nope', valid: false})
                );
        });

        expect(onConditionGenerated).not.toHaveBeenCalled();
    });

    it('ignores a proposal marked valid but with an empty or missing condition', () => {
        const onConditionGenerated = vi.fn();

        renderHook(() =>
            useComponentRuleCopilot({
                componentName: 'slack',
                currentCondition: '',
                onConditionGenerated,
            })
        );

        act(() => {
            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor('proposeComponentRuleCondition', JSON.stringify({condition: '', valid: true}));
        });

        act(() => {
            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor('proposeComponentRuleCondition', JSON.stringify({valid: true}));
        });

        expect(onConditionGenerated).not.toHaveBeenCalled();
    });

    it('stops applying proposals once unmounted', () => {
        const onConditionGenerated = vi.fn();

        const {unmount} = renderHook(() =>
            useComponentRuleCopilot({
                componentName: 'slack',
                currentCondition: '',
                onConditionGenerated,
            })
        );

        act(() => {
            unmount();
        });

        act(() => {
            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor(
                    'proposeComponentRuleCondition',
                    JSON.stringify({condition: "contains(inputParameters['channel'], 'C05')", valid: true})
                );
        });

        expect(onConditionGenerated).not.toHaveBeenCalled();
    });
});
