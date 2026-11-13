import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationComponentElementList from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationComponentElementList';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    copyToClipboard: vi.fn(),
}));

vi.mock('@uidotdev/usehooks', () => ({
    useCopyToClipboard: () => [null, hoisted.copyToClipboard],
}));

const elements = [
    {description: 'Sends an email', id: 'GMAIL_SEND_EMAIL', title: 'Send Email'},
    {description: undefined, id: 'GMAIL_LIST_LABELS', title: 'List Labels'},
];

const renderList = (overrides: Partial<Parameters<typeof IntegrationComponentElementList>[0]> = {}) =>
    render(
        <TooltipProvider>
            <IntegrationComponentElementList
                elements={elements}
                idLabel="Tool ID"
                isError={false}
                isLoading={false}
                noun="tools"
                onRetry={vi.fn()}
                {...overrides}
            />
        </TooltipProvider>
    );

describe('IntegrationComponentElementList', () => {
    it('shows the description and id of an expanded element', async () => {
        renderList();

        await userEvent.click(screen.getByRole('button', {name: 'Send Email'}));

        expect(screen.getByText('Sends an email')).toBeInTheDocument();
        expect(screen.getByText('Tool ID')).toBeInTheDocument();
        expect(screen.getByText('GMAIL_SEND_EMAIL')).toBeInTheDocument();
    });

    it('copies the id of an element to the clipboard', async () => {
        renderList();

        await userEvent.click(screen.getByRole('button', {name: 'Send Email'}));
        await userEvent.click(screen.getByRole('button', {name: 'Copy Tool ID'}));

        expect(hoisted.copyToClipboard).toHaveBeenCalledWith('GMAIL_SEND_EMAIL');
    });

    it('lists every element', () => {
        renderList();

        expect(screen.getByRole('button', {name: 'List Labels'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Send Email'})).toBeInTheDocument();
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    });

    it('shows an empty state when the component has no elements', () => {
        renderList({elements: []});

        expect(screen.getByText('No tools')).toBeInTheDocument();
    });

    it('shows a loading indicator while the elements load', () => {
        renderList({isLoading: true});

        expect(screen.getByText('Loading tools...')).toBeInTheDocument();
    });

    it('offers a retry when loading failed', async () => {
        const onRetry = vi.fn();

        renderList({isError: true, onRetry});

        await userEvent.click(screen.getByRole('button', {name: 'Retry'}));

        expect(onRetry).toHaveBeenCalled();
    });
});
