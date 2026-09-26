# TickCount

[![Build](https://github.com/ZZPBY/tickcount/actions/workflows/android.yml/badge.svg)](https://github.com/ZZPBY/tickcount/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

一个极简、纯离线的安卓倒数日日历。在日历上点一天、起个名字，就能看到距离（或已经过去）
那一天还有多久。

## 功能

- **倒数到任意一天** —— 点日历上的一天，起个名字。
- **单行定宽显示** —— `yyyy年MM月dd日 HH时mm分ss秒`，每秒跳动。
- **过去的日子往上数** —— 同一行，标签换成「已过去」。
- **支持多个倒数日** —— 每个命名的日子在日历上有一个彩色小圆点，一眼看清这个月有什么。
- **快速翻月** —— 点月份标题会弹出年月选择窗，带「±1 年」和「±10 年」按钮。
- **Material 3** —— 亮色 / 暗色，Android 12+ 支持跟随壁纸取色。
- **中英双语** —— 跟随系统语言，日期格式也本地化。
- **不要任何权限、不联网、无统计。**
- 单模块，不含任何第三方库，`minSdk` 29。

## 倒计时的读法

目标是**所选日期当地时间的 00:00**。这个时刻过去之后，同一套算法反向运行，
标签从「还有」变成「已过去」。

仍然处于**高位**（自身为 0，且所有更高位也为 0）的字段，会用与格式字母等宽的短横线占位；
而夹在计数单位中间的 0 保留为真实的数字，因此正常倒数时不会闪出短横线：

| 剩余时间 | 显示 |
|---|---|
| 4 个月 11 天 6 时 30 分 15 秒 | `----年04月11日 06时30分15秒` |
| 2 天 30 秒 | `----年--月02日 00时00分30秒` |
| 3 年 4 个月 | `0003年04月11日 06时30分15秒` |
| 未设置倒数日 | `----年--月--日 --时--分--秒` |

整行使用等宽字体，数字与短横线宽度完全一致，因此秒数跳动时不会左右抖动。
字号在运行时实测并缩放至可用宽度，保证在任何机型和字体缩放下都保持单行。

年、月、天使用日历运算，小时、分、秒使用流逝时间。因此夏令时切换的那一天
仍然算作一天，哪怕它实际只有 23 或 25 小时。实现见
[`Countdown.kt`](app/src/main/java/io/github/zzpby/tickcount/domain/Countdown.kt)。

## 下载

APK 发布在 [Releases](../../releases/latest)，也可以从 **Actions** 标签页里
任意一次成功的构建中下载产物。

配置了签名 Secret 时，Release APK 使用维护者的正式密钥签名；未配置时 CI 回退到
Android debug 密钥，并会在日志里给出警告。debug 密钥签的包可以正常安装，
但**无法覆盖安装**已用正式密钥签名的版本，也不适合上架应用商店。
用自己的密钥构建见[签名](#签名)。

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
| SDK | compileSdk 37、targetSdk 36、minSdk 29 |
| 存储 | `SharedPreferences` 里存一个小 JSON 文档 |
| 依赖 | 仅 AndroidX 与 Compose |

`minSdk` 为 29，`java.time` 由系统直接提供，因此不需要 `coreLibraryDesugaring`，
也不需要任何兼容垫片。

## 目录结构

```
app/src/main/java/io/github/zzpby/tickcount/
├── MainActivity.kt              边到边（edge-to-edge）宿主
├── data/                        CountdownEvent 模型 + JSON 持久化
├── domain/                      纯逻辑，有单元测试
│   ├── Countdown.kt             年/月/天/时/分/秒 的拆解与占位格式化
│   └── CalendarMath.kt          月历网格构造
└── ui/
    ├── MainViewModel.kt         状态 + 全进程唯一的那个时钟
    ├── TickCountScreen.kt       唯一的界面
    ├── calendar/                手写月历 + 年月选择窗
    ├── countdown/               顶部倒计时
    └── theme/                   Material 3 配色与字体
```

倒计时算法由 `app/src/test/` 下的单元测试覆盖，包括月长、闰日、跨年以及
两种夏令时切换。

## 应用图标

![图标预览](tools/icon-preview.png)

矢量 XML 无法绘制汉字，因此启动器文字由 [`tools/IconGen.java`](tools/IconGen.java)
栅格化：它加载微软雅黑，把文字缩放到正好落在 Android 自适应图标的 66dp 安全圆内，
再输出 `res/drawable-xxxhdpi/` 中的 PNG。

```bash
java tools/IconGen.java app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png
```

## 隐私

TickCount 只在自己私有的 `SharedPreferences` 中保存一份 `(日期, 名称)` 列表，别无其他。
它不声明任何权限，不发起网络请求，不含统计与广告。该列表仅在 Android 自身的云备份
开启时才会离开设备，而这由 `res/xml/backup_rules.xml` 决定。

## 许可

[MIT](LICENSE)。
