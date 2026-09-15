import {act, renderHook} from '@/shared/util/test-utils';
import {type ReactNode} from 'react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {type DialogStepI, DialogStepsProvider, type DialogStepsProviderProps} from './DialogStepsProvider';
import {useDialogSteps, useOptionalDialogSteps} from './hooks/useDialogSteps';

const steps: DialogStepI[] = [
    {id: 'basics', label: 'Basics'},
    {id: 'workflows', label: 'Workflows'},
    {id: 'review', label: 'Review'},
];

type ProviderOptionsType = Partial<Omit<DialogStepsProviderProps, 'children'>>;

function renderDialogSteps(providerOptions: ProviderOptionsType = {}) {
    return renderHook(() => useDialogSteps(), {
        wrapper: ({children}: {children: ReactNode}) => (
            <DialogStepsProvider steps={steps} {...providerOptions}>
                {children}
            </DialogStepsProvider>
        ),
    });
}

afterEach(() => {
    vi.restoreAllMocks();
});

describe('DialogStepsProvider - Initial state', () => {
    it('should start on the first step with nothing completed', () => {
        const {result} = renderDialogSteps();

        expect(result.current.currentStep.id).toBe('basics');
        expect(result.current.currentStepIndex).toBe(0);
        expect(result.current.isFirstStep).toBe(true);
        expect(result.current.isLastStep).toBe(false);
        expect(result.current.completedStepIds.size).toBe(0);
    });

    it('should start on defaultStepId when given', () => {
        const {result} = renderDialogSteps({defaultStepId: 'workflows'});

        expect(result.current.currentStep.id).toBe('workflows');
    });

    it('should make only the first incomplete step reachable', () => {
        const {result} = renderDialogSteps();

        expect(result.current.getStepStatus('basics')).toEqual({
            isCompleted: false,
            isCurrent: true,
            isReachable: true,
        });
        expect(result.current.getStepStatus('workflows')).toEqual({
            isCompleted: false,
            isCurrent: false,
            isReachable: false,
        });
        expect(result.current.getStepStatus('review')).toEqual({
            isCompleted: false,
            isCurrent: false,
            isReachable: false,
        });
    });
});

describe('DialogStepsProvider - Navigation', () => {
    it('should complete the current step and advance on goToNextStep', async () => {
        const {result} = renderDialogSteps();

        await act(async () => {
            await result.current.goToNextStep();
        });

        expect(result.current.currentStep.id).toBe('workflows');
        expect(result.current.getStepStatus('basics')).toEqual({
            isCompleted: true,
            isCurrent: false,
            isReachable: true,
        });
        expect(result.current.getStepStatus('workflows').isReachable).toBe(true);
        expect(result.current.getStepStatus('review').isReachable).toBe(false);
    });

    it('should ignore goToStep for a locked step', () => {
        const {result} = renderDialogSteps();

        act(() => {
            result.current.goToStep('review');
        });

        expect(result.current.currentStep.id).toBe('basics');
    });

    it('should go back to a completed step and keep the first incomplete step reachable', async () => {
        const {result} = renderDialogSteps();

        await act(async () => {
            await result.current.goToNextStep();
        });

        await act(async () => {
            await result.current.goToNextStep();
        });

        act(() => {
            result.current.goToStep('workflows');
        });

        expect(result.current.currentStep.id).toBe('workflows');
        expect(result.current.getStepStatus('workflows')).toEqual({
            isCompleted: true,
            isCurrent: true,
            isReachable: true,
        });
        expect(result.current.getStepStatus('review')).toEqual({
            isCompleted: false,
            isCurrent: false,
            isReachable: true,
        });
    });

    it('should not advance when the current step cannot proceed', async () => {
        const {result} = renderDialogSteps({
            steps: [{canProceed: false, id: 'basics', label: 'Basics'}, ...steps.slice(1)],
        });

        await act(async () => {
            await result.current.goToNextStep();
        });

        expect(result.current.currentStep.id).toBe('basics');
        expect(result.current.completedStepIds.size).toBe(0);
    });

    it('should call onComplete instead of advancing on the last step', async () => {
        const onComplete = vi.fn();
        const {result} = renderDialogSteps({defaultStepId: 'review', onComplete});

        await act(async () => {
            await result.current.goToNextStep();
        });

        expect(onComplete).toHaveBeenCalledTimes(1);
        expect(result.current.currentStep.id).toBe('review');
        expect(result.current.completedStepIds.has('review')).toBe(true);
    });
});

