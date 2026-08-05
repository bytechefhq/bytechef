export interface ProjectGitConfigurationI {
    branch?: string;
    enabled?: boolean;
    projectId?: number;
}

export interface UpdateProjectGitConfigurationInputI {
    branch: string;
    enabled: boolean;
}

interface MutationCallbacksI {
    onSuccess?: () => void;
}

export interface ProjectGitApiI {
    useProjectGitConfigurationQuery: (projectId: number) => {data: ProjectGitConfigurationI | undefined};
    usePullProjectFromGitMutation: (callbacks: MutationCallbacksI) => {
        isPending: boolean;
        mutate: (input: {id: number}) => void;
    };
    useUpdateProjectGitConfigurationMutation: (callbacks: MutationCallbacksI) => {
        mutate: (
            input: {id: number; projectGitConfiguration: UpdateProjectGitConfigurationInputI},
            callbacks?: MutationCallbacksI
        ) => void;
    };
    useWorkspaceProjectGitConfigurationsQuery: (
        workspaceId: number,
        enabled: boolean
    ) => {data: ProjectGitConfigurationI[] | undefined; error: Error | null; isLoading: boolean};
}

const noopProjectGitApi: ProjectGitApiI = {
    useProjectGitConfigurationQuery: () => ({data: undefined}),
    usePullProjectFromGitMutation: () => ({
        isPending: false,
        mutate: () => {},
    }),
    useUpdateProjectGitConfigurationMutation: () => ({
        mutate: () => {},
    }),
    useWorkspaceProjectGitConfigurationsQuery: () => ({data: undefined, error: null, isLoading: false}),
};

let projectGitApi: ProjectGitApiI = noopProjectGitApi;

export function registerProjectGitApi(api: ProjectGitApiI) {
    projectGitApi = api;
}

export function getProjectGitApi(): ProjectGitApiI {
    return projectGitApi;
}
