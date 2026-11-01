import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import McpConnectClientDetail from './McpConnectClientDetail';
import {getMcpConnectAuthMode} from './mcpConnectAuthMode';
import {MCP_CONNECT_CLIENTS} from './mcpConnectClients';

const DEFAULT_API_KEY_SOURCE = 'an API key created under Settings → API Keys';

interface McpConnectPanelPropsI {
    apiKeySource?: string;
    authenticationRequired?: boolean;
    mcpServerUrl: string;
    onRefresh?: () => void;
}

const McpConnectPanel = ({
    apiKeySource = DEFAULT_API_KEY_SOURCE,
    authenticationRequired = true,
    mcpServerUrl,
    onRefresh,
}: McpConnectPanelPropsI) => {
    const [selectedClientId, setSelectedClientId] = useState(MCP_CONNECT_CLIENTS[0].id);

    const authorizationServerEnabled = useApplicationInfoStore((state) => state.oauth2.authorizationServer.enabled);

    const mode = getMcpConnectAuthMode({authenticationRequired, oauthAvailable: authorizationServerEnabled});

    const selectedClient = useMemo(
        () => MCP_CONNECT_CLIENTS.find((client) => client.id === selectedClientId) ?? MCP_CONNECT_CLIENTS[0],
        [selectedClientId]
    );

    return (
        <div className="space-y-6">
            <div className="flex flex-col gap-6 md:flex-row md:gap-8">
                <div className="md:hidden">
                    <Select onValueChange={setSelectedClientId} value={selectedClient.id}>
                        <SelectTrigger aria-label="Client">
                            <SelectValue />
                        </SelectTrigger>

                        <SelectContent>
                            {MCP_CONNECT_CLIENTS.map((client) => (
                                <SelectItem key={client.id} value={client.id}>
                                    {client.title}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>

                <fieldset className="hidden w-56 shrink-0 flex-col gap-1 border-0 p-0 md:flex">
                    <legend className="mb-2 text-sm font-medium text-muted-foreground">Pick your client</legend>

                    {MCP_CONNECT_CLIENTS.map((client) => {
                        const Logo = client.logo;

                        const selected = client.id === selectedClient.id;

                        return (
                            <button
                                aria-pressed={selected}
                                className={twMerge(
                                    'flex items-center gap-3 rounded-md border border-transparent px-3 py-2 text-left text-sm text-foreground hover:bg-muted',
                                    selected && 'border-border bg-muted font-medium'
                                )}
                                key={client.id}
                                onClick={() => setSelectedClientId(client.id)}
                                type="button"
                            >
                                <Logo className="size-4 shrink-0" />

                                {client.title}
                            </button>
                        );
                    })}
                </fieldset>

                <div className="min-w-0 flex-1">
                    <McpConnectClientDetail
                        apiKeySource={apiKeySource}
                        client={selectedClient}
                        mcpServerUrl={mcpServerUrl}
                        mode={mode}
                        onRefresh={onRefresh}
                    />
                </div>
            </div>
        </div>
    );
};

export default McpConnectPanel;
