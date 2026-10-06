import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {ConnectedUserMcpServerTool, useEnableConnectedUserMcpToolMutation} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';

const ConnectedUserMcpServerListItemToolRow = ({tool}: {tool: ConnectedUserMcpServerTool}) => {
    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: tool.componentName,
        componentVersion: tool.componentVersion,
    });

    const toolDefinition = componentDefinition?.clusterElements?.find(
        (clusterElement) => clusterElement.type === 'TOOLS' && clusterElement.name === tool.name
    );

    const queryClient = useQueryClient();

    const enableConnectedUserMcpToolMutation = useEnableConnectedUserMcpToolMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserMcpServers']});
        },
    });

    return (
        <li className="flex items-center gap-2 py-0.5">
            <div className="flex min-w-0 flex-1 flex-col">
                <span className="truncate text-sm font-medium">{toolDefinition?.title || tool.name}</span>

                {toolDefinition?.description && (
                    <span className="truncate text-xs text-muted-foreground">{toolDefinition.description}</span>
                )}
            </div>

            <div className="relative mr-11 flex shrink-0 items-center">
                {enableConnectedUserMcpToolMutation.isPending && (
                    <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                )}

                <Switch
                    checked={tool.enabled}
                    onCheckedChange={(value) => {
                        enableConnectedUserMcpToolMutation.mutate({enable: value, id: tool.id});
                    }}
                />
            </div>
        </li>
    );
};

export default ConnectedUserMcpServerListItemToolRow;
