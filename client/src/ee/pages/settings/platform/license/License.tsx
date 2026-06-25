import PageLoader from '@/components/PageLoader';
import LicenseDetails from '@/ee/pages/settings/platform/license/components/LicenseDetails';
import LicenseUpload from '@/ee/pages/settings/platform/license/components/LicenseUpload';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useLicenceQuery} from '@/shared/middleware/graphql';

const LICENCE_PRESENT_STATUSES = new Set(['EXPIRED', 'GRACE', 'INVALID', 'VALID']);

const License = () => {
    const {data: licenceData, error: licenceError, isLoading: licenceLoading} = useLicenceQuery();

    const licence = licenceData?.licence;
    const hasLicence = licence != null && LICENCE_PRESENT_STATUSES.has(licence.status);

    return (
        <PageLoader errors={[licenceError]} loading={licenceLoading}>
            <LayoutContainer header={<Header centerTitle position="main" title="License" />} leftSidebarOpen={false}>
                <div className="mx-auto max-w-2xl py-8">
                    {hasLicence ? <LicenseDetails licence={licence} /> : <LicenseUpload />}
                </div>
            </LayoutContainer>
        </PageLoader>
    );
};

export default License;
