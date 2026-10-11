import {canOpenInBuilder} from '@/ee/pages/embedded/automation-hub/utils/builderAccess';
import {describe, expect, it} from 'vitest';

const templateCopy = {copiedFromWorkflowUuid: 'tpl-1', kind: 'COPY', workflowUuid: 'copy-1'} as const;
const blankAutomation = {kind: 'COPY', workflowUuid: 'blank-1'} as const;
const reference = {automationWorkflowUuid: 'tpl-1', kind: 'REFERENCE', workflowUuid: 'tpl-1'} as const;

describe('canOpenInBuilder', () => {
    it('opens any copy when editing is allowed', () => {
        const builderAccess = {editWorkflowAllowed: true, newWorkflowEnabled: false};

        expect(canOpenInBuilder(templateCopy, builderAccess)).toBe(true);
        expect(canOpenInBuilder(blankAutomation, builderAccess)).toBe(true);
    });

    it('never opens a reference, whose definition belongs to the template', () => {
        expect(canOpenInBuilder(reference, {editWorkflowAllowed: true, newWorkflowEnabled: true})).toBe(false);
    });

    it('never opens a dangling automation', () => {
        expect(
            canOpenInBuilder({...templateCopy, dangling: true}, {editWorkflowAllowed: true, newWorkflowEnabled: true})
        ).toBe(false);
    });

    it('keeps a template copy closed when editing is withdrawn, even with New workflow on', () => {
        expect(canOpenInBuilder(templateCopy, {editWorkflowAllowed: false, newWorkflowEnabled: true})).toBe(false);
    });

    it('opens a from-scratch automation when New workflow is on although editing is withdrawn', () => {
        expect(canOpenInBuilder(blankAutomation, {editWorkflowAllowed: false, newWorkflowEnabled: true})).toBe(true);
    });

    it('keeps a from-scratch automation closed when both editing and New workflow are off', () => {
        expect(canOpenInBuilder(blankAutomation, {editWorkflowAllowed: false, newWorkflowEnabled: false})).toBe(false);
    });
});
