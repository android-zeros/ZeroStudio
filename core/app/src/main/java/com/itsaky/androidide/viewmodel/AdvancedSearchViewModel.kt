package com.itsaky.androidide.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsaky.androidide.search.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.io.File

class AdvancedSearchViewModel : ViewModel() {

    val results = MutableLiveData<List<SearchResultItem>>()
    val isSearching = MutableLiveData(false)
    val statusInfo = MutableLiveData("")
    
    // 搜索配置
    var query = ""
    var replacement: String? = null
    var scope = SearchScope.ALL
    var customScope = CustomScopeType.ALL_PLACES

    var targetModule: File? = null
    var targetDirectory: File? = null 

    var fileMasks = mutableListOf<String>()
    var excludePatterns = mutableListOf<String>()
    
    var matchCase = false
    var wholeWord = false
    var useRegex = false
    
    private var searchJob: Job? = null
    private val resultBuffer = ArrayList<SearchResultItem>()

    fun startSearch(currentEditorFile: File?) {
        searchJob?.cancel()
        resultBuffer.clear()
        results.value = emptyList()
        isSearching.value = true
        statusInfo.value = "Searching..."

        val config = SearchConfig(
            query = query,
            replacement = replacement,
            scope = scope,
            customScope = customScope,
            targetModule = targetModule,
            targetDirectory = targetDirectory,
            currentFile = currentEditorFile,
            fileMasks = fileMasks,
            isCaseSensitive = matchCase,
            isWholeWord = wholeWord,
            isRegex = useRegex,
            excludePatterns = excludePatterns
        )

        searchJob = viewModelScope.launch {
            ProjectSearchEngine.search(config)
                .onCompletion { 
                    isSearching.value = false 
                    val count = resultBuffer.filterIsInstance<TextMatchResult>().size
                    statusInfo.value = if(count == 0) "No matches found" else "Found $count matches"
                }
                .collect { batch ->
                    resultBuffer.addAll(batch)
                    results.postValue(ArrayList(resultBuffer))
                }
        }
    }

    fun stopSearch() {
        searchJob?.cancel()
        isSearching.value = false
        statusInfo.value = "Search cancelled"
    }
}