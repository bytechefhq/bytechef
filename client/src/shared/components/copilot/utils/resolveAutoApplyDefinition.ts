import {MODE, Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {extractDefinitionFromMessage} from '@/shared/components/copilot/utils/extractDefinitionFromMessage';
import {ThreadMessageLike} from '@assistant-ui/react';
import {parse as yamlParse} from 'yaml';

function isWorkflowNode(value: unknown): boolean {
    if (typeof value !== 'object' || value === null || Array.isArray(value)) {
        return false;
    }

    const {name, type} = value as {name?: unknown; type?: unknown};

    return typeof name === 'string' && name.length > 0 && typeof type === 'string' && type.length > 0;
}

function isWorkflowNodeArray(value: unknown): boolean {
    return Array.isArray(value) && value.every(isWorkflowNode);
}

function isWorkflowDocument(definition: string): boolean {
    let parsed: unknown;

    try {
        parsed = JSON.parse(definition);
    } catch {
        try {
            parsed = yamlParse(definition);
        } catch {
            return false;
        }
    }

    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
        return false;
    }

    const {tasks, triggers} = parsed as {tasks?: unknown; triggers?: unknown};

    return isWorkflowNodeArray(tasks) && (triggers === undefined || isWorkflowNodeArray(triggers));
}

export function resolveAutoApplyDefinition(
    source: Source | undefined,
    mode: MODE | undefined,
    messages: ThreadMessageLike[]
): string | null {
    if (source !== Source.WORKFLOW_CODE_EDITOR || mode !== MODE.BUILD) {
        return null;
    }

    const lastAssistantMessage = [...messages].reverse().find((message) => message.role === 'assistant');

    if (!lastAssistantMessage) {
        return null;
    }

    const definition = extractDefinitionFromMessage(lastAssistantMessage.content);

    if (!definition || !isWorkflowDocument(definition)) {
        return null;
    }

    return definition;
}
