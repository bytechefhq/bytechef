import {parseJson} from '@/shared/components/ai-chat/messages/toToolResultDataPart';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {useCallback, useEffect, useRef} from 'react';

const APPLIED_MESSAGE = '✓ Applied the condition to the rule editor.';
const PROPOSE_TOOL_NAME = 'proposeComponentRuleCondition';

interface UseComponentRuleCopilotParamsI {
    componentName: string;
    currentCondition: string;
    onConditionGenerated: (condition: string) => void;
    toolName?: string;
}

interface UseComponentRuleCopilotResultI {
    handleCopilotClose: () => void;
    openCopilot: () => void;
}

const useComponentRuleCopilot = ({
    componentName,
    currentCondition,
    onConditionGenerated,
    toolName,
}: UseComponentRuleCopilotParamsI): UseComponentRuleCopilotResultI => {
    const conversationTokenRef = useRef<string | null>(null);
    const pendingAppliedRef = useRef<boolean>(false);

    const openCopilot = useCallback(() => {
        const {context, generateConversationId, resetMessages, saveConversationState, setContext} =
            useCopilotStore.getState();

        conversationTokenRef.current = saveConversationState();

        resetMessages();
        generateConversationId();

        setContext({
            ...context,
            mode: MODE.ASK,
            parameters: {componentName, toolName},
            source: Source.COMPONENT_RULE,
        });
    }, [componentName, toolName]);

    const handleCopilotClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState(conversationTokenRef.current);
    }, []);

    useEffect(() => {
        const unregisterContributor = useCopilotStateContributorRegistry.getState().register(() => ({
            componentName,
            currentCondition,
            toolName,
        }));

        const unregisterToolResult = useCopilotToolResultHandlerRegistry
            .getState()
            .register(PROPOSE_TOOL_NAME, (content) => {
                const result = parseJson<{condition?: string; valid?: boolean}>(content, `${PROPOSE_TOOL_NAME} result`);

                if (result?.valid !== true || !result.condition) {
                    return;
                }

                pendingAppliedRef.current = true;

                onConditionGenerated(result.condition);
            });

        const unregisterPostTurn = useCopilotPostTurnRegistry.getState().register(Source.COMPONENT_RULE, () => {
            if (!pendingAppliedRef.current) {
                return;
            }

            pendingAppliedRef.current = false;

            useCopilotStore.getState().appendToLastAssistantMessage(APPLIED_MESSAGE);
        });

        return () => {
            unregisterContributor();
            unregisterToolResult();
            unregisterPostTurn();
        };
    }, [componentName, currentCondition, onConditionGenerated, toolName]);

    return {handleCopilotClose, openCopilot};
};

export default useComponentRuleCopilot;
