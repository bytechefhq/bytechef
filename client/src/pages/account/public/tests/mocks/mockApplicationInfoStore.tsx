import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {Mock, vi} from 'vitest';

export const createMockApplicationInfoStore = (overrides = {}) => {
    const defaultMock = {
        getApplicationInfo: vi.fn(),
        signUp: {
            activationRequired: false,
            enabled: true,
        },
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
