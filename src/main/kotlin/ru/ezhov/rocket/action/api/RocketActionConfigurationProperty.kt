package ru.ezhov.rocket.action.api

/**
 * Свойство действия
 */
interface RocketActionConfigurationProperty {
    /**
     * Должен быть уникальный в разрезе действия
     * @return ключ свойства
     */
    fun key(): String

    /**
     * @return название свойства для отображения
     */
    fun name(): String

    /**
     * @return описание свойства для отображения
     */
    fun description(): String

    /**
     * Это свойство подсказывает пользователю о необходимости заполнения, но не гарантирует заполненность.
     * @return обязательность заполнения свойства.
     */
    fun isRequired(): Boolean

    /**
     * Тип свойства, по-умолчанию [PropertyType.STRING]
     */
    fun type(): PropertyType = PropertyType.STRING

    /**
     * Значение по-умолчанию
     */
    fun default(): String?
}