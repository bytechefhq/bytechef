import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';

export const getEnvironmentName = (environmentId: number): string => {
    const environmentLabel = ENVIRONMENT_CONFIGS[environmentId]?.label.toLowerCase();

    if (!environmentLabel) {
        return 'this environment';
    }

    return `${environmentLabel.charAt(0).toUpperCase()}${environmentLabel.slice(1)}`;
};
