type MonacoType = typeof import('monaco-editor');

const IMPLICIT_ANY_SUGGESTION_CODES = [7043, 7044, 7045, 7046, 7047, 7048, 7049, 7050];

const GRAALVM_JAVASCRIPT_LIBS = ['lib.es2024.d.ts'];

export const GRAALVM_CONSOLE_LIB_PATH = 'ts:bytechef/graalvm-console.d.ts';

export const GRAALVM_CONSOLE_LIB = `declare var console: {
    debug(...data: any[]): void;
    error(...data: any[]): void;
    info(...data: any[]): void;
    log(...data: any[]): void;
    trace(...data: any[]): void;
    warn(...data: any[]): void;
};
`;

export function configureJavaScriptDefaults(monaco: MonacoType): void {
    const {javascriptDefaults} = monaco.typescript;

    javascriptDefaults.setCompilerOptions({
        ...javascriptDefaults.getCompilerOptions(),
        checkJs: true,
        lib: GRAALVM_JAVASCRIPT_LIBS,
    });
    javascriptDefaults.addExtraLib(GRAALVM_CONSOLE_LIB, GRAALVM_CONSOLE_LIB_PATH);

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
