import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

/**
 * A code workflow has no visual canvas, so its editor chrome (language badge, Test Configuration, Save) belongs in the
 * project header rather than a second header row of its own. The source editor lives several layers below that header,
 * so it publishes the state the header needs here.
 */
export interface CodeWorkflowHeaderStateI {
    dirty: boolean;
    language?: string;
    onSaveClick?: () => void;
    onTestConfigurationClick?: () => void;
    reset: () => void;
    saving: boolean;
    setCodeWorkflowHeaderState: (state: Partial<CodeWorkflowHeaderStateI>) => void;
    testConfigurationDisabled: boolean;
}

const useCodeWorkflowHeaderStore = create<CodeWorkflowHeaderStateI>()(
    devtools(
        (set) => ({
            dirty: false,
            language: undefined,
            onSaveClick: undefined,
            onTestConfigurationClick: undefined,
            reset: () =>
                set(() => ({
                    dirty: false,
                    language: undefined,
                    onSaveClick: undefined,
                    onTestConfigurationClick: undefined,
                    saving: false,
                    testConfigurationDisabled: true,
                })),
            saving: false,
            setCodeWorkflowHeaderState: (state) => set(() => state),
            testConfigurationDisabled: true,
        }),
        {
            name: 'code-workflow-header',
        }
    )
);

export default useCodeWorkflowHeaderStore;
