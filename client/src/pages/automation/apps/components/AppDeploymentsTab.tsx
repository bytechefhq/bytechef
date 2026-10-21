import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import AppDeployDialog from '@/pages/automation/apps/components/AppDeployDialog';
import {
    APP_DEPLOYMENT_PROVIDER_LABELS,
    APP_DEPLOYMENT_STATUS_LABELS,
    APP_DEPLOYMENT_STATUS_STYLES,
    isAppDeploymentInProgress,
} from '@/pages/automation/apps/constants';
import useAppDeployments, {AppDeploymentItemType} from '@/pages/automation/apps/hooks/useAppDeployments';
import useInvalidateAppDeploymentQueries from '@/pages/automation/apps/hooks/useInvalidateAppDeploymentQueries';
import {
    AppDeploymentStatus,
    useAppInstanceVersionsQuery,
    useCancelAppDeploymentMutation,
    useRollbackAppInstanceMutation,
} from '@/shared/middleware/graphql';
import {RocketIcon, RotateCcwIcon, XCircleIcon} from 'lucide-react';
import {useEffect, useMemo, useRef, useState} from 'react';

const LIMIT_STEPS = [50, 100, 200];

const VERSION_LIMIT = 50;

interface AppDeploymentsTabProps {
    appInstanceId: string;
    canDeploy?: boolean;
    deploymentConnectionId?: string | null;
    deploymentProviderAvailable?: boolean;
}

const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '-');

const isWebUrl = (url: string) => url.startsWith('http://') || url.startsWith('https://');

