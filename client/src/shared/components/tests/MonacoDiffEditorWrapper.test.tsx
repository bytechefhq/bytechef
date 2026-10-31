import {render} from '@/shared/util/test-utils';
import {useEffect} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import MonacoDiffEditorWrapper from '../MonacoDiffEditorWrapper';

const hoisted = vi.hoisted(() => {
    const calls: string[] = [];

    const diffEditor = {
        getModel: vi.fn(() => ({
            modified: {dispose: vi.fn(() => calls.push('modified.dispose'))},
            original: {dispose: vi.fn(() => calls.push('original.dispose'))},
        })),
        setModel: vi.fn((model: unknown) => calls.push(`setModel(${model === null ? 'null' : 'model'})`)),
    };

    return {calls, diffEditor, diffEditorProps: [] as Array<Record<string, unknown>>};
});

vi.mock('@monaco-editor/react', () => ({
    DiffEditor: (props: {onMount?: (editor: unknown) => void} & Record<string, unknown>) => {
        hoisted.diffEditorProps.push(props);

        useEffect(() => {
            props.onMount?.(hoisted.diffEditor);
            // eslint-disable-next-line react-hooks/exhaustive-deps
        }, []);

        return <div data-testid="mock-diff-editor" />;
    },
}));

vi.mock('@/shared/components/monacoSetup', () => ({
    ensureMonacoConfigured: vi.fn(() => Promise.resolve()),
    getMonacoInstance: vi.fn(() => undefined),
    isMonacoConfigured: vi.fn(() => true),
}));

describe('MonacoDiffEditorWrapper', () => {
    beforeEach(() => {
        hoisted.calls.length = 0;
        hoisted.diffEditorProps.length = 0;
    });

    it('keeps the models for the wrapper to dispose', () => {
        render(<MonacoDiffEditorWrapper language="json" modified="{}" original="{}" />);

        expect(hoisted.diffEditorProps[0]).toMatchObject({
            keepCurrentModifiedModel: true,
            keepCurrentOriginalModel: true,
        });
    });

    it('resets the editor model before disposing the models on unmount', () => {
        const {unmount} = render(<MonacoDiffEditorWrapper language="json" modified="{}" original="{}" />);

        expect(hoisted.calls).toEqual([]);

        unmount();

        expect(hoisted.calls).toEqual(['setModel(null)', 'original.dispose', 'modified.dispose']);
    });
});
