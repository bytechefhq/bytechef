import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {BoxIcon, FlaskConicalIcon, type LucideIcon, WrenchIcon} from 'lucide-react';

export interface EnvironmentConfigI {
    description: string;
    icon: LucideIcon;
    label: string;
    shortLabel: string;
    sidebarTheme: 'development' | 'production' | 'staging';
    styleType: 'destructive-outline' | 'secondary-outline' | 'warning-outline';
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
        description: 'Live environment serving real users, with strict safeguards in place.',
        icon: BoxIcon,
        label: 'PRODUCTION',
        shortLabel: 'PRD',
        sidebarTheme: 'production',
        styleType: 'destructive-outline',
    },
    [STAGING_ENVIRONMENT]: {
        description: 'Final testing, QA, and validation of changes before they reach production.',
        icon: FlaskConicalIcon,
        label: 'STAGING',
        shortLabel: 'STG',
        sidebarTheme: 'staging',
        styleType: 'warning-outline',
    },
};
