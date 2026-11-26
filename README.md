# Safe Creative

**Safe Creative** 是一个面向 Minecraft **1.21.1 / NeoForge 21.1.219** 的纯客户端模组，为创造模式补充三项安全便利功能。

- 中键安全取块
- 原方块掉落
- 容器保护

模组本身不要求服务器安装；特殊掉落功能仅在本机房主的单人世界或已开启 LAN 的世界中生效。

## 功能

### 中键安全取块

在创造模式中使用鼠标中键取方块时，模组会固定当前快捷栏格并避免覆盖背包中的其他物品：

- 准星指向空气、超出方块交互距离或没有可取物品时，不执行操作。
- 当前快捷栏物品与目标物品相同时，保持不变。
- 目标物品已在背包中时，与当前快捷栏整组交换。
- 目标物品不在背包且背包存在空位时，将当前物品移走，再将目标物品放入原快捷栏格。
- 背包已满且无法安全腾位时，只会尝试丢出整组当前物品；无法确认服务端腾出格子时，不会覆盖物品。
- 支持保留数量、耐久、物品名称、附魔和容器内容。
- 继续保留原版中键取物操作和 `Ctrl + 中键` 复制方块数据的行为。
- 实体取物仍交由原版处理。

### 原方块掉落

使用快捷键切换该功能。默认关闭。

开启后，仅当以下条件同时满足时，成功挖掉方块后会在原地补回对应方块的本体物品：

- 玩家处于创造模式。
- 玩家为单人世界或自己开启的 LAN 世界中的本机房主。
- 方块移除成功且方块掉落规则允许掉落。
- 方块物品可直接由方块类型生成，而不是复制掉落表结果。

例如，挖掉石头会掉落石头本身，而不是圆石。非空潜影盒不会额外复制物品或容器内容，方块原有掉落逻辑、取消事件和游戏规则仍会得到尊重。

连接其他玩家的服务器时，该功能不会模拟掉落。

### 容器保护

使用快捷键切换该功能。默认关闭。

开启后，创造模式下在左键挖掘请求发出前拦截容器破坏，适用于：

- 点按挖掩；
- 长按挖掘；
- 从普通方块扫到容器；
- 原版容器、末影箱以及客户端可识别物品栏的模组容器。

右键打开容器不受影响。该保护只作用于本地直接挖掘请求，不会阻挡爆炸、其他玩家或范围挖掘造成的连带破坏。

## 快捷键

| 功能 | 默认按键 | 说明 |
| --- | --- | --- |
| 原方块掉落 | `Alt + J` | 开启或关闭，当前运行有效 |
| 容器保护 | `Alt + K` | 开启或关闭，当前运行有效 |

快捷键可在 Minecraft 的“选项 → 控制 → 按键绑定”中修改。切换状态时会在快捷栏上方显示提示。两个开关互相独立，也不会创建额外配置文件；重启游戏后恢复为关闭状态。

三项功能均只在创造模式中启用，不改变生存模式或冒险模式玩法。

## 安装

1. 安装 Minecraft 1.21.1。
2. 安装兼容的 NeoForge：建议使用 **21.1.219**。
3. 将构建产物 `build/libs/safecreative-1.0.0.jar` 放入游戏的 `mods` 文件夹。
4. 启动游戏并确认“模组列表”中出现 **Safe Creative**。

本模组为客户端模组，不要求普通服务器安装相同模组。

## 从源码构建

环境要求：

- JDK 21
- Windows、Linux 或 macOS
- 可访问 NeoForge 和 Minecraft 所需的 Maven 仓库

Windows：

```powershell
.\gradlew.bat clean build -Djava.io.tmpdir=C:\Temp
```

Linux 或 macOS：

```bash
./gradlew clean build
```

构建完成后，产物位于：

```text
build/libs/safecreative-1.0.0.jar
```

如果 IDE 缺少依赖，可刷新 Gradle 缓存：

## 开发运行

启动客户端开发实例：

```text
./gradlew runClient
```

启动服务端开发实例：

```text
./gradlew runServer
```

Windows 下可使用 `gradlew.bat` 代替 `./gradlew`。

## 验收状态

已完成：

- Wrapper 构建成功。
- 构建产物包含三个 Mixin、中英文翻译和 NeoForge 模组元数据。
- 产物声明 Minecraft `1.21.1`，NeoForge 版本范围从 `21.1.219` 起。
- 独立开发实例日志确认 Safe Creative 已加载，三个 Mixin 均已应用。
- 开发实例可启动集成服务器并进入测试世界。

尚未在此环境中取得逐项、可复现的完整游戏验收结果，包括：

- 空气中键取块；
- 背包整组交换；
- 满包时的单次丢弃；
- 石头掉落石头；
- 潜影盒不重复掉落；
- 长按及扫到容器时的保护；
- 生存模式边界；
- LAN 与远程服务器边界。

使用模组时应优先在测试存档中验证背包、容器和掉落行为。

## 技术实现

- NeoForge 1.21.1 客户端模组入口：[SafeCreative.java](src/main/java/com/example/safecreative/SafeCreative.java)
- 安全中键取块：[SafePick.java](src/main/java/com/example/safecreative/SafePick.java)
- 原方块掉落：[OriginalBlockDrops.java](src/main/java/com/example/safecreative/OriginalBlockDrops.java)
- 容器保护：[ContainerProtection.java](src/main/java/com/example/safecreative/ContainerProtection.java)
- Mixin 注册：[safecreative.mixins.json](src/main/resources/safecreative.mixins.json)
- 模组元数据：[neoforge.mods.toml](src/main/resources/META-INF/neoforge.mods.toml)

项目不包含自定义网络频道或第三方运行时依赖。中键取块的背包操作会等待服务端同步确认，确认失败、超时或玩家状态变化后不会继续放入候选物品。

## 兼容性说明

- 客户端功能依赖模组在客户端加载。
- 原方块掉落以集成服务器和本机房主身份为准，不会为远程服务器模拟掉落。
- 容器识别同时检查原版容器类型、方块实体、末影箱、讲台和 NeoForge 方块物品栏能力。
- 容器保护只拦截直接攻击挖掘请求，不是通用的区域或爆炸保护。

## 许可证

模组元数据将许可证声明为 **All Rights Reserved**。仓库中的 `TEMPLATE_LICENSE.txt` 是 NeoForged MDK 模板文件所适用的 MIT 许可证，不会单独代表对模组新增源代码的再授权。
