import useOpenInEmbeddedEditor from '@/ee/pages/embedded/shared/useOpenInEmbeddedEditor';
import OpenInButton from '@/shared/components/OpenInButton';

interface OpenAutomationButtonProps {
    label: string;
    workflowUuid?: string;
}

const OpenAutomationButton = ({label, workflowUuid}: OpenAutomationButtonProps) => {
    const {openAutomation} = useOpenInEmbeddedEditor();

    return <OpenInButton label={label} onClick={() => openAutomation(workflowUuid)} />;
};

export default OpenAutomationButton;
