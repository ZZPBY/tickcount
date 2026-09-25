# TickCount

一个极简、纯离线的安卓小应用，只回答一个问题：**距离某一天还有多少天？**

在日历上点一天、起个名字，屏幕上方就会显示它离现在还有多远 —— 一个大号天数，
加一个实时跳动的 `HH:MM:SS`。已经过去的日子会改成「已经过去多少天」，
所以生日、纪念日、考试、旅行都能用。

*[English](README.md) | 中文*

---

## 功能

- **倒数到任意一天** —— 点日历上的一天，起个名字，完成。
- **可以有多个倒数日** —— 每个命名的日子在日历上有一个彩色小圆点，一眼看清这个月有什么。
- **实时 `HH:MM:SS`** —— 每秒跳动，并且正好在天数变化的那一刻归零重来。
- **过去的日子也支持** —— 「12 天前」，并且继续往上数。
- **Material 3** —— 亮色 / 暗色，Android 12+ 支持跟随壁纸取色。
- **中英双语** —— 跟随系统语言；日期格式也是本地化的（`2027年2月6日 星期六` / `Saturday, February 6, 2027`）。
- **不要任何权限、不联网、无统计** —— 它连申请联网的能力都没有。
- **体积小** —— 单模块，不含任何第三方 UI 或日历库，minSdk 29。

## 那两个数字分别是什么

顶部刻意显示的是**两个不同的量**，因为它们回答的是两个不同的问题：

| | 含义 |
|---|---|
| **大号数字** | 今天到目标日期的**日历天数**。选一个两天后的日子，即使现在是晚上 23:00（其实只剩 25 小时），它也显示 `2`。这才是人问「还有多少天」时想要的答案。 |
| **下面的时钟** | **当前这一天**还剩多少时间（过去的日子则是已经过了多少时间）。它在午夜归零，而那一刻正好就是天数变化的那一刻，所以两个数字永远不会互相矛盾。 |

另一种做法是把它们合成一个时长 `D 天 HH:MM:SS`，但那会在重要日子前的大半天里
一直显示 `0 天`，看起来像 bug。完整推理见
[`Countdown.kt`](app/src/main/java/com/tickcount/app/domain/Countdown.kt)。

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
app/src/main/java/com/tickcount/app/
├── MainActivity.kt              边到边（edge-to-edge）宿主
├── data/                        CountdownEvent 模型 + JSON 持久化
├── domain/                      纯逻辑，有单元测试
│   ├── Countdown.kt             天数计算，正确处理夏令时
│   └── CalendarMath.kt          月历网格构造
└── ui/
    ├── MainViewModel.kt         状态 + 全进程唯一的那个时钟
    ├── TickCountScreen.kt       唯一的界面
    ├── calendar/                手写的月历
    ├── countdown/               顶部倒计时
    └── theme/                   Material 3 配色与字体
```

## 隐私

TickCount 只在自己私有的 `SharedPreferences` 里存一份 `(日期, 名称)` 列表，别的什么都没有。
它**不声明任何权限**，不发起任何网络请求，没有统计、没有广告。
唯一可能离开设备的就是这份列表本身 —— 前提是你自己开启了安卓的云备份，
而这由 `res/xml/backup_rules.xml` 决定。

## 许可

[MIT](LICENSE)，随便用。
