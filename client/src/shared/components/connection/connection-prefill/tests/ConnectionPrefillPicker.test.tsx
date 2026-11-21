import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeAll, describe, expect, it, vi} from 'vitest';

import ConnectionPrefillPicker from '../ConnectionPrefillPicker';
import {ConnectionPrefillOptionI} from '../types';

const OPTIONS: ConnectionPrefillOptionI[] = [
    {
        componentDocumentationUrl: 'https://docs.bytechef.io/reference/components/notion_v1',
        componentName: 'notion',
        description: 'Official Notion MCP server',
        label: 'Notion',
        notes: [],
        prefill: {
            authorizationType: 'OAUTH2_AUTHORIZATION_CODE_PKCE',
            parameters: {transportType: 'httpStreamable', url: 'https://mcp.notion.com/mcp'},
        },
        value: 'notion',
        websiteUrl: 'https://developers.notion.com/docs/mcp',
    },
    {
        label: 'Databricks Genie',
        notes: ['Replace {host} in the server URL.'],
        prefill: {
            authorizationType: 'OAUTH2_AUTHORIZATION_CODE',
            parameters: {url: 'https://{host}/api/2.0/mcp/genie'},
        },
        value: 'databricks-genie',
    },
];

describe('ConnectionPrefillPicker', () => {
    beforeAll(() => {
        Element.prototype.scrollIntoView = Element.prototype.scrollIntoView || vi.fn();
        Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();

        if (!window.ResizeObserver) {
            window.ResizeObserver = class {
                disconnect() {}

                observe() {}

                unobserve() {}
            } as never;
        }
    });

    it('renders nothing when there are no options', () => {
        const {container} = render(<ConnectionPrefillPicker onSelect={vi.fn()} options={[]} />);

        expect(container).toBeEmptyDOMElement();
    });

    it('filters the servers by the search text', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        render(<ConnectionPrefillPicker onSelect={vi.fn()} options={OPTIONS} />);

        await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));

        expect(await screen.findByRole('option', {name: 'Notion'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Databricks Genie'})).toBeInTheDocument();

        await user.type(screen.getByPlaceholderText('Search...'), 'genie');

        expect(screen.queryByRole('option', {name: 'Notion'})).not.toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Databricks Genie'})).toBeInTheDocument();
    });

    it('hands the picked option over and shows its details', async () => {
        const onSelect = vi.fn();
        const user = userEvent.setup({pointerEventsCheck: 0});

        render(<ConnectionPrefillPicker onSelect={onSelect} options={OPTIONS} />);

        await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));
        await user.click(await screen.findByRole('option', {name: 'Notion'}));

        expect(onSelect).toHaveBeenCalledWith(OPTIONS[0]);
        expect(screen.getByText('Official Notion MCP server')).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Server documentation'})).toHaveAttribute(
            'href',
            'https://developers.notion.com/docs/mcp'
        );
        expect(screen.getByRole('link', {name: 'ByteChef also has a Notion component'})).toHaveAttribute(
            'href',
            'https://docs.bytechef.io/reference/components/notion_v1'
        );
    });

    it('shows the notes of the picked option', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        render(<ConnectionPrefillPicker onSelect={vi.fn()} options={OPTIONS} />);

        await user.click(screen.getByRole('combobox', {name: 'Choose a known server'}));
        await user.click(await screen.findByRole('option', {name: 'Databricks Genie'}));

        expect(screen.getByText('Replace {host} in the server URL.')).toBeInTheDocument();
        expect(screen.queryByRole('link')).not.toBeInTheDocument();
    });
});
