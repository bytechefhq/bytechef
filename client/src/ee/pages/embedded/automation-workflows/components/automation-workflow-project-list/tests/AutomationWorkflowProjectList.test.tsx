import {TooltipProvider} from '@/components/ui/tooltip';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectList from '../AutomationWorkflowProjectList';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

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

const renderList = () =>
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
                projects={[project]}
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

    it('shows the workflow actions on the Workflows tab and the configuration list on the Configurations tab', async () => {
        const user = userEvent.setup();

        renderList();

        expect(await screen.findByRole('button', {name: 'Create Workflow'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New Configuration'})).not.toBeInTheDocument();

        await user.click(screen.getByRole('tab', {name: 'Configurations'}));

        expect(await screen.findByText('No Configurations')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'New Configuration'})).toBeEnabled();
        expect(screen.queryByRole('button', {name: 'Create Workflow'})).not.toBeInTheDocument();
    });

    it('opens the configuration dialog from the New Configuration button and closes it on cancel', async () => {
        const user = userEvent.setup();

        renderList();

        await user.click(await screen.findByRole('tab', {name: 'Configurations'}));
        await user.click(await screen.findByRole('button', {name: 'New Configuration'}));

        expect(await screen.findByText('Create Configuration')).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Cancel'}));

        await waitFor(() => expect(screen.queryByText('Create Configuration')).not.toBeInTheDocument());
    });
});
