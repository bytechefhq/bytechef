import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogHeader, DialogMain} from '@/components/Dialog';
import {Input} from '@/components/ui/input';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAddAiHubUserConnectorMutation, useAiHubChatToolableComponentsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {Loader2Icon, PlugIcon, SearchIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

interface AiHubAddConnectorDialogProps {
    addedComponentNames: Set<string>;
    onClose: () => void;
}

const AiHubAddConnectorDialog = ({addedComponentNames, onClose}: AiHubAddConnectorDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [searchTerm, setSearchTerm] = useState('');

    const queryClient = useQueryClient();

    const {data, isLoading} = useAiHubChatToolableComponentsQuery(
        {workspaceId: String(currentWorkspaceId ?? '')},
        {enabled: currentWorkspaceId != null}
    );

    const addMutation = useAddAiHubUserConnectorMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['aiHubUserConnectors']}),
    });

    const components = useMemo(() => {
        const normalizedSearch = searchTerm.trim().toLowerCase();

        return [...(data?.aiHubChatToolableComponents ?? [])]
            .filter((component) => !addedComponentNames.has(component.componentName))
            .filter((component) => {
                if (!normalizedSearch) {
                    return true;
                }

                const label = (component.title ?? component.componentName).toLowerCase();

                return (
                    label.includes(normalizedSearch) || component.componentName.toLowerCase().includes(normalizedSearch)
                );
            })
            .sort((first, second) =>
                (first.title ?? first.componentName).localeCompare(second.title ?? second.componentName)
            );
    }, [addedComponentNames, data, searchTerm]);

    const handleAdd = (componentName: string, componentVersion: number) => {
        addMutation.mutate({
            componentName,
            componentVersion,
            connectionId: null,
            environment: currentEnvironmentId,
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
                    <DialogHeader description="Add a pre-built connector to your AI Hub." title="Add Connector" />

                    <DialogBody>
                        <div className="relative">
                            <SearchIcon className="absolute top-1/2 left-2 size-4 -translate-y-1/2 text-muted-foreground" />

                            <Input
                                className="pl-8"
                                onChange={(event) => setSearchTerm(event.target.value)}
                                placeholder="Search connectors..."
                                value={searchTerm}
                            />
                        </div>

                        <div className="flex max-h-96 flex-col gap-1 overflow-y-auto">
                            {isLoading && (
                                <div className="flex justify-center py-6">
                                    <Loader2Icon className="size-5 animate-spin text-muted-foreground" />
                                </div>
                            )}

                            {!isLoading && components.length === 0 && (
                                <div className="py-6 text-center text-sm text-muted-foreground">
                                    No connectors to add.
                                </div>
                            )}

                            {components.map((component) => (
                                <div
                                    className="flex items-center gap-2.5 rounded-md px-2 py-1.5"
                                    key={component.componentName}
                                >
                                    {component.icon ? (
                                        <InlineSVG className="size-5 shrink-0" src={component.icon} />
                                    ) : (
                                        <PlugIcon className="size-5 shrink-0 text-muted-foreground" />
                                    )}

                                    <div className="flex min-w-0 flex-1 flex-col">
                                        <span className="truncate text-sm font-medium">
                                            {component.title ?? component.componentName}
                                        </span>

                                        {component.description && (
                                            <span className="truncate text-xs text-muted-foreground">
                                                {component.description}
                                            </span>
                                        )}
                                    </div>

                                    <Button
                                        label="Add"
                                        onClick={() => handleAdd(component.componentName, component.componentVersion)}
                                        size="sm"
                                        variant="outline"
                                    />
                                </div>
                            ))}
                        </div>
                    </DialogBody>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AiHubAddConnectorDialog;
