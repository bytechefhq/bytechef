import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import {Project} from '@/shared/middleware/automation/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectsStepButton from './ProjectsStepButton';

const hoisted = vi.hoisted(() => ({navigate: vi.fn()}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');

    return {...actual, useNavigate: () => hoisted.navigate};
});

const draftProjectWithWorkflow: Project = {id: 2, name: 'Invoice digest', projectWorkflowIds: [21, 22], workspaceId: 1};

beforeEach(() => {
    vi.clearAllMocks();

    environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
});

describe('ProjectsStepButton', () => {
    it('opens the one project that can be published in the workflow editor', async () => {
        const handleClick = vi.fn();

        render(<ProjectsStepButton nextProject={draftProjectWithWorkflow} onClick={handleClick} />);

        await userEvent.click(screen.getByRole('button', {name: 'Open Invoice digest'}));

        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/projects/2/project-workflows/21');
        expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it('goes to the project list when there is no single project to open', async () => {
        render(<ProjectsStepButton />);

        await userEvent.click(screen.getByRole('button', {name: 'Go to Projects'}));

        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/projects');
        expect(environmentStore.getState().currentEnvironmentId).toBe(DEVELOPMENT_ENVIRONMENT);
    });

    // The project list only exists in Development; its route redirects every other environment to Deployments.
    it('switches to Development before going to the project list from another environment', async () => {
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});

        render(<ProjectsStepButton />);

        await userEvent.click(screen.getByRole('button', {name: 'Go to Projects in Development'}));

        expect(environmentStore.getState().currentEnvironmentId).toBe(DEVELOPMENT_ENVIRONMENT);
        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/projects');
    });
});
