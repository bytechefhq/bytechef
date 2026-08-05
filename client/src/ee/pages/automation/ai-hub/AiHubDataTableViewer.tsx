import EmbeddableDataTable from '@/pages/automation/datatable/EmbeddableDataTable';
import {ExternalLinkIcon} from 'lucide-react';
import {Link} from 'react-router-dom';

interface AiHubDataTableViewerPropsI {
    dataTableId: string;
    name: string;
}

const AiHubDataTableViewer = ({dataTableId, name}: AiHubDataTableViewerPropsI) => {
    const fullViewHref = `/automation/datatables/${dataTableId}`;

    return (
        <div className="flex size-full flex-col">
            <header className="flex shrink-0 items-center justify-between gap-2 border-b border-stroke-neutral-secondary px-4 py-3">
                <span className="truncate text-sm font-semibold text-content-neutral-primary">{name}</span>

                <Link
                    className="flex shrink-0 items-center gap-1 text-xs text-content-brand-primary hover:underline"
                    rel="noreferrer"
                    target="_blank"
                    to={fullViewHref}
                >
                    Open in full view
                    <ExternalLinkIcon className="size-3" />
                </Link>
            </header>

            <div className="min-h-0 flex-1">
                <EmbeddableDataTable dataTableId={dataTableId} />
            </div>
        </div>
    );
};

export default AiHubDataTableViewer;
