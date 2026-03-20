package com.driftrush.game

import kotlin.random.Random

object RandomTrackGenerator {

    // Середня довжина: 25-40 блоків
    fun generate(seed: Long = System.currentTimeMillis()): List<TrackBlock> {
        val rng = Random(seed)
        val blocks = mutableListOf<TrackBlock>()
        val targetLength = rng.nextInt(25, 40)

        // Завжди починаємо з широкої прямої
        blocks.add(TrackBlock(BlockType.WIDE_STRAIGHT))
        blocks.add(TrackBlock(BlockType.STRAIGHT))

        var consecutiveTurns = 0
        var lastType: BlockType? = null

        while (blocks.size < targetLength - 2) {
            val nextBlock = pickNextBlock(rng, lastType, consecutiveTurns)
            blocks.add(TrackBlock(nextBlock))

            if (nextBlock.isTurn()) {
                consecutiveTurns++
            } else {
                consecutiveTurns = 0
            }
            lastType = nextBlock
        }

        // Закінчуємо широкою прямою перед фінішем
        blocks.add(TrackBlock(BlockType.STRAIGHT))
        blocks.add(TrackBlock(BlockType.WIDE_STRAIGHT))

        return blocks
    }

    private fun pickNextBlock(
        rng: Random,
        last: BlockType?,
        consecutiveTurns: Int
    ): BlockType {
        // Не більше 2 поворотів підряд
        if (consecutiveTurns >= 2) {
            return if (rng.nextFloat() < 0.6f) BlockType.STRAIGHT else BlockType.WIDE_STRAIGHT
        }

        // Після зигзагу — обовʼязково пряма
        if (last == BlockType.ZIGZAG_LEFT || last == BlockType.ZIGZAG_RIGHT) {
            return BlockType.STRAIGHT
        }

        // Після різкого повороту — пряма
        if (last == BlockType.SHARP_LEFT || last == BlockType.SHARP_RIGHT) {
            return BlockType.STRAIGHT
        }

        val roll = rng.nextFloat()
        return when {
            roll < 0.20f -> BlockType.STRAIGHT
            roll < 0.30f -> BlockType.WIDE_STRAIGHT
            roll < 0.45f -> BlockType.TURN_LEFT
            roll < 0.60f -> BlockType.TURN_RIGHT
            roll < 0.70f -> BlockType.ZIGZAG_LEFT
            roll < 0.80f -> BlockType.ZIGZAG_RIGHT
            roll < 0.90f -> BlockType.SHARP_LEFT
            else         -> BlockType.SHARP_RIGHT
        }
    }

    private fun BlockType.isTurn() = this in listOf(
        BlockType.TURN_LEFT, BlockType.TURN_RIGHT,
        BlockType.SHARP_LEFT, BlockType.SHARP_RIGHT,
        BlockType.ZIGZAG_LEFT, BlockType.ZIGZAG_RIGHT
    )
}
