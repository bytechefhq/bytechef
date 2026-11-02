import {Tabs, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {ReactNode} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';

const TOOL_INVOCATIONS_TAB = 'tool-invocations';
const WORKFLOW_EXECUTIONS_TAB = 'workflow-executions';

interface ExecutionsTabsProps {
    fallback?: ReactNode;
}

const ExecutionsTabs = ({fallback}: ExecutionsTabsProps) => {
    const edition = useApplicationInfoStore((state) => state.application?.edition);

    const {pathname} = useLocation();
    const navigate = useNavigate();

    if (edition !== EditionType.EE) {
        return <>{fallback}</>;
    }

    const executionsPath = pathname.startsWith('/embedded/') ? '/embedded/executions' : '/automation/executions';

    const currentTab = pathname.startsWith(`${executionsPath}/${TOOL_INVOCATIONS_TAB}`)
        ? TOOL_INVOCATIONS_TAB
        : WORKFLOW_EXECUTIONS_TAB;

    const handleValueChange = (value: string) =>
        navigate(value === TOOL_INVOCATIONS_TAB ? `${executionsPath}/${TOOL_INVOCATIONS_TAB}` : executionsPath);

    return (
        <Tabs onValueChange={handleValueChange} value={currentTab}>
            <TabsList>
                <TabsTrigger value={WORKFLOW_EXECUTIONS_TAB}>Workflow Executions</TabsTrigger>

                <TabsTrigger value={TOOL_INVOCATIONS_TAB}>Tool Invocations</TabsTrigger>
            </TabsList>
        </Tabs>
    );
};

export default ExecutionsTabs;
