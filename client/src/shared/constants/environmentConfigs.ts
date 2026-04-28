import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {BoxIcon, FlaskConicalIcon, type LucideIcon, WrenchIcon} from 'lucide-react';

export interface EnvironmentConfigI {
    description: string;
    icon: LucideIcon;
    label: string;
    styleType: 'primary-outline' | 'secondary-outline' | 'warning-outline';
}

export const ENVIRONMENT_CONFIGS: Record<number, EnvironmentConfigI> = {
    [DEVELOPMENT_ENVIRONMENT]: {
        description: 'Features are unstable, experimental, and may change or break frequently.',
        icon: WrenchIcon,
        label: 'DEVELOPMENT',
        styleType: 'secondary-outline',
    },
    [PRODUCTION_ENVIRONMENT]: {
        description: 'Live environment used by real users. Optimized for performance with strict safeguards.',
        icon: BoxIcon,
        label: 'PRODUCTION',
        styleType: 'primary-outline',
    },
    [STAGING_ENVIRONMENT]: {
        description: 'Used for final testing, QA, and validation before release.',
        icon: FlaskConicalIcon,
        label: 'STAGING',
        styleType: 'warning-outline',
    },
};
