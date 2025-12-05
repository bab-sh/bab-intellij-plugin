# bab Changelog

## [Unreleased]

## [0.0.7]

### Added

- Hierarchical task grouping in tool window (tasks with colons are grouped by prefix)

### Changed

- Add changelog validation to CI workflows

## [0.0.6]

### Changed

- Update deep task nesting regex

## [0.0.5]

### Added

- Full autocompletion for external tasks showing complete task names (e.g., `external:build`, `external:test`)
- Ctrl+Click navigation to task definitions in included babfiles
- Support for task variants with colon syntax (e.g., `test:race`, `lint:fix`)

### Changed

- External tasks now display the included file path in completion popup instead of generic label
- Simplified and unified task reference resolution logic

### Fixed

- Validation now checks if tasks actually exist in included files (not just prefix existence)

## [0.0.4]

### Added

- Support for cross-file task references with include prefix syntax (`includename:taskname`)
- Autocompletion for include prefixes in task dependencies
- Validation for include prefix references

### Fixed

- Fixed false positive errors for tasks named `deps`, `run`, or `task`
- Improved robustness of task reference detection with full PSI tree validation

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

[Unreleased]: https://github.com/bab-sh/bab-intellij-plugin/compare/v0.0.7...HEAD
[0.0.7]: https://github.com/bab-sh/bab-intellij-plugin/compare/v0.0.6...v0.0.7
[0.0.6]: https://github.com/bab-sh/bab-intellij-plugin/compare/v0.0.5...v0.0.6
[0.0.5]: https://github.com/bab-sh/bab-intellij-plugin/compare/v0.0.4...v0.0.5
[0.0.4]: https://github.com/bab-sh/bab-intellij-plugin/compare/v0.0.3...v0.0.4
[0.0.3]: https://github.com/bab-sh/bab-intellij-plugin/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/bab-sh/bab-intellij-plugin/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/bab-sh/bab-intellij-plugin/commits/v0.0.1
