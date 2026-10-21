import {DataPillType} from '@/shared/types';
import {act, render, screen} from '@testing-library/react';
import {SuggestionKeyDownProps, SuggestionProps} from '@tiptap/suggestion';
import {createRef} from 'react';
import {describe, expect, it, vi} from 'vitest';

import PropertyMentionsInputEditorSuggestionList, {
    PropertyMentionsInputListRefType,
} from './PropertyMentionsInputEditorSuggestionList';

vi.mock('react-inlinesvg', () => ({
    default: () => <span />,
}));

function renderList(values: string[], query: string, listRef = createRef<PropertyMentionsInputListRefType>()) {
    const items: DataPillType[] = values.map((value) => ({id: value, value}));

    const props = {command: vi.fn(), items, query} as unknown as SuggestionProps<DataPillType>;

    return render(<PropertyMentionsInputEditorSuggestionList ref={listRef} {...props} />);
}

describe('PropertyMentionsInputEditorSuggestionList', () => {
    it('highlights the part of each path that matches the query', () => {
        renderList(['firecrawl_5.data.json.result', 'firecrawl_5.data.jsonResult'], 'result');

        const highlights = screen.getAllByTestId('data-pill-suggestion-match');

        expect(highlights.map((highlight) => highlight.textContent)).toEqual(['result', 'Result']);
        expect(screen.getAllByRole('button').map((button) => button.textContent)).toEqual([
            'firecrawl_5.data.json.result',
            'firecrawl_5.data.jsonResult',
        ]);
    });

    it('renders the plain path when the query is empty', () => {
        renderList(['firecrawl_5.data'], '');

        expect(screen.queryByTestId('data-pill-suggestion-match')).toBeNull();
        expect(screen.getByRole('button').textContent).toBe('firecrawl_5.data');
    });

    it('scrolls the highlighted suggestion into view when navigating with the arrow keys', () => {
        const listRef = createRef<PropertyMentionsInputListRefType>();

        renderList(['config', 'config.batches', 'config.batchSize'], '', listRef);

        const scrollIntoViewMock = vi.mocked(Element.prototype.scrollIntoView);

        scrollIntoViewMock.mockClear();

        act(() => {
            listRef.current?.onKeyDown({
                event: new KeyboardEvent('keydown', {key: 'ArrowDown'}),
            } as SuggestionKeyDownProps);
        });

        expect(scrollIntoViewMock).toHaveBeenLastCalledWith({block: 'nearest'});
        expect(scrollIntoViewMock.mock.contexts.at(-1)).toBe(screen.getAllByRole('listitem')[1]);
    });

    it('scrolls back to the first suggestion when ArrowDown wraps around from the last row', () => {
        const listRef = createRef<PropertyMentionsInputListRefType>();

        renderList(['config', 'config.batches'], '', listRef);

        const scrollIntoViewMock = vi.mocked(Element.prototype.scrollIntoView);

        act(() => {
            listRef.current?.onKeyDown({
                event: new KeyboardEvent('keydown', {key: 'ArrowDown'}),
            } as SuggestionKeyDownProps);
        });

        scrollIntoViewMock.mockClear();

        act(() => {
            listRef.current?.onKeyDown({
                event: new KeyboardEvent('keydown', {key: 'ArrowDown'}),
            } as SuggestionKeyDownProps);
        });

        expect(scrollIntoViewMock).toHaveBeenLastCalledWith({block: 'nearest'});
        expect(scrollIntoViewMock.mock.contexts.at(-1)).toBe(screen.getAllByRole('listitem')[0]);
    });

    it('scrolls the first suggestion into view when the filtered items change', () => {
        const listRef = createRef<PropertyMentionsInputListRefType>();
        const command = vi.fn();

        const {rerender} = renderList(['config', 'config.batches', 'config.batchSize'], '', listRef);

        act(() => {
            listRef.current?.onKeyDown({
                event: new KeyboardEvent('keydown', {key: 'ArrowDown'}),
            } as SuggestionKeyDownProps);
        });

        const scrollIntoViewMock = vi.mocked(Element.prototype.scrollIntoView);

        scrollIntoViewMock.mockClear();

        const filteredItems: DataPillType[] = ['config.batches', 'config.batchSize'].map((value) => ({
            id: value,
            value,
        }));

        rerender(
            <PropertyMentionsInputEditorSuggestionList
                ref={listRef}
                {...({command, items: filteredItems, query: 'batch'} as unknown as SuggestionProps<DataPillType>)}
            />
        );

        expect(scrollIntoViewMock).toHaveBeenLastCalledWith({block: 'nearest'});
        expect(scrollIntoViewMock.mock.contexts.at(-1)).toBe(screen.getAllByRole('listitem')[0]);
    });
});
