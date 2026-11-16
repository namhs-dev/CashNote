package com.namhs.cashnote.presentation.category

import androidx.lifecycle.viewModelScope
import com.namhs.cashnote.R
import com.namhs.cashnote.domain.model.Category
import com.namhs.cashnote.domain.model.CategoryDeleteResult
import com.namhs.cashnote.domain.usecase.AddCategoryUseCase
import com.namhs.cashnote.domain.usecase.DeleteCategoryUseCase
import com.namhs.cashnote.domain.usecase.GetCategoriesUseCase
import com.namhs.cashnote.domain.usecase.UpdateCategoryUseCase
import com.namhs.cashnote.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class CategoryManageViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
    private val addCategory: AddCategoryUseCase,
    private val updateCategory: UpdateCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase
) : MviViewModel<CategoryManageUiState, CategoryManageIntent, CategoryManageSideEffect>(
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

            CategoryManageIntent.AddClicked ->
                setState {
                    copy(editor = CategoryEditorState(color = categoryColorPalette.first()))
                }

            is CategoryManageIntent.CategoryClicked -> setState {
                copy(
                    editor = CategoryEditorState(
                        categoryId = intent.category.id,
                        name = intent.category.name,
                        color = intent.category.color
                    )
                )
            }

            is CategoryManageIntent.EditorNameChanged ->
                updateEditor { copy(name = intent.name, nameError = null) }

            is CategoryManageIntent.EditorColorSelected -> updateEditor {
                copy(color = intent.color)
            }

            CategoryManageIntent.EditorSaveClicked -> saveEditor()

            CategoryManageIntent.EditorDismissed -> {
                // 저장 중에는 닫지 않는다. 저장이 끝나면 알아서 닫힌다
                if (currentState.editor?.isSaving == true) return
                setState { copy(editor = null) }
            }

            CategoryManageIntent.EditorDeleteClicked -> updateEditor {
                if (isNew) this else copy(isConfirmingDelete = true)
            }

            CategoryManageIntent.DeleteConfirmed -> deleteEditingCategory()

            CategoryManageIntent.DeleteCancelled -> updateEditor {
                copy(isConfirmingDelete = false)
            }
        }
    }

    private fun deleteEditingCategory() {
        val state = currentState
        val editor = state.editor ?: return
        if (editor.isSaving) return
        val category = state.categories.firstOrNull { it.id == editor.categoryId } ?: return

        updateEditor { copy(isSaving = true, isConfirmingDelete = false) }
        viewModelScope.launch {
            try {
                when (val result = deleteCategory(category)) {
                    CategoryDeleteResult.Deleted -> setState { copy(editor = null) }

                    // 수정 다이얼로그는 그대로 두어 이름·색상만 바꾸는 쪽으로 이어갈 수 있게 한다
                    is CategoryDeleteResult.InUse -> {
                        updateEditor { copy(isSaving = false) }
                        postSideEffect(
                            CategoryManageSideEffect.ShowCategoryInUse(result.transactionCount)
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateEditor { copy(isSaving = false) }
                postSideEffect(
                    CategoryManageSideEffect.ShowMessage(R.string.category_delete_failed)
                )
            }
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

    private fun updateEditor(reduce: CategoryEditorState.() -> CategoryEditorState) {
        setState { copy(editor = editor?.reduce()) }
    }

    private fun saveEditor() {
        val state = currentState
        val editor = state.editor ?: return
        if (editor.isSaving) return

        val name = editor.name.trim()
        val nameError = validateName(name, editor.categoryId, state.categories)
        if (nameError != null) {
            updateEditor { copy(nameError = nameError) }
            return
        }

        updateEditor { copy(isSaving = true) }
        viewModelScope.launch {
            try {
                if (editor.categoryId == null) {
                    // 아이콘은 아직 화면에서 쓰지 않아 기본 카테고리의 기타와 같은 키를 둔다
                    addCategory(Category(0, name, state.type, ICON_DEFAULT, editor.color))
                } else {
                    // 유형은 바꾸지 않는다. 이미 이 카테고리를 쓰는 거래의 유형과 어긋나기 때문이다
                    val original = state.categories.first { it.id == editor.categoryId }
                    updateCategory(original.copy(name = name, color = editor.color))
                }
                setState { copy(editor = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateEditor { copy(isSaving = false) }
                postSideEffect(CategoryManageSideEffect.ShowMessage(R.string.category_save_failed))
            }
        }
    }

    companion object {
        const val ICON_DEFAULT = "etc"

        /** 비었거나, 같은 유형에서 자기 자신이 아닌 카테고리와 이름이 같으면 오류 */
        internal fun validateName(
            name: String,
            categoryId: Long?,
            categories: List<Category>
        ): CategoryNameError? = when {
            name.isBlank() -> CategoryNameError.EMPTY
            categories.any { it.id != categoryId && it.name == name } -> CategoryNameError.DUPLICATE
            else -> null
        }
    }
}
