import WorkflowChatSidebar from '@/pages/automation/workflow-chat/components/WorkflowChatSidebar';
import {useWorkflowChatStore} from '@/pages/automation/workflow-chat/stores/useWorkflowChatStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useEffect} from 'react';
import {Outlet, useParams} from 'react-router-dom';

const WorkflowChatContainer = () => {
    const {workflowExecutionId} = useParams();

    const reset = useWorkflowChatStore((state) => state.reset);

    useEffect(() => {
        return () => {
            reset();
        };
    }, [reset]);

    return (
        <LayoutContainer
            className="bg-surface-main"
            leftSidebarBody={<WorkflowChatSidebar />}
            leftSidebarHeader={<Header position="sidebar" title="Workflow Chat" />}
            leftSidebarWidth="72"
        >
            {workflowExecutionId ? (
                <Outlet />
            ) : (
                <div className="flex flex-1 items-center justify-center">
                    <div className="text-center text-muted-foreground">
                        <p className="text-lg">No Chat selected. Please selected the one.</p>
                    </div>
                </div>
            )}
        </LayoutContainer>
    );
};

export default WorkflowChatContainer;
