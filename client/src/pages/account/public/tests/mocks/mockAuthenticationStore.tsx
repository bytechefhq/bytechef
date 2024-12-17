import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {Mock, vi} from 'vitest';

export const createMockAuthenticationStore = (overrides = {}) => {
    const defaultMock = {
        authenticated: false,
        login: vi.fn().mockResolvedValue(null),
        loginError: false,
    };

    return {
        ...defaultMock,
        ...overrides,
    };
};

export const mockAuthenticationStore = (overrides = {}) => {
    const mockStore = createMockAuthenticationStore(overrides);

    (useAuthenticationStore as unknown as Mock).mockReturnValue(mockStore);

    return mockStore;
};
