# Minecraft Mod Development

用于 Minecraft Java Edition 模组开发的仓库。

## 本机环境

- macOS 26.5.1 (Apple Silicon)
- Java 21 LTS
- Git 2.50+
- GitHub CLI 2.94+

## 下一步

确定目标 Minecraft 版本和模组加载器后，再生成对应的 Gradle 项目：

- Fabric：轻量、更新快，适合从零开始和客户端模组。
- NeoForge：事件与生态较完整，适合内容型模组和较复杂项目。

项目生成后，使用仓库自带的 Gradle Wrapper 构建，不依赖全局安装 Gradle。

```bash
./gradlew build
```

