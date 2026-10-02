import ComponentVisibilityTab from '@/ee/pages/settings/platform/component-policies/components/ComponentVisibilityTab';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';

const ComponentPolicies = () => {
    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" title="Component Policies" />}
            leftSidebarOpen={false}
        >
            <div className="w-full px-6 3xl:mx-auto 3xl:w-4/5">
                <ComponentVisibilityTab />
            </div>
        </LayoutContainer>
    );
};

export default ComponentPolicies;
