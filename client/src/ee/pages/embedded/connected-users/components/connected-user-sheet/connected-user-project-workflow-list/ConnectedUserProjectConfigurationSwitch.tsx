import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useEnableConnectedUserAutomationProjectConfigurationMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface ConnectedUserProjectConfigurationSwitchProps {
    configuration: {configurationEnabled: boolean; configurationName: string; enabled: boolean; projectId: string};
    connectedUserId: string;
}

const ConnectedUserProjectConfigurationSwitch = ({
    configuration,
    connectedUserId,
}: ConnectedUserProjectConfigurationSwitchProps) => {
    const queryClient = useQueryClient();

    const enableConnectedUserAutomationProjectConfigurationMutation =
        useEnableConnectedUserAutomationProjectConfigurationMutation({
            onSuccess: () => {
                void queryClient.invalidateQueries({queryKey: ['connectedUserProjects']});
            },
        });

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <span className="relative flex items-center">
                    {enableConnectedUserAutomationProjectConfigurationMutation.isPending && (
                        <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                    )}

                    <Switch
                        aria-label={`Enable ${configuration.configurationName}`}
                        checked={configuration.configurationEnabled && configuration.enabled}
                        disabled={!configuration.configurationEnabled}
                        onCheckedChange={(value) =>
                            enableConnectedUserAutomationProjectConfigurationMutation.mutate({
                                connectedUserId,
                                enable: value,
                                projectId: configuration.projectId,
                            })
                        }
                    />
                </span>
            </TooltipTrigger>

            <TooltipContent side="left">
                {configuration.configurationEnabled
                    ? 'Turns the workflows of this configuration on or off for this user'
                    : 'An admin disabled this configuration, so its workflows do not run for this user'}
            </TooltipContent>
        </Tooltip>
    );
};

export default ConnectedUserProjectConfigurationSwitch;
