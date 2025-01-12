import {usePasswordResetStore} from '@/pages/account/public/stores/usePasswordResetStore';
import {Mock, vi} from 'vitest';

export const createMockAuthenticationStore = (overrides = {}) => {
    const defaultMock = {
        reset: vi.fn(),
        resetPasswordFailure: false,
        resetPasswordInit: vi.fn(),
        resetPasswordSuccess: false,
    };

    return {
        ...defaultMock,
        ...overrides,
    };
};

export const mockPasswordResetStore = (overrides = {}) => {
    const mockStore = createMockAuthenticationStore(overrides);

    (usePasswordResetStore as unknown as Mock).mockReturnValue(mockStore);

    return mockStore;
};
