<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# auto-issue Changelog

## [1.0.0] - 2026-05-14

### Added
- 🤖 AI-Powered Issue Generation using LLM (Large Language Models) to automatically create meaningful JIRA issues from TODO comments
- 📊 Smart Code Analysis that gathers project structure and code context for AI
- ⚙️ Seamless JIRA Integration with automatic issue creation and code linking
- 🎯 One-Click Workflow with gutter icon for triggering issue generation
- 🔗 Auto-Update Code with issue keys after successful creation
- 🛠️ Extensible Architecture supporting multiple LLM providers and custom issue strategies via plugin extension points
- 🎨 User-Friendly Configuration through IntelliJ settings
- ✨ Comprehensive IDE Integration for IntelliJ IDEA 2025.3+

### Infrastructure
- Complete CI/CD pipeline with GitHub Actions
- Automated plugin signing and publishing
- Code quality checks with Qodana and KTLint
- Test coverage reporting with Kover
- Built-in support for multiple LLM providers (Google Vertex AI, etc.)
