import type {ScriptLanguageType} from './completionTypes';

export function buildActionCallSnippet(
    actionName: string,
    language: ScriptLanguageType,
    matchingConnectionNames: string[],
    acceptsClusterElements: boolean
): string {
    void language;

    const parts = ['{$1}'];

    let nextStop = 2;

    if (matchingConnectionNames.length === 1) {
        parts.push(`'${matchingConnectionNames[0]}'`);

        nextStop = 3;
    } else if (matchingConnectionNames.length > 1) {
        parts.push("'$2'");

        nextStop = 3;
    }

    if (acceptsClusterElements) {
        parts.push(`{$${nextStop}}`);
    }

    return `${actionName}(${parts.join(', ')})`;
}

export function buildFunctionSnippet(functionName: string): string {
    return `${functionName}($1)`;
}
