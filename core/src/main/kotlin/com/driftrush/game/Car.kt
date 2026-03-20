package com.driftrush.game

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.MathUtils
import kotlin.math.*

class Car(
    private val texture: Texture,
    private val speedMultiplier: Float = 1.0f,
    private val handlingMultiplier: Float = 1.0f
) {

    companion object {
        const val BASE_SPEED = 280f        // пікселів/сек
        const val MAX_DRIFT_ANGLE = 55f    // максимальний кут заносу
        const val DRIFT_RATE = 60f         // градусів/сек при утриманні
        const val STABILIZE_RATE = 120f    // градусів/сек при відпусканні
        const val CAR_WIDTH = 40f
        const val CAR_HEIGHT = 64f
        const val SMOKE_INTERVAL = 0.05f
    }

    var position = Vector2(0f, 0f)
    var velocityDir = Vector2(0f, 1f)   // куди рухається
    var facingAngle = 90f               // кут машини (градуси, 90 = вверх)
    var driftAngle = 0f                 // поточний кут дрифту
    var isDrifting = false
    var driftDirection = 0              // +1 = право, -1 = ліво
    var isAlive = true

    private var smokeTimer = 0f

    // Частинки диму
    val smokeParticles = mutableListOf<SmokeParticle>()
    // Слід
    val trailPoints = mutableListOf<Vector2>()
    private val MAX_TRAIL = 60

    // Поточна швидкість (зростає з часом)
    var currentSpeed = BASE_SPEED

    fun startDrift(right: Boolean) {
        if (!isAlive) return
        isDrifting = true
        driftDirection = if (right) 1 else -1
    }

    fun stopDrift() {
        isDrifting = false
        driftDirection = 0
    }

    fun update(delta: Float, track: Track) {
        if (!isAlive) return

        // Оновлюємо кут дрифту
        if (isDrifting) {
            val rate = DRIFT_RATE * handlingMultiplier * delta
            driftAngle = (driftAngle + rate * driftDirection)
                .coerceIn(-MAX_DRIFT_ANGLE, MAX_DRIFT_ANGLE)
        } else {
            // Стабілізація
            val rate = STABILIZE_RATE * delta
            if (abs(driftAngle) < rate) {
                driftAngle = 0f
            } else {
                driftAngle -= sign(driftAngle) * rate
            }
        }

        // Кут руху = кут машини + кут дрифту
        val totalAngle = facingAngle + driftAngle
        val rad = totalAngle * MathUtils.degreesToRadians
        velocityDir.set(cos(rad), sin(rad))

        // Рух вперед
        val speed = currentSpeed * speedMultiplier
        position.x += velocityDir.x * speed * delta
        position.y += velocityDir.y * speed * delta

        // Підлаштовуємо facingAngle до траси
        val seg = track.getSegmentAt(position)
        if (seg != null) {
            val targetAngle = MathUtils.atan2(seg.direction.y, seg.direction.x) * MathUtils.radiansToDegrees
            // Плавно повертаємо машину в напрям траси
            val turnRate = 80f * handlingMultiplier * delta
            val angleDiff = normalizeAngle(targetAngle - facingAngle)
            facingAngle += angleDiff.coerceIn(-turnRate, turnRate)
        }

        // Перевірка виходу за межі
        if (!track.isOnTrack(position)) {
            isAlive = false
        }

        // Дим при дрифті
        if (isDrifting && abs(driftAngle) > 15f) {
            smokeTimer += delta
            if (smokeTimer >= SMOKE_INTERVAL) {
                smokeTimer = 0f
                spawnSmoke()
            }
        }

        // Оновлення частинок диму
        val iter = smokeParticles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.update(delta)
            if (p.isDead) iter.remove()
        }

        // Слід
        trailPoints.add(position.cpy())
        if (trailPoints.size > MAX_TRAIL) trailPoints.removeAt(0)
    }

    private fun spawnSmoke() {
        val rad = facingAngle * MathUtils.degreesToRadians
        val behind = Vector2(-cos(rad), -sin(rad))
        // Два колеса
        val normal = Vector2(-behind.y, behind.x)
        val leftWheel = Vector2(
            position.x + behind.x * CAR_HEIGHT * 0.3f + normal.x * CAR_WIDTH * 0.35f,
            position.y + behind.y * CAR_HEIGHT * 0.3f + normal.y * CAR_WIDTH * 0.35f
        )
        val rightWheel = Vector2(
            position.x + behind.x * CAR_HEIGHT * 0.3f - normal.x * CAR_WIDTH * 0.35f,
            position.y + behind.y * CAR_HEIGHT * 0.3f - normal.y * CAR_WIDTH * 0.35f
        )
        smokeParticles.add(SmokeParticle(leftWheel))
        smokeParticles.add(SmokeParticle(rightWheel))
    }

    private fun normalizeAngle(a: Float): Float {
        var angle = a % 360f
        if (angle > 180f) angle -= 360f
        if (angle < -180f) angle += 360f
        return angle
    }

    fun draw(batch: SpriteBatch) {
        if (!isAlive) return
        val w = CAR_WIDTH * 1.8f
        val h = CAR_HEIGHT * 1.8f
        batch.draw(
            texture,
            position.x - w / 2f,
            position.y - h / 2f,
            w / 2f, h / 2f,
            w, h,
            1f, 1f,
            -(facingAngle - 90f),  // LibGDX rotate: 0° = вверх
            0, 0,
            texture.width, texture.height,
            false, false
        )
    }

    fun drawSmoke(shape: ShapeRenderer) {
        shape.begin(ShapeRenderer.ShapeType.Filled)
        for (p in smokeParticles) {
            shape.setColor(0.8f, 0.8f, 0.8f, p.alpha)
            shape.circle(p.x, p.y, p.size, 6)
        }
        shape.end()
    }

    fun drawTrail(shape: ShapeRenderer) {
        if (trailPoints.size < 2) return
        shape.begin(ShapeRenderer.ShapeType.Line)
        for (i in 1 until trailPoints.size) {
            val alpha = i.toFloat() / trailPoints.size * 0.4f
            shape.setColor(0.9f, 0.5f, 0.1f, alpha)
            shape.line(trailPoints[i-1].x, trailPoints[i-1].y,
                       trailPoints[i].x, trailPoints[i].y)
        }
        shape.end()
    }
}

// Частинка диму
class SmokeParticle(x: Float, y: Float) {
    var x = x; var y = y
    var size = 8f
    var alpha = 0.5f
    var vx = (Math.random().toFloat() - 0.5f) * 30f
    var vy = (Math.random().toFloat() - 0.5f) * 30f
    var isDead = false

    constructor(pos: Vector2) : this(pos.x, pos.y)

    fun update(delta: Float) {
        x += vx * delta
        y += vy * delta
        size += 20f * delta
        alpha -= 1.2f * delta
        if (alpha <= 0f) { alpha = 0f; isDead = true }
    }
}
