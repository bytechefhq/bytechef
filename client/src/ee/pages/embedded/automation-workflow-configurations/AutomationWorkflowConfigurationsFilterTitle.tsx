import Badge from '@/components/Badge/Badge';

interface AutomationWorkflowConfigurationsFilterTitleProps {
    projectName?: string;
}

const AutomationWorkflowConfigurationsFilterTitle = ({
    projectName,
}: AutomationWorkflowConfigurationsFilterTitleProps) => (
    <div className="space-x-1">
        <span className="text-sm font-semibold text-muted-foreground uppercase">Filter by:</span>

        <Badge className="border-transparent" styleType="primary-outline" weight="semibold">
            <span className="text-sm">{`Projects: ${projectName ?? 'All Projects'}`}</span>
        </Badge>
    </div>
);

export default AutomationWorkflowConfigurationsFilterTitle;
