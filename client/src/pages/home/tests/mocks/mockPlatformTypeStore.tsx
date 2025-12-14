import {Mock, vi} from 'vitest';

import {PlatformType, usePlatformTypeStore} from '../../stores/usePlatformTypeStore';

export const createMockPlatformTypeStore = (overrides = {}) => {
    const defaultMock = {
        currentType: PlatformType.AUTOMATION,
        setCurrentType: vi.fn(),
    };

    return {
        ...defaultMock,
        ...overrides,
    };
};

export const mockPlatformTypeStore = (overrides = {}) => {
    const mockStore = createMockPlatformTypeStore(overrides);

    (usePlatformTypeStore as unknown as Mock).mockReturnValue(mockStore);

    return mockStore;
};
