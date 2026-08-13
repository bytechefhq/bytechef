import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {parseJson} from '@/shared/components/ai-chat/messages/toToolResultDataPart';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {useCallback, useEffect, useRef, useState} from 'react';

const APPLIED_MESSAGE = '✓ Applied the schema to the builder.';

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

    const pendingSchemaRef = useRef<SchemaRecordType | null>(null);
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
            parameters: {propertyPath, title, workflowId, workflowNodeName},
            source: Source.JSON_SCHEMA_BUILDER,
        });

        setCopilotPanelOpen(true);
    }, [propertyPath, title, workflowId, workflowNodeName]);

    const handleCopilotClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState(conversationTokenRef.current);

        setCopilotPanelOpen(false);
    }, []);

    useEffect(() => {
        const unregisterContributor = useCopilotStateContributorRegistry.getState().register(() => ({
            currentJsonSchema: schemaRef.current,
            propertyPath,
            workflowId,
            workflowNodeName,
        }));

        const unregisterToolResult = useCopilotToolResultHandlerRegistry
            .getState()
            .register('updateJsonSchema', (content) => {
                const result = parseJson<{schema?: SchemaRecordType}>(content, 'updateJsonSchema result');

                if (result?.schema) {
                    pendingSchemaRef.current = result.schema;

                    onSchemaApply(result.schema);
                }
            });

        const unregisterPostTurn = useCopilotPostTurnRegistry.getState().register(Source.JSON_SCHEMA_BUILDER, () => {
            const schema = pendingSchemaRef.current;

            pendingSchemaRef.current = null;

            if (schema == null) {
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

    return {copilotPanelOpen, handleCopilotClose, handleCopilotOpen};
}
