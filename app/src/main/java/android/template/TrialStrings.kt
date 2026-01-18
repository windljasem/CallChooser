package com.callchooser.app

/**
 * Trial and Premium локалізовані рядки
 */
data class TrialStrings(
    // Trial status
    val trialActive: String,
    val trialDaysLeft: (Int) -> String,
    val trialExpired: String,
    
    // Premium
    val premiumTitle: String,
    val premiumFeatures: List<String>,
    val premiumPrice: String,
    
    // Buttons
    val buyNow: String,
    val restorePurchases: String,
    val maybeLater: String,
    
    // Messages
    val premiumRequired: String,
    val premiumUnlocked: String,
    val purchaseSuccess: String,
    val purchaseFailed: String,
    val purchaseCanceled: String,
    val restoreSuccess: String,
    val restoreNotFound: String,
    val restoreFailed: String,
    val processingPurchase: String,
    val noInternet: String,
    
    // Developer Mode
    val devModeActive: String
)

/**
 * Українська (UK)
 */
fun getTrialStringsUK(): TrialStrings = TrialStrings(
    // Trial status
    trialActive = "Пробний період: %d днів",
    trialDaysLeft = { days -> 
        when {
            days == 1 -> "⚠️ Залишився 1 день trial"
            days in 2..4 -> "⚠️ Залишилось $days дні trial"
            else -> "⚠️ Залишилось $days днів trial"
        }
    },
    trialExpired = "Пробний період закінчився",
    
    // Premium
    premiumTitle = "Розблокуйте Premium",
    premiumFeatures = listOf(
        "✅ Необмежені дзвінки через месенджери",
        "✅ Без обмежень часу",
        "✅ Всі майбутні функції",
        "✅ Одноразова оплата"
    ),
    premiumPrice = "Тільки 2.99€",
    
    // Buttons
    buyNow = "Купити Premium",
    restorePurchases = "Відновити покупки",
    maybeLater = "Можливо пізніше",
    
    // Messages
    premiumRequired = "Потрібен Premium для месенджерів",
    premiumUnlocked = "🎉 Premium розблоковано!",
    purchaseSuccess = "✅ Покупка успішна!",
    purchaseFailed = "❌ Помилка покупки",
    purchaseCanceled = "Покупку скасовано",
    restoreSuccess = "✅ Покупки відновлено!",
    restoreNotFound = "ℹ️ Покупок не знайдено",
    restoreFailed = "❌ Помилка відновлення",
    processingPurchase = "⏳ Обробка покупки...",
    noInternet = "⚠️ Потрібне інтернет-з'єднання",
    
    // Developer Mode
    devModeActive = "🔧 Developer Mode активний"
)

/**
 * Англійська (EN)
 */
fun getTrialStringsEN(): TrialStrings = TrialStrings(
    // Trial status
    trialActive = "Trial period: %d days",
    trialDaysLeft = { days -> 
        when {
            days == 1 -> "⚠️ 1 day left"
            else -> "⚠️ $days days left"
        }
    },
    trialExpired = "Trial period expired",
    
    // Premium
    premiumTitle = "Unlock Premium",
    premiumFeatures = listOf(
        "✅ Unlimited messenger calls",
        "✅ No time limits",
        "✅ All future features",
        "✅ One-time payment"
    ),
    premiumPrice = "Only 2.99€",
    
    // Buttons
    buyNow = "Buy Premium",
    restorePurchases = "Restore Purchases",
    maybeLater = "Maybe Later",
    
    // Messages
    premiumRequired = "Premium required for messengers",
    premiumUnlocked = "🎉 Premium unlocked!",
    purchaseSuccess = "✅ Purchase successful!",
    purchaseFailed = "❌ Purchase failed",
    purchaseCanceled = "Purchase canceled",
    restoreSuccess = "✅ Purchases restored!",
    restoreNotFound = "ℹ️ No purchases found",
    restoreFailed = "❌ Restore failed",
    processingPurchase = "⏳ Processing purchase...",
    noInternet = "⚠️ Internet connection required",
    
    // Developer Mode
    devModeActive = "🔧 Developer Mode active"
)

/**
 * Отримати strings по мові
 */
fun getTrialStrings(language: Language): TrialStrings {
    return when (language) {
        Language.UK -> getTrialStringsUK()
        Language.EN -> getTrialStringsEN()
    }
}
