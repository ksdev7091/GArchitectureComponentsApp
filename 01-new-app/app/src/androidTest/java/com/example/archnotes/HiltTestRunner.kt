package com.example.archnotes

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Instrumented Test 用のランナー。
 * 本番の [ArchNotesApplication](@HiltAndroidApp)の代わりに Hilt のテスト用 Application を使う。
 * `build.gradle.kts` の `testInstrumentationRunner` に指定している。
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
