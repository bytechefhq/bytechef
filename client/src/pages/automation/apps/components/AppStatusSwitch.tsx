import Switch from '@/components/Switch/Switch';
import {App, AppStatus, useUpdateAppStatusMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface AppStatusSwitchProps {
    app: App;
    canManage: boolean;
}

const AppStatusSwitch = ({app, canManage}: AppStatusSwitchProps) => {
    const queryClient = useQueryClient();

    const updateAppStatusMutation = useUpdateAppStatusMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['apps']});
            queryClient.invalidateQueries({queryKey: ['app']});
        },
    });

    return (
        <Switch
            aria-label={`${app.name} enabled`}
            checked={app.status === AppStatus.Enabled}
            disabled={!canManage}
            onCheckedChange={(checked) =>
                updateAppStatusMutation.mutate({
                    id: app.id,
                    status: checked ? AppStatus.Enabled : AppStatus.Disabled,
                })
            }
        />
    );
};

export default AppStatusSwitch;
