# bab

![Build](https://github.com/bab-sh/bab-intellij-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
IntelliJ IDEA plugin for [bab](https://bab.sh) - a simple, cross-platform task runner.

## Features

- File type recognition for `babfile.yml` and `babfile.yaml` (case-insensitive)
- YAML syntax highlighting for babfiles
- Schema-based validation with inline errors and warnings
- Autocompletion for babfile properties (tasks, deps, run, cmd, platforms, includes)
- Task dependency validation with errors for undefined task references
- Self-dependency detection (errors when a task depends on itself)
- Task name autocompletion in `deps` field (excludes current task to prevent circular deps)
- Ctrl+Click navigation from dependency to task definition
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "bab"</kbd> >
  <kbd>Install</kbd>

- Manually:

  Download the [latest release](https://github.com/bab-sh/bab-intellij-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Settings</kbd> > <kbd>Install plugin from disk...</kbd>
