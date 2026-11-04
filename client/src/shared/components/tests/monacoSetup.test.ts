import {beforeAll, describe, expect, it, vi} from 'vitest';

vi.mock('monaco-editor/editor/editor.worker.js?worker', () => ({default: class EditorWorker {}}));
vi.mock('monaco-editor/language/json/json.worker.js?worker', () => ({default: class JsonWorker {}}));
vi.mock('monaco-editor/language/typescript/ts.worker.js?worker', () => ({default: class TsWorker {}}));
vi.mock('monaco-yaml/yaml.worker?worker', () => ({default: class YamlWorker {}}));

let monacoSetup: typeof import('../monacoSetup');

describe('monacoSetup', () => {
    beforeAll(async () => {
        Object.assign(document, {queryCommandSupported: () => false});
        window.matchMedia ??= (query: string) =>
            ({
                addEventListener: () => {},
                addListener: () => {},
                matches: false,
                media: query,
                removeEventListener: () => {},
                removeListener: () => {},
            }) as unknown as MediaQueryList;

        monacoSetup = await import('../monacoSetup');

        await monacoSetup.ensureMonacoConfigured();
    });

    it('configures one Monaco instance', () => {
        expect(monacoSetup.isMonacoConfigured()).toBe(true);
        expect(monacoSetup.getMonacoInstance()).toBeDefined();
    });

    it.each([
        ['javascript', "function perform(input) {\n    return 'value';\n}", 'keyword'],
        ['python', "def perform(input):\n    return 'value'", 'keyword'],
    ])('highlights %s code', async (language, code, tokenType) => {
        const monaco = monacoSetup.getMonacoInstance()!;

        const html = await monaco.editor.colorize(code, language, {});

        expect(html).toContain('mtk');

        const tokens = monaco.editor.tokenize(code, language);

        expect(tokens.flat().map((token: {type: string}) => token.type)).toContainEqual(
            expect.stringContaining(tokenType)
        );
    });

    it('inserts a new line on enter in javascript', async () => {
        const monaco = monacoSetup.getMonacoInstance()!;

        await monaco.editor.colorize('', 'javascript', {});

        const model = monaco.editor.createModel('function perform(input) {\n}', 'javascript');
        const container = document.createElement('div');
        const editor = monaco.editor.create(container, {model});

        editor.setPosition({column: 26, lineNumber: 1});
        editor.trigger('keyboard', 'type', {text: '\n'});

        expect(model.getLineCount()).toBe(3);
        expect(model.getLineContent(1)).toBe('function perform(input) {');
        expect(model.getLineContent(3)).toBe('}');

        editor.dispose();
        model.dispose();
    });
});
