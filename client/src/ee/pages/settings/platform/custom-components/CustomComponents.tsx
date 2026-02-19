import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import CustomComponentList from '@/ee/pages/settings/platform/custom-components/components/CustomComponentList';
import {useGetCustomComponentsQuery} from '@/ee/pages/settings/platform/custom-components/queries/customComponents.queries';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Link2Icon} from 'lucide-react';

const CustomComponents = () => {
    const {
        data: customComponents,
        error: apiConnectorsError,
        isLoading: apiConnectorsLoading,
    } = useGetCustomComponentsQuery();

    return (
        <LayoutContainer
            header={
                customComponents &&
                customComponents.length > 0 && <Header centerTitle={true} position="main" title="Custom Components" />
            }
            leftSidebarOpen={false}
        >
            <PageLoader errors={[apiConnectorsError]} loading={apiConnectorsLoading}>
                {customComponents && customComponents?.length > 0 ? (
                    <CustomComponentList customComponents={customComponents} />
                ) : (
                    <EmptyList
                        icon={<Link2Icon className="size-12 text-gray-400" />}
                        message="You do not have any Custom Components created yet."
                        title="No Custom Components"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default CustomComponents;
