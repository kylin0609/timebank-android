# 时间银行 (Time Bank)

一个基于时间管理理念的 Android 应用，通过「时间兑换」机制帮助你更好地管理手机使用时间。

## ✨ 核心理念

- **正向应用**：使用有益的应用（如学习、阅读、健身类应用）可以积累时间
- **负向应用**：使用娱乐类应用（如游戏、短视频）需要消耗时间
- **时间兑换**：使用 1 分钟正向应用，可兑换一定时间的负向应用使用时长
- **余额管理**：当余额不足时，负向应用将被阻止启动

## 🎯 功能特性

### 核心功能
- ⏰ **每日自动重置**：每天 0 点自动重置余额为 5 分钟
- 📊 **应用分类管理**：将已安装应用分类为正向、负向或中立
- 🔄 **自定义兑换比例**：可调整正向应用与负向应用的时间兑换比例（0.1 ~ 2.0）
- 🔒 **余额不足阻止**：负向应用余额不足时显示阻止页面
- 📱 **实时监控**：后台服务实时监控应用使用情况（0.1 秒检测间隔）

### 数据管理
- 🧹 **每周数据清除**：每周只能清除一次数据，防止滥用
- 💾 **数据持久化**：使用 DataStore 保存配置和余额
- 🔐 **权限管理**：需要使用情况访问权限和悬浮窗权限

## 🎨 设计特点

- **极简主义**：去除繁琐元素，聚焦核心功能
- **超大数字**：96sp 的余额显示，一目了然
- **渐变配色**：紫蓝渐变主题，精致优雅
- **轻盈透明**：半透明卡片设计，0dp 阴影
- **Material 3**：遵循最新 Material Design 规范

## 📱 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose + Material 3
- **架构模式**：MVVM（ViewModel + Repository）
- **依赖注入**：Hilt
- **数据库**：Room
- **数据存储**：DataStore (Preferences)
- **异步处理**：Kotlin Coroutines + Flow

## 🛠️ 项目结构

```
app/
├── src/main/java/com/timebank/app/
│   ├── MainActivity.kt                    # 主界面（主页、分类、设置）
│   ├── BlockActivity.kt                   # 阻止页面
│   ├── TimeBankApplication.kt             # Application 类
│   │
│   ├── data/                              # 数据层
│   │   ├── local/
│   │   │   ├── database/                  # Room 数据库
│   │   │   │   ├── AppClassificationDao.kt
│   │   │   │   └── TimeBankDatabase.kt
│   │   │   └── datastore/                 # DataStore 配置
│   │   │       └── PreferenceKeys.kt
│   │   └── repository/                    # Repository 实现
│   │       ├── AppClassificationRepositoryImpl.kt
│   │       └── ConfigRepositoryImpl.kt
│   │
│   ├── domain/                            # 领域层
│   │   ├── model/                         # 数据模型
│   │   │   ├── AppClassification.kt
│   │   │   └── AppType.kt
│   │   └── repository/                    # Repository 接口
│   │       ├── AppClassificationRepository.kt
│   │       └── ConfigRepository.kt
│   │
│   ├── presentation/                      # 表现层
│   │   ├── ui/
│   │   │   └── classify/                  # 分类页面
│   │   │       └── ClassifyScreen.kt
│   │   └── viewmodel/                     # ViewModel
│   │       ├── ClassifyViewModel.kt
│   │       ├── HomeViewModel.kt
│   │       └── SettingsViewModel.kt
│   │
│   ├── service/                           # 服务
│   │   └── MonitorService.kt              # 监控服务
│   │
│   ├── util/                              # 工具类
│   │   └── PermissionManager.kt
│   │
│   ├── di/                                # 依赖注入模块
│   │   ├── DatabaseModule.kt
│   │   └── RepositoryModule.kt
│   │
│   └── ui/theme/                          # 主题
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
└── src/main/res/
    ├── drawable/                          # 矢量图标
    │   ├── ic_launcher_background.xml
    │   └── ic_launcher_foreground.xml
    └── mipmap-*/                          # 应用图标
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17
- Android SDK API 34
- Kotlin 1.9.0

### 构建步骤

1. **克隆仓库**
   ```bash
   git clone https://github.com/你的用户名/timebank-android.git
   cd timebank-android
   ```

2. **打开项目**
   - 使用 Android Studio 打开项目
   - 等待 Gradle 同步完成

3. **运行应用**
   - 连接 Android 设备或启动模拟器
   - 点击 Run 按钮或执行 `./gradlew installDebug`

### 权限配置

应用需要以下权限才能正常工作：

1. **使用情况访问权限**（Usage Stats）
   - 用于检测当前前台应用
   - 首次启动时会引导用户授权

2. **悬浮窗权限**（Overlay Permission）
   - 用于在阻止页面上显示内容
   - 首次启动时会引导用户授权

## 📖 使用指南

### 1. 首次使用
1. 授予「使用情况访问权限」和「悬浮窗权限」
2. 默认余额为 5 分钟（300 秒）

### 2. 应用分类
1. 进入「分类」页面
2. 点击「+」添加应用到对应分类：
   - **正向**：使用时增加余额
   - **负向**：使用时消耗余额
   - **中立**：不影响余额
3. 长按应用图标可移除分类

### 3. 调整兑换比例
1. 进入「设置」页面
2. 点击「兑换比例」设置项
3. 使用滑块调整比例（0.1 ~ 2.0）
   - 比例越大，使用正向应用获得的时间越多

### 4. 数据管理
- 每天 0 点自动重置余额为 5 分钟
- 每周只能手动清除一次数据（重置为 5 分钟）

## 🔧 配置说明

### 修改默认余额
在 `ConfigRepositoryImpl.kt` 中修改：
```kotlin
prefs[PreferenceKeys.CURRENT_BALANCE] ?: 300L  // 默认 5 分钟（秒）
```

### 修改监控间隔
在 `MonitorService.kt` 中修改：
```kotlin
delay(100)  // 100ms = 0.1 秒
```

### 修改每周清除限制
在 `SettingsViewModel.kt` 中修改：
```kotlin
val oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L  // 7 天
```

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 贡献步骤
1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📮 联系方式

如有问题或建议，欢迎通过 Issue 反馈。

## 🙏 致谢

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material 3](https://m3.material.io/)
- [Hilt](https://dagger.dev/hilt/)
- [Room](https://developer.android.com/training/data-storage/room)

---

**注意**：本应用需要 Android 5.1（API 22）及以上版本。
