import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {Mock, vi} from 'vitest';

export const createMockApplicationInfoStore = (overrides = {}) => {
    const defaultMock = {
        signUp: {
            activationRequired: false,
            enabled: true,
        },
        // eslint-disable-next-line sort-keys
        getApplicationInfo: vi.fn(),
    };

    return {
        ...defaultMock,
        ...overrides,
    };
};

export const mockApplicationInfoStore = (overrides = {}) => {
    const mockStore = createMockApplicationInfoStore(overrides);

    (useApplicationInfoStore as unknown as Mock).mockReturnValue(mockStore);

    return mockStore;
};
