import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';

// import PageLoader from '@/components/PageLoader';
// TODO: Uncomment when ApiConnectorImportDialog is implemented
// import ApiConnectorImportDialog from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorImportDialog';
import ApiConnectorList from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorList';

// TODO: Uncomment when apiConnectors.queries is implemented
// import {useGetApiConnectorsQuery} from '@/ee/shared/queries/platform/apiConnectors.queries';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Link2Icon} from 'lucide-react';

// TODO: Remove mock data when useGetApiConnectorsQuery is implemented
const mockApiConnectors: Array<{
    description?: string;
    enabled: boolean;
    endpoints?: Array<{
        httpMethod: string;
        id: string;
        lastExecutionDate?: Date;
        name: string;
        path: string;
    }>;
    id: string;
    lastModifiedDate?: Date;
    name: string;
    title: string;
}> = [];

const ApiConnectors = () => {
    // TODO: Uncomment when apiConnectors.queries is implemented
    // const {
    //     data: apiConnectors,
    //     error: apiConnectorsError,
    //     isLoading: apiConnectorsLoading,
    // } = useGetApiConnectorsQuery();
    const apiConnectors = mockApiConnectors;

    return (
        <LayoutContainer
            header={
                apiConnectors &&
                apiConnectors.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            apiConnectors &&
                            apiConnectors.length > 0 && (
                                // TODO: Replace with ApiConnectorImportDialog when implemented
                                <Button>Import Open API</Button>
                            )
                        }
                        title="API Connectors"
                    />
                )
            }
            leftSidebarOpen={false}
        >
            {/* TODO: Wrap with PageLoader when apiConnectors.queries is implemented */}

            {apiConnectors && apiConnectors?.length > 0 ? (
                <ApiConnectorList apiConnectors={apiConnectors} />
            ) : (
                <EmptyList
                    button={
                        // TODO: Replace with ApiConnectorImportDialog when implemented
                        <Button>Import Open API</Button>
                    }
                    icon={<Link2Icon className="size-12 text-gray-400" />}
                    message="You do not have any API Connectors created yet."
                    title="No API Connectors"
                />
            )}
        </LayoutContainer>
    );
};

export default ApiConnectors;
