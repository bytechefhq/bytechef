import {parseJson} from '@/shared/components/ai-chat/messages/toToolResultDataPart';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {SPACE} from '@/shared/constants';
import {useCallback, useEffect, useRef, useState} from 'react';

const APPLIED_MESSAGE = '✓ Applied the sample output.';

interface UseSampleOutputCopilotParamsI {
    onApply: (value: string) => void;
    sampleOutputRef: {current: object | undefined};
    workflowId?: string;
    workflowNodeName?: string;
}

interface UseSampleOutputCopilotResultI {
    copilotPanelOpen: boolean;
    handleCopilotClose: () => void;
    handleCopilotOpen: () => void;
}

export function useSampleOutputCopilot({
    onApply,
    sampleOutputRef,
    workflowId,
    workflowNodeName,
}: UseSampleOutputCopilotParamsI): UseSampleOutputCopilotResultI {
    const [copilotPanelOpen, setCopilotPanelOpen] = useState(false);

    const pendingAppliedRef = useRef<boolean>(false);
    const conversationTokenRef = useRef<string | null>(null);

    const handleCopilotOpen = useCallback(() => {
        const {context, generateConversationId, resetMessages, saveConversationState, setContext} =
            useCopilotStore.getState();

        conversationTokenRef.current = saveConversationState();
        resetMessages();
        generateConversationId();

        setContext({
            ...context,
            mode: MODE.ASK,
            parameters: {workflowId, workflowNodeName},
            source: Source.SAMPLE_OUTPUT,
        });

        setCopilotPanelOpen(true);
    }, [workflowId, workflowNodeName]);

    const handleCopilotClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState(conversationTokenRef.current);

        setCopilotPanelOpen(false);
    }, []);

    useEffect(() => {
        const unregisterContributor = useCopilotStateContributorRegistry.getState().register(() => ({
            currentSampleOutput: sampleOutputRef.current,
            workflowId,
            workflowNodeName,
        }));

        const unregisterToolResult = useCopilotToolResultHandlerRegistry
            .getState()
            .register('updateSampleOutput', (content) => {
                const result = parseJson<{sampleOutput?: unknown}>(content, 'updateSampleOutput result');

                if (result?.sampleOutput !== undefined) {
                    pendingAppliedRef.current = true;

                    onApply(JSON.stringify(result.sampleOutput, null, SPACE));
                }
            });

        const unregisterPostTurn = useCopilotPostTurnRegistry.getState().register(Source.SAMPLE_OUTPUT, () => {
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
    }, [onApply, sampleOutputRef, workflowId, workflowNodeName]);

    return {copilotPanelOpen, handleCopilotClose, handleCopilotOpen};
}
