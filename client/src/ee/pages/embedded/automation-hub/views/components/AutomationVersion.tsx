import Badge from '@/components/Badge/Badge';

interface AutomationVersionProps {
    workflowVersion?: number;
}

const AutomationVersion = ({workflowVersion}: AutomationVersionProps) => {
    if (!workflowVersion) {
        return null;
    }

    return <Badge className="shrink-0" label={`V${workflowVersion}`} styleType="outline-outline" weight="regular" />;
};

export default AutomationVersion;
