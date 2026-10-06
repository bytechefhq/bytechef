import {McpServer} from '@/shared/middleware/graphql';

interface McpServerToolCountsProps {
    mcpServer: McpServer;
    workflowToolCount: number;
}

const McpServerToolCounts = ({mcpServer, workflowToolCount}: McpServerToolCountsProps) => {
    const componentToolCount = (mcpServer.mcpComponents ?? []).reduce(
        (count, mcpComponent) => count + (mcpComponent?.mcpTools?.length ?? 0),
        0
    );

    return (
        <>
            <span className="mr-1">
                {componentToolCount === 1 ? '1 component tool' : `${componentToolCount} component tools`}
            </span>

            <span className="mx-1">-</span>

            <span className="mr-1">
                {workflowToolCount === 1 ? '1 workflow tool' : `${workflowToolCount} workflow tools`}
            </span>
        </>
    );
};

export default McpServerToolCounts;
