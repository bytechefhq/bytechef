import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {
    useProvisionConnectedUserMcpServerMutation,
    useProvisionableConnectedUserMcpServersQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

interface ProvisionMcpServerDialogProps {
    connectedUserId: number;
    onClose: () => void;
}

interface ProvisionableMcpServerI {
    coveredComponentNames: string[];
    id: string;
    missingComponentNames: string[];
    name: string;
    provisioned: boolean;
}

const describeMcpServer = ({coveredComponentNames, missingComponentNames}: ProvisionableMcpServerI): string => {
    const covered = coveredComponentNames.join(', ');
    const missing = missingComponentNames.join(', ');

    if (coveredComponentNames.length === 0) {
        return `Add ${missing} on the Integrations tab first.`;
    }

    if (missingComponentNames.length === 0) {
        return `Uses ${covered}`;
    }

    return `Uses ${covered}. Missing ${missing}: add those integrations first to expose them.`;
};

const ProvisionMcpServerDialog = ({connectedUserId, onClose}: ProvisionMcpServerDialogProps) => {
    const [mcpServerId, setMcpServerId] = useState('');

    const queryClient = useQueryClient();

    const {data} = useProvisionableConnectedUserMcpServersQuery({connectedUserId: connectedUserId.toString()});

    const provisionConnectedUserMcpServerMutation = useProvisionConnectedUserMcpServerMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ConnectedUserKeys.connectedUser(connectedUserId)});
            void queryClient.invalidateQueries({queryKey: ['connectedUserMcpServers']});
            void queryClient.invalidateQueries({queryKey: ['provisionableConnectedUserMcpServers']});

            onClose();
        },
    });

    const provisionableMcpServers = data?.provisionableConnectedUserMcpServers;

    const orderedMcpServers = useMemo(
        () => [
            ...(provisionableMcpServers ?? []).filter((mcpServer) => !mcpServer.provisioned),
            ...(provisionableMcpServers ?? []).filter((mcpServer) => mcpServer.provisioned),
        ],
        [provisionableMcpServers]
    );

    const selectedMcpServer = orderedMcpServers.find((mcpServer) => mcpServer.id === mcpServerId);

    const canSave = !!selectedMcpServer && selectedMcpServer.coveredComponentNames.length > 0;

    const handleSave = () => {
        provisionConnectedUserMcpServerMutation.mutate({
            connectedUserId: connectedUserId.toString(),
            mcpServerId,
        });
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-xl" showCloseButton>
                <DialogHeader>
                    <DialogTitle>New MCP Server</DialogTitle>

                    <DialogDescription>
                        Expose an MCP server through the integrations this connected user already has.
                    </DialogDescription>
                </DialogHeader>

                <div className="flex flex-col gap-4">
                    {provisionableMcpServers?.length === 0 ? (
                        <p className="text-sm text-content-neutral-secondary">No MCP servers are defined.</p>
                    ) : (
                        <fieldset className="border-0 p-0">
                            <RadioGroup name="mcpServerId" onValueChange={setMcpServerId} value={mcpServerId}>
                                {orderedMcpServers.map((mcpServer) => (
                                    <div
                                        className="flex items-start gap-3 rounded border border-border/50 px-3 py-2"
                                        key={mcpServer.id}
                                    >
                                        <RadioGroupItem
                                            className="mt-1"
                                            disabled={mcpServer.coveredComponentNames.length === 0}
                                            id={`provision-mcp-server-${mcpServer.id}`}
                                            value={mcpServer.id}
                                        />

                                        <Label
                                            className="flex flex-1 flex-col items-start gap-1"
                                            htmlFor={`provision-mcp-server-${mcpServer.id}`}
                                        >
                                            <span className="flex items-center gap-2 text-sm font-medium">
                                                {mcpServer.name}

                                                {mcpServer.provisioned && (
                                                    <Badge label="Provisioned" styleType="success-filled" />
                                                )}
                                            </span>

                                            <span className="text-sm font-normal text-content-neutral-secondary">
                                                {describeMcpServer(mcpServer)}
                                            </span>
                                        </Label>
                                    </div>
                                ))}
                            </RadioGroup>
                        </fieldset>
                    )}
                </div>

                <DialogFooter>
                    {provisionableMcpServers?.length === 0 ? (
                        <Button label="Close" onClick={onClose} variant="outline" />
                    ) : (
                        <>
                            <Button label="Cancel" onClick={onClose} variant="outline" />

                            <Button
                                disabled={!canSave || provisionConnectedUserMcpServerMutation.isPending}
                                label="Save"
                                onClick={handleSave}
                            />
                        </>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default ProvisionMcpServerDialog;
