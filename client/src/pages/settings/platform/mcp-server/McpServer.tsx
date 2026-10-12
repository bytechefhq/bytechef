import McpServerConfiguration from '@/shared/components/mcp-server/McpServerConfiguration';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useManagementMcpServerCanonicalUrlQuery} from '@/shared/middleware/graphql';

const McpServer = () => {
    const {data: canonicalUrlData} = useManagementMcpServerCanonicalUrlQuery();

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
            </div>
        </LayoutContainer>
    );
};

export default McpServer;
