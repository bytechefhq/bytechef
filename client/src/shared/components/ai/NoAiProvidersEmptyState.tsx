import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import {AI_PROVIDERS_DOCUMENTATION_URL} from '@/pages/settings/settingsDocumentationUrls';
import {AI_PROVIDERS_LINK_LABEL} from '@/shared/constants';
import {AI_PROVIDERS_SETTINGS_PATH} from '@/shared/navigation/settingsPaths';
import {BrainCircuitIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {useNavigate} from 'react-router-dom';

interface NoAiProvidersEmptyStateProps {
    children?: ReactNode;
    className?: string;
    description: ReactNode;
}

const NoAiProvidersEmptyState = ({children, className, description}: NoAiProvidersEmptyStateProps) => {
    const navigate = useNavigate();

    return (
        <EmptyState
            actions={
                <Button label={AI_PROVIDERS_LINK_LABEL} onClick={() => void navigate(AI_PROVIDERS_SETTINGS_PATH)} />
            }
            className={className}
            description={description}
            docsLink={{href: AI_PROVIDERS_DOCUMENTATION_URL, label: 'How to set up AI providers'}}
            icon={<BrainCircuitIcon />}
            title="No AI providers enabled"
        >
            {children}
        </EmptyState>
    );
};

export default NoAiProvidersEmptyState;
