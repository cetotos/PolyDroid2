package com.cetotos.polydroid2

import android.content.Context
import android.util.Log
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.File

object RootFs {
    private const val TAG = "PolyDroid2"
    private const val VERSION = 6

    fun rootDir(ctx: Context): File = File(ctx.filesDir, "rootfs")

    fun isInstalled(ctx: Context): Boolean {
        val versionFile = File(rootDir(ctx), ".pd_version")
        return versionFile.exists() && versionFile.readText().trim() == VERSION.toString()
    }

    fun install(ctx: Context, onProgress: (String) -> Unit) {
        val root = rootDir(ctx)
        if (root.exists()) root.deleteRecursively()
        root.mkdirs()

        onProgress("Extracting rootfs")
        Log.i(TAG, "Extracting rootfs...")

        ctx.assets.open("rootfs.tar.xz").use { raw ->
            BufferedInputStream(raw, 65536).use { buffered ->
                XZInputStream(buffered).use { xz ->
                    TarArchiveInputStream(xz).use { tar ->
                        var entry = tar.nextEntry
                        while (entry != null) {
                            val outFile = File(root, entry.name)

                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else if (entry.isSymbolicLink) {
                                try {
                                    val target = java.nio.file.Paths.get(entry.linkName)
                                    val link = outFile.toPath()
                                    outFile.parentFile?.mkdirs()
                                    java.nio.file.Files.createSymbolicLink(link, target)
                                } catch (e: Exception) {
                                    Log.w(TAG, "failed to create symlink: ${entry.name} -> ${entry.linkName}")
                                }
                            } else {
                                outFile.parentFile?.mkdirs()
                                outFile.outputStream().use { out ->
                                    tar.copyTo(out)
                                }
                                if (entry.mode and 0b001_001_001 != 0) {
                                    outFile.setExecutable(true, false)
                                }
                            }
                            entry = tar.nextEntry
                        }
                    }
                }
            }
        }
        val polyBinary = File(root, "polytoria/Polytoria Client.x86_64")
        if (polyBinary.exists()) polyBinary.setExecutable(true, false)
        root.resolve("usr/bin").listFiles()?.forEach { it.setExecutable(true, false) }
        root.resolve("usr/sbin").listFiles()?.forEach { it.setExecutable(true, false) }
        File(root, ".pd_version").writeText(VERSION.toString())
        Log.i(TAG, "Rootfs extraction complete")
        onProgress("Rootfs ready.")
        extractPolytoria(ctx, onProgress)

    }

    fun extractPolytoria(ctx: Context, onProgress: (String) -> Unit) {
        val polyDir = File(rootDir(ctx), "polytoria")
        if (File(polyDir, "Polytoria Client.x86_64").exists()) {
            onProgress("Polytoria client already installed.")
            return
        }

        polyDir.mkdirs()
        onProgress("Extracting Polytoria client...")
        Log.i(TAG, "Extracting client...")

        ctx.assets.open("polytoria_client.txz").use { raw ->
            BufferedInputStream(raw, 65536).use { buffered ->
                XZInputStream(buffered).use { xz ->
                    TarArchiveInputStream(xz).use { tar ->
                        var entry = tar.nextEntry
                        while (entry != null) {
                            val outFile = File(polyDir, entry.name)

                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else if (entry.isSymbolicLink) {
                                try {
                                    val target = java.nio.file.Paths.get(entry.linkName)
                                    val link = outFile.toPath()
                                    outFile.parentFile?.mkdirs()
                                    java.nio.file.Files.createSymbolicLink(link, target)
                                } catch (e: Exception) {
                                    Log.w(TAG, "failed to create symlink: ${entry.name} -> ${entry.linkName}")
                                }
                            } else {
                                outFile.parentFile?.mkdirs()
                                outFile.outputStream().use { out ->
                                    tar.copyTo(out)
                                }
                                if (entry.mode and 0b001_001_001 != 0) {
                                    outFile.setExecutable(true, false)
                                }
                            }
                            entry = tar.nextEntry
                        }
                    }
                }
            }
        }

        File(polyDir, "Polytoria Client.x86_64").setExecutable(true, false)
        Log.i(TAG, "Extracted successfuly")
        onProgress("Extraction complete")
    }

}
