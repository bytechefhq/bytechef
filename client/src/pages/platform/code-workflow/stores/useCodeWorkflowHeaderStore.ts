import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

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
