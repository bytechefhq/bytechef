import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import useConnectedUserSheetStore from '@/ee/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import {ReactNode} from 'react';
import {useNavigate} from 'react-router-dom';

interface ConnectedUserSheetEmptyStatePageLinkI {
    label: string;
    to: string;
}

interface ConnectedUserSheetEmptyStateProps {
    description: string;
    icon: ReactNode;
    nothingToProvision: boolean;
    nothingToProvisionDescription: string;
    onProvision: () => void;
    pageLink: ConnectedUserSheetEmptyStatePageLinkI;
    provisionLabel: string;
    title: string;
}

const ConnectedUserSheetEmptyState = ({
    description,
    icon,
    nothingToProvision,
    nothingToProvisionDescription,
    onProvision,
    pageLink,
    provisionLabel,
    title,
}: ConnectedUserSheetEmptyStateProps) => {
    const setConnectedUserSheetOpen = useConnectedUserSheetStore((state) => state.setConnectedUserSheetOpen);

    const navigate = useNavigate();

    const handlePageLinkClick = () => {
        setConnectedUserSheetOpen(false);

        void navigate(pageLink.to);
    };

    return (
        <EmptyState
            actions={
                nothingToProvision ? (
                    <Button label={pageLink.label} onClick={handlePageLinkClick} />
                ) : (
                    <Button label={provisionLabel} onClick={onProvision} />
                )
            }
            description={nothingToProvision ? nothingToProvisionDescription : description}
            icon={icon}
            title={title}
        />
    );
};

export default ConnectedUserSheetEmptyState;
