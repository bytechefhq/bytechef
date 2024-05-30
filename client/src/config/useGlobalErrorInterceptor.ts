import {useToast} from '@/components/ui/use-toast';
import fetchIntercept from 'fetch-intercept';

export default function useGlobalErrorInterceptor() {
    const {toast} = useToast();

    const unregister = fetchIntercept.register({
        response: function (response) {
            if (response.status < 200 || response.status > 299) {
                const clonedResponse = response.clone();

                clonedResponse
                    .json()
                    .then((data) => {
                        toast({
                            description: data.detail,
                            title: data.title,
                            variant: 'destructive',
                        });
                    })
                    .catch(() => {});
            }

            return response;
        },
    });

    return {unregister};
}
