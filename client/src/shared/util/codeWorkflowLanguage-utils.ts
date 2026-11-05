const CODE_WORKFLOW_LANGUAGE_LABELS: Record<string, string> = {
    JAVA: 'Java',
    JAVASCRIPT: 'JavaScript',
    PYTHON: 'Python',
    RUBY: 'Ruby',
};

export function isJavaCodeWorkflow(codeWorkflow?: boolean, codeWorkflowLanguage?: string): boolean {
    return codeWorkflow === true && codeWorkflowLanguage?.toUpperCase() === 'JAVA';
}

export default function getCodeWorkflowLanguageLabel(language?: string): string | undefined {
    if (!language) {
        return undefined;
    }

    return CODE_WORKFLOW_LANGUAGE_LABELS[language.toUpperCase()] ?? language;
}
