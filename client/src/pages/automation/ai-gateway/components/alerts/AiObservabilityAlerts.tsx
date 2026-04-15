import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

import AiObservabilityAlertHistory from './AiObservabilityAlertHistory';
import AiObservabilityAlertRules from './AiObservabilityAlertRules';
import AiObservabilityNotificationChannels from './AiObservabilityNotificationChannels';

type AlertsSubTabType = 'channels' | 'history' | 'rules';

const SUB_TABS: {label: string; value: AlertsSubTabType}[] = [
    {label: 'Rules', value: 'rules'},
    {label: 'Channels', value: 'channels'},
    {label: 'History', value: 'history'},
];

const AiObservabilityAlerts = () => {
    const [activeSubTab, setActiveSubTab] = useState<AlertsSubTabType>('rules');

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4 flex gap-1 py-4">
                {SUB_TABS.map((tab) => (
                    <button
                        className={twMerge(
                            'rounded px-3 py-1 text-sm font-medium',
                            activeSubTab === tab.value
                                ? 'bg-primary text-primary-foreground'
                                : 'bg-muted text-muted-foreground hover:bg-muted/80'
                        )}
                        key={tab.value}
                        onClick={() => setActiveSubTab(tab.value)}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            {activeSubTab === 'rules' && <AiObservabilityAlertRules />}

            {activeSubTab === 'channels' && <AiObservabilityNotificationChannels />}

            {activeSubTab === 'history' && <AiObservabilityAlertHistory />}
        </div>
    );
};

export default AiObservabilityAlerts;
