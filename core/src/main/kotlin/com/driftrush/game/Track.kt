package com.driftrush.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Rectangle
import com.driftrush.DriftRushGame
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Один відрізок траси у світовому просторі
data class TrackSegment(
    val start: Vector2,
    val end: Vector2,
    val direction: Vector2,     // нормалізований напрям
    val normal: Vector2,        // перпендикуляр (ліво від напряму)
    val width: Float,
    val blockType: BlockType
)

class Track(private val blocks: List<TrackBlock>, private val isTutorial: Boolean = false) {

    companion object {
        const val BASE_WIDTH = 160f        // ширина траси
        const val SEGMENT_LENGTH = 300f    // довжина одного прямого сегмента
        const val TURN_SEGMENTS = 12       // кількість мікро-сегментів у повороті
    }

    val segments = mutableListOf<TrackSegment>()
    val totalLength: Float get() = segments.size * (SEGMENT_LENGTH / TURN_SEGMENTS)

    // Позиції бонусів (монети)
    val coinPositions = mutableListOf<Vector2>()
    // Позиції boost (зірочки)
    val boostPositions = mutableListOf<Vector2>()

    // Де фініш
    var finishLine: Vector2 = Vector2()
    var finishDirection: Vector2 = Vector2(0f, 1f)

    init {
        buildTrack()
        placePickups()
    }

    private fun buildTrack() {
        var pos = Vector2(0f, 0f)
        var angle = 90f // дивимось вверх (північ)

        fun rad(deg: Float) = deg * Math.PI.toFloat() / 180f

        fun addSeg(width: Float, type: BlockType) {
            val dir = Vector2(cos(rad(angle)), sin(rad(angle)))
            val normal = Vector2(-dir.y, dir.x)
            val end = Vector2(pos.x + dir.x * SEGMENT_LENGTH, pos.y + dir.y * SEGMENT_LENGTH)
            segments.add(TrackSegment(pos.cpy(), end.cpy(), dir.cpy(), normal.cpy(), width, type))
            pos.set(end)
        }

        fun addTurn(leftTurn: Boolean, sharp: Boolean) {
            val totalAngle = if (sharp) 100f else 90f
            val steps = TURN_SEGMENTS
            val angleStep = if (leftTurn) (totalAngle / steps) else -(totalAngle / steps)
            val segLen = SEGMENT_LENGTH * 0.55f
            repeat(steps) {
                angle += angleStep
                val dir = Vector2(cos(rad(angle)), sin(rad(angle)))
                val normal = Vector2(-dir.y, dir.x)
                val end = Vector2(pos.x + dir.x * segLen, pos.y + dir.y * segLen)
                val type = if (leftTurn) BlockType.TURN_LEFT else BlockType.TURN_RIGHT
                segments.add(TrackSegment(pos.cpy(), end.cpy(), dir.cpy(), normal.cpy(), BASE_WIDTH, type))
                pos.set(end)
            }
        }

        for (block in blocks) {
            val w = if (block.type == BlockType.WIDE_STRAIGHT) BASE_WIDTH * 1.35f else BASE_WIDTH
            when (block.type) {
                BlockType.STRAIGHT       -> repeat(3) { addSeg(w, BlockType.STRAIGHT) }
                BlockType.WIDE_STRAIGHT  -> repeat(3) { addSeg(w, BlockType.WIDE_STRAIGHT) }
                BlockType.TURN_LEFT      -> addTurn(true, false)
                BlockType.TURN_RIGHT     -> addTurn(false, false)
                BlockType.SHARP_LEFT     -> addTurn(true, true)
                BlockType.SHARP_RIGHT    -> addTurn(false, true)
                BlockType.ZIGZAG_LEFT    -> { addTurn(false, false); addTurn(true, false) }
                BlockType.ZIGZAG_RIGHT   -> { addTurn(true, false); addTurn(false, false) }
            }
        }

        if (segments.isNotEmpty()) {
            val last = segments.last()
            finishLine.set(last.end)
            finishDirection.set(last.direction)
        }
    }

