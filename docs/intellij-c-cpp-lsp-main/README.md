# C/C++ Plugin for IntelliJ IDEA Community Edition

The C/C++ IntelliJ IDEA plugin provides language support for the C/C++ using [LSP4IJ](https://github.com/redhat-developer/lsp4ij).

To provide those support, the plugin required installed external tools:

 * [Clangd](https://github.com/clangd/clangd)
 * [LLDB](https://github.com/llvm/llvm-project/tree/main/lldb)
 * [Bear](https://github.com/rizsotto/Bear) (optional)

## How to install

- Open the folder with this project, run `./gradlew buildPlugin` (`.\gradlew.bat buildPlugin` for Windows) to build the plugin
- The built plug-in will be at the `build/distributions` folder in .zip format
- Install pligin from disk: Settings > Plugins > cog icon > Install plugin from disk
- Reload your IDE

## License

Apache License Version 2.0
