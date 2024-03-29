package ba.ahavic.artistfy.ui.base.viewmodel

import android.os.Bundle
import androidx.lifecycle.*
import androidx.navigation.NavDirections
import ba.ahavic.artistfy.ui.base.AppError
import ba.ahavic.artistfy.ui.base.AppException
import ba.ahavic.artistfy.ui.base.ReasonOfError
import ba.ahavic.artistfy.ui.base.SingleLiveEvent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

abstract class BaseViewModel : ViewModel(), LifecycleObserver {

    private val _error = SingleLiveEvent<BaseError>()
    val error: LiveData<BaseError>
        get() = _error

    private val _loading = SingleLiveEvent<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _loading

    private val _navigationAction = SingleLiveEvent<NavigationAction>()
    val navigationAction: LiveData<NavigationAction> = _navigationAction

    var arguments: Bundle = Bundle()

    /**
     * [_coroutineExceptionHandler] context element is used as generic catch block of coroutine.
     */
    private var _coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        isLoading(false)
        if (exception is AppException) defaultErrorHandler(exception.appError)
        else throw exception
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    protected open fun onLifeCycleOwnerResume() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    protected open fun onLifeCycleOwnerPause() {
    }

    protected fun setError(error: BaseError) {
        // log error
        _error.value = error
    }

    protected fun isLoading(isLoading: Boolean) {
        _loading.postValue(isLoading)
    }

    protected fun navigate(navAction: NavigationAction) {
        _navigationAction.value = navAction
    }

    protected fun navigate(navDirections: NavDirections) {
        _navigationAction.value = NavigationAction.To(navDirections)
    }

    /**
     * Override to provide specific feature error handling but don't forget to call super!
     */
    protected open fun defaultErrorHandler(appError: AppError) {
        _error.value = when (appError.reasonOfError) {
            ReasonOfError.ServerError -> ServerError
            ReasonOfError.UnKnownHost -> UnknownHostError
            else -> DefaultError
        }
    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit) {
        try {
            viewModelScope.launch(_coroutineExceptionHandler) {
                block()
            }
        } catch (exception: Exception) {
           throw exception
        } finally {
            if (isLoading.value != false) {
                isLoading(false)
            }
        }
    }
}

sealed class NavigationAction {
    data class To(val directions: NavDirections): NavigationAction()
    object Back: NavigationAction()
}