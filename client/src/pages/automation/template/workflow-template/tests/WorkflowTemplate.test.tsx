import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowTemplate from '../WorkflowTemplate';

const hoisted = vi.hoisted(() => ({
    captureProjectWorkflowCreatedFromTemplate: vi.fn(),
    mutate: vi.fn(),
    navigate: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');

    return {
        ...actual,
        useNavigate: () => hoisted.navigate,
        useParams: () => ({id: 'workflow-uuid', projectId: '3'}),
    };
});

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({
        captureProjectWorkflowCreatedFromTemplate: hoisted.captureProjectWorkflowCreatedFromTemplate,
    }),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useImportWorkflowTemplateMutation: (options: {onSuccess: (data: {importWorkflowTemplate: string}) => void}) => ({
        mutate: hoisted.mutate.mockImplementation(() => options.onSuccess({importWorkflowTemplate: 'workflow-9'})),
    }),
    useWorkflowTemplateQuery: () => ({
        data: {workflowTemplate: {components: [], description: 'Template description', workflow: null}},
    }),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetProjectQuery: () => ({data: {id: 3, name: 'Target Project'}}),
}));

vi.mock('@/pages/automation/projects/components/ProjectDialog', () => ({default: () => null}));

vi.mock('@/pages/automation/template/workflow-template/components/ProjectsComboBox', () => ({default: () => null}));

const renderWorkflowTemplate = () =>
    render(
        <MemoryRouter>
            <WorkflowTemplate fromInternalFlow />
        </MemoryRouter>
    );

describe('WorkflowTemplate', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should import the template into the selected project', async () => {
        renderWorkflowTemplate();

        await userEvent.click(screen.getByRole('button', {name: 'Import now'}));

        expect(hoisted.mutate).toHaveBeenCalledWith({
            projectId: '3',
            sharedWorkflow: false,
            workflowUuid: 'workflow-uuid',
        });
    });

    it('should capture analytics and open the imported workflow on successful import', async () => {
        renderWorkflowTemplate();

        await userEvent.click(screen.getByRole('button', {name: 'Import now'}));

        expect(hoisted.captureProjectWorkflowCreatedFromTemplate).toHaveBeenCalledTimes(1);
        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/projects/3/project-workflows/workflow-9');
    });
});
