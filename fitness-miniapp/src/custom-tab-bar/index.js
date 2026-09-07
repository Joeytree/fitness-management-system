Component({
  data: {
    selected: 0,
    tabs: [
      { path: '/pages/home/index', text: '首页', icon: '🏠' },
      { path: '/pages/course/index', text: '课程', icon: '💪' },
      { path: '/pages/community/index', text: '社区', icon: '🌱' },
      { path: '/pages/mine/index', text: '我的', icon: '👤' }
    ]
  },
  methods: {
    switchTab(e) {
      const index = e.currentTarget.dataset.index
      const tab = this.data.tabs[index]
      wx.switchTab({ url: tab.path })
      this.setData({ selected: index })
    },
    init(index) {
      this.setData({ selected: index })
    }
  }
})
