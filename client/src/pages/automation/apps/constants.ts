import {AppHostingTarget} from '@/shared/middleware/graphql';

export const HOSTING_TARGET_LABELS: Record<AppHostingTarget, string> = {
    [AppHostingTarget.SelfHosted]: 'Self-hosted',
    [AppHostingTarget.Vercel]: 'Vercel',
};

export const APP_MANAGE_SCOPE = 'APP_MANAGE';
