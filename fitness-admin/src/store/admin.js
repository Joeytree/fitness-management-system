import { defineStore } from 'pinia'

export const useAdminStore = defineStore('admin', {
  state: () => ({
    permissions: [],
    pendingCount: 0,
    pendingDetail: {}
  }),
  getters: {
    hasAll: (state) => state.permissions.includes('all'),
    can(state) {
      return (code) => state.permissions.includes('all') || state.permissions.includes(code)
    }
  },
  actions: {
    setPermissions(perms) { this.permissions = perms || [] },
    setPendingCount(n) { this.pendingCount = n },
    setPendingDetail(d) { this.pendingDetail = d || {} }
  }
})
