import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import ApiConnectorImportDialog from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorImportDialog';
import ApiConnectorList from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorList';
import {useGetApiConnectorsQuery} from '@/ee/shared/queries/platform/apiConnectors.queries';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Link2Icon} from 'lucide-react';

const ApiConnectors = () => {
    const {
        data: apiConnectors,
        error: apiConnectorsError,
        isLoading: apiConnectorsLoading,
    } = useGetApiConnectorsQuery();

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
                                <ApiConnectorImportDialog triggerNode={<Button>Import Open API</Button>} />
                            )
                        }
                        title="API Connectors"
                    />
                )
            }
            leftSidebarOpen={false}
        >
            <PageLoader errors={[apiConnectorsError]} loading={apiConnectorsLoading}>
                {apiConnectors && apiConnectors?.length > 0 ? (
                    <ApiConnectorList apiConnectors={apiConnectors} />
                ) : (
                    <EmptyList
                        button={<ApiConnectorImportDialog triggerNode={<Button>Import Open API</Button>} />}
                        icon={<Link2Icon className="size-12 text-gray-400" />}
                        message="You do not have any API Connectors created yet."
                        title="No API Connectors"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default ApiConnectors;
