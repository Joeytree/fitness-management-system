import config from './config'

// 过滤 undefined/null 参数：uni.request 对 GET data 里的 undefined 会序列化成字面量 "undefined" 传给后端
function clean(data) {
  const out = {}
  Object.keys(data || {}).forEach(k => {
    if (data[k] !== undefined && data[k] !== null && data[k] !== '') out[k] = data[k]
  })
  return out
}

function request(options) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.request({
      url: config.BASE_URL + options.url,
      method: options.method || 'GET',
      data: clean(options.data),
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : ''
      },
      success: (res) => {
        const body = res.data
        if (body && body.code === 0) {
          resolve(body.data)
        } else if (body && body.code === 401) {
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          uni.reLaunch({ url: '/pages/login/index' })
          reject(body)
        } else {
          uni.showToast({ title: (body && body.message) || '请求失败', icon: 'none' })
          reject(body)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络连接失败，请确认后端已启动', icon: 'none' })
        reject(err)
      }
    })
  })
}

export function get(url, data) {
  return request({ url, method: 'GET', data })
}

export function post(url, data) {
  return request({ url, method: 'POST', data })
}

export function put(url, data) {
  return request({ url, method: 'PUT', data })
}

export function del(url, data) {
  return request({ url, method: 'DELETE', data })
}

export default { get, post, put, del }
