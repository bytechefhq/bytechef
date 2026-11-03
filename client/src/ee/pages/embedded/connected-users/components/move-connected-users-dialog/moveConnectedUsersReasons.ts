export const CONFIGURATIONS_LOAD_FAILED_REASON = "Couldn't load the configurations. Try again later.";

export const NO_CONNECTED_USERS_REASON = 'No connected users on this configuration';

export const NO_OTHER_CONFIGURATION_REASON = 'No other configuration in this environment';

export const formatSkippedLine = (
    label: string,
    {reason, workflowLabels}: {reason: string; workflowLabels: string[]}
): string => {
    const workflows = workflowLabels.join(', ');

    switch (reason) {
        case 'TARGET_LACKS_REFERENCED_WORKFLOWS':
            return `${label}: the target has no ${workflows}`;
        case 'TARGET_LACKS_ENABLED_WORKFLOWS':
            return `${label}: the target has no enabled ${workflows}`;
        case 'CONNECTION_MISMATCH':
            return `${label}: the target uses a different authorization type`;
        case 'NOT_ON_SOURCE':
            return `${label}: no longer on this configuration`;
        case 'MOVE_FAILED':
            return `${label}: the move failed, try again`;
        default:
            return `${label}: ${reason}`;
    }
};
