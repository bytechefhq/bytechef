'use client';

import ApprovalTaskDetail from './components/ApprovalTaskDetail';
import ApprovalTaskList from './components/ApprovalTaskList';
import {useApprovalTasks} from './hooks/useApprovalTasks';

export default function ApprovalTasks() {
    useApprovalTasks();

    return (
        <div className="flex size-full bg-background">
            <ApprovalTaskList />

            <div className="h-full flex-1">
                <ApprovalTaskDetail />
            </div>
        </div>
    );
}
