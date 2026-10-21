# Android 4.4 兼容测试版

版本：**5.3-rc1 / versionCode 10**。兼容目标下限为 **Android 4.4 / API 19**。

这是独立分支的候选版，不替换 main 分支的 5.2 稳定版。目前没有在 Android 4.4 真机或 API 19 模拟器上完成运行验证；“构建与静态检查通过”不能当作“所有 4.4 电视实测兼容”。

## 已完成的兼容处理

| 项目 | Android 4.4 | Android 5.0 及以上 |
| --- | --- | --- |
| 主题 | Holo 全屏主题 | 保留 Material 全屏主题 |
| 顶栏矢量图标 | 由相同路径导出的 xxxhdpi PNG | 保留 anydpi-v21 矢量资源 |
| 卡片与预览圆角 | Canvas Path 裁切 | 原生 outline 裁切 |
| 原生 Z 轴阴影 | 跳过，保留缩放、高光与前景动画 | 保留 elevation |
| 字距 | 系统默认字距 | 保留显式字距 |
| 低分辨率图标回退 | 不调用新版本 loadBanner | 版本保护后读取 Banner |
| 壁纸与玻璃取样 | 壁纸长边按 1280 上限、以 2 的幂降采样；玻璃缓存宽 240 | 保留现有壁纸精度；缓存宽 360 |

所有 API 20/21 的直接框架调用均集中在 `AndroidCompat.Api21`，由外层 SDK>=21 判断后进入。原生 Canvas 的新版本浮点圆角重载已替换为旧版本可用的 RectF 重载。

四应用 Dock、第二排、全应用页、壁纸设置和遥控导航仍保留。图标、图片和 Java 代码不含 JNI 本地库，没有把 APK 限定为 ARM64。

## 已执行的检查

- Manifest 最低 API 19，D8 `--min-api 19`；生成 DEX 035。
- 构建、对齐与签名验证通过；包括 Android 4.4 所需的 v1/JAR 签名。
- API 静态检查对照 SDK 的 api-versions.xml 扫描编译后的方法调用；只发现隔离在 Api21 类中的高版本引用，没有未解析的公开框架方法调用。
- 未限定版本的资源不含 VectorDrawable；每个 v21 矢量都有 PNG 回退；旧主题不引用 Material。
- 测试 APK 与 5.2 发布版使用同一签名证书。发布私钥仍不在仓库中。

静态检查涵盖直接方法调用、版本保护约定及关键资源回退；不等同于 Android Lint 的完整检查，也不验证厂商服务、设备渲染、字体和运行时内存行为。

## 在自己的 4.4 电视上测试

从对应预发布页面下载 `QuietTV-5.3-rc1-android44.apk`：

```sh
TV=192.168.1.100:5555
adb connect "$TV"
adb -s "$TV" shell getprop ro.build.version.sdk
adb -s "$TV" install -r QuietTV-5.3-rc1-android44.apk
adb -s "$TV" shell am start -n cn.quiet.tv/.MainActivity
```

先保留原厂桌面，只测试：冷启动、上下左右/返回、全应用末行、壁纸切换、菜单圆角、应用启动后返回。失败时提供具体型号、固件、API 级别和 `adb -s "$TV" logcat -d -s AndroidRuntime:E QuietWallpaper:W` 的相关日志，移除设备与账号信息。

**微鲸 HDMI/CEC 接口、系统设置组件、原厂桌面与广告包管理尚未在 4.4 固件上验证。** 5.2 教程中的 `pm hide` 等操作是已测试 Android 5.x 微鲸固件的做法，不是这个兼容 APK 在 4.4 上附带的通用系统能力，请勿照搬到陌生设备。

测试包与稳定版包名相同。已有 5.2 的设备没有必要为 4.4 适配而升级；候选版版本号更高，回退可能需要卸载并丢失本应用设置。测试时保留原厂桌面和 ADB 恢复通道。

## 自行构建与检查

与主教程一样准备 JDK 17、SDK Platform 33、Build Tools 33.0.0，并额外安装 Python 3：

```sh
bash build.sh
python3 scripts/check-api19.py
```

默认构建仍使用自己的开发签名，不能冒充发布版签名。需要修改旧版本图标时，在安装 sharp 后运行：

```sh
node design/export-legacy-icons.cjs
```
