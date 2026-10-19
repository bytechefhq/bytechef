import {AnalyticsI, useAnalytics} from '@/shared/hooks/useAnalytics';
import {renderHook, waitFor} from '@testing-library/react';
import posthog from 'posthog-js';
import {beforeEach, describe, expect, it, vi} from 'vitest';

describe('useAnalytics', () => {
    beforeEach(() => {
        vi.mocked(posthog.capture).mockClear();
    });

    it.each<[keyof AnalyticsI, string, Record<string, unknown>]>([
        ['captureProjectCreatedFromTemplate', 'project_created', {template: true}],
        ['captureProjectImported', 'project_created', {imported: true}],
        ['captureProjectWorkflowCreatedFromTemplate', 'project_workflow_created', {template: true}],
    ])('%s captures %s with %o', async (methodName, eventName, properties) => {
        const {result} = renderHook(() => useAnalytics());

        (result.current[methodName] as () => void)();

        await waitFor(() => expect(posthog.capture).toHaveBeenCalledWith(eventName, properties));
    });
});
