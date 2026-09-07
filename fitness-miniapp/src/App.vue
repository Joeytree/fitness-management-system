<script>
export default {
  onLaunch() {
    console.log('多功能健身小程序启动')
  }
}
</script>

<style>
/* 全局设计 token（薄荷绿清新系） */
page {
  --primary: #10b981;
  --primary-dark: #0ea371;
  --grad: linear-gradient(135deg, #34d399, #10b981);
  --bg: #f6faf8;
  --card: #ffffff;
  --text-1: #1f2937;
  --text-2: #6b7280;
  --text-3: #9ca3af;
  --warn: #f59e0b;
  --danger: #ef4444;
  --info: #3b82f6;
  --r-sm: 10rpx;
  --r-md: 14rpx;
  --r-lg: 18rpx;
  --r-xl: 22rpx;
  background-color: var(--bg);
  color: var(--text-1);
  font-size: 28rpx;
}

/* 通用工具类 */
.page-pad {
  padding: 24rpx;
}
.card {
  background: var(--card);
  border-radius: 28rpx;
  padding: 28rpx;
  box-shadow: 0 6rpx 24rpx rgba(16, 185, 129, 0.06);
  margin-bottom: 24rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--text-1);
  margin: 28rpx 0 20rpx;
}
.muted {
  color: var(--text-2);
  font-size: 24rpx;
}
.pill {
  display: inline-block;
  padding: 6rpx 20rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  background: #ecfdf5;
  color: var(--primary-dark);
}
.btn-primary {
  background: var(--grad);
  color: #fff;
  border-radius: 44rpx;
  font-weight: 600;
}
</style>
