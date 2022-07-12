package ru.ezhov.rocket.action.plugin.noteonfile.event

//Должен создаваться в рамках панели, чтоб не аффектить другие панели
class EventObserver {
    private val listTextLoadingListener: MutableList<TextLoadingListener> = mutableListOf()
    private val listTextChangingListener: MutableList<TextChangingListener> = mutableListOf()
    private val listTextSavingListener: MutableList<TextSavingListener> = mutableListOf()

    fun notifyTextLoading(text: String) {
        listTextLoadingListener.forEach { it.loading(text) }
    }

    fun register(listener: TextLoadingListener) {
        listTextLoadingListener.add(listener)
    }

    fun notifyTextSaving(text: String) {
        listTextSavingListener.forEach { it.saving(text) }
    }

    fun register(listener: TextSavingListener) {
        listTextSavingListener.add(listener)
    }

    fun notifyTextChanging(text: String) {
        listTextChangingListener.forEach { it.changing(text) }
    }

    fun register(listener: TextChangingListener) {
        listTextChangingListener.add(listener)
    }
}
