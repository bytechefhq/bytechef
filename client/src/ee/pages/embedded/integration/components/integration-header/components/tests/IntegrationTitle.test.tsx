import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationTitle from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationTitle';
import {Integration, IntegrationStatus} from '@/ee/shared/middleware/embedded/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {expect, it} from 'vitest';

const integration = {
    componentName: 'slack',
    id: 1,
    lastIntegrationVersion: 5,
    lastStatus: IntegrationStatus.Draft,
    name: 'Slack',
} as Integration;

const renderIntegrationTitle = (overrides: Partial<Integration>) =>
    render(
        <TooltipProvider>
            <IntegrationTitle integration={{...integration, ...overrides} as Integration} />
        </TooltipProvider>
    );

it('shows the draft version of an integration that was never published', () => {
    renderIntegrationTitle({});

    expect(screen.getByText('V5')).toBeInTheDocument();
    expect(screen.getByText('DRAFT')).toBeInTheDocument();
});

it('shows the last published version when the draft has no unpublished changes', () => {
    renderIntegrationTitle({lastPublishedDate: new Date(), unpublishedChanges: false} as Partial<Integration>);

    expect(screen.getByText('V4')).toBeInTheDocument();
    expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
    expect(screen.queryByText('DRAFT')).not.toBeInTheDocument();
});

it('shows the draft version when the draft has unpublished changes', () => {
    renderIntegrationTitle({lastPublishedDate: new Date(), unpublishedChanges: true} as Partial<Integration>);

    expect(screen.getByText('V5')).toBeInTheDocument();
    expect(screen.getByText('DRAFT')).toBeInTheDocument();
    expect(screen.queryByText('PUBLISHED')).not.toBeInTheDocument();
});
