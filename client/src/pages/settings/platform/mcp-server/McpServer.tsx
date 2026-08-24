import Switch from '@/components/Switch/Switch';
import McpServerConfiguration from '@/shared/components/mcp-server/McpServerConfiguration';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    useManagementMcpServerAuthenticationRequiredQuery,
    useManagementMcpServerUrlQuery,
    useUpdateManagementMcpServerAuthenticationRequiredMutation,
    useUpdateManagementMcpServerUrlMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useState} from 'react';

const McpServer = () => {
    const [mcpServerUrl, setMcpServerUrl] = useState<string | undefined>(undefined);

    const {data} = useManagementMcpServerUrlQuery();
    const {data: authenticationRequiredData} = useManagementMcpServerAuthenticationRequiredQuery();

    const queryClient = useQueryClient();

    const updateManagementMcpServerUrlMutation = useUpdateManagementMcpServerUrlMutation({
        onSuccess: (data) => {
            setMcpServerUrl(data.updateManagementMcpServerUrl);
        },
    });

    const updateManagementMcpServerAuthenticationRequiredMutation =
        useUpdateManagementMcpServerAuthenticationRequiredMutation({
            onSuccess: () => {
                queryClient.invalidateQueries({queryKey: ['managementMcpServerAuthenticationRequired']});
            },
        });

    const handleRefresh = () => {
        updateManagementMcpServerUrlMutation.mutate({});
    };

    const handleAuthenticationRequiredChange = (authenticationRequired: boolean) => {
        updateManagementMcpServerAuthenticationRequiredMutation.mutate({authenticationRequired});
    };

    useEffect(() => {
        if (data?.managementMcpServerUrl) {
            setMcpServerUrl(data.managementMcpServerUrl);
        }
    }, [data?.managementMcpServerUrl]);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle
                    description="Connect AI assistants to ByteChef over the Model Context Protocol."
                    position="main"
                    title="MCP Server"
                />
            }
            leftSidebarOpen={false}
        >
            <div className="max-w-(--breakpoint-lg) p-4 3xl:mx-auto 3xl:w-4/5">
                {authenticationRequiredData !== undefined && (
                    <div className="mb-4">
                        <Switch
                            checked={authenticationRequiredData.managementMcpServerAuthenticationRequired}
                            label="Require authentication"
                            onCheckedChange={handleAuthenticationRequiredChange}
                        />
                    </div>
                )}

                {mcpServerUrl && <McpServerConfiguration mcpServerUrl={mcpServerUrl} onRefresh={handleRefresh} />}
            </div>
        </LayoutContainer>
    );
};

export default McpServer;
