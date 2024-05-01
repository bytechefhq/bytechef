import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';

const Account = () => {
    return (
        <LayoutContainer
            header={<PageHeader centerTitle={true} position="main" title="Account" />}
            leftSidebarOpen={false}
        >
            <div className="w-full p-4 2xl:mx-auto 2xl:w-4/5">TODO</div>
        </LayoutContainer>
    );
};

export default Account;
