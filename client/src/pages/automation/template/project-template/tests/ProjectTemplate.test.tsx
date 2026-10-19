import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectTemplate from '../ProjectTemplate';

const hoisted = vi.hoisted(() => ({
    captureProjectCreatedFromTemplate: vi.fn(),
    captureProjectWorkflowCreatedFromTemplate: vi.fn(),
    mutate: vi.fn(),
    navigate: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');

    return {
        ...actual,
        useNavigate: () => hoisted.navigate,
        useParams: () => ({id: 'template-1'}),
    };
});

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 7}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({
        captureProjectCreatedFromTemplate: hoisted.captureProjectCreatedFromTemplate,
        captureProjectWorkflowCreatedFromTemplate: hoisted.captureProjectWorkflowCreatedFromTemplate,
    }),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useImportProjectTemplateMutation: (options: {onSuccess: () => void}) => ({
        mutate: hoisted.mutate.mockImplementation(() => options.onSuccess()),
    }),
    useProjectTemplateQuery: () => ({
        data: {
            projectTemplate: {
                components: [
                    {key: 'workflow-1', value: []},
                    {key: 'workflow-2', value: []},
                ],
                description: 'Template description',
                project: {name: 'Template Project'},
                workflows: [
                    {id: 'workflow-1', label: 'First Workflow'},
                    {id: 'workflow-2', label: 'Second Workflow'},
                ],
            },
        },
    }),
}));

const renderProjectTemplate = () =>
    render(
        <MemoryRouter>
            <ProjectTemplate fromInternalFlow />
        </MemoryRouter>
    );

describe('ProjectTemplate', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should import the template into the current workspace', async () => {
        renderProjectTemplate();

        await userEvent.click(screen.getByRole('button', {name: 'Import now'}));

        expect(hoisted.mutate).toHaveBeenCalledWith({id: 'template-1', sharedProject: false, workspaceId: '7'});
    });

    it('should capture the project and one event per template workflow on successful import', async () => {
        renderProjectTemplate();

        await userEvent.click(screen.getByRole('button', {name: 'Import now'}));

        expect(hoisted.captureProjectCreatedFromTemplate).toHaveBeenCalledTimes(1);
        expect(hoisted.captureProjectWorkflowCreatedFromTemplate).toHaveBeenCalledTimes(2);
        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/projects');
    });
});
