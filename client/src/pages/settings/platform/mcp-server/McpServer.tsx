import McpServerConfiguration from '@/shared/components/mcp-server/McpServerConfiguration';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useManagementMcpServerUrlQuery, useUpdateManagementMcpServerUrlMutation} from '@/shared/middleware/graphql';
import {useEffect, useState} from 'react';

const McpServer = () => {
    const [mcpServerUrl, setMcpServerUrl] = useState<string | undefined>(undefined);

    const {data} = useManagementMcpServerUrlQuery();

    useEffect(() => {
        if (data?.managementMcpServerUrl) {
            setMcpServerUrl(data.managementMcpServerUrl);
        }
    }, [data?.managementMcpServerUrl]);

    const updateManagementMcpServerUrlMutation = useUpdateManagementMcpServerUrlMutation({
        onSuccess: (data) => {
            setMcpServerUrl(data.updateManagementMcpServerUrl);
        },
    });

    const handleRefresh = () => {
        updateManagementMcpServerUrlMutation.mutate({});
    };

    return (
        <LayoutContainer header={<Header centerTitle position="main" title="MCP Server" />} leftSidebarOpen={false}>
            <div className="max-w-screen-lg p-4 3xl:mx-auto 3xl:w-4/5">
                {mcpServerUrl && <McpServerConfiguration mcpServerUrl={mcpServerUrl} onRefresh={handleRefresh} />}
            </div>
        </LayoutContainer>
    );
};

export default McpServer;
