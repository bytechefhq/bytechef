import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationSelect from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationSelect';
import {mockScrollIntoView, render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ComponentProps} from 'react';
import {beforeEach, expect, it, vi} from 'vitest';

mockScrollIntoView();
windowResizeObserver();

const mockSetSelectedIntegrationId = vi.fn();

const longIntegrationName = 'An integration with a name long enough to need a tooltip';

const mockIntegrations = [
    {componentName: 'gmail', id: 1, multipleInstances: false, name: 'Gmail'},
    {componentName: 'slack', id: 2, multipleInstances: false, name: 'Slack'},
    {componentName: 'jira', id: 3, multipleInstances: false, name: longIntegrationName},
];

const renderIntegrationSelect = (props: Partial<ComponentProps<typeof IntegrationSelect>> = {}) =>
    render(
        <TooltipProvider>
            <IntegrationSelect
                integrationId={1}
                integrations={mockIntegrations}
                selectedIntegrationId={1}
                setSelectedIntegrationId={mockSetSelectedIntegrationId}
                {...props}
            />
        </TooltipProvider>
    );

beforeEach(() => {
    vi.clearAllMocks();
});

it('reads "Current integration" while the current integration is selected', () => {
    renderIntegrationSelect();

    expect(screen.getByRole('combobox', {name: 'Select integration'})).toHaveTextContent('Current integration');
});

it('reads "All integrations" while every integration is selected', () => {
    renderIntegrationSelect({selectedIntegrationId: 0});

    expect(screen.getByRole('combobox', {name: 'Select integration'})).toHaveTextContent('All integrations');
});

it('shows the name of another selected integration', () => {
    renderIntegrationSelect({selectedIntegrationId: 2});

    expect(screen.getByRole('combobox', {name: 'Select integration'})).toHaveTextContent('Slack');
});

it('shows the full name of a long selected integration', () => {
    renderIntegrationSelect({selectedIntegrationId: 3});

    expect(screen.getByRole('combobox', {name: 'Select integration'})).toHaveTextContent(longIntegrationName);
});

it('lists the pinned options and the integrations when opened', async () => {
    renderIntegrationSelect();

    await userEvent.click(screen.getByRole('combobox', {name: 'Select integration'}));

    expect(screen.getByPlaceholderText('Search integrations...')).toBeInTheDocument();
    expect(screen.getByRole('option', {name: 'Current integration'})).toBeInTheDocument();
    expect(screen.getByRole('option', {name: 'All integrations'})).toBeInTheDocument();
    expect(screen.getByRole('option', {name: 'Slack'})).toBeInTheDocument();
});

it('filters the integrations by the typed text', async () => {
    renderIntegrationSelect();

    await userEvent.click(screen.getByRole('combobox', {name: 'Select integration'}));
    await userEvent.type(screen.getByPlaceholderText('Search integrations...'), 'sla');

    expect(screen.getByRole('option', {name: 'Slack'})).toBeInTheDocument();
    expect(screen.queryByRole('option', {name: 'Gmail'})).not.toBeInTheDocument();
});

it('selects an integration by id', async () => {
    renderIntegrationSelect();

    await userEvent.click(screen.getByRole('combobox', {name: 'Select integration'}));
    await userEvent.click(screen.getByRole('option', {name: 'Slack'}));

    expect(mockSetSelectedIntegrationId).toHaveBeenCalledWith(2);
});

it('selects every integration through the All integrations option', async () => {
    renderIntegrationSelect();

    await userEvent.click(screen.getByRole('combobox', {name: 'Select integration'}));
    await userEvent.click(screen.getByRole('option', {name: 'All integrations'}));

    expect(mockSetSelectedIntegrationId).toHaveBeenCalledWith(0);
});