    private fun placePickups() {
        // Монети кожні ~4 сегменти на центрі траси
        for (i in segments.indices step 4) {
            val seg = segments[i]
            val mid = Vector2(
                (seg.start.x + seg.end.x) / 2f,
                (seg.start.y + seg.end.y) / 2f
            )
            // Зсув вправо або вліво випадково
            val offset = (Random.nextFloat() - 0.5f) * seg.width * 0.5f
            coinPositions.add(Vector2(mid.x + seg.normal.x * offset, mid.y + seg.normal.y * offset))
        }
        // Boost кожні ~15 сегментів
        for (i in segments.indices step 15) {
            val seg = segments[i]
            val mid = Vector2(
                (seg.start.x + seg.end.x) / 2f,
                (seg.start.y + seg.end.y) / 2f
            )
            boostPositions.add(mid.cpy())
        }
    }

    // Знайти найближчий сегмент до позиції
    fun getSegmentAt(worldPos: Vector2, searchRadius: Float = 800f): TrackSegment? {
        var best: TrackSegment? = null
        var bestDist = Float.MAX_VALUE
        for (seg in segments) {
            // Проекція точки на відрізок
            val ab = Vector2(seg.end.x - seg.start.x, seg.end.y - seg.start.y)
            val ap = Vector2(worldPos.x - seg.start.x, worldPos.y - seg.start.y)
            val lenSq = ab.x * ab.x + ab.y * ab.y
            val t = if (lenSq > 0f) (ap.dot(ab) / lenSq).coerceIn(0f, 1f) else 0f
            val closest = Vector2(seg.start.x + ab.x * t, seg.start.y + ab.y * t)
            val d = worldPos.dst(closest)
            if (d < bestDist && d < searchRadius) {
                bestDist = d
                best = seg
            }
        }
        return best
    }

    // Перевіряємо чи машина на трасі
    fun isOnTrack(worldPos: Vector2): Boolean {
        val seg = getSegmentAt(worldPos) ?: return false
        // Проекція позиції на нормаль сегмента
        val toPos = Vector2(worldPos.x - seg.start.x, worldPos.y - seg.start.y)
        val lateralDist = Math.abs(toPos.dot(seg.normal))
        return lateralDist < seg.width / 2f + 20f
    }

    // Відстань від краю (0 = центр, 1 = самий край)
    fun getEdgeRatio(worldPos: Vector2): Float {
        val seg = getSegmentAt(worldPos) ?: return 0f
        val toPos = Vector2(worldPos.x - seg.start.x, worldPos.y - seg.start.y)
        val lateralDist = Math.abs(toPos.dot(seg.normal))
        return (lateralDist / (seg.width / 2f)).coerceIn(0f, 1f)
    }

    // Прогрес по трасі (0.0 = старт, 1.0 = фініш)
    fun getProgress(worldPos: Vector2): Float {
        if (segments.isEmpty()) return 0f
        var closest = 0
        var bestDist = Float.MAX_VALUE
        for (i in segments.indices) {
            val seg = segments[i]
            val mid = Vector2((seg.start.x + seg.end.x) / 2f, (seg.start.y + seg.end.y) / 2f)
            val d = mid.dst(worldPos)
            if (d < bestDist) { bestDist = d; closest = i }
        }
        return closest.toFloat() / segments.size.toFloat()
    }

