const ARRAY_INDEX_SEGMENT_PATTERN = /^\[(index|\d+)\]$/;

const IDENTIFIER_PATTERN = /^[a-zA-Z_]\w*$/;

export function appendDataPillPathSegment(reference: string, segment: string): string {
    if (ARRAY_INDEX_SEGMENT_PATTERN.test(segment)) {
        return `${reference}${segment}`;
    }

    if (IDENTIFIER_PATTERN.test(segment)) {
        return `${reference}.${segment}`;
    }

    return `${reference}['${segment}']`;
}

export function buildDataPillPath(workflowNodeName: string, segments: Array<string>): string {
    return segments.reduce((reference, segment) => appendDataPillPathSegment(reference, segment), workflowNodeName);
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function getDataPillSampleValue(sampleOutput: any, segments: Array<string>): any {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return segments.reduce((value: any, segment) => {
        if (value === null || value === undefined) {
            return undefined;
        }

        const arrayIndexMatch = ARRAY_INDEX_SEGMENT_PATTERN.exec(segment);

        if (arrayIndexMatch) {
            if (!Array.isArray(value)) {
                return undefined;
            }

            const arrayIndex = arrayIndexMatch[1] === 'index' ? 0 : Number(arrayIndexMatch[1]);

            return value[arrayIndex];
        }

        const container = Array.isArray(value) ? value[0] : value;

        return typeof container === 'object' && container !== null ? container[segment] : undefined;
    }, sampleOutput);
}
