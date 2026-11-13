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
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
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

const joinTitles = (titles: string[]): string =>
    titles.length > 1 ? `${titles.slice(0, -1).join(', ')} and ${titles[titles.length - 1]}` : (titles[0] ?? '');

const describeMcpServer = (
    {coveredComponentNames, missingComponentNames}: ProvisionableMcpServerI,
    getComponentTitle: (componentName: string) => string
): string => {
    const covered = joinTitles(coveredComponentNames.map(getComponentTitle));
    const missing = joinTitles(missingComponentNames.map(getComponentTitle));
    const missingIntegrations = missingComponentNames.length > 1 ? 'integrations' : 'integration';

    if (coveredComponentNames.length === 0) {
        return `Unavailable: its tools use ${missing}, and this user has no ${missing} ${missingIntegrations} yet. Add ${missingComponentNames.length > 1 ? 'them' : 'it'} with Provision Integration on the Integrations tab, then come back here.`;
    }

    if (missingComponentNames.length === 0) {
        return `Uses ${covered}`;
    }

    return `Uses ${covered}. Its ${missing} tools stay hidden until this user has the ${missing} ${missingIntegrations}; add ${missingComponentNames.length > 1 ? 'them' : 'it'} with Provision Integration on the Integrations tab.`;
};

const ProvisionMcpServerDialog = ({connectedUserId, onClose}: ProvisionMcpServerDialogProps) => {
    const [mcpServerId, setMcpServerId] = useState('');

    const queryClient = useQueryClient();

    const {data} = useProvisionableConnectedUserMcpServersQuery({connectedUserId: connectedUserId.toString()});

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const provisionConnectedUserMcpServerMutation = useProvisionConnectedUserMcpServerMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ConnectedUserKeys.connectedUser(connectedUserId)});
            void queryClient.invalidateQueries({queryKey: ['connectedUserMcpServers']});
            void queryClient.invalidateQueries({queryKey: ['provisionableConnectedUserMcpServers']});

            onClose();
        },
    });

    const provisionableMcpServers = data?.provisionableConnectedUserMcpServers;

    const unprovisionedMcpServers = useMemo(
        () => (provisionableMcpServers ?? []).filter((mcpServer) => !mcpServer.provisioned),
        [provisionableMcpServers]
    );

    const componentTitles = useMemo(
        () =>
            new Map(
                (componentDefinitions ?? []).map((componentDefinition) => [
                    componentDefinition.name,
                    componentDefinition.title || componentDefinition.name,
                ])
            ),
        [componentDefinitions]
    );

    const getComponentTitle = (componentName: string) => componentTitles.get(componentName) || componentName;

    const selectedMcpServer = unprovisionedMcpServers.find((mcpServer) => mcpServer.id === mcpServerId);

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
                    {provisionableMcpServers && unprovisionedMcpServers.length === 0 ? (
                        <p className="text-sm text-content-neutral-secondary">
                            {provisionableMcpServers.length === 0
                                ? 'No MCP servers are defined.'
                                : 'Every MCP server is already provisioned for this user.'}
                        </p>
                    ) : (
                        <fieldset className="border-0 p-0">
                            <RadioGroup name="mcpServerId" onValueChange={setMcpServerId} value={mcpServerId}>
                                {unprovisionedMcpServers.map((mcpServer) => (
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
                                            <span className="text-sm font-medium">{mcpServer.name}</span>

                                            <span className="text-sm font-normal text-content-neutral-secondary">
                                                {describeMcpServer(mcpServer, getComponentTitle)}
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
