package app.gamenative.ui.screen.wow

import java.io.File

private val LOW_END_ADRENO_SOCS = listOf("SM6225", "SM6115", "SM6375", "SM6350")
private val LOW_END_ADRENO_MODEL = Regex("\\b6[0-2][0-9]\\b")

/**
 * True for Adreno 600-629 (610/612/613/615/616/618/619/620; Snapdragon 4xx/6xx), judged from the GL renderer
 * string first and the SoC model as a fallback (SM6225 is the Snapdragon 685). Adreno 630+ is far stronger and
 * stays on the regular profile.
 */
internal fun isLowEndAdreno6xx(renderer: String, socModel: String): Boolean {
    val r = renderer.lowercase()
    if (r.contains("adreno") && LOW_END_ADRENO_MODEL.containsMatchIn(r)) return true
    return LOW_END_ADRENO_SOCS.any { socModel.contains(it, ignoreCase = true) }
}

/**
 * Picks the performance cores of a big.LITTLE chip from each core's max frequency, as a CPU list such as
 * "4,5,6,7". Cores at or above the midpoint between the slowest and fastest core count as performance cores.
 * Returns null when the frequencies are unreadable or all cores are equal, so the container keeps its default.
 */
internal fun selectPerformanceCores(maxFreqsKHz: List<Long>): String? {
    val top = maxFreqsKHz.maxOrNull() ?: return null
    val bottom = maxFreqsKHz.min()
    if (bottom <= 0L || top == bottom) return null
    val midpoint = (top + bottom) / 2.0
    return maxFreqsKHz.indices.filter { maxFreqsKHz[it] >= midpoint }.joinToString(",")
}

/** Reads each core's max frequency from sysfs. An unreadable core counts as 0, which makes the result null. */
internal fun performanceCoreList(): String? {
    val count = Runtime.getRuntime().availableProcessors()
    val freqs = (0 until count).map { cpu ->
        runCatching { File("/sys/devices/system/cpu/cpu$cpu/cpufreq/cpuinfo_max_freq").readText().trim().toLong() }
            .getOrDefault(0L)
    }
    return selectPerformanceCores(freqs)
}

/** Render size for low-end Adreno 6xx: fewer pixels means less GPU load and less memory for render targets. */
internal const val LOW_END_SCREEN_SIZE = "854x480"

/**
 * DXVK settings for the container. On low-end devices DXVK's default of one shader compiler thread per core (8)
 * starves the game and spikes memory during loading, so it's capped; the reported video memory is capped too so the
 * client keeps its texture budget small.
 */
internal fun dxwrapperConfig(version: String, lowEnd: Boolean): String =
    if (lowEnd) "version=$version,numCompilerThreads=2,maxDeviceMemory=2048" else "version=$version"
