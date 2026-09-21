import {MODE, Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {extractDefinitionFromMessage} from '@/shared/components/copilot/utils/extractDefinitionFromMessage';
import {ThreadMessageLike} from '@assistant-ui/react';
import {parse as yamlParse} from 'yaml';

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

    return typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed);
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
