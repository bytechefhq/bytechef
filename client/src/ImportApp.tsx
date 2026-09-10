import {Toaster} from '@/components/ui/sonner';
import useFetchInterceptor from '@/config/useFetchInterceptor';
import {Outlet} from 'react-router-dom';

const ImportApp = () => {
    useFetchInterceptor();

    return (
        <>
            <Outlet />

            <Toaster />
        </>
    );
};

export default ImportApp;
