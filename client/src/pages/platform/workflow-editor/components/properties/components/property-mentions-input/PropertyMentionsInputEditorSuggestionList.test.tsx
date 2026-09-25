import {DataPillType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {SuggestionProps} from '@tiptap/suggestion';
import {describe, expect, it, vi} from 'vitest';

import PropertyMentionsInputEditorSuggestionList from './PropertyMentionsInputEditorSuggestionList';

vi.mock('react-inlinesvg', () => ({
    default: () => <span />,
}));

function renderList(values: string[], query: string) {
    const items: DataPillType[] = values.map((value) => ({id: value, value}));

    const props = {command: vi.fn(), items, query} as unknown as SuggestionProps<DataPillType>;

    return render(<PropertyMentionsInputEditorSuggestionList {...props} />);
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
});