    fun draw(shape: ShapeRenderer, camX: Float, camY: Float) {
        // Фон траси — малюємо кожен сегмент як прямокутник
        shape.begin(ShapeRenderer.ShapeType.Filled)

        for (seg in segments) {
            val hw = seg.width / 2f
            // Чотири кути сегмента
            val p1 = Vector2(seg.start.x + seg.normal.x * hw, seg.start.y + seg.normal.y * hw)
            val p2 = Vector2(seg.start.x - seg.normal.x * hw, seg.start.y - seg.normal.y * hw)
            val p3 = Vector2(seg.end.x - seg.normal.x * hw, seg.end.y - seg.normal.y * hw)
            val p4 = Vector2(seg.end.x + seg.normal.x * hw, seg.end.y + seg.normal.y * hw)

            // Основна траса — світло-сірий асфальт
            shape.setColor(0.85f, 0.85f, 0.85f, 1f)
            shape.triangle(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
            shape.triangle(p1.x, p1.y, p3.x, p3.y, p4.x, p4.y)
        }

        // Бортики — помаранчеві/червоні смуги
        shape.setColor(0.95f, 0.4f, 0.1f, 1f)
        for (seg in segments) {
            val hw = seg.width / 2f
            val bw = 12f
            // Лівий бортик
            val lOut = Vector2(seg.start.x + seg.normal.x * hw, seg.start.y + seg.normal.y * hw)
            val lIn  = Vector2(seg.start.x + seg.normal.x * (hw - bw), seg.start.y + seg.normal.y * (hw - bw))
            val lOut2 = Vector2(seg.end.x + seg.normal.x * hw, seg.end.y + seg.normal.y * hw)
            val lIn2  = Vector2(seg.end.x + seg.normal.x * (hw - bw), seg.end.y + seg.normal.y * (hw - bw))
            shape.triangle(lOut.x, lOut.y, lIn.x, lIn.y, lIn2.x, lIn2.y)
            shape.triangle(lOut.x, lOut.y, lIn2.x, lIn2.y, lOut2.x, lOut2.y)
            // Правий бортик
            val rOut = Vector2(seg.start.x - seg.normal.x * hw, seg.start.y - seg.normal.y * hw)
            val rIn  = Vector2(seg.start.x - seg.normal.x * (hw - bw), seg.start.y - seg.normal.y * (hw - bw))
            val rOut2 = Vector2(seg.end.x - seg.normal.x * hw, seg.end.y - seg.normal.y * hw)
            val rIn2  = Vector2(seg.end.x - seg.normal.x * (hw - bw), seg.end.y - seg.normal.y * (hw - bw))
            shape.triangle(rOut.x, rOut.y, rIn.x, rIn.y, rIn2.x, rIn2.y)
            shape.triangle(rOut.x, rOut.y, rIn2.x, rIn2.y, rOut2.x, rOut2.y)
        }

        // Центральна пунктирна лінія
        shape.setColor(0.9f, 0.85f, 0.2f, 0.7f)
        var dashCount = 0
        for (seg in segments) {
            if (dashCount % 3 == 0) {
                val mid1 = seg.start
                val mid2 = seg.end
                val dw = 4f
                val n = seg.normal
                shape.triangle(mid1.x + n.x * dw, mid1.y + n.y * dw,
                                mid1.x - n.x * dw, mid1.y - n.y * dw,
                                mid2.x - n.x * dw, mid2.y - n.y * dw)
                shape.triangle(mid1.x + n.x * dw, mid1.y + n.y * dw,
                                mid2.x - n.x * dw, mid2.y - n.y * dw,
                                mid2.x + n.x * dw, mid2.y + n.y * dw)
            }
            dashCount++
        }

        // Фінішна лінія
        if (segments.isNotEmpty()) {
            val last = segments.last()
            val hw = last.width / 2f
            shape.setColor(0.2f, 0.8f, 0.2f, 1f)
            val fl = last.end
            val n = last.normal
            val fw = 10f
            val d = last.direction
            shape.triangle(fl.x + n.x * hw, fl.y + n.y * hw,
                            fl.x - n.x * hw, fl.y - n.y * hw,
                            fl.x + d.x * fw - n.x * hw, fl.y + d.y * fw - n.y * hw)
            shape.triangle(fl.x + n.x * hw, fl.y + n.y * hw,
                            fl.x + d.x * fw - n.x * hw, fl.y + d.y * fw - n.y * hw,
                            fl.x + d.x * fw + n.x * hw, fl.y + d.y * fw + n.y * hw)
        }

        shape.end()
    }

    fun drawPickups(shape: ShapeRenderer, collectedCoins: Set<Int>, collectedBoosts: Set<Int>) {
        shape.begin(ShapeRenderer.ShapeType.Filled)

        // Монети — жовті кола
        shape.setColor(1f, 0.85f, 0.1f, 1f)
        for ((i, pos) in coinPositions.withIndex()) {
            if (i !in collectedCoins) {
                shape.circle(pos.x, pos.y, 14f, 8)
            }
        }

        // Boost — сині зірки (прості кола з обводкою)
        shape.setColor(0.2f, 0.6f, 1f, 1f)
        for ((i, pos) in boostPositions.withIndex()) {
            if (i !in collectedBoosts) {
                shape.circle(pos.x, pos.y, 18f, 6)
            }
        }
        shape.end()

        // Обводка монет
        shape.begin(ShapeRenderer.ShapeType.Line)
        shape.setColor(0.8f, 0.6f, 0f, 1f)
        for ((i, pos) in coinPositions.withIndex()) {
            if (i !in collectedCoins) {
                shape.circle(pos.x, pos.y, 14f, 8)
            }
        }
        shape.end()
    }
}
