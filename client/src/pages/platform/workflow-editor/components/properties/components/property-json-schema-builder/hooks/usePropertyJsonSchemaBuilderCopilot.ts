import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {parseJson} from '@/shared/components/ai-chat/messages/toToolResultDataPart';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {getUserTurnCount} from '@/shared/components/copilot/utils/getUserTurnCount';
import {useCallback, useEffect, useRef, useState} from 'react';
import {toast} from 'sonner';

const APPLIED_MESSAGE = '✓ Applied the schema to the builder.';

const isSchemaRecord = (value: unknown): value is SchemaRecordType =>
    typeof value === 'object' && value !== null && !Array.isArray(value);

interface UsePropertyJsonSchemaBuilderCopilotParamsI {
    onSchemaApply: (schema: SchemaRecordType) => void;
    propertyPath?: string;
    schemaRef: {current: SchemaRecordType | undefined};
    title?: string;
    workflowId?: string;
    workflowNodeName?: string;
}

interface UsePropertyJsonSchemaBuilderCopilotResultI {
    copilotPanelOpen: boolean;
    handleCopilotClose: () => void;
    handleCopilotOpen: () => void;
}

export function usePropertyJsonSchemaBuilderCopilot({
    onSchemaApply,
    propertyPath,
    schemaRef,
    title,
    workflowId,
    workflowNodeName,
}: UsePropertyJsonSchemaBuilderCopilotParamsI): UsePropertyJsonSchemaBuilderCopilotResultI {
    const [copilotPanelOpen, setCopilotPanelOpen] = useState(false);

    const appliedTurnRef = useRef<number | null>(null);
    const conversationTokenRef = useRef<string | null>(null);

    const handleCopilotOpen = useCallback(() => {
        if (conversationTokenRef.current !== null) {
            return;
        }

        const {context, generateConversationId, resetMessages, saveConversationState, setContext} =
            useCopilotStore.getState();

        conversationTokenRef.current = saveConversationState();
        resetMessages();
        generateConversationId();

        setContext({
            ...context,
            mode: MODE.ASK,
            parameters: {propertyPath, title, workflowId, workflowNodeName},
            source: Source.JSON_SCHEMA_BUILDER,
        });

        setCopilotPanelOpen(true);
    }, [propertyPath, title, workflowId, workflowNodeName]);

    const handleCopilotClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState(conversationTokenRef.current);

        conversationTokenRef.current = null;

        setCopilotPanelOpen(false);
    }, []);

    useEffect(() => {
        const unregisterContributor = useCopilotStateContributorRegistry.getState().register(() => ({
            currentJsonSchema: schemaRef.current,
            ...(propertyPath === undefined ? {} : {propertyPath}),
            ...(workflowId === undefined ? {} : {workflowId}),
            ...(workflowNodeName === undefined ? {} : {workflowNodeName}),
        }));

        const unregisterToolResult = useCopilotToolResultHandlerRegistry
            .getState()
            .register('updateJsonSchema', (content) => {
                const result = parseJson<{error?: unknown; schema?: unknown}>(content, 'updateJsonSchema result');

                if (isSchemaRecord(result?.schema)) {
                    appliedTurnRef.current = getUserTurnCount();

                    onSchemaApply(result.schema);

                    return;
                }

                const errorMessage = typeof result?.error === 'string' ? result.error : null;

                toast.error(errorMessage ?? 'The assistant could not update the schema.');
            });

        const unregisterPostTurn = useCopilotPostTurnRegistry.getState().register(Source.JSON_SCHEMA_BUILDER, () => {
            const appliedTurn = appliedTurnRef.current;

            appliedTurnRef.current = null;

            if (appliedTurn === null || appliedTurn !== getUserTurnCount()) {
                return;
            }

            useCopilotStore.getState().appendToLastAssistantMessage(APPLIED_MESSAGE);
        });

        return () => {
            unregisterContributor();
            unregisterToolResult();
            unregisterPostTurn();
        };
    }, [onSchemaApply, propertyPath, schemaRef, workflowId, workflowNodeName]);

    useEffect(
        () => () => {
            useCopilotStore.getState().restoreConversationState(conversationTokenRef.current);

            conversationTokenRef.current = null;
        },
        []
    );

    return {copilotPanelOpen, handleCopilotClose, handleCopilotOpen};
}
