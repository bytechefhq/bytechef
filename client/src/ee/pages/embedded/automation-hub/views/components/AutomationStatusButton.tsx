import Button from '@/components/Button/Button';
import {ConnectedUserProjectWorkflow} from '@/ee/shared/middleware/embedded/public';
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

    return (
        <Button
            aria-label={`${enabled ? 'Disable' : 'Enable'} ${label}`}
            className={twMerge(
                'min-w-24 text-(--hub-on-accent)',
                enabled
                    ? 'bg-(--hub-disable) hover:bg-(--hub-disable-hover) active:bg-(--hub-disable-hover)'
                    : 'bg-(--hub-enable) hover:bg-(--hub-enable-hover) active:bg-(--hub-enable-hover)'
            )}
            disabled={unpublished || automation.dangling}
            label={enabled ? 'Disable' : 'Enable'}
            onClick={() => onEnabledChange(!enabled)}
            size="sm"
            title={unpublished ? UNPUBLISHED_TITLE : undefined}
            variant="default"
        />
    );
};

export default AutomationStatusButton;
