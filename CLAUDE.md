# 普陀中学摄影社安卓 App（pcpths_Android_Dev）

原生安卓 App，应用名「普中摄影社」。内容从官网（https://kylinle.github.io/pcpths/）同步并翻译成原生界面。
官网文件夹 `officialwebsite/` 是只读参考材料（自带 .git，不在此仓库管理）。

## 发布信息

- GitHub 仓库：**KylinLe/pcpths-android**（public，main 分支）
- 正式版：GitHub Releases（v1.0.0 起），安装包固定名 `pcpths-android.apk`
- 官方下载链接（永远指向最新版）：https://github.com/KylinLe/pcpths-android/releases/latest/download/pcpths-android.apk
- **签名密钥**：`keystore/pcpths-release.jks`（密码在 `keystore.properties`，两者均已加入 .gitignore，**切勿删除/外泄**——丢失后无法再更新 App）。Java 17 的 PKCS12 密钥库不支持存储/密钥分开设密码，两者必须相同。
- 应用图标：由 `logo/AppLogo.png`（相机图形设计稿）生成，脚本思路见图标生成历史；改 logo 后需重新生成 mipmap-*/ic_launcher*.png 全套。
- 发布流程：改代码 → 构建 → `./gradlew :app:assembleRelease` → `gh release create vX.Y.Z app/build/outputs/apk/release/app-release.apk#pcpths-android.apk --repo KylinLe/pcpths-android`

## 与用户协作的方式（重要）

- 用户不是程序员，需求可能模糊或含错误。以产品经理思维工作：先理解真实需求，主动给建议并说明理由，用大白话解释技术决策，不要把专业选项清单抛给用户。
- 范围红线：只允许修改本仓库；`officialwebsite/` 只读；绝不触碰用户的其他项目（如 KylinLe/cp 上课平台）。
- 所有 git/GitHub 操作由 Claude 代劳；用户只负责描述需求和确认结果。

## 产品需求（用户原话的整理）

1. 真正的原生 App（不要网页套壳）。
2. 主页：底部胶囊型底栏 + 分区；底栏放不下可左右滑动；标签自动来自官网栏目（新增栏目 → 同步后自动多一个标签）。
3. 打开的计划说明页有返回按钮，回到进入前所在的分区（早期版本是回首页，用户后来改为回来源分区，所有新栏目/页面同样适用）；系统返回键同样。
4. 内容跟随官网：每次启动同步一次 → 之后全部走本地缓存；手动点刷新或重启才重新联网。官网新增栏目/通知/计划说明页自动适配。
5. 不展示官网的「APP 下载」板块和「上课平台」入口。
6. 术语：plan/ 下的页面是"计划说明页"，不是"文章"。
7. 与官网 AI 的联动约定已发给用户（官网新组件类型须报告用户 → 转告本 App 升级）。

## 技术栈

- Kotlin + Jetpack Compose（Material3），AGP 8.9.2，Kotlin 2.1.20，Gradle 8.12.1
- compileSdk/targetSdk 36，minSdk 24，包名 com.pcpths.app
- 依赖：Jsoup（网页解析）、OkHttp（抓取）、Compose BOM 2025.05.00
- 无本地数据库：缓存 = filesDir/site/ 下的官网页面原文（镜像），启动时解析进内存

## 架构

```
app/src/main/java/com/pcpths/app/
├── MainActivity.kt            # 入口
├── data/
│   ├── Model.kt               # 内容数据模型（栏目/块/通知/说明页）
│   ├── Parser.kt              # 官网 HTML → 原生数据（Jsoup，只认官网既有组件）
│   ├── SiteSync.kt            # 同步器：启动/刷新时抓取官网整站小页面到 filesDir/site/
│   └── AppRepository.kt       # 状态中枢：缓存加载、同步、通知关闭记忆(SharedPreferences)
└── ui/
    ├── Theme.kt               # 设计变量 C.*（与官网 CSS 变量一一对应：细线/黑白/字距）
    ├── App.kt                 # 主界面：通知条 + 分区内容 + 详情页 + 底栏
    ├── BottomBar.kt           # 底部胶囊底栏（LazyRow 可滑动 + 铃铛 + 刷新）
    ├── HomeTab.kt             # 首页「留住光」+ 六边形光圈原生重绘(Canvas)
    ├── SectionTab.kt          # 栏目分区渲染（列表/课程卡片/岗位表→卡片堆叠）
    ├── DetailPage.kt          # 计划说明页（顶部返回按钮）
    └── NoticeStrip.kt         # 黑底通知条
```

## 关键规则

- 设计沿用官网规范：1dp 浅灰细线分隔；纯黑仅三处强调（列表顶部起始线、按钮描边、胶囊选中态）；颜色用 LocalAppColors 令牌；字距强调。
- 深色模式（跟随系统，isSystemInDarkTheme）：官网黑白系统的"反相"——黑底白字、通知条白底黑字、强调线变白、胶囊选中态白色；首页光圈带呼吸光晕（Theme.kt 两套 AppColors）。values-night/themes.xml 控制状态栏深色。
- 动画语言（官网同款缓动 SiteEase = cubic-bezier(.22,.61,.24,1)）：首页光圈随滚动旋转 min(px*0.06,36°)（官网对焦感）；通知条滑入/退出 + 铃铛重播（replayKey）；分区切换交叉淡入；详情页右滑入/滑出；分区条目依次浮现（Reveal，前 8 条错峰）。
- 下载板块的排除方式：按内容判断（栏目内含 .dl-opts/.dl-btn 即跳过），不依赖 id。
- 底栏标签名：优先官网导航链接文字，缺省用栏目 h2 文字。
- 通知点击规则（App.kt handleNoticeTap）：锚点指向 App 内栏目 → 切标签；指向 App 不展示的栏目（如 #app 下载板块）→ 默认浏览器打开官网对应位置；站内页面 → App 内打开；外部链接 → 浏览器。
- 同步只抓同站点 .html（两层链接），失败不影响已有缓存。
- 测试：ParserTest 用 officialwebsite 快照（app/src/test/resources/fixtures/），官网大改后需手动更新快照。

## 构建与运行

工具都在项目内 tooling/（JDK 17、Gradle 8.12.1），不动系统：

```bash
export JAVA_HOME="$PWD/tooling/jdk-17.0.21+9/Contents/Home"
tooling/gradle-8.12.1/bin/gradle :app:assembleDebug        # 出 APK：app/build/outputs/apk/debug/app-debug.apk
tooling/gradle-8.12.1/bin/gradle :app:assembleRelease      # 正式版（签名见「发布信息」）
tooling/gradle-8.12.1/bin/gradle :app:testDebugUnitTest   # 跑解析测试
```

仓库已附 Gradle Wrapper（`./gradlew`，构建说明见 README）。

模拟器（官方，Apple Silicon）：avd 名 pcpths；system-images;android-36;google_apis;arm64-v8a。
启动：`~/Library/Android/sdk/emulator/emulator -avd pcpths -no-snapshot -no-audio`
adb 在 `~/Library/Android/sdk/platform-tools/adb`。

## 待办

- [x] 发布：GitHub 仓库（KylinLe/pcpths-android）+ 签名 release APK v1.0.0（2026-11-13）
- [ ] 官网「APP 下载」板块的安卓按钮指向本仓库下载链接——由官网智能体改，提示词已发给用户
- [ ] 应用图标已用 logo/AppLogo.png 生成；若 logo 改版，需重新生成图标并升级版本
