import Badge from '@/components/Badge/Badge';
import PageLoader from '@/components/PageLoader';
import AppInstanceActions from '@/pages/automation/apps/components/AppInstanceActions';
import {APP_INSTANCE_STATUS_LABELS, APP_STATUS_LABELS, HOSTING_TARGET_LABELS} from '@/pages/automation/apps/constants';
import {useHostedAppInstancesQuery} from '@/shared/middleware/graphql';
import {Link} from 'react-router-dom';

interface HostedAppInstancesProps {
    canManage: boolean;
    environmentId: string;
    workspaceId: string;
}

const HostedAppInstances = ({canManage, environmentId, workspaceId}: HostedAppInstancesProps) => {
    const {data, error, isLoading} = useHostedAppInstancesQuery({environmentId, workspaceId});

    const hostedAppInstances = data?.hostedAppInstances ?? [];

    if (!isLoading && !error && hostedAppInstances.length === 0) {
        return null;
    }

    return (
        <section aria-label="Hosted for other workspaces" className="w-full space-y-2 px-4 py-6 3xl:mx-auto 3xl:w-4/5">
            <h2 className="text-base font-semibold">Hosted for other workspaces</h2>

            <p className="text-sm text-content-neutral-secondary">
                Instances this workspace runs of apps owned by another workspace. The app itself is managed there.
            </p>

            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                <ul className="divide-y rounded-md border">
                    {hostedAppInstances.map((hostedAppInstance) => (
                        <li
                            className="flex items-center justify-between gap-4 px-3 py-2"
                            data-testid={`hosted-app-instance-row-${hostedAppInstance.id}`}
                            key={hostedAppInstance.id}
                        >
                            <div className="flex flex-col gap-1">
                                <div className="flex items-center gap-2">
                                    <Link
                                        className="font-medium hover:underline"
                                        to={`/automation/apps/instances/${hostedAppInstance.id}`}
                                    >
                                        {hostedAppInstance.name}
                                    </Link>

                                    <Badge
                                        label={APP_INSTANCE_STATUS_LABELS[hostedAppInstance.status]}
                                        styleType="secondary-filled"
                                        weight="semibold"
                                    />

                                    <Badge
                                        label={HOSTING_TARGET_LABELS[hostedAppInstance.hostingTarget]}
                                        styleType="outline-outline"
                                    />
                                </div>

                                <div className="flex items-center gap-3 text-xs text-content-neutral-secondary">
                                    <span>{`App: ${hostedAppInstance.app.name}`}</span>

                                    <span>{APP_STATUS_LABELS[hostedAppInstance.app.status]}</span>

                                    {hostedAppInstance.app.description && (
                                        <span>{hostedAppInstance.app.description}</span>
                                    )}

                                    {hostedAppInstance.url && <span>{hostedAppInstance.url}</span>}

                                    {hostedAppInstance.secretKeyHint && (
                                        <span>{`Key …${hostedAppInstance.secretKeyHint}`}</span>
                                    )}
                                </div>
                            </div>

                            {canManage && (
                                <AppInstanceActions appId={hostedAppInstance.app.id} appInstance={hostedAppInstance} />
                            )}
                        </li>
                    ))}
                </ul>
            </PageLoader>
        </section>
    );
};

export default HostedAppInstances;
