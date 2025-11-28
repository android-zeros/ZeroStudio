/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.models

import androidx.annotation.DrawableRes
import com.blankj.utilcode.util.ImageUtils
import com.itsaky.androidide.resources.R
import java.io.File

/**
 * Info about file extensions in the file tree view.
 *
 * @author Akash Yadav
 */
enum class FileExtension(val extension: String, @DrawableRes val icon: Int) {
  JAVA("java", R.drawable.ic_language_java),
  JSP("jsp", R.drawable.ic_language_java),
  JAR("jar", R.drawable.ic_language_java),
  KT("kt", R.drawable.ic_language_kotlin),
  KTS("kts", R.drawable.ic_language_kts),
  XML("xml", R.drawable.ic_file_type_xml),
  GRADLE("gradle", R.drawable.ic_gradle),
  GROOVY("groovy", R.drawable.ic_gradle),
  JSON("json", R.drawable.ic_file_type_json),
  PROPERTIES("properties", R.drawable.ic_language_properties),
  APK("apk", R.drawable.ic_file_type_apk),
  TXT("txt", R.drawable.ic_file_type_txt),
  MD("md", R.drawable.ic_file_type_code),
  LOG("log", R.drawable.ic_file_type_log),
  
  
  TOML("toml", R.drawable.ic_file_type_code),
  POM("pom", R.drawable.ic_file_type_code),
  PRO("pro", R.drawable.ic_file_type_code),
  TPL("tpl", R.drawable.ic_file_type_code),
  PREFS("prefs", R.drawable.ic_file_type_code),
  KEYS("keys", R.drawable.ic_file_type_code),
  YML("yml", R.drawable.ic_file_type_code),
  ANDROIDIDEROOT("androidide_root", R.drawable.ic_file_type_code),
  EDITORCONFIG("editorconfig", R.drawable.ic_file_type_editorconfig),
  GITATTRIBUTES("gitattributes", R.drawable.ic_file_type_git),
  GITGNORE("gitignore", R.drawable.ic_file_type_git),
  GITMODULES("gitmodules", R.drawable.ic_file_type_git),
  MAILMAP("mailmap", R.drawable.ic_email),
  
  // Native Build: Objective-C/Objective-C++，C/C++，cmake，c#，Rust，Golang，Swift，Make / GNU Make，Ada，D language，Pascal / Delphi，MSBuild，Ninja，Bazel
  CLANGJJCPP("cpp", R.drawable.ic_file_type_clang_cpp), //clang++ 源码文件
  CLANGJJCC("cc", R.drawable.ic_file_type_clang_cpp), //clang++ 源码文件
  CLANGJJCXX("cxx", R.drawable.ic_file_type_clang_cpp), //clang++ 源码文件
  CLANGJJC("C", R.drawable.ic_file_type_clang_cpp), //clang++ 源码文件
  CLANGJJHXX("hxx", R.drawable.ic_file_type_clang_class), //clang++ 头文件
  CLANGJJHPP("hpp", R.drawable.ic_file_type_clang_class), //clang++ 头文件
  CLANGJJHH("hh", R.drawable.ic_file_type_clang_class), //clang++ 头文件
  CLANGJJH("h", R.drawable.ic_file_type_clang_h), //clang/++头文件
  CLANGC("c", R.drawable.ic_file_type_clang_c), //clang 源码文件
  CLANGT("t", R.drawable.ic_file_type_clang_t), //clang 源码文件  TEMPLATE
 
  OBJCLANG("m", R.drawable.ic_file_type_clang_m), //Objective-C 源码文件
  OBJCLANGJJ("mm", R.drawable.ic_file_type_clang_m_small), //Objective-C++ 源码文件
 
  CLANGCS("cs", R.drawable.ic_file_type_net_clang), //c#lang 源码文件
  CLANGCSPROJ("csproj", R.drawable.ic_file_type_net_clang), //c#lang 项目文件
  
  CMAKELIST("CMakeLists.txt", R.drawable.ic_file_type_cmake), //cmake 配置文件
  CMAKE("cmake", R.drawable.ic_file_type_cmake), //cmake源码文件
  
  RUSTLANG("rs", R.drawable.ic_file_type_language_rust), //rust
  GOLANG("go", R.drawable.ic_file_type_language_golang), //golang
  SWIFT("swift", R.drawable.ic_file_type_language_swift), //swift
  
  ADB("adb", R.drawable.ic_file_type_code), //ada lang
  ADS("ads", R.drawable.ic_file_type_code), //ada lang
  DLANG("d", R.drawable.ic_file_type_code), //d lang
  PAS("pas", R.drawable.ic_file_type_code), //Pascal / Delphi
  DPR("dpr", R.drawable.ic_file_type_code), //Pascal / Delphi
  F90("f90", R.drawable.ic_file_type_code), //Fortran
  FPRTRANF("f", R.drawable.ic_file_type_code), //Fortran
  FOR("for", R.drawable.ic_file_type_code), //Fortran
  
  GUNMAKEFILE("makefile", R.drawable.ic_file_type_language_build_system_bazel2), //gun make
  GUNMK("mk", R.drawable.ic_file_type_language_build_system_bazel2), //gun make
  DP("dp", R.drawable.ic_file_type_language_build_system_bazel2), //配置文件
  AC("ac", R.drawable.ic_file_type_language_build_system_bazel2), //配置文件
  AM("am", R.drawable.ic_file_type_language_build_system_bazel2), //配置文件
  NINJA("ninja", R.drawable.ic_file_type_language_build_system_ninja), //ninja
  
