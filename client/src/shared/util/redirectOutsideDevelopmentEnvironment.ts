import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {redirect} from 'react-router-dom';

const redirectOutsideDevelopmentEnvironment = (path: string) => {
    if (environmentStore.getState().currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT) {
        return redirect(path);
    }

    return null;
};

export default redirectOutsideDevelopmentEnvironment;
