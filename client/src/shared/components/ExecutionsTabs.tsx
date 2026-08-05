import {Tabs, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {useLocation, useNavigate} from 'react-router-dom';

interface ExecutionsTabsProps {
    basePath: string;
}

const ExecutionsTabs = ({basePath}: ExecutionsTabsProps) => {
    const navigate = useNavigate();

    const location = useLocation();

    const toolInvocationsPath = `${basePath}/tool-invocations`;

    const activeTab = location.pathname === toolInvocationsPath ? toolInvocationsPath : basePath;

    return (
        <Tabs onValueChange={(path) => navigate(path)} value={activeTab}>
            <TabsList>
                <TabsTrigger value={basePath}>Workflow Executions</TabsTrigger>

                <TabsTrigger value={toolInvocationsPath}>Tool Invocations</TabsTrigger>
            </TabsList>
        </Tabs>
    );
};

export default ExecutionsTabs;
