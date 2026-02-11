import LoadingIcon from '@/components/LoadingIcon';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const OAuth2Redirect = () => {
    const {authenticated, getAccount} = useAuthenticationStore(
        useShallow((state) => ({
            authenticated: state.authenticated,
            getAccount: state.getAccount,
        }))
    );

    const analytics = useAnalytics();

    const navigate = useNavigate();

    useEffect(() => {
        getAccount().then((account) => {
            if (account) {
                analytics.identify(account);

                navigate('/', {replace: true});
            } else {
                navigate('/login?error=oauth2', {replace: true});
            }
        });
    }, [analytics, getAccount, navigate]);

    if (authenticated) {
        navigate('/', {replace: true});
    }

    return (
        <div className="flex h-screen items-center justify-center">
            <LoadingIcon />
        </div>
    );
};

export default OAuth2Redirect;
