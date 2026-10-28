import IntegrationList from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationList';
import {IntegrationListItemTabType} from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationListItemTabs';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {MemoryRouter, useLocation} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/ee/pages/embedded/integrations/components/integration-list/IntegrationListItem', () => ({
    default: ({
        activeTab,
        configurationsContent,
        integration,
        onTabChange,
        workflowsContent,
    }: {
        activeTab?: IntegrationListItemTabType;
        configurationsContent: React.ReactNode;
        integration: Integration;
        onTabChange: (tab?: IntegrationListItemTabType) => void;
        workflowsContent: React.ReactNode;
    }) => (
        <div>
            <span>{`${integration.name} tab ${activeTab ?? 'none'}`}</span>

            <button onClick={() => onTabChange('configurations')}>{`Open ${integration.name} configurations`}</button>

            <button
                onClick={() => onTabChange(activeTab ? undefined : 'workflows')}
            >{`Toggle ${integration.name}`}</button>

            {activeTab === 'configurations' && configurationsContent}

            {activeTab === 'workflows' && workflowsContent}
        </div>
    ),
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationsPanel',
    () => ({
        default: ({integration}: {integration: Integration}) => <div>{`${integration.name} configurations panel`}</div>,
    })
);

vi.mock('@/ee/pages/embedded/integrations/components/integration-workflow-list/IntegrationWorkflowList', () => ({
    default: ({integration}: {integration: Integration}) => <div>{`${integration.name} workflow list`}</div>,
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-workflow-executions-sheet/IntegrationInstanceConfigurationWorkflowExecutionsSheet',
    () => ({
        default: () => <div>Workflow executions sheet</div>,
    })
);

const integrations = [
    {componentName: 'accelo', id: 1, multipleInstances: false, name: 'Accelo', tags: []},
    {componentName: 'asana', id: 2, multipleInstances: false, name: 'Asana', tags: []},
] as Integration[];

const CurrentSearch = () => {
    const location = useLocation();

    return <div>{`search: ${location.search}`}</div>;
};

const renderList = (initialEntry = '/embedded/integrations', newlyCreatedIntegrationId?: number) =>
    render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <IntegrationList
                integrations={integrations}
                newlyCreatedIntegrationId={newlyCreatedIntegrationId}
                tags={[]}
            />

            <CurrentSearch />
        </MemoryRouter>
    );

describe('IntegrationList', () => {
    beforeEach(() => {
        Element.prototype.scrollIntoView = vi.fn();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('starts with every row collapsed and no tab selected', () => {
        renderList();

        expect(screen.getByText('Accelo tab none')).toBeInTheDocument();
        expect(screen.getByText('Asana tab none')).toBeInTheDocument();
        expect(screen.queryByText(/workflow list/)).not.toBeInTheDocument();
        expect(screen.queryByText(/configurations panel/)).not.toBeInTheDocument();
    });

    it('opens the newly created integration on the workflows tab and leaves the others closed', () => {
        renderList('/embedded/integrations', 2);

        expect(screen.getByText('Asana tab workflows')).toBeInTheDocument();
        expect(screen.getByText('Asana workflow list')).toBeInTheDocument();
        expect(screen.getByText('Accelo tab none')).toBeInTheDocument();
    });

    it('expands and collapses an integration on its own', () => {
        renderList();

        fireEvent.click(screen.getByText('Toggle Accelo'));

        expect(screen.getByText('Accelo workflow list')).toBeInTheDocument();
        expect(screen.getByText('Asana tab none')).toBeInTheDocument();

        fireEvent.click(screen.getByText('Toggle Accelo'));

        expect(screen.getByText('Accelo tab none')).toBeInTheDocument();
    });

    it('shows the configurations panel and the workflow executions sheet once on the configurations tab', () => {
        renderList();

        expect(screen.queryByText('Workflow executions sheet')).not.toBeInTheDocument();

        fireEvent.click(screen.getByText('Open Accelo configurations'));
        fireEvent.click(screen.getByText('Open Asana configurations'));

        expect(screen.getByText('Accelo configurations panel')).toBeInTheDocument();
        expect(screen.getByText('Asana configurations panel')).toBeInTheDocument();
        expect(screen.getAllByText('Workflow executions sheet')).toHaveLength(1);
    });

    it('opens the configurations tab of the integration named in the URL, scrolls to it and drops the one-off params', () => {
        const scrolledElements: Element[] = [];

        Element.prototype.scrollIntoView = vi.fn(function (this: Element) {
            scrolledElements.push(this);
        });

        renderList('/embedded/integrations?categoryId=4&integrationId=2&tab=configurations');

        expect(screen.getByText('Asana tab configurations')).toBeInTheDocument();
        expect(screen.getByText('Asana configurations panel')).toBeInTheDocument();
        expect(screen.getByText('Accelo tab none')).toBeInTheDocument();
        expect(scrolledElements).toHaveLength(1);
        expect(scrolledElements[0]).toHaveTextContent('Asana tab configurations');
        expect(screen.getByText('search: ?categoryId=4')).toBeInTheDocument();
    });
});
