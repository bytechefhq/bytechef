import {renderHook} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import {getCompletionContext} from './completionContextRegistry';
import {useCompletionContext} from './useCompletionContext';

import type {editor} from 'monaco-editor';

describe('useCompletionContext', () => {
    const makeModel = (uri: string) => ({uri: {toString: () => uri}}) as unknown as editor.ITextModel;

    it('registers on mount and unregisters on unmount', () => {
        const {unmount} = renderHook(() =>
            useCompletionContext(makeModel('inmemory://a'), {kind: 'customComponent', language: 'javascript'})
        );

        expect(getCompletionContext('inmemory://a')?.kind).toBe('customComponent');

        unmount();

        expect(getCompletionContext('inmemory://a')).toBeUndefined();
    });

    it('keeps two mounted editors apart', () => {
        const first = renderHook(() =>
            useCompletionContext(makeModel('inmemory://a'), {kind: 'customComponent', language: 'javascript'})
        );

        renderHook(() => useCompletionContext(makeModel('inmemory://b'), {kind: 'customComponent', language: 'ruby'}));

        first.unmount();

        expect(getCompletionContext('inmemory://a')).toBeUndefined();
        expect(getCompletionContext('inmemory://b')).toMatchObject({language: 'ruby'});
    });

    it('does nothing without a model', () => {
        renderHook(() => useCompletionContext(undefined, {kind: 'customComponent', language: 'javascript'}));

        expect(getCompletionContext('undefined')).toBeUndefined();
    });
});
