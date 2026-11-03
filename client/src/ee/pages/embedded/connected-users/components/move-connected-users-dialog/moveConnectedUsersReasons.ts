export const ALREADY_ON_TARGET_REASON = 'Already on this configuration';

export const CONFIGURATIONS_LOAD_FAILED_REASON = "Couldn't load the configurations. Try again later.";

export const NO_CONNECTED_USERS_REASON = 'No connected users on this configuration';

export const NO_OTHER_CONFIGURATION_REASON = 'No other configuration in this environment';

export const formatSkippedReason = (
    {reason}: {reason: string},
    notOnSourceReason = 'No longer on this configuration'
): string => {
    switch (reason) {
        case 'ALREADY_ON_TARGET':
            return ALREADY_ON_TARGET_REASON;
        case 'TARGET_LACKS_REFERENCED_WORKFLOWS':
            return "The target configuration doesn't have these workflows:";
        case 'TARGET_LACKS_ENABLED_WORKFLOWS':
            return "These workflows aren't enabled on the target configuration:";
        case 'CONNECTION_MISMATCH':
            return 'The target configuration uses a different authorization type';
        case 'NOT_ON_SOURCE':
            return notOnSourceReason;
        case 'MOVE_FAILED':
            return 'The move failed. Try again.';
        default:
            return reason;
    }
};
