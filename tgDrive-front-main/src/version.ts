// 版本配置 - 每次部署手动递增
export const APP_VERSION = '1.0.21'

const VERSION_KEY = 'tgdrive_app_version'

export function checkVersionUpdate(): boolean {
  try {
    const storedVersion = localStorage.getItem(VERSION_KEY)
    if (storedVersion && storedVersion !== APP_VERSION) {
      // 版本已更新，需要通知用户
      return true
    }
    // 首次访问或版本一致，记录当前版本
    localStorage.setItem(VERSION_KEY, APP_VERSION)
    return false
  } catch {
    return false
  }
}

export function markVersionSeen(): void {
  localStorage.setItem(VERSION_KEY, APP_VERSION)
}
