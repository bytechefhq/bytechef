import {EmbedInitParamsI} from '@/ee/pages/embedded/shared/useEmbedHandshake';

const CONNECTION_DIALOG_ALLOWED_CLAIM = 'connectionDialogAllowed';

export const getConnectionDialogAllowedClaim = (jwtToken?: string | null): boolean => {
    const payloadSegment = jwtToken?.split('.')[1];

    if (!payloadSegment) {
        return true;
    }

    try {
        const payload = JSON.parse(atob(payloadSegment.replace(/-/g, '+').replace(/_/g, '/')));

        const connectionDialogAllowedClaim = payload?.[CONNECTION_DIALOG_ALLOWED_CLAIM];

        return typeof connectionDialogAllowedClaim === 'boolean' ? connectionDialogAllowedClaim : true;
    } catch {
        return true;
    }
};

export const resolveConnectionDialogAllowed = ({connectionDialogAllowed, jwtToken}: EmbedInitParamsI): boolean =>
    getConnectionDialogAllowedClaim(jwtToken) && connectionDialogAllowed !== false;
