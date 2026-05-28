import LoadingIcon from '@/components/LoadingIcon';
import {Switch} from '@/components/ui/switch';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ConnectedUserMcpServerTool, useEnableConnectedUserMcpToolMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import InlineSVG from 'react-inlinesvg';

const ConnectedUserMcpServerListItemToolRow = ({tool}: {tool: ConnectedUserMcpServerTool}) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const componentDefinition = componentDefinitions?.find((definition) => definition.name === tool.componentName);

    const queryClient = useQueryClient();

    const enableConnectedUserMcpToolMutation = useEnableConnectedUserMcpToolMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['connectedUserMcpServers']});
        },
    });

    return (
        <li className="flex items-center justify-between rounded-md p-2 py-1 hover:bg-gray-50">
            <div className="flex items-center gap-x-2 text-sm font-semibold">
                {componentDefinition?.icon && (
                    <div className="flex items-center justify-center rounded-full border p-1">
                        <InlineSVG className="size-5 flex-none" src={componentDefinition.icon} />
                    </div>
                )}

                <span>{tool.name}</span>
            </div>

            <div className="relative mr-11 flex items-center">
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