describe('DialogStepsProvider - Validation', () => {
    it('should stay on the step when validateStep returns false', async () => {
        const validateStep = vi.fn().mockResolvedValue(false);
        const {result} = renderDialogSteps({validateStep});

        await act(async () => {
            await result.current.goToNextStep();
        });

        expect(validateStep).toHaveBeenCalledWith(steps[0]);
        expect(result.current.currentStep.id).toBe('basics');
        expect(result.current.completedStepIds.size).toBe(0);
        expect(result.current.isPending).toBe(false);
    });

    it('should be pending while validateStep is running', async () => {
        let resolveValidation: (isValid: boolean) => void = () => undefined;

        const validateStep = vi.fn(
            () =>
                new Promise<boolean>((resolve) => {
                    resolveValidation = resolve;
                })
        );

        const {result} = renderDialogSteps({validateStep});

        let nextStepPromise: Promise<void> = Promise.resolve();

        act(() => {
            nextStepPromise = result.current.goToNextStep();
        });

        expect(result.current.isPending).toBe(true);

        await act(async () => {
            resolveValidation(true);

            await nextStepPromise;
        });

        expect(result.current.isPending).toBe(false);
        expect(result.current.currentStep.id).toBe('workflows');
    });

    it('should clear pending and rethrow when validateStep throws', async () => {
        const validateStep = vi.fn().mockRejectedValue(new Error('Validation failed'));
        const {result} = renderDialogSteps({validateStep});

        await act(async () => {
            await expect(result.current.goToNextStep()).rejects.toThrow('Validation failed');
        });

        expect(result.current.isPending).toBe(false);
        expect(result.current.currentStep.id).toBe('basics');
    });
});

describe('DialogStepsProvider - Controlled and changing steps', () => {
    it('should report step changes without moving in controlled mode', async () => {
        const onCurrentStepChange = vi.fn();
        const {result} = renderDialogSteps({currentStepId: 'basics', onCurrentStepChange});

        await act(async () => {
            await result.current.goToNextStep();
        });

        expect(onCurrentStepChange).toHaveBeenCalledWith('workflows');
        expect(result.current.currentStep.id).toBe('basics');
    });

    it('should fall back to the first incomplete step when the current step is removed', async () => {
        let providerSteps = steps;

        const {rerender, result} = renderHook(() => useDialogSteps(), {
            wrapper: ({children}: {children: ReactNode}) => (
                <DialogStepsProvider steps={providerSteps}>{children}</DialogStepsProvider>
            ),
        });

        await act(async () => {
            await result.current.goToNextStep();
        });

        expect(result.current.currentStep.id).toBe('workflows');

        providerSteps = [steps[0], steps[2]];

        rerender();

        expect(result.current.currentStep.id).toBe('review');
        expect(result.current.currentStepIndex).toBe(1);
    });
});

describe('DialogStepsProvider - Errors', () => {
    it('should throw when steps is empty', () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);

        expect(() =>
            renderHook(() => useDialogSteps(), {
                wrapper: ({children}: {children: ReactNode}) => (
                    <DialogStepsProvider steps={[]}>{children}</DialogStepsProvider>
                ),
            })
        ).toThrow('DialogStepsProvider requires at least one step');
    });

    it('should throw from useDialogSteps outside a provider', () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);

        expect(() => renderHook(() => useDialogSteps())).toThrow(
            'useDialogSteps must be used within a DialogStepsProvider'
        );
    });

    it('should return null from useOptionalDialogSteps outside a provider', () => {
        const {result} = renderHook(() => useOptionalDialogSteps());

        expect(result.current).toBeNull();
    });
});

describe('DialogStepsProvider - TypeScript', () => {
    it('should require steps', () => {
        // @ts-expect-error - steps is required
        const element = <DialogStepsProvider>content</DialogStepsProvider>;

        expect(element).toBeTruthy();
    });
});
