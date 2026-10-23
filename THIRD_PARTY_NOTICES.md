# 素材、代码来源与许可

## 项目代码

静境 TV 自编写代码按根目录 MIT LICENSE 分发。第三方内容仍遵守各自许可。

## CleanYourTV

- 作者：Gathin，Copyright (c) 2026 Gathin。
- 仓库：https://github.com/Gathin23/cleanyourtv
- 参考提交：7377a9be8ca0b33af6fea006230414582fd150bd。
- 授权：MIT，完整文本为 `licenses/CleanYourTV-MIT.txt`，APK 内也保留声明。
- 复用/适配：GlassView.java、pill_bg.xml、部分图标资源、应用图标/banner、aurora 壁纸；布局、Dock、导航与焦点机制借鉴上游后移植到 API 21。
- 5.4-rc2 另引入提交 `5e9e4f119e405eb8bcc18296f2567c9d6a7c4217` 的 `examples/README.md` 目录数据，以及 `examples/tcl-g08-eu-android12/debloat.txt` 来源说明。目录只作社区参考；仅整理其中三项推送/消息/日志组件到独立的精确固件规则，不执行原始文本或整份清单。
- 主要改动：旧系统原生实现、中文交互、壁纸菜单、独立应用页、焦点恢复、独立图标前景与厂商 HDMI/设置适配。

## Outfit

字体来自上游 Outfit，按 SIL OFL 1.1 分发。全文见 `licenses/Outfit-OFL.txt`。为旧 Android 将可变字体实例化成 300/500 两个静态字重，文件为 `assets/fonts/outfit-light.ttf`、`outfit-medium.ttf`。保留原字体与许可，不单独出售字体。

## 原创生成壁纸与图稿

- `assets/wallpapers/mac-silk.jpg`、`mac-coast.jpg`：生成的 mac 风格原创壁纸，不是 Apple 原图。
- `assets/wallpapers/mountain-lake.png`：生成的原创山湖壁纸，1672×941；提示词见 `artwork/mountain-lake-prompt.txt`。
- `assets/icons/brand-study.png`：生成的品牌卡片视觉样稿；用于首页四品牌取样，非品牌方官方资产、非独立矢量前景。
- 其他入口的可编辑设计位于 `design/catalog.js`，透明前景导出位于 `assets/icons/`。

项目自行创作的图稿与生成壁纸以仓库 MIT 许可提供，但该许可不转让第三方商标权，也不代表品牌授权。应用品牌名称及标识属于各自权利人。项目与 Apple、微鲸及所展示应用的品牌没有隶属关系，不声称使用了 Apple 官方图标。

## 厂商接口与截图

WhaleyInputs 为测试设备的适配代码；仓库不包含原厂 APK、框架 JAR、固件镜像或反编译代码。截图只展示桌面测试状态，不能当作其他电视的兼容性保证。
