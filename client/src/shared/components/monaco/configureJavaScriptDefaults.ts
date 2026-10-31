type MonacoType = typeof import('monaco-editor');

const IMPLICIT_ANY_SUGGESTION_CODES = [7043, 7044, 7045, 7046, 7047, 7048, 7049, 7050];

export function configureJavaScriptDefaults(monaco: MonacoType): void {
    const {javascriptDefaults} = monaco.typescript;

    javascriptDefaults.setCompilerOptions({...javascriptDefaults.getCompilerOptions(), checkJs: true});
    const diagnosticsOptions = javascriptDefaults.getDiagnosticsOptions();

    javascriptDefaults.setDiagnosticsOptions({
        ...diagnosticsOptions,
        diagnosticCodesToIgnore: [
            ...(diagnosticsOptions.diagnosticCodesToIgnore ?? []),
            ...IMPLICIT_ANY_SUGGESTION_CODES,
        ],
        noSemanticValidation: true,
    });
}
