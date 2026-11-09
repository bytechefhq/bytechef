export const VARIABLE_PATH_FUNCTION_NAMES = ['exists', 'getOrDefault'];

const VARIABLE_PATH_ARGUMENT = new RegExp(
    `(?:^|[^\\w.])(?:${VARIABLE_PATH_FUNCTION_NAMES.join('|')})\\(\\s*['"]([\\w.\\[\\]-]*)$`
);

interface VariablePathSuggestionMatchI {
    query: string;
    range: {from: number; to: number};
    text: string;
}

export function findVariablePathSuggestionMatch({
    $position,
}: {
    $position: {nodeBefore: {isText: boolean; text?: string | null} | null; pos: number};
}): VariablePathSuggestionMatchI | null {
    const nodeBefore = $position.nodeBefore;

    if (!nodeBefore || !nodeBefore.isText || !nodeBefore.text) {
        return null;
    }

    const match = VARIABLE_PATH_ARGUMENT.exec(nodeBefore.text);

    if (!match) {
        return null;
    }

    const query = match[1];
    const to = $position.pos;

    return {query, range: {from: to - query.length, to}, text: query};
}
