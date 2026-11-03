import Badge from '@/components/Badge/Badge';

export interface SkippedUserDisplayI {
    label: string;
    reason: string;
    workflowLabels: string[];
}

interface MoveConnectedUsersSkippedListProps {
    skippedUsers: SkippedUserDisplayI[];
}

const MoveConnectedUsersSkippedList = ({skippedUsers}: MoveConnectedUsersSkippedListProps) => (
    <ul aria-label="Skipped users" className="divide-y divide-border/50 rounded-md border border-border/50">
        {skippedUsers.map((skippedUser, index) => (
            <li className="px-3 py-2" key={`${skippedUser.label}-${index}`}>
                <p className="font-medium text-content-neutral-primary">{skippedUser.label}</p>

                <p className="text-sm text-content-neutral-secondary">{skippedUser.reason}</p>

                {skippedUser.workflowLabels.length > 0 && (
                    <div className="mt-1 flex flex-wrap gap-1">
                        {skippedUser.workflowLabels.map((workflowLabel) => (
                            <Badge key={workflowLabel} label={workflowLabel} styleType="secondary-outline" />
                        ))}
                    </div>
                )}
            </li>
        ))}
    </ul>
);

export default MoveConnectedUsersSkippedList;
