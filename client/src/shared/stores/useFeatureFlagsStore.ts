export const useFeatureFlagsStore = (name: string) => {
    return import.meta.env[name];
};
