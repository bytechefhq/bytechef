import {AppEventModel} from '@/middleware/embedded/configuration';
import AppEventListItem from '@/pages/embedded/app-events/components/AppEventListItem';

const AppEventList = ({appEvents}: {appEvents: AppEventModel[]}) => {
    return (
        <ul className="w-full divide-y divide-gray-100 px-2 xl:mx-auto 2xl:w-4/5" role="list">
            {appEvents.map((appEvent) => {
                return <AppEventListItem appEvent={appEvent} key={appEvent.id} />;
            })}
        </ul>
    );
};

export default AppEventList;
