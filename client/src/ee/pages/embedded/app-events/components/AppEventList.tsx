import AppEventListItem from '@/ee/pages/embedded/app-events/components/AppEventListItem';
import {AppEvent} from '@/ee/shared/middleware/embedded/configuration';

const AppEventList = ({appEvents}: {appEvents: AppEvent[]}) => {
    return (
        <ul className="w-full divide-y divide-gray-100 px-4 3xl:mx-auto 3xl:w-4/5" role="list">
            {appEvents.map((appEvent) => {
                return <AppEventListItem appEvent={appEvent} key={appEvent.id} />;
            })}
        </ul>
    );
};

export default AppEventList;
