import {describe, expect, it, vi} from 'vitest';

import {configureJavaScriptDefaults} from './configureJavaScriptDefaults';

describe('configureJavaScriptDefaults', () => {
    it('checks JavaScript so the language service stops listing every identifier, without type errors or implicit-any hints', () => {
        const javascriptDefaults = {
            getCompilerOptions: vi.fn(() => ({allowNonTsExtensions: true})),
            getDiagnosticsOptions: vi.fn(() => ({noSyntaxValidation: false})),
            setCompilerOptions: vi.fn(),
            setDiagnosticsOptions: vi.fn(),
        };

        configureJavaScriptDefaults({typescript: {javascriptDefaults}} as never);

        expect(javascriptDefaults.setCompilerOptions).toHaveBeenCalledWith({allowNonTsExtensions: true, checkJs: true});
        expect(javascriptDefaults.setDiagnosticsOptions).toHaveBeenCalledWith({
            diagnosticCodesToIgnore: [7043, 7044, 7045, 7046, 7047, 7048, 7049, 7050],
            noSemanticValidation: true,
            noSyntaxValidation: false,
        });
    });
});
