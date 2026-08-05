const CODE_WORKFLOW_LANGUAGE_LABELS: Record<string, string> = {
    JAVA: 'Java',
    JAVASCRIPT: 'JavaScript',
    PYTHON: 'Python',
    RUBY: 'Ruby',
};

/**
 * Whether a code-backed project or integration is Java-backed. Its workflows come from a compiled JAR, so there is no
 * source to edit and no canvas worth opening — the editor is not reachable for it at all.
 */
export function isJavaCodeWorkflow(codeWorkflow?: boolean, codeWorkflowLanguage?: string): boolean {
    return codeWorkflow === true && codeWorkflowLanguage?.toUpperCase() === 'JAVA';
}

/**
 * Turns a code workflow language into its display name ('JAVASCRIPT' -> 'JavaScript'). An unmapped value is returned
 * as-is so a newly supported language shows something readable rather than nothing.
 */
export default function getCodeWorkflowLanguageLabel(language?: string): string | undefined {
    if (!language) {
        return undefined;
    }

    return CODE_WORKFLOW_LANGUAGE_LABELS[language.toUpperCase()] ?? language;
}
