package com.aura.core.i18n

object StringsRu {
    object Common {
        val appName get() = I18n.t("common.appName", "AURA")
        val userFallback get() = I18n.t("common.userFallback", "Пользователь")
        val back get() = I18n.t("common.back", "Назад")
        val settings get() = I18n.t("common.settings", "Настройки")
        val edit get() = I18n.t("common.edit", "Редактировать")
        val skip get() = I18n.t("common.skip", "Пропустить")
        val continueAction get() = I18n.t("common.continue", "ПРОДОЛЖИТЬ")
        val completeSurvey get() = I18n.t("common.completeSurvey", "ЗАВЕРШИТЬ АНКЕТУ")
        val saving get() = I18n.t("common.saving", "СОХРАНЕНИЕ...")
    }

    object Splash {
        val subtitle get() = I18n.t("splash.subtitle", "ОСОЗНАННАЯ АРХИТЕКТУРА КРАСОТЫ")
    }

    object Home {
        val today get() = I18n.t("home.today", "Сегодня")
        val goodMorningPrefix get() = I18n.t("home.goodMorningPrefix", "Доброе утро")
        val weather get() = I18n.t("home.weather", "Погода")
        val humidity get() = I18n.t("home.humidity", "Уровень влажности")
        val humiditySubtitle get() = I18n.t("home.humiditySubtitle", "Оптимально для увлажнения")
        val airQuality get() = I18n.t("home.airQuality", "Качество воздуха")
        val airGood get() = I18n.t("home.airGood", "Хорошее")
        val ritual get() = I18n.t("home.ritual", "Ритуал")
        val ritualStepsLeft get() = I18n.t("home.ritualStepsLeft", "Осталось 3 шага")
        val ritualCleanser get() = I18n.t("home.ritualCleanser", "Мягкое очищение")
        val ritualCleanserSubtitle get() = I18n.t("home.ritualCleanserSubtitle", "Массаж 60 секунд")
        val ritualVitaminC get() = I18n.t("home.ritualVitaminC", "Сыворотка с витамином C")
        val ritualVitaminCSubtitle get() = I18n.t("home.ritualVitaminCSubtitle", "Осветляющий комплекс")
        val ritualGel get() = I18n.t("home.ritualGel", "Увлажняющий гель")
        val ritualGelSubtitle get() = I18n.t("home.ritualGelSubtitle", "Нужен гиалуроновый буст")
        val ritualSpf get() = I18n.t("home.ritualSpf", "Солнцезащитный крем SPF 50+")
        val ritualSpfSubtitle get() = I18n.t("home.ritualSpfSubtitle", "Важно: высокий УФ-индекс")
        val checked get() = I18n.t("home.checked", "Отмечено")
        val aiInsights get() = I18n.t("home.aiInsights", "ИИ-инсайты Aura")
        val viewAll get() = I18n.t("home.viewAll", "Смотреть все")
        val weeklyScan get() = I18n.t("home.weeklyScan", "Еженедельный скан кожи")
        val weeklyScanSubtitle get() = I18n.t("home.weeklyScanSubtitle", "Нужно выполнить сегодня")
        val refresh get() = I18n.t("home.refresh", "Обновление ухода")
        val refreshSubtitle get() = I18n.t("home.refreshSubtitle", "Ночной крем скоро закончится")
        val hydrationAlert get() = I18n.t("home.hydrationAlert", "Оповещение об увлажнении")
        val hydrationAlertSubtitle get() = I18n.t("home.hydrationAlertSubtitle", "Пейте воду: кожа сегодня сухая")
    }

    object Profile {
        val title get() = I18n.t("profile.title", "МОЙ ПРОФИЛЬ")
    }

    object Chat {
        val aiShort get() = I18n.t("chat.aiShort", "ИИ")
        val aiAssistant get() = I18n.t("chat.aiAssistant", "ИИ-ассистент")
        val online get() = I18n.t("chat.online", "В сети")
        val menu get() = I18n.t("chat.menu", "Меню")
        val quickQuestions get() = I18n.t("chat.quickQuestions", "Быстрые вопросы")
        val q1 get() = I18n.t("chat.q1", "Что мне подходит?")
        val q2 get() = I18n.t("chat.q2", "Мой тип кожи?")
        val q3 get() = I18n.t("chat.q3", "Лучший SPF?")
        val hello get() = I18n.t("chat.hello", "Привет! Я ваш ИИ-ассистент по уходу за кожей. Чем могу помочь?")
        val userExample get() = I18n.t("chat.userExample", "У меня сухая кожа, что выбрать?")
        val assistantExample get() = I18n.t("chat.assistantExample", "Для сухой кожи рекомендую увлажняющую сыворотку с гиалуроновой кислотой и плотный крем с керамидами. Хотите, чтобы я подобрал продукты?")
        val inputPlaceholder get() = I18n.t("chat.inputPlaceholder", "Введите сообщение...")
        val add get() = I18n.t("chat.add", "Добавить")
        val camera get() = I18n.t("chat.camera", "Камера")
        val send get() = I18n.t("chat.send", "Отправить")
        val mic get() = I18n.t("chat.mic", "Микрофон")
    }

    object Auth {
        val subtitle get() = I18n.t("auth.subtitle", "Ваш персональный ИИ-уход")
        val loginTitle get() = I18n.t("auth.loginTitle", "Авторизация")
        val registerTitle get() = I18n.t("auth.registerTitle", "Регистрация")
        val nameLabel get() = I18n.t("auth.nameLabel", "ИМЯ")
        val loginLabel get() = I18n.t("auth.loginLabel", "ЛОГИН")
        val phoneLabel get() = I18n.t("auth.phoneLabel", "ТЕЛЕФОН")
        val emailLabel get() = I18n.t("auth.emailLabel", "ЭЛЕКТРОННАЯ ПОЧТА")
        val passwordLabel get() = I18n.t("auth.passwordLabel", "ПАРОЛЬ")
        val confirmLabel get() = I18n.t("auth.confirmLabel", "ПОДТВЕРЖДЕНИЕ")
        val forgotPassword get() = I18n.t("auth.forgotPassword", "Забыли пароль?")
        val loginAction get() = I18n.t("auth.loginAction", "Войти")
        val registerAction get() = I18n.t("auth.registerAction", "Зарегистрироваться")
        val noAccount get() = I18n.t("auth.noAccount", "Нет аккаунта? ")
        val hasAccount get() = I18n.t("auth.hasAccount", "Есть аккаунт? ")
        val enterEmail get() = I18n.t("auth.enterEmail", "Введите email")
        val invalidEmail get() = I18n.t("auth.invalidEmail", "Некорректный email")
        val minPassword get() = I18n.t("auth.minPassword", "Пароль минимум 6 символов")
        val enterName get() = I18n.t("auth.enterName", "Введите имя")
        val minName get() = I18n.t("auth.minName", "Имя минимум 2 символа")
        val enterLogin get() = I18n.t("auth.enterLogin", "Введите логин")
        val invalidLogin get() = I18n.t("auth.invalidLogin", "Логин: только латиница, цифры, _")
        val invalidPhone get() = I18n.t("auth.invalidPhone", "Некорректный телефон")
        val passwordsMismatch get() = I18n.t("auth.passwordsMismatch", "Пароли не совпадают")
    }

    object Survey {
        val title get() = I18n.t("survey.title", "Анализ кожи")
        val progress get() = I18n.t("survey.progress", "ПРОГРЕСС")
        val blocksSuffix get() = I18n.t("survey.blocksSuffix", "БЛОКОВ")
    }
}
