# TickCount

一个极简、纯离线的安卓小应用，只回答一个问题：**距离（或已经过去）某一天，还有多久？**

在日历上点一天、起个名字，屏幕上方就会把这段距离拆成
**年 / 月 / 天 / 小时 / 分钟 / 秒**，并实时跳动。已经过去的日子会改成「已过去」往上数，
所以生日、纪念日、考试、旅行都能用。

*[English](README.md) | 中文*

> 装到手机上后，中文系统的应用名显示为 **倒数日**，其他语言显示 **TickCount** ——
> 和图标上的「倒数日」保持一致。

---

## 功能

- **倒数到任意一天** —— 点日历上的一天，起个名字，完成。
- **单行定宽显示** —— `yyyy年MM月dd日 HH时mm分ss秒`，实时跳动。数值为 0 的位会用
  **与格式字母等宽的短横线**占位，所以不足一年的倒计时显示
  `----年04月11日 06时30分15秒`，而不是 `00年…`。用等宽字体是为了让每一位
  宽度恒定，秒数跳动时整行不会左右抖动或重新排版。
- **过去的日子往上数** —— 同一行，标签换成「已过去」。
- **可以有多个倒数日** —— 每个命名的日子在日历上有一个彩色小圆点，一眼看清这个月有什么。
- **快速跳转** —— 点日历上的「2026年9月」会弹出年月选择窗，带「±1 年」和「±10 年」按钮，
  不用一个月一个月地点。
- **没设过的日子显示空白格式行** —— `----年--月--日 --时--分--秒`。
  倒数日是你自己创建的，App 不会替你凭空算一个。
- **Material 3** —— 亮色 / 暗色，Android 12+ 支持跟随壁纸取色。
- **中英双语** —— 跟随系统语言；日期格式也本地化（`2027年2月6日 星期六` / `Saturday, February 6, 2027`）。
- **不要任何权限、不联网、无统计** —— 它连申请联网的能力都没有。
- **体积小** —— 单模块，不含任何第三方 UI 或日历库，minSdk 29。

## 到底在数什么

倒计时的目标是**所选日期当地时间的 00:00** —— 只有这样「距离 6 号还有多久」才是没有歧义的。
这个午夜一旦过去，同一套算法就反过来跑，标签从「还有」变成「已过去」，
于是倒数日不用任何特殊处理就变成了纪念日。

年、月、天用的是**日历**运算（一个月就是一个自然月，不管它有多少天），
小时、分、秒用的是流逝时间。这是「1 年 2 个月 5 天」唯一能有意义的前提，
也是这里用 `ChronoUnit` 而不是拿毫秒总数去除的原因。
夏令时切换的那一天仍然算作一天，哪怕它实际只有 23 或 25 小时。

实现见 [`Countdown.kt`](app/src/main/java/io/github/zzpby/tickcount/domain/Countdown.kt)。

## 下载

从 [最新 Release](../../releases/latest) 下载 APK，或者在 **Actions** 里任意一次
成功的构建中下载产物（Artifacts）。

在手机上打开文件安装即可（需要允许浏览器或文件管理器「安装未知来源应用」）。

> **签名说明。** CI 发布的版本使用 Android 标准的 **debug 密钥**签名，因为仓库里
> 没有私钥。APK 是经过压缩混淆、不可调试的，正常安装没有问题，但**不适合上架应用商店**。
> 如果你 fork 之后想正式发布，在项目根目录建一个 `keystore.properties`（已被 `.gitignore` 忽略）：
>
> ```properties
> storeFile=release.jks
> storePassword=…
> keyAlias=…
> keyPassword=…
> ```
>
> 之后 `assembleRelease` 会自动用你的密钥。注意：换密钥意味着用户必须先卸载 debug 签名的版本。

## 自己编译

需要：

- JDK 17
- Android SDK Platform 37（`android-37`）和 Build Tools 36.0.0
- 在 `local.properties` 里写好 SDK 路径，或者导出 `ANDROID_HOME`

```bash
# 单元测试
./gradlew testDebugUnitTest

# 可安装的 APK -> app/build/outputs/apk/release/
./gradlew assembleRelease
```

Windows 下把 `./gradlew` 换成 `gradlew.bat`。

## 技术栈

| | |
|---|---|
| 语言 | Kotlin 2.4.20 |
| UI | Jetpack Compose + Material 3（Compose BOM 2026.09.00） |
| 构建 | AGP 9.4.1、Gradle 9.6.0，版本集中在 `gradle/libs.versions.toml` |
| SDK | compileSdk 37、targetSdk 36、minSdk 29 |
| 存储 | `SharedPreferences` 里存一个小 JSON 文档 |
| 依赖 | 只有 AndroidX + Compose，**没有任何第三方库** |

因为 `minSdk` 是 29，`java.time` 由系统直接提供：不需要 `coreLibraryDesugaring`，
也不需要任何兼容垫片。

### 目录结构

```
app/src/main/java/io/github/zzpby/tickcount/
├── MainActivity.kt              边到边（edge-to-edge）宿主
├── data/                        CountdownEvent 模型 + JSON 持久化
├── domain/                      纯逻辑，有单元测试
│   ├── Countdown.kt             年/月/天/时/分/秒 的拆解
│   └── CalendarMath.kt          月历网格构造
└── ui/
    ├── MainViewModel.kt         状态 + 全进程唯一的那个时钟
    ├── TickCountScreen.kt       唯一的界面
    ├── calendar/                手写月历 + 年月选择窗
    ├── countdown/               顶部倒计时
    └── theme/                   Material 3 配色与字体
```

### 应用图标

图标是白底黑色「倒数日」：

![图标预览](tools/icon-preview.png)

矢量 XML 画不了汉字，所以文字是用 [`tools/IconGen.java`](tools/IconGen.java) 栅格化的 ——
一个 JDK 小工具，加载微软雅黑、把文字缩放到正好落在 Android 自适应图标的 66dp 安全圆内，
再输出 `res/drawable-xxxhdpi/` 里的 PNG。想换文字就重新跑一次：

```bash
java tools/IconGen.java app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png
```

## 隐私

TickCount 只在自己私有的 `SharedPreferences` 里存一份 `(日期, 名称)` 列表，别的什么都没有。
它**不声明任何权限**，不发起任何网络请求，没有统计、没有广告。
唯一可能离开设备的就是这份列表本身 —— 前提是你自己开启了安卓的云备份，
而这由 `res/xml/backup_rules.xml` 决定。

## 许可

[MIT](LICENSE)，随便用。
