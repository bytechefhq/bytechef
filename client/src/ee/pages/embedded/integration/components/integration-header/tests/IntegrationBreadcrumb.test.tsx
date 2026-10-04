import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationBreadcrumb from '@/ee/pages/embedded/integration/components/integration-header/components/IntegrationBreadcrumb';
import {IntegrationStatus} from '@/ee/shared/middleware/embedded/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {expect, it} from 'vitest';

const mockIntegration = {
    componentName: 'gmail',
    id: 1,
    lastIntegrationVersion: 3,
    lastStatus: IntegrationStatus.Draft,
    multipleInstances: false,
    name: 'Gmail',
};

it('shows the integration title with its version and status', () => {
    render(
        <TooltipProvider>
            <IntegrationBreadcrumb integration={mockIntegration} />
        </TooltipProvider>
    );

    expect(screen.getByRole('heading', {name: 'Gmail'})).toBeInTheDocument();
    expect(screen.getByText('V3')).toBeInTheDocument();
    expect(screen.getByText(IntegrationStatus.Draft)).toBeInTheDocument();
});

it('shows the item switcher after a separator when one is given', () => {
    render(
        <TooltipProvider>
            <IntegrationBreadcrumb integration={mockIntegration} itemSelect={<button>workflow2</button>} />
        </TooltipProvider>
    );

    expect(screen.getByRole('button', {name: 'workflow2'})).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
});

it('omits the separator and switcher without an item switcher', () => {
    render(
        <TooltipProvider>
            <IntegrationBreadcrumb integration={mockIntegration} />
        </TooltipProvider>
    );

    expect(screen.getAllByRole('listitem')).toHaveLength(1);
});
