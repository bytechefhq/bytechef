import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useCanManageAiSkill from '../useCanManageAiSkill';

const hoisted = vi.hoisted(() => ({
    state: {
        account: {authorities: [] as string[], login: 'owner@localhost.com'} as
            {authorities: string[]; login: string} | undefined,
        authenticated: true,
    },
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn((selector: (state: typeof hoisted.state) => unknown) => selector(hoisted.state)),
}));

describe('useCanManageAiSkill', () => {
    beforeEach(() => {
        hoisted.state = {account: {authorities: ['ROLE_USER'], login: 'owner@localhost.com'}, authenticated: true};
    });

    it('lets the creator manage the skill', () => {
        const {result} = renderHook(() => useCanManageAiSkill('owner@localhost.com'));

        expect(result.current).toBe(true);
    });

    it('does not let another member manage the skill', () => {
        const {result} = renderHook(() => useCanManageAiSkill('other@localhost.com'));

        expect(result.current).toBe(false);
    });

    it('lets a tenant admin manage any skill', () => {
        hoisted.state = {account: {authorities: ['ROLE_ADMIN'], login: 'admin@localhost.com'}, authenticated: true};

        const {result} = renderHook(() => useCanManageAiSkill('other@localhost.com'));

        expect(result.current).toBe(true);
    });

    it('does not let anyone manage a skill without a known creator unless they are a tenant admin', () => {
        const {result} = renderHook(() => useCanManageAiSkill(null));

        expect(result.current).toBe(false);
    });

    it('does not let a visitor without an account manage the skill', () => {
        hoisted.state = {account: undefined, authenticated: false};

        const {result} = renderHook(() => useCanManageAiSkill(undefined));

        expect(result.current).toBe(false);
    });
});
