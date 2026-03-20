package com.driftrush.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.driftrush.DriftRushGame
import com.driftrush.CAR_DATA

class CarSelectScreen(private val game: DriftRushGame) : Screen {

    private val shape = ShapeRenderer()
    private val W = DriftRushGame.WORLD_WIDTH
    private val H = DriftRushGame.WORLD_HEIGHT
    private val cam = com.badlogic.gdx.graphics.OrthographicCamera()

    private var selectedIdx = CAR_DATA.indexOfFirst { it.assetName == game.selectedCar }.coerceAtLeast(0)
    private var message = ""
    private var messageTimer = 0f

    init { cam.setToOrtho(false, W, H) }

    // Сітка: 3 колонки
    private val COLS = 3
    private val CELL_W = 130f
    private val CELL_H = 160f
    private val CELL_PAD = 12f

    override fun show() {}

    override fun render(delta: Float) {
        if (messageTimer > 0f) messageTimer -= delta

        Gdx.gl.glClearColor(0.97f, 0.96f, 0.92f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        cam.update()
        shape.projectionMatrix = cam.combined
        game.batch.projectionMatrix = cam.combined

        val gridLeft = (W - COLS * (CELL_W + CELL_PAD) + CELL_PAD) / 2f

        shape.begin(ShapeRenderer.ShapeType.Filled)

        // Фон
        shape.setColor(0.97f, 0.96f, 0.92f, 1f)
        shape.rect(0f, 0f, W, H)

        // Монети — топ бар
        shape.setColor(1f, 0.85f, 0.15f, 1f)
        shape.roundedRect(W/2f - 100f, H - 72f, 200f, 48f, 12f)

        // Клітинки машин
        for ((i, car) in CAR_DATA.withIndex()) {
            val col = i % COLS
            val row = i / COLS
            val x = gridLeft + col * (CELL_W + CELL_PAD)
            val y = H - 170f - row * (CELL_H + CELL_PAD)

            val owned = car.assetName in game.unlockedCars
            val isSelected = i == selectedIdx

            // Фон клітинки
            when {
                isSelected -> shape.setColor(0.25f, 0.6f, 0.95f, 1f)
                owned      -> shape.setColor(0.9f, 0.95f, 1f, 1f)
                else       -> shape.setColor(0.88f, 0.88f, 0.88f, 1f)
            }
            shape.roundedRect(x, y, CELL_W, CELL_H, 12f)

            // Замок overlay якщо не куплена
            if (!owned) {
                shape.setColor(0f, 0f, 0f, 0.15f)
                shape.roundedRect(x, y, CELL_W, CELL_H, 12f)
            }
        }

        shape.end()

        // Batch для зображень машин і тексту
        game.batch.begin()

        // Монети текст
        game.fontMedium.setColor(0.3f, 0.2f, 0f, 1f)
        game.fontMedium.draw(game.batch, "\uD83E\uDE99 ${game.coins}", W/2f - 85f, H - 36f)

        // Заголовок
        game.fontMedium.setColor(0.15f, 0.15f, 0.15f, 1f)
        game.fontMedium.draw(game.batch, "ВИБІР МАШИНИ", W/2f - 130f, H - 90f)

        // Малюємо машини
        for ((i, car) in CAR_DATA.withIndex()) {
            val col = i % COLS
            val row = i / COLS
            val x = gridLeft + col * (CELL_W + CELL_PAD)
            val y = H - 170f - row * (CELL_H + CELL_PAD)

            val owned = car.assetName in game.unlockedCars

            // Зображення
            val tex = game.assets.get("${car.assetName}.png", Texture::class.java)
            val imgSize = 70f
            val alpha = if (owned) 1f else 0.4f
            game.batch.setColor(1f, 1f, 1f, alpha)
            game.batch.draw(tex, x + CELL_W/2f - imgSize/2f, y + CELL_H - imgSize - 8f, imgSize, imgSize)
            game.batch.setColor(1f, 1f, 1f, 1f)

            // Назва
            val nameColor = if (i == selectedIdx) 1f else 0.2f
            game.fontSmall.setColor(nameColor, nameColor, nameColor, 1f)
            val shortName = car.displayName.split(" ").first()
            game.fontSmall.draw(game.batch, shortName, x + 6f, y + CELL_H - imgSize - 14f)

            // Ціна або "ВИБРАНО"
            when {
                car.assetName == game.selectedCar -> {
                    game.fontSmall.setColor(0.1f, 0.7f, 0.3f, 1f)
                    game.fontSmall.draw(game.batch, "ОБРАНО", x + 10f, y + 28f)
                }
                owned -> {
                    game.fontSmall.setColor(0.3f, 0.3f, 0.8f, 1f)
                    game.fontSmall.draw(game.batch, "ВИБРАТИ", x + 6f, y + 28f)
                }
                else -> {
                    game.fontSmall.setColor(0.7f, 0.5f, 0f, 1f)
                    game.fontSmall.draw(game.batch, "\uD83E\uDE99${car.price}", x + 16f, y + 28f)
                }
            }

            // Замок
            if (!owned) {
                game.fontMedium.setColor(0.3f, 0.3f, 0.3f, 0.6f)
                game.fontMedium.draw(game.batch, "\uD83D\uDD12", x + CELL_W/2f - 14f, y + CELL_H/2f + 10f)
            }
        }

        // Повідомлення (купівля/помилка)
        if (messageTimer > 0f) {
            game.fontMedium.setColor(0.15f, 0.15f, 0.15f, messageTimer.coerceAtMost(1f))
            game.fontMedium.draw(game.batch, message, W/2f - 130f, 110f)
        }

        // Назад
        game.fontSmall.setColor(0.4f, 0.4f, 0.4f, 1f)
        game.fontSmall.draw(game.batch, "< НАЗАД", 16f, 45f)

        game.batch.end()

        // Тапи
        if (Gdx.input.justTouched()) {
            val touch = cam.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))

            if (touch.y < 60f && touch.x < 120f) {
                game.setScreen(MenuScreen(game)); return
            }

            val gl = (W - COLS * (CELL_W + CELL_PAD) + CELL_PAD) / 2f
            for ((i, car) in CAR_DATA.withIndex()) {
                val col = i % COLS
                val row = i / COLS
                val x = gl + col * (CELL_W + CELL_PAD)
                val y = H - 170f - row * (CELL_H + CELL_PAD)
                val rect = Rectangle(x, y, CELL_W, CELL_H)

                if (rect.contains(touch.x, touch.y)) {
                    selectedIdx = i
                    val owned = car.assetName in game.unlockedCars

                    if (owned) {
                        // Вибрати машину
                        game.selectedCar = car.assetName
                        game.savePrefs()
                        message = "Обрано: ${car.displayName}!"
                        messageTimer = 2f
                    } else {
                        // Купити
                        if (game.coins >= car.price) {
                            game.coins -= car.price
                            game.unlockedCars.add(car.assetName)
                            game.selectedCar = car.assetName
                            game.savePrefs()
                            message = "Куплено ${car.displayName}!"
                            messageTimer = 2.5f
                        } else {
                            val need = car.price - game.coins
                            message = "Не вистачає ${need} монет!"
                            messageTimer = 2f
                        }
                    }
                    break
                }
            }
        }
    }

    override fun resize(w: Int, h: Int) { cam.setToOrtho(false, W, H) }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() { shape.dispose() }
}
