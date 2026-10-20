import Badge from '@/components/Badge/Badge';
import {EyeIcon} from 'lucide-react';

const WorkflowViewOnlyBadge = () => (
    <Badge
        aria-label="View only"
        className="ml-3"
        icon={<EyeIcon />}
        role="status"
        styleType="secondary-outline"
        title="You can view this workflow but not change or run it"
    >
        View only
    </Badge>
);

export default WorkflowViewOnlyBadge;
