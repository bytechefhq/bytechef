import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';

const Account = () => {
    return (
        <LayoutContainer header={<Header centerTitle={true} position="main" title="Account" />} leftSidebarOpen={false}>
            <div className="w-full p-4 2xl:mx-auto 2xl:w-4/5">TODO</div>
        </LayoutContainer>
    );
};

export default Account;
