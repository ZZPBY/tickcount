# TickCount

[![Build](https://github.com/ZZPBY/tickcount/actions/workflows/android.yml/badge.svg)](https://github.com/ZZPBY/tickcount/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

一个极简、纯离线的安卓倒数日日历。在日历上点一天、起个名字，就能看到距离（或已经过去）
那一天还有多久。

## 功能

- **侧边栏导航** —— 左上角的菜单按钮打开一个占半个屏幕的侧边栏，里面是主页、日历、
  外观与显示、语言、更新日志和项目介绍。
- **倒数日列表** —— 主页一个倒数日一张圆角卡片：名字、标签、日期，右侧还剩多少天。
- **置顶** —— 倒数日详情里打开「置顶」，它就会一直排在列表最上面，不受排序方式影响。
- **排序** —— 列表右上角的排序按钮，可以按**设定时间升序**、**设定时间降序**或
  **添加时间（新的在前）**排列，选择会记住。
- **搜索** —— 列表上方一个搜索框，框内右侧可选按**名字**（含标签）、**日期**或**时间**筛选。
- **精确到分钟** —— 每个倒数日可以是「全天」，也可以指定到具体某时某分。
- **多标签** —— 一个倒数日可以加任意多个标签。
- **自定义颜色** —— 每个倒数日可以有自己的颜色，在编辑窗口里用色相条调；不指定就用主题色。
- **事件详细** —— 点一张卡片进入它自己的画面：年月日时分秒的完整拆分，加上日期、
  时间和标签。
- **日历** —— 手写月历，一天有多个倒数日时最多显示三个圆点。点一天，那天的倒数日就
  列在月历下方，同样能搜索和排序；右上角的「+」在那天新建。
- **快速翻月** —— 点月份标题会弹出年月选择窗，带「±1 年」和「±10 年」按钮。
- **删除前会确认** —— 点「删除」不会立刻生效，先问一次。
- **桌面小组件** —— 把某一天放到桌面上，时分秒实时跳动，随尺寸自动换排版，依然是零权限。
- **主题色** —— 白、黑、蓝、绿四种预设，也能用色相条自己调一个。不跟随系统深色开关，
  也不从系统壁纸取色，选了就固定；配色取自 Tailwind CSS。
- **中英双语** —— 跟随系统语言，也可以在侧边栏里手动选中文或 English；日期格式也一并切换。
- **不要任何权限、不联网、无统计。**
- 单模块，不含任何第三方库，`minSdk` 27（Android 8.1）。

## 倒计时的读法

目标是**所选日期当地时间的 00:00**。这个时刻过去之后，同一套算法反向运行，
标签从「还有」变成「已过去」。

还没数到的部分，前面为 0 的位会用短横线占位，所以一眼就能看出还剩多久：

| 剩余时间 | 显示 |
|---|---|
| 4 个月 11 天 6 时 30 分 15 秒 | `----年04月11日 06时30分15秒` |
| 2 天 30 秒 | `----年--月02日 00时00分30秒` |
| 3 年 4 个月 11 天 6 时 30 分 15 秒 | `0003年04月11日 06时30分15秒` |

年、月、天按日历算，小时、分、秒按流逝时间算，所以夏令时切换的那一天仍然算一整天，
哪怕它实际只有 23 或 25 小时。

## 桌面小组件

长按桌面空白处 → 添加小组件 → 选「TickCount」，再挑一个倒数日。默认 4×2，拖大拖小
会自己换排版：拖扁了是「名字 + 天数 + 时钟」一行，拖大了再加上目标日期，并把天数
拆成「1年1个月4天」。

秒针由系统自己走，不额外唤醒应用，**也不需要任何权限**。

## 下载

APK 发布在 [Releases](../../releases/latest)，也可以从 **Actions** 标签页里
任意一次成功的构建中下载产物。每个版本改了什么见 [`更新日志.txt`](更新日志.txt)。


## 编译

需要：

- JDK 17
- Android SDK Platform 37 和 Build Tools 36.0.0
- 在 `local.properties` 里写好 SDK 路径，或导出 `ANDROID_HOME`

```bash
./gradlew testDebugUnitTest   # 单元测试
./gradlew assembleRelease     # APK -> app/build/outputs/apk/release/
```

Windows 下使用 `gradlew.bat`。

## 技术栈

| | |
|---|---|
| 语言 | Kotlin 2.4.20 |
| UI | Jetpack Compose + Material 3（Compose BOM 2026.09.00） |
| 构建 | AGP 9.4.1、Gradle 9.6.0，版本集中在 `gradle/libs.versions.toml` |
| SDK | compileSdk 37、targetSdk 36、minSdk 27 |
| 存储 | `SharedPreferences` 里存一个小 JSON 文档 |
| 依赖 | 仅 AndroidX 与 Compose |

`minSdk` 为 27。`java.time` 在 API 26 就有了，主题里的 `windowLightNavigationBar`
在 27 也有了，所以既不需要 `coreLibraryDesugaring`，也不需要任何兼容垫片，
更不必把主题按版本拆成两份。

## 目录结构

```
app/src/main/java/io/github/zzpby/tickcount/
├── MainActivity.kt              边到边宿主 + 应用主题（含状态栏图标配色）
├── data/                        模型与持久化
│   ├── CountdownEvent.kt        倒数日模型：id / 日期 / 时间 / 标签 / 颜色 / 置顶
│   ├── EventStore.kt            倒数日的 JSON 存储，含旧格式迁移
│   └── SettingsStore.kt         主题、排序与语言选择的存储
├── domain/                      纯逻辑，有单元测试
│   ├── Countdown.kt             年/月/天/时/分/秒 的拆解与占位格式化
│   ├── CalendarMath.kt          月历网格构造
│   ├── CountdownList.kt         列表排序、天数距离与「已过去」判定
│   ├── Search.kt                搜索范围与匹配规则
│   ├── Changelog.kt             把更新日志拆成一个个版本
│   ├── Readme.kt                从 README 里抽取项目介绍的章节
│   └── WidgetCountdown.kt       小组件的天数拆分与形态判定
└── ui/
    ├── MainViewModel.kt         状态 + 全进程唯一的那个时钟
    ├── TickCountApp.kt          根画面：抽屉、屏幕切换、返回键、各弹窗
    ├── AppDrawer.kt             侧边抽屉
    ├── AppTitleBar.kt           标题栏：菜单 / 返回 + 名字 + 动作槽
    ├── HomeScreen.kt            倒数日列表
    ├── CalendarScreen.kt        月历 + 选中那天的倒数日
    ├── EventCard.kt             倒数日卡片，主页与日历共用
    ├── SearchField.kt           搜索框与范围选择
    ├── EventDetailScreen.kt     单个倒数日的详细画面
    ├── EventEditorDialog.kt     新建 / 编辑：名字、标签、日期、时间、颜色
    ├── DeleteConfirmDialog.kt   删除前的确认弹窗
    ├── AppearanceScreen.kt      外观与显示：四个主题预设 + 自定义色相
    ├── ChangelogScreen.kt       更新日志，正文取自本仓库的同名文件
    ├── ProjectIntroScreen.kt    项目介绍，正文抽自本文件
    ├── calendar/                手写月历 + 年月选择窗
    ├── components/              Canvas 图标、色相条、Markdown 渲染、本地化格式
    ├── widget/                  桌面小组件：渲染、接收器、配置界面
    └── theme/                   Material 3 配色（Tailwind 色值）与字体
```

倒计时算法、列表排序、搜索匹配、README 抽取与 Markdown 解析都由 `app/src/test/`
下的单元测试覆盖，包括月长、闰日、跨年、两种夏令时切换，以及「今天不算已过去」
这个容易差一天的边界。

## 应用图标

![图标预览](.tools/图标生成/icon-preview.png)

矢量 XML 无法绘制汉字，因此启动器文字由
[`.tools/图标生成/IconGen.java`](.tools/图标生成/IconGen.java) 栅格化：
它加载微软雅黑，把文字缩放到正好落在 Android 自适应图标的 66dp 安全圆内，
再输出 `res/drawable-xxxhdpi/` 中的 PNG。

```bash
java .tools/图标生成/IconGen.java app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png
```

## 隐私

TickCount 只在自己私有的 `SharedPreferences` 中保存一份 `(日期, 名称, 颜色)` 列表，
别无其他。颜色是每个倒数日自己的颜色，用来在日历和列表里区分它们。桌面小组件另有一份
`tickcount_widgets`，只记录「哪个小组件显示哪一天」；主题选择存在 `tickcount_settings`。
三份都在应用私有目录里，都不离开设备。
它不声明任何权限，不发起网络请求，不含统计与广告。关于框里有一个指向本仓库的链接，
点击时交给浏览器打开——TickCount 自己没有联网能力，也没有 `INTERNET` 权限。
该列表仅在 Android 自身的云备份开启时才会离开设备，而这由 `res/xml/backup_rules.xml` 决定。

## 许可

[MIT](LICENSE)。
