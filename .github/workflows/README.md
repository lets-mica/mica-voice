# mica-voice CI 说明

## 工作流

| 工作流 | 触发条件 | 说明 |
| ------ | -------- | ---- |
| `test-and-build.yml` | push / PR | 跑 `mvn package -P !develop`，JDK 矩阵 `[8, 17, 21, 25]` |
| `publish-snapshot.yml` | push / PR 到 `master` | 当 pom 里 `<revision>` 是 SNAPSHOT 时发布到 OSSRH |

## 已知潜在问题（与 mica-voice 现状不完全匹配）

1. **`publish-snapshot.yml` 提取版本失败**：脚本用 `sed -n 's/<revision>\(.*\)<\/revision>/\1/p' pom.xml`
   提取 `<revision>` 标签内容，但当前 `pom.xml` 直接写的是 `<version>1.0.0-SNAPSHOT</version>`，
   不是 `<revision>` —— 发布步骤会被跳过（`is_snapshot` 为空）。修复方法（任选其一）：
   - 在 pom.xml 顶层加 `<revision>${revision}</revision>` 并加 flatten-maven-plugin
   - 或把 sed 的匹配改为 `<version>`（注意 maven-flatten-plugin 也会改写）
2. **`test-and-build.yml` 没跑单测**：`mvn package` 默认不执行 `mvn test`。
   如果要单测在 CI 跑，需要改成 `mvn verify` 或显式加 `mvn test package`。
3. **JDK 25 没本地验证**：sherpa-onnx-demo README 说"实测 JDK 25 可运行"，但 mica-voice-core 自身没在
   JDK 25 上验证过，遇到问题可临时从矩阵里去掉 25。
4. **`-P !develop`**：当前项目没有 develop profile，这个写法是 NOP（无害）。
5. **`deploy.sh` 假设 JDK 17**：`export JAVA_HOME=`/usr/libexec/java_home -v 17`` —— macOS 专用
   命令，CI 是 ubuntu-latest 跑这个脚本会失败。release profile 由 publish-snapshot 显式指定
   Java 17/21，问题不大。
6. **`MAVEN_GPG_PRIVATE_KEY` 等 secrets**：发布到 OSSRH 需要在 GitHub repo 里配置好这些 secrets。

## 当前我们主要用的 CI

- ✅ `test-and-build.yml` 直接可用（跑 `mvn package` 会顺带触发 test-compile + jar 打包）
- ⚠️ 单测没有自动化跑（沙盒限制 surefire 写元数据，本地同理）

## 待办（如果你后面想做）

- [ ] 把 publish-snapshot 的 `<revision>` sed 改成匹配 `<version>`，或用 flatten-maven-plugin
- [ ] 在 test-and-build 里加 `mvn verify` 替代 `mvn package`，让单测纳入 CI
- [ ] 矩阵加 Windows / macOS
- [ ] 加上 `dorny/pipe-best-version` 或 `JReleaser` 自动在 tag 触发 release