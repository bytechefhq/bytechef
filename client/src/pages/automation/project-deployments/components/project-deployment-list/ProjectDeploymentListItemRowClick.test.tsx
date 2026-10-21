import {Collapsible} from '@/components/ui/collapsible';
import {TooltipProvider} from '@/components/ui/tooltip';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentListItem from './ProjectDeploymentListItem';

const hoisted = vi.hoisted(() => ({
    openProjectMock: vi.fn(),
}));

vi.mock('@/pages/automation/project-deployments/hooks/useOpenInProject', () => ({
    default: () => ({
        openProject: hoisted.openProjectMock,
        openProjectWorkflow: vi.fn(),
    }),
}));

vi.mock('@/shared/mutations/automation/projectDeployments.mutations', () => ({
    useDeleteProjectDeploymentMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableProjectDeploymentMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/projectDeploymentTags.mutations', () => ({
    useUpdateProjectDeploymentTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureProjectDeploymentEnabled: vi.fn()}),
}));

const projectDeployment: ProjectDeployment = {
    enabled: true,
    environmentId: 0,
    id: 42,
    name: 'My Project Deployment',
    projectDeploymentWorkflows: [],
    projectId: 7,
    projectVersion: 1,
    tags: [],
};

const renderItem = () =>
    render(
        <TooltipProvider>
            <Collapsible>
                <ProjectDeploymentListItem projectDeployment={projectDeployment} />
            </Collapsible>
        </TooltipProvider>
    );

// The real menu and the real Collapsible render here: the sibling ProjectDeploymentListItem.test.tsx mocks both, so it
// cannot tell whether a click inside the portaled menu reaches the row. The keyboard opens the menu, because a pointer
// click on the trigger bubbles to the row in jsdom, which has no hit testing and ignores the body pointer-events lock.
const openMenu = async () => {
    renderItem();

    screen
        .getAllByRole('button')
        .find((button) => button.getAttribute('aria-haspopup') === 'menu')!
        .focus();

    await userEvent.keyboard('{ArrowDown}');
};

const getWorkflowsTrigger = () => screen.getByRole('button', {hidden: true, name: /workflows/});

describe('ProjectDeploymentListItem row click', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('should expand the workflows when the row is clicked', async () => {
        renderItem();

        await userEvent.click(screen.getByText('My Project Deployment'));

        await waitFor(() => expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'true'));
    });

    it('should keep the workflows collapsed when a menu row is clicked', async () => {
        await openMenu();

        expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'false');

        await userEvent.click(screen.getByRole('menuitem', {name: 'Open Project'}));

        expect(hoisted.openProjectMock).toHaveBeenCalledWith(7);
        expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'false');
    });

    it('should keep the workflows collapsed when the menu itself is clicked', async () => {
        await openMenu();

        await userEvent.click(screen.getByRole('menu'));

        expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'false');
    });
});
