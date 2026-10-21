import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {
    AppDeploymentProviderType,
    AppDeploymentStatus,
    AppHostingTarget,
    AppInstanceStatus,
    AppStatus,
    AppUserAuthMode,
    EnvironmentEnum,
} from '@/shared/middleware/graphql';

export const HOSTING_TARGET_LABELS: Record<AppHostingTarget, string> = {
    [AppHostingTarget.SelfHosted]: 'Self-hosted',
    [AppHostingTarget.Vercel]: 'Vercel',
};

export const APP_STATUS_LABELS: Record<AppStatus, string> = {
    [AppStatus.Active]: 'Active',
    [AppStatus.Archived]: 'Archived',
    [AppStatus.Draft]: 'Draft',
    [AppStatus.Suspended]: 'Suspended',
};

export const APP_INSTANCE_STATUS_LABELS: Record<AppInstanceStatus, string> = {
    [AppInstanceStatus.Active]: 'Active',
    [AppInstanceStatus.Suspended]: 'Suspended',
    [AppInstanceStatus.Terminated]: 'Terminated',
};

export const APP_DEPLOYMENT_STATUS_LABELS: Record<AppDeploymentStatus, string> = {
    [AppDeploymentStatus.Building]: 'Building',
    [AppDeploymentStatus.Canceled]: 'Canceled',
    [AppDeploymentStatus.Failed]: 'Failed',
    [AppDeploymentStatus.Queued]: 'Queued',
    [AppDeploymentStatus.Ready]: 'Ready',
};

export const APP_DEPLOYMENT_STATUS_STYLES: Record<
    AppDeploymentStatus,
    'destructive-filled' | 'outline-outline' | 'primary-outline' | 'secondary-filled' | 'success-filled'
> = {
    [AppDeploymentStatus.Building]: 'primary-outline',
    [AppDeploymentStatus.Canceled]: 'outline-outline',
    [AppDeploymentStatus.Failed]: 'destructive-filled',
    [AppDeploymentStatus.Queued]: 'secondary-filled',
    [AppDeploymentStatus.Ready]: 'success-filled',
};

export const APP_DEPLOYMENT_PROVIDER_LABELS: Record<AppDeploymentProviderType, string> = {
    [AppDeploymentProviderType.DeployHook]: 'Deploy hook',
    [AppDeploymentProviderType.External]: 'External',
    [AppDeploymentProviderType.Vercel]: 'Vercel',
};

export const APP_USER_AUTH_MODE_LABELS: Record<AppUserAuthMode, string> = {
    [AppUserAuthMode.None]: 'None',
    [AppUserAuthMode.Optional]: 'Optional',
    [AppUserAuthMode.Required]: 'Required',
};

export const APP_ENVIRONMENT_IDS = [DEVELOPMENT_ENVIRONMENT, STAGING_ENVIRONMENT, PRODUCTION_ENVIRONMENT];

export const APP_ENVIRONMENT_ENUMS: Record<number, EnvironmentEnum> = {
    [DEVELOPMENT_ENVIRONMENT]: EnvironmentEnum.Development,
    [PRODUCTION_ENVIRONMENT]: EnvironmentEnum.Production,
    [STAGING_ENVIRONMENT]: EnvironmentEnum.Staging,
};

export const APP_DEPLOY_SCOPE = 'APP_DEPLOY';

export const APP_MANAGE_SCOPE = 'APP_MANAGE';

export const APP_DEPLOYMENT_POLL_INTERVAL_MILLIS = 5000;

export const APP_DEPLOYMENT_IN_PROGRESS_STATUSES: AppDeploymentStatus[] = [
    AppDeploymentStatus.Building,
    AppDeploymentStatus.Queued,
];

export const isAppDeploymentInProgress = (status: AppDeploymentStatus) =>
    APP_DEPLOYMENT_IN_PROGRESS_STATUSES.includes(status);

export const DEPLOYMENT_CONNECTION_COMPONENTS: Record<AppHostingTarget, {componentName: string; label: string}> = {
    [AppHostingTarget.SelfHosted]: {componentName: 'httpClient', label: 'HTTP Client'},
    [AppHostingTarget.Vercel]: {componentName: 'vercel', label: 'Vercel'},
};
