# 普中摄影社 App

普陀中学摄影社官方安卓 App（原生 Kotlin + Jetpack Compose）。

## 下载

最新正式版安装包：[pcpths-android.apk](https://github.com/KylinLe/pcpths-android/releases/latest/download/pcpths-android.apk)

## 特性

- **内容自动同步官网**：每次启动抓取一次官网内容，之后全程离线可用；手动刷新或重启才会重新联网。官网新增栏目、通知、计划说明页，App 自动适配。
- **底部胶囊导航**：栏目标签自动来自官网导航，底栏放不下可左右滑动。
- **计划说明页**：进入详情后可一键返回进入前所在的分区（系统返回键同样）。
- **通知条**：黑底通知条支持逐条关闭、铃铛一键重显；指向 App 不展示内容的通知自动用浏览器打开官网。
- **深色模式**：跟随系统，黑白反相设计。
- **设计语言与官网一致**：细线 / 黑白 / 字距 / 六边形光圈，动画缓动与官网同款。

## 技术栈

Kotlin · Jetpack Compose (Material3) · Jsoup · OkHttp · minSdk 24 / targetSdk 36

无本地数据库：官网页面原文镜像缓存到应用私有目录，启动时解析进内存。

## 构建

需要 JDK 17（Gradle 已随仓库附 Wrapper）：

```bash
./gradlew :app:assembleDebug        # 调试版
./gradlew :app:assembleRelease      # 正式版（需要 keystore.properties，见 app/build.gradle.kts）
```

## 相关项目

- 官网：[KylinLe/pcpths](https://github.com/KylinLe/pcpths)（[kylinle.github.io/pcpths](https://kylinle.github.io/pcpths/)）
