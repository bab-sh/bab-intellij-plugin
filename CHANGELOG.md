# bab Changelog

## [Unreleased]

## [0.0.3]

### Added
- Tool window with hierarchical tree view of all tasks across babfiles
- Support for babfile includes with nested task display
- Double-click navigation to task definitions from tool window
- Context menu with "Go to Source" and "Copy Task Name" actions
- Toolbar with refresh and collapse all functionality
- Speed search for quick task finding in tool window
- Support for environment-specific babfiles (e.g., `babfile.dev.yml`, `babfile.prod.yml`)

### Changed
- Refactored task node structure to reduce code duplication
- Externalized inspection error messages for i18n support
- Improved disposal handling and threading in tool window panel
- Consolidated icon management in `BabIcons` singleton

### Fixed
- Fixed deprecated `project.disposed` API usage
- Added proper error handling and logging for babfile parsing

## [0.0.2]

### Added
- Add Icons and plugin logo

## [0.0.1]

### Added
- File type recognition for `babfile.yml` and `babfile.yaml` (case-insensitive)
- JSON Schema validation with inline errors and warnings
- Autocompletion for babfile structure (tasks, deps, run, cmd, platforms, includes)
- Task dependency validation (errors for undefined task references)
- Self-dependency detection (errors when a task depends on itself)
- Task name autocompletion in `deps` field (excludes current task)
- Ctrl+Click navigation from dependency to task definition
- YAML syntax highlighting via bundled YAML plugin
