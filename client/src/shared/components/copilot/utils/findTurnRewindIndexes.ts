interface RoleMessageI {
    role: string;
}

export interface TurnRewindIndexesI {
    agentUserMessageIndex: number;
    userMessageIndex: number;
}

function findLastUserMessageIndex(messages: readonly RoleMessageI[], fromIndex: number): number {
    for (let index = Math.min(fromIndex, messages.length - 1); index >= 0; index--) {
        if (messages[index].role === 'user') {
            return index;
        }
    }

    return -1;
}

function countUserMessagesAfter(messages: readonly RoleMessageI[], index: number): number {
    return messages.slice(index + 1).filter((message) => message.role === 'user').length;
}

function findUserMessageIndexFromEnd(messages: readonly RoleMessageI[], userMessagesAfter: number): number {
    let remaining = userMessagesAfter;

    for (let index = messages.length - 1; index >= 0; index--) {
        if (messages[index].role !== 'user') {
            continue;
        }

        if (remaining === 0) {
            return index;
        }

        remaining--;
    }

    return -1;
}

/**
 * Resolves the user message that starts the turn containing the UI message at `fromIndex`, both in the UI messages
 * and in the agent transcript. One turn can render as several assistant messages and the agent keeps its own
 * assistant and tool messages, so the two lists are matched by user message, counted from the end: after a restored
 * conversation the agent only holds the latest turns.
 */
export function findTurnRewindIndexes(
    messages: readonly RoleMessageI[],
    agentMessages: readonly RoleMessageI[],
    fromIndex: number
): TurnRewindIndexesI | null {
    const userMessageIndex = findLastUserMessageIndex(messages, fromIndex);

    if (userMessageIndex === -1) {
        return null;
    }

    const agentUserMessageIndex = findUserMessageIndexFromEnd(
        agentMessages,
        countUserMessagesAfter(messages, userMessageIndex)
    );

    if (agentUserMessageIndex === -1) {
        return null;
    }

    return {agentUserMessageIndex, userMessageIndex};
}
