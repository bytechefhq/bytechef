import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import ComboBox from '@/components/ComboBox/ComboBox';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {DEPLOYMENT_CONNECTION_COMPONENTS, HOSTING_TARGET_LABELS} from '@/pages/automation/apps/constants';
import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import {
    AppHostingTarget,
    AppInstance,
    useAppBindableConnectionsQuery,
    useAppInstanceDeploymentSettingsQuery,
    useUpdateAppInstanceDeploymentSettingsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {FormEvent, ReactNode, useEffect, useMemo, useState} from 'react';

const ID_PLACEHOLDER = '{id}';

const TARGET_PREVIEW = 'preview';
const TARGET_PRODUCTION = 'production';

type SettingValuesType = Record<string, string>;

const KNOWN_SETTING_KEYS: Record<AppHostingTarget, string[]> = {
    [AppHostingTarget.SelfHosted]: ['cancelUrl', 'deployUrl', 'statusUrl'],
    [AppHostingTarget.Vercel]: ['projectId', 'repository', 'target', 'teamId'],
};

const toStoredSettings = (deploymentSettings: unknown): Record<string, unknown> =>
    deploymentSettings && typeof deploymentSettings === 'object' && !Array.isArray(deploymentSettings)
        ? {...(deploymentSettings as Record<string, unknown>)}
        : {};

interface SettingFieldI {
    help?: string;
    key: string;
    label: string;
    placeholder?: string;
    required?: boolean;
}

const VERCEL_FIELDS: SettingFieldI[] = [
    {help: 'The id of the Vercel project, for example prj_123.', key: 'projectId', label: 'Project ID', required: true},
    {help: 'Leave empty for a project of your personal account.', key: 'teamId', label: 'Team ID (optional)'},
    {
        help: 'The Git repository Vercel builds from, in owner/name form.',
        key: 'repository',
        label: 'Repository',
        placeholder: 'acme/sales-app',
        required: true,
    },
];

const toDefaultTemplate = (deployUrl: string, suffix: string) => {
    const trimmedDeployUrl = deployUrl.trim();

    if (!trimmedDeployUrl) {
        return `<deploy URL>${suffix}`;
    }

    const [path, ...queryParts] = trimmedDeployUrl.split('?');
    const query = queryParts.join('?');
    const pathWithoutTrailingSlash = path.endsWith('/') ? path.slice(0, -1) : path;

    return query ? `${pathWithoutTrailingSlash}${suffix}?${query}` : `${pathWithoutTrailingSlash}${suffix}`;
};

const validateHttpUrl = (value: string, label: string, httpAllowed: boolean): string | undefined => {
    let url: URL;

    try {
        url = new URL(value);
    } catch {
        return `${label} must be an absolute http or https URL`;
    }

    if ((url.protocol !== 'http:' && url.protocol !== 'https:') || !url.hostname) {
        return `${label} must be an absolute http or https URL`;
    }

    if (url.username || url.password || url.hash) {
        return `${label} must not contain credentials or a fragment`;
    }

    if (url.protocol === 'http:' && !httpAllowed) {
        return `${label} must use https outside the Development environment`;
    }

    return undefined;
};

const validateSettings = (
    hostingTarget: AppHostingTarget,
    settingValues: SettingValuesType,
    environmentId: number
): Record<string, string> => {
    const errors: Record<string, string> = {};

    if (hostingTarget === AppHostingTarget.Vercel) {
        if (!settingValues.projectId?.trim()) {
            errors.projectId = 'Project ID is required';
        }

        const repository = settingValues.repository?.trim() || '';
        const repositoryParts = repository.split('/');

        if (!repository) {
            errors.repository = 'Repository is required';
        } else if (repositoryParts.length !== 2 || !repositoryParts[0].trim() || !repositoryParts[1].trim()) {
            errors.repository = 'Repository must be in owner/name form';
        }

        return errors;
    }

    const httpAllowed = environmentId === DEVELOPMENT_ENVIRONMENT;
    const deployUrl = settingValues.deployUrl?.trim() || '';

    if (!deployUrl) {
        errors.deployUrl = 'Deploy URL is required';
    } else {
        const deployUrlError = validateHttpUrl(deployUrl, 'Deploy URL', httpAllowed);

        if (deployUrlError) {
            errors.deployUrl = deployUrlError;
        }
    }

    for (const [key, label] of [
        ['statusUrl', 'Status URL'],
        ['cancelUrl', 'Cancel URL'],
    ]) {
        const template = settingValues[key]?.trim() || '';

        if (!template) {
            continue;
        }

        if (!template.includes(ID_PLACEHOLDER)) {
            errors[key] = `${label} must contain ${ID_PLACEHOLDER}`;

            continue;
        }

        const templateError = validateHttpUrl(template.replaceAll(ID_PLACEHOLDER, 'id'), label, httpAllowed);

        if (templateError) {
            errors[key] = templateError;
        }
    }

    return errors;
};

const toSettingValues = (deploymentSettings: unknown): SettingValuesType => {
    const settingValues: SettingValuesType = {};

    if (deploymentSettings && typeof deploymentSettings === 'object') {
        for (const [key, value] of Object.entries(deploymentSettings as Record<string, unknown>)) {
            if (value !== null && value !== undefined) {
                settingValues[key] = String(value);
            }
        }
    }

    return settingValues;
};

interface AppInstanceDeploymentSettingsDialogProps {
    appInstance: Pick<AppInstance, 'id' | 'name'>;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    triggerNode?: ReactNode;
}

const AppInstanceDeploymentSettingsDialog = ({
    appInstance,
    onOpenChange,
    open,
    triggerNode,
}: AppInstanceDeploymentSettingsDialogProps) => {
    const [connectionId, setConnectionId] = useState('');
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [initializedAppInstanceId, setInitializedAppInstanceId] = useState<string | null>(null);
    const [isOpen, setIsOpen] = useState(false);
    const [settingValues, setSettingValues] = useState<SettingValuesType>({});
    const [showClearDialog, setShowClearDialog] = useState(false);

    const queryClient = useQueryClient();

    const dialogOpen = open ?? isOpen;

    const {data: deploymentSettingsData, isFetching: deploymentSettingsFetching} =
        useAppInstanceDeploymentSettingsQuery({id: appInstance.id}, {enabled: dialogOpen});

    const {data: bindableConnectionsData, isFetching: bindableConnectionsFetching} = useAppBindableConnectionsQuery(
        {appInstanceId: appInstance.id},
        {enabled: dialogOpen}
    );

    const updateDeploymentSettingsMutation = useUpdateAppInstanceDeploymentSettingsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['app']});
            queryClient.invalidateQueries({queryKey: ['appInstance']});
            queryClient.invalidateQueries({queryKey: ['appInstanceDeploymentSettings']});

            setShowClearDialog(false);

            handleOpenChange(false);
        },
    });

    const storedAppInstance =
        deploymentSettingsData?.appInstance.id === appInstance.id ? deploymentSettingsData.appInstance : undefined;

    const formInitialized = storedAppInstance !== undefined && initializedAppInstanceId === storedAppInstance.id;
    const hostingTarget = storedAppInstance?.hostingTarget;
    const environmentId = Number(storedAppInstance?.environmentId);
    const productionInstance = environmentId === PRODUCTION_ENVIRONMENT;
    const readOnly = !storedAppInstance?.deploymentProviderAvailable;

    const requiredComponent = hostingTarget ? DEPLOYMENT_CONNECTION_COMPONENTS[hostingTarget] : undefined;

    const connectionItems = useMemo(() => {
        const items = (bindableConnectionsData?.appBindableConnections ?? [])
            .filter((connection) => connection.componentName === requiredComponent?.componentName)
            .map((connection) => ({label: connection.name, value: connection.id}));

        const storedConnectionId = storedAppInstance?.deploymentConnectionId;

        if (storedConnectionId && !items.some((item) => item.value === storedConnectionId)) {
            const connectionListSettled = !bindableConnectionsFetching && bindableConnectionsData !== undefined;

            items.push({
                label: connectionListSettled
                    ? `Connection ${storedConnectionId} (may not be a ${requiredComponent?.label} connection)`
                    : `Connection ${storedConnectionId}`,
                value: storedConnectionId,
            });
        }

        return items;
    }, [
        bindableConnectionsData,
        bindableConnectionsFetching,
        requiredComponent?.componentName,
        requiredComponent?.label,
        storedAppInstance?.deploymentConnectionId,
    ]);

    const defaultTarget = productionInstance ? TARGET_PRODUCTION : TARGET_PREVIEW;

    const targetOptions = productionInstance ? [TARGET_PRODUCTION, TARGET_PREVIEW] : [TARGET_PREVIEW];

    const selectedTarget = settingValues.target || defaultTarget;

    const targetNotAllowed = !!settingValues.target && !targetOptions.includes(settingValues.target);

    const offeredTargets = targetNotAllowed ? [...targetOptions, settingValues.target] : targetOptions;

    function handleOpenChange(nextOpen: boolean) {
        setFieldErrors({});
        setIsOpen(nextOpen);

        onOpenChange?.(nextOpen);
    }

    const setSettingValue = (key: string, value: string) => setSettingValues({...settingValues, [key]: value});

    const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!hostingTarget || readOnly || !formInitialized) {
            return;
        }

        const errors = validateSettings(hostingTarget, settingValues, environmentId);

        if (!connectionId) {
            errors.connectionId = 'Connection is required';
        }

        setFieldErrors(errors);

        if (Object.keys(errors).length > 0) {
            return;
        }

        const deploymentSettings = toStoredSettings(storedAppInstance?.deploymentSettings);

        for (const settingKey of KNOWN_SETTING_KEYS[hostingTarget]) {
            const value = (settingValues[settingKey] || '').trim();

            if (value) {
                deploymentSettings[settingKey] = value;
            } else {
                delete deploymentSettings[settingKey];
            }
        }

        updateDeploymentSettingsMutation.mutate({
            id: appInstance.id,
            input: {deploymentConnectionId: connectionId, deploymentSettings},
        });
    };

    const renderTextField = (settingField: SettingFieldI, placeholder?: string) => (
        <div className="space-y-1" key={settingField.key}>
            <Label htmlFor={`deployment-setting-${settingField.key}`}>{settingField.label}</Label>

            <Input
                aria-invalid={fieldErrors[settingField.key] !== undefined}
                disabled={readOnly}
                id={`deployment-setting-${settingField.key}`}
                onChange={(event) => setSettingValue(settingField.key, event.target.value)}
                placeholder={placeholder || settingField.placeholder}
                value={settingValues[settingField.key] || ''}
            />

            {settingField.help && <p className="text-xs text-content-neutral-secondary">{settingField.help}</p>}

            {fieldErrors[settingField.key] && (
                <p className="text-sm text-content-destructive">{fieldErrors[settingField.key]}</p>
            )}
        </div>
    );

    useEffect(() => {
        if (!dialogOpen) {
            setInitializedAppInstanceId(null);

            return;
        }

        if (storedAppInstance && !deploymentSettingsFetching && initializedAppInstanceId !== storedAppInstance.id) {
            setConnectionId(storedAppInstance.deploymentConnectionId || '');
            setInitializedAppInstanceId(storedAppInstance.id);
            setSettingValues(toSettingValues(storedAppInstance.deploymentSettings));
        }
    }, [deploymentSettingsFetching, dialogOpen, initializedAppInstanceId, storedAppInstance]);

    return (
        <>
            <Dialog onOpenChange={handleOpenChange} open={dialogOpen}>
                {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

                <DialogContent size="md">
                    <DialogMain>
                        <DialogHeader
                            description="How the hosting provider deploys this instance. The credential lives in the connection; these settings are not secret and are visible to everyone who can view the instance."
                            title={`Deployment of ${appInstance.name}`}
                        />

                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit}>
                            <DialogBody>
                                {!formInitialized && (
                                    <p className="py-4 text-sm text-content-neutral-secondary">
                                        Loading the deployment settings...
                                    </p>
                                )}

                                {formInitialized && hostingTarget && requiredComponent && (
                                    <fieldset className="space-y-4 border-0 p-0">
                                        {readOnly && (
                                            <p className="rounded-md border p-3 text-sm text-content-neutral-secondary">
                                                {`No deployment provider is available for the ${HOSTING_TARGET_LABELS[hostingTarget]} hosting target on this server, so these settings cannot be changed here.`}
                                            </p>
                                        )}

                                        <div className="space-y-1">
                                            <Label>Connection</Label>

                                            <ComboBox
                                                aria-label="Connection"
                                                disabled={readOnly}
                                                emptyMessage={`No ${requiredComponent.label} connection is available in the workspace and environment of this instance.`}
                                                items={connectionItems}
                                                onChange={(item) => setConnectionId(item?.value ?? '')}
                                                value={connectionId}
                                            />

                                            {connectionItems.length === 0 ? (
                                                <p className="text-xs text-content-neutral-secondary">
                                                    {`No ${requiredComponent.label} connection is available in the workspace and environment of this instance. Create a ${requiredComponent.label} connection there first.`}
                                                </p>
                                            ) : (
                                                <p className="text-xs text-content-neutral-secondary">
                                                    {`A ${requiredComponent.label} connection of the workspace and environment of this instance.`}
                                                </p>
                                            )}

                                            {fieldErrors.connectionId && (
                                                <p className="text-sm text-content-destructive">
                                                    {fieldErrors.connectionId}
                                                </p>
                                            )}
                                        </div>

                                        {hostingTarget === AppHostingTarget.Vercel && (
                                            <>
                                                {VERCEL_FIELDS.map((settingField) => renderTextField(settingField))}

                                                <div className="space-y-1">
                                                    <Label htmlFor="deployment-setting-target">Target</Label>

                                                    <Select
                                                        disabled={readOnly}
                                                        onValueChange={(target) => setSettingValue('target', target)}
                                                        value={selectedTarget}
                                                    >
                                                        <SelectTrigger
                                                            aria-label="Target"
                                                            id="deployment-setting-target"
                                                        >
                                                            <SelectValue />
                                                        </SelectTrigger>

                                                        <SelectContent>
                                                            {offeredTargets.map((targetOption) => (
                                                                <SelectItem key={targetOption} value={targetOption}>
                                                                    {targetOption}
                                                                </SelectItem>
                                                            ))}
                                                        </SelectContent>
                                                    </Select>

                                                    <p className="text-xs text-content-neutral-secondary">
                                                        {productionInstance
                                                            ? 'A production instance deploys to the production target unless you choose preview.'
                                                            : 'Only a production instance may deploy to the production target.'}
                                                    </p>

                                                    {targetNotAllowed && (
                                                        <p className="text-sm text-content-destructive">
                                                            {`The environment of this instance does not allow the stored target "${settingValues.target}". The server rejects these settings until you choose another target.`}
                                                        </p>
                                                    )}
                                                </div>
                                            </>
                                        )}

                                        {hostingTarget === AppHostingTarget.SelfHosted && (
                                            <>
                                                {renderTextField({
                                                    help: 'ByteChef sends a POST with the version to this URL to start a deployment.',
                                                    key: 'deployUrl',
                                                    label: 'Deploy URL',
                                                    placeholder: 'https://deploy.example.com/deployments',
                                                    required: true,
                                                })}

                                                {renderTextField(
                                                    {
                                                        help: `Optional. ${ID_PLACEHOLDER} is replaced with the deployment id your endpoint returned. Leave empty for the default shown.`,
                                                        key: 'statusUrl',
                                                        label: 'Status URL (optional)',
                                                    },
                                                    toDefaultTemplate(
                                                        settingValues.deployUrl || '',
                                                        `/${ID_PLACEHOLDER}`
                                                    )
                                                )}

                                                {renderTextField(
                                                    {
                                                        help: `Optional. ${ID_PLACEHOLDER} is replaced with the deployment id your endpoint returned. Leave empty for the default shown.`,
                                                        key: 'cancelUrl',
                                                        label: 'Cancel URL (optional)',
                                                    },
                                                    toDefaultTemplate(
                                                        settingValues.deployUrl || '',
                                                        `/${ID_PLACEHOLDER}/cancel`
                                                    )
                                                )}
                                            </>
                                        )}
                                    </fieldset>
                                )}
                            </DialogBody>

                            <DialogFooter>
                                {formInitialized && !readOnly && storedAppInstance?.deploymentConnectionId && (
                                    <Button
                                        className="mr-auto"
                                        label="Remove deployment configuration"
                                        onClick={() => setShowClearDialog(true)}
                                        type="button"
                                        variant="destructiveGhost"
                                    />
                                )}

                                <DialogCancelButton label={readOnly ? 'Close' : 'Cancel'} />

                                {!readOnly && (
                                    <Button
                                        disabled={!formInitialized || updateDeploymentSettingsMutation.isPending}
                                        label="Save"
                                        type="submit"
                                    />
                                )}
                            </DialogFooter>
                        </form>
                    </DialogMain>
                </DialogContent>
            </Dialog>

            <AlertDialog
                confirmLabel="Remove"
                description="Remove the deployment connection and settings of this instance? Deployments through the provider stop working until you configure them again."
                isPending={updateDeploymentSettingsMutation.isPending}
                onCancel={() => setShowClearDialog(false)}
                onConfirm={() =>
                    updateDeploymentSettingsMutation.mutate({
                        id: appInstance.id,
                        input: {deploymentConnectionId: null, deploymentSettings: {}},
                    })
                }
                open={showClearDialog}
                title="Remove deployment configuration?"
            />
        </>
    );
};

export default AppInstanceDeploymentSettingsDialog;
