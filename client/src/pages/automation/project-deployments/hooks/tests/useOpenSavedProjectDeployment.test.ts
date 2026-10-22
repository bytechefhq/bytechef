import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useOpenSavedProjectDeployment from '../useOpenSavedProjectDeployment';

const {navigateMock} = vi.hoisted(() => ({
    navigateMock: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => {
    const actual = await importOriginal<typeof import('react-router-dom')>();

    return {
        ...actual,
        useNavigate: () => navigateMock,
    };
});

describe('useOpenSavedProjectDeployment', () => {
    const onDevelopmentDeploymentSaved = vi.fn();

    beforeEach(() => {
        navigateMock.mockReset();
        onDevelopmentDeploymentSaved.mockReset();

        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
    });

    const renderOpenSavedProjectDeployment = () =>
        renderHook(() => useOpenSavedProjectDeployment({onDevelopmentDeploymentSaved, projectId: 7}));

    it('hands a development deployment to the caller without leaving the page', () => {
        const {result} = renderOpenSavedProjectDeployment();

        act(() => result.current(42, DEVELOPMENT_ENVIRONMENT));

        expect(onDevelopmentDeploymentSaved).toHaveBeenCalledWith(42);
        expect(navigateMock).not.toHaveBeenCalled();
        expect(environmentStore.getState().currentEnvironmentId).toBe(DEVELOPMENT_ENVIRONMENT);
    });

    it('treats a deployment without an environment as a development deployment', () => {
        const {result} = renderOpenSavedProjectDeployment();

        act(() => result.current(42));

        expect(onDevelopmentDeploymentSaved).toHaveBeenCalledWith(42);
        expect(navigateMock).not.toHaveBeenCalled();
    });

    it.each([STAGING_ENVIRONMENT, PRODUCTION_ENVIRONMENT])(
        'switches to environment %i and opens the deployments page on the saved deployment',
        (environmentId) => {
            const {result} = renderOpenSavedProjectDeployment();

            act(() => result.current(42, environmentId));

            expect(onDevelopmentDeploymentSaved).not.toHaveBeenCalled();
            expect(environmentStore.getState().currentEnvironmentId).toBe(environmentId);
            expect(navigateMock).toHaveBeenCalledWith('/automation/deployments?projectId=7', {
                state: {projectDeploymentId: 42},
            });
        }
    );
});
