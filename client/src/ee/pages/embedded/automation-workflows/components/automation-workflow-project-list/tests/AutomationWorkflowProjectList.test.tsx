import {TooltipProvider} from '@/components/ui/tooltip';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectList from '../AutomationWorkflowProjectList';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

vi.mock('@/shared/hooks/useHasEmbeddedScope', () => ({
    UNION_ENVIRONMENT_ID: -1,
    useEmbeddedScopeState: () => ({editionUnknown: false, error: false, granted: true, loading: false}),
    useHasEmbeddedScope: () => true,
}));

vi.mock('@/shared/queries/platform/environments.queries', () => ({
    useGetEnvironmentsQuery: () => ({data: [{id: 1, name: 'Development'}]}),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectConfigurationsQuery: () => ({
        data: {automationWorkflowProjectConfigurations: []},
    }),
    useAutomationWorkflowProjectVersionsQuery: () => ({
        data: {
            automationWorkflowProjectVersions: [
                {description: null, publishedDate: null, status: 'PUBLISHED', version: 1},
            ],
        },
    }),
}));

const project: AutomationWorkflowProjectType = {
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

const renderList = (projectOverrides: Partial<AutomationWorkflowProjectType> = {}) =>
    render(
        <TooltipProvider>
            <AutomationWorkflowProjectList
                newlyCreatedProjectId="project-1"
                onCreateWorkflow={vi.fn()}
                onDeleteProject={vi.fn()}
                onDeleteWorkflow={vi.fn()}
                onEditProject={vi.fn()}
                onEditWorkflow={vi.fn()}
                onImportWorkflow={vi.fn()}
                onPublishProject={vi.fn()}
                onSelectWorkflow={vi.fn()}
                onUpdateTags={vi.fn()}
                projects={[{...project, ...projectOverrides}]}
                tags={[]}
            />
        </TooltipProvider>
    );

describe('AutomationWorkflowProjectList', () => {
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
        expect(screen.queryByRole('button', {name: 'More Workflow Creation Actions'})).not.toBeInTheDocument();

        await user.click(screen.getByRole('tab', {name: 'Configurations'}));

        expect(await screen.findByText('No Configurations')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Create Configuration'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New Configuration'})).not.toBeInTheDocument();
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

        expect(await screen.findByRole('button', {name: 'New Configuration'})).toBeInTheDocument();
    });

    it('hides configuration creation until the project is published', async () => {
        const user = userEvent.setup();

        renderList({configurationCount: 0, published: false});

        await user.click(await screen.findByRole('tab', {name: 'Configurations'}));

        expect(await screen.findByText('Publish the project to configure it')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Configuration'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New Configuration'})).not.toBeInTheDocument();
    });

    it('opens the configuration dialog from the empty state and closes it on cancel', async () => {
        const user = userEvent.setup();

        renderList();

        await user.click(await screen.findByRole('tab', {name: 'Configurations'}));
        await user.click(await screen.findByRole('button', {name: 'Create Configuration'}));

        expect(await screen.findByRole('dialog')).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Cancel'}));

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });
});
