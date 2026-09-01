const ARRAY_INDEX_PLACEHOLDER = '[index]';

const NUMERIC_BRACKET_SEGMENT_PATTERN = /\[(\d+)\]/g;

const ARRAY_INDEX_PLACEHOLDER_PATTERN = /\[index\]/g;

const DOT_BEFORE_ARRAY_INDEX_PLACEHOLDER = `.${ARRAY_INDEX_PLACEHOLDER}`;

export interface ArrayIndexSegmentI {
    arrayIndex: number;
    endOffsetExclusive: number;
    startOffset: number;
}

export type ArrayIndexPartType =
    | {text: string; type: 'literal'}
    | {arrayIndex: number; occurrence: number; type: 'arrayIndex'};

export interface SetArrayIndexProps {
    arrayIndex: number;
    occurrence: number;
    reference: string;
}

export function getArrayIndexSegments(reference: string): Array<ArrayIndexSegmentI> {
    if (!reference) {
        return [];
    }

    const segments: Array<ArrayIndexSegmentI> = [];

    for (const match of reference.matchAll(NUMERIC_BRACKET_SEGMENT_PATTERN)) {
        if (match.index === undefined) {
            continue;
        }

        segments.push({
            arrayIndex: Number(match[1]),
            endOffsetExclusive: match.index + match[0].length,
            startOffset: match.index,
        });
    }

    return segments;
}

export function setArrayIndex({arrayIndex, occurrence, reference}: SetArrayIndexProps): string {
    const segments = getArrayIndexSegments(reference);

    const segment = segments[occurrence];

    if (!segment) {
        return reference;
    }

    const beforeSegment = reference.slice(0, segment.startOffset);

    const afterSegment = reference.slice(segment.endOffsetExclusive);

    return `${beforeSegment}[${arrayIndex}]${afterSegment}`;
}

export function toArrayIndexTemplate(reference: string): string {
    if (!reference) {
        return reference;
    }

    return reference.replace(NUMERIC_BRACKET_SEGMENT_PATTERN, ARRAY_INDEX_PLACEHOLDER);
}

export function resolveArrayIndexTemplate(reference: string, arrayIndex = 0): string {
    if (!reference) {
        return reference;
    }

    const collapsedReference = reference.replaceAll(DOT_BEFORE_ARRAY_INDEX_PLACEHOLDER, ARRAY_INDEX_PLACEHOLDER);

    return collapsedReference.replace(ARRAY_INDEX_PLACEHOLDER_PATTERN, `[${arrayIndex}]`);
}

export function splitByArrayIndex(reference: string): Array<ArrayIndexPartType> {
    const segments = getArrayIndexSegments(reference);

    if (!segments.length) {
        return [{text: reference, type: 'literal'}];
    }

    const parts: Array<ArrayIndexPartType> = [];

    let cursorOffset = 0;

    segments.forEach((segment, occurrence) => {
        if (segment.startOffset > cursorOffset) {
            parts.push({text: reference.slice(cursorOffset, segment.startOffset), type: 'literal'});
        }

        parts.push({arrayIndex: segment.arrayIndex, occurrence, type: 'arrayIndex'});

        cursorOffset = segment.endOffsetExclusive;
    });

    if (cursorOffset < reference.length) {
        parts.push({text: reference.slice(cursorOffset), type: 'literal'});
    }

    return parts;
}
