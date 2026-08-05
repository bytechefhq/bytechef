/**
 * Edition seam for the project git-configuration feature (EE-only). CE surfaces (Projects, ProjectListItem, the
 * project header settings menu) consume this registry instead of importing the EE query/mutation hooks statically —
 * the client-side equivalent of the server's optional-bean SPI pattern.
 *
 * The default implementation is a no-op: queries report nothing to show and mutations do nothing, which is exactly
 * the CE behavior (the git menu items are EEVersion-gated anyway, so the mutations are unreachable). The EE bundle
 * replaces it via {@link registerProjectGitApi} from main.tsx's edition bootstrap, which runs after the application
 * info (and therefore the edition) is known but before the first render — so hook identity never changes between
 * renders and the rules of hooks hold.
 */

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
