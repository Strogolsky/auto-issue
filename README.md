# auto-issue

![Build](https://github.com/Strogolsky/auto-issue/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [ ] Get familiar with the [template documentation][template].
- [ ] Adjust the [pluginGroup](./gradle.properties) and [pluginName](./gradle.properties), as well as the [id](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/kotlin).
- [ ] Adjust the plugin description in `README` (see [Tips][docs:plugin-description])
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the `MARKETPLACE_ID` in the above README badges. You can obtain it once the plugin is published to JetBrains Marketplace.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.
- [ ] Configure the [CODECOV_TOKEN](https://docs.codecov.com/docs/quick-start) secret for automated test coverage reports on PRs

<!-- Plugin description -->

## 🚀 What is auto-issue?

**auto-issue** is an intelligent IntelliJ IDEA plugin that transforms your TODO comments into JIRA tasks automatically! 
Instead of manually creating issues, just write a TODO in your code and let AI do the heavy lifting. 
The plugin analyzes your code context and generates comprehensive JIRA issues in seconds.

### ✨ Key Features

- **🤖 AI-Powered Issue Generation** — Uses LLM (Large Language Models) to automatically create meaningful JIRA issues from your TODOs
- **📊 Smart Code Analysis** — Analyzes project structure, files, and code context to provide AI with all necessary information
- **⚙️ Seamless JIRA Integration** — Automatically creates issues in JIRA and updates your code with issue keys
- **🎯 One-Click Workflow** — Just click the gutter icon next to any TODO comment to start the process
- **🔗 Auto-Update Code** — Successfully created issues are automatically linked in your source code
- **🛠️ Extensible Architecture** — Supports multiple LLM providers and custom issue strategies via plugin extension points

### 📋 Requirements

- **IntelliJ IDEA 2025.3** or later (or other JetBrains IDEs based on IntelliJ Platform 253+)
- **JIRA Account** with API token for project access
- **LLM Provider Credentials** (Google Vertex AI with Gemini or compatible LLM service)

### ⚙️ Configuration

The plugin provides easy configuration through IntelliJ settings:

1. **JIRA Integration** — Settings > Tools > AutoIssue > Jira Integration
   - Configure JIRA URL and API credentials
   - Select target JIRA project

2. **LLM Agent** — Settings > Tools > AutoIssue > LLM Agent
   - Set up your LLM provider credentials
   - Choose model and parameters

### 🎬 How It Works

1. **Write TODOs** — Add TODO comments in your code as usual
2. **See the Icon** — A "+" icon appears in the editor gutter next to unresolved TODOs
3. **Click to Create** — Click the icon to trigger issue generation
4. **AI Analyzes** — The plugin gathers project context and uses AI to generate an issue
5. **Review & Confirm** — A dialog shows the generated issue for your review
6. **Create & Link** — Confirm to create the issue in JIRA and automatically update your code with the issue key

### 💡 Usage Example

```
// Before:
// TODO fix authentication bug in login flow

// After (automatic):
// TODO [PROJ-123]
```

Just click the gutter icon and the plugin handles the rest!

This specific section is a source for the [plugin.xml](/src/main/resources/META-INF/plugin.xml) file which will be extracted by the [Gradle](/build.gradle.kts) during the build process.

To keep everything working, do not remove `<!-- ... -->` sections. 
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "auto-issue"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/Strogolsky/auto-issue/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## License

Copyright (c) 2026 Sergei Iarylkin.

This project is licensed under the Apache License, Version 2.0 — see the [LICENSE](LICENSE) file for details.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
