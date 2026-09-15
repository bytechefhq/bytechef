import {getRoleLabel, getScopeActionLabel} from '@/shared/util/role-utils';
import {describe, expect, it} from 'vitest';

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

    it('title-cases a scope name', () => {
        expect(getRoleLabel('WORKFLOW_VIEW')).toBe('Workflow View');
    });

    it('returns an empty label for an empty value', () => {
        expect(getRoleLabel('')).toBe('');
    });
});

describe('getScopeActionLabel', () => {
    it('drops the module a heading already carries', () => {
        expect(getScopeActionLabel('WORKFLOW', 'WORKFLOW_VIEW')).toBe('View');
        expect(getScopeActionLabel('WORKSPACE', 'WORKSPACE_MEMBER_MANAGE')).toBe('Member Manage');
    });

    it('keeps the full label when the scope does not carry its module prefix', () => {
        // The module is the enum declaring the scope, not the scope name's first word, so a module is free to declare
        // a scope named anything. Stripping blindly would leave an unlabelled checkbox.
        expect(getScopeActionLabel('WORKSPACE', 'LEGACY_SCOPE')).toBe('Legacy Scope');
    });
});
