import {useSettingsMenu} from '@/pages/automation/project/components/project-header/components/settings-menu/hooks/useSettingsMenu';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    getProjectGitConfiguration: vi.fn(),
    pullProjectFromGit: vi.fn(),
    updateProjectGitConfiguration: vi.fn(),
}));

vi.mock('@/ee/shared/middleware/automation/configuration', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/ee/shared/middleware/automation/configuration')>()),
    ProjectGitApi: class {
        getProjectGitConfiguration = hoisted.getProjectGitConfiguration;
        pullProjectFromGit = hoisted.pullProjectFromGit;
        updateProjectGitConfiguration = hoisted.updateProjectGitConfiguration;
    },
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => vi.fn(),
    useSearchParams: () => [new URLSearchParams()],
}));

vi.mock('sonner', () => ({toast: vi.fn()}));

await import('@/ee/shared/edition/registerEditionModules');

const project: Project = {id: 7, name: 'Project', projectWorkflowIds: [11], workspaceId: 1};
const workflow: Workflow = {id: 'workflow-1', projectWorkflowId: 11};

describe('useSettingsMenu with the EE project Git API registered', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

        hoisted.getProjectGitConfiguration.mockReset();
        hoisted.getProjectGitConfiguration.mockResolvedValue({branch: 'main', enabled: true});
        hoisted.updateProjectGitConfiguration.mockReset();
        hoisted.updateProjectGitConfiguration.mockResolvedValue(undefined);
    });

    const renderSettingsMenu = () =>
        renderHook(() => useSettingsMenu({project, workflow}), {
            wrapper: ({children}: {children: ReactNode}) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            ),
        });

    it('loads the project Git configuration', async () => {
        const {result} = renderSettingsMenu();

        await waitFor(() => expect(result.current.projectGitConfiguration).toEqual({branch: 'main', enabled: true}));

        expect(hoisted.getProjectGitConfiguration).toHaveBeenCalledWith({id: 7});
    });

    it('saves the configuration, refreshes the cached Git configurations and closes the dialog', async () => {
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');
        const onSuccess = vi.fn();

        const {result} = renderSettingsMenu();

        act(() => {
            result.current.handleUpdateProjectGitConfigurationSubmit({
                onSuccess,
                projectGitConfiguration: {branch: 'develop', enabled: false},
            });
        });

        await waitFor(() => expect(onSuccess).toHaveBeenCalled());

        expect(hoisted.updateProjectGitConfiguration).toHaveBeenCalledWith({
            id: 7,
            projectGitConfiguration: {branch: 'develop', enabled: false},
        });
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['projectGitConfigurations', 7]});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['projectGitConfigurations']});
    });
});
