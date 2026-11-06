import AssetFileListItem from '@/pages/automation/asset-files/components/AssetFileListItem';
import {AssetFile, Tag} from '@/shared/middleware/graphql';

interface AssetFileListPropsI {
    files: AssetFile[];
    onDelete: (fileId: string, fileName: string) => void;
    onRename: (fileId: string, fileName: string) => void;
    tags: Tag[];
}

const AssetFileList = ({files, onDelete, onRename, tags}: AssetFileListPropsI) => (
    <div className="w-full divide-y divide-border/50 px-4 3xl:mx-auto 3xl:w-4/5">
        {files.map((file) => {
            const fileTagIds = new Set((file.tags ?? []).map((tag) => tag.id));

            return (
                <AssetFileListItem
                    file={file}
                    key={file.id}
                    onDelete={onDelete}
                    onRename={onRename}
                    remainingTags={tags.filter((tag) => !fileTagIds.has(tag.id))}
                />
            );
        })}
    </div>
);

export default AssetFileList;
