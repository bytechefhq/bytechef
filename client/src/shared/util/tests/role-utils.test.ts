import {describe, expect, it} from 'vitest';

import {getRoleLabel} from '../role-utils';

describe('getRoleLabel', () => {
    it('strips the ROLE_ prefix and title-cases the rest', () => {
        expect(getRoleLabel('ROLE_ADMIN')).toBe('Admin');
    });

    it('title-cases a bare role name', () => {
        expect(getRoleLabel('EDITOR')).toBe('Editor');
    });

    it('turns underscore-separated words into space-separated words', () => {
        expect(getRoleLabel('ROLE_WORKSPACE_ADMIN')).toBe('Workspace Admin');
    });

    it('normalizes lower-case input', () => {
        expect(getRoleLabel('viewer')).toBe('Viewer');
    });

    it('ignores leading, trailing and repeated underscores', () => {
        expect(getRoleLabel('_PROJECT__OWNER_')).toBe('Project Owner');
    });

    it('only strips ROLE_ when it is a prefix', () => {
        expect(getRoleLabel('SUPER_ROLE_ADMIN')).toBe('Super Role Admin');
    });

    it('returns an empty label for an empty value', () => {
        expect(getRoleLabel('')).toBe('');
    });
});
