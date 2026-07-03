import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const appendCalls: Array<unknown> = [];

vi.mock('@assistant-ui/react', async () => {
    const actual = await vi.importActual<typeof import('@assistant-ui/react')>('@assistant-ui/react');

    return {
        ...actual,
        useThreadRuntime: vi.fn(() => ({
            append: (message: unknown) => appendCalls.push(message),
            getState: () => ({messages: []}),
            subscribe: () => () => {},
        })),
    };
});

import SelectPropertyOptionMessage from '../SelectPropertyOptionMessage';

const DATA = {
    componentName: 'slack',
    kind: 'select-property-option' as const,
    options: [
        {label: 'general', value: 'C06H2PR8LSV'},
        {label: 'random', value: 'C06GSJ5RPBN'},
    ],
    propertyName: 'channel',
    truncated: false,
};

describe('SelectPropertyOptionMessage', () => {
    beforeEach(() => {
        appendCalls.length = 0;
    });

    it('renders a picker of all options and submits the picked option value', async () => {
        render(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            <SelectPropertyOptionMessage {...({data: DATA} as any)} />
        );

        await userEvent.click(screen.getByRole('combobox'));

        await userEvent.click(screen.getByRole('option', {name: 'general'}));

        expect(appendCalls).toHaveLength(1);
        expect(JSON.stringify(appendCalls[0])).toContain('User picked: general (value: C06H2PR8LSV)');
    });

    it('filters by label when the user types in the search', async () => {
        render(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            <SelectPropertyOptionMessage {...({data: DATA} as any)} />
        );

        await userEvent.click(screen.getByRole('combobox'));

        const searchBox = screen.getByPlaceholderText('Search...');

        await userEvent.type(searchBox, 'gener');

        expect(screen.getByRole('option', {name: 'general'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'random'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('option', {name: 'general'}));

        expect(JSON.stringify(appendCalls[0])).toContain('User picked: general (value: C06H2PR8LSV)');
    });
});
