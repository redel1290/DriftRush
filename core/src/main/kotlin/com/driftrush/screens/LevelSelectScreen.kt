package com.driftrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.driftrush.DriftRushGame

class LevelSelectScreen(private val game: DriftRushGame) : Screen {

    private val shape = ShapeRenderer()
    private val W = DriftRushGame.WORLD_WIDTH
    private val H = DriftRushGame.WORLD_HEIGHT
    private val cam = com.badlogic.gdx.graphics.OrthographicCamera()

    // Сітка: 5 колонок × N рядків, показуємо 30 рівнів на сторінці
    private val COLS = 5
    private val CELL_SIZE = 76f
    private val CELL_PAD = 12f
    private var page = 0           // 0 = рівні 1-30, 1 = 31-60, тощо
    private val LEVELS_PER_PAGE = 30

    init { cam.setToOrtho(false, W, H) }

    override fun show() {}

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.97f, 0.96f, 0.92f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        cam.update()
        shape.projectionMatrix = cam.combined
        game.batch.projectionMatrix = cam.combined

        val startLevel = page * LEVELS_PER_PAGE + 1
        val endLevel = minOf(startLevel + LEVELS_PER_PAGE - 1, 500)

        shape.begin(ShapeRenderer.ShapeType.Filled)

        val gridLeft = (W - COLS * (CELL_SIZE + CELL_PAD) + CELL_PAD) / 2f
        var idx = 0

        for (lvl in startLevel..endLevel) {
            val col = idx % COLS
            val row = idx / COLS
            val x = gridLeft + col * (CELL_SIZE + CELL_PAD)
            val y = H - 160f - row * (CELL_SIZE + CELL_PAD)

            val unlocked = lvl <= game.unlockedLevels
            val completed = lvl < game.unlockedLevels

            when {
                completed -> shape.setColor(0.3f, 0.78f, 0.45f, 1f)  // зелений — пройдено
                unlocked  -> shape.setColor(0.25f, 0.6f, 0.95f, 1f)  // синій — доступний
                else      -> shape.setColor(0.75f, 0.75f, 0.75f, 1f) // сірий — заблоковано
            }

            // Заокруглений квадрат
            shape.roundedRect(x, y, CELL_SIZE, CELL_SIZE, 10f)

            // Тінь
            if (unlocked) {
                shape.setColor(0f, 0f, 0f, 0.08f)
                shape.roundedRect(x + 2f, y - 2f, CELL_SIZE, CELL_SIZE, 10f)
            }

            idx++
        }

        // Кнопки сторінок
        if (page > 0) {
            shape.setColor(0.3f, 0.3f, 0.3f, 0.8f)
            shape.roundedRect(20f, H/2f - 30f, 50f, 60f, 8f)
        }
        if (endLevel < 500) {
            shape.setColor(0.3f, 0.3f, 0.3f, 0.8f)
            shape.roundedRect(W - 70f, H/2f - 30f, 50f, 60f, 8f)
        }

        shape.end()

        // Текст
        game.batch.begin()

        game.fontMedium.setColor(0.15f, 0.15f, 0.15f, 1f)
        game.fontMedium.draw(game.batch, "ВИБІР РІВНЯ", W/2f - 100f, H - 50f)

        game.fontSmall.setColor(0.4f, 0.4f, 0.4f, 1f)
        game.fontSmall.draw(game.batch, "Сторінка ${page+1} / ${(500 + LEVELS_PER_PAGE - 1) / LEVELS_PER_PAGE}", W/2f - 75f, H - 88f)

        val gridLeft2 = (W - COLS * (CELL_SIZE + CELL_PAD) + CELL_PAD) / 2f
        idx = 0
        for (lvl in startLevel..endLevel) {
            val col = idx % COLS
            val row = idx / COLS
            val x = gridLeft2 + col * (CELL_SIZE + CELL_PAD)
            val y = H - 160f - row * (CELL_SIZE + CELL_PAD)
            val unlocked = lvl <= game.unlockedLevels

            if (unlocked) {
                game.fontSmall.setColor(1f, 1f, 1f, 1f)
            } else {
                game.fontSmall.setColor(0.5f, 0.5f, 0.5f, 1f)
            }
            game.fontSmall.draw(game.batch, "$lvl", x + CELL_SIZE/2f - 16f, y + CELL_SIZE/2f + 10f)
            idx++
        }

        // Стрілки
        if (page > 0) {
            game.fontMedium.setColor(1f, 1f, 1f, 1f)
            game.fontMedium.draw(game.batch, "<", 28f, H/2f + 18f)
        }
        if (endLevel < 500) {
            game.fontMedium.setColor(1f, 1f, 1f, 1f)
            game.fontMedium.draw(game.batch, ">", W - 58f, H/2f + 18f)
        }

        // Назад
        game.fontSmall.setColor(0.4f, 0.4f, 0.4f, 1f)
        game.fontSmall.draw(game.batch, "< НАЗАД", 16f, 45f)

        game.batch.end()

        // Тапи
        if (Gdx.input.justTouched()) {
            val touch = cam.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))

            // Назад
            if (touch.y < 60f && touch.x < 120f) {
                game.setScreen(ModeSelectScreen(game)); return
            }
            // Стрілка ліво
            if (page > 0 && touch.x < 90f && touch.y in (H/2f-40f)..(H/2f+40f)) {
                page--; return
            }
            // Стрілка право
            if (endLevel < 500 && touch.x > W - 90f && touch.y in (H/2f-40f)..(H/2f+40f)) {
                page++; return
            }

            // Натиснення на рівень
            val gl = (W - COLS * (CELL_SIZE + CELL_PAD) + CELL_PAD) / 2f
            idx = 0
            for (lvl in startLevel..endLevel) {
                val col = idx % COLS
                val row = idx / COLS
                val x = gl + col * (CELL_SIZE + CELL_PAD)
                val y = H - 160f - row * (CELL_SIZE + CELL_PAD)
                val rect = Rectangle(x, y, CELL_SIZE, CELL_SIZE)
                if (rect.contains(touch.x, touch.y) && lvl <= game.unlockedLevels) {
                    game.setScreen(GameScreen(game, lvl, false))
                    return
                }
                idx++
            }
        }
    }

    override fun resize(w: Int, h: Int) { cam.setToOrtho(false, W, H) }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { shape.dispose() }
}
