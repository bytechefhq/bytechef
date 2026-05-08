import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAiEvalRulesQuery, useAiEvalScoreConfigsQuery} from '@/shared/middleware/graphql';
import {PlusIcon, StarIcon} from 'lucide-react';
import {useState} from 'react';

import {AiEvalScoreConfigType} from '../../types';
import AiEvalRuleDialog from './AiEvalRuleDialog';
import AiEvalRules from './AiEvalRules';
import AiEvalScoreAnalytics from './AiEvalScoreAnalytics';
import AiEvalScoreConfigDialog from './AiEvalScoreConfigDialog';

type ScoresTabType = 'analytics' | 'configs' | 'rules';

const AiEvalScores = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [activeTab, setActiveTab] = useState<ScoresTabType>('configs');
    const [showConfigDialog, setShowConfigDialog] = useState(false);
    const [showEvalRuleDialog, setShowEvalRuleDialog] = useState(false);
    const [editingConfig, setEditingConfig] = useState<AiEvalScoreConfigType | undefined>();

    const {data: scoreConfigsData, isLoading: scoreConfigsIsLoading} = useAiEvalScoreConfigsQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const {data: evalRulesData, isLoading: evalRulesIsLoading} = useAiEvalRulesQuery({
        workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
    });

    const scoreConfigs = (scoreConfigsData?.aiEvalScoreConfigs ?? []).filter(
        (config): config is NonNullable<typeof config> => config != null
    );
    const evalRules = (evalRulesData?.aiEvalRules ?? []).filter(
        (rule): rule is NonNullable<typeof rule> => rule != null
    );

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <div className="mb-4 flex items-center justify-between">
                <h2 className="text-lg font-semibold">Scores</h2>

                <div className="flex gap-2">
                    {activeTab === 'configs' && (
                        <button
                            className="flex items-center gap-1 rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/90"
                            onClick={() => {
                                setEditingConfig(undefined);
                                setShowConfigDialog(true);
                            }}
                        >
                            <PlusIcon className="size-4" />
                            New Score Config
                        </button>
                    )}

                    {activeTab === 'rules' && (
                        <button
                            className="flex items-center gap-1 rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/90"
                            onClick={() => setShowEvalRuleDialog(true)}
                        >
                            <PlusIcon className="size-4" />
                            New Eval Rule
                        </button>
                    )}
                </div>
            </div>

            <div className="mb-4 flex gap-1 border-b">
                {(['configs', 'rules', 'analytics'] as ScoresTabType[]).map((tab) => (
                    <button
                        className={`px-4 py-2 text-sm font-medium ${
                            activeTab === tab
                                ? 'border-b-2 border-primary text-primary'
                                : 'text-muted-foreground hover:text-foreground'
                        }`}
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                    >
                        {tab === 'configs' ? 'Score Configs' : tab === 'rules' ? 'Eval Rules' : 'Analytics'}
                    </button>
                ))}
            </div>

            {activeTab === 'configs' && (
                <>
                    {scoreConfigsIsLoading ? (
                        <PageLoader loading={true} />
                    ) : scoreConfigs.length === 0 ? (
                        <EmptyList
                            icon={<StarIcon className="size-12 text-muted-foreground" />}
                            message="Define score dimensions like relevance, helpfulness, or safety."
                            title="No Score Configs"
                        />
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm">
                                <thead>
                                    <tr className="border-b text-muted-foreground">
                                        <th className="px-3 py-2 font-medium">Name</th>

                                        <th className="px-3 py-2 font-medium">Data Type</th>

                                        <th className="px-3 py-2 font-medium">Range</th>

                                        <th className="px-3 py-2 font-medium">Description</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {scoreConfigs.map((config) => (
                                        <tr
                                            className="cursor-pointer border-b hover:bg-muted/50"
                                            key={config.id}
                                            onClick={() => {
                                                setEditingConfig(config);
                                                setShowConfigDialog(true);
                                            }}
                                        >
                                            <td className="px-3 py-2 font-medium">{config.name}</td>

                                            <td className="px-3 py-2">{config.dataType || '-'}</td>

                                            <td className="px-3 py-2">
                                                {config.minValue != null && config.maxValue != null
                                                    ? `${config.minValue} - ${config.maxValue}`
                                                    : '-'}
                                            </td>

                                            <td className="px-3 py-2 text-muted-foreground">
                                                {config.description || '-'}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </>
            )}

            {activeTab === 'rules' && <AiEvalRules evalRules={evalRules} isLoading={evalRulesIsLoading} />}

            {activeTab === 'analytics' && <AiEvalScoreAnalytics />}

            {showConfigDialog && (
                <AiEvalScoreConfigDialog
                    editingConfig={editingConfig}
                    onClose={() => {
                        setShowConfigDialog(false);
                        setEditingConfig(undefined);
                    }}
                />
            )}

            {showEvalRuleDialog && (
                <AiEvalRuleDialog onClose={() => setShowEvalRuleDialog(false)} scoreConfigs={scoreConfigs} />
            )}
        </div>
    );
};

export default AiEvalScores;
