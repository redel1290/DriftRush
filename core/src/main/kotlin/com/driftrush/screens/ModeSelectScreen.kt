package com.driftrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.driftrush.DriftRushGame

class ModeSelectScreen(private val game: DriftRushGame) : Screen {

    private val shape = ShapeRenderer()
    private val W = DriftRushGame.WORLD_WIDTH
    private val H = DriftRushGame.WORLD_HEIGHT

    private val cam = com.badlogic.gdx.graphics.OrthographicCamera()

    // Ліва половина — Фіксовані рівні, права — Рандом
    private val fixedZone  = Rectangle(0f, 0f, W/2f, H)
    private val randomZone = Rectangle(W/2f, 0f, W/2f, H)

    private val randomLocked get() = game.completedLevels < 20

    init {
        cam.setToOrtho(false, W, H)
    }

    override fun show() {}

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        cam.update()
        shape.projectionMatrix = cam.combined
        game.batch.projectionMatrix = cam.combined

        shape.begin(ShapeRenderer.ShapeType.Filled)

        // === ЛІВА ПОЛОВИНА — Фіксовані рівні ===
        shape.setColor(0.25f, 0.6f, 0.95f, 1f)
        shape.rect(0f, 0f, W/2f, H)

        // Декор — дорожні лінії
        shape.setColor(1f, 1f, 1f, 0.15f)
        for (i in 0..5) {
            shape.rect(W/4f - 6f, i * 160f + 20f, 12f, 100f)
        }

        // === ПРАВА ПОЛОВИНА — Рандом ===
        if (randomLocked) {
            shape.setColor(0.55f, 0.55f, 0.6f, 1f)
        } else {
            shape.setColor(0.95f, 0.45f, 0.15f, 1f)
        }
        shape.rect(W/2f, 0f, W/2f, H)

        // Декор
        shape.setColor(1f, 1f, 1f, 0.1f)
        for (i in 0..5) {
            shape.circle(W * 0.75f, i * 180f - 40f, 50f, 16)
        }

        // Роздільник
        shape.setColor(1f, 1f, 1f, 0.9f)
        shape.rect(W/2f - 2f, 0f, 4f, H)

        shape.end()

        // Текст
        game.batch.begin()

        // === Ліва — Фіксовані ===
        game.fontLarge.setColor(1f, 1f, 1f, 1f)
        game.fontLarge.draw(game.batch, "500", 30f, H - 120f)
        game.fontMedium.setColor(1f, 1f, 1f, 1f)
        game.fontMedium.draw(game.batch, "РІВНІВ", 22f, H - 160f)

        game.fontSmall.setColor(1f, 1f, 1f, 0.9f)
        val fixedDesc = "Фіксовані карти — кожен\nрівень унікальний і\nретельно збалансований.\nПочинаєш з простих,\nдалі складніше!\n\nТвій прогрес: ${game.completedLevels}/500"
        var y = H - 230f
        for (line in fixedDesc.split("\n")) {
            game.fontSmall.draw(game.batch, line, 18f, y)
            y -= 32f
        }

        // Поточний рівень
        game.fontMedium.setColor(0.9f, 1f, 0.7f, 1f)
        game.fontMedium.draw(game.batch, "Рівень ${game.unlockedLevels}", 20f, H/2f - 60f)

        // === Права — Рандом ===
        if (randomLocked) {
            game.fontLarge.setColor(1f, 1f, 1f, 0.5f)
            game.fontLarge.draw(game.batch, "РАНДОМ", W/2f + 8f, H - 120f)

            game.fontMedium.setColor(1f, 1f, 1f, 0.7f)
            game.fontMedium.draw(game.batch, "РЕЖИМ", W/2f + 24f, H - 160f)

            // Замок
            game.fontLarge.setColor(1f, 1f, 1f, 0.6f)
            game.fontLarge.draw(game.batch, "\uD83D\uDD12", W/2f + 76f, H/2f + 30f)

            game.fontSmall.setColor(1f, 1f, 1f, 0.85f)
            val lockDesc = "Щоразу нова траса —\nповна несподіванка!\n\nЗАБЛОКОВАНО\nПройди ${20 - game.completedLevels}\nрівнів для розблокування"
            var yR = H - 230f
            for (line in lockDesc.split("\n")) {
                game.fontSmall.draw(game.batch, line, W/2f + 12f, yR)
                yR -= 32f
            }
        } else {
            game.fontLarge.setColor(1f, 1f, 1f, 1f)
            game.fontLarge.draw(game.batch, "РАНДОМ", W/2f + 8f, H - 120f)

            game.fontMedium.setColor(1f, 1f, 1f, 1f)
            game.fontMedium.draw(game.batch, "РЕЖИМ", W/2f + 24f, H - 160f)

            game.fontSmall.setColor(1f, 1f, 1f, 0.9f)
            val randDesc = "Щоразу нова траса —\nповна несподіванка!\n\nКожен старт генерує\nунікальну карту.\nЧи зможеш пройти\nнезнайому трасу?"
            var yR = H - 230f
            for (line in randDesc.split("\n")) {
                game.fontSmall.draw(game.batch, line, W/2f + 12f, yR)
                yR -= 32f
            }

            game.fontMedium.setColor(0.9f, 1f, 0.7f, 1f)
            game.fontMedium.draw(game.batch, "ГРАТИ!", W/2f + 52f, H/2f - 60f)
        }

        // Кнопка назад
        game.fontSmall.setColor(1f, 1f, 1f, 0.8f)
        game.fontSmall.draw(game.batch, "< НАЗАД", 16f, 45f)

        game.batch.end()

        // Обробка тапів
        if (Gdx.input.justTouched()) {
            val touch = cam.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))

            // Назад
            if (touch.y < 60f && touch.x < 120f) {
                game.setScreen(MenuScreen(game))
                return
            }

            when {
                fixedZone.contains(touch.x, touch.y) -> {
                    // Фіксований режим — відкрити вибір рівня
                    game.setScreen(LevelSelectScreen(game))
                }
                randomZone.contains(touch.x, touch.y) && !randomLocked -> {
                    // Рандом режим
                    game.setScreen(GameScreen(game, -1, true))
                }
            }
        }
    }

    override fun resize(width: Int, height: Int) { cam.setToOrtho(false, W, H) }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { shape.dispose() }
}
