import config from './config'

// 统一文件上传：选择文件 → 上传到后端 /upload → 返回相对 url
// 消除 publish / course-edit / action-edit / profile 等处的重复 uni.uploadFile 逻辑

/**
 * 上传单个本地文件，返回后端相对 url（如 /upload/xxx.png）
 * @param {string} filePath 本地临时文件路径
 * @returns {Promise<string>}
 */
export function uploadFile(filePath) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: config.BASE_URL + '/upload',
      filePath,
      name: 'file',
      header: { 'Authorization': 'Bearer ' + (uni.getStorageSync('token') || '') },
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.code === 0 && data.data && data.data.url) {
            resolve(data.data.url)
          } else {
            uni.showToast({ title: (data && data.message) || '上传失败', icon: 'none' })
            reject(new Error((data && data.message) || '上传失败'))
          }
        } catch (e) {
          uni.showToast({ title: '上传失败', icon: 'none' })
          reject(e)
        }
      },
      fail: () => {
        uni.showToast({ title: '上传失败', icon: 'none' })
        reject(new Error('上传失败'))
      }
    })
  })
}

/**
 * 选择并上传单张图片，返回相对 url
 * @param {number} [count=1] 可选图片数量
 * @returns {Promise<string>} 第一张图片的相对 url
 */
export function chooseAndUploadImage(count = 1) {
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count,
      success: (res) => {
        const path = res.tempFilePaths && res.tempFilePaths[0]
        if (!path) return reject(new Error('未选择图片'))
        uploadFile(path).then(resolve).catch(reject)
      },
      fail: (err) => reject(err)
    })
  })
}

export default { uploadFile, chooseAndUploadImage }
