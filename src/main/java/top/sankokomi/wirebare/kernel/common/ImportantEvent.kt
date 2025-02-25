package top.sankokomi.wirebare.kernel.common

class ImportantEvent(
    val message: String,
    val synopsis: EventSynopsis,
    val cause: Throwable?
)