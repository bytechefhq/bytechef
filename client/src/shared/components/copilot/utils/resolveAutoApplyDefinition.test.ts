import {MODE, Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {ThreadMessageLike} from '@assistant-ui/react';
import {describe, expect, it} from 'vitest';

import {resolveAutoApplyDefinition} from './resolveAutoApplyDefinition';

const assistant = (content: string): ThreadMessageLike => ({content, role: 'assistant'});
const user = (content: string): ThreadMessageLike => ({content, role: 'user'});

describe('resolveAutoApplyDefinition', () => {
    it('returns the fenced definition from the last assistant BUILD-mode reply', () => {
        const messages = [user('add a delay step'), assistant('Done.\n```json\n{"tasks": []}\n```')];

        expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBe('{"tasks": []}');
    });

    it('returns null in ASK mode (ASK replies are prose, not a definition)', () => {
        const messages = [user('what does this do?'), assistant('It runs the tasks in order.')];

        expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.ASK, messages)).toBeNull();
    });

    it('returns null for other sources even in BUILD mode', () => {
        const messages = [assistant('```json\n{"tasks": []}\n```')];

        expect(resolveAutoApplyDefinition(Source.CODE_EDITOR, MODE.BUILD, messages)).toBeNull();
    });

    it('returns null when there is no assistant message yet', () => {
        expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, [user('add a step')])).toBeNull();
    });

    it('returns null when mode is undefined', () => {
        const messages = [assistant('```json\n{"tasks": []}\n```')];

        expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, undefined, messages)).toBeNull();
    });

    it('returns null when the extracted definition is empty', () => {
        expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, [assistant('   ')])).toBeNull();
    });

    it('picks the most recent assistant reply when several exist', () => {
        const messages = [
            assistant('```json\n{"tasks": ["old"]}\n```'),
            user('change it'),
            assistant('```json\n{"tasks": ["new"]}\n```'),
        ];

        expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBe(
            '{"tasks": ["new"]}'
        );
    });
});
