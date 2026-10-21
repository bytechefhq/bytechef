import Switch from '@/components/Switch/Switch';
import {APP_STATUS_LABELS} from '@/pages/automation/apps/constants';
import {App, AppStatus, useUpdateAppStatusMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface AppStatusSwitchProps {
    app: Pick<App, 'id' | 'name' | 'status'>;
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
        <div className="flex items-center gap-2">
            <span className="text-xs text-content-neutral-secondary">{APP_STATUS_LABELS[app.status]}</span>

            <Switch
                aria-label={`${app.name} active`}
                checked={app.status === AppStatus.Active}
                disabled={!canManage || app.status === AppStatus.Archived}
                onCheckedChange={(checked) =>
                    updateAppStatusMutation.mutate({
                        id: app.id,
                        status: checked ? AppStatus.Active : AppStatus.Suspended,
                    })
                }
            />
        </div>
    );
};

export default AppStatusSwitch;
