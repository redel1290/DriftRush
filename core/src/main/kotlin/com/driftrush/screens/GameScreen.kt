package com.driftrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.driftrush.DriftRushGame
import com.driftrush.CAR_DATA
import com.driftrush.game.*

class GameScreen(
    private val game: DriftRushGame,
    private val levelIndex: Int,
    private val isRandom: Boolean
) : Screen {

    private val W = DriftRushGame.WORLD_WIDTH
    private val H = DriftRushGame.WORLD_HEIGHT

    private val shape = ShapeRenderer()
    private val worldCam = OrthographicCamera()
    private val hudCam = OrthographicCamera()

    private val track: Track
    private val car: Car
    private val scoreManager = ScoreManager()

    private val isTutorial = levelIndex == 1 && !isRandom
    private var gameState = State.INTRO
    private var countdownTimer = if (isTutorial) 99f else 3f
    private var tutorialStep = 0
    private var finishHandled = false

    private val collectedCoins  = mutableSetOf<Int>()
    private val collectedBoosts = mutableSetOf<Int>()

    private var shakeTimer = 0f
    private var shakeAmount = 0f
    private var edgeTimer = 0f
    private var speedLevel = 1f
    private var distanceTraveled = 0f

    enum class State { INTRO, PLAYING, DEAD, FINISHED }

    init {
        worldCam.setToOrtho(false, W, H)
        hudCam.setToOrtho(false, W, H)

        val blocks = if (isRandom) RandomTrackGenerator.generate() else LevelData.getLevel(levelIndex)
        track = Track(blocks, isTutorial)

        val carData = CAR_DATA.find { it.assetName == game.selectedCar } ?: CAR_DATA[0]
        val tex = game.assets.get("${carData.assetName}.png", Texture::class.java)
        car = Car(tex, carData.speed, carData.handling)

        if (track.segments.isNotEmpty()) {
            val s0 = track.segments[0]
            car.position.set(s0.start)
            car.facingAngle = MathUtils.atan2(s0.direction.y, s0.direction.x) * MathUtils.radiansToDegrees
        }
    }

    override fun show() {}

    override fun render(delta: Float) {
        val dt = delta.coerceAtMost(0.05f)

        Gdx.gl.glClearColor(0.72f, 0.88f, 0.58f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Input
        when (gameState) {
            State.INTRO -> {
                if (Gdx.input.justTouched()) {
                    if (isTutorial && tutorialStep < 3) {
                        tutorialStep++
                        if (tutorialStep >= 3) { countdownTimer = 3f }
                    } else if (!isTutorial) {
                        // нічого — відлік йде сам
                    }
                }
                if (!isTutorial || tutorialStep >= 3) {
                    countdownTimer -= dt
                    if (countdownTimer <= 0f) gameState = State.PLAYING
                }
            }
            State.PLAYING -> {
                if (Gdx.input.isTouched) car.startDrift(Gdx.input.x > Gdx.graphics.width / 2)
                else car.stopDrift()
            }
            else -> {}
        }

        // Update
        if (gameState == State.PLAYING) {
            distanceTraveled += car.currentSpeed * dt
            speedLevel = 1f + distanceTraveled / 20000f
            car.currentSpeed = Car.BASE_SPEED * speedLevel
            car.update(dt, track)
            scoreManager.update(dt)

            if (!car.isAlive) {
                gameState = State.DEAD
                try { Gdx.input.vibrate(300) } catch (_: Exception) {}
                val coins = scoreManager.calculateCoinsEarned(levelIndex, false)
                game.addCoins(coins)
                game.setScreen(GameOverScreen(game, levelIndex, isRandom, scoreManager.score, coins, 0, false))
                return
            }

            track.coinPositions.forEachIndexed { i, pos ->
                if (i !in collectedCoins && car.position.dst(pos) < 32f) {
                    collectedCoins.add(i); scoreManager.onCoinCollected()
                }
            }
            track.boostPositions.forEachIndexed { i, pos ->
                if (i !in collectedBoosts && car.position.dst(pos) < 38f) {
                    collectedBoosts.add(i); scoreManager.onBoostCollected()
                    shakeTimer = 0.2f; shakeAmount = 5f
                }
            }
            edgeTimer += dt
            if (edgeTimer >= 0.3f) {
                edgeTimer = 0f
                val e = track.getEdgeRatio(car.position)
                if (e > 0.75f) { scoreManager.onEdgeBonus(e); shakeTimer = 0.06f; shakeAmount = 2f }
            }
            if (track.getEdgeRatio(car.position) < 0.25f && MathUtils.random() < 0.008f) {
                scoreManager.onCleanCorner()
            }
            if (shakeTimer > 0f) shakeTimer -= dt

            if (!finishHandled && car.position.dst(track.finishLine) < 90f) {
                finishHandled = true
                gameState = State.FINISHED
                val coins = scoreManager.calculateCoinsEarned(levelIndex, true)
                val stars = scoreManager.getStars(true)
                game.addCoins(coins)
                if (!isRandom && levelIndex >= game.unlockedLevels) game.unlockedLevels = levelIndex + 1
                if (!isRandom) game.completedLevels = maxOf(game.completedLevels, levelIndex)
                game.savePrefs()
                game.setScreen(GameOverScreen(game, levelIndex, isRandom, scoreManager.score, coins, stars, true))
                return
            }
        }

        // Camera
        val shake = if (shakeTimer > 0f) (MathUtils.random() - 0.5f) * shakeAmount * 2f else 0f
        worldCam.position.x += (car.position.x - worldCam.position.x) * 9f * dt + shake
        worldCam.position.y += (car.position.y + H * 0.22f - worldCam.position.y) * 9f * dt
        worldCam.update()
        hudCam.update()

        // World render
        shape.projectionMatrix = worldCam.combined
        game.batch.projectionMatrix = worldCam.combined

        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.setColor(0.68f, 0.85f, 0.54f, 1f)
        shape.rect(worldCam.position.x - W, worldCam.position.y - H, W*2f, H*2f)
        // Шахова текстура трави
        shape.setColor(0.63f, 0.80f, 0.50f, 0.35f)
        var gx = ((worldCam.position.x - W) / 60f).toInt() * 60f
        while (gx < worldCam.position.x + W) {
            var gy = ((worldCam.position.y - H) / 60f).toInt() * 60f
            while (gy < worldCam.position.y + H) {
                if (((gx/60).toInt() + (gy/60).toInt()) % 2 == 0) shape.rect(gx, gy, 60f, 60f)
                gy += 60f
            }
            gx += 60f
        }
        shape.end()

        track.draw(shape, worldCam.position.x, worldCam.position.y)
        track.drawPickups(shape, collectedCoins, collectedBoosts)
        car.drawSmoke(shape)
        car.drawTrail(shape)

        game.batch.begin()
        car.draw(game.batch)
        game.batch.end()

        val edgeR = track.getEdgeRatio(car.position)
        if (edgeR > 0.75f && gameState == State.PLAYING) {
            shape.begin(ShapeRenderer.ShapeType.Filled)
            shape.setColor(1f, 0.88f, 0.2f, 0.9f)
            repeat(4) {
                val ox = (MathUtils.random() - 0.5f) * 24f
                val oy = (MathUtils.random() - 0.5f) * 24f
                shape.circle(car.position.x + ox, car.position.y + oy, 2.5f + MathUtils.random()*2f, 4)
            }
            shape.end()
        }

        // HUD
        shape.projectionMatrix = hudCam.combined
        game.batch.projectionMatrix = hudCam.combined

        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.setColor(0f, 0f, 0f, 0.30f)
        shape.rect(0f, H - 84f, W, 84f)
        val comboRatio = (scoreManager.combo / 10f).coerceIn(0f, 1f)
        if (comboRatio > 0f) {
            shape.setColor(0.3f, 0.65f, 1f, 1f)
            shape.rect(10f, H - 82f, (W - 20f) * comboRatio, 7f)
        }
        val spdRatio = (speedLevel - 1f).coerceIn(0f, 1f)
        shape.setColor(0.95f, 0.45f, 0.15f, 0.75f)
        shape.rect(0f, 0f, W * spdRatio, 5f)
        shape.end()

        game.batch.begin()
        game.fontMedium.setColor(1f, 1f, 1f, 1f)
        game.fontMedium.draw(game.batch, "${scoreManager.score}", W/2f - 55f, H - 24f)
        if (scoreManager.combo > 1) {
            game.fontSmall.setColor(1f, 0.88f, 0.25f, 1f)
            game.fontSmall.draw(game.batch, "x${scoreManager.multiplier}  COMBO ${scoreManager.combo}", W/2f - 68f, H - 58f)
        }
        game.fontSmall.setColor(1f, 0.85f, 0.2f, 1f)
        game.fontSmall.draw(game.batch, "[M] ${scoreManager.coinsCollected}", 12f, H - 26f)
        game.fontSmall.setColor(0.9f, 0.9f, 0.9f, 1f)
        game.fontSmall.draw(game.batch, if (isRandom) "РАНДОМ" else "РВ $levelIndex", W - 102f, H - 26f)
        game.batch.end()

        // Intro overlay
        if (gameState == State.INTRO) {
            shape.begin(ShapeRenderer.ShapeType.Filled)
            shape.setColor(0f, 0f, 0f, 0.60f)
            shape.rect(0f, 0f, W, H)
            shape.end()

            game.batch.begin()
            if (isTutorial && tutorialStep < 3) {
                val tips = listOf(
                    "DRIFT RUSH\n \nМашина їде сама.\nТи керуєш заносом!\n \nТоркнись щоб далі...",
                    "УТРИМУЙ ПАЛЕЦЬ —\nмашина дрифтує!\n \nЧим довше тримаєш,\nтим крутіший занос.\n \nТоркнись щоб далі...",
                    "Їдь по КРАЮ\nдля бонусних очок!\n \nЗбирай монети [M]\nна трасі.\n \nТоркнись щоб почати!"
                )
                var ty = H/2f + 130f
                for (line in tips[tutorialStep].split("\n")) {
                    game.fontMedium.setColor(1f, 1f, 1f, 1f)
                    game.fontMedium.draw(game.batch, line, W/2f - 145f, ty)
                    ty -= 46f
                }
            } else {
                val c = (countdownTimer.toInt() + 1).coerceIn(1, 3)
                val txt = if (countdownTimer < 0.55f) "GO!" else "$c"
                game.fontLarge.setColor(1f, 0.88f, 0.25f, 1f)
                game.fontLarge.draw(game.batch, txt, W/2f - 30f, H/2f + 40f)
            }
            game.batch.end()
        }
    }

    override fun resize(w: Int, h: Int) {
        worldCam.setToOrtho(false, W, H)
        hudCam.setToOrtho(false, W, H)
    }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { shape.dispose() }
}
