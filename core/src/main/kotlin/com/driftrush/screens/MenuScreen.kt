package com.driftrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.driftrush.DriftRushGame
import com.driftrush.CAR_DATA

class MenuScreen(private val game: DriftRushGame) : Screen {

    private val shape = ShapeRenderer()
    private val W = DriftRushGame.WORLD_WIDTH
    private val H = DriftRushGame.WORLD_HEIGHT

    // Кнопки
    private val btnPlay    = Rectangle(W/2-120f, H/2-30f, 240f, 70f)
    private val btnModes   = Rectangle(W/2-120f, H/2-130f, 240f, 70f)
    private val btnCars    = Rectangle(W/2-120f, H/2-230f, 240f, 70f)

    private val cam = com.badlogic.gdx.graphics.OrthographicCamera()

    init {
        cam.setToOrtho(false, W, H)
    }

    override fun show() {
        game.loadPrefs()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.97f, 0.96f, 0.92f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        cam.update()
        shape.projectionMatrix = cam.combined

        // Фон — градієнт (підроблений через прямокутники)
        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.setColor(0.97f, 0.96f, 0.92f, 1f)
        shape.rect(0f, 0f, W, H)

        // Декоративні кола на фоні
        shape.setColor(1f, 0.88f, 0.7f, 0.4f)
        shape.circle(W * 0.15f, H * 0.8f, 120f, 32)
        shape.circle(W * 0.85f, H * 0.25f, 90f, 32)
        shape.setColor(0.7f, 0.88f, 1f, 0.3f)
        shape.circle(W * 0.5f, H * 0.6f, 200f, 32)

        // Топ-бар монети
        shape.setColor(1f, 0.85f, 0.15f, 1f)
        shape.roundedRect(W/2 - 90f, H - 75f, 180f, 50f, 12f)

        // Кнопка Play
        shape.setColor(0.2f, 0.75f, 0.35f, 1f)
        shape.roundedRect(btnPlay.x, btnPlay.y, btnPlay.width, btnPlay.height, 16f)

        // Кнопка Режими
        shape.setColor(0.25f, 0.55f, 0.95f, 1f)
        shape.roundedRect(btnModes.x, btnModes.y, btnModes.width, btnModes.height, 16f)

        // Кнопка Машини
        shape.setColor(0.9f, 0.4f, 0.15f, 1f)
        shape.roundedRect(btnCars.x, btnCars.y, btnCars.width, btnCars.height, 16f)

        shape.end()

        // Текст
        game.batch.projectionMatrix = cam.combined
        game.batch.begin()

        // Заголовок
        game.fontLarge.setColor(0.15f, 0.15f, 0.15f, 1f)
        game.fontLarge.draw(game.batch, "DRIFT", W/2 - 72f, H - 130f)
        game.fontLarge.setColor(0.9f, 0.4f, 0.1f, 1f)
        game.fontLarge.draw(game.batch, "RUSH", W/2 - 60f, H - 175f)

        // Монети
        game.fontMedium.setColor(0.3f, 0.2f, 0f, 1f)
        game.fontMedium.draw(game.batch, "[M] ${game.coins}", W/2 - 70f, H - 38f)

        // Поточна машина
        val carData = CAR_DATA.find { it.assetName == game.selectedCar }
        game.fontSmall.setColor(0.4f, 0.4f, 0.4f, 1f)
        game.fontSmall.draw(game.batch, "Авто: ${carData?.displayName ?: "-"}", W/2 - 80f, H/2 + 65f)

        // Рівень
        game.fontSmall.draw(game.batch, "Рівень: ${game.unlockedLevels}/500", W/2 - 80f, H/2 + 40f)

        // Кнопки текст
        game.fontMedium.setColor(1f, 1f, 1f, 1f)
        game.fontMedium.draw(game.batch, "ГРАТИ", btnPlay.x + 55f, btnPlay.y + 46f)
        game.fontMedium.draw(game.batch, "РЕЖИМ", btnModes.x + 45f, btnModes.y + 46f)
        game.fontMedium.draw(game.batch, "МАШИНИ", btnCars.x + 38f, btnCars.y + 46f)

        game.batch.end()

        // Обробка тапів
        if (Gdx.input.justTouched()) {
            val touch = cam.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
            when {
                btnPlay.contains(touch.x, touch.y) -> {
                    game.setScreen(GameScreen(game, game.unlockedLevels, false))
                }
                btnModes.contains(touch.x, touch.y) -> {
                    game.setScreen(ModeSelectScreen(game))
                }
                btnCars.contains(touch.x, touch.y) -> {
                    game.setScreen(CarSelectScreen(game))
                }
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        cam.setToOrtho(false, W, H)
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {
        shape.dispose()
    }
}

// Розширення для ShapeRenderer — заокруглений прямокутник
fun ShapeRenderer.roundedRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
    val segs = 8
    rect(x + r, y, w - 2*r, h)
    rect(x, y + r, w, h - 2*r)
    arc(x + r, y + r, r, 180f, 90f, segs)
    arc(x + w - r, y + r, r, 270f, 90f, segs)
    arc(x + w - r, y + h - r, r, 0f, 90f, segs)
    arc(x + r, y + h - r, r, 90f, 90f, segs)
}
