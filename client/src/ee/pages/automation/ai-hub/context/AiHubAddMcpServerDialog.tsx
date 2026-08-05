import Button from '@/components/Button/Button';
import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAddAiHubMcpServerMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface AiHubAddMcpServerDialogProps {
    onClose: () => void;
}

/**
 * Add a custom MCP server: a name, the server URL, and an optional bearer token (stored encrypted server-side).
 * Persists via addAiHubMcpServer and invalidates the servers query so the Custom MCP list refreshes. Tool
 * discovery + agent bridging land in a follow-up increment.
 */
const AiHubAddMcpServerDialog = ({onClose}: AiHubAddMcpServerDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [name, setName] = useState('');
    const [url, setUrl] = useState('');
    const [authToken, setAuthToken] = useState('');

    const queryClient = useQueryClient();

    const addMutation = useAddAiHubMcpServerMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiHubMcpServers']});

            onClose();
        },
    });

    const trimmedName = name.trim();
    const trimmedUrl = url.trim();
    const canSubmit = trimmedName.length > 0 && trimmedUrl.length > 0 && !addMutation.isPending;

    const handleSubmit = () => {
        if (!canSubmit) {
            return;
        }

        addMutation.mutate({
            authToken: authToken.trim() || null,
            environment: currentEnvironmentId,
            name: trimmedName,
            url: trimmedUrl,
            workspaceId: String(currentWorkspaceId ?? ''),
        });
    };

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent className="max-w-lg" showCloseButton>
                <DialogHeader>
                    <DialogTitle>Add MCP server</DialogTitle>

                    <DialogDescription>
                        Connect an external Model Context Protocol server by URL. Its tools become available to the
                        agent from chat.
                    </DialogDescription>
                </DialogHeader>

                <div className="flex flex-col gap-4">
                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="mcp-server-name">Name</Label>

                        <Input
                            id="mcp-server-name"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="My MCP server"
                            value={name}
                        />
                    </div>

                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="mcp-server-url">Server URL</Label>

                        <Input
                            id="mcp-server-url"
                            onChange={(event) => setUrl(event.target.value)}
                            placeholder="https://example.com/mcp"
                            value={url}
                        />
                    </div>

                    <div className="flex flex-col gap-1.5">
                        <Label htmlFor="mcp-server-token">Bearer token (optional)</Label>

                        <Input
                            id="mcp-server-token"
                            onChange={(event) => setAuthToken(event.target.value)}
                            placeholder="Leave blank for servers that need no auth"
                            type="password"
                            value={authToken}
                        />
                    </div>
                </div>

                <div className="flex justify-end gap-2">
                    <Button label="Cancel" onClick={onClose} type="button" variant="outline" />

                    <Button disabled={!canSubmit} label="Add server" onClick={handleSubmit} type="button" />
                </div>
            </DialogContent>
        </Dialog>
    );
};

export default AiHubAddMcpServerDialog;
