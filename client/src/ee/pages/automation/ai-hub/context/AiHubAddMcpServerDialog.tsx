import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
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
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Connect an external Model Context Protocol server by URL. Its tools become available to the agent from chat."
                        title="Add MCP server"
                    />

                    <DialogBody>
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
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button disabled={!canSubmit} label="Add server" onClick={handleSubmit} type="button" />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AiHubAddMcpServerDialog;
