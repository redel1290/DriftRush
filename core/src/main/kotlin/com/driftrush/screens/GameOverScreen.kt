package com.driftrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.driftrush.DriftRushGame

class GameOverScreen(
    private val game: DriftRushGame,
    private val levelIndex: Int,
    private val isRandom: Boolean,
    private val score: Int,
    private val coinsEarned: Int,
    private val stars: Int,
    private val completed: Boolean
) : Screen {

    private val W = DriftRushGame.WORLD_WIDTH
    private val H = DriftRushGame.WORLD_HEIGHT
    private val shape = ShapeRenderer()
    private val cam = OrthographicCamera()

    private val btnRetry    = Rectangle(W/2f - 115f, H/2f - 80f,  230f, 62f)
    private val btnNext     = Rectangle(W/2f - 115f, H/2f - 160f, 230f, 62f)
    private val btnMenu     = Rectangle(W/2f - 115f, H/2f - 240f, 230f, 62f)

    private var animTimer = 0f

    init { cam.setToOrtho(false, W, H) }

    override fun show() {}

    override fun render(delta: Float) {
        animTimer += delta

        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        cam.update()
        shape.projectionMatrix = cam.combined
        game.batch.projectionMatrix = cam.combined

        shape.begin(ShapeRenderer.ShapeType.Filled)

        // Фон
        if (completed) {
            shape.setColor(0.1f, 0.25f, 0.12f, 1f)
        } else {
            shape.setColor(0.22f, 0.1f, 0.1f, 1f)
        }
        shape.rect(0f, 0f, W, H)

        // Декоративне коло
        val pulse = 0.92f + 0.08f * Math.sin(animTimer * 2.5).toFloat()
        if (completed) shape.setColor(0.2f, 0.75f, 0.35f, 0.2f)
        else shape.setColor(0.85f, 0.2f, 0.2f, 0.2f)
        shape.circle(W/2f, H/2f + 60f, 180f * pulse, 32)

        // Картка результату
        shape.setColor(1f, 1f, 1f, 0.06f)
        shape.roundedRect(W/2f - 160f, H/2f - 280f, 320f, 420f, 20f)

        // Кнопки
        if (completed) shape.setColor(0.2f, 0.75f, 0.35f, 1f)
        else shape.setColor(0.85f, 0.3f, 0.2f, 1f)
        shape.roundedRect(btnRetry.x, btnRetry.y, btnRetry.width, btnRetry.height, 14f)

        if (completed && !isRandom && levelIndex < 500) {
            shape.setColor(0.25f, 0.55f, 0.95f, 1f)
            shape.roundedRect(btnNext.x, btnNext.y, btnNext.width, btnNext.height, 14f)
        }

        shape.setColor(0.35f, 0.35f, 0.4f, 1f)
        shape.roundedRect(btnMenu.x, btnMenu.y, btnMenu.width, btnMenu.height, 14f)

        shape.end()

        game.batch.begin()

        // Заголовок
        if (completed) {
            game.fontLarge.setColor(0.3f, 1f, 0.5f, 1f)
            game.fontLarge.draw(game.batch, "ФІНІШ!", W/2f - 72f, H - 120f)
        } else {
            game.fontLarge.setColor(1f, 0.35f, 0.25f, 1f)
            game.fontLarge.draw(game.batch, "АВАРІЯ", W/2f - 78f, H - 120f)
        }

        // Зірки (тільки якщо пройшов)
        if (completed) {
            val starStr = when (stars) {
                3 -> "***  "
                2 -> "**o  "
                else -> "*oo  "
            }
            game.fontLarge.setColor(1f, 0.88f, 0.2f, 1f)
            game.fontLarge.draw(game.batch, starStr, W/2f - 72f, H - 180f)
        }

        // Очки
        game.fontMedium.setColor(1f, 1f, 1f, 1f)
        game.fontMedium.draw(game.batch, "Очки: $score", W/2f - 80f, H/2f + 160f)

        // Монети
        game.fontMedium.setColor(1f, 0.85f, 0.2f, 1f)
        game.fontMedium.draw(game.batch, "+$coinsEarned [M]", W/2f - 60f, H/2f + 110f)

        // Рівень
        game.fontSmall.setColor(0.75f, 0.75f, 0.75f, 1f)
        val lvlTxt = if (isRandom) "Рандом режим" else "Рівень $levelIndex"
        game.fontSmall.draw(game.batch, lvlTxt, W/2f - 72f, H/2f + 72f)

        // Загальні монети
        game.fontSmall.setColor(1f, 0.85f, 0.2f, 1f)
        game.fontSmall.draw(game.batch, "[M] ${game.coins} всього", W/2f - 70f, H/2f + 44f)

        // Кнопки текст
        game.fontMedium.setColor(1f, 1f, 1f, 1f)
        val retryTxt = if (completed) "ЩЕ РАЗ" else "ПОВТОР"
        game.fontMedium.draw(game.batch, retryTxt, btnRetry.x + 54f, btnRetry.y + 42f)

        if (completed && !isRandom && levelIndex < 500) {
            game.fontMedium.setColor(1f, 1f, 1f, 1f)
            game.fontMedium.draw(game.batch, "ДАЛІ >", btnNext.x + 54f, btnNext.y + 42f)
        }

        game.fontMedium.setColor(0.9f, 0.9f, 0.9f, 1f)
        game.fontMedium.draw(game.batch, "МЕНЮ", btnMenu.x + 70f, btnMenu.y + 42f)

        game.batch.end()

        // Тапи
        if (Gdx.input.justTouched()) {
            val t = cam.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
            when {
                btnRetry.contains(t.x, t.y) ->
                    game.setScreen(GameScreen(game, levelIndex, isRandom))

                btnNext.contains(t.x, t.y) && completed && !isRandom && levelIndex < 500 ->
                    game.setScreen(GameScreen(game, levelIndex + 1, false))

                btnMenu.contains(t.x, t.y) ->
                    game.setScreen(MenuScreen(game))
            }
        }
    }

    override fun resize(w: Int, h: Int) { cam.setToOrtho(false, W, H) }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { shape.dispose() }
}
