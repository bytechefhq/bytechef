import {describe, expect, it, vi} from 'vitest';

import {
    GRAALVM_CONSOLE_LIB,
    GRAALVM_CONSOLE_LIB_PATH,
    configureJavaScriptDefaults,
} from './configureJavaScriptDefaults';

describe('configureJavaScriptDefaults', () => {
    it('checks JavaScript so the language service stops listing every identifier, without type errors or implicit-any hints', () => {
        const javascriptDefaults = {
            addExtraLib: vi.fn(),
            getCompilerOptions: vi.fn(() => ({allowNonTsExtensions: true})),
            getDiagnosticsOptions: vi.fn(() => ({noSyntaxValidation: false})),
            setCompilerOptions: vi.fn(),
            setDiagnosticsOptions: vi.fn(),
        };

        configureJavaScriptDefaults({typescript: {javascriptDefaults}} as never);

        expect(javascriptDefaults.setCompilerOptions).toHaveBeenCalledWith(
            expect.objectContaining({allowNonTsExtensions: true, checkJs: true})
        );
        expect(javascriptDefaults.setDiagnosticsOptions).toHaveBeenCalledWith({
            diagnosticCodesToIgnore: [7043, 7044, 7045, 7046, 7047, 7048, 7049, 7050],
            noSemanticValidation: true,
            noSyntaxValidation: false,
        });
    });

    it('loads only the ECMAScript library, since scripts run in GraalVM where browser globals do not exist', () => {
        const javascriptDefaults = {
            addExtraLib: vi.fn(),
            getCompilerOptions: vi.fn(() => ({})),
            getDiagnosticsOptions: vi.fn(() => ({})),
            setCompilerOptions: vi.fn(),
            setDiagnosticsOptions: vi.fn(),
        };

        configureJavaScriptDefaults({typescript: {javascriptDefaults}} as never);

        expect(javascriptDefaults.setCompilerOptions).toHaveBeenCalledWith(
            expect.objectContaining({lib: ['lib.es2024.d.ts']})
        );
        expect(javascriptDefaults.addExtraLib).toHaveBeenCalledWith(GRAALVM_CONSOLE_LIB, GRAALVM_CONSOLE_LIB_PATH);
        expect(GRAALVM_CONSOLE_LIB).toContain('declare var console');
    });
});
