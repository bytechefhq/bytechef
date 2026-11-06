import useOpenInEmbeddedEditor from '@/ee/pages/embedded/shared/useOpenInEmbeddedEditor';
import OpenInButton from '@/shared/components/OpenInButton';

interface OpenAutomationWorkflowButtonProps {
    label: string;
    workflowUuid?: string;
}

const OpenAutomationWorkflowButton = ({label, workflowUuid}: OpenAutomationWorkflowButtonProps) => {
    const {openAutomationWorkflow} = useOpenInEmbeddedEditor();

    return <OpenInButton label={label} onClick={() => openAutomationWorkflow(workflowUuid)} />;
};

export default OpenAutomationWorkflowButton;
