package com.driftrush.game

enum class BlockType {
    STRAIGHT,       // пряма
    TURN_LEFT,      // поворот ліво 90°
    TURN_RIGHT,     // поворот право 90°
    ZIGZAG_LEFT,    // зигзаг починається ліво
    ZIGZAG_RIGHT,   // зигзаг починається право
    WIDE_STRAIGHT,  // широка пряма (легше)
    SHARP_LEFT,     // різкий поворот ліво
    SHARP_RIGHT     // різкий поворот право
}

data class TrackBlock(
    val type: BlockType,
    val length: Int = 1  // кількість юнітів довжини
)

// Визначення складності блоку (0.0 = легко, 1.0 = важко)
fun BlockType.difficulty(): Float = when (this) {
    BlockType.WIDE_STRAIGHT  -> 0.0f
    BlockType.STRAIGHT       -> 0.1f
    BlockType.TURN_LEFT      -> 0.4f
    BlockType.TURN_RIGHT     -> 0.4f
    BlockType.ZIGZAG_LEFT    -> 0.6f
    BlockType.ZIGZAG_RIGHT   -> 0.6f
    BlockType.SHARP_LEFT     -> 0.8f
    BlockType.SHARP_RIGHT    -> 0.8f
}
