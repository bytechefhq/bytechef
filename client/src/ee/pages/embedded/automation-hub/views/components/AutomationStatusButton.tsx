import Button from '@/components/Button/Button';
import {
    describeAttentionReason,
    isInvalidWorkflowAttentionReason,
} from '@/ee/pages/embedded/automation-hub/utils/attentionReason';
import {ConnectedUserProjectWorkflow} from '@/ee/shared/middleware/embedded/public';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

const UNPUBLISHED_TITLE = 'Publish this automation in the builder before enabling it';

interface AutomationStatusButtonProps {
    automation: ConnectedUserProjectWorkflow;
    label: string;
    onEnabledChange: (enabled: boolean) => void;
}

const AutomationStatusButton = ({automation, label, onEnabledChange}: AutomationStatusButtonProps) => {
    const enabled = !!automation.enabled;
    const unpublished = automation.kind === 'COPY' && !automation.workflowVersion;
    const invalidWorkflow = !enabled && isInvalidWorkflowAttentionReason(automation.attentionReason);

    const disabledTitle = useMemo(() => {
        if (unpublished) {
            return UNPUBLISHED_TITLE;
        }

        return invalidWorkflow ? describeAttentionReason(automation.attentionReason) : undefined;
    }, [automation.attentionReason, invalidWorkflow, unpublished]);

    return (
        <Button
            aria-label={`${enabled ? 'Disable' : 'Enable'} ${label}`}
            className={twMerge(
                'min-w-24 text-(--hub-on-accent)',
                enabled
                    ? 'bg-(--hub-disable) hover:bg-(--hub-disable-hover) active:bg-(--hub-disable-hover)'
                    : 'bg-(--hub-enable) hover:bg-(--hub-enable-hover) active:bg-(--hub-enable-hover)'
            )}
            disabled={unpublished || invalidWorkflow || automation.dangling}
            label={enabled ? 'Disable' : 'Enable'}
            onClick={() => onEnabledChange(!enabled)}
            size="sm"
            title={disabledTitle}
            variant="default"
        />
    );
};

export default AutomationStatusButton;
