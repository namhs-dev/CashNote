package com.namhs.cashnote.presentation.category

import androidx.lifecycle.viewModelScope
import com.namhs.cashnote.domain.usecase.GetCategoriesUseCase
import com.namhs.cashnote.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class CategoryManageViewModel @Inject constructor(private val getCategories: GetCategoriesUseCase) :
    MviViewModel<CategoryManageUiState, CategoryManageIntent, CategoryManageSideEffect>(
        CategoryManageUiState()
    ) {

    init {
        observeCategories()
    }

    override fun onIntent(intent: CategoryManageIntent) {
        when (intent) {
            is CategoryManageIntent.TypeChanged -> setState { copy(type = intent.type) }

            CategoryManageIntent.BackClicked ->
                postSideEffect(CategoryManageSideEffect.NavigateBack)
        }
    }

    // 탭(유형)이 바뀔 때마다 해당 유형의 카테고리를 다시 구독한다
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeCategories() {
        viewModelScope.launch {
            uiState.map { it.type }
                .distinctUntilChanged()
                .flatMapLatest { type -> getCategories(type) }
                .collect { categories -> setState { copy(categories = categories) } }
        }
    }
}
