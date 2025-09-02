import { defineStore } from 'pinia';

export const useAppStore = defineStore('app', {
    state: () => ({
        isLoading: false,
        error: null,
        categories: []
    }),
    actions: {
        setLoading(loading) {
            this.isLoading = loading;
        },
        setError(error) {
            this.error = error;
        },
        setCategories(categories) {
            this.categories = categories;
        }
    }
});
