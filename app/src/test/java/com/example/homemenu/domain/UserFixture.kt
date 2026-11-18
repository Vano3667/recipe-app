package com.example.homemenu.domain

import java.io.File

/**
 * The user's real import file (`main`, commit 11059ed), kept at the repository root and never modified.
 * Unit tests run with the `app/` module as working directory; we also walk up the tree to be safe.
 */
object UserFixture {
    const val FILE_NAME = "recipes_from_budget_classic_1500kcal.json"
    const val SIZE_BYTES = 439_609L
    const val RECIPES = 140
    const val INGREDIENTS = 841
    const val STEPS = 692

    fun file(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val f = File(dir, FILE_NAME)
            if (f.isFile) return f
            dir = dir.parentFile
        }
        throw AssertionError("$FILE_NAME not found from ${File("").absolutePath}: the user fixture must stay in the repository root")
    }

    fun text(): String = file().readText(Charsets.UTF_8)
}
