import {EventEmitter} from 'node:events';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {stopProcess} from '../template/scripts/deploy-agent.mjs';

const GRACE_MILLISECONDS = 1000;

function fakeChild() {
    const child = new EventEmitter();

    child.exitCode = null;
    child.pid = 424242;
    child.signalCode = null;

    return child;
}

function exit(child, signal) {
    child.signalCode = signal;
    child.emit('exit', null, signal);
}

describe('deploy agent stopProcess', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('sends SIGTERM, then SIGKILL after the grace period to a child that is still running', () => {
        const child = fakeChild();
        const signalFunction = vi.fn();

        stopProcess(child, GRACE_MILLISECONDS, signalFunction);

        expect(signalFunction.mock.calls).toEqual([[child, 'SIGTERM']]);

        vi.advanceTimersByTime(GRACE_MILLISECONDS);

        expect(signalFunction.mock.calls).toEqual([
            [child, 'SIGTERM'],
            [child, 'SIGKILL'],
        ]);
        expect(vi.getTimerCount()).toBe(0);
    });

    it('issues no kill once the child has exited on SIGTERM and leaves no timer behind', () => {
        const child = fakeChild();
        const signalFunction = vi.fn((target, signal) => {
            if (signal === 'SIGTERM') {
                exit(target, 'SIGTERM');
            }
        });

        stopProcess(child, GRACE_MILLISECONDS, signalFunction);

        expect(vi.getTimerCount()).toBe(0);

        vi.advanceTimersByTime(GRACE_MILLISECONDS * 2);

        expect(signalFunction.mock.calls).toEqual([[child, 'SIGTERM']]);
    });

    it('issues no kill for a child that exited without its exit event being seen', () => {
        const child = fakeChild();
        const signalFunction = vi.fn();

        stopProcess(child, GRACE_MILLISECONDS, signalFunction);

        child.exitCode = 0;

        vi.advanceTimersByTime(GRACE_MILLISECONDS);

        expect(signalFunction.mock.calls).toEqual([[child, 'SIGTERM']]);
    });

    it('arms one escalation timer however often a child is stopped', () => {
        const child = fakeChild();
        const signalFunction = vi.fn();

        stopProcess(child, GRACE_MILLISECONDS, signalFunction);
        stopProcess(child, GRACE_MILLISECONDS, signalFunction);

        expect(vi.getTimerCount()).toBe(1);

        vi.advanceTimersByTime(GRACE_MILLISECONDS);

        expect(signalFunction.mock.calls.filter(([, signal]) => signal === 'SIGKILL')).toHaveLength(1);
    });
});
