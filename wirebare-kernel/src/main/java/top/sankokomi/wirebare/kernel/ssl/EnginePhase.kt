package top.sankokomi.wirebare.kernel.ssl

enum class EnginePhase {
    Initial,
    HandshakeStarted,
    HandshakeFinished,
    Closed,
    Unknown
}