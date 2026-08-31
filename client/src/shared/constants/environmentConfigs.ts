import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {BoxIcon, FlaskConicalIcon, type LucideIcon, WrenchIcon} from 'lucide-react';

export interface EnvironmentConfigI {
    description: string;
    icon: LucideIcon;
    label: string;
    /** Worn by the compact selector in the sidebar header, where the full label does not fit beside the wordmark. */
    shortLabel: string;
    /** Value of the sidebar's `data-environment` attribute, which selects the tint rules in styles/index.css. */
    sidebarTheme: 'development' | 'production' | 'staging';
    styleType: 'primary-outline' | 'secondary-outline' | 'warning-outline';
}

export const ENVIRONMENT_CONFIGS: Record<number, EnvironmentConfigI> = {
    [DEVELOPMENT_ENVIRONMENT]: {
        description: 'Features are unstable, experimental, and may change or break frequently.',
        icon: WrenchIcon,
        label: 'DEVELOPMENT',
        shortLabel: 'DEV',
        sidebarTheme: 'development',
        styleType: 'secondary-outline',
    },
    [PRODUCTION_ENVIRONMENT]: {
        description: 'Live environment used by real users. Optimized for performance with strict safeguards.',
        icon: BoxIcon,
        label: 'PRODUCTION',
        shortLabel: 'PRD',
        sidebarTheme: 'production',
        styleType: 'primary-outline',
    },
    [STAGING_ENVIRONMENT]: {
        description: 'Used for final testing, QA, and validation before release.',
        icon: FlaskConicalIcon,
        label: 'STAGING',
        shortLabel: 'STG',
        sidebarTheme: 'staging',
        styleType: 'warning-outline',
    },
};
