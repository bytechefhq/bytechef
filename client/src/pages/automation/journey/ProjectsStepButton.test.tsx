import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import {useOnEnvironmentChange} from '@/shared/hooks/useOnEnvironmentChange';
import {Project} from '@/shared/middleware/automation/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {ReactElement} from 'react';
import {RouterProvider, createMemoryRouter, redirect, useNavigate} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectsStepButton from './ProjectsStepButton';

const draftProjectWithWorkflow: Project = {id: 2, name: 'Invoice digest', projectWorkflowIds: [21, 22], workspaceId: 1};

/*
 * Renders the button on a page that, like Executions, rewrites its own URL when the environment changes, next to
 * routes that copy the real Projects route's guard: the project list only exists in Development.
 */
const renderOnExecutionsPage = (button: ReactElement) => {
    const ExecutionsPage = () => {
        const navigate = useNavigate();

        useOnEnvironmentChange(() => navigate('/automation/executions?status='));

        return button;
    };

    const router = createMemoryRouter(
        [
            {element: <ExecutionsPage />, path: '/automation/executions'},
            {element: <p>Deployments page</p>, path: '/automation/deployments'},
            {
                element: <p>Projects page</p>,
                loader: async () =>
                    environmentStore.getState().currentEnvironmentId === DEVELOPMENT_ENVIRONMENT
                        ? null
                        : redirect('/automation/deployments'),
                path: '/automation/projects',
            },
            {
                element: <p>Workflow editor</p>,
                path: '/automation/projects/:projectId/project-workflows/:projectWorkflowId',
            },
        ],
        {initialEntries: ['/automation/executions']}
    );

    render(<RouterProvider router={router} />);

    return router;
};

beforeEach(() => {
    environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
});

describe('ProjectsStepButton', () => {
    it('opens the one project that can be published in the workflow editor', async () => {
        const handleClick = vi.fn();

        const router = renderOnExecutionsPage(
            <ProjectsStepButton nextProject={draftProjectWithWorkflow} onClick={handleClick} />
        );

        await userEvent.click(screen.getByRole('button', {name: 'Open Invoice digest'}));

        expect(await screen.findByText('Workflow editor')).toBeInTheDocument();
        expect(router.state.location.pathname).toBe('/automation/projects/2/project-workflows/21');
        expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it('goes to the project list when there is no single project to open', async () => {
        renderOnExecutionsPage(<ProjectsStepButton />);

        await userEvent.click(screen.getByRole('button', {name: 'Go to Projects'}));

        expect(await screen.findByText('Projects page')).toBeInTheDocument();
        expect(environmentStore.getState().currentEnvironmentId).toBe(DEVELOPMENT_ENVIRONMENT);
    });

    it('switches to Development before going to the project list from another environment', async () => {
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});

        renderOnExecutionsPage(<ProjectsStepButton />);

        await userEvent.click(screen.getByRole('button', {name: 'Go to Projects in Development'}));

        expect(await screen.findByText('Projects page')).toBeInTheDocument();
        expect(environmentStore.getState().currentEnvironmentId).toBe(DEVELOPMENT_ENVIRONMENT);
    });

    // Drafts are edited and test-run in Development only; the editor would otherwise run against this environment.
    it('switches to Development before opening a project from another environment', async () => {
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});

        renderOnExecutionsPage(<ProjectsStepButton nextProject={draftProjectWithWorkflow} />);

        await userEvent.click(screen.getByRole('button', {name: 'Open Invoice digest in Development'}));

        expect(await screen.findByText('Workflow editor')).toBeInTheDocument();
        expect(environmentStore.getState().currentEnvironmentId).toBe(DEVELOPMENT_ENVIRONMENT);
    });
});
