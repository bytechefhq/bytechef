/**
 * Moved to the shared asset-file viewer module so the Files page and the AI Hub resource panel render files
 * identically; this re-export keeps existing AI Hub import sites stable.
 */
export {default} from '@/shared/components/asset-file-viewer/useAssetFileContent';
