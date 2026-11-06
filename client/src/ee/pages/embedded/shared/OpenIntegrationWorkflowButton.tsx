import useOpenInEmbeddedEditor from '@/ee/pages/embedded/shared/useOpenInEmbeddedEditor';
import OpenInButton from '@/shared/components/OpenInButton';

interface OpenIntegrationWorkflowButtonProps {
    integrationId: number;
    workflowUuid?: string;
}

const OpenIntegrationWorkflowButton = ({integrationId, workflowUuid}: OpenIntegrationWorkflowButtonProps) => {
    const {openIntegrationWorkflow} = useOpenInEmbeddedEditor();

    return (
        <OpenInButton
            label="Open in integration"
            onClick={() => void openIntegrationWorkflow(integrationId, workflowUuid)}
        />
    );
};

export default OpenIntegrationWorkflowButton;