  BAZEL("bazel", R.drawable.ic_file_type_language_build_system_bazel), //bazel
  BUILD("build", R.drawable.ic_file_type_language_build_system_bazel2), //bazel
  
  PROTO("proto", R.drawable.ic_file_type_language_protobuf), //bazel
  
  //script
  BAT("bat", R.drawable.ic_file_type_text_terminal_script),
  CMD("cmd", R.drawable.ic_file_type_text_terminal_script),
  SHELL("sh", R.drawable.ic_file_type_text_terminal_script),
  PYTHON("py", R.drawable.ic_file_type_text_terminal_script),
  
  
  //Specify folder settings icon
  GIT_DIRECTORY("", R.drawable.ic_folder_type_git), // .git
  GITHUB_DIRECTORY("", R.drawable.ic_folder_type_github), //github
  IDEA_DIRECTORY("", R.drawable.ic_folder_type_idea), //idea
  
  BENCHMARK_DIRECTORY("", R.drawable.ic_folder_type_benchmark), //benchmark
  GRADLE_DIRECTORY("", R.drawable.ic_folder_type_gradle), //gradle
  DOCS_TEXT_DIRECTORY("", R.drawable.ic_folder_type_docs), //docs and text
  IMAGE_DIRECTORY("", R.drawable.ic_folder_type_image), //image
  JAVA_DIRECTORY("", R.drawable.ic_folder_type_java), //java folder
  KOTLIN_DIRECTORY("", R.drawable.ic_folder_type_kotlin), //kotlin folder
  LOG_DIRECTORY("", R.drawable.ic_folder_type_log), //log
  SOURCE_SRC_MAIN_DIRECTORY("", R.drawable.ic_folder_type_src), // src/main
  TESTING_DIRECTORY("", R.drawable.ic_folder_type_testing), //test and testing
  WORKSPACE_DIRECTORY("", R.drawable.ic_folder_type_workspace), // workspace
  
  DIRECTORY("", R.drawable.ic_folder), //Default folder icon
  
  // No suffix file
  IMAGE("", R.drawable.ic_file_type_image),
  GRADLEW("", R.drawable.ic_file_type_text_terminal_script),
  LICENSE("", R.drawable.ic_file_type_license),
  
  //Unknown file
  UNKNOWN("", R.drawable.ic_file_type_unknown);


  /** Factory class for getting [FileExtension] instances. */
  class Factory {
    companion object {

      /** Get [FileExtension] for the given file. */
      @JvmStatic
      fun forFile(file: File?): FileExtension {
        return if (file?.isDirectory == true) {
                when (file.name) {
                      ".git" -> WORKSPACE_DIRECTORY
                      "git" -> WORKSPACE_DIRECTORY
                      
                      "github" -> GITHUB_DIRECTORY
                      ".github" -> GITHUB_DIRECTORY
                      
                      ".idea" -> IDEA_DIRECTORY
                      "idea" -> IDEA_DIRECTORY
                      
                      ".gradle" -> GRADLE_DIRECTORY
                      "gradle" -> GRADLE_DIRECTORY
                      
                      "docs" -> DOCS_TEXT_DIRECTORY
                      "doc" -> DOCS_TEXT_DIRECTORY
                      "text" -> DOCS_TEXT_DIRECTORY
                      "txt" -> DOCS_TEXT_DIRECTORY
                      "pdf" -> DOCS_TEXT_DIRECTORY
                      
                      "image" -> IMAGE_DIRECTORY
                      "images" -> IMAGE_DIRECTORY
                      
                      "java" -> JAVA_DIRECTORY
                      "kotlin" -> KOTLIN_DIRECTORY
                      ".kotlin" -> KOTLIN_DIRECTORY
                      
                      "log" -> LOG_DIRECTORY
                      "logs" -> LOG_DIRECTORY
                      "logging" -> LOG_DIRECTORY
                      "logger" -> LOG_DIRECTORY
                      
                      "src" -> SOURCE_SRC_MAIN_DIRECTORY
                      "main" -> SOURCE_SRC_MAIN_DIRECTORY
                      "build" -> SOURCE_SRC_MAIN_DIRECTORY
                      
                      "test" -> TESTING_DIRECTORY
                      "tests" -> TESTING_DIRECTORY
                      "testing" -> TESTING_DIRECTORY
                      "androidTest" -> TESTING_DIRECTORY
                      "commonTest" -> TESTING_DIRECTORY
                      "gradleToolingTest" -> TESTING_DIRECTORY
                      "lspTest" -> TESTING_DIRECTORY
                      "unitTest" -> TESTING_DIRECTORY
                      "测试" -> TESTING_DIRECTORY
                      "experiment" -> TESTING_DIRECTORY
                      
                      "workspace" -> WORKSPACE_DIRECTORY
                      
                 else -> DIRECTORY
              }
          }
        
          else if (ImageUtils.isImage(file)) IMAGE
          else if ("gradlew" == file?.name) GRADLEW
          else if ("LICENSE" == file?.name) LICENSE
          else forExtension(file?.extension)
      }

      /** Get [FileExtension] for the given extension. */
      @JvmStatic
      fun forExtension(extension: String?): FileExtension {
        // To not assign IMAGE, GRADLEW and DIRECTORY in case of an empty extension,
        // we check if an extension is empty here
        if (extension.isNullOrEmpty()) {
          return UNKNOWN
        }
        
        for (value in entries) {
          if (value.extension == extension) {
            return value
          }
        }

        return UNKNOWN
      }
    }
  }
}
