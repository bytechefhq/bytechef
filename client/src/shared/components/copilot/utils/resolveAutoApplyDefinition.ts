import {MODE, Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {extractDefinitionFromMessage} from '@/shared/components/copilot/utils/extractDefinitionFromMessage';
import {ThreadMessageLike} from '@assistant-ui/react';

/**
 * Resolve the workflow definition that should be auto-applied to the Workflow Code editor when a
 * copilot turn completes.
 *
 * Only BUILD-mode turns in the Workflow Code editor produce a workflow definition — ASK-mode replies
 * are prose and must never be loaded into the Monaco buffer. Returns null when there is nothing safe
 * to apply: wrong source/mode, no assistant reply yet, or an empty extraction.
 *
 * @param source the copilot source for the current panel
 * @param mode the live ASK/BUILD selection from the conversation context
 * @param messages the current conversation messages, oldest first
 */
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

    return definition ? definition : null;
}
