import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useCodeWorkflowHeaderStore from '@/pages/platform/code-workflow/stores/useCodeWorkflowHeaderStore';
import {SaveIcon, Settings2Icon} from 'lucide-react';
import {useShallow} from 'zustand/react/shallow';

/**
 * The code workflow source editor's controls, rendered in the project header so a code workflow gets one header row
 * rather than two. The editor publishes its state into {@link useCodeWorkflowHeaderStore}.
 */
const CodeWorkflowHeaderActions = () => {
    const {onSaveClick, onTestConfigurationClick, testConfigurationDisabled} = useCodeWorkflowHeaderStore(
        useShallow((state) => ({
            onSaveClick: state.onSaveClick,
            onTestConfigurationClick: state.onTestConfigurationClick,
            testConfigurationDisabled: state.testConfigurationDisabled,
        }))
    );

    if (!onSaveClick) {
        return null;
    }

    return (
        <div className="flex items-center gap-2">
            {onTestConfigurationClick && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <span tabIndex={0}>
                            <Button
                                aria-label="Test Configuration"
                                disabled={testConfigurationDisabled}
                                icon={<Settings2Icon />}
                                onClick={onTestConfigurationClick}
                                size="iconSm"
                                variant="secondary"
                            />
                        </span>
                    </TooltipTrigger>

                    <TooltipContent>
                        {testConfigurationDisabled
                            ? 'Declare a connection or an input in the source to enable test configuration.'
                            : 'Set the workflow test configuration.'}
                    </TooltipContent>
                </Tooltip>
            )}
        </div>
    );
};

/**
 * The code workflow's Save, rendered as an icon so it can sit in the same button group as Test.
 */
export const CodeWorkflowSaveButton = () => {
    const {dirty, onSaveClick, saving} = useCodeWorkflowHeaderStore(
        useShallow((state) => ({
            dirty: state.dirty,
            onSaveClick: state.onSaveClick,
            saving: state.saving,
        }))
    );

    if (!onSaveClick) {
        return null;
    }

    return (
        <Button
            className="rounded-r-none"
            disabled={!dirty || saving}
            icon={<SaveIcon />}
            onClick={onSaveClick}
            size="icon"
        />
    );
};

export default CodeWorkflowHeaderActions;
