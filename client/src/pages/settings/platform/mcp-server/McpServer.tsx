import McpServerConfiguration from '@/shared/components/mcp-server/McpServerConfiguration';
import McpServerConfigurationCode from '@/shared/components/mcp-server/McpServerConfigurationCode';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    useManagementMcpServerCanonicalUrlQuery,
    useManagementMcpServerUrlQuery,
    useUpdateManagementMcpServerUrlMutation,
} from '@/shared/middleware/graphql';
import {useEffect, useState} from 'react';

const McpServer = () => {
    const [mcpServerUrl, setMcpServerUrl] = useState<string | undefined>(undefined);

    const {data: canonicalUrlData} = useManagementMcpServerCanonicalUrlQuery();
    const {data} = useManagementMcpServerUrlQuery();

    const updateManagementMcpServerUrlMutation = useUpdateManagementMcpServerUrlMutation({
        onSuccess: (data) => {
            setMcpServerUrl(data.updateManagementMcpServerUrl);
        },
    });

    const handleRefresh = () => {
        updateManagementMcpServerUrlMutation.mutate({});
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
            <div className="max-w-(--breakpoint-lg) self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
                {canonicalUrlData?.managementMcpServerCanonicalUrl && (
                    <div className="space-y-4">
                        <h2 className="font-semibold text-foreground">MCP server URL</h2>

                        <McpServerConfiguration
                            mcpServerUrl={canonicalUrlData.managementMcpServerCanonicalUrl}
                            signInWithByteChefAvailable
                        />
                    </div>
                )}

                {mcpServerUrl && (
                    <fieldset className="mt-8 space-y-4 border-0 p-0">
                        <legend className="font-semibold text-foreground">URL for external identity providers</legend>

                        <p className="text-sm text-muted-foreground">
                            Use this URL when your MCP client signs in through your own identity provider. Regenerating
                            it invalidates the old URL.
                        </p>

                        <McpServerConfigurationCode codeSnippet={mcpServerUrl} onRefresh={handleRefresh} />
                    </fieldset>
                )}
            </div>
        </LayoutContainer>
    );
};

export default McpServer;
