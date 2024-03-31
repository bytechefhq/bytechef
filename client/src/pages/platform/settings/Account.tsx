import LayoutContainer from "@/layouts/LayoutContainer";
import PageHeader from "@/layouts/PageHeader";

const Account = () => {
    return <LayoutContainer
        header={
            <PageHeader
                centerTitle={true}
                position="main"
                title="Account"
            />
        }
        leftSidebarOpen={false}
    >
        <div className="p-4">TODO</div>
    </LayoutContainer>
}

export default Account;
