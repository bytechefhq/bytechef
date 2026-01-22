import {useActivateStore} from '@/pages/account/public/stores/useActivateStore';
import {Mock, vi} from 'vitest';

export const createMockActivateStore = (overrides = {}) => {
    const defaultMock = {
        activate: vi.fn(),
        activationFailure: false,
        activationSuccess: false,
        loading: false,
    };

    return {
        ...defaultMock,
        ...overrides,
    };
};

export const mockActivateStore = (overrides = {}) => {
    const mockStore = createMockActivateStore(overrides);

    (useActivateStore as unknown as Mock).mockReturnValue(mockStore);

    return mockStore;
};
