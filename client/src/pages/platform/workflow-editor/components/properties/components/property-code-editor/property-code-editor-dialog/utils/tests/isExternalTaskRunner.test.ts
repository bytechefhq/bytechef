import {describe, expect, it} from 'vitest';

import {isExternalTaskRunner} from '../isExternalTaskRunner';

describe('isExternalTaskRunner', () => {
    it('treats a shell script as external whatever the task runner says', () => {
        expect(isExternalTaskRunner('shell', undefined)).toBe(true);
        expect(isExternalTaskRunner('SHELL', {taskRunner: {type: 'graalvm'}})).toBe(true);
    });

    it('treats a script in a non-GraalVM task runner as external', () => {
        expect(isExternalTaskRunner('javascript', {taskRunner: {type: 'docker'}})).toBe(true);
        expect(isExternalTaskRunner('python', {taskRunner: {type: 'process'}})).toBe(true);
    });

    it('treats a script in the GraalVM task runner as not external', () => {
        expect(isExternalTaskRunner('javascript', {taskRunner: {type: 'graalvm'}})).toBe(false);
    });

    it('treats a script that leaves the task runner to its default as not external', () => {
        expect(isExternalTaskRunner('python', undefined)).toBe(false);
        expect(isExternalTaskRunner('python', {})).toBe(false);
        expect(isExternalTaskRunner('javascript', {taskRunner: 'docker'})).toBe(false);
        expect(isExternalTaskRunner('javascript', {taskRunner: {type: 3}})).toBe(false);
    });
});
