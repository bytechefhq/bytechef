'use client';

import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import Header from '@/shared/layout/Header';

import ApprovalTaskDetail from './components/ApprovalTaskDetail';
import ApprovalTaskList from './components/ApprovalTaskList';
import {useApprovalTasks} from './hooks/useApprovalTasks';

export default function ApprovalTasks() {
    useApprovalTasks();

    return (
        <div className="flex size-full bg-background">
            <ApprovalTaskList />

            <div className="flex h-full flex-1 flex-col">
                <Header position="main" right={<EnvironmentSelect />} title="" />

                <div className="min-h-0 flex-1 overflow-hidden">
                    <ApprovalTaskDetail />
                </div>
            </div>
        </div>
    );
}
