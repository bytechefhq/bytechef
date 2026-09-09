import IntegrationList from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationList';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/ee/pages/embedded/integrations/components/integration-list/IntegrationListItem', async () => {
    const {CollapsibleTrigger} = await import('@/components/ui/collapsible');

    return {
        default: ({integration}: {integration: {name: string}}) => (
            <CollapsibleTrigger>{integration.name} header</CollapsibleTrigger>
        ),
    };
});

vi.mock('@/ee/pages/embedded/integrations/components/integration-workflow-list/IntegrationWorkflowList', () => ({
    default: ({integration}: {integration: {name: string}}) => <div>{integration.name} workflows</div>,
}));

const integrations = [
    {componentName: 'accelo', id: 1, multipleInstances: false, name: 'Accelo', tags: []},
    {componentName: 'asana', id: 2, multipleInstances: false, name: 'Asana', tags: []},
];

const renderList = (newlyCreatedIntegrationId?: number) =>
    render(
        <IntegrationList integrations={integrations} newlyCreatedIntegrationId={newlyCreatedIntegrationId} tags={[]} />
    );

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('IntegrationList', () => {
    it('keeps every integration collapsed by default', () => {
        renderList();

        expect(screen.getByText('Accelo header')).toBeInTheDocument();
        expect(screen.queryByText('Accelo workflows')).not.toBeInTheDocument();
    });

    it('expands the newly created integration and leaves the others closed', () => {
        renderList(2);

        expect(screen.getByText('Asana workflows')).toBeInTheDocument();
        expect(screen.queryByText('Accelo workflows')).not.toBeInTheDocument();
    });

    it('expands and collapses an integration on its own', async () => {
        renderList();

        await userEvent.click(screen.getByText('Accelo header'));

        expect(screen.getByText('Accelo workflows')).toBeInTheDocument();
        expect(screen.queryByText('Asana workflows')).not.toBeInTheDocument();

        await userEvent.click(screen.getByText('Accelo header'));

        expect(screen.queryByText('Accelo workflows')).not.toBeInTheDocument();
    });

    it('lets the newly created integration be collapsed again', async () => {
        renderList(2);

        await userEvent.click(screen.getByText('Asana header'));

        expect(screen.queryByText('Asana workflows')).not.toBeInTheDocument();
    });
});
