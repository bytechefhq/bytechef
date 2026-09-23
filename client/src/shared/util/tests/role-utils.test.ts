import {getRoleLabel, getScopeActionLabel} from '@/shared/util/role-utils';
import {describe, expect, it} from 'vitest';

describe('getRoleLabel', () => {
    it('title-cases a role or scope name', () => {
        expect(getRoleLabel('ROLE_ADMIN')).toBe('Admin');
        expect(getRoleLabel('WORKFLOW_VIEW')).toBe('Workflow View');
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
