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

    it('returns null for an unfenced prose reply', () => {
        const messages = [user('add a delay step'), assistant('I added a delay step for you.')];

        expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBeNull();
    });

    it('returns null when the fenced block is not a JSON/YAML document', () => {
        const messages = [assistant('```\njust some notes\n```')];

        expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBeNull();
    });

    it('returns null when the fenced block is neither valid JSON nor valid YAML', () => {
        const messages = [assistant('```json\n{"tasks": [\n```')];

        expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBeNull();
    });

    it('accepts a fenced yaml document', () => {
        const messages = [assistant('```yaml\nlabel: x\ntasks: []\n```')];

        expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBe(
            'label: x\ntasks: []'
        );
    });

    it('picks the most recent assistant reply when several exist', () => {
        const messages = [
            assistant('```json\n{"tasks": [{"name": "old_1", "type": "delay/v1/delay"}]}\n```'),
            user('change it'),
            assistant('```json\n{"tasks": [{"name": "new_1", "type": "delay/v1/delay"}]}\n```'),
        ];

        expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBe(
            '{"tasks": [{"name": "new_1", "type": "delay/v1/delay"}]}'
        );
    });

    describe('workflow shape validation', () => {
        it('returns null for a JSON object that is not a workflow', () => {
            const messages = [assistant('```json\n{"notes": "Remember to add a delay step"}\n```')];

            expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBeNull();
        });

        it('returns null for a YAML mapping that is not a workflow', () => {
            const messages = [assistant('```yaml\nnotes: remember to add a delay step\n```')];

            expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBeNull();
        });

        it('returns null when tasks is not an array', () => {
            const messages = [assistant('```json\n{"tasks": {"name": "delay_1", "type": "delay/v1/delay"}}\n```')];

            expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBeNull();
        });

        it('returns null when a task is missing its type', () => {
            const messages = [assistant('```json\n{"tasks": [{"name": "delay_1"}]}\n```')];

            expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBeNull();
        });

        it('returns null when a task is not an object', () => {
            const messages = [assistant('```json\n{"tasks": ["delay_1"]}\n```')];

            expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBeNull();
        });

        it('returns null when triggers is present but not an array of nodes', () => {
            const messages = [assistant('```json\n{"tasks": [], "triggers": [{"name": "trigger_1"}]}\n```')];

            expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBeNull();
        });

        it('accepts a workflow with named and typed triggers and tasks', () => {
            const definition =
                '{"label": "x", "tasks": [{"name": "delay_1", "parameters": {}, "type": "delay/v1/delay"}], ' +
                '"triggers": [{"name": "trigger_1", "type": "manual/v1/manual"}]}';

            const messages = [assistant('```json\n' + definition + '\n```')];

            expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBe(definition);
        });

        it('accepts a YAML workflow with typed tasks', () => {
            const messages = [assistant('```yaml\ntasks:\n  - name: delay_1\n    type: delay/v1/delay\n```')];

            expect(resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, MODE.BUILD, messages)).toBe(
                'tasks:\n  - name: delay_1\n    type: delay/v1/delay'
            );
        });
    });
});
