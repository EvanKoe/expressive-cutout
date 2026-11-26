<div align="center">

<img width="1280" height="640" alt="Frame 25" src="https://github.com/user-attachments/assets/d801de28-eac6-4ffd-8474-55d9a8af4dc3" />


# Expressive Cutout

[English](README.md) | **简体中文**

**完全离线的灵动岛，遵循 Google 的 Material Expressive 设计。**

Expressive Cutout 把 Android 手机上的挖孔摄像头变成一块小巧、会“活”起来的灵动岛。通知会落在这里，系统事件会从这里掠过，正在播放的内容、来电对象和计时器剩余时间，也都会以实时磁贴的形式出现在屏幕顶端——全部采用 Material Expressive 的形状与弹性动画，以及 Material You 配色，连圆角半径都可以逐项调整。

它完全在本地运行。应用没有任何网络权限，因此什么都不会被上传：没有账号、没有分析、也没有追踪。

本说明对应原项目 [EvanKoe/expressive-cutout](https://github.com/EvanKoe/expressive-cutout) 的简体中文本地化分支，原作者为 EvanKoe，许可证为 [GNU GPL-3.0](LICENSE)。

[![下载最新正式版](https://img.shields.io/badge/Download-latest%20release-005AC1?style=for-the-badge&logo=github&logoColor=white)](https://github.com/EvanKoe/expressive-cutout/releases)
[![Stars](https://img.shields.io/github/stars/EvanKoe/expressive-cutout?style=for-the-badge)](https://github.com/EvanKoe/expressive-cutout/stargazers)
[![License](https://img.shields.io/badge/license-GPLv3-blue?style=for-the-badge)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Discord](https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/SNfcuuJeYF)

</div>

---

## 目录

- [截图](#截图)
- [下载与安装](#下载与安装)
- [功能](#功能)
- [权限与 Shizuku](#权限与-shizuku)
- [特别感谢](#特别感谢)
- [许可证](#许可证)

---

## 截图

<!-- Screenshots go here. -->

---

## 下载与安装

<div align="center">

[![下载最新正式版](https://img.shields.io/badge/Download-latest%20release-005AC1?style=for-the-badge&logo=github&logoColor=white)](https://github.com/EvanKoe/expressive-cutout/releases)

</div>

需要 Android 10（API 29）或更新版本。首次启动时，应用会引导你完成它所需要的三项设置：通知使用权（用于镜像通知）、无障碍服务（用于绘制悬浮层），以及可选的电池优化豁免，让灵动岛在后台也能稳定工作。

欢迎加入我们的 [Discord 服务器](https://discord.gg/SNfcuuJeYF)，提交反馈、功能建议和问题报告，并与 Expressive by Evan 社区交流。

<table>
 <tr>
  <td>
   <img width="1080" height="2410" alt="Screenshot_20261006-105109" src="https://github.com/user-attachments/assets/a8baa083-8ae0-400e-8c64-be51dde517dd" />
   <sub><b>设置页</b></sub>
  </td>

  <td>
   <img width="1080" height="2410" alt="Screenshot_20261006-105118" src="https://github.com/user-attachments/assets/fc0cb5d9-268f-446c-9404-01523cb73ef3" />
   <sub><b>主页面</b></sub>
  </td>

  <td>
   <img width="1080" height="2410" alt="Screenshot_20261006-105209" src="https://github.com/user-attachments/assets/4edac07d-a49b-4f41-b18c-d59769098495" />
   <sub><b>外观设置</b></sub>
  </td>
 </tr>

 <tr>
  <td>
   <img width="1080" height="2410" alt="Screenshot_20261006-105513" src="https://github.com/user-attachments/assets/2233784a-1be2-4814-ac25-b3b7d72720d2" />
   <sub><b>音乐磁贴</b></sub>
  </td>

  <td>
   <img width="1080" height="2410" alt="Screenshot_20261006-105542" src="https://github.com/user-attachments/assets/b54b2d1b-24f9-4672-b3b9-8d12ac41d876" />
   <sub><b>通话磁贴</b></sub>
  </td>

  <td>
   <img width="1080" height="2410" alt="Screenshot_20261006-105619" src="https://github.com/user-attachments/assets/7719a864-651e-4c39-ad4b-6939938d5b87" />
   <sub><b>回复消息</b></sub>
  </td>
 </tr>
</table>

---

## 功能

声明：这个应用最初是我学习 Jetpack Compose 的项目。开发过程中我使用了人工智能辅助。

| 分类 | 功能 |
| --- | --- |
| **通知** | 将任意应用的通知镜像到灵动岛 · 图标直接取自通知本身 · 可自动展开显示标题和正文，也可只保留应用图标 · 可在岛上直接使用操作按钮和内联回复 · 滑动关闭也会同时清除系统通知 |
| **音乐磁贴** | 正在播放的歌曲名和艺术家 · 收起状态下显示专辑封面，播放时可选择旋转，暂停时停止 · 展开后提供上一首 / 播放暂停 / 下一首控件 |
| **通话磁贴** | 正在进行的通话会显示来电者姓名、联系人头像和通话时长 · 可使用拨号应用自带的通话操作，例如挂断 · 来电视图会显示来电号码以及接听 / 拒接按钮，点按可打开通话界面 · 可选用更高的两行来电布局和全宽按钮 |
| **计时器磁贴** | 镜像时钟应用中正在运行的倒计时，包括 Android 16 及以上的实时更新计时器 · 使用时钟应用的真实按钮和标签，例如加 1 分钟和重置 · “暂停”会实时切换为“继续” |
| **系统事件** | 开始充电和停止充电 · 电量过低 · Wi-Fi 已连接和已断开 · 耳机插入和拔出 · USB 已连接和已移除 · 热点开和关 · 设备已解锁 |
| **隐私指示点** *（Shizuku）* | 当有应用正在使用相机（绿色）、麦克风（红色）或位置（蓝色）时，在挖孔处作出标记 · 每个指示点可单独开关，并有各自的颜色 · 可放在左侧或右侧，也可排成一行或叠成一列 |
| **状态栏** *（Shizuku）* | 隐藏系统状态栏中的通知图标，只由灵动岛来提示 · 隐藏电池、Wi-Fi 和信号图标 · 隐藏时钟 · 从源头静音系统提醒，从而关闭声音、振动和横幅弹出，只由灵动岛提醒你 · 锁屏可单独设置规则 |
| **按应用控制** | 关闭某个应用后，它发出的内容都不会进入挖孔区，通知和媒体都不例外 · 也可以让某个应用只出现在普通岛上，永不进入展开态 · 应用列表可搜索 |
| **按事件自定义** | 可从完整 Material 图标库中任选图标，并带搜索过滤 · 可使用动画图标，并可选择循环播放 · 可为单个事件覆盖颜色 · 可为单个事件设置屏幕停留时间（或在其状态持续期间一直显示） · 也可以一次性为所有事件使用同一种 Material You 颜色 |
| **尺寸与位置** | 普通岛和展开岛的宽度、高度可分别设置 · 圆角可统一设置，也可只改上/下圆角，或逐角调整 · 可调整垂直和水平位置 · 提供实时预览，并可切换浅色 / 深色背景 · 可恢复默认 |
| **外观** | 普通岛和展开岛可分别设置纯色或渐变背景，并调整方向和不透明度 · 柔和阴影 · 描边可单独设置宽度和颜色 · 各处均可使用 Material You、预设色或自定义十六进制颜色 · 应用主题可选浅色、深色或跟随系统 |
| **操作按钮** | 四种按钮样式（Expressive 色调、Expressive 填充、Material You、描边） · 四种回复栏样式，包括分段条 · 可调整按钮颜色和高度 · 取消按钮可放在左侧或发送按钮旁边 · 通话和计时器磁贴可使用各自的按钮颜色 |
| **动画** | Expressive 弹簧或缓入缓出风格 · 慢速 / 默认 / 快速 · 大弹跳 / 普通 / 小弹跳 · 动画时长可在 0 到 1000 毫秒之间缩放 · 可对照实时示例进行调节 |
| **行为与手势** | 可在延迟后自动收起，也可保持展开直到点按 · 收起后也可选择完全消失 · 上滑收起 · 左右滑动关闭，普通岛和展开岛可分别设置 · 可在锁屏时隐藏，并彻底拆除悬浮层，不再占用资源 |
| **隐私** | 完全没有网络权限，因此任何数据都无法离开设备 · 没有分析和追踪 · 屏幕内容仅用于实现功能，既不会存储，也不会发送 · 每项权限都会在应用内说明，完整清单可在“权限详情”页面查看 |
| **测试** | 可给自己发送带回复和操作按钮的测试通知 · 可触发测试中的通话和测试来电 · 可在各事件的详情页预览该系统事件在岛上的效果 |

---

## 权限与 Shizuku

你的数据隐私很重要。应用只会申请维持功能所必需的最低权限。此外，它没有 `INTERNET` 网络访问权限，这意味着不会有任何内容被发送到互联网：没有分析、没有使用数据，通知内容也不会存储到任何地方。一切始终只留在你的手机上。下面列出的是完整清单，每项权限也会在应用内的“权限详情”页面有更详细的说明：

- 通知使用权：用于在灵动岛中显示通知。Android 把媒体访问也绑定到同一授权，因此音乐磁贴也靠它来显示正在播放的内容并进行控制。
- 无障碍服务：用于在其他应用上方显示悬浮层。它也可能读取屏幕内容，但只是为了让功能生效：会读取当前前台是哪个应用（以便在你使用该应用时隐藏相应磁贴）。它只看这些——对其余应用而言，看到的只是一个包名，没有更多内容。读取到的信息会显示在岛上，随即丢弃：不会写入存储，不会记录日志，而且由于应用没有网络权限，也无法发送到任何地方。
- 振动：用于应用内和挖孔区的触觉反馈。
- 忽略电池优化（可选）：确保电池优化不会把应用杀掉。
- 发送通知（可选）：仅用于测试。
- 音频设置：以便展开后的通话磁贴可以使用“静音”和“扬声器”按钮来路由正在进行的通话音频，因为没有拨号应用会把这两项作为通知操作提供。它不会录音——此权限也做不到这一点。
- 电话状态（可选）：只报告当前是正在振铃、已接通还是已结束——不会读取电话号码、来电身份或通话记录。有了它，通话磁贴可以在你挂断的瞬间关闭，而不必等到发现拨号应用的通知消失。没有这项权限时，磁贴同样会关闭，只是会晚几秒。
- Shizuku（可选）：仅用于 Android 保留给 shell 用户的两项功能——隐藏系统状态栏图标，以及读取哪些应用正在使用麦克风、相机或位置，以便显示隐私指示点。Shizuku 是你需要自行安装并启动的独立应用，并且要在其界面中批准本应用。没有 Shizuku 时，这两项功能不可用，其余功能仍可正常工作。
- 网络状态：只能读取你是否已连接到网络（用于 Wi-Fi 事件），并不能真正访问网络。
- Wi-Fi 状态：从 Android 12L 起，系统只会向持有此权限的应用通报热点的开关，因此热点事件要靠它触发。该权限为只读，会自动授予：不能开关 Wi-Fi 或热点，不能查看已连接设备，也不会提供网络访问。
- 应用列表可见性（`<queries>`，不是需要你手动授予的权限）：在 Android 11 及以上，应用默认看不到其他应用，除非说明自己需要看到什么。本应用申请查看带启动器图标的应用，以便“应用”页面列出它们供按应用开关使用，也能用应用图标为灵动岛着色，并判断是否已安装 Shizuku。这里有意没有申请 `QUERY_ALL_PACKAGES`。

---

## 特别感谢

<!-- Special thanks go here. -->
即使这个项目最初只是一个个人应用，许多已有项目也给了我所需的灵感，无论是界面还是功能：
- [Dynamic spot](https://play.google.com/store/apps/details?id=com.jamworks.dynamicspot&hl=en)
- [Material Capsule](https://play.google.com/store/apps/details?id=com.pryshedko.mtisland&hl=en)

还要特别感谢 [Sameerasw 的 Essential app for Android](https://github.com/sameerasw/essentials)，它有非常出色的 Material You Expressive 实现。作者做得非常认真，他的应用都给人一种打磨得很细致的感觉。

也感谢所有在 Discord、GitHub 和 Reddit 上参与本项目的人，无论是提交错误、提出功能需求，还是创建 Issue。

---

## 许可证

Expressive Cutout 是自由软件，采用 [GNU General Public License v3.0](LICENSE) 许可。你可以自行使用、研究、分享和修改；任何再分发的衍生作品都必须继续使用同一许可证。

原项目由 [EvanKoe](https://github.com/EvanKoe) 在 [EvanKoe/expressive-cutout](https://github.com/EvanKoe/expressive-cutout) 发布。
