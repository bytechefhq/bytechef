import {TestWebSearchProviderMutation} from '@/shared/middleware/graphql';
import {twMerge} from 'tailwind-merge';

type WebSearchProviderTestResultType = TestWebSearchProviderMutation['testWebSearchProvider'];

const WebSearchProviderTestResult = ({result}: {result?: WebSearchProviderTestResultType}) => {
    if (!result) {
        return null;
    }

    return (
        <p
            className={twMerge('text-sm', result.success ? 'text-content-success-primary' : 'text-destructive')}
            role="status"
        >
            {result.success ? 'The test search worked.' : result.errorMessage || 'The test search failed.'}
        </p>
    );
};

export default WebSearchProviderTestResult;
