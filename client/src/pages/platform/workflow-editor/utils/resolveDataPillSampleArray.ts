import {getArrayIndexSegments} from './dataPillArrayIndex';
import {safeResolvePath} from './encodingUtils';

const WORKFLOW_NODE_NAME_TERMINATOR_PATTERN = /[.[]/;

export const MAX_SAMPLE_ARRAY_ROWS = 50;

export interface SampleArrayI {
    cappedItems: Array<unknown>;
    uncappedLength: number;
}

export interface ResolveDataPillSampleArrayProps {
    mentionId: string | null | undefined;
    occurrence: number;
    sampleOutputs: Record<string, unknown>;
}

export function resolveDataPillSampleArray({
    mentionId,
    occurrence,
    sampleOutputs,
}: ResolveDataPillSampleArrayProps): SampleArrayI | undefined {
    if (!mentionId) {
        return undefined;
    }

    const segment = getArrayIndexSegments(mentionId)[occurrence];

    if (!segment) {
        return undefined;
    }

    const arrayReference = mentionId.slice(0, segment.startOffset);

    const workflowNodeNameEndIndex = arrayReference.search(WORKFLOW_NODE_NAME_TERMINATOR_PATTERN);

    const hasPathAfterWorkflowNodeName = workflowNodeNameEndIndex !== -1;

    const workflowNodeName = hasPathAfterWorkflowNodeName
        ? arrayReference.slice(0, workflowNodeNameEndIndex)
        : arrayReference;

    const sampleOutput = sampleOutputs[workflowNodeName];

    if (sampleOutput == null) {
        return undefined;
    }

    let resolvedArray: unknown;

    if (hasPathAfterWorkflowNodeName) {
        const pathAfterWorkflowNodeName = arrayReference.slice(workflowNodeNameEndIndex).replace(/^\./, '');

        resolvedArray = safeResolvePath(sampleOutput as object, pathAfterWorkflowNodeName);
    } else {
        resolvedArray = sampleOutput;
    }

    if (!Array.isArray(resolvedArray)) {
        return undefined;
    }

    return {
        cappedItems: resolvedArray.slice(0, MAX_SAMPLE_ARRAY_ROWS),
        uncappedLength: resolvedArray.length,
    };
}

export function formatSampleValue(value: unknown, maxLength = 64): string {
    let formatted: string;

    if (value === null) {
        formatted = 'null';
    } else if (value === undefined) {
        formatted = 'undefined';
    } else if (typeof value === 'string') {
        formatted = `"${value}"`;
    } else if (typeof value === 'object') {
        try {
            formatted = JSON.stringify(value) ?? String(value);
        } catch {
            formatted = String(value);
        }
    } else {
        formatted = String(value);
    }

    formatted = formatted.replace(/\s+/g, ' ');

    return formatted.length > maxLength ? `${formatted.slice(0, maxLength - 1)}…` : formatted;
}
