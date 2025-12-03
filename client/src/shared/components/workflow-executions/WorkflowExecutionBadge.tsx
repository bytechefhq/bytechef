import Badge from '@/components/Badge/Badge';
import React from 'react';

type StyleType = NonNullable<React.ComponentProps<typeof Badge>['styleType']>;

const STYLE_MAP: Record<string, StyleType> = {
    COMPLETED: 'success-outline',
    FAILED: 'destructive-filled',
};

const WorkflowExecutionBadge = ({status}: {status: string}) => {
    const styleType: StyleType = STYLE_MAP[status] ?? 'secondary-filled';

    return (
        <div className="flex items-center">
            <Badge className="uppercase" label={status ?? ''} styleType={styleType} weight="semibold" />
        </div>
    );
};

export default WorkflowExecutionBadge;
