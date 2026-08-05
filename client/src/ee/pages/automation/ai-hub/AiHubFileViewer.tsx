/**
 * The file viewer moved to the shared asset-file viewer module so the Files page detail sheet and the AI Hub
 * resource panel render files identically (format-aware chart pane, interactive HTML, CSV table, binary
 * fallbacks). This re-export keeps existing AI Hub import sites stable; `AiHubViewModeType` from the tabs store
 * is structurally identical to the shared `AssetFileViewerModeType`.
 */
export {default} from '@/shared/components/asset-file-viewer/AssetFileViewer';
