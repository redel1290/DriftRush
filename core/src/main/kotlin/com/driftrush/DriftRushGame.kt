package com.driftrush

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.driftrush.screens.MenuScreen

class DriftRushGame : Game() {

    lateinit var batch: SpriteBatch
    lateinit var fontSmall: BitmapFont
    lateinit var fontMedium: BitmapFont
    lateinit var fontLarge: BitmapFont
    lateinit var assets: AssetManager

    // Дані гравця (зберігаються через Preferences)
    var coins: Int = 0
    var unlockedLevels: Int = 1
    var unlockedCars: MutableSet<String> = mutableSetOf("car_red_sport")
    var selectedCar: String = "car_red_sport"
    var completedLevels: Int = 0

    companion object {
        const val WORLD_WIDTH = 480f
        const val WORLD_HEIGHT = 854f
        lateinit var instance: DriftRushGame
    }

    override fun create() {
        instance = this
        batch = SpriteBatch()
        assets = AssetManager()

        // Завантажуємо всі спрайти машин
        CAR_DATA.forEach { car ->
            assets.load("${car.assetName}.png", Texture::class.java)
        }
        assets.finishLoading()

        // Шрифти — bitmap
        fontSmall = BitmapFont()
        fontSmall.data.setScale(1.2f)
        fontMedium = BitmapFont()
        fontMedium.data.setScale(2f)
        fontLarge = BitmapFont()
        fontLarge.data.setScale(3f)

        loadPrefs()
        setScreen(MenuScreen(this))
    }

    fun loadPrefs() {
        val prefs = Gdx.app.getPreferences("DriftRushPrefs")
        coins = prefs.getInteger("coins", 0)
        unlockedLevels = prefs.getInteger("unlockedLevels", 1)
        completedLevels = prefs.getInteger("completedLevels", 0)
        selectedCar = prefs.getString("selectedCar", "car_red_sport")
        val carsStr = prefs.getString("unlockedCars", "car_red_sport")
        unlockedCars = carsStr.split(",").toMutableSet()
    }

    fun savePrefs() {
        val prefs = Gdx.app.getPreferences("DriftRushPrefs")
        prefs.putInteger("coins", coins)
        prefs.putInteger("unlockedLevels", unlockedLevels)
        prefs.putInteger("completedLevels", completedLevels)
        prefs.putString("selectedCar", selectedCar)
        prefs.putString("unlockedCars", unlockedCars.joinToString(","))
        prefs.flush()
    }

    fun addCoins(amount: Int) {
        coins += amount
        savePrefs()
    }

    override fun dispose() {
        batch.dispose()
        fontSmall.dispose()
        fontMedium.dispose()
        fontLarge.dispose()
        assets.dispose()
    }
}

// ===== ДАНІ МАШИН =====
data class CarData(
    val assetName: String,
    val displayName: String,
    val price: Int,       // 0 = безкоштовна
    val speed: Float,     // відносна швидкість 1.0 = базова
    val handling: Float   // керованість 1.0 = базова
)

val CAR_DATA = listOf(
    CarData("car_red_sport",    "Red Sport",    0,    1.0f, 1.0f),
    CarData("car_yellow_sport", "Yellow Sport", 150,  1.05f, 1.05f),
    CarData("car_green_sport",  "Green Sport",  300,  1.1f,  1.0f),
    CarData("car_purple_sport", "Purple Sport", 500,  1.1f,  1.1f),
    CarData("car_blue_muscle",  "Blue Muscle",  750,  1.15f, 0.95f),
    CarData("car_red_muscle",   "Red Muscle",   1000, 1.2f,  0.95f),
    CarData("car_black_muscle", "Black Muscle", 1500, 1.25f, 0.9f),
    CarData("car_orange_muscle","Orange Muscle",2000, 1.3f,  0.9f),
    CarData("car_silver_sedan", "Silver Sedan", 2500, 1.2f,  1.15f),
    CarData("car_gold_sedan",   "Gold Sedan",   3500, 1.25f, 1.2f),
    CarData("car_black_sedan",  "Black Sedan",  5000, 1.35f, 1.1f),
    CarData("car_white_sedan",  "White Sedan",  7500, 1.4f,  1.15f)
)
