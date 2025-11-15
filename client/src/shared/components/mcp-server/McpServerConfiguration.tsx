import {Alert, AlertTitle} from '@/components/ui/alert';
import {Badge} from '@/components/ui/badge';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import McpServerConfigurationCode from '@/shared/components/mcp-server/McpServerConfigurationCode';
import {InfoCircledIcon} from '@radix-ui/react-icons';

const McpServerConfiguration = ({mcpServerUrl, onRefresh}: {mcpServerUrl: string; onRefresh: () => void}) => {
    const codeSnippet1 = `{
  mcpServers: {
    "ByteChef": {
      "command": "npx",
      "args": [
        "-y",
        "supergateway",
        "--streamableHttp",
        "${mcpServerUrl}"
      ]
    }
  }
}`;

    const codeSnippet2 = `{
  mcpServers: {
    ByteChef: {
      url: ${mcpServerUrl}
    }
  }
}`;

    const codeSnippet3 = mcpServerUrl;

    return (
        <Tabs defaultValue="claude">
            <TabsList>
                <TabsTrigger value="claude">Claude</TabsTrigger>

                <TabsTrigger value="cursor">Cursor</TabsTrigger>

                <TabsTrigger value="windsurf">Windsurf</TabsTrigger>

                <TabsTrigger value="other">Other</TabsTrigger>
            </TabsList>

            <TabsContent value="claude">
                <div className="mt-6 space-y-8">
                    <div className="space-y-4">
                        <div className="flex items-start gap-4">
                            <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-sm font-semibold text-muted-foreground">
                                1
                            </div>

                            <div className="flex-1 space-y-4">
                                <h2 className="font-semibold text-foreground">Add MCP Server</h2>

                                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                    <span>Go to</span>

                                    <a
                                        className="text-content-brand-primary hover:underline"
                                        href="https://claude.ai/settings/integrations"
                                        rel="noreferrer"
                                        target="_blank"
                                    >
                                        claude.ai/settings/integrations
                                    </a>

                                    <span>→</span>

                                    <Badge variant="outline">Add More</Badge>
                                </div>

                                <Alert className="flex items-center gap-2" variant="default">
                                    <span>
                                        <InfoCircledIcon />
                                    </span>

                                    <AlertTitle className="mb-0 text-muted-foreground">
                                        This feature is only available with a Claude Pro subscription. Alternatively,
                                        you can use the Claude desktop application to connect to the MCP server.
                                    </AlertTitle>
                                </Alert>

                                {codeSnippet3 && (
                                    <McpServerConfigurationCode codeSnippet={codeSnippet3} onRefresh={onRefresh} />
                                )}
                            </div>
                        </div>
                    </div>

                    <div className="space-y-4">
                        <div className="flex items-start gap-4">
                            <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-sm font-semibold text-muted-foreground">
                                2
                            </div>

                            <div className="flex-1 space-y-4">
                                <h2 className="font-semibold text-foreground">Configure Claude Integration</h2>

                                <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
                                    <span>Open Claude</span>

                                    <span>→</span>

                                    <Badge variant="outline">Settings</Badge>

                                    <span>→</span>

                                    <Badge variant="outline">Developer</Badge>

                                    <span>→</span>

                                    <Badge variant="outline">Edit Config</Badge>

                                    <span>→</span>

                                    <Badge variant="outline">Open claude_desktop_config.json</Badge>
                                </div>

                                <p className="text-sm text-muted-foreground">
                                    Paste the configuration below and save, then quit and restart Claude.
                                </p>

                                <McpServerConfigurationCode codeSnippet={codeSnippet1} onRefresh={onRefresh} />
                            </div>
                        </div>
                    </div>
                </div>
            </TabsContent>

            <TabsContent value="cursor">
                <div className="mt-6 space-y-8">
                    <div className="space-y-4">
                        <div className="flex items-start gap-4">
                            <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-sm font-semibold text-muted-foreground">
                                1
                            </div>

                            <div className="flex-1 space-y-4">
                                <h2 className="font-semibold text-foreground">Configure Cursor</h2>

                                <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
                                    <span>Open Cursor</span>

                                    <span>→</span>

                                    <Badge variant="outline">Settings</Badge>

                                    <span>→</span>

                                    <Badge variant="outline">Cursor Settings</Badge>

                                    <span>→</span>

                                    <Badge variant="outline">MCP</Badge>

                                    <span>→</span>

                                    <Badge variant="outline">Add new global MCP server</Badge>
                                </div>

                                <p className="text-sm text-muted-foreground">Paste the configuration below and save.</p>

                                <McpServerConfigurationCode codeSnippet={codeSnippet2} onRefresh={onRefresh} />
                            </div>
                        </div>
                    </div>
                </div>
            </TabsContent>

            <TabsContent value="windsurf">
                <div className="mt-6 space-y-8">
                    <div className="space-y-4">
                        <div className="flex items-start gap-4">
                            <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-sm font-semibold text-muted-foreground">
                                1
                            </div>

                            <div className="flex-1 space-y-4">
                                <h2 className="font-semibold text-foreground">Configure Windsurf</h2>

                                <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
                                    <span>Open Windsurf</span>

                                    <span>→</span>

                                    <Badge variant="outline">Settings</Badge>

                                    <span>→</span>

                                    <Badge variant="outline">Advanced</Badge>

                                    <span>→</span>

                                    <Badge variant="outline">Cascade</Badge>

                                    <span>→</span>

                                    <Badge variant="outline">Add Server</Badge>

                                    <span>→</span>

                                    <Badge variant="outline">Add custom server</Badge>
                                </div>

                                <p className="text-sm text-muted-foreground">Paste the configuration below and save.</p>

                                <McpServerConfigurationCode codeSnippet={codeSnippet2} onRefresh={onRefresh} />
                            </div>
                        </div>
                    </div>
                </div>
            </TabsContent>

            <TabsContent value="other">
                <div className="mt-6 space-y-8">
                    <div className="space-y-4">
                        <div className="flex items-start gap-4">
                            <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-sm font-semibold text-muted-foreground">
                                1
                            </div>

                            <div className="flex-1 space-y-4">
                                <h2 className="font-semibold text-foreground">Server URL</h2>

                                {codeSnippet3 && (
                                    <McpServerConfigurationCode codeSnippet={codeSnippet3} onRefresh={onRefresh} />
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </TabsContent>
        </Tabs>
    );
};

export default McpServerConfiguration;