const AppDeploymentsTab = ({
    appInstanceId,
    canDeploy = false,
    deploymentConnectionId,
    deploymentProviderAvailable = false,
}: AppDeploymentsTabProps) => {
    const [cancelDeployment, setCancelDeployment] = useState<AppDeploymentItemType | undefined>();
    const [limitIndex, setLimitIndex] = useState(0);
    const [rollbackDeployment, setRollbackDeployment] = useState<AppDeploymentItemType | undefined>();
    const [showDeployDialog, setShowDeployDialog] = useState(false);

    const wasInProgressRef = useRef(false);

    const limit = LIMIT_STEPS[limitIndex];

    const {appDeployments, error, isLoading} = useAppDeployments(appInstanceId, limit);

    const {data: appVersionsData, isLoading: appVersionsLoading} = useAppInstanceVersionsQuery(
        {appInstanceId, limit: VERSION_LIMIT},
        {enabled: canDeploy}
    );

    const invalidateAppDeploymentQueries = useInvalidateAppDeploymentQueries();

    const cancelAppDeploymentMutation = useCancelAppDeploymentMutation({
        onSuccess: () => {
            invalidateAppDeploymentQueries();

            setCancelDeployment(undefined);
        },
    });

    const rollbackAppInstanceMutation = useRollbackAppInstanceMutation({
        onSuccess: () => {
            invalidateAppDeploymentQueries();

            setRollbackDeployment(undefined);
        },
    });

    const mayHaveMore = appDeployments.length === limit;
    const atMaximum = limitIndex === LIMIT_STEPS.length - 1;

    const appVersions = useMemo(
        () => appVersionsData?.appInstanceVersions ?? [],
        [appVersionsData?.appInstanceVersions]
    );

    const deploymentInProgress = useMemo(
        () => appDeployments.some((appDeployment) => isAppDeploymentInProgress(appDeployment.status)),
        [appDeployments]
    );

    const deployDisabledReason = useMemo(() => {
        if (!deploymentProviderAvailable) {
            return 'No deployment provider is available for the hosting target of this instance.';
        }

        if (!deploymentConnectionId) {
            return 'Configure a deployment connection in the deployment settings of this instance first.';
        }

        if (deploymentInProgress) {
            return 'A deployment is in progress. Wait for it to finish or cancel it.';
        }

        if (appVersionsLoading) {
            return 'Loading the versions of this app.';
        }

        if (appVersions.length === 0) {
            return 'This app has no versions yet. Create one with "bytechef automation app version create".';
        }

        return undefined;
    }, [
        appVersions.length,
        appVersionsLoading,
        deploymentConnectionId,
        deploymentInProgress,
        deploymentProviderAvailable,
    ]);

    const canRollback = (appDeployment: AppDeploymentItemType) =>
        appDeployment.status === AppDeploymentStatus.Ready && !appDeployment.current;

    useEffect(() => {
        if (wasInProgressRef.current && !deploymentInProgress) {
            invalidateAppDeploymentQueries();
        }

        wasInProgressRef.current = deploymentInProgress;
    }, [deploymentInProgress, invalidateAppDeploymentQueries]);

    return (
        <div className="space-y-4">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <p className="text-sm text-content-neutral-secondary">
                    The deployment history of this instance, newest first.
                </p>

                {canDeploy && (
                    <div className="flex flex-col items-end gap-1">
                        <Button
                            disabled={deployDisabledReason !== undefined}
                            icon={<RocketIcon />}
                            label="Deploy"
                            onClick={() => setShowDeployDialog(true)}
                        />

                        {deployDisabledReason && (
                            <p className="max-w-80 text-right text-xs text-content-neutral-secondary">
                                {deployDisabledReason}
                            </p>
                        )}
                    </div>
                )}
            </div>

            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                {appDeployments.length > 0 ? (
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Version</TableHead>

                                <TableHead>Status</TableHead>

                                <TableHead>Provider</TableHead>

                                <TableHead>Recorded by</TableHead>

                                <TableHead>Created</TableHead>

                                <TableHead>Finished</TableHead>

                                <TableHead>URL</TableHead>

                                {canDeploy && <TableHead className="sr-only">Actions</TableHead>}
                            </TableRow>
                        </TableHeader>

                        <TableBody>
                            {appDeployments.map((appDeployment) => (
                                <TableRow key={appDeployment.id}>
                                    <TableCell className="font-medium">{appDeployment.appVersion.name}</TableCell>

                                    <TableCell>
                                        <div className="flex flex-col gap-1">
                                            <Badge
                                                label={APP_DEPLOYMENT_STATUS_LABELS[appDeployment.status]}
                                                styleType={APP_DEPLOYMENT_STATUS_STYLES[appDeployment.status]}
                                                weight="semibold"
                                            />

                                            {appDeployment.errorMessage && (
                                                <span className="text-xs break-words whitespace-pre-wrap text-content-destructive-primary">
                                                    {appDeployment.errorMessage}
                                                </span>
                                            )}
                                        </div>
                                    </TableCell>

                                    <TableCell>{APP_DEPLOYMENT_PROVIDER_LABELS[appDeployment.provider]}</TableCell>

                                    <TableCell>{appDeployment.createdBy || '-'}</TableCell>

                                    <TableCell>{formatDate(appDeployment.createdDate)}</TableCell>

                                    <TableCell>{formatDate(appDeployment.finishedDate)}</TableCell>

                                    <TableCell>
                                        {appDeployment.url && isWebUrl(appDeployment.url) ? (
                                            <a
                                                className="text-content-brand-primary hover:underline"
                                                href={appDeployment.url}
                                                rel="noopener noreferrer"
                                                target="_blank"
                                            >
                                                {appDeployment.url}
                                            </a>
                                        ) : (
                                            appDeployment.url || '-'
                                        )}
                                    </TableCell>

                                    {canDeploy && (
                                        <TableCell className="text-right">
                                            {canRollback(appDeployment) && (
                                                <Button
                                                    aria-label={`Roll back to ${appDeployment.appVersion.name}`}
                                                    disabled={deploymentInProgress}
                                                    icon={<RotateCcwIcon />}
                                                    label="Roll back"
                                                    onClick={() => setRollbackDeployment(appDeployment)}
                                                    size="sm"
                                                    variant="ghost"
                                                />
                                            )}

                                            {isAppDeploymentInProgress(appDeployment.status) && (
                                                <Button
                                                    aria-label={`Cancel the deployment of ${appDeployment.appVersion.name}`}
                                                    icon={<XCircleIcon />}
                                                    label="Cancel"
                                                    onClick={() => setCancelDeployment(appDeployment)}
                                                    size="sm"
                                                    variant="destructiveGhost"
                                                />
                                            )}
                                        </TableCell>
                                    )}
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                ) : (
                    <p className="py-8 text-center text-sm text-content-neutral-secondary">
                        No deployments yet. Deployments are recorded by CI or from the command line with &quot;bytechef
                        automation app deployment record&quot;.
                    </p>
                )}

                {appDeployments.length > 0 && mayHaveMore && !atMaximum && (
                    <div className="flex justify-center">
                        <Button label="Show more" onClick={() => setLimitIndex(limitIndex + 1)} variant="outline" />
                    </div>
                )}

                {appDeployments.length > 0 && mayHaveMore && atMaximum && (
                    <p className="text-center text-xs text-content-neutral-secondary">
                        Only the newest {limit} deployments are shown.
                    </p>
                )}
            </PageLoader>

            {canDeploy && showDeployDialog && (
                <AppDeployDialog
                    appInstanceId={appInstanceId}
                    appVersions={appVersions}
                    onOpenChange={setShowDeployDialog}
                    open={showDeployDialog}
                />
            )}

            {canDeploy && (
                <AlertDialog
                    cancelLabel="Keep"
                    confirmButtonVariant="default"
                    confirmLabel="Roll back"
                    description={
                        rollbackDeployment
                            ? `Deploy version "${rollbackDeployment.appVersion.name}" again? The hosting provider builds it as a new deployment.`
                            : undefined
                    }
                    isPending={rollbackAppInstanceMutation.isPending}
                    onCancel={() => setRollbackDeployment(undefined)}
                    onConfirm={() =>
                        rollbackDeployment &&
                        rollbackAppInstanceMutation.mutate({
                            appInstanceId,
                            appVersionId: rollbackDeployment.appVersion.id,
                        })
                    }
                    open={rollbackDeployment !== undefined}
                    title="Roll back?"
                />
            )}

            {canDeploy && (
                <AlertDialog
                    cancelLabel="Keep running"
                    confirmLabel="Cancel deployment"
                    description={
                        cancelDeployment
                            ? `Cancel the deployment of version "${cancelDeployment.appVersion.name}"? The hosting provider is asked to stop the build.`
                            : undefined
                    }
                    isPending={cancelAppDeploymentMutation.isPending}
                    onCancel={() => setCancelDeployment(undefined)}
                    onConfirm={() =>
                        cancelDeployment &&
                        cancelAppDeploymentMutation.mutate({appDeploymentId: cancelDeployment.id, appInstanceId})
                    }
                    open={cancelDeployment !== undefined}
                    title="Cancel deployment?"
                />
            )}
        </div>
    );
};

export default AppDeploymentsTab;
