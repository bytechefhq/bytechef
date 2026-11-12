import {ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';

export const CONNECTION_ORIGIN_BADGES: Record<ConnectionOriginEnum, {description: string; label: string}> = {
    [ConnectionOriginEnum.Internal]: {
        description: 'An admin connection shared with every connected user in this environment.',
        label: 'Internal',
    },
    [ConnectionOriginEnum.Provisioned]: {
        description: 'An admin created this connection for the connected user.',
        label: 'Provisioned',
    },
    [ConnectionOriginEnum.SelfServe]: {
        description: 'The connected user created this connection.',
        label: 'Self-created',
    },
};
