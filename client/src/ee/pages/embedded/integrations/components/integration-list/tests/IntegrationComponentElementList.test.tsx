import IntegrationComponentElementList from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationComponentElementList';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

const elements = [
    {description: 'Sends an email', id: 'GMAIL_SEND_EMAIL', title: 'Send Email'},
    {description: undefined, id: 'GMAIL_LIST_LABELS', title: 'List Labels'},
];

const renderList = (overrides: Partial<Parameters<typeof IntegrationComponentElementList>[0]> = {}) =>
    render(
        <IntegrationComponentElementList
            elements={elements}
            idLabel="Tool ID"
            isError={false}
            isLoading={false}
            noun="tools"
            onRetry={vi.fn()}
            {...overrides}
        />
    );

describe('IntegrationComponentElementList', () => {
    it('shows the description and id of an expanded element', async () => {
        renderList();

        await userEvent.click(screen.getByRole('button', {name: 'Send Email'}));

        expect(screen.getByText('Sends an email')).toBeInTheDocument();
        expect(screen.getByText('Tool ID')).toBeInTheDocument();
        expect(screen.getByText('GMAIL_SEND_EMAIL')).toBeInTheDocument();
    });

    it('filters elements by the search text', async () => {
        renderList();

        await userEvent.type(screen.getByRole('textbox', {name: 'Search tools'}), 'labels');

        expect(screen.getByRole('button', {name: 'List Labels'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Send Email'})).not.toBeInTheDocument();
    });

    it('says when no element matches the search text', async () => {
        renderList();

        await userEvent.type(screen.getByRole('textbox', {name: 'Search tools'}), 'nothing');

        expect(screen.getByText('No tools match your search.')).toBeInTheDocument();
    });

    it('shows an empty state when the component has no elements', () => {
        renderList({elements: []});

        expect(screen.getByText('No tools')).toBeInTheDocument();
    });

    it('offers a retry when loading failed', async () => {
        const onRetry = vi.fn();

        renderList({isError: true, onRetry});

        await userEvent.click(screen.getByRole('button', {name: 'Retry'}));

        expect(onRetry).toHaveBeenCalled();
    });
});
