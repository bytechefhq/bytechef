import {TooltipProvider} from '@/components/ui/tooltip';
import {AutomationProjectsQuery} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationProjectList from '../AutomationProjectList';

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];

vi.mock('@/shared/queries/platform/environments.queries', () => ({
    useGetEnvironmentsQuery: () => ({data: [{id: 1, name: 'Development'}]}),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationProjectConfigurationsQuery: () => ({
        data: {automationProjectConfigurations: []},
    }),
    useAutomationProjectVersionsQuery: () => ({
        data: {
            automationProjectVersions: [{description: null, publishedDate: null, status: 'PUBLISHED', version: 1}],
        },
    }),
}));

const project: AutomationProjectType = {
    categoryId: null,
    configurationCount: 0,
    description: null,
    id: 'project-1',
    lastPublishedDate: null,
    lastPublishedVersion: 1,
    name: 'Alpha Project',
    permissionExpression: null,
    published: true,
    tagIds: [],
    version: 2,
    workflowTemplates: [],
};

const renderList = (projectOverrides: Partial<AutomationProjectType> = {}) =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <AutomationProjectList
                    newlyCreatedProjectId="project-1"
                    onCreateWorkflow={vi.fn()}
                    onDeleteWorkflow={vi.fn()}
                    onEditProject={vi.fn()}
                    onEditWorkflow={vi.fn()}
                    onImportWorkflow={vi.fn()}
                    onSelectWorkflow={vi.fn()}
                    onUpdateTags={vi.fn()}
                    projects={[{...project, ...projectOverrides}]}
                    tags={[]}
                />
            </TooltipProvider>
        </MemoryRouter>
    );

describe('AutomationProjectList', () => {
    beforeEach(() => {
        windowResizeObserver();

        Element.prototype.hasPointerCapture = vi.fn();
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.scrollIntoView = vi.fn();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('offers creation only from the empty states while the project has no workflows or configurations', async () => {
        const user = userEvent.setup();

        renderList();

        expect(await screen.findByText('No Workflows')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Create Workflow'})).toHaveTextContent('Create Workflow');
        expect(screen.getAllByRole('group', {name: 'Workflow Creation Actions'})).toHaveLength(1);

        await user.click(screen.getByRole('tab', {name: 'Configurations'}));

        expect(await screen.findByRole('heading', {name: 'No configurations in Development'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Configure'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Configuration'})).not.toBeInTheDocument();
    });

    it('shows the tab actions once the project has workflows and configurations', async () => {
        const user = userEvent.setup();

        renderList({
            configurationCount: 1,
            workflowTemplates: [
                {
                    components: [],
                    description: null,
                    label: 'Greeting',
                    lastModifiedDate: null,
                    permissionExpression: null,
                    triggers: [],
                    workflowId: 'workflow-1',
                    workflowUuid: 'workflow-uuid-1',
                },
            ],
        });

        expect(await screen.findByRole('button', {name: 'Create Workflow'})).toHaveTextContent('New Workflow');

        await user.click(screen.getByRole('tab', {name: 'Configurations'}));

        expect(await screen.findByRole('button', {name: 'Create Configuration'})).toHaveTextContent(
            'New Configuration'
        );
    });

    it('hides configuration creation until the project is published', async () => {
        const user = userEvent.setup();

        renderList({configurationCount: 0, published: false});

        await user.click(await screen.findByRole('tab', {name: 'Configurations'}));

        expect(await screen.findByText('Publish the project to configure it')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Configure'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Configuration'})).not.toBeInTheDocument();
    });

    it('opens the configuration dialog from the empty state and closes it on cancel', async () => {
        const user = userEvent.setup();

        renderList();

        await user.click(await screen.findByRole('tab', {name: 'Configurations'}));
        await user.click(await screen.findByRole('button', {name: 'Configure'}));

        expect(await screen.findByRole('dialog')).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Cancel'}));

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });
});
