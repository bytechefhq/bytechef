import {renderHook} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {usePersistJobId} from '../usePersistJobId';

describe('usePersistJobId', () => {
    const workflowId = 'test-workflow';
    const environmentId = 1;
    const key = `bytechef.workflow-test-run.${workflowId}:${environmentId}`;
    const jobId = 'test-job-id';

    afterEach(() => {
        localStorage.clear();
        vi.restoreAllMocks();
    });

    it('returns null for getPersistedJobId when workflowId or environmentId is missing', () => {
        const {result: result1} = renderHook(() => usePersistJobId(undefined, environmentId));
        expect(result1.current.getPersistedJobId()).toBeNull();

        const {result: result2} = renderHook(() => usePersistJobId(workflowId, undefined));
        expect(result2.current.getPersistedJobId()).toBeNull();
    });

    it('returns null for getPersistedJobId when nothing is in localStorage', () => {
        const {result} = renderHook(() => usePersistJobId(workflowId, environmentId));
        expect(result.current.getPersistedJobId()).toBeNull();
    });

    it('returns the value from localStorage for getPersistedJobId', () => {
        localStorage.setItem(key, jobId);

        const {result} = renderHook(() => usePersistJobId(workflowId, environmentId));
        expect(result.current.getPersistedJobId()).toBe(jobId);
    });

    it('persists jobId to localStorage', () => {
        const {result} = renderHook(() => usePersistJobId(workflowId, environmentId));
        result.current.persistJobId(jobId);

        expect(localStorage.getItem(key)).toBe(jobId);
    });

    it('removes jobId from localStorage when jobId is null', () => {
        localStorage.setItem(key, jobId);

        const {result} = renderHook(() => usePersistJobId(workflowId, environmentId));
        result.current.persistJobId(null);

        expect(localStorage.getItem(key)).toBeNull();
    });

    it('does nothing in persistJobId when workflowId or environmentId is missing', () => {
        const setItemSpy = vi.spyOn(Storage.prototype, 'setItem');
        const removeItemSpy = vi.spyOn(Storage.prototype, 'removeItem');

        const {result: result1} = renderHook(() => usePersistJobId(undefined, environmentId));
        result1.current.persistJobId(jobId);
        expect(setItemSpy).not.toHaveBeenCalled();

        const {result: result2} = renderHook(() => usePersistJobId(workflowId, undefined));
        result2.current.persistJobId(null);
        expect(removeItemSpy).not.toHaveBeenCalled();
    });

    it('handles localStorage errors in getPersistedJobId', () => {
        vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
            throw new Error('localStorage error');
        });

        const {result} = renderHook(() => usePersistJobId(workflowId, environmentId));
        expect(result.current.getPersistedJobId()).toBeNull();
    });

    it('handles localStorage errors in persistJobId when setting item', () => {
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
        vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
            throw new Error('localStorage error');
        });

        const {result} = renderHook(() => usePersistJobId(workflowId, environmentId));
        result.current.persistJobId(jobId);

        expect(consoleSpy).toHaveBeenCalledWith('Failed to persist job ID:', expect.any(Error));
    });

    it('handles localStorage errors in persistJobId when removing item', () => {
        vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
            throw new Error('localStorage error');
        });

        const {result} = renderHook(() => usePersistJobId(workflowId, environmentId));
        // Should not throw and not log error (as per implementation)
        expect(() => result.current.persistJobId(null)).not.toThrow();
    });
});
