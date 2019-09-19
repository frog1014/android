
// hack for set locale @androidx.appcompat:appcompat:1.1.0
override fun attachBaseContext(base: Context) {
        LocaleManager.newConfig(base).let {
            if (isAfter24 && !isAfter26) try {
                applyOverrideConfiguration(it)
            } catch (e: Throwable) {
            }
            super.attachBaseContext(base.createConfigurationContext(it))
        }
    }