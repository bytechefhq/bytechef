import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import AppInstanceActions from '@/pages/automation/apps/components/AppInstanceActions';
import AppInstanceDialog from '@/pages/automation/apps/components/AppInstanceDialog';
import {
    APP_ENVIRONMENT_IDS,
    APP_INSTANCE_STATUS_LABELS,
    HOSTING_TARGET_LABELS,
} from '@/pages/automation/apps/constants';
import ResourceVisibilityBadge from '@/shared/components/visibility/ResourceVisibilityBadge';
import {ResourceVisibilityValueType} from '@/shared/components/visibility/ResourceVisibilityPicker';
import {toEnvironmentName} from '@/shared/constants';
import {App, AppInstance, ForeignAppInstance} from '@/shared/middleware/graphql';
import {useGetUserWorkspacesQuery} from '@/shared/queries/automation/workspaces.queries';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useMemo} from 'react';
import {Link} from 'react-router-dom';

interface AppInstancesTabProps {
    app: Pick<App, 'foreignInstances' | 'id' | 'instances'>;
    canManageApp: boolean;
    canManageInstance: (appInstance: AppInstance) => boolean;
}

const AppInstancesTab = ({app, canManageApp, canManageInstance}: AppInstancesTabProps) => {
    const account = useAuthenticationStore((state) => state.account);

    const {data: userWorkspaces} = useGetUserWorkspacesQuery(
        account?.id ?? 0,
        account?.id != null && app.foreignInstances.length > 0
    );

    const workspaceNamesById = useMemo(
        () => new Map((userWorkspaces ?? []).map((workspace) => [String(workspace.id), workspace.name])),
        [userWorkspaces]
    );

    const environmentGroups = useMemo(
        () =>
            APP_ENVIRONMENT_IDS.map((environmentId) => ({
                appInstances: app.instances.filter(
                    (appInstance) => Number(appInstance.environmentId) === environmentId
                ),
                environmentId,
                foreignAppInstances: app.foreignInstances.filter(
                    (foreignAppInstance) => Number(foreignAppInstance.environmentId) === environmentId
                ),
            })).filter(
                (environmentGroup) =>
                    environmentGroup.appInstances.length > 0 || environmentGroup.foreignAppInstances.length > 0
            ),
        [app.foreignInstances, app.instances]
    );

    const getWorkspaceName = (foreignAppInstance: ForeignAppInstance) =>
        workspaceNamesById.get(foreignAppInstance.workspaceId) || `Workspace ${foreignAppInstance.workspaceId}`;

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <p className="text-sm text-content-neutral-secondary">
                    Each instance has its own environment, URL, key and bindings.
                </p>

                {canManageApp && <AppInstanceDialog appId={app.id} triggerNode={<Button label="New Instance" />} />}
            </div>

            {environmentGroups.length === 0 && (
                <p className="py-8 text-center text-sm text-content-neutral-secondary">
                    This app has no instances you can view.
                </p>
            )}

            {environmentGroups.map((environmentGroup) => {
                const environmentName = toEnvironmentName(environmentGroup.environmentId);

                return (
                    <section aria-label={environmentName} className="space-y-2" key={environmentGroup.environmentId}>
                        <h3 className="text-sm font-semibold">{environmentName}</h3>

                        <ul className="divide-y rounded-md border">
                            {environmentGroup.appInstances.map((appInstance) => (
                                <li
                                    className="flex items-center justify-between gap-4 px-3 py-2"
                                    data-testid={`app-instance-row-${appInstance.id}`}
                                    key={appInstance.id}
                                >
                                    <div className="flex flex-col gap-1">
                                        <div className="flex items-center gap-2">
                                            <Link
                                                className="font-medium hover:underline"
                                                to={`/automation/apps/instances/${appInstance.id}`}
                                            >
                                                {appInstance.name}
                                            </Link>

                                            <Badge
                                                label={APP_INSTANCE_STATUS_LABELS[appInstance.status]}
                                                styleType="secondary-filled"
                                                weight="semibold"
                                            />

                                            <Badge
                                                label={HOSTING_TARGET_LABELS[appInstance.hostingTarget]}
                                                styleType="outline-outline"
                                            />

                                            <ResourceVisibilityBadge
                                                visibility={
                                                    (appInstance.visibility ||
                                                        'WORKSPACE') as ResourceVisibilityValueType
                                                }
                                            />
                                        </div>

                                        <div className="flex items-center gap-3 text-xs text-content-neutral-secondary">
                                            {appInstance.url && <span>{appInstance.url}</span>}

                                            {appInstance.secretKeyHint && (
                                                <span>{`Key …${appInstance.secretKeyHint}`}</span>
                                            )}
                                        </div>
                                    </div>

                                    {canManageInstance(appInstance) && (
                                        <AppInstanceActions appId={app.id} appInstance={appInstance} />
                                    )}
                                </li>
                            ))}

                            {environmentGroup.foreignAppInstances.map((foreignAppInstance) => (
                                <li
                                    className="flex items-center justify-between gap-4 px-3 py-2"
                                    data-testid={`app-foreign-instance-row-${foreignAppInstance.id}`}
                                    key={foreignAppInstance.id}
                                >
                                    <div className="flex flex-col gap-1">
                                        <div className="flex items-center gap-2">
                                            <span className="font-medium">{foreignAppInstance.name}</span>

                                            <Badge
                                                label={APP_INSTANCE_STATUS_LABELS[foreignAppInstance.status]}
                                                styleType="secondary-filled"
                                                weight="semibold"
                                            />
                                        </div>

                                        <span className="text-xs text-content-neutral-secondary">{`Workspace: ${getWorkspaceName(foreignAppInstance)}`}</span>
                                    </div>

                                    <span className="text-xs text-content-neutral-secondary">
                                        Managed in its own workspace
                                    </span>
                                </li>
                            ))}
                        </ul>
                    </section>
                );
            })}
        </div>
    );
};

export default AppInstancesTab;
