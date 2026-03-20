package com.driftrush.game

class ScoreManager {

    var score: Int = 0
        private set
    var combo: Int = 0
        private set
    var maxCombo: Int = 0
        private set
    var coinsCollected: Int = 0
        private set
    var boostsCollected: Int = 0
        private set
    var edgeBonusCount: Int = 0
        private set
    var cleanCorners: Int = 0
        private set

    private var comboTimer = 0f
    private val COMBO_TIMEOUT = 3.5f

    val multiplier: Int get() = when {
        combo >= 10 -> 5
        combo >= 5  -> 3
        combo >= 2  -> 2
        else        -> 1
    }

    fun update(delta: Float) {
        if (combo > 0) {
            comboTimer += delta
            if (comboTimer >= COMBO_TIMEOUT) {
                resetCombo()
            }
        }
    }

    fun onCleanCorner() {
        combo++
        cleanCorners++
        comboTimer = 0f
        if (combo > maxCombo) maxCombo = combo
        addScore(50 * multiplier)
    }

    fun onEdgeBonus(edgeRatio: Float) {
        if (edgeRatio > 0.75f) {
            edgeBonusCount++
            addScore((30 * multiplier * edgeRatio).toInt())
        }
    }

    fun onCoinCollected() {
        coinsCollected++
        addScore(10 * multiplier)
    }

    fun onBoostCollected() {
        boostsCollected++
        combo += 2
        comboTimer = 0f
        if (combo > maxCombo) maxCombo = combo
        addScore(100 * multiplier)
    }

    fun onCrash() {
        resetCombo()
    }

    fun onLevelComplete(timeBonus: Int = 0) {
        addScore(500 * multiplier + timeBonus)
    }

    private fun addScore(points: Int) {
        score += points
    }

    private fun resetCombo() {
        combo = 0
        comboTimer = 0f
    }

    // Розраховуємо монети за результат рівня
    fun calculateCoinsEarned(levelIndex: Int, completed: Boolean): Int {
        if (!completed) {
            // Провал — мінімальні монети за спробу
            return (score / 100).coerceIn(1, 10)
        }

        var coins = 0
        coins += score / 50              // базово з очок
        coins += coinsCollected          // зібрані монети
        coins += cleanCorners * 2        // за чисті повороти
        coins += edgeBonusCount          // за їзду по краю
        coins += if (maxCombo >= 5) 20 else 0   // бонус за комбо
        coins += if (maxCombo >= 10) 30 else 0  // бонус за велике комбо

        return coins.coerceAtLeast(5)
    }

    // Зірки за рівень (1-3)
    fun getStars(completed: Boolean): Int {
        if (!completed) return 0
        return when {
            score >= 2000 && maxCombo >= 5 -> 3
            score >= 1000 -> 2
            else -> 1
        }
    }
}
