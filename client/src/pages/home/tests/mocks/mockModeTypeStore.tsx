import {Mock, vi} from 'vitest';

import {ModeType, useModeTypeStore} from '../../stores/useModeTypeStore';

export const createMockModeTypeStore = (overrides = {}) => {
    const defaultMock = {
        currentType: ModeType.AUTOMATION,
        setCurrentType: vi.fn(),
    };

    return {
        ...defaultMock,
        ...overrides,
    };
};

export const mockModeTypeStore = (overrides = {}) => {
    const mockStore = createMockModeTypeStore(overrides);

    (useModeTypeStore as unknown as Mock).mockReturnValue(mockStore);

    return mockStore;
};
