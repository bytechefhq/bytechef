import {McpProjectsByServerIdQuery, useMcpProjectsByServerIdQuery} from '@/shared/middleware/graphql';

export type McpProjectItemType = NonNullable<NonNullable<McpProjectsByServerIdQuery['mcpProjectsByServerId']>[number]>;
export type McpProjectWorkflowItemType = NonNullable<NonNullable<McpProjectItemType['mcpProjectWorkflows']>[number]>;

const useMcpProjectList = (mcpServerId: string) => {
    const {data: mcpProjectsData, isLoading} = useMcpProjectsByServerIdQuery({
        mcpServerId,
    });

    const mcpProjects =
        mcpProjectsData?.mcpProjectsByServerId?.filter(
            (project): project is NonNullable<typeof project> => project !== null
        ) || [];

    return {isLoading, mcpProjects};
};

export default useMcpProjectList;
