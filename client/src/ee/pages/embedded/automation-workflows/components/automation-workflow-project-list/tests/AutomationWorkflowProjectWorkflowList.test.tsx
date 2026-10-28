import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationWorkflowProjectWorkflowList from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-list/AutomationWorkflowProjectWorkflowList';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

vi.mock(
    '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-list/AutomationWorkflowProjectWorkflowListItem',
    () => ({
        default: ({workflow}: {workflow: {label: string}}) => <li>{workflow.label}</li>,
    })
);

const handlers = {
    onCreateWorkflow: vi.fn(),
    onDeleteWorkflow: vi.fn(),
    onEditWorkflow: vi.fn(),
    onImportWorkflow: vi.fn(),
    onSelectWorkflow: vi.fn(),
};

const project: AutomationWorkflowProjectType = {
    automationHubVisible: true,
    categoryId: null,
    description: null,
    id: 'project-1',
    lastPublishedVersion: null,
    name: 'Alpha Project',
    permissionExpression: null,
    published: false,
    tagIds: [],
    version: 1,
    workflowTemplates: [
        {
            components: [],
            description: null,
            label: 'Sync Contacts',
            lastModifiedDate: null,
            permissionExpression: null,
            triggers: [],
            workflowId: 'draft-wf-1',
            workflowUuid: 'wf-uuid-1',
        },
    ],
};

const renderList = () =>
    render(
        <TooltipProvider>
            <AutomationWorkflowProjectWorkflowList {...handlers} project={project} />
        </TooltipProvider>
    );

describe('AutomationWorkflowProjectWorkflowList', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('workflow creation actions', () => {
        it('should create a workflow when New Workflow is clicked', async () => {
            renderList();

            await userEvent.click(screen.getByRole('button', {name: 'Create Workflow'}));

            expect(handlers.onCreateWorkflow).toHaveBeenCalledWith('project-1');
        });

        it('should render an Import Workflow row', async () => {
            renderList();

            await userEvent.click(screen.getByRole('button', {name: 'More Workflow Creation Actions'}));

            expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
            expect(screen.getByRole('menuitem', {name: 'Import Workflow'})).toHaveClass('text-content-neutral-primary');
        });

        it('should import a workflow when Import Workflow is clicked', async () => {
            renderList();

            await userEvent.click(screen.getByRole('button', {name: 'More Workflow Creation Actions'}));

            await userEvent.click(screen.getByRole('menuitem', {name: 'Import Workflow'}));

            expect(handlers.onImportWorkflow).toHaveBeenCalledWith('project-1');
        });
    });
});
