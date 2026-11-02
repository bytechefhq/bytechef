import {ReactNode, useLayoutEffect} from 'react';

import {setWorkflowEditorReadOnly} from '../utils/workflowEditorReadOnlyGuard';
import {WorkflowEditorReadOnlyContext} from './workflowEditorReadOnlyContext';

interface WorkflowEditorReadOnlyProviderProps {
    children: ReactNode;
    readOnly: boolean;
}

const WorkflowEditorReadOnlyProvider = ({children, readOnly}: WorkflowEditorReadOnlyProviderProps) => {
    useLayoutEffect(() => {
        setWorkflowEditorReadOnly(readOnly);

        return () => setWorkflowEditorReadOnly(false);
    }, [readOnly]);

    return <WorkflowEditorReadOnlyContext.Provider value={readOnly}>{children}</WorkflowEditorReadOnlyContext.Provider>;
};

export default WorkflowEditorReadOnlyProvider;
