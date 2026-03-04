import {McpProject, useMcpProjectsByServerIdQuery} from '@/shared/middleware/graphql';

const useMcpProjectList = (mcpServerId: string) => {
    const {data: mcpProjectsData, isLoading} = useMcpProjectsByServerIdQuery({
        mcpServerId,
    });

    const mcpProjects =
        mcpProjectsData?.mcpProjectsByServerId?.filter((project): project is McpProject => project !== null) || [];

    return {isLoading, mcpProjects};
};

export default useMcpProjectList;
